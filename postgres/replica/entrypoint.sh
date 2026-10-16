#!/bin/sh
set -e
mkdir -p /var/lib/postgresql/data
chown -R postgres:postgres /var/lib/postgresql/data
if [ ! -s /var/lib/postgresql/data/PG_VERSION ]; then
  export PGPASSWORD=replicator
  until pg_isready -h postgres -U replicator; do sleep 1; done
  rm -rf /var/lib/postgresql/data/*
  gosu postgres pg_basebackup -h postgres -D /var/lib/postgresql/data -U replicator -v -P -R
fi
exec docker-entrypoint.sh postgres
