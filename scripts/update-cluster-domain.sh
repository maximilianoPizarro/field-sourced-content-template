#!/usr/bin/env bash
set -euo pipefail

#
# Updates the cluster domain across the entire repo.
#
# Usage:
#   ./scripts/update-cluster-domain.sh <new-cluster-id>
#
# Example:
#   ./scripts/update-cluster-domain.sh k6jxj
#
# The script auto-detects the current cluster ID from the codebase,
# replaces every occurrence of cluster-OLD.dynamic.redhatworkshops.io
# with cluster-NEW.dynamic.redhatworkshops.io, commits, and pushes.
#

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SEARCH_DIRS=("${REPO_ROOT}/examples/helm" "${REPO_ROOT}/showroom")

NEW_ID="${1:-}"
if [[ -z "$NEW_ID" ]]; then
  echo "Usage: $0 <new-cluster-id>"
  echo "Example: $0 k6jxj"
  exit 1
fi

OLD_ID=$(grep -rhoP 'cluster-\K[a-z0-9]+(?=\.dynamic\.redhatworkshops\.io)' "${SEARCH_DIRS[@]}" | sort | uniq -c | sort -rn | head -1 | awk '{print $2}')

if [[ -z "$OLD_ID" ]]; then
  echo "ERROR: Could not detect current cluster ID"
  exit 1
fi

if [[ "$OLD_ID" == "$NEW_ID" ]]; then
  echo "Cluster ID is already '$NEW_ID'. Nothing to do."
  exit 0
fi

OLD_DOMAIN="cluster-${OLD_ID}.dynamic.redhatworkshops.io"
NEW_DOMAIN="cluster-${NEW_ID}.dynamic.redhatworkshops.io"

echo "Detected current domain: $OLD_DOMAIN"
echo "Replacing with:          $NEW_DOMAIN"
echo ""

CHANGED_FILES=$(grep -rl "$OLD_DOMAIN" "${SEARCH_DIRS[@]}" || true)

if [[ -z "$CHANGED_FILES" ]]; then
  echo "No files contain '$OLD_DOMAIN'. Nothing to do."
  exit 0
fi

COUNT=0
while IFS= read -r file; do
  sed -i "s/${OLD_DOMAIN}/${NEW_DOMAIN}/g" "$file"
  rel="${file#"${REPO_ROOT}/"}"
  echo "  updated: $rel"
  COUNT=$((COUNT + 1))
done <<< "$CHANGED_FILES"

echo ""
echo "$COUNT file(s) updated."
echo ""

cd "$REPO_ROOT"
git add -A
git diff --cached --stat

echo ""
read -rp "Commit and push? [Y/n] " answer
answer="${answer:-Y}"

if [[ "$answer" =~ ^[Yy]$ ]]; then
  git commit -m "chore: update cluster domain from ${OLD_ID} to ${NEW_ID}"
  git push
  echo ""
  echo "Done. ArgoCD will pick up the changes on next sync."
else
  echo "Changes staged but NOT committed. Run 'git commit' manually."
fi
