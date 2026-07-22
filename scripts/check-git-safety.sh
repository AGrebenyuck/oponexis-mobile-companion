#!/usr/bin/env bash

set -euo pipefail

repository_root="$(git rev-parse --show-toplevel)"
cd "$repository_root"

forbidden_file_pattern='(^|/)(local\.properties|\.env($|\.)|.*\.(jks|keystore|p12|pfx|pem|key|apk|aab)|google-services\.json|secrets?\.(properties|json|ya?ml|env)|credentials\.(properties|json))$'

forbidden_files="$(git ls-files | grep -E "$forbidden_file_pattern" || true)"
if [[ -n "$forbidden_files" ]]; then
    printf '%s\n' "ERROR: sensitive or generated files are tracked:"
    printf '%s\n' "$forbidden_files"
    exit 1
fi

secret_pattern='-----BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY-----|(^|[^A-Za-z0-9])(AKIA[0-9A-Z]{16}|ghp_[A-Za-z0-9]{20,}|github_pat_[A-Za-z0-9_]{20,}|sk-[A-Za-z0-9_-]{20,})'
secret_files="$(git grep --cached -I -l -E -e "$secret_pattern" -- . 2>/dev/null || true)"
if [[ -n "$secret_files" ]]; then
    printf '%s\n' "ERROR: possible secret patterns found in tracked files:"
    printf '%s\n' "$secret_files"
    exit 1
fi

printf '%s\n' "Repository safety check passed."
