#!/usr/bin/env bash
# Copies the current Supabase (prod) data into the local Docker Postgres
# container used by docker-compose.yml. Run this after `docker compose up -d
# --build` on any machine (dev box or home server) that has network access to
# Supabase. Requires DB_PROD_URL to be set in .env.
#
# WARNING: this replaces everything in the local container's public schema.

set -euo pipefail
cd "$(dirname "$0")/.."

# Read values directly instead of `source .env` — .env contains raw
# connection strings with unquoted `&`, which a shell would otherwise
# interpret as a background operator and silently truncate the value.
env_value() {
  grep -m1 -E "^$1=" .env | cut -d'=' -f2-
}

DB_PROD_URL=$(env_value DB_PROD_URL)
DB_DEV_USERNAME=$(env_value DB_DEV_USERNAME)

if [ -z "${DB_PROD_URL:-}" ]; then
  echo "DB_PROD_URL not set in .env" >&2
  exit 1
fi

url="${DB_PROD_URL#jdbc:}"
host=$(echo "$url" | sed -E 's#postgresql://([^:/]+).*#\1#')
port=$(echo "$url" | sed -E 's#postgresql://[^:/]+:([0-9]+).*#\1#')
dbname=$(echo "$url" | sed -E 's#.*/([^/?]+)\?.*#\1#')
prod_user=$(echo "$url" | sed -E 's#.*user=([^&]+).*#\1#')
prod_pass=$(echo "$url" | sed -E 's#.*password=([^&]+).*#\1#')

local_user="${DB_DEV_USERNAME:-postgres}"
dump_file=$(mktemp)
trap 'rm -f "$dump_file"' EXIT

echo "Dumping public schema from Supabase ($host:$port/$dbname) ..."
# Supabase runs Postgres 17, which emits a `transaction_timeout` SET that
# our local Postgres 16 container doesn't recognize (harmless, but noisy) —
# strip it rather than upgrading the local image just for this.
docker run --rm -e PGPASSWORD="$prod_pass" postgres:17 \
  pg_dump -h "$host" -p "$port" -U "$prod_user" -d "$dbname" \
  --schema=public --no-owner --no-privileges \
  | grep -v '^SET transaction_timeout' > "$dump_file"

echo "Resetting local schema in messmate-postgres ..."
# Only drop — the dump's own CREATE SCHEMA statement recreates it, so we
# don't create it here (avoids a harmless "already exists" error below).
docker exec messmate-postgres psql -U "$local_user" -d mess-mate \
  -c "DROP SCHEMA public CASCADE;"

echo "Restoring dump into local container ..."
docker exec -i messmate-postgres psql -U "$local_user" -d mess-mate < "$dump_file"

echo "Restarting app container ..."
docker compose restart app

echo "Done."
