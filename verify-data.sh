#!/bin/bash
LOGIN=$(curl -s -X POST http://127.0.0.1:8081/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -H 'X-Tenant-ID: default' \
  -d '{"email":"demo@crm.com","password":"Demo@2026!","tenantId":"default"}')
TOKEN=$(echo "$LOGIN" | python3 -c 'import sys,json; print(json.load(sys.stdin)["data"]["accessToken"])')
echo "Login OK"

declare -A PORTS=([accounts]=8083 [contacts]=8084 [leads]=8082 [opportunities]=8085 [activities]=8086 [cases]=8092 [campaigns]=8093)

for ep in accounts contacts leads opportunities activities cases campaigns; do
  PORT=${PORTS[$ep]}
  RESULT=$(curl -s "http://127.0.0.1:${PORT}/api/v1/${ep}?size=1" \
    -H "Authorization: Bearer ${TOKEN}" \
    -H 'X-Tenant-ID: default')
  COUNT=$(echo "$RESULT" | python3 -c 'import sys,json; d=json.load(sys.stdin); print(d.get("data",{}).get("totalElements","?"))' 2>/dev/null || echo '?')
  printf "%-15s %s records\n" "$ep" "$COUNT"
done
