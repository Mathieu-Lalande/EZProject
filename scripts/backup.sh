#!/usr/bin/env bash
# Sauvegarde PostgreSQL d'EZProject (format custom, compressé), avec rétention.
#   PGHOST PGPORT PGUSER PGPASSWORD PGDATABASE : connexion (variables standard libpq)
#   BACKUP_DIR (défaut ./backups), BACKUP_RETENTION_DAYS (défaut 14)
# Exemple cron (tous les jours à 2h) : 0 2 * * * /opt/ezproject/scripts/backup.sh >> /var/log/ezproject-backup.log 2>&1
set -euo pipefail

BACKUP_DIR="${BACKUP_DIR:-./backups}"
RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-14}"
DB="${PGDATABASE:?PGDATABASE requis}"
STAMP="$(date -u +%Y%m%dT%H%M%SZ)"
FILE="${BACKUP_DIR}/ezproject-${DB}-${STAMP}.dump"

mkdir -p "${BACKUP_DIR}"
# --enable-row-security : les tables sont protégées par RLS (FORCE) ; sans organisation positionnée,
# les politiques laissent lire toutes les lignes, ce qui produit une sauvegarde complète.
pg_dump --format=custom --compress=9 --no-owner --enable-row-security --file="${FILE}.partial" "${DB}"
mv "${FILE}.partial" "${FILE}"
# Vérifie que l'archive est lisible avant de la conserver
pg_restore --list "${FILE}" > /dev/null
echo "$(date -u +%FT%TZ) sauvegarde OK : ${FILE} ($(du -h "${FILE}" | cut -f1))"

# Rétention : suppression des sauvegardes plus anciennes que RETENTION_DAYS jours (uniquement ce motif de nom)
find "${BACKUP_DIR}" -maxdepth 1 -type f -name "ezproject-${DB}-*.dump" -mtime "+${RETENTION_DAYS}" -print -delete
