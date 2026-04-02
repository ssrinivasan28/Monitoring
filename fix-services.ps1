# Fix Loki XML
$lokiXml = @"
<service>
  <id>IPMonitoring-Loki_IOHANA</id>
  <name>IP Monitoring - Loki [IOHANA]</name>
  <description>Loki log aggregation server for IOHANA</description>
  <executable>C:\Program Files\Island Pacific\Operations Monitor\IOHANA\loki\loki.exe</executable>
  <arguments>-config.file="C:\Program Files\Island Pacific\Operations Monitor\IOHANA\loki\loki.yaml"</arguments>
  <workingdirectory>C:\Program Files\Island Pacific\Operations Monitor\IOHANA\loki</workingdirectory>
  <log mode="roll"></log>
  <onfailure action="restart" delay="10 sec"/>
  <onfailure action="restart" delay="20 sec"/>
  <onfailure action="restart" delay="30 sec"/>
</service>
"@
$lokiXml | Set-Content "C:\Program Files\Island Pacific\Operations Monitor\IOHANA\services\IPMonitoring-Loki.xml" -Encoding UTF8
Write-Host "Fixed Loki XML"

# Fix Prometheus XML
$promXml = @"
<service>
  <id>IPMonitoringPrometheus_IOHANA</id>
  <name>IP Monitoring - Prometheus [IOHANA]</name>
  <description>Prometheus metrics collection for IOHANA</description>
  <executable>C:\Program Files\Island Pacific\Operations Monitor\IOHANA\prometheus\prometheus.exe</executable>
  <arguments>--config.file="C:\Program Files\Island Pacific\Operations Monitor\IOHANA\prometheus\prometheus.yml" --storage.tsdb.path="C:\Program Files\Island Pacific\Operations Monitor\IOHANA\data\prometheus" --web.listen-address=:9090</arguments>
  <workingdirectory>C:\Program Files\Island Pacific\Operations Monitor\IOHANA\prometheus</workingdirectory>
  <log mode="roll"></log>
  <onfailure action="restart" delay="10 sec"/>
  <onfailure action="restart" delay="20 sec"/>
  <onfailure action="restart" delay="30 sec"/>
  <resetfailure>1 hour</resetfailure>
</service>
"@
$promXml | Set-Content "C:\Program Files\Island Pacific\Operations Monitor\IOHANA\services\IPMonitoringPrometheus.xml" -Encoding UTF8
Write-Host "Fixed Prometheus XML"

# Fix Grafana XML
$grafXml = @"
<service>
  <id>IPMonitoringGrafana_IOHANA</id>
  <name>IP Monitoring - Grafana [IOHANA]</name>
  <description>Grafana dashboards for IOHANA</description>
  <executable>C:\Program Files\Island Pacific\Operations Monitor\IOHANA\grafana\bin\grafana-server.exe</executable>
  <arguments>--homepath "C:\Program Files\Island Pacific\Operations Monitor\IOHANA\grafana" --config "C:\Program Files\Island Pacific\Operations Monitor\IOHANA\grafana\conf\custom.ini"</arguments>
  <workingdirectory>C:\Program Files\Island Pacific\Operations Monitor\IOHANA\grafana</workingdirectory>
  <log mode="roll"></log>
  <onfailure action="restart" delay="10 sec"/>
  <onfailure action="restart" delay="20 sec"/>
  <onfailure action="restart" delay="30 sec"/>
  <resetfailure>1 hour</resetfailure>
</service>
"@
$grafXml | Set-Content "C:\Program Files\Island Pacific\Operations Monitor\IOHANA\services\IPMonitoringGrafana.xml" -Encoding UTF8
Write-Host "Fixed Grafana XML"

Write-Host "`nRestarting services..."

# Restart services
& "C:\Program Files\Island Pacific\Operations Monitor\IOHANA\services\IPMonitoring-Loki.exe" restart
& "C:\Program Files\Island Pacific\Operations Monitor\IOHANA\services\IPMonitoringPrometheus.exe" restart  
& "C:\Program Files\Island Pacific\Operations Monitor\IOHANA\services\IPMonitoringGrafana.exe" restart

Start-Sleep -Seconds 2
Write-Host "`nService status:"
Get-Service -Name "*IPMonitoring*Loki*","*IPMonitoringPrometheus*","*IPMonitoringGrafana*" | Select-Object DisplayName, Status
