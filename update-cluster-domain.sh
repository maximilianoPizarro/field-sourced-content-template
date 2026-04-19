#!/bin/bash
# =============================================================================
# update-cluster-domain.sh
#
# Updates the OpenShift cluster domain across ALL configuration files in this
# repository. Run this when migrating to a new cluster.
#
# Usage:
#   ./update-cluster-domain.sh <NEW_CLUSTER_DOMAIN>
#
# Example:
#   ./update-cluster-domain.sh apps.cluster-abc12.dynamic.redhatworkshops.io
#
# What it does:
#   1. Scans all YAML/YML files for the old domain and replaces with the new one
#   2. Updates the deployer.domain in the parent values.yaml
#   3. Commits and pushes changes (optional)
#   4. Prints a summary of files changed
#
# After running this script:
#   - Push changes to Git so ArgoCD syncs the new domain
#   - The showroom uses __CLUSTER_DOMAIN__ placeholder (no changes needed)
#   - Software template skeletons with OIDCPolicy issuerURL will be updated
# =============================================================================

set -euo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
HELM_DIR="${SCRIPT_DIR}/examples/helm"
SHOWROOM_DIR="${SCRIPT_DIR}/showroom"

# --- Validate input ---
if [ $# -lt 1 ]; then
    echo -e "${RED}Error: Missing required argument.${NC}"
    echo ""
    echo "Usage: $0 <NEW_CLUSTER_DOMAIN>"
    echo ""
    echo "Example:"
    echo "  $0 apps.cluster-abc12.dynamic.redhatworkshops.io"
    exit 1
fi

NEW_DOMAIN="$1"

# Validate domain format
if [[ ! "$NEW_DOMAIN" =~ ^apps\. ]]; then
    echo -e "${YELLOW}Warning: Domain doesn't start with 'apps.' — are you sure?${NC}"
    echo "  Expected format: apps.cluster-XXXXX.dynamic.redhatworkshops.io"
    read -p "  Continue anyway? [y/N] " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

# --- Detect current domain ---
echo -e "${CYAN}Detecting current cluster domain...${NC}"

CURRENT_DOMAIN=$(grep -r -oh 'apps\.cluster-[a-z0-9]*\.dynamic\.redhatworkshops\.io' \
    "${HELM_DIR}" "${SHOWROOM_DIR}" --include="*.yaml" --include="*.yml" 2>/dev/null \
    | sort | uniq -c | sort -rn | head -1 | awk '{print $2}')

if [ -z "$CURRENT_DOMAIN" ]; then
    echo -e "${YELLOW}No existing cluster domain found in YAML files.${NC}"
    echo "  This may be a fresh clone. Setting domain for the first time."
    CURRENT_DOMAIN="__PLACEHOLDER_DOMAIN__"
fi

if [ "$CURRENT_DOMAIN" = "$NEW_DOMAIN" ]; then
    echo -e "${GREEN}Domain is already set to ${NEW_DOMAIN}. Nothing to do.${NC}"
    exit 0
fi

echo -e "  Current: ${RED}${CURRENT_DOMAIN}${NC}"
echo -e "  New:     ${GREEN}${NEW_DOMAIN}${NC}"
echo ""

# --- Count affected files ---
echo -e "${CYAN}Scanning for files to update...${NC}"

if [ "$CURRENT_DOMAIN" != "__PLACEHOLDER_DOMAIN__" ]; then
    AFFECTED_FILES=$(grep -r -l "$CURRENT_DOMAIN" "${HELM_DIR}" "${SHOWROOM_DIR}" \
        --include="*.yaml" --include="*.yml" 2>/dev/null || true)
else
    AFFECTED_FILES=""
fi

if [ -z "$AFFECTED_FILES" ]; then
    echo -e "${YELLOW}No files contain the current domain.${NC}"
fi

FILE_COUNT=$(echo "$AFFECTED_FILES" | grep -c . 2>/dev/null || echo "0")
echo -e "  Found ${CYAN}${FILE_COUNT}${NC} files with domain references"
echo ""

# --- Perform replacement ---
echo -e "${CYAN}Replacing domain in all files...${NC}"

CHANGED=0

if [ "$CURRENT_DOMAIN" != "__PLACEHOLDER_DOMAIN__" ] && [ -n "$AFFECTED_FILES" ]; then
    while IFS= read -r file; do
        if [ -f "$file" ]; then
            sed -i "s|${CURRENT_DOMAIN}|${NEW_DOMAIN}|g" "$file"
            REL_PATH="${file#${SCRIPT_DIR}/}"
            echo -e "  ${GREEN}✓${NC} ${REL_PATH}"
            CHANGED=$((CHANGED + 1))
        fi
    done <<< "$AFFECTED_FILES"
fi

# --- Update deployer.domain in parent values.yaml ---
PARENT_VALUES="${HELM_DIR}/values.yaml"
if [ -f "$PARENT_VALUES" ]; then
    if grep -q 'domain: ""' "$PARENT_VALUES" 2>/dev/null; then
        sed -i "s|domain: \"\"|domain: \"${NEW_DOMAIN}\"|" "$PARENT_VALUES"
        echo -e "  ${GREEN}✓${NC} examples/helm/values.yaml (deployer.domain)"
        CHANGED=$((CHANGED + 1))
    elif grep -q "domain:" "$PARENT_VALUES" 2>/dev/null; then
        sed -i "s|domain:.*|domain: \"${NEW_DOMAIN}\"|" "$PARENT_VALUES"
        echo -e "  ${GREEN}✓${NC} examples/helm/values.yaml (deployer.domain updated)"
        CHANGED=$((CHANGED + 1))
    fi
fi

echo ""
echo -e "${GREEN}Done!${NC} Updated ${CHANGED} files."
echo ""

# --- Summary ---
echo -e "${CYAN}═══════════════════════════════════════════════════════════${NC}"
echo -e "${CYAN}  Cluster Domain Migration Summary${NC}"
echo -e "${CYAN}═══════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "  Old domain: ${RED}${CURRENT_DOMAIN}${NC}"
echo -e "  New domain: ${GREEN}${NEW_DOMAIN}${NC}"
echo -e "  Files updated: ${CYAN}${CHANGED}${NC}"
echo ""
echo -e "${CYAN}  Key URLs for the new cluster:${NC}"
echo -e "  Developer Hub:  https://backstage-developer-hub-developer-hub.${NEW_DOMAIN}"
echo -e "  Gitea:          https://gitea.${NEW_DOMAIN}"
echo -e "  ArgoCD:         https://openshift-gitops-server-openshift-gitops.${NEW_DOMAIN}"
echo -e "  DevSpaces:      https://devspaces.${NEW_DOMAIN}"
echo -e "  Keycloak:       https://rhbk.${NEW_DOMAIN}"
echo -e "  Showroom:       https://showroom-showroom.${NEW_DOMAIN}"
echo ""
echo -e "${CYAN}  Next steps:${NC}"
echo -e "  1. Review changes:  ${YELLOW}git diff${NC}"
echo -e "  2. Commit:          ${YELLOW}git add -A && git commit -m 'chore: migrate to ${NEW_DOMAIN}'${NC}"
echo -e "  3. Push:            ${YELLOW}git push${NC}"
echo -e "  4. ArgoCD will auto-sync the new domain to the cluster"
echo -e "  5. Restart showroom: ${YELLOW}oc rollout restart deployment/showroom -n showroom${NC}"
echo ""
echo -e "${CYAN}═══════════════════════════════════════════════════════════${NC}"
