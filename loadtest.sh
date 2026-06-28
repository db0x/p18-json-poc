#!/usr/bin/env bash
#
# Lasttest fuer den JSON-PoC.
# Erzeugt eindeutige JSON-Dokumente und schreibt sie parallel via curl
# an POST /api/documents. Jeder Datensatz ist garantiert verschieden
# (laufender Index geht in jedes variable Feld ein -> keine Redundanz).
#
# Nutzung:
#   ./loadtest.sh [ANZAHL] [PARALLELITAET]
#
# Beispiele:
#   ./loadtest.sh                # 1000 Dokumente, 20 parallel (Defaults)
#   ./loadtest.sh 5000 50        # 5000 Dokumente, 50 parallel
#
# Konfiguration auch per Env-Variablen: COUNT, CONCURRENCY, BASE_URL
#
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
ENDPOINT="${BASE_URL}/api/documents"

# Pools, aus denen je nach Index variiert wird. Der Index selbst geht
# zusaetzlich in jedes Feld ein, damit kein Datensatz dem anderen gleicht.
FIRST_NAMES=(Anna Bjoern Clara David Elena Frank Greta Hassan Ida Jonas Karin Lars Mira Noah Olga Pavel Quinn Rosa Sven Tanja Umut Vera Wei Xenia Yusuf Zoe)
LAST_NAMES=(Schmidt Mueller Weber Fischer Wagner Becker Hoffmann Schulz Koch Richter Klein Wolf Neumann Schwarz Braun Krueger Lange Werner Krause Lehmann)
CITIES=(Berlin Hamburg Muenchen Koeln Frankfurt Stuttgart Duesseldorf Leipzig Dresden Hannover Nuernberg Bremen Bonn Essen Karlsruhe)
DEPARTMENTS=(Engineering Sales Marketing Finance HR Legal Support Research Operations Design)
TAGS=(alpha beta gamma delta epsilon zeta eta theta iota kappa)

# Baut fuer einen Index ein eindeutiges JSON und sendet es per POST.
send_one() {
    local i="$1"

    local fn="${FIRST_NAMES[$((i % ${#FIRST_NAMES[@]}))]}"
    local ln="${LAST_NAMES[$((i % ${#LAST_NAMES[@]}))]}"
    local city="${CITIES[$((i % ${#CITIES[@]}))]}"
    local dept="${DEPARTMENTS[$((i % ${#DEPARTMENTS[@]}))]}"
    local tag1="${TAGS[$((i % ${#TAGS[@]}))]}"
    local tag2="${TAGS[$(((i / 3) % ${#TAGS[@]}))]}"

    # Index in jedem variablen Feld -> global eindeutiger Datensatz.
    local name="${fn} ${ln} #${i}"
    local email="${fn,,}.${ln,,}.${i}@example.com"
    local age=$(( 18 + (i % 62) ))
    local salary=$(( 30000 + (i * 7) % 120000 ))
    local active=true; (( i % 2 )) && active=false
    local score="$(( i % 1000 )).$(( i % 100 ))"
    local zip; zip="$(printf '%05d' $(( (i * 13) % 100000 )))"
    local ts; ts="$(date -u +%Y-%m-%dT%H:%M:%SZ)"

    local payload
    payload=$(cat <<JSON
{
  "name": "${name}",
  "email": "${email}",
  "externalId": "ext-${i}",
  "city": "${city}",
  "department": "${dept}",
  "age": ${age},
  "salary": ${salary},
  "active": ${active},
  "score": ${score},
  "tags": ["${tag1}", "${tag2}", "n-${i}"],
  "address": { "street": "Musterstrasse ${i}", "zip": "${zip}", "city": "${city}" },
  "createdAt": "${ts}",
  "seq": ${i}
}
JSON
)

    local http_code
    http_code=$(curl -s -o /dev/null -w '%{http_code}' \
        -X POST "${ENDPOINT}" \
        -H 'Content-Type: application/json' \
        --data "${payload}") || http_code="000"

    if [ "${http_code}" != "200" ]; then
        echo "FEHLER bei #${i}: HTTP ${http_code}" >&2
        return 1
    fi
}

# Worker-Einsprung: xargs ruft 'bash loadtest.sh --worker N' auf. Dieser
# Prozess sourct das Script von oben neu (Arrays + Funktion sind also da),
# erledigt genau einen Request und beendet sich, bevor die Orchestrierung
# unten erreicht wird.
if [ "${1:-}" = "--worker" ]; then
    send_one "$2"
    exit $?
fi

# ---- Orchestrierung (nur im Haupt-Aufruf) --------------------------------
COUNT="${1:-${COUNT:-1000}}"
CONCURRENCY="${2:-${CONCURRENCY:-20}}"

echo "Lasttest gegen ${ENDPOINT}"
echo "Anzahl: ${COUNT}  |  Parallelitaet: ${CONCURRENCY}"

# Vorab-Check: App erreichbar?
probe=$(curl -s -o /dev/null -w '%{http_code}' --max-time 3 \
    "${BASE_URL}/api/documents/search?field=name&value=__probe__" || echo "000")
if [ "${probe}" != "200" ]; then
    echo "WARN: ${BASE_URL} antwortet nicht (HTTP ${probe}) - laeuft die App?" >&2
fi

start=$(date +%s.%N)

# Index 1..COUNT parallel abarbeiten; jeder Slot startet einen Worker.
# '|| true', damit ein einzelner fehlgeschlagener Request den Lauf nicht abbricht.
seq 1 "${COUNT}" | xargs -P "${CONCURRENCY}" -I {} bash "$0" --worker {} || true

end=$(date +%s.%N)
elapsed=$(awk "BEGIN { printf \"%.2f\", ${end} - ${start} }")
rate=$(awk "BEGIN { printf \"%.1f\", ${COUNT} / (${end} - ${start}) }")

echo "----------------------------------------"
echo "Fertig: ${COUNT} Dokumente in ${elapsed}s  (~${rate} req/s)"
