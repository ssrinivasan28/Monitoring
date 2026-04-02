# Test Log Monitoring Setup

## Folder Created
**C:\TestLogs**

## Instructions

1. **Copy log files** from SmartRetail to the test folder:
   ```powershell
   # Copy some recent catalina logs
   Copy-Item "C:\Program Files\Island Pacific\SmartRetail\SmartRetail_Trunk_NonSSO\server\logs\catalina.2026-01-*.log" -Destination "C:\TestLogs\"
   
   # Copy some localhost logs
   Copy-Item "C:\Program Files\Island Pacific\SmartRetail\SmartRetail_Trunk_NonSSO\server\logs\localhost.2026-01-*.log" -Destination "C:\TestLogs\"
   
   # Copy some stderr logs
   Copy-Item "C:\Program Files\Island Pacific\SmartRetail\SmartRetail_Trunk_NonSSO\server\logs\smartretail_trunk_nonsso-stderr.2026-01-*.log" -Destination "C:\TestLogs\"
   ```

2. **Start the monitor**:
   ```powershell
   java -jar "target\monitors\LogKeywordMonitor.jar" "logkeywordmonitor.properties"
   ```

3. **Check metrics**:
   ```powershell
   curl http://localhost:3023/metrics | Select-String "log_"
   ```

## What to Look For

The monitor will:
- Scan all `*.log` files in `C:\TestLogs`
- Detect keywords: ERROR, FATAL, Exception, OutOfMemoryError, etc.
- Send email alerts when keywords are found
- Show metrics like:
  - `log_file_lines_scanned_total{logfile="C:\\TestLogs\\catalina.2026-01-30.log"}`
  - `log_keyword_matches_total{logfile="...",keyword="ERROR"}`

## After Testing

Once confirmed working, update paths back to:
```properties
monitor.logfile.1.path=C:\\Program Files\\Island Pacific\\SmartRetail\\SmartRetail_Trunk_NonSSO\\server\\logs\\catalina.*.log
```
