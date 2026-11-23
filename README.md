# EZProject

Outil de gestion de projets **modulable**, pensé à la fois pour les équipes de **développement**, les missions de **conseil** (CIO office, PMO) et le **pilotage global** d'un portefeuille (COO, office manager).

| Module | Pour qui | Ce qu'il fait |
|---|---|---|
| **Portefeuille** | COO / PMO / managers | KPIs, météo (RAG) de chaque projet, points d'attention, prochains jalons, charge de l'équipe. Widgets affichables/masquables, vue tableau ou cartes. |
| **Mon espace** | Tout le monde | Mes tâches triées par urgence (en retard, cette semaine, plus tard) + todos personnels privés. |
| **Kanban** | Équipes projet | 5 colonnes, glisser-déposer, priorités, story points, échéances, filtre « Mes tâches », ajout rapide. |
| **Checklist** | Équipes projet | Todos partagés par projet. |
| **Rétro-planning** | Chefs de projet, consultants | Phases et jalons calculés **à rebours depuis l'échéance**, en jours ouvrés. Gabarits Dev / Conseil / Interne, Gantt, alerte si le démarrage calculé précède le début prévu. |
| **Planning poker** | Équipes agiles | Cartes Fibonacci, votes masqués jusqu'à la révélation, moyenne, suggestion, consensus, report automatique des points sur la tâche. |

Chaque projet active **uniquement les modules dont il a besoin** (onglet *Modules*). Les modules par défaut dépendent du type de projet. Une palette de commandes (`Ctrl/⌘ + K`) et un thème clair/sombre sont disponibles partout.

## Stack

- **Backend** : Java 21, Spring Boot 3.5.16 (Web, Data JPA, Validation, Security, Actuator), Flyway, H2 (dev) / PostgreSQL (prod), Maven.
- **Frontend** : React 19, TypeScript (strict), Vite 7, Tailwind CSS 4, TanStack Query 5, React Router 7, dnd-kit, lucide-react.
- **Tests** : JUnit 5, AssertJ, Spring Boot Test + MockMvc, spring-security-test ; Vitest + Testing Library.

## Démarrage rapide (dev)

Prérequis : JDK 21, Maven 3.9+, Node 22+.

```bash
# Terminal 1 : API sur http://localhost:8080 (profil dev, H2 en mémoire + données de démo)
cd backend
mvn spring-boot:run

# Terminal 2 : UI sur http://localhost:5173 (proxy /api -> 8080)
cd frontend
npm install
npm run dev
```

Comptes de démonstration (profil `dev` uniquement), mot de passe `demo1234` (surchargeable via `DEMO_PASSWORD`) :

| Email | Rôle | Profil |
|---|---|---|
| `camille@ezproject.local` | ADMIN | COO : portefeuille, gestion des utilisateurs |
| `lea@ezproject.local` | MANAGER | PMO : crée/pilote les projets |
| `hugo@`, `sarah@`, `thomas@ezproject.local` | MEMBER | Contributeurs |

## Tests

```bash
cd backend && mvn verify      # 17 tests : rétro-planning, poker, sécurité (401/403/CSRF/IDOR), validation
cd frontend && npm test       # 11 tests : client HTTP (CSRF, Problem Details), composants
cd frontend && npm run build  # typecheck strict + build
```

## Production

Profil `prod` : PostgreSQL et secrets **uniquement via variables d'environnement**.

```bash
export DB_URL=jdbc:postgresql://localhost:5432/ezproject DB_USERNAME=... DB_PASSWORD=...
export ADMIN_EMAIL=admin@exemple.fr ADMIN_PASSWORD='<12 caractères minimum>'   # crée le 1er admin si la base est vide
export APP_CORS_ALLOWED_ORIGINS=https://ezproject.exemple.fr
docker compose up -d                                  # PostgreSQL local (lit DB_USERNAME / DB_PASSWORD)
cd backend && mvn package && java -jar target/ezproject-backend-0.1.0-SNAPSHOT.jar --spring.profiles.active=prod
cd frontend && npm run build                          # dist/ à servir derrière le même domaine que /api
```

Servez `frontend/dist` et l'API **sous le même domaine** (reverse proxy : `/api` → Spring Boot) : les cookies de session et CSRF restent same-site, sans CORS à ouvrir.

## Architecture

```
backend/src/main/java/com/ezproject
├── common/          exceptions métier, handler global (RFC 9457 Problem Details), Clock injectable
├── security/        Spring Security (session + CSRF SPA), utilisateur courant, bootstrap admin
├── user/            utilisateurs et rôles (ADMIN, MANAGER, MEMBER)
├── project/         projets, type, statut, météo, modules activables
├── task/            Kanban : tâches, déplacement/renumérotation des colonnes
├── todo/            todos personnels (privés) et checklists projet
├── retroplanning/   phases/jalons + RetroPlanningCalculator (calcul pur, testé)
├── poker/           tours de planning poker (agrégat PokerRound -> PokerVote)
├── portfolio/       agrégation multi-projets pour la vue COO
└── seed/            données de démo (profil dev)

frontend/src
├── api/             client HTTP (CSRF), types, hooks React Query
├── components/      UI réutilisable (Button, Modal, Badge…) et layout (Sidebar, CommandPalette)
├── features/        board, todos, retro, poker, portfolio, projects
└── pages/           Portefeuille, Mon espace, Projets, Projet (onglets par module), Équipe
```

Package-by-feature, controllers sans logique métier, injection par constructeur, DTO en `record`, entités avec méthodes métier (ex. `PokerRound.vote()`, `Task.moveTo()`), schéma géré par Flyway (`ddl-auto: validate`).

## Sécurité

- **Authentification par session** (cookie `HttpOnly`, `SameSite=Lax`, `Secure` en prod) plutôt qu'un JWT stocké en `localStorage` : aucun jeton accessible au JavaScript.
- **CSRF** actif, pattern cookie-to-header (`XSRF-TOKEN` → `X-XSRF-TOKEN`) selon la documentation Spring Security pour les SPA.
- **Autorisations** : `@PreAuthorize` (création/modification de projet : MANAGER/ADMIN ; suppression et création d'utilisateurs : ADMIN ; portefeuille : MANAGER/ADMIN).
- **IDOR** : un todo personnel d'un autre utilisateur répond 404.
- **Poker** : les cartes des autres ne sont jamais envoyées par l'API avant la révélation.
- **Entrées** validées par Bean Validation ; requêtes JPA paramétrées ; erreurs sans stacktrace ni détail interne.
- Mots de passe hachés via `DelegatingPasswordEncoder` (bcrypt par défaut) ; 12 caractères minimum à la création.

### Limites connues / prochaines étapes

- Pas de **contrôle d'accès par projet** : tout utilisateur authentifié voit et modifie les tâches de tous les projets. À ajouter (table `project_member`) si des projets doivent être cloisonnés (clients différents, confidentialité).
- Pas de **limitation des tentatives de connexion** (brute force) : à ajouter (ex. Bucket4j ou au niveau du reverse proxy).
- Planning poker en **polling** (2 s) : suffisant pour une équipe, à passer en WebSocket/SSE pour plus d'interactivité.
- Le mot de passe de démo est une valeur par défaut du **profil dev uniquement** ; le profil prod n'a aucun secret par défaut.
- Pas encore de jours fériés dans le calcul des jours ouvrés du rétro-planning.
