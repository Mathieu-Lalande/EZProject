# EZProject

Outil SaaS de gestion de projets **multi-organisation** et **modulable**, conçu pour les équipes de **développement**, les missions de **conseil** (CIO office, PMO) et le **pilotage de portefeuille** (COO, office manager).

## Fonctionnalités

### Pilotage (COO / PMO)

| Domaine | Ce qu'il fait |
|---|---|
| **Portefeuille** | Indicateurs (projets ouverts, P1, en pause, à risque, risques critiques, budgets dépassés, retards), tri par **priorité P1→P4**, météo avec **tendance**, détection des **météos non mises à jour** (plus de 14 jours), filtre par équipe, export CSV. Le périmètre dépend du rôle. |
| **Roadmap** | Gantt de tous les projets ouverts, regroupés par équipe, avec phases et jalons de rétro-planning (horizon de 6 à 18 mois). |
| **Capacité** | Heatmap personnes × semaines : charge affectée (staffing) face à la disponibilité réelle (temps partiel, jours fériés, absences). La surcharge est signalée. |
| **Temps & budget** | Feuille de temps hebdomadaire et saisie par projet, consommation du budget (jauge), rythme sur 4 semaines, **date d'épuisement prévue**, répartition par personne et par mois. Une dérive budgétaire (consommation plus rapide que l'avancement) remonte au portefeuille. |
| **Risques & décisions** | Registre des risques (probabilité × impact, matrice, plan d'action, responsable) et relevé de décisions (instance, date, statut). |
| **Météo & COPIL** | Points d'avancement publiés et historisés (synthèse, réalisé, prochaines étapes, arbitrages). **Rapport COPIL imprimable / PDF** agrégeant planning, risques, décisions et budget selon les droits du lecteur. |
| **Cycle de vie** | Statuts avec **motif obligatoire** pour la pause ou l'annulation, journal des statuts, priorité en un clic. |

### Projets et équipes

| Domaine | Ce qu'il fait |
|---|---|
| **Kanban** | Glisser-déposer, priorités, story points, échéances. **Étiquettes**, **sous-tâches**, **commentaires** et **pièces jointes** ; filtres par étiquette ou sur ses propres tâches. |
| **Modules** | Checklist d'équipe, rétro-planning (à rebours, jours ouvrés et **jours fériés**), planning poker **en temps réel**, temps & budget, risques. Chaque module s'active par projet. |
| **Mon espace** | Mes tâches par urgence, todos personnels, feuille de temps, déclaration d'absences. |

### SaaS, comptes et sécurité

| Domaine | Ce qu'il fait |
|---|---|
| **Organisations** | Inscription self-service, isolation stricte (applicative et **Row-Level Security PostgreSQL**), offres Free / Team / Enterprise avec quotas. Une personne appartient à une seule organisation. |
| **Droits** | Rôles d'organisation, équipes, rôles projet, **matrice de permissions** personnalisable (voir / ajouter / modifier / supprimer par élément), visibilité Organisation / Équipe / Confidentiel, invités. |
| **Comptes** | Profil, changement de mot de passe, **mot de passe provisoire** par un administrateur (changement imposé), **désactivation immédiate**, **double authentification TOTP** avec codes de secours. |
| **RGPD** | Export de mes données (JSON), suppression de mon compte (anonymisation), export complet et **suppression de l'organisation**, modèle de politique de confidentialité, motif d'absence privé. |
| **Traçabilité** | **Journal d'audit** paginé et filtrable : connexions, droits, projets, temps, risques, imports… |
| **Données** | Import CSV des projets et des tâches (vérification à blanc, tout ou rien), exports CSV. |

## Modèle de droits

**Rôles dans l'organisation**

| Rôle | Portée |
|---|---|
| `OWNER` | Propriétaire : offre, suppression de l'organisation, accès complet. |
| `ADMIN` | Membres, équipes, permissions, journal d'audit ; accès complet aux projets. |
| `MANAGER` | Vision COO / PMO : voit tous les projets et en est Responsable. |
| `MEMBER` | Projets de ses équipes selon son rôle d'équipe, plus les projets ouverts à l'organisation (lecture). |
| `GUEST` | Externe : uniquement les projets où il est ajouté explicitement, au plus Contributeur. |

**Rôles sur un projet.** Ce sont Responsable, Contributeur et Lecteur. On prend le plus élevé entre le rôle hérité de l'équipe et le rôle explicite sur le projet.

**Permissions.** La matrice couvre le projet, les tâches, les commentaires, la checklist, le rétro-planning, le poker, les temps et le budget, les risques et la publication de la météo. Par défaut, un Lecteur (souvent un client) ne voit ni les temps ni le budget.

**Codes d'erreur.**
- Un objet d'une autre organisation, ou non visible, répond **404**.
- Une action interdite répond **403**.

La politique d'accès tient dans `AccessRules` (fonction pure testée) ; le backend fait toujours autorité.

## Stack

- **Backend** : Java 21, Spring Boot 3.5.16 (Web, Data JPA, Validation, Security, Actuator), Flyway, PostgreSQL (H2 en développement), Micrometer/Prometheus, Redis (optionnel), Caffeine, Apache Commons CSV, Maven Wrapper.
- **Frontend** : React 19, TypeScript strict, Vite 7, Tailwind CSS 4, TanStack Query 5, React Router 7, dnd-kit, lucide-react, qrcode.
- **Tests** :
  - JUnit 5, Spring Boot Test, MockMvc, spring-security-test ;
  - Vitest + Testing Library ;
  - Playwright (de bout en bout).

## Lancer en local

Seul le **JDK 21** est requis pour le backend : le Maven Wrapper (`./mvnw`, `mvnw.cmd` sous Windows) télécharge Maven. Node 22+ est requis pour le frontend.

### 1. Développement (rechargement à chaud)

```bash
cd backend && ./mvnw spring-boot:run       # API :8080, base H2 en mémoire (remise à zéro à chaque démarrage)
cd frontend && npm install && npm run dev  # UI http://localhost:5173 (proxy /api -> 8080)
```

### 2. Local « comme en prod » : PostgreSQL + jar unique

Les données sont conservées entre les redémarrages ; l'API et l'interface sont servies par le même jar.

```bash
docker compose up -d db
cd backend && ./mvnw -Pfullstack package   # construit l'interface et l'embarque (Windows : -Dnpm=npm.cmd)
java -jar target/ezproject-backend-0.1.0-SNAPSHOT.jar --spring.profiles.active=local
```

Ouvrez ensuite http://localhost:8080.

### 3. Tout en Docker

```bash
cp .env.example .env        # optionnel
docker compose up --build   # http://localhost:8080
```

Pour repartir de zéro : `docker compose down -v`.

### Comptes de démonstration

Les comptes sont créés au premier démarrage (profils `dev` et `local`). Mot de passe : `demo1234`, modifiable via `DEMO_PASSWORD`. Pour une base vierge, mettez `SEED_DEMO=false` et définissez `ADMIN_EMAIL` / `ADMIN_PASSWORD`.

| Compte | Profil | Ce qu'il illustre |
|---|---|---|
| `camille@ezproject.local` | OWNER (COO) | Portefeuille, roadmap, capacité, administration, audit |
| `lea@ezproject.local` | MANAGER (PMO) | Pilotage de tous les projets, saisie de temps pour autrui |
| `hugo@ezproject.local` | Responsable « Delivery Web » | Surchargé dans la vue capacité, publie la météo |
| `sarah@ezproject.local` | Contributrice | Crée des tâches et des risques, sans supprimer |
| `thomas@ezproject.local` | Responsable « Conseil SI » | Projet AUDITSI en dérive budgétaire, membre du projet confidentiel |
| `nina@ezproject.local` | Contributrice, 4 j/sem. | Temps partiel, ne voit pas les projets « Équipe » des autres |
| `julie@ezproject.local` | GUEST (cliente) | Lecture seule sur « Portail client », sans budget |
| `bob@acme.local` | OWNER d'une autre organisation | Isolation totale |

## Importer vos données existantes

Menu **Organisation → Import / export** : télécharger le modèle, le remplir (ou exporter Excel en « CSV UTF-8 »), cliquer sur **Vérifier** puis **Importer**.

- **Projets** : `code*`, `nom*`, `type`, `statut`, `motif_statut`, `priorite`, `meteo`, `client`, `equipe`, `responsable`, `visibilite`, `debut`, `echeance`, `budget_jours`, `description`.
- **Tâches** : `projet*`, `titre*`, `statut`, `priorite`, `assigne`, `points`, `echeance`, `etiquettes` (séparées par `|`), `description`.

## Tests

```bash
cd backend && ./mvnw verify   # 93 tests (les tests Redis sont ignorés si aucun Redis n'écoute sur localhost:6379)
cd frontend && npm test       # 15 tests
```

Tests de bout en bout, l'application tournant sur http://localhost:8080 en profil `local` :

```bash
python3 e2e/api_smoke.py
cd e2e && npm ci && node ui_smoke.cjs
```

La **CI** (GitHub Actions) enchaîne :
- les tests backend, avec un service Redis ;
- les tests et le build du frontend ;
- les tests de bout en bout sur le jar et PostgreSQL 16 (RLS actif), parcours API puis navigateur ;
- la construction de l'image Docker ;
- la revue des dépendances des PR.

Elle est complétée par **CodeQL** (Java et TypeScript) et **Dependabot**.

## Mise en production

L'image Docker contient l'API et l'interface. Servez-la en **HTTPS** derrière un reverse proxy.

| Variable | Rôle |
|---|---|
| `SPRING_PROFILES_ACTIVE=prod` | Cookie `Secure`, aucune donnée de démo, logs JSON |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | PostgreSQL 13+ |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD`, `ADMIN_ORGANIZATION` | Premier propriétaire si la base est vide |
| `MFA_ENCRYPTION_KEY` | **Obligatoire** pour la 2FA : 32 octets en Base64 (`openssl rand -base64 32`), à conserver hors base |
| `FORWARD_HEADERS_STRATEGY=native` | Derrière un proxy de confiance : vraie IP client (anti-abus) et détection HTTPS |
| `SESSION_TIMEOUT` | Expiration après inactivité (défaut `8h`) |
| `METRICS_TOKEN` | Active `/actuator/prometheus` (en-tête `Authorization: Bearer …`) |
| `REDIS_ENABLED=true` + `SPRING_DATA_REDIS_HOST` | Plusieurs instances : compteurs anti-abus et temps réel partagés |
| `LOG_FORMAT` | Format des logs JSON (`ecs` par défaut, ou `logstash`, `gelf`) |

**Supervision**
- **Sondes** : `/actuator/health/liveness` et `/actuator/health/readiness`, publiques.
- **Métriques** : techniques (HTTP, JVM, pool de connexions) et métier (`ezproject_auth_login_total`, `ezproject_ratelimit_rejected_total`, `ezproject_auth_mfa_total`, `ezproject_import_total`, `ezproject_sse_connections`).
- **Alertes d'exemple** : `docs/monitoring/prometheus-alerts.yml`.

**Sauvegardes**

```bash
docker compose --profile backup run --rm backup                       # vers ./backups (rétention 14 jours)
PGHOST=… PGUSER=… PGPASSWORD=… PGDATABASE=ezproject scripts/backup.sh  # hors Docker, planifiable en cron
PGUSER=postgres scripts/restore.sh backups/<fichier>.dump base_vide    # restauration dans une base VIDE
```

- **Sauvegarde** : `pg_dump --enable-row-security`, archive vérifiée avant conservation.
- **Restauration** : elle demande un rôle superutilisateur ou `BYPASSRLS`. Le cycle complet (sauvegarde, restauration, redémarrage) a été vérifié sur PostgreSQL 16.

**Migrations**
- **V1 à V9** sont communes.
- **V3** est une migration Java (code projet unique par organisation).
- **V10** (Row-Level Security) ne s'applique qu'à PostgreSQL (`db/vendor/postgresql`).
- Une base existante monte de version sans perte de données.

## Architecture

```
backend/src/main/java/com/ezproject
├── organization/   locataire SaaS, offres, inscription, export / suppression
├── access/         rôles projet, permissions, matrice, AccessRules, ProjectAccessService, membres
├── account/        mon compte, mots de passe, RGPD (PersonalDataContributor), administration des comptes
├── mfa/            double authentification TOTP (RFC 6238), secret chiffré AES-GCM, codes de secours
├── audit/          journal d'audit paginé
├── team/           équipes
├── project/        projets, priorité, statuts + journal, modules
├── task/           Kanban ; task/collab : commentaires, sous-tâches, pièces jointes
├── todo/ retroplanning/ poker/   modules projet (poker : temps réel SSE + relais Redis)
├── time/           temps passés et budget
├── capacity/       capacité, absences, staffing
├── calendar/       jours ouvrés et fériés
├── risk/ report/ roadmap/   risques et décisions, météo et COPIL, roadmap
├── portfolio/      agrégation multi-projets
├── dataexchange/   import / export CSV
├── ratelimit/      anti-abus (mémoire ou Redis)
├── tenant/         Row-Level Security (organisation posée sur chaque connexion)
├── observability/  métriques, protection de /actuator/prometheus
└── security/       session, CSRF, CSP, état du compte, amorçage admin
```

## Sécurité

- **Sessions** : cookie `HttpOnly` / `SameSite=Lax` (`Secure` en prod), CSRF cookie-to-header, expiration configurable, nouvel identifiant de session après connexion, 2FA et changement de mot de passe.
- **Isolation multi-organisation à deux niveaux** :
  - contrôles applicatifs par objet (404 hors périmètre) ;
  - **Row-Level Security PostgreSQL** (25 tables), qui rattrape tout oubli de filtre.
- **Authentification** :
  - limitation anti-abus (IP, compte, inscription, import, API) ;
  - blocage de compte après échecs, y compris sur la 2FA ;
  - TOTP avec anti-rejeu et secret chiffré ;
  - comptes désactivables immédiatement.
- **Pièces jointes** :
  - liste blanche d'extensions (ni SVG, ni HTML, ni exécutable) ;
  - type MIME déduit de l'extension, avec **vérification de la signature binaire** ;
  - nom de fichier assaini, 5 Mo maximum ;
  - téléchargement toujours en `attachment`, avec `nosniff` et une CSP `sandbox`.
- **En-têtes** : CSP stricte sans script inline, `X-Frame-Options: DENY`, `Referrer-Policy`.
- **Données** :
  - entrées validées, requêtes paramétrées, erreurs sans détail interne ;
  - export CSV protégé contre l'injection de formules ;
  - mots de passe bcrypt (12 caractères minimum) ;
  - codes de secours hachés.

### Choix assumés et prochaines étapes

- **Emails et facturation** viendront dans un second temps : invitations par lien, réinitialisation du mot de passe par email, notifications, paiement de l'offre.
- **Antivirus** : les pièces jointes ne sont pas scannées. Brancher un service d'analyse (ClamAV…) avant d'accepter des fichiers de tiers non fiables.
- **Stockage des pièces jointes** : il se fait en base, ce qui est simple et sauvegardé avec le reste. Au-delà de quelques Go, passer sur un stockage objet (S3 compatible).
- **Politique de confidentialité** : le modèle (`/privacy`) est à compléter par l'éditeur du service.
