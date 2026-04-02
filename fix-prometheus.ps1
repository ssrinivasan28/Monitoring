# Clean up prometheus.yml - remove old duplicate job entries
$promFile = "C:\Program Files\Island Pacific\Operations Monitor\IOHANA\prometheus\prometheus.yml"

$cleanYaml = @'
# my global config
global:
  scrape_interval: 15s # Set the scrape interval to every 15 seconds. Default is every 1 minute.
  evaluation_interval: 15s # Evaluate rules every 15 seconds. The default is every 1 minute.
  # scrape_timeout is set to the global default (10s).

# Alertmanager configuration
alerting:
  alertmanagers:
    - static_configs:
        - targets:
          # - alertmanager:9093

# Load rules once and periodically evaluate them according to the global 'evaluation_interval'.
rule_files:
  # - "first_rules.yml"
  # - "second_rules.yml"

# A scrape configuration containing exactly one endpoint to scrape:
# Here it's Prometheus itself.
scrape_configs:
  # The job name is added as a label `job=<job_name>` to any timeseries scraped from this config.
  - job_name: "prometheus"

    # metrics_path defaults to '/metrics'
    # scheme defaults to 'http'.

    static_configs:
      - targets: ["localhost:9090"]
        labels:
          app: "prometheus"

# --- IP Monitor Jobs Below ---

  - job_name: "ibm-ifs-error-monitor-iohana"
    static_configs:
      - targets: ["localhost:3010"]
        labels:
          app: "ibm-ifs-error-monitor-iohana"
          client: "IOHANA"
          instance_id: "IOHANA"

  - job_name: "ibm-realtime-ifs-monitor-iohana"
    static_configs:
      - targets: ["localhost:3011"]
        labels:
          app: "ibm-realtime-ifs-monitor-iohana"
          client: "IOHANA"
          instance_id: "IOHANA"

  - job_name: "ibm-jobqueue-count-monitor-iohana"
    static_configs:
      - targets: ["localhost:3012"]
        labels:
          app: "ibm-jobqueue-count-monitor-iohana"
          client: "IOHANA"
          instance_id: "IOHANA"

  - job_name: "ibm-jobqueue-status-monitor-iohana"
    static_configs:
      - targets: ["localhost:3013"]
        labels:
          app: "ibm-jobqueue-status-monitor-iohana"
          client: "IOHANA"
          instance_id: "IOHANA"

  - job_name: "server-uptime-monitor-iohana"
    static_configs:
      - targets: ["localhost:3014"]
        labels:
          app: "server-uptime-monitor-iohana"
          client: "IOHANA"
          instance_id: "IOHANA"

  - job_name: "ibm-subsystem-monitor-iohana"
    static_configs:
      - targets: ["localhost:3015"]
        labels:
          app: "ibm-subsystem-monitor-iohana"
          client: "IOHANA"
          instance_id: "IOHANA"

  - job_name: "ibm-system-matrix-iohana"
    static_configs:
      - targets: ["localhost:3016"]
        labels:
          app: "ibm-system-matrix-iohana"
          client: "IOHANA"
          instance_id: "IOHANA"

  - job_name: "ibm-user-profile-checker-iohana"
    static_configs:
      - targets: ["localhost:3017"]
        labels:
          app: "ibm-user-profile-checker-iohana"
          client: "IOHANA"
          instance_id: "IOHANA"

  - job_name: "network-enabler-iohana"
    static_configs:
      - targets: ["localhost:3018"]
        labels:
          app: "network-enabler-iohana"
          client: "IOHANA"
          instance_id: "IOHANA"

  - job_name: "qsysopr-monitor-iohana"
    static_configs:
      - targets: ["localhost:3019"]
        labels:
          app: "qsysopr-monitor-iohana"
          client: "IOHANA"
          instance_id: "IOHANA"
'@

$cleanYaml | Set-Content $promFile -Encoding UTF8
Write-Host "Cleaned prometheus.yml - removed duplicate old-style job entries"

# Restart Prometheus to reload configuration
Write-Host "Restarting Prometheus..."
& "C:\Program Files\Island Pacific\Operations Monitor\IOHANA\services\IPMonitoringPrometheus.exe" restart
Start-Sleep -Seconds 2

Write-Host "Done! Old metrics with duplicate job names will age out based on Prometheus retention settings."
