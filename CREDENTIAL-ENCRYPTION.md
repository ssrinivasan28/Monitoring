# Credential Encryption (DPAPI)

All Island Pacific monitoring services support encrypted credentials in their `.properties` files. Values are encrypted with **Windows DPAPI (machine scope)** using `CredTool.jar` and stored as `DPAPI(base64...)`. Decryption happens automatically at service startup. Plaintext values continue to work — encryption is opt-in per field.

> **Important:** A `DPAPI(...)` value can ONLY be decrypted on the machine where it was encrypted. Always run CredTool on the machine where the monitor services run. When moving to a new machine, re-encrypt all values there.

---

## 1. How to Encrypt a Value

CredTool.jar is installed to the install root, e.g.
`C:\Program Files\Island Pacific\Operations Monitor\<ClientId>\CredTool.jar`
(dev build: `target\monitors\CredTool.jar`)

**UI (recommended):**
1. Double-click `CredTool.jar` (or `java -jar CredTool.jar`)
2. Paste the password into *Value to encrypt*
3. Click **Encrypt**, then **Copy to Clipboard**
4. Paste into the `.properties` file as the field value

**Command line:**
```powershell
java -jar CredTool.jar "myPassword"
# Output: DPAPI(AQAAANCMnd8B...)
```

**Example — before / after:**
```properties
# Before
ibmi.password=secret123

# After
ibmi.password=DPAPI(AQAAANCMnd8BFdERjHoAwE/Cl+sBAAAA...)
```

After editing a properties file, **restart the corresponding Windows service** (`IPMonitoring_<MonitorName>`).

---

## 2. What to Modify — email.properties (shared, all monitors)

Each monitor folder under `monitoring-services\<MonitorName>\` has an `email.properties`.

> **Installer note:** the install wizard encrypts `mail.smtp.password` and `mail.oauth2.client.secret` automatically (DPAPI, via the wizard's own encryption step) when generating `email.properties` — no manual step needed for these. On upgrade, existing plaintext values are also encrypted. Manual encryption with CredTool is only needed if you edit these files by hand later.

| Field | Encrypt when |
|---|---|
| `mail.smtp.password` | `mail.auth.method=SMTP` with authentication |
| `mail.oauth2.client.secret` | `mail.auth.method=OAUTH2` |
| `mail.password` | IBMJobQueStatusMonitor only (legacy key name) |

---

## 3. What to Modify — Monitor-Specific Properties

| Monitor | Properties file field(s) to encrypt |
|---|---|
| IBMIFSErrorMonitor | `ibmi.password` |
| IBMRealTimeIFSMonitor | `ibmi.password` |
| IBMJobQueCountMonitor | `ibmi.password` |
| IBMJobQueStatusMonitor | `ibmi.password` |
| IBMSubSystemMonitor | `ibmi.password` |
| IBMMatrixMonitor | `ibmi.password` |
| IBMQSYSOPRMonitor | `ibmi.password` |
| IBMFileMemberMonitor | `ibmi.password` |
| IBMNetworkEnabler | `password` |
| IBMUserProfileChecker | `password` (user config file) |
| ShareFileMonitor | `ftp.password` |
| WinMonitor | `windows.server.<host>.password` (one per remote host) |
| WinServiceMonitor | `monitor.server.<name>.password` (one per remote server) |
| ServiceScheduler | `job.<n>.password` (one per job, e.g. `job.1.password`) |
| ServerUpTimeMonitor | email fields only |
| WinFSErrorMonitor | email fields only |
| WinFSCardinalityMonitor | email fields only |
| LogKeywordMonitor | email fields only |
| FolderLogKeywordMonitor | email fields only |
| SSLCertMonitor | email fields only |

**Do NOT encrypt:** usernames, hostnames, ports, paths, thresholds — only the password/secret fields above are decrypted.

---

## 4. Per-Host Fields — Encrypt Each Separately

For multi-entry fields, run CredTool once per password:

```properties
# WinServiceMonitor
monitor.server.APPSRV01.password=DPAPI(AQAAANCMnd8B...)
monitor.server.APPSRV02.password=DPAPI(AQAAAKx3Pmd9...)

# ServiceScheduler
job.1.password=DPAPI(AQAAANCMnd8B...)
job.2.password=DPAPI(AQAAAJq8Rt2c...)
```

---

## 5. Troubleshooting

| Symptom | Cause / Fix |
|---|---|
| Service fails at startup: `Failed to decrypt DPAPI(...) credential` | Value encrypted on a different machine, or properties file copied from another install. Re-encrypt on this machine with CredTool. |
| Authentication fails after encrypting | Wrong plaintext was encrypted, or wrapper edited/truncated. Re-encrypt and paste the full `DPAPI(...)` string on one line. |
| Value not decrypted (sent as-is) | Wrapper must be exactly `DPAPI(` ... `)` — check spelling/case and the closing parenthesis. |

## 6. Implementation Notes (Developers)

- Decryption: `com.islandpacific.monitoring.common.CredentialProtector.resolve(String)` — wraps every credential `getProperty` read (51 sites, 32 files)
- Encryption: `CredentialProtector.protect(String)` — DPAPI machine scope (`CRYPTPROTECT_LOCAL_MACHINE`), via JNA `Crypt32Util`
- Tool: `com.islandpacific.monitoring.credtool.MainCredTool` (CLI) / `CredToolUI` (Swing), built as `target\monitors\CredTool.jar`
- New credential fields MUST be read through `CredentialProtector.resolve(...)`
- Tests: `CredentialProtectorTest` (round-trip, plaintext passthrough, corrupt-blob)
