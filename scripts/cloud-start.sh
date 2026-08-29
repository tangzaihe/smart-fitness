#!/usr/bin/env bash
#
# Cloud Agent start phase for smart-fitness.
#
# Runs on every VM boot. It reconciles the per-boot runtime state that is not
# captured by the disk snapshot: the PostgreSQL cluster and the Redis daemon.
# Application schema migrations run automatically via Flyway when the backend
# boots, so they are intentionally not performed here.
#
# Must tolerate restarts and avoid launching duplicate daemons.
set -euo pipefail

REDIS_PASSWORD=smartfitness

echo "==> Starting PostgreSQL"
if ! pg_isready -h localhost -p 5432 -q 2>/dev/null; then
  sudo pg_ctlcluster 16 main start 2>/dev/null || true
fi
for _ in $(seq 1 30); do
  pg_isready -h localhost -p 5432 -q && break
  sleep 1
done
pg_isready -h localhost -p 5432 -q && echo "    PostgreSQL is ready"

echo "==> Starting Redis"
if ! redis-cli -a "${REDIS_PASSWORD}" ping >/dev/null 2>&1; then
  sudo redis-server /etc/redis/redis.conf --daemonize yes --requirepass "${REDIS_PASSWORD}"
  sleep 1
fi
redis-cli -a "${REDIS_PASSWORD}" ping >/dev/null 2>&1 && echo "    Redis is ready"

echo "==> Start phase complete"
