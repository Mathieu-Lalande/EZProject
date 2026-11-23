# EZProject

Outil SaaS de gestion de projets **multi-organisation** et **modulable**, conçu pour les équipes de **développement**, les missions de **conseil** (CIO office, PMO) et le **pilotage de portefeuille** (COO, office manager).

## Fonctionnalités

| Domaine | Ce qu'il fait |
|---|---|
| **Portefeuille** | KPI (projets ouverts, P1, en pause, à risque, retards), tri par **priorité P1→P4**, météo, statut et motif de pause, filtre par **équipe**, prochains jalons, charge par personne, export CSV. Le périmètre dépend du rôle : toute l'organisation pour COO/PMO, les projets accessibles pour les autres. |
| **Cycle de vie projet** | Statuts Planifié / En cours / **En pause** / Terminé / Annulé ; **motif obligatoire** pour la pause et l'annulation ; **journal des statuts** (qui, quand, pourquoi) ; priorité modifiable en un clic. |
| **Organisations (SaaS)** | Inscription self-service, isolation stricte des données entre organisations, offres **Free / Team / Enterprise** avec quotas (projets, utilisateurs). |
| **Équipes et rôles** | Plusieurs équipes par organisation ; chaque projet appartient à une équipe ; rôle d'équipe **Responsable / Contributeur / Lecteur**. |
| **Permissions fines** | Matrice **rôle × élément × action** (voir / ajouter / modifier / supprimer) sur le projet, les tâches, la checklist, le rétro-planning et le poker. Personnalisable par organisation (offres Team et Enterprise). |
| **Visibilité** | Par projet : *Organisation* (lecture pour tous), *Équipe*, *Confidentiel* (uniquement les membres ajoutés). Ajout de membres transverses et d'**invités** (clients, partenaires). |
| **Reprise des données** | Import CSV des **projets** (création ou mise à jour par code, équipes créées à la volée) et des **tâches**. Vérification à blanc puis import « tout ou rien ». Libellés français acceptés, séparateur `;` ou `,`. |
| **Modules projet** | Kanban (glisser-déposer), checklist, rétro-planning (calcul à rebours en jours ouvrés + Gantt), planning poker. Chaque module s'active ou se désactive par projet. |
| **Ergonomie** | Palette de commandes `Ctrl/⌘ + K`, thème clair/sombre, interface en français. |

## Modèle de droits

**Rôle dans l'organisation**

| Rôle | Portée |
|---|---|
| `OWNER` | Propriétaire du compte : offre, administration, accès complet. |
| `ADMIN` | Membres, équipes, matrice de permissions ; accès complet aux projets. |
| `MANAGER` | Vision COO / PMO : voit tous les projets et en est Responsable. |
| `MEMBER` | Accède aux projets de ses équipes selon son rôle d'équipe, plus les projets ouverts à l'organisation (en lecture). |
| `GUEST` | Externe : uniquement les projets où il est ajouté explicitement, au plus Contributeur. |

**Rôle sur un projet.** On prend le plus élevé entre le rôle hérité de l'équipe et le rôle attribué explicitement sur le projet. Les permissions découlent ensuite de la matrice de l'organisation.

**Codes d'erreur.**
- Un projet d'une autre organisation, ou non visible, répond **404** : on ne révèle pas son existence.
- Une action non autorisée sur un projet visible répond **403**.

**Où est appliquée la politique.** Toute la politique d'accès tient dans `AccessRules`, une fonction pure testée unitairement, appliquée par `ProjectAccessService` dans chaque service métier. L'interface masque les actions non autorisées grâce à `myPermissions`, mais c'est le backend qui fait autorité.

## Stack

- **Backend** : Java 21, Spring Boot 3.5.16 (Web, Data JPA, Validation, Security, Actuator), Flyway, Apache Commons CSV, H2 (dev) / PostgreSQL (prod), Maven.
- **Frontend** : React 19, TypeScript strict, Vite 7, Tailwind CSS 4, TanStack Query 5, React Router 7, dnd-kit, lucide-react.
- **Tests** : JUnit 5, AssertJ, Spring Boot Test + MockMvc, spring-security-test ; Vitest + Testing Library.

## Lancer en local

Trois façons, de la plus « dev » à la plus proche de la production. Les **comptes de démo** sont créés automatiquement au premier démarrage (profils `dev` et `local`).

### 1. Développement (rechargement à chaud)

Prérequis : JDK 21, Maven 3.9+, Node 22+.

```bash
cd backend && mvn spring-boot:run           # API :8080, profil dev : base H2 en mémoire (remise à zéro à chaque démarrage)
cd frontend && npm install && npm run dev   # UI http://localhost:5173 (proxy /api -> 8080)
```

### 2. Local « comme en prod » : PostgreSQL + jar unique

Les données sont **conservées** entre les redémarrages. L'API et l'interface sont servies par le même jar sur http://localhost:8080.

```bash
docker compose up -d db                                   # PostgreSQL 17 local
cd backend && mvn -Pfullstack package                     # construit l'interface et l'embarque dans le jar
java -jar target/ezproject-backend-0.1.0-SNAPSHOT.jar --spring.profiles.active=local
```

Sous Windows, ajoutez `-Dnpm=npm.cmd` à la commande Maven.

### 3. Tout en Docker

```bash
cp .env.example .env        # optionnel : changer les mots de passe locaux
docker compose up --build   # http://localhost:8080
```

Pour repartir de zéro : `docker compose down -v` (supprime aussi le volume de la base).

### Comptes de démonstration

Mot de passe : `demo1234`, modifiable via la variable `DEMO_PASSWORD`. Pour une base sans données de démo, mettez `SEED_DEMO=false` et définissez `ADMIN_EMAIL` / `ADMIN_PASSWORD`.

| Compte | Organisation | Rôle | Ce qu'il illustre |
|---|---|---|---|
| `camille@ezproject.local` | Horizon Conseil | OWNER (COO) | Portefeuille complet, administration, matrice |
| `lea@ezproject.local` | Horizon Conseil | MANAGER (PMO) | Pilotage de tous les projets |
| `hugo@ezproject.local` | Horizon Conseil | Responsable « Delivery Web » | Crée des projets dans son équipe, gère les accès |
| `sarah@ezproject.local` | Horizon Conseil | Contributrice « Delivery Web » | Crée et modifie des tâches, sans les supprimer |
| `thomas@ezproject.local` | Horizon Conseil | Responsable « Conseil SI » | Ajouté au projet confidentiel MANDA |
| `nina@ezproject.local` | Horizon Conseil | Contributrice « Conseil SI » | Ne voit pas les projets « Équipe » des autres |
| `julie@ezproject.local` | Horizon Conseil | GUEST (cliente) | Lecture seule sur « Portail client » uniquement |
| `bob@acme.local` | Acme Industries | OWNER | Seconde organisation, totalement isolée |

Vous pouvez aussi créer votre propre organisation depuis `/signup`.

## Importer vos données existantes

Menu **Organisation → Import / export** : télécharger le modèle, le remplir (ou exporter votre Excel en « CSV UTF-8 »), cliquer sur **Vérifier** puis **Importer**.

- **Projets** : `code*`, `nom*`, `type`, `statut`, `motif_statut`, `priorite`, `meteo`, `client`, `equipe`, `responsable` (email), `visibilite`, `debut`, `echeance`, `budget_jours`, `description`.
- **Tâches** : `projet*` (code), `titre*`, `statut`, `priorite`, `assigne` (email), `points`, `echeance`, `description`.
- Les dates sont acceptées au format `jj/mm/aaaa` ou `aaaa-mm-jj`. Les valeurs peuvent être des codes (`ACTIVE`, `P1`…) ou des libellés (« En cours », « Vert », « Conseil »…).
- Une erreur sur une seule ligne annule tout l'import, et le rapport indique la ligne et la cause.

## Tests

```bash
cd backend && mvn verify     # 53 tests : règles d'accès, isolation multi-organisation, matrice, statuts, quotas, import/export, anti-abus, sécurité
cd frontend && npm test      # 15 tests : client HTTP (CSRF), permissions, motif obligatoire, composants
cd frontend && npm run build # typecheck strict + build
```

## Mise en production

L'image Docker (`Dockerfile` à la racine) contient l'API et l'interface ; le profil `prod` ne crée **aucun compte de démo**.

| Variable | Rôle |
|---|---|
| `SPRING_PROFILES_ACTIVE=prod` | Profil de production : cookie de session `Secure`, aucune donnée de démo |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Base PostgreSQL (13+) |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD`, `ADMIN_ORGANIZATION` | Premier propriétaire, créé si la base est vide (offre Enterprise) |
| `FORWARD_HEADERS_STRATEGY=native` | À activer derrière un reverse proxy de confiance, pour la vraie IP client (anti-abus) et la détection HTTPS |

- **Déploiement** : servez l'application en **HTTPS** (cookie `Secure`) derrière un reverse proxy qui termine TLS.
- **Création des comptes** : ensuite, chaque organisation se crée via `/signup`, et les comptes sont ajoutés par les administrateurs (Organisation → Membres).
- **Migrations** :
  - `V2` passe une base existante en multi-organisation sans perte.
  - `V3` (migration Java) rend le code projet unique par organisation.
  - `V4` retire l'organisation vide d'une installation neuve.
  - La montée de version V1 → V4 a été vérifiée sur PostgreSQL 16.

## Architecture

```
backend/src/main/java/com/ezproject
├── organization/   locataire SaaS, offres et quotas, inscription
├── access/         rôles projet, permissions, matrice, AccessRules (politique pure), ProjectAccessService, membres
├── team/           équipes et appartenances
├── project/        projets, priorité, statut + journal, modules
├── task/ todo/ retroplanning/ poker/   modules projet (contrôlés par ProjectAccessService)
├── portfolio/      agrégation multi-projets selon le périmètre visible
├── dataexchange/   import CSV transactionnel, export CSV (anti-injection de formules)
├── security/       session + CSRF SPA, en-têtes (CSP), utilisateur courant, amorçage admin
├── ratelimit/      limitation anti-abus (seaux à jetons, blocage de compte)
└── seed/           données de démo (profil dev)
db/migration        V1, V2, V4 (SQL) et V3 (Java)
```

## Sécurité

- **Authentification** : session par cookie `HttpOnly` / `SameSite=Lax` (`Secure` en prod) et protection **CSRF** de type cookie-to-header.
- **Multi-organisation** : chaque lecture passe par l'organisation de l'utilisateur connecté. Les identifiants d'une autre organisation (projet, utilisateur, équipe) répondent 404. Des tests d'intégration le vérifient.
- **IDOR** : le contrôle se fait au niveau de l'objet (tâche, phase, tour de poker, todo) via son projet, pas seulement sur l'URL.
- **Import** :
  - taille limitée à 2 Mo et 5 000 lignes ;
  - les lignes passent par les mêmes services et autorisations que l'interface ;
  - les messages d'erreur techniques ne sont pas exposés.
- **Export** : protection contre l'**injection de formules CSV** (OWASP), avec les préfixes `= + - @` neutralisés.
- **Mots de passe** : `DelegatingPasswordEncoder` (bcrypt), 12 caractères minimum.
- **Limitation anti-abus** (réponse `429` + `Retry-After`, limites dans `app.rate-limit.*`) :

  | Protection | Limite par défaut |
  |---|---|
  | Tentatives de connexion par IP | 20 / 10 min |
  | Échecs de connexion par compte (blocage, même depuis d'autres IP) | 5 / 15 min |
  | Inscriptions par IP | 5 / heure |
  | Imports CSV par utilisateur | 20 / heure |
  | API par utilisateur connecté | 600 / min |
  | API anonyme par IP | 120 / min |

  Les échecs sont comptés aussi pour les emails inexistants, pour ne pas révéler quels comptes existent. Une connexion réussie remet le compteur à zéro.
- **En-têtes** : `Content-Security-Policy` stricte (aucun script inline), `X-Frame-Options: DENY`, `Referrer-Policy: same-origin`.

### Choix assumés et prochaines étapes

- **Une personne appartient à une seule organisation** : l'email est unique sur toute la plateforme.
- **Facturation et emails** sont prévus dans un second temps : changement d'offre réservé au propriétaire et sans paiement, création des comptes par un administrateur avec mot de passe initial (pas encore d'invitation par lien).
- Les compteurs anti-abus sont **en mémoire, par instance** : avec plusieurs instances, il faudra un stockage partagé (Redis).
- **Isolation multi-organisation applicative**, faite dans les services. Pour du SaaS à grande échelle, envisager en plus le *Row-Level Security* PostgreSQL.
- Planning poker en polling toutes les 2 s ; jours fériés non gérés dans le rétro-planning.
