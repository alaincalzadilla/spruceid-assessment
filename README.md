# SpruceID Assessment — Proof of Private Key Control (Nonce + Signature)

This repo demonstrates a minimal challenge–response flow to **prove control of a private key**:

- **Verifier (Web API)** — issues a **nonce** (single-use, TTL) and verifies a signature over that nonce using a **pre-installed public key**.
- **Holder (Bash script)** — owns the **private key**, requests nonce, signs it with **ES256 (ECDSA P-256)** via OpenSSL, and calls the verifier.

This aligns with common self-custodial auth flows: **server issues challenge → holder signs → server verifies & consumes challenge** (preventing replay).

---

## Table of Contents

1. [Architecture](#architecture)
2. [Prerequisites & Installation](#prerequisites--installation)
3. [Generate Keys (one-time)](#generate-keys-one-time)
4. [Configure the Verifier](#configure-the-verifier)
5. [Run Everything](#run-everything)
6. [API Reference](#api-reference)
7. [How It Works (cryptography)](#how-it-works-cryptography)
8. [Troubleshooting](#troubleshooting)
9. [Security Notes & Extensions](#security-notes--extensions)
10. [Appendix — Manual test](#appendix--manual-test-no-script)

---

## Architecture

```
Holder (Bash)                       Verifier (Web API - WebFlux)
---------------                     --------------------------------------
1) GET /nonce  ------------------->  Issues single-use nonce (TTL=5m)

2) Sign exact ASCII "nonce"
   with ES256 via OpenSSL

3) POST /verify { nonce, sig } --->  Validates signature with stored public key,
                                     consumes nonce (prevents replay)
<----------------------------------  200 { "verified": true }
```
---

## Prerequisites & Installation

### Required

- **Java 21+** (builds/run fine on JDK 25)
- **Maven 3.9+**
- **OpenSSL** (signing with ES256)
- **curl**
- *(Optional)* **jq** (just for pretty JSON parsing in shell)

---

## Generate Keys (one-time)

This step is not required, I use a **single pre-generated keypair** for the demo:

```bash
# From repo root (or anywhere)
openssl ecparam -name prime256v1 -genkey -noout -out key.pem        # private key (holder)
openssl pkey -in key.pem -pubout -out pub.pem                       # public key (verifier)
```

- Keep **`key.pem`** with the **holder**.
- Copy **`pub.pem`** into the verifier so it can verify signatures.

---

## Configure the Verifier

If generating a new pair of keys, place the new public key at:

```
verifier/src/main/resources/pub.pem
```

**Or** set a path via env/property:

```bash
# Example: use an absolute path
export VERIFIER_PUBKEY_PATH="/absolute/path/to/pub.pem"
# or in application.properties: verifier.pubkey.path: /absolute/path/to/pub.pem
```

---

## Run Everything

Open **two terminals**:

### Terminal A — Verifier

```bash
cd spruceid-assessment/verifier
mvn spring-boot:run
# Starts on http://localhost:8080
```

### Terminal B — Holder

```bash
cd spruceid-assessment/holder
chmod +x holder.sh
# Ensure the private key is present next to the script:
#   ls -l key.pem
./holder.sh
```

**Expected output (abridged):**
```
[holder] using verifier: http://localhost:8080               
[holder] using key file: key.pem
[holder] fetching nonce...
[holder] /api/nonce/issue response: {"nonce":"LugnsYgkxpz50O8a7AvD4cxARgBOM3QFhsUXqPz_daA","issuedAt":1759557863441,"expiresAt":1759558163441}
[holder] nonce: LugnsYgkxpz50O8a7AvD4cxARgBOM3QFhsUXqPz_daA
[holder] signing nonce with ES256...
[holder] signature (base64, first 60 chars): MEQCIBDm3A/88ObhqGLhdKOoofdim77IA7pEnNWHO12pNUK2AiBpZqZJU/XC...
[holder] request body: {
  "nonce": "LugnsYgkxpz50O8a7AvD4cxARgBOM3QFhsUXqPz_daA",
  "sigBase64Url": "MEQCIBDm3A/88ObhqGLhdKOoofdim77IA7pEnNWHO12pNUK2AiBpZqZJU/XCpT1PGqd5oQVUfunkampAEzsvNo6nFapzFw=="
}
[holder] calling /api/verify...
[holder] /api/verify response: {"verified":true}
```

---

## API Reference

### `GET /api/nonce/issue`

Issues a **single-use** nonce. (No input required for the single-key variant.)

**Response**
```json
{
  "nonce": "SuOOneVi-3ZJzSQDUllFPGYorfz_XJeit-BoyrloETA",
  "issuedAt": 1759556112728,
  "expiresAt": 1759556412728
}
```

### `POST /api/verify`

The **holder** signs the exact ASCII `nonce` using ES256 and sends:

**Request**
```json
{
  "nonce": "8fC5gRgMvYAj9aWz67AzlmHZ2ZkFrUQjbt9yNrEkbt0",
  "sigBase64Url": "MEQCID3bR...==" // base64 or base64url; server accepts both
}
```

**Response**
```json
{ "verified": true }
```

**Error responses**
- `401`
```json
{
  "verified": false,
  "message": "The nonce is invalid or has already been consumed."
}
```
```json
{
  "verified": false,
  "message": "Signature verification failed."
}
```
- `400`
```json
{
  "verified": false,
  "message": "Request processing failed. Invalid request: ..."
}
```
---

## How It Works (cryptography)

- **Keypair:** EC P-256 (**prime256v1**) as the signing identity.
- **Challenge:** Verifier issues a random 32-byte **nonce** (base64url), keeps it server-side with TTL & single-use.
- **Proof:** Holder signs the **exact nonce string** with `SHA256withECDSA` (ES256).
- **Verification:** Server reconstructs the same nonce bytes and verifies the ECDSA **DER** signature with the stored public key; if valid, the nonce is **consumed**.

This prevents **replays** (a captured signature can’t be reused because the nonce is one-time).

---

## Troubleshooting

### Verifier won’t start / fails to load key
- Console shows something like `Public key not found: ...`  
  → Ensure `verifier/src/main/resources/pub.pem` exists **or** set `VERIFIER_PUBKEY_PATH=/abs/path/pub.pem`.

### Holder prints nothing or exits early
- The provided `holder.sh` prints each step and traps errors. If needed, run:
  ```bash
  bash -x holder.sh
  ```
- Ensure the verifier is running:
  ```bash
  curl -v http://localhost:8080/nonce
  ```

---

## Security Notes & Extensions

- **CSRF/Auth disabled:** The API is public for the exercise. In real life, gate access (e.g., IP allowlist, mTLS, or auth).
- **Rate limiting:** Add a simple rate limit on `/api/nonce/issue` and `/api/verify` to resist abuse.
- **Short TTL:** Keep nonce TTL tight (e.g., 1–5 minutes).
- **Multi-holder variant:** Support multiple public keys by mapping `kid → public key`. Then change `GET /api/nonce/issue?kid=...` and include `kid` in the verify request.
- **Aud/Origin binding (optional):** Include `aud` or intended origin in the message if moving beyond this minimal Bash design.

---

---

## Appendix — Manual test (no script)

Reproducing what the holder does manually:

```bash
# 1) Fetch nonce
NONCE="$(curl -s 'http://localhost:8080/api/nonce/issue' | jq -r .nonce)"
echo "NONCE: $NONCE"

# 2) Sign the exact nonce (DER ECDSA)
printf "%s" "$NONCE" | openssl dgst -sha256 -sign key.pem > sig.der

# 3) Encode signature (base64)
if base64 --help 2>&1 | grep -qE '(-w|--wrap)'; then
  SIG="$(base64 -w0 < sig.der)"
else
  SIG="$(base64 < sig.der | tr -d '
')"
fi
echo "SIG: $SIG"

# 4) Verify via API
curl -s -X POST http://localhost:8080/api/verify   -H 'Content-Type: application/json' -d '{"nonce":"'$NONCE'","sigBase64Url":"'$SIG'"}'
# -> {"verified":true}
```
