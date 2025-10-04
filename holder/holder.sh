#!/usr/bin/env bash
VERIFIER_URL="${VERIFIER_URL:-http://localhost:8080}"
KEY_FILE="${KEY_FILE:-key.pem}"   # pre-generated private key (P-256 / ES256)

# Cross-platform base64 without line wraps
b64() {
  if base64 --help 2>&1 | grep -qE '(-w|--wrap)'; then
    base64 -w0
  else
    base64 | tr -d '\n'
  fi
}

echo "[holder] using verifier: $VERIFIER_URL"
echo "[holder] using key file: $KEY_FILE"

# Ensure private key exists
if [[ ! -f "$KEY_FILE" ]]; then
  echo "[holder] key not found. generate once with:"
  echo "openssl ecparam -name prime256v1 -genkey -noout -out key.pem"
  exit 1
fi

# Get nonce (no -f; show HTTP errors if any)
echo "[holder] fetching nonce..."
NONCE_JSON="$(curl -sS "${VERIFIER_URL}/api/nonce/issue")"
echo "[holder] /api/nonce/issue response: $NONCE_JSON"

# Extract nonce (use jq if present; fallback to sed)
if command -v jq >/dev/null 2>&1; then
  NONCE="$(printf '%s' "$NONCE_JSON" | jq -r '.nonce // empty')"
else
  NONCE="$(printf '%s' "$NONCE_JSON" | sed -n 's/.*"nonce"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')"
fi

if [[ -z "${NONCE:-}" ]]; then
  echo "[holder] could not parse nonce from response above" >&2
  exit 1
fi
echo "[holder] nonce: $NONCE"