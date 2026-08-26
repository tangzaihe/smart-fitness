#!/usr/bin/env bash
#
# Cloud Agent install phase for smart-fitness.
#
# Runs once after the repository is checked out (and, with environment builds,
# is baked into the base snapshot). It installs the system services the stack
# needs, provisions the local PostgreSQL role/database, generates the local
# .env files, builds the backend, and installs the mobile dependencies.
#
# Must be idempotent: it is safe to run repeatedly against a partially prepared
# or cached VM.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

DB_USER=smartfitness
DB_PASSWORD=smartfitness
DB_NAME=smartfitness
REDIS_PASSWORD=smartfitness

echo "==> Installing system packages (maven, postgresql, redis)"
if ! command -v mvn >/dev/null 2>&1 || ! command -v psql >/dev/null 2>&1 || ! command -v redis-server >/dev/null 2>&1; then
  sudo apt-get update -qq
  sudo DEBIAN_FRONTEND=noninteractive apt-get install -y -qq \
    maven postgresql postgresql-contrib redis-server
fi

echo "==> Generating backend .env (localhost services)"
if [ ! -f .env ]; then
  cp .env.example .env
fi
# Redisson always sends AUTH, so the local Redis must have a password.
sed -i "s|^REDIS_PASSWORD=.*|REDIS_PASSWORD=${REDIS_PASSWORD}|" .env

echo "==> Ensuring PostgreSQL cluster is running"
sudo pg_ctlcluster 16 main start 2>/dev/null || true
for _ in $(seq 1 30); do
  pg_isready -h localhost -p 5432 -q && break
  sleep 1
done

echo "==> Provisioning PostgreSQL role and database (idempotent)"
sudo -u postgres psql -tc "SELECT 1 FROM pg_roles WHERE rolname='${DB_USER}'" | grep -q 1 \
  || sudo -u postgres psql -c "CREATE ROLE ${DB_USER} LOGIN PASSWORD '${DB_PASSWORD}';"
sudo -u postgres psql -tc "SELECT 1 FROM pg_database WHERE datname='${DB_NAME}'" | grep -q 1 \
  || sudo -u postgres psql -c "CREATE DATABASE ${DB_NAME} OWNER ${DB_USER};"

echo "==> Building backend and installing modules to the local Maven repo"
# 'install' (not 'package') publishes the sibling module jars to ~/.m2 so the
# backend terminal can run 'mvn -pl smart-fitness-api spring-boot:run'.
# Testcontainers-based tests need Docker, which is not available, so tests are skipped.
mvn -q -pl smart-fitness-api -am install -DskipTests

echo "==> Installing mobile dependencies"
cd smart-fitness-mobile
if [ ! -f .env ]; then
  cp .env.example .env
fi
npm ci

echo "==> Install phase complete"
