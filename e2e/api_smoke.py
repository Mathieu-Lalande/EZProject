"""Parcours API de bout en bout (application sur EZ_BASE_URL, défaut http://localhost:8080, profil local + PostgreSQL).

Rejouable : le projet créé porte un code unique. Utilisé par la CI (job e2e)."""
import os
import http.cookiejar
import json
import sys
import urllib.error
import urllib.parse
import urllib.request
import uuid

BASE = os.environ.get("EZ_BASE_URL", "http://localhost:8080")
failures = []


def past_working_day():
    """Un jour ouvré passé (sans jour férié fixe français) : la saisie de temps refuse le futur."""
    import datetime
    d = datetime.date.today() - datetime.timedelta(days=14)
    while d.weekday() >= 5 or (d.month, d.day) in {(1, 1), (5, 1), (5, 8), (7, 14), (8, 15), (11, 1), (11, 11), (12, 25)}:
        d -= datetime.timedelta(days=1)
    return d.isoformat()


class Client:
    def __init__(self):
        self.jar = http.cookiejar.CookieJar()
        self.opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(self.jar))

    def token(self):
        return next((c.value for c in self.jar if c.name == "XSRF-TOKEN"), "")

    def call(self, method, path, body=None, form=None, raw=None, content_type=None):
        headers = {}
        data = None
        if method != "GET":
            headers["X-XSRF-TOKEN"] = urllib.parse.unquote(self.token())
        if form is not None:
            data = urllib.parse.urlencode(form).encode()
            headers["Content-Type"] = "application/x-www-form-urlencoded"
        elif body is not None:
            data = json.dumps(body).encode()
            headers["Content-Type"] = "application/json"
        elif raw is not None:
            data = raw
            headers["Content-Type"] = content_type
        req = urllib.request.Request(BASE + path, data=data, headers=headers, method=method)
        try:
            with self.opener.open(req) as r:
                payload = r.read()
                return r.status, payload
        except urllib.error.HTTPError as e:
            return e.code, e.read()

    def json(self, method, path, body=None):
        status, payload = self.call(method, path, body=body)
        return status, (json.loads(payload) if payload and payload[:1] in (b"{", b"[") else payload)

    def login(self, email):
        self.call("GET", "/api/auth/me")
        status, _ = self.call("POST", "/api/auth/login", form={"email": email, "password": "demo1234"})
        return status


def check(label, condition, detail=""):
    print(("OK   " if condition else "FAIL ") + label + (f"  [{detail}]" if not condition and detail else ""))
    if not condition:
        failures.append(label)


coo = Client()
check("connexion COO", coo.login("camille@ezproject.local") == 204)
s, projects = coo.json("GET", "/api/projects")
codes = sorted(p["code"] for p in projects)
check("COO voit les projets Horizon, pas ERP", s == 200 and {"PORTAIL", "MANDA"} <= set(codes) and "ERP" not in codes, codes)
s, pf = coo.json("GET", "/api/portfolio")
check("portefeuille (budget, risques, météo)", s == 200 and pf["kpis"]["criticalRisks"] >= 1, s)
portail = next(p["id"] for p in projects if p["code"] == "PORTAIL")
code = "E2E" + uuid.uuid4().hex[:6].upper()
s, created = coo.json("POST", "/api/projects", {"code": code, "name": "Projet E2E", "type": "CONSULTING"})
check("création de projet", s == 201, created)
s, tasks = coo.json("GET", f"/api/projects/{portail}/tasks")
task_id = tasks[0]["id"]
check("tâches avec compteurs", s == 200 and "counts" in tasks[0])
s, _ = coo.json("POST", f"/api/projects/{portail}/time", {"date": past_working_day(), "days": 0.5})
check("saisie de temps", s == 201, s)
s, summary = coo.json("GET", f"/api/projects/{portail}/time/summary")
check("synthèse budget", s == 200 and summary["consumedDays"] > 0)
png = bytes([0x89, 0x50, 0x4E, 0x47, 13, 10, 26, 10, 0, 0, 0, 13])
boundary = uuid.uuid4().hex
multipart = (f"--{boundary}\r\nContent-Disposition: form-data; name=\"file\"; filename=\"plan.png\"\r\n"
             f"Content-Type: image/png\r\n\r\n").encode() + png + f"\r\n--{boundary}--\r\n".encode()
s, att = coo.call("POST", f"/api/tasks/{task_id}/attachments", raw=multipart, content_type=f"multipart/form-data; boundary={boundary}")
att_id = json.loads(att)["id"] if s == 201 else None
check("pièce jointe (bytea)", s == 201, att)
s, content = coo.call("GET", f"/api/attachments/{att_id}")
check("téléchargement de la pièce jointe", s == 200 and content == png)
s, _ = coo.json("POST", f"/api/projects/{portail}/risks", {"title": "Risque PG", "probability": 3, "impact": 4})
check("création de risque", s == 201, s)
s, _ = coo.json("POST", f"/api/projects/{portail}/reports", {"health": "GREEN", "trend": "UP", "summary": "Point PG"})
check("publication de météo", s == 201, s)
s, cap = coo.json("GET", "/api/capacity?weeks=4")
check("capacité", s == 200 and len(cap["people"]) >= 5)
s, audit = coo.json("GET", "/api/organization/audit?q=pg")
check("journal d'audit", s == 200 and audit["totalElements"] >= 1, audit)
s, exp = coo.json("GET", "/api/organization/export")
check("export organisation", s == 200 and any(p["code"] == code for p in exp["projects"]))
s, dup = coo.json("POST", "/api/users", {"email": "bob@acme.local", "displayName": "Doublon", "password": "un-mot-de-passe-solide", "role": "MEMBER"})
check("email d'une autre organisation refusé proprement (409, pas 500)", s == 409, s)
s, mfa = coo.json("POST", "/api/account/mfa/setup")
check("initialisation 2FA (secret chiffré)", s == 200 and mfa["otpauthUri"].startswith("otpauth://"))

acme = Client()
check("connexion Acme", acme.login("bob@acme.local") == 204)
s, projects = acme.json("GET", "/api/projects")
check("Acme ne voit que ERP", [p["code"] for p in projects] == ["ERP"], projects)
s, _ = acme.json("GET", f"/api/projects/{portail}")
check("Acme : projet Horizon introuvable (404)", s == 404, s)
s, _ = acme.call("GET", f"/api/attachments/{att_id}")
check("Acme : pièce jointe Horizon introuvable (404)", s == 404, s)
s, users = acme.json("GET", "/api/users")
check("Acme : 1 seul utilisateur visible", s == 200 and len(users) == 1)
s, pf = acme.json("GET", "/api/portfolio")
check("Acme : portefeuille limité", s == 200 and [p["code"] for p in pf["projects"]] == ["ERP"])

print("\n" + ("TOUT EST VERT" if not failures else f"{len(failures)} ÉCHEC(S) : {failures}"))
sys.exit(1 if failures else 0)
