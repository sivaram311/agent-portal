#!/bin/bash
# PREPROD Machine Gateway — hostname + JWT (public-IP edges disabled)
# Requires PREPROD_ADMIN_PASSWORD in the environment; never hardcode credentials here.
set -euo pipefail

BASE='https://agent-portal-staging.delena.buzz'

if [ -z "${PREPROD_ADMIN_PASSWORD:-}" ]; then
  echo "Set PREPROD_ADMIN_PASSWORD before running this script." >&2
  exit 1
fi

RESPONSE=$(curl -sS -X POST "$BASE/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"admin\",\"password\":\"${PREPROD_ADMIN_PASSWORD}\",\"clientId\":\"agent-portal\"}")

TOKEN=$(printf '%s' "$RESPONSE" | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')
if [ -z "${TOKEN}" ]; then
  echo "LOGIN FAILED:"
  echo "$RESPONSE"
  exit 1
fi
echo "token_len=${#TOKEN}"

curl -sS -X POST "$BASE/api/machine" \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{}'
echo
