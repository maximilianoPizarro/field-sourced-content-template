#!/usr/bin/env bash
#
# argocd-sync.sh — Quick ArgoCD operations via REST API
#
# Usage:
#   ./scripts/argocd-sync.sh list                          # list all apps
#   ./scripts/argocd-sync.sh status <app>                  # detailed status
#   ./scripts/argocd-sync.sh sync <app> [--prune]          # sync app
#   ./scripts/argocd-sync.sh refresh <app>                 # hard refresh
#   ./scripts/argocd-sync.sh sync-all                      # sync all OutOfSync apps
#   ./scripts/argocd-sync.sh diagnose <app>                # show issues
#

set -euo pipefail

ARGOCD_HOST="${ARGOCD_HOST:-openshift-gitops-server-openshift-gitops.$(oc whoami --show-console 2>/dev/null | sed 's|https://console-openshift-console\.||')}"
ARGOCD_NS="${ARGOCD_NS:-openshift-gitops}"

_token=""
_get_token() {
  if [ -n "$_token" ]; then return; fi
  local pass
  pass=$(oc get secret openshift-gitops-cluster -n "$ARGOCD_NS" -o jsonpath='{.data.admin\.password}' | base64 -d)
  _token=$(curl -sk -H "Content-Type: application/json" \
    "https://$ARGOCD_HOST/api/v1/session" \
    -d "{\"username\":\"admin\",\"password\":\"$pass\"}" \
    | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")
}

_api() {
  local method="${1}" path="${2}" body="${3:-}"
  _get_token
  if [ -n "$body" ]; then
    curl -sk -X "$method" -H "Authorization: Bearer $_token" -H "Content-Type: application/json" \
      "https://$ARGOCD_HOST${path}" -d "$body"
  else
    curl -sk -X "$method" -H "Authorization: Bearer $_token" \
      "https://$ARGOCD_HOST${path}"
  fi
}

cmd_list() {
  _api GET "/api/v1/applications" | python3 -c "
import sys,json
data = json.load(sys.stdin)
print(f'{\"APP\":<55} {\"SYNC\":<14} {\"HEALTH\":<14} REVISION')
print('-'*100)
for a in sorted(data.get('items',[]), key=lambda x: x['metadata']['name']):
    name = a['metadata']['name']
    sync = a.get('status',{}).get('sync',{}).get('status','')
    health = a.get('status',{}).get('health',{}).get('status','')
    rev = a.get('status',{}).get('sync',{}).get('revision','')[:8]
    print(f'{name:<55} {sync:<14} {health:<14} {rev}')
"
}

cmd_status() {
  local app="$1"
  _api GET "/api/v1/applications/$app" | python3 -c "
import sys,json
a = json.load(sys.stdin)
s = a.get('status',{})
sync = s.get('sync',{})
health = s.get('health',{})
op = s.get('operationState',{})
src = a.get('spec',{}).get('source',{})
print(f'Application:  {a[\"metadata\"][\"name\"]}')
print(f'Repo:         {src.get(\"repoURL\",\"\")}')
print(f'Path:         {src.get(\"path\",\"\")}')
print(f'Target Rev:   {src.get(\"targetRevision\",\"\")}')
print(f'Sync Status:  {sync.get(\"status\",\"\")}')
print(f'Sync Rev:     {sync.get(\"revision\",\"\")[:12]}')
print(f'Health:       {health.get(\"status\",\"\")}')
if op:
    print(f'Last Op:      {op.get(\"phase\",\"\")} — {op.get(\"message\",\"\")[:200]}')
    print(f'Started:      {op.get(\"startedAt\",\"\")}')
    print(f'Finished:     {op.get(\"finishedAt\",\"\")}')
resources = s.get('resources',[])
oos = [r for r in resources if r.get('status')=='OutOfSync']
deg = [r for r in resources if r.get('health',{}).get('status') in ('Degraded','Missing')]
if oos:
    print(f'\nOutOfSync ({len(oos)}):')
    for r in oos[:10]:
        print(f'  {r[\"kind\"]}/{r[\"name\"]} ns={r.get(\"namespace\",\"\")}')
if deg:
    print(f'\nDegraded/Missing ({len(deg)}):')
    for r in deg[:10]:
        msg = r.get('health',{}).get('message','')[:150]
        print(f'  {r[\"kind\"]}/{r[\"name\"]} — {msg}')
conds = s.get('conditions',[])
if conds:
    print('\nConditions:')
    for c in conds:
        print(f'  [{c.get(\"type\",\"\")}] {c.get(\"message\",\"\")[:200]}')
"
}

cmd_sync() {
  local app="$1" prune="${2:-false}"
  [ "$prune" = "--prune" ] && prune="true"
  echo "Syncing $app (prune=$prune)..."
  _api POST "/api/v1/applications/$app/sync" "{\"prune\":$prune}" | python3 -c "
import sys,json
r = json.load(sys.stdin)
phase = r.get('status',{}).get('operationState',{}).get('phase','Initiated')
print(f'Sync triggered — phase: {phase}')
" 2>/dev/null || echo "Sync request sent"
}

cmd_refresh() {
  local app="$1"
  echo "Hard refreshing $app..."
  _api GET "/api/v1/applications/$app?refresh=hard" | python3 -c "
import sys,json
a = json.load(sys.stdin)
sync = a.get('status',{}).get('sync',{})
print(f'Refreshed — sync: {sync.get(\"status\",\"\")} rev: {sync.get(\"revision\",\"\")[:12]}')
"
}

cmd_sync_all() {
  echo "Finding OutOfSync applications..."
  local apps
  apps=$(_api GET "/api/v1/applications" | python3 -c "
import sys,json
data = json.load(sys.stdin)
for a in data.get('items',[]):
    if a.get('status',{}).get('sync',{}).get('status')=='OutOfSync':
        print(a['metadata']['name'])
")
  if [ -z "$apps" ]; then
    echo "All applications are in sync!"
    return
  fi
  echo "$apps" | while read -r app; do
    cmd_sync "$app"
    sleep 2
  done
}

cmd_diagnose() {
  local app="$1"
  _api GET "/api/v1/applications/$app" | python3 -c "
import sys,json
a = json.load(sys.stdin)
s = a.get('status',{})
sync = s.get('sync',{})
health = s.get('health',{})
op = s.get('operationState',{})
resources = s.get('resources',[])
issues = []
recs = []
if sync.get('status')=='OutOfSync':
    oos = [r for r in resources if r.get('status')=='OutOfSync']
    issues.append(f'OutOfSync with {len(oos)} resource(s) diverged')
    recs.append('Run: ./scripts/argocd-sync.sh sync $app')
if health.get('status') in ('Degraded','Missing'):
    deg = [r for r in resources if r.get('health',{}).get('status') in ('Degraded','Missing')]
    for r in deg[:5]:
        msg = r.get('health',{}).get('message','')[:200]
        issues.append(f'{r[\"kind\"]}/{r[\"name\"]} is {r[\"health\"][\"status\"]}: {msg}')
    recs.append('Check pod logs for degraded resources')
if op.get('phase')=='Failed':
    issues.append(f'Last sync FAILED: {op.get(\"message\",\"\")[:300]}')
    recs.append('Fix the error, then: ./scripts/argocd-sync.sh refresh $app && ./scripts/argocd-sync.sh sync $app')
for c in s.get('conditions',[]):
    issues.append(f'[{c.get(\"type\",\"\")}] {c.get(\"message\",\"\")[:200]}')
if not issues:
    print('No issues detected. Application is healthy and in sync.')
else:
    print('Issues:')
    for i in issues: print(f'  - {i}')
    print('\nRecommendations:')
    for r in recs: print(f'  - {r}')
"
}

case "${1:-help}" in
  list)      cmd_list ;;
  status)    cmd_status "${2:?app name required}" ;;
  sync)      cmd_sync "${2:?app name required}" "${3:-}" ;;
  refresh)   cmd_refresh "${2:?app name required}" ;;
  sync-all)  cmd_sync_all ;;
  diagnose)  cmd_diagnose "${2:?app name required}" ;;
  *)
    echo "Usage: $0 {list|status|sync|refresh|sync-all|diagnose} [app] [--prune]"
    exit 1 ;;
esac
