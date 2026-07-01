# Monitoring

Dieser PoC liefert zwei Arten von Observability-Daten: **Metriken** (Zahlen wie
Rate, Latenz, Fehler) und **Logs** (einzelne Request-Logzeilen). Beides wird in
**Grafana** zusammengeführt.

## Komponenten

| Komponente | Wo | Port | Aufgabe |
|---|---|---|---|
| Spring-Boot-App | Host | 8080 | REST-Schnittstelle (`/api/documents`); erzeugt Metriken (Actuator) und Logzeilen (Logdatei) |
| postgres | Container | 5432 | Datenbank für JSON-Dokumente und Audit-Einträge (keine Monitoring-Rolle) |
| prometheus | Container | 9090 | **Holt** (scrapet) die Metriken alle 5 s von der App ab und speichert sie als Zeitreihen |
| alloy | Container | 12345 | **Tailt** die App-Logdatei und **pusht** die Zeilen an Loki |
| loki | Container | 3100 | Speichert und indexiert die Logs (Labels wie `app`, `level`), macht sie abfragbar |
| grafana | Container | 3000 | Oberfläche; greift auf die Datasources Prometheus (Dashboard) und Loki (Explore) zu |

## Datenfluss

```
                                   ┌────────────┐   scrape   ┌────────────┐
   App  ──/actuator/prometheus──▶  │ prometheus │ ◀───pull── │            │
    │                              └────────────┘            │  grafana   │
    │                                                        │ (Dashboards│
    └──json-poc.log──▶ alloy ──push──▶ loki ────────────────▶│ + Explore) │
                                                             └────────────┘
   App ──▶ postgres   (fachliche Daten, kein Monitoring)
```

Kernunterschied: **Prometheus holt Metriken** (pull), **Alloy liefert Logs** an
Loki (push). Grafana führt beides nur zur Anzeige zusammen.

## Metriken

- App-seitig: `spring-boot-starter-actuator` + `micrometer-registry-prometheus`.
- Endpoint: <http://localhost:8080/actuator/prometheus>
- Zentrale Metrik: `http.server.requests` — pro Endpoint mit Tags `uri`,
  `method`, `status`, `outcome` (Latenz-Histogramme sind aktiviert).
- Konfiguration: `application.yml` (`management.*`), Scrape-Ziel in
  `monitoring/prometheus/prometheus.yml`.

## Logs

- App-seitig: strukturiertes Logging (Logstash-JSON) nach `logs/json-poc.log`
  (`logging.structured.format.file: logstash` in `application.yml`).
- Versand: `monitoring/alloy/config.alloy` tailt die Datei, parst die JSON-Zeile
  und hebt `level`/`logger` als Labels heraus (Label `app="json-poc"`).
- Abfrage: Grafana → Explore → Datasource **Loki**, z. B.:

  ```logql
  {app="json-poc"}                       # alle App-Logs
  {app="json-poc"} |= "Created document" # nur POST-Aufrufe
  {app="json-poc", level="WARN"}         # z. B. 404-Fälle
  ```

## Starten

```bash
docker compose up -d     # postgres, prometheus, alloy, loki, grafana
mvn spring-boot:run      # App auf dem Host (Port 8080) -> schreibt logs/json-poc.log
./loadtest.sh 200 20     # Traffic erzeugen
```

- Grafana: <http://localhost:3000> (admin/admin) → Dashboard „JSON-poc API Monitoring"
- Prometheus Targets: <http://localhost:9090/targets>
- Metrik-Rohdaten: <http://localhost:8080/actuator/prometheus>

## Annahmen & Stolperfallen

- **App läuft auf dem Host** (nicht im Container). Prometheus scrapet sie über
  `host.docker.internal:8080` (per `extra_hosts: host-gateway` gemappt). Wird die
  App später containerisiert, nur das Scrape-Ziel in `prometheus.yml` anpassen.
- **Log-Verzeichnis** `logs/` muss vor `docker compose up` existieren und dem
  Host-User gehören, sonst legt Docker es root-owned an und die App kann nicht
  schreiben.
- **Provisionierung wird nur beim Start gelesen.** Änderungen unter
  `monitoring/grafana/...` erst nach `docker restart grafana` aktiv (analog
  `docker restart prometheus` für `prometheus.yml`).