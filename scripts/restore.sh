#!/bin/sh
# Restauration d'une sauvegarde EZProject dans une base VIDE.
#   ./restore.sh <fichier.dump> <base_cible>
# Connexion : variables libpq (PGHOST, PGPORT, PGUSER, PGPASSWORD). Le rôle doit pouvoir contourner le RLS
# pendant le chargement (superutilisateur ou attribut BYPASSRLS), car COPY est refusé sous RLS forcé.
# Ensuite, démarrer l'application sur cette base : Flyway valide le schéma, aucune migration n'est rejouée.
set -eu

FILE="${1:?usage : restore.sh <fichier.dump> <base_cible>}"
TARGET="${2:?usage : restore.sh <fichier.dump> <base_cible>}"
OWNER="${APP_DB_OWNER:-}"

pg_restore --list "${FILE}" > /dev/null
pg_restore --exit-on-error --no-owner ${OWNER:+--role="${OWNER}"} --dbname="${TARGET}" "${FILE}"
echo "Restauration terminée dans ${TARGET}."
