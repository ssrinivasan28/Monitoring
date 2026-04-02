#define AppName "Island Pacific Operation Monitor Suite"
#define AppVersion "1.0.0"
#define AppPublisher "Island Pacific Retail Systems"
; Registry keys - base for shared data, Clients subkey for per-client data
#define AppRegKey "Software\IslandPacific\Monitoring"
#define AppRegKeyClients "Software\IslandPacific\Monitoring\Clients"

[Setup]
; CRITICAL FOR MULTI-CLIENT: AppId MUST be dynamic to create separate Add/Remove Programs entries
; The uninstaller will read ClientInstanceId from {app}\.client_instance_id file in InitializeUninstall
; This ensures the AppId resolves correctly even during uninstall
AppId={{IP-MonitorSuite-{code:GetClientInstanceId}}
UsePreviousLanguage=no
AppName={#AppName}
AppVersion={#AppVersion}
AppPublisher={#AppPublisher}
; Version info for Programs and Features
VersionInfoVersion=1.0.0.0
VersionInfoCompany=Island Pacific Retail Systems
VersionInfoProductName={#AppName}
VersionInfoDescription=Island Pacific Operation Monitor Suite Installer
UninstallDisplayName={#AppName} ({code:GetClientInstanceId})

ArchitecturesInstallIn64BitMode=x64compatible
; CRITICAL: Include ClientInstanceId in directory to prevent file overwrites between clients
DefaultDirName={commonpf}\Island Pacific\Operations Monitor\{code:GetClientInstanceId}
; Allow user to change directory
DisableDirPage=no
UsePreviousAppDir=yes
DisableProgramGroupPage=yes
PrivilegesRequired=admin
PrivilegesRequiredOverridesAllowed=commandline dialog
UsePreviousPrivileges=no
WizardStyle=modern
WizardSizePercent=125
WizardImageFile=installer\resources\wizard_modern.bmp
WizardSmallImageFile=installer\resources\wizard_small_modern.bmp
SetupIconFile=installer\resources\ip-monitoring.ico
; License agreement
LicenseFile=installer\resources\license.txt
; Show version in title bar
AppVerName={#AppName} v{#AppVersion}
; Copyright and support info
AppCopyright=Copyright © 2025 Island Pacific Retail Systems
AppSupportURL=https://www.islandpacific.com/
AppUpdatesURL=https://www.islandpacific.com/

Compression=lzma
SolidCompression=yes

OutputDir=.\installer\output
OutputBaseFilename=IslandPacificMonitoringSetup

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Messages]
WelcomeLabel1=Welcome to the [name] Setup Wizard
WelcomeLabel2=This wizard will guide you through the installation of the Island Pacific Monitoring Suite.%n%nThis comprehensive monitoring solution includes:%n%n• Prometheus - Time-series metrics collection%n• Grafana - Beautiful dashboards and visualization%n• IBM i Monitors - IFS Error, Job Queue, Real-Time monitoring%n• Server UpTime Monitor - System availability tracking%n• Email Alerts - SMTP and OAuth2 (Microsoft 365) support%n%nClick Next to continue, or Cancel to exit Setup.
FinishedHeadingLabel=You're All Set!
FinishedLabel=The Island Pacific Monitoring Suite is now installed and running.%n%n✓ Prometheus is collecting metrics%n✓ Grafana dashboards are ready%n✓ Selected monitors are active%n✓ Email alerts are configured%n%nAccess your dashboards at http://localhost:{code:GetGrafanaPort}%n%nClick Finish to close this wizard.
FinishedLabelNoIcons=The Island Pacific Monitoring Suite is now installed.%n%n✓ Selected monitors are active%n✓ Email alerts are configured%n%nCheck your application logs for monitoring activity.
ClickFinish=Installation is complete. Click Finish to close this wizard.

[Dirs]
Name: "{app}\services"
Name: "{app}\logs"; Flags: uninsneveruninstall
Name: "{app}\data"; Flags: uninsneveruninstall; Check: IsMonitoringServerRole
Name: "{app}\data\prometheus"; Flags: uninsneveruninstall; Check: IsMonitoringServerRole
Name: "{app}\data\loki"; Flags: uninsneveruninstall; Check: IsMonitoringServerRole
Name: "{app}\data\loki\chunks"; Flags: uninsneveruninstall; Check: IsMonitoringServerRole
Name: "{app}\data\loki\rules"; Flags: uninsneveruninstall; Check: IsMonitoringServerRole
Name: "{app}\loki"; Check: IsMonitoringServerRole
Name: "{app}\promtail"; Check: IsLogAgentRole
Name: "{app}\grafana\conf"; Check: IsMonitoringServerRole
Name: "{app}\grafana\data"; Flags: uninsneveruninstall; Check: IsMonitoringServerRole
Name: "{app}\monitoring-services"

[Files]
; Core services - Prometheus & Grafana
Source: "installer\resources\WinSW.exe"; DestDir: "{app}\services"; DestName: "IPMonitoringPrometheus.exe"; Flags: ignoreversion; Check: IsMonitoringServerRole
Source: "installer\resources\WinSW.exe"; DestDir: "{app}\services"; DestName: "IPMonitoringGrafana.exe"; Flags: ignoreversion; Check: IsMonitoringServerRole
; Prometheus - exclude data directory to preserve metrics during upgrade
Source: "installer\resources\prometheus\*"; DestDir: "{app}\prometheus"; Excludes: "data\*"; Flags: recursesubdirs createallsubdirs ignoreversion; Check: IsMonitoringServerRole
; Grafana - exclude data directory to preserve dashboards/users during upgrade
Source: "installer\resources\grafana\*"; DestDir: "{app}\grafana"; Excludes: "data\*"; Flags: recursesubdirs createallsubdirs ignoreversion; Check: IsMonitoringServerRole

; Loki - log aggregation server (only for Monitoring Server role)
Source: "installer\resources\loki\loki.exe"; DestDir: "{app}\loki"; Flags: ignoreversion; Check: IsMonitoringServerRole
Source: "installer\resources\loki\loki.yaml.template"; DestDir: "{app}\loki"; Flags: ignoreversion; Check: IsMonitoringServerRole
Source: "installer\resources\WinSW.exe"; DestDir: "{app}\services"; DestName: "IPMonitoring-Loki.exe"; Flags: ignoreversion; Check: IsMonitoringServerRole

; Promtail - log agent (only for Application Server role)
Source: "installer\resources\promtail\promtail.exe"; DestDir: "{app}\promtail"; Flags: ignoreversion; Check: IsLogAgentRole
Source: "installer\resources\promtail\promtail.yaml.template"; DestDir: "{app}\promtail"; Flags: ignoreversion; Check: IsLogAgentRole
Source: "installer\resources\WinSW.exe"; DestDir: "{app}\services"; DestName: "IPMonitoring-LogAgent.exe"; Flags: ignoreversion; Check: IsLogAgentRole

; Monitoring Services - WinSW wrappers (only for actual services in resources)
Source: "installer\resources\WinSW.exe"; DestDir: "{app}\services"; DestName: "IPMonitoring_IBMIFSErrorMonitor.exe"; Flags: ignoreversion
Source: "installer\resources\WinSW.exe"; DestDir: "{app}\services"; DestName: "IPMonitoring_IBMRealTimeIFSMonitor.exe"; Flags: ignoreversion
Source: "installer\resources\WinSW.exe"; DestDir: "{app}\services"; DestName: "IPMonitoring_IBMJobQueCountMonitor.exe"; Flags: ignoreversion
Source: "installer\resources\WinSW.exe"; DestDir: "{app}\services"; DestName: "IPMonitoring_IBMJobQueStatusMonitor.exe"; Flags: ignoreversion
Source: "installer\resources\WinSW.exe"; DestDir: "{app}\services"; DestName: "IPMonitoring_ServerUpTimeMonitor.exe"; Flags: ignoreversion
Source: "installer\resources\WinSW.exe"; DestDir: "{app}\services"; DestName: "IPMonitoring_IBMSubSystemMonitoring.exe"; Flags: ignoreversion
Source: "installer\resources\WinSW.exe"; DestDir: "{app}\services"; DestName: "IPMonitoring_IBMSystemMatrix.exe"; Flags: ignoreversion
Source: "installer\resources\WinSW.exe"; DestDir: "{app}\services"; DestName: "IPMonitoring_IBMUserProfileChecker.exe"; Flags: ignoreversion
Source: "installer\resources\WinSW.exe"; DestDir: "{app}\services"; DestName: "IPMonitoring_NetWorkEnabler.exe"; Flags: ignoreversion
Source: "installer\resources\WinSW.exe"; DestDir: "{app}\services"; DestName: "IPMonitoring_QSYSOPRMonitoring.exe"; Flags: ignoreversion
Source: "installer\resources\WinSW.exe"; DestDir: "{app}\services"; DestName: "IPMonitoring_WinFSErrorMonitor.exe"; Flags: ignoreversion
Source: "installer\resources\WinSW.exe"; DestDir: "{app}\services"; DestName: "IPMonitoring_WinFSCardinalityMonitor.exe"; Flags: ignoreversion

; Monitoring Services - JAR files only (always updated during upgrade)
; Properties files and logs are handled separately to preserve user data
Source: "installer\resources\monitoring-services\IBMIFSErrorMonitor\*.jar"; DestDir: "{app}\monitoring-services\IBMIFSErrorMonitor"; Flags: ignoreversion; Check: IsIFSErrorMonitorSelected
Source: "installer\resources\monitoring-services\IBMRealTimeIFSMonitor\*.jar"; DestDir: "{app}\monitoring-services\IBMRealTimeIFSMonitor"; Flags: ignoreversion; Check: IsRealTimeIFSMonitorSelected
Source: "installer\resources\monitoring-services\IBMJobQueCountMonitor\*.jar"; DestDir: "{app}\monitoring-services\IBMJobQueCountMonitor"; Flags: ignoreversion; Check: IsJobQueCountMonitorSelected
Source: "installer\resources\monitoring-services\IBMJobQueStatusMonitor\*.jar"; DestDir: "{app}\monitoring-services\IBMJobQueStatusMonitor"; Flags: ignoreversion; Check: IsJobQueStatusMonitorSelected
Source: "installer\resources\monitoring-services\ServerUpTimeMonitor\*.jar"; DestDir: "{app}\monitoring-services\ServerUpTimeMonitor"; Flags: ignoreversion; Check: IsServerUpTimeMonitorSelected
Source: "installer\resources\monitoring-services\IBMSubSystemMonitoring\*.jar"; DestDir: "{app}\monitoring-services\IBMSubSystemMonitoring"; Flags: ignoreversion; Check: IsSubSystemMonitorSelected
Source: "installer\resources\monitoring-services\IBMSystemMatrix\*.jar"; DestDir: "{app}\monitoring-services\IBMSystemMatrix"; Flags: ignoreversion; Check: IsSystemMatrixSelected
Source: "installer\resources\monitoring-services\IBMUserProfileChecker\*.jar"; DestDir: "{app}\monitoring-services\IBMUserProfileChecker"; Flags: ignoreversion; Check: IsUserProfileCheckerSelected
Source: "installer\resources\monitoring-services\NetWorkEnabler\*.jar"; DestDir: "{app}\monitoring-services\NetWorkEnabler"; Flags: ignoreversion; Check: IsNetWorkEnablerSelected
Source: "installer\resources\monitoring-services\QSYSOPRMonitoring\*.jar"; DestDir: "{app}\monitoring-services\QSYSOPRMonitoring"; Flags: ignoreversion; Check: IsQSYSOPRMonitorSelected
Source: "installer\resources\monitoring-services\WinFSErrorMonitor\*.jar"; DestDir: "{app}\monitoring-services\WinFSErrorMonitor"; Flags: ignoreversion; Check: IsWinFSErrorMonitorSelected
Source: "installer\resources\monitoring-services\WinFSCardinalityMonitor\*.jar"; DestDir: "{app}\monitoring-services\WinFSCardinalityMonitor"; Flags: ignoreversion; Check: IsWinFSCardinalityMonitorSelected

; Properties files - only copy if they don't exist (preserve user customizations on upgrade)
; email.properties is excluded because it is dynamically generated
Source: "installer\resources\monitoring-services\IBMIFSErrorMonitor\*.properties"; DestDir: "{app}\monitoring-services\IBMIFSErrorMonitor"; Excludes: "email.properties"; Flags: onlyifdoesntexist; Check: IsIFSErrorMonitorSelected
Source: "installer\resources\monitoring-services\IBMRealTimeIFSMonitor\*.properties"; DestDir: "{app}\monitoring-services\IBMRealTimeIFSMonitor"; Excludes: "email.properties"; Flags: onlyifdoesntexist; Check: IsRealTimeIFSMonitorSelected
Source: "installer\resources\monitoring-services\IBMJobQueCountMonitor\*.properties"; DestDir: "{app}\monitoring-services\IBMJobQueCountMonitor"; Excludes: "email.properties"; Flags: onlyifdoesntexist; Check: IsJobQueCountMonitorSelected
Source: "installer\resources\monitoring-services\IBMJobQueStatusMonitor\*.properties"; DestDir: "{app}\monitoring-services\IBMJobQueStatusMonitor"; Excludes: "email.properties"; Flags: onlyifdoesntexist; Check: IsJobQueStatusMonitorSelected
Source: "installer\resources\monitoring-services\ServerUpTimeMonitor\*.properties"; DestDir: "{app}\monitoring-services\ServerUpTimeMonitor"; Excludes: "email.properties"; Flags: onlyifdoesntexist; Check: IsServerUpTimeMonitorSelected
Source: "installer\resources\monitoring-services\IBMSubSystemMonitoring\*.properties"; DestDir: "{app}\monitoring-services\IBMSubSystemMonitoring"; Excludes: "email.properties"; Flags: onlyifdoesntexist; Check: IsSubSystemMonitorSelected
Source: "installer\resources\monitoring-services\IBMSystemMatrix\*.properties"; DestDir: "{app}\monitoring-services\IBMSystemMatrix"; Excludes: "email.properties"; Flags: onlyifdoesntexist; Check: IsSystemMatrixSelected
Source: "installer\resources\monitoring-services\IBMUserProfileChecker\*.properties"; DestDir: "{app}\monitoring-services\IBMUserProfileChecker"; Excludes: "email.properties"; Flags: onlyifdoesntexist; Check: IsUserProfileCheckerSelected
Source: "installer\resources\monitoring-services\NetWorkEnabler\*.properties"; DestDir: "{app}\monitoring-services\NetWorkEnabler"; Excludes: "email.properties"; Flags: onlyifdoesntexist; Check: IsNetWorkEnablerSelected
Source: "installer\resources\monitoring-services\QSYSOPRMonitoring\*.properties"; DestDir: "{app}\monitoring-services\QSYSOPRMonitoring"; Excludes: "email.properties"; Flags: onlyifdoesntexist; Check: IsQSYSOPRMonitorSelected
Source: "installer\resources\monitoring-services\WinFSErrorMonitor\*.properties"; DestDir: "{app}\monitoring-services\WinFSErrorMonitor"; Excludes: "email.properties"; Flags: onlyifdoesntexist; Check: IsWinFSErrorMonitorSelected
Source: "installer\resources\monitoring-services\WinFSCardinalityMonitor\*.properties"; DestDir: "{app}\monitoring-services\WinFSCardinalityMonitor"; Excludes: "email.properties"; Flags: onlyifdoesntexist; Check: IsWinFSCardinalityMonitorSelected

[Registry]
; Core settings
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "InstallPath"; ValueData: "{app}"; Flags: uninsdeletekey
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "InstallRole"; ValueData: "{code:GetInstallRole}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "PrometheusPort"; ValueData: "{code:GetPromPort}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "GrafanaPort"; ValueData: "{code:GetGrafPort}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "GrafanaAdminPassword"; ValueData: "{code:GetGrafPwd}"; Flags: uninsdeletevalue
; Service ports (saved for subsequent installs)
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "IFSErrorMonitorPort"; ValueData: "{code:GetIFSErrorMonitorPort}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "RealTimeIFSMonitorPort"; ValueData: "{code:GetRealTimeIFSMonitorPort}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "JobQueCountMonitorPort"; ValueData: "{code:GetJobQueCountMonitorPort}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "JobQueStatusMonitorPort"; ValueData: "{code:GetJobQueStatusMonitorPort}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "ServerUpTimeMonitorPort"; ValueData: "{code:GetServerUpTimeMonitorPort}"; Flags: uninsdeletevalue
; IBM i settings (saved for subsequent installs)
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "IBMiServer"; ValueData: "{code:GetIBMiServer}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "IBMiUser"; ValueData: "{code:GetIBMiUser}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "IBMiPassword"; ValueData: "{code:GetIBMiPassword}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "ClientName"; ValueData: "{code:GetClientName}"; Flags: uninsdeletevalue
; Email settings (saved for subsequent installs)
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "EmailAuthMethod"; ValueData: "{code:GetEmailAuthMethod}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "SmtpHost"; ValueData: "{code:GetSmtpHost}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "SmtpPort"; ValueData: "{code:GetSmtpPort}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "SmtpUsername"; ValueData: "{code:GetSmtpUsername}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "SmtpPassword"; ValueData: "{code:GetSmtpPassword}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "SmtpAuth"; ValueData: "{code:GetSmtpAuth}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "SmtpStartTls"; ValueData: "{code:GetSmtpStartTls}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "OAuthTenant"; ValueData: "{code:GetOAuthTenant}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "OAuthClientId"; ValueData: "{code:GetOAuthClientId}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "OAuthClientSecret"; ValueData: "{code:GetOAuthClientSecret}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "OAuthTokenUrl"; ValueData: "{code:GetOAuthTokenUrl}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "OAuthMailUrl"; ValueData: "{code:GetOAuthMailUrl}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "OAuthScope"; ValueData: "{code:GetOAuthScope}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "OAuthFromUser"; ValueData: "{code:GetOAuthFromUser}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "EmailFrom"; ValueData: "{code:GetEmailFrom}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "EmailTo"; ValueData: "{code:GetEmailTo}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "EmailBcc"; ValueData: "{code:GetEmailBcc}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "ClientNameEmail"; ValueData: "{code:GetClientNameEmail}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "EmailImportance"; ValueData: "{code:GetEmailImportance}"; Flags: uninsdeletevalue
; Multi-client installation support
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "ClientInstanceId"; ValueData: "{code:GetClientInstanceId}"; Flags: uninsdeletevalue
; Installed services tracking (saved to remember which services were installed)
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "InstalledPrometheus"; ValueData: "{code:GetInstalledPrometheus}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "InstalledGrafana"; ValueData: "{code:GetInstalledGrafana}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "InstalledIFSErrorMonitor"; ValueData: "{code:GetInstalledIFSErrorMonitor}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "InstalledRealTimeIFSMonitor"; ValueData: "{code:GetInstalledRealTimeIFSMonitor}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "InstalledJobQueCountMonitor"; ValueData: "{code:GetInstalledJobQueCountMonitor}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "InstalledJobQueStatusMonitor"; ValueData: "{code:GetInstalledJobQueStatusMonitor}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "InstalledServerUpTimeMonitor"; ValueData: "{code:GetInstalledServerUpTimeMonitor}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "InstalledSubSystemMonitor"; ValueData: "{code:GetInstalledSubSystemMonitor}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "InstalledSystemMatrix"; ValueData: "{code:GetInstalledSystemMatrix}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "InstalledUserProfileChecker"; ValueData: "{code:GetInstalledUserProfileChecker}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "InstalledNetWorkEnabler"; ValueData: "{code:GetInstalledNetWorkEnabler}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "InstalledQSYSOPRMonitor"; ValueData: "{code:GetInstalledQSYSOPRMonitor}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "InstalledWinFSErrorMonitor"; ValueData: "{code:GetInstalledWinFSErrorMonitor}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "InstalledWinFSCardinalityMonitor"; ValueData: "{code:GetInstalledWinFSCardinalityMonitor}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "SubSystemMonitorPort"; ValueData: "{code:GetSubSystemMonitorPort}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "SystemMatrixPort"; ValueData: "{code:GetSystemMatrixPort}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "UserProfileCheckerPort"; ValueData: "{code:GetUserProfileCheckerPort}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "NetWorkEnablerPort"; ValueData: "{code:GetNetWorkEnablerPort}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "QSYSOPRMonitorPort"; ValueData: "{code:GetQSYSOPRMonitorPort}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "WinFSErrorMonitorPort"; ValueData: "{code:GetWinFSErrorMonitorPort}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "WinFSCardinalityMonitorPort"; ValueData: "{code:GetWinFSCardinalityMonitorPort}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "WinClientName"; ValueData: "{code:GetWinClientName}"; Flags: uninsdeletevalue
; Loki/Promtail settings
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "InstallRole"; ValueData: "{code:GetInstallRole}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "LokiPort"; ValueData: "{code:GetLokiPort}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "LokiDataDir"; ValueData: "{code:GetLokiDataDir}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "LokiHost"; ValueData: "{code:GetLokiHost}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "LogAgentAppName"; ValueData: "{code:GetLogAgentAppName}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "LogAgentEnvironment"; ValueData: "{code:GetLogAgentEnvironment}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "LogAgentLogPath"; ValueData: "{code:GetLogAgentLogPath}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "InstalledLoki"; ValueData: "{code:GetInstalledLoki}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "InstalledLogAgent"; ValueData: "{code:GetInstalledLogAgent}"; Flags: uninsdeletevalue

[Run]
Filename: "http://localhost:{code:GetGrafPort}"; Description: "Open Grafana Dashboard in browser"; Flags: postinstall shellexec nowait unchecked skipifsilent; Check: IsMonitoringServerRole
Filename: "http://localhost:{code:GetPromPort}"; Description: "Open Prometheus Web UI in browser"; Flags: postinstall shellexec nowait unchecked skipifsilent; Check: IsMonitoringServerRole


[UninstallRun]
; Service stop/uninstall is handled by CurUninstallStepChanged(usUninstall) which:
; 1. Reads the saved ClientInstanceId from .client_instance_id file
; 2. Stops and uninstalls ALL client-suffixed services correctly
; 3. Handles both suffixed and non-suffixed names for backwards compatibility
; Leaving these [UninstallRun] entries would be redundant and could cause issues
; since they reference base service names that don't exist in multi-client setups.

[UninstallDelete]
; Remove client identity file
Type: files; Name: "{app}\.client_instance_id"
; Remove runtime-generated service XML files
Type: files; Name: "{app}\services\*.xml"
; Remove client-suffixed service executables (created at runtime for multi-client installs)
Type: files; Name: "{app}\services\IPMonitoring_IBMIFSErrorMonitor_*.exe"
Type: files; Name: "{app}\services\IPMonitoring_IBMRealTimeIFSMonitor_*.exe"
Type: files; Name: "{app}\services\IPMonitoring_IBMJobQueCountMonitor_*.exe"
Type: files; Name: "{app}\services\IPMonitoring_IBMJobQueStatusMonitor_*.exe"
Type: files; Name: "{app}\services\IPMonitoring_ServerUpTimeMonitor_*.exe"
Type: files; Name: "{app}\services\IPMonitoring_IBMSubSystemMonitoring_*.exe"
Type: files; Name: "{app}\services\IPMonitoring_IBMSystemMatrix_*.exe"
Type: files; Name: "{app}\services\IPMonitoring_IBMUserProfileChecker_*.exe"
Type: files; Name: "{app}\services\IPMonitoring_NetWorkEnabler_*.exe"
Type: files; Name: "{app}\services\IPMonitoring_QSYSOPRMonitoring_*.exe"
Type: files; Name: "{app}\services\IPMonitoring_WinFSErrorMonitor_*.exe"
Type: files; Name: "{app}\services\IPMonitoring_WinFSCardinalityMonitor_*.exe"
; Remove generated email.properties files from each monitor folder
Type: files; Name: "{app}\monitoring-services\*\email.properties"
; Remove services folder if empty after file deletion
Type: dirifempty; Name: "{app}\services"
; Remove monitoring-services subfolders if empty
Type: dirifempty; Name: "{app}\monitoring-services\IBMIFSErrorMonitor"
Type: dirifempty; Name: "{app}\monitoring-services\IBMRealTimeIFSMonitor"
Type: dirifempty; Name: "{app}\monitoring-services\IBMJobQueCountMonitor"
Type: dirifempty; Name: "{app}\monitoring-services\IBMJobQueStatusMonitor"
Type: dirifempty; Name: "{app}\monitoring-services\ServerUpTimeMonitor"
Type: dirifempty; Name: "{app}\monitoring-services\IBMSubSystemMonitoring"
Type: dirifempty; Name: "{app}\monitoring-services\IBMSystemMatrix"
Type: dirifempty; Name: "{app}\monitoring-services\IBMUserProfileChecker"
Type: dirifempty; Name: "{app}\monitoring-services\NetWorkEnabler"
Type: dirifempty; Name: "{app}\monitoring-services\QSYSOPRMonitoring"
Type: dirifempty; Name: "{app}\monitoring-services\WinFSErrorMonitor"
Type: dirifempty; Name: "{app}\monitoring-services\WinFSCardinalityMonitor"
Type: dirifempty; Name: "{app}\monitoring-services"
; Remove prometheus and grafana config folders
Type: dirifempty; Name: "{app}\prometheus"
Type: dirifempty; Name: "{app}\grafana\conf\provisioning\datasources"
Type: dirifempty; Name: "{app}\grafana\conf\provisioning\dashboards"
Type: dirifempty; Name: "{app}\grafana\conf\provisioning"
Type: dirifempty; Name: "{app}\grafana\conf"
Type: dirifempty; Name: "{app}\grafana"
; Remove Loki and Promtail folders if empty (preserve data on uninstall)
Type: files; Name: "{app}\loki\loki.yaml"
Type: files; Name: "{app}\promtail\promtail.yaml"
Type: dirifempty; Name: "{app}\loki"
Type: dirifempty; Name: "{app}\promtail"
; Remove main app folder if empty
Type: dirifempty; Name: "{app}"

[Code]
var
  ConfigPage: TInputQueryWizardPage;
  CoreServicesPage: TWizardPage;      // Page 1: Prometheus, Grafana, Loki, Promtail
  IBMiMonitorsPage: TWizardPage;      // Page 2: IBM i monitoring services
  WindowsMonitorsPage: TWizardPage;   // Page 3: Windows file system monitors
  ServicePortsPage: TWizardPage;
  IBMiConfigPage: TInputQueryWizardPage;
  
  // Install Role Selection (Monitoring Server / Log Agent)
  InstallRolePage: TInputOptionWizardPage;
  InstallRole: string;  // 'Monitoring' or 'Agent'
  
  // Loki Configuration Page (for Monitoring Server role)
  LokiConfigPage: TInputQueryWizardPage;
  
  // Promtail/Log Agent Configuration Page (for Application Server role)
  LogAgentConfigPage: TWizardPage;
  LogAgentHostEdit, LogAgentPortEdit, LogAgentAppNameEdit: TNewEdit;
  LogAgentEnvironmentCombo: TNewComboBox;
  LogAgentLogPathEdit: TNewEdit;
  
  // Service selection checkboxes (custom page for enable/disable control)
  ChkPrometheus, ChkGrafana, ChkLoki, ChkPromtail: TNewCheckBox;
  ChkIFSErrorMonitor, ChkRealTimeIFSMonitor: TNewCheckBox;
  ChkJobQueCountMonitor, ChkJobQueStatusMonitor, ChkServerUpTimeMonitor: TNewCheckBox;
  ChkSubSystemMonitor, ChkSystemMatrix, ChkUserProfileChecker: TNewCheckBox;
  ChkNetWorkEnabler, ChkQSYSOPRMonitor: TNewCheckBox;
  ChkWinFSErrorMonitor, ChkWinFSCardinalityMonitor: TNewCheckBox;
  
  // Windows monitors client name
  WinClientNameEdit: TNewEdit;
  LblWinClientName: TNewStaticText;
  
  // Service ports custom page controls - Edit boxes
  IFSErrorMonitorPortEdit, RealTimeIFSMonitorPortEdit: TNewEdit;
  JobQueCountMonitorPortEdit, JobQueStatusMonitorPortEdit: TNewEdit;
  ServerUpTimeMonitorPortEdit: TNewEdit;
  SubSystemMonitorPortEdit, SystemMatrixPortEdit, UserProfileCheckerPortEdit: TNewEdit;
  NetWorkEnablerPortEdit, QSYSOPRMonitorPortEdit: TNewEdit;
  WinFSErrorMonitorPortEdit, WinFSCardinalityMonitorPortEdit: TNewEdit;
  
  // Service ports custom page controls - Labels (for dynamic visibility)
  LblIFSErrorMonitorPort, LblRealTimeIFSMonitorPort: TNewStaticText;
  LblJobQueCountMonitorPort, LblJobQueStatusMonitorPort: TNewStaticText;
  LblServerUpTimeMonitorPort: TNewStaticText;
  LblSubSystemMonitorPort, LblSystemMatrixPort, LblUserProfileCheckerPort: TNewStaticText;
  LblNetWorkEnablerPort, LblQSYSOPRMonitorPort: TNewStaticText;
  LblWinFSErrorMonitorPort, LblWinFSCardinalityMonitorPort: TNewStaticText;
  
  // Email configuration pages - split into multiple clean pages
  EmailAuthMethodPage: TInputOptionWizardPage;
  SmtpConfigPage: TWizardPage;
  OAuthConfigPage1: TWizardPage;
  OAuthConfigPage2: TWizardPage;
  EmailCommonPage: TWizardPage;

  // Optional extra port scan page
  ExtraPortsPage: TWizardPage;
  ExtraPortsMemo: TNewMemo;
  
  // SMTP custom page controls
  SmtpHostEdit, SmtpPortEdit, SmtpUsernameEdit, SmtpPasswordEdit: TNewEdit;
  SmtpAuthCheckbox, SmtpStartTlsCheckbox: TNewCheckBox;
  
  // OAuth2 custom page controls
  OAuthTenantEdit, OAuthClientIdEdit, OAuthClientSecretEdit: TNewEdit;
  OAuthScopeEdit, OAuthFromUserEdit: TNewEdit;
  OAuthTokenUrlEdit, OAuthMailUrlEdit: TNewEdit;
  
  // Email Recipients page controls
  EmailFromEdit, EmailToEdit, EmailBccEdit, EmailClientNameEdit: TNewEdit;
  EmailImportanceCombo: TNewComboBox;
  
  // Upgrade detection
  IsUpgrade: Boolean;
  
  // Track if core services were previously installed (cannot be uninstalled)
  PrometheusInstalled, GrafanaInstalled, LokiInstalled, PromtailInstalled: Boolean;
  
  // Multi-client installation support
  ClientInstancePage: TInputQueryWizardPage;
  InstallModePage: TInputOptionWizardPage;  // New vs Modify existing selection
  ClientInstanceId: string;
  IsFirstInstallation: Boolean;
  IsNewClientInstallation: Boolean;  // User explicitly chose "New Installation"
  UseExistingGrafana: Boolean;
  ExistingGrafanaPort: string;
  ExistingPrometheusPort: string;
  ExistingInstallPath: string;
  ClientPortOffset: Integer;

// =============================================================================
// CRITICAL: InitializeUninstall runs BEFORE any {code:...} is evaluated for AppId
// This allows us to load the ClientInstanceId from a file so GetClientInstanceId
// returns the correct value even during uninstall operations
// NOTE: We use {uninstallexe} instead of {app} because {app} is not available
// until AFTER AppId is resolved, which creates a circular dependency
// =============================================================================
function InitializeUninstall(): Boolean;
var
  ClientIdFile: string;
  ClientIdContent: AnsiString;
  AppDir: string;
begin
  Result := True;
  // Derive app directory from uninstaller path (unins000.exe is in {app})
  // {uninstallexe} is available immediately, unlike {app} which requires AppId first
  AppDir := ExtractFileDir(ExpandConstant('{uninstallexe}'));
  
  // Read ClientInstanceId from the identity file created during installation
  ClientIdFile := AppDir + '\.client_instance_id';
  if FileExists(ClientIdFile) then
  begin
    if LoadStringFromFile(ClientIdFile, ClientIdContent) then
      ClientInstanceId := Trim(String(ClientIdContent));
  end;
  // If file doesn't exist, ClientInstanceId will be empty and GetClientInstanceId returns 'Default'
end;

// Load a default client instance id when exactly one client exists (upgrade convenience)
procedure LoadDefaultClientInstanceId;
var
  SubKeys: TArrayOfString;
begin
  if ClientInstanceId <> '' then Exit;
  if RegGetSubkeyNames(HKLM, '{#AppRegKeyClients}', SubKeys) then
    if GetArrayLength(SubKeys) = 1 then
      ClientInstanceId := SubKeys[0];
end;

// Check if this is an upgrade (previous installation exists)
function CheckIsUpgrade: Boolean;
var
  InstallPath: string;
begin
  Result := RegQueryStringValue(HKLM, '{#AppRegKey}', 'InstallPath', InstallPath) and (InstallPath <> '');
end;

// Check if a base installation exists (Prometheus/Grafana already installed)
function CheckBaseInstallationExists: Boolean;
var
  TempPort: string;
begin
  Result := RegQueryStringValue(HKLM, '{#AppRegKey}', 'InstallPath', ExistingInstallPath) and (ExistingInstallPath <> '');
  if Result then
  begin
    if not RegQueryStringValue(HKLM, '{#AppRegKey}', 'GrafanaPort', TempPort) or (TempPort = '') then
      ExistingGrafanaPort := '3000'
    else
      ExistingGrafanaPort := TempPort;
    if not RegQueryStringValue(HKLM, '{#AppRegKey}', 'PrometheusPort', TempPort) or (TempPort = '') then
      ExistingPrometheusPort := '9090'
    else
      ExistingPrometheusPort := TempPort;
  end;
end;

// Check if a specific client instance already exists
function CheckClientExists(ClientId: string): Boolean;
var
  ClientPath: string;
begin
  Result := RegQueryStringValue(HKLM, '{#AppRegKeyClients}\' + ClientId, 'InstallPath', ClientPath) and (ClientPath <> '');
end;

// Get number of installed client instances (for port offset calculation)
function GetInstalledClientCount: Integer;
var
  SubKeys: TArrayOfString;
begin
  Result := 0;
  if RegGetSubkeyNames(HKLM, '{#AppRegKeyClients}', SubKeys) then
    Result := GetArrayLength(SubKeys);
end;

// Initialize ClientInstanceId as early as possible (before any {code:...} in [Setup])
procedure InitClientInstanceId;
var
  I: Integer;
  ParamVal, RegVal: string;
begin
  if ClientInstanceId <> '' then Exit;

  // Check command-line switches: /CLIENT=XYZ or /CLIENTINSTANCEID=XYZ
  for I := 1 to ParamCount do
  begin
    ParamVal := ParamStr(I);
    if (Pos('/CLIENT=', Uppercase(ParamVal)) = 1) or (Pos('-CLIENT=', Uppercase(ParamVal)) = 1) then
    begin
      ClientInstanceId := Copy(ParamVal, Pos('=', ParamVal) + 1, MaxInt);
      Exit;
    end;
    if (Pos('/CLIENTINSTANCEID=', Uppercase(ParamVal)) = 1) or (Pos('-CLIENTINSTANCEID=', Uppercase(ParamVal)) = 1) then
    begin
      ClientInstanceId := Copy(ParamVal, Pos('=', ParamVal) + 1, MaxInt);
      Exit;
    end;
  end;

  // Fallback: existing registry value
  if RegQueryStringValue(HKLM, '{#AppRegKey}', 'ClientInstanceId', RegVal) then
    ClientInstanceId := RegVal;
end;

const
  GENERIC_WRITE = $40000000;
  FILE_SHARE_READ = $00000001;
  FILE_SHARE_WRITE = $00000002;
  OPEN_EXISTING = 3;
  INVALID_HANDLE_VALUE = -1;
  SERVICES_REG_PATH = 'SYSTEM\CurrentControlSet\Services\';

function GetClientRegistryValue(ValueName, Default: string): string; forward;

// Calculate port with offset for multi-client installations
// Each new client gets ports offset by 100 from the previous one
function GetOffsetPort(BasePort: Integer): string;
begin
  Result := IntToStr(BasePort + ClientPortOffset);
end;

function ServiceExists(BaseServiceId: string): Boolean;
begin
  // Robust service detection: support both generic and client-suffixed IDs.
  // 1) If caller passed a fully qualified (possibly suffixed) ID, check it directly.
  if RegKeyExists(HKLM, SERVICES_REG_PATH + BaseServiceId) then
  begin
    Result := True;
    Exit;
  end;
  // 2) Otherwise (or additionally), check client-suffixed form when ClientInstanceId is set.
  if (ClientInstanceId <> '') and RegKeyExists(HKLM, SERVICES_REG_PATH + BaseServiceId + '_' + ClientInstanceId) then
  begin
    Result := True;
    Exit;
  end;
  // 3) Finally, check the generic/base form.
  Result := RegKeyExists(HKLM, SERVICES_REG_PATH + BaseServiceId);
end;

function WasServiceInstalled(ValueName, BaseServiceId: string): Boolean;
begin
  if GetClientRegistryValue(ValueName, '') = 'true' then
    Result := True
  else
    Result := ServiceExists(BaseServiceId);
end;

// Get client instance ID - MUST be completely self-contained since it's called
// from [Setup] section before InitializeSetup runs. Cannot use {app} or call
// any functions that might indirectly use {app}.
function GetClientInstanceId(Param: string): string;
var
  I: Integer;
  ParamVal, RegVal: string;
begin
  // Return cached value if already set
  if ClientInstanceId <> '' then
  begin
    Result := ClientInstanceId;
    Exit;
  end;

  // Check command-line switches: /CLIENT=XYZ or /CLIENTINSTANCEID=XYZ
  for I := 1 to ParamCount do
  begin
    ParamVal := ParamStr(I);
    if (Pos('/CLIENT=', Uppercase(ParamVal)) = 1) or (Pos('-CLIENT=', Uppercase(ParamVal)) = 1) then
    begin
      ClientInstanceId := Copy(ParamVal, Pos('=', ParamVal) + 1, MaxInt);
      Result := ClientInstanceId;
      Exit;
    end;
    if (Pos('/CLIENTINSTANCEID=', Uppercase(ParamVal)) = 1) or (Pos('-CLIENTINSTANCEID=', Uppercase(ParamVal)) = 1) then
    begin
      ClientInstanceId := Copy(ParamVal, Pos('=', ParamVal) + 1, MaxInt);
      Result := ClientInstanceId;
      Exit;
    end;
  end;

  // Fallback: check registry for existing ClientInstanceId
  // Using literal string instead of {#AppRegKey} to be extra safe
  if RegQueryStringValue(HKLM, 'Software\IslandPacific\Monitoring', 'ClientInstanceId', RegVal) then
  begin
    ClientInstanceId := RegVal;
    Result := RegVal;
    Exit;
  end;

  // Return empty - client must provide a valid name
  Result := '';
end;

// Registry helper function
function GetRegistryValue(ValueName, Default: string): string;
var
  Value: string;
begin
  if RegQueryStringValue(HKLM, '{#AppRegKey}', ValueName, Value) then
    Result := Value
  else
    Result := Default;
end;

// Client-aware registry helper: prefer client branch when ClientInstanceId is set
// Only use base registry values if we're upgrading the SAME ROLE (avoid cross-role config mixup)
function GetClientRegistryValue(ValueName, Default: string): string;
var
  Value: string;
  PreviousRole: string;
begin
  if (ClientInstanceId <> '') and RegQueryStringValue(HKLM, '{#AppRegKeyClients}\' + ClientInstanceId, ValueName, Value) then
    Result := Value
  else if RegQueryStringValue(HKLM, '{#AppRegKey}', ValueName, Value) then
  begin
    // Only use base registry values if upgrading the same role
    // Get the previous installation's role
    PreviousRole := '';
    RegQueryStringValue(HKLM, '{#AppRegKey}', 'InstallRole', PreviousRole);
    // If previous role exists and differs from current role, don't use its values
    if (PreviousRole <> '') and (PreviousRole <> InstallRole) then
      Result := Default
    else
      Result := Value;
  end
  else
    Result := Default;
end;

// Getter functions for registry values
function GetPromPort(Param: string): string;
begin
  Result := ConfigPage.Values[0];
  if Result = '' then Result := '9090';
end;

function GetGrafPort(Param: string): string;
begin
  Result := ConfigPage.Values[1];
  if Result = '' then Result := '3000';
end;

function GetGrafPwd(Param: string): string;
begin
  Result := ConfigPage.Values[2];
  if Result = '' then Result := 'admin';
end;

// Service port getters
function GetIFSErrorMonitorPort(Param: string): string;
begin
  Result := IFSErrorMonitorPortEdit.Text;
  if Result = '' then Result := '3010';
end;

function GetRealTimeIFSMonitorPort(Param: string): string;
begin
  Result := RealTimeIFSMonitorPortEdit.Text;
  if Result = '' then Result := '3011';
end;

function GetJobQueCountMonitorPort(Param: string): string;
begin
  Result := JobQueCountMonitorPortEdit.Text;
  if Result = '' then Result := '3012';
end;

function GetJobQueStatusMonitorPort(Param: string): string;
begin
  Result := JobQueStatusMonitorPortEdit.Text;
  if Result = '' then Result := '3013';
end;

function GetServerUpTimeMonitorPort(Param: string): string;
begin
  Result := ServerUpTimeMonitorPortEdit.Text;
  if Result = '' then Result := '3014';
end;

function GetIBMiServer(Param: string): string;
begin
  Result := IBMiConfigPage.Values[0];
end;

function GetIBMiUser(Param: string): string;
begin
  Result := IBMiConfigPage.Values[1];
end;

function GetIBMiPassword(Param: string): string;
begin
  Result := IBMiConfigPage.Values[2];
end;

function GetClientName(Param: string): string;
begin
  Result := IBMiConfigPage.Values[3];
  // Client name should match the Client Instance ID if not specified
  if Result = '' then Result := ClientInstanceId;
end;

function GetEmailAuthMethod(Param: string): string;
begin
  if EmailAuthMethodPage.SelectedValueIndex = 0 then
    Result := 'SMTP'
  else
    Result := 'OAUTH2';
end;

function GetSmtpHost(Param: string): string;
begin
  Result := SmtpHostEdit.Text;
  if Result = '' then Result := 'smtp.office365.com';
end;

function GetSmtpPort(Param: string): string;
begin
  Result := SmtpPortEdit.Text;
  if Result = '' then Result := '587';
end;

function GetSmtpUsername(Param: string): string;
begin
  Result := SmtpUsernameEdit.Text;
end;

function GetSmtpPassword(Param: string): string;
begin
  Result := SmtpPasswordEdit.Text;
end;

function GetSmtpAuth(Param: string): string;
begin
  if SmtpAuthCheckbox.Checked then
    Result := 'true'
  else
    Result := 'false';
end;

function GetSmtpStartTls(Param: string): string;
begin
  if SmtpStartTlsCheckbox.Checked then
    Result := 'true'
  else
    Result := 'false';
end;

function GetOAuthTenant(Param: string): string;
begin
  Result := OAuthTenantEdit.Text;
end;

function GetOAuthClientId(Param: string): string;
begin
  Result := OAuthClientIdEdit.Text;
end;

function GetOAuthClientSecret(Param: string): string;
begin
  Result := OAuthClientSecretEdit.Text;
end;

function GetOAuthScope(Param: string): string;
begin
  Result := OAuthScopeEdit.Text;
  if Result = '' then Result := 'https://graph.microsoft.com/.default';
end;

function GetOAuthFromUser(Param: string): string;
begin
  Result := OAuthFromUserEdit.Text;
end;

function GetOAuthTokenUrl(Param: string): string;
var
  TenantId: string;
begin
  Result := OAuthTokenUrlEdit.Text;
  if Result = '' then
  begin
    TenantId := OAuthTenantEdit.Text;
    if TenantId <> '' then
      Result := 'https://login.microsoftonline.com/' + TenantId + '/oauth2/v2.0/token';
  end;
end;

function GetOAuthMailUrl(Param: string): string;
var
  FromUser: string;
begin
  Result := OAuthMailUrlEdit.Text;
  if Result = '' then
  begin
    FromUser := OAuthFromUserEdit.Text;
    if FromUser <> '' then
      Result := 'https://graph.microsoft.com/v1.0/users/' + FromUser + '/sendMail';
  end;
end;

function GetEmailFrom(Param: string): string;
begin
  Result := EmailFromEdit.Text;
end;

function GetEmailTo(Param: string): string;
begin
  Result := EmailToEdit.Text;
end;

function GetEmailBcc(Param: string): string;
begin
  Result := EmailBccEdit.Text;
end;

function GetClientNameEmail(Param: string): string;
begin
  Result := EmailClientNameEdit.Text;
  if Result = '' then Result := GetClientName('');
end;

function GetEmailImportance(Param: string): string;
begin
  Result := EmailImportanceCombo.Text;
  if Result = '' then Result := 'Normal';
end;

// Getter functions for installed services tracking
function GetInstalledPrometheus(Param: string): string;
begin
  if ChkPrometheus.Checked then Result := 'true' else Result := 'false';
end;

function GetInstalledGrafana(Param: string): string;
begin
  if ChkGrafana.Checked then Result := 'true' else Result := 'false';
end;

function GetInstalledIFSErrorMonitor(Param: string): string;
begin
  if ChkIFSErrorMonitor.Checked then Result := 'true' else Result := 'false';
end;

function GetInstalledRealTimeIFSMonitor(Param: string): string;
begin
  if ChkRealTimeIFSMonitor.Checked then Result := 'true' else Result := 'false';
end;

function GetInstalledJobQueCountMonitor(Param: string): string;
begin
  if ChkJobQueCountMonitor.Checked then Result := 'true' else Result := 'false';
end;

function GetInstalledJobQueStatusMonitor(Param: string): string;
begin
  if ChkJobQueStatusMonitor.Checked then Result := 'true' else Result := 'false';
end;

function GetInstalledServerUpTimeMonitor(Param: string): string;
begin
  if ChkServerUpTimeMonitor.Checked then Result := 'true' else Result := 'false';
end;

function GetInstalledSubSystemMonitor(Param: string): string;
begin
  if ChkSubSystemMonitor.Checked then Result := 'true' else Result := 'false';
end;

function GetInstalledSystemMatrix(Param: string): string;
begin
  if ChkSystemMatrix.Checked then Result := 'true' else Result := 'false';
end;

function GetInstalledUserProfileChecker(Param: string): string;
begin
  if ChkUserProfileChecker.Checked then Result := 'true' else Result := 'false';
end;

function GetInstalledNetWorkEnabler(Param: string): string;
begin
  if ChkNetWorkEnabler.Checked then Result := 'true' else Result := 'false';
end;

function GetInstalledQSYSOPRMonitor(Param: string): string;
begin
  if ChkQSYSOPRMonitor.Checked then Result := 'true' else Result := 'false';
end;

function GetInstalledWinFSErrorMonitor(Param: string): string;
begin
  if ChkWinFSErrorMonitor.Checked then Result := 'true' else Result := 'false';
end;

function GetInstalledWinFSCardinalityMonitor(Param: string): string;
begin
  if ChkWinFSCardinalityMonitor.Checked then Result := 'true' else Result := 'false';
end;

function GetSubSystemMonitorPort(Param: string): string;
begin
  Result := SubSystemMonitorPortEdit.Text;
end;

function GetSystemMatrixPort(Param: string): string;
begin
  Result := SystemMatrixPortEdit.Text;
end;

function GetUserProfileCheckerPort(Param: string): string;
begin
  Result := UserProfileCheckerPortEdit.Text;
end;

function GetNetWorkEnablerPort(Param: string): string;
begin
  Result := NetWorkEnablerPortEdit.Text;
end;

function GetQSYSOPRMonitorPort(Param: string): string;
begin
  Result := QSYSOPRMonitorPortEdit.Text;
end;

function GetWinFSErrorMonitorPort(Param: string): string;
begin
  Result := WinFSErrorMonitorPortEdit.Text;
end;

function GetWinFSCardinalityMonitorPort(Param: string): string;
begin
  Result := WinFSCardinalityMonitorPortEdit.Text;
end;

function GetWinClientName(Param: string): string;
begin
  Result := WinClientNameEdit.Text;
  if Result = '' then Result := GetClientInstanceId('');
  if Result = '' then Result := 'DefaultClient';
end;

// ===== INSTALL ROLE FUNCTIONS =====
function GetInstallRole(Param: string): string;
begin
  Result := InstallRole;
  if Result = '' then Result := 'Monitoring';
end;

function IsMonitoringServerRole: Boolean;
begin
  Result := (InstallRole = 'Monitoring') or (InstallRole = '');
end;

function IsWinAgentRole: Boolean;
begin
  Result := (InstallRole = 'WinAgent');
end;

function IsLogAgentRole: Boolean;
begin
  Result := (InstallRole = 'Agent');
end;

// ===== LOKI CONFIGURATION GETTERS =====
function GetLokiPort(Param: string): string;
begin
  if LokiConfigPage <> nil then
    Result := LokiConfigPage.Values[0]
  else
    Result := GetClientRegistryValue('LokiPort', '3100');
  if Result = '' then Result := '3100';
end;

function GetLokiDataDir(Param: string): string;
begin
  if LokiConfigPage <> nil then
    Result := LokiConfigPage.Values[1]
  else
    Result := GetClientRegistryValue('LokiDataDir', '');
  // Only use {app} after wizard has been shown (WizardForm exists)
  if (Result = '') and (WizardForm <> nil) then
    Result := ExpandConstant('{app}\data\loki')
  else if Result = '' then
    Result := 'C:\ProgramData\IPMonitoring\loki';  // Safe fallback outside Program Files
end;

// ===== LOG AGENT (PROMTAIL) CONFIGURATION GETTERS =====
function GetLokiHost(Param: string): string;
begin
  if LogAgentHostEdit <> nil then
    Result := LogAgentHostEdit.Text
  else
    Result := GetClientRegistryValue('LokiHost', 'localhost');
  if Result = '' then Result := 'localhost';
end;

function GetLogAgentAppName(Param: string): string;
begin
  if LogAgentAppNameEdit <> nil then
    Result := LogAgentAppNameEdit.Text
  else
    Result := GetClientRegistryValue('LogAgentAppName', 'MyApp');
  if Result = '' then Result := 'MyApp';
end;

function GetLogAgentEnvironment(Param: string): string;
begin
  if LogAgentEnvironmentCombo <> nil then
    Result := LogAgentEnvironmentCombo.Text
  else
    Result := GetClientRegistryValue('LogAgentEnvironment', 'PROD');
  if Result = '' then Result := 'PROD';
end;

function GetLogAgentLogPath(Param: string): string;
begin
  if LogAgentLogPathEdit <> nil then
    Result := LogAgentLogPathEdit.Text
  else
    Result := GetClientRegistryValue('LogAgentLogPath', 'C:/logs/*.log');
  if Result = '' then Result := 'C:/logs/*.log';
end;

// ===== INSTALLED SERVICE TRACKING FOR LOKI/PROMTAIL =====
function GetInstalledLoki(Param: string): string;
begin
  if IsMonitoringServerRole and ChkLoki.Checked then Result := 'true' else Result := 'false';
end;

function GetInstalledLogAgent(Param: string): string;
begin
  // Promtail can be installed via Log Agent role OR via checkbox on Monitoring Server
  if IsLogAgentRole or ChkPromtail.Checked then Result := 'true' else Result := 'false';
end;

// Helper functions to check service checkbox state
function IsPrometheusSelected: Boolean;
begin
  Result := ChkPrometheus.Checked;
end;

function IsGrafanaSelected: Boolean;
begin
  Result := ChkGrafana.Checked;
end;

function IsLokiSelected: Boolean;
begin
  Result := ChkLoki.Checked;
end;

function IsPromtailSelected: Boolean;
begin
  Result := ChkPromtail.Checked;
end;

function IsIFSErrorMonitorSelected: Boolean;
begin
  Result := ChkIFSErrorMonitor.Checked;
end;

function IsRealTimeIFSMonitorSelected: Boolean;
begin
  Result := ChkRealTimeIFSMonitor.Checked;
end;

function IsJobQueCountMonitorSelected: Boolean;
begin
  Result := ChkJobQueCountMonitor.Checked;
end;

function IsJobQueStatusMonitorSelected: Boolean;
begin
  Result := ChkJobQueStatusMonitor.Checked;
end;

function IsServerUpTimeMonitorSelected: Boolean;
begin
  Result := ChkServerUpTimeMonitor.Checked;
end;

function IsSubSystemMonitorSelected: Boolean;
begin
  Result := ChkSubSystemMonitor.Checked;
end;

function IsSystemMatrixSelected: Boolean;
begin
  Result := ChkSystemMatrix.Checked;
end;

function IsUserProfileCheckerSelected: Boolean;
begin
  Result := ChkUserProfileChecker.Checked;
end;

function IsNetWorkEnablerSelected: Boolean;
begin
  Result := ChkNetWorkEnabler.Checked;
end;

function IsQSYSOPRMonitorSelected: Boolean;
begin
  Result := ChkQSYSOPRMonitor.Checked;
end;

function IsWinFSErrorMonitorSelected: Boolean;
begin
  Result := ChkWinFSErrorMonitor.Checked;
end;

function IsWinFSCardinalityMonitorSelected: Boolean;
begin
  Result := ChkWinFSCardinalityMonitor.Checked;
end;

// ===== UI HELPER FUNCTIONS FOR CONSISTENT STYLING =====

function CreateSectionHeader(Page: TWizardPage; Caption: string; var TopPos: Integer): TNewStaticText;
begin
  Result := TNewStaticText.Create(Page);
  Result.Parent := Page.Surface;
  Result.Caption := Caption;
  Result.Left := 0;
  Result.Top := TopPos;
  Result.Font.Style := [fsBold];
  Result.Font.Size := 9;
  TopPos := TopPos + Result.Height + 2;
end;

function CreateSeparator(Page: TWizardPage; var TopPos: Integer): TBevel;
begin
  Result := TBevel.Create(Page);
  Result.Parent := Page.Surface;
  Result.Left := 0;
  Result.Top := TopPos;
  Result.Width := Page.SurfaceWidth;
  Result.Height := 2;
  Result.Shape := bsTopLine;
  TopPos := TopPos + 8;
end;

function CreateHelperText(Page: TWizardPage; Caption: string; var TopPos: Integer): TNewStaticText;
begin
  Result := TNewStaticText.Create(Page);
  Result.Parent := Page.Surface;
  Result.Caption := Caption;
  Result.Left := 0;
  Result.Top := TopPos;
  Result.Font.Name := 'Segoe UI';
  Result.Font.Size := 8;
  Result.Font.Color := $666666; // Gray color
  Result.WordWrap := True;
  Result.Width := Page.SurfaceWidth;
  TopPos := TopPos + Result.Height + 4;
end;

function CreateFieldLabel(Page: TWizardPage; Caption: string; var TopPos: Integer): TNewStaticText;
begin
  Result := TNewStaticText.Create(Page);
  Result.Parent := Page.Surface;
  Result.Caption := Caption;
  Result.Left := 0;
  Result.Top := TopPos;
  TopPos := TopPos + Result.Height + 4;
end;

function CreateTextEdit(Page: TWizardPage; DefaultValue: string; IsPassword: Boolean; var TopPos: Integer): TNewEdit;
begin
  Result := TNewEdit.Create(Page);
  Result.Parent := Page.Surface;
  Result.Left := 0;
  Result.Top := TopPos;
  Result.Width := Page.SurfaceWidth;
  Result.Height := 23;
  Result.Text := DefaultValue;
  if IsPassword then
    Result.PasswordChar := '*';
  TopPos := TopPos + Result.Height + 10;
end;

// ===== CORE SERVICES PAGE CONTROLS =====
procedure CreateCoreServicesControls(Page: TWizardPage);
var
  TopPos: Integer;
  NoteLabel: TNewStaticText;
  AnyMonitorInstalled: Boolean;
begin
  // Check if any monitoring service is currently installed
  AnyMonitorInstalled := WasServiceInstalled('InstalledIFSErrorMonitor', 'IPMonitoring_IBMIFSErrorMonitor') or
                         WasServiceInstalled('InstalledRealTimeIFSMonitor', 'IPMonitoring_IBMRealTimeIFSMonitor') or
                         WasServiceInstalled('InstalledJobQueCountMonitor', 'IPMonitoring_IBMJobQueCountMonitor') or
                         WasServiceInstalled('InstalledJobQueStatusMonitor', 'IPMonitoring_IBMJobQueStatusMonitor') or
                         WasServiceInstalled('InstalledServerUpTimeMonitor', 'IPMonitoring_ServerUpTimeMonitor') or
                         WasServiceInstalled('InstalledSubSystemMonitor', 'IPMonitoring_IBMSubSystemMonitoring') or
                         WasServiceInstalled('InstalledSystemMatrix', 'IPMonitoring_IBMSystemMatrix') or
                         WasServiceInstalled('InstalledUserProfileChecker', 'IPMonitoring_IBMUserProfileChecker') or
                         WasServiceInstalled('InstalledNetWorkEnabler', 'IPMonitoring_NetWorkEnabler') or
                         WasServiceInstalled('InstalledQSYSOPRMonitor', 'IPMonitoring_QSYSOPRMonitoring') or
                         WasServiceInstalled('InstalledWinFSErrorMonitor', 'IPMonitoring_WinFSErrorMonitor') or
                         WasServiceInstalled('InstalledWinFSCardinalityMonitor', 'IPMonitoring_WinFSCardinalityMonitor');

  TopPos := 8;
  
  // Prometheus checkbox
  ChkPrometheus := TNewCheckBox.Create(Page);
  ChkPrometheus.Parent := Page.Surface;
  ChkPrometheus.Left := 20;
  ChkPrometheus.Top := TopPos;
  ChkPrometheus.Width := Page.SurfaceWidth - 30;
  ChkPrometheus.Height := 21;
  ChkPrometheus.Checked := True;
  
  if PrometheusInstalled then
  begin
    if AnyMonitorInstalled then
    begin
      ChkPrometheus.Enabled := False;
      ChkPrometheus.Caption := 'Prometheus (Metrics Collection) - Required by monitors';
    end
    else
      ChkPrometheus.Caption := 'Prometheus (Metrics Collection) - Installed';
  end
  else
    ChkPrometheus.Caption := 'Prometheus (Metrics Collection)';
  
  TopPos := TopPos + 28;
  
  // Grafana checkbox
  ChkGrafana := TNewCheckBox.Create(Page);
  ChkGrafana.Parent := Page.Surface;
  ChkGrafana.Left := 20;
  ChkGrafana.Top := TopPos;
  ChkGrafana.Width := Page.SurfaceWidth - 30;
  ChkGrafana.Height := 21;
  ChkGrafana.Checked := True;
  
  if GrafanaInstalled then
  begin
    if AnyMonitorInstalled then
    begin
      ChkGrafana.Enabled := False;
      ChkGrafana.Caption := 'Grafana (Dashboard && Visualization) - Required by monitors';
    end
    else
      ChkGrafana.Caption := 'Grafana (Dashboard && Visualization) - Installed';
  end
  else
    ChkGrafana.Caption := 'Grafana (Dashboard && Visualization)';
  
  TopPos := TopPos + 28;
  
  // Loki checkbox
  ChkLoki := TNewCheckBox.Create(Page);
  ChkLoki.Parent := Page.Surface;
  ChkLoki.Left := 20;
  ChkLoki.Top := TopPos;
  ChkLoki.Width := Page.SurfaceWidth - 30;
  ChkLoki.Height := 21;
  ChkLoki.Checked := True;
  
  if LokiInstalled then
    ChkLoki.Caption := 'Loki (Log Storage Server) - Installed'
  else
    ChkLoki.Caption := 'Loki (Log Storage Server)';
  
  TopPos := TopPos + 28;
  
  // Promtail checkbox
  ChkPromtail := TNewCheckBox.Create(Page);
  ChkPromtail.Parent := Page.Surface;
  ChkPromtail.Left := 20;
  ChkPromtail.Top := TopPos;
  ChkPromtail.Width := Page.SurfaceWidth - 30;
  ChkPromtail.Height := 21;
  ChkPromtail.Checked := True;
  
  if PromtailInstalled then
    ChkPromtail.Caption := 'Promtail (Log Shipping Agent) - Installed'
  else
    ChkPromtail.Caption := 'Promtail (Log Shipping Agent)';
  
  TopPos := TopPos + 40;
  
  // Note about uninstallation
  if PrometheusInstalled and GrafanaInstalled and AnyMonitorInstalled then
  begin
    NoteLabel := TNewStaticText.Create(Page);
    NoteLabel.Parent := Page.Surface;
    NoteLabel.Caption := 'Note: Uncheck all monitoring services first to uninstall core services.';
    NoteLabel.Left := 20;
    NoteLabel.Top := TopPos;
    NoteLabel.Font.Color := clGray;
    NoteLabel.Font.Size := 8;
  end;
end;

// ===== IBM i MONITORS PAGE CONTROLS =====
procedure CreateIBMiMonitorsControls(Page: TWizardPage);
var
  TopPos: Integer;
begin
  TopPos := 8;
  
  // IBM IFS Error Monitor
  ChkIFSErrorMonitor := TNewCheckBox.Create(Page);
  ChkIFSErrorMonitor.Parent := Page.Surface;
  ChkIFSErrorMonitor.Caption := 'IBM IFS Error Monitor - Monitors IBM i IFS error logs';
  ChkIFSErrorMonitor.Left := 20;
  ChkIFSErrorMonitor.Top := TopPos;
  ChkIFSErrorMonitor.Width := Page.SurfaceWidth - 30;
  ChkIFSErrorMonitor.Height := 21;
  ChkIFSErrorMonitor.Checked := WasServiceInstalled('InstalledIFSErrorMonitor', 'IPMonitoring_IBMIFSErrorMonitor');
  
  TopPos := TopPos + 28;
  
  // IBM Real-Time IFS Monitor
  ChkRealTimeIFSMonitor := TNewCheckBox.Create(Page);
  ChkRealTimeIFSMonitor.Parent := Page.Surface;
  ChkRealTimeIFSMonitor.Caption := 'IBM Real-Time IFS Monitor - Real-time file system monitoring';
  ChkRealTimeIFSMonitor.Left := 20;
  ChkRealTimeIFSMonitor.Top := TopPos;
  ChkRealTimeIFSMonitor.Width := Page.SurfaceWidth - 30;
  ChkRealTimeIFSMonitor.Height := 21;
  ChkRealTimeIFSMonitor.Checked := WasServiceInstalled('InstalledRealTimeIFSMonitor', 'IPMonitoring_IBMRealTimeIFSMonitor');
  
  TopPos := TopPos + 28;
  
  // IBM Job Queue Count Monitor
  ChkJobQueCountMonitor := TNewCheckBox.Create(Page);
  ChkJobQueCountMonitor.Parent := Page.Surface;
  ChkJobQueCountMonitor.Caption := 'IBM Job Queue Count Monitor - Monitors job queue counts';
  ChkJobQueCountMonitor.Left := 20;
  ChkJobQueCountMonitor.Top := TopPos;
  ChkJobQueCountMonitor.Width := Page.SurfaceWidth - 30;
  ChkJobQueCountMonitor.Height := 21;
  ChkJobQueCountMonitor.Checked := WasServiceInstalled('InstalledJobQueCountMonitor', 'IPMonitoring_IBMJobQueCountMonitor');
  
  TopPos := TopPos + 28;
  
  // IBM Job Queue Status Monitor
  ChkJobQueStatusMonitor := TNewCheckBox.Create(Page);
  ChkJobQueStatusMonitor.Parent := Page.Surface;
  ChkJobQueStatusMonitor.Caption := 'IBM Job Queue Status Monitor - Monitors queue health';
  ChkJobQueStatusMonitor.Left := 20;
  ChkJobQueStatusMonitor.Top := TopPos;
  ChkJobQueStatusMonitor.Width := Page.SurfaceWidth - 30;
  ChkJobQueStatusMonitor.Height := 21;
  ChkJobQueStatusMonitor.Checked := WasServiceInstalled('InstalledJobQueStatusMonitor', 'IPMonitoring_IBMJobQueStatusMonitor');
  
  TopPos := TopPos + 28;
  
  // Server UpTime Monitor
  ChkServerUpTimeMonitor := TNewCheckBox.Create(Page);
  ChkServerUpTimeMonitor.Parent := Page.Surface;
  ChkServerUpTimeMonitor.Caption := 'Server UpTime Monitor - Tracks system availability';
  ChkServerUpTimeMonitor.Left := 20;
  ChkServerUpTimeMonitor.Top := TopPos;
  ChkServerUpTimeMonitor.Width := Page.SurfaceWidth - 30;
  ChkServerUpTimeMonitor.Height := 21;
  ChkServerUpTimeMonitor.Checked := WasServiceInstalled('InstalledServerUpTimeMonitor', 'IPMonitoring_ServerUpTimeMonitor');
  
  TopPos := TopPos + 28;
  
  // IBM SubSystem Monitoring
  ChkSubSystemMonitor := TNewCheckBox.Create(Page);
  ChkSubSystemMonitor.Parent := Page.Surface;
  ChkSubSystemMonitor.Caption := 'IBM SubSystem Monitor - Monitors IBM i subsystems';
  ChkSubSystemMonitor.Left := 20;
  ChkSubSystemMonitor.Top := TopPos;
  ChkSubSystemMonitor.Width := Page.SurfaceWidth - 30;
  ChkSubSystemMonitor.Height := 21;
  ChkSubSystemMonitor.Checked := WasServiceInstalled('InstalledSubSystemMonitor', 'IPMonitoring_IBMSubSystemMonitoring');
  
  TopPos := TopPos + 28;
  
  // IBM System Matrix
  ChkSystemMatrix := TNewCheckBox.Create(Page);
  ChkSystemMatrix.Parent := Page.Surface;
  ChkSystemMatrix.Caption := 'IBM System Matrix - System performance metrics';
  ChkSystemMatrix.Left := 20;
  ChkSystemMatrix.Top := TopPos;
  ChkSystemMatrix.Width := Page.SurfaceWidth - 30;
  ChkSystemMatrix.Height := 21;
  ChkSystemMatrix.Checked := WasServiceInstalled('InstalledSystemMatrix', 'IPMonitoring_IBMSystemMatrix');
  
  TopPos := TopPos + 28;
  
  // IBM User Profile Checker
  ChkUserProfileChecker := TNewCheckBox.Create(Page);
  ChkUserProfileChecker.Parent := Page.Surface;
  ChkUserProfileChecker.Caption := 'IBM User Profile Checker - Notify disabled user profiles';
  ChkUserProfileChecker.Left := 20;
  ChkUserProfileChecker.Top := TopPos;
  ChkUserProfileChecker.Width := Page.SurfaceWidth - 30;
  ChkUserProfileChecker.Height := 21;
  ChkUserProfileChecker.Checked := WasServiceInstalled('InstalledUserProfileChecker', 'IPMonitoring_IBMUserProfileChecker');
  
  TopPos := TopPos + 28;
  
  // Network Enabler
  ChkNetWorkEnabler := TNewCheckBox.Create(Page);
  ChkNetWorkEnabler.Parent := Page.Surface;
  ChkNetWorkEnabler.Caption := 'IBM Network Enabler - Enable locked network user profiles';
  ChkNetWorkEnabler.Left := 20;
  ChkNetWorkEnabler.Top := TopPos;
  ChkNetWorkEnabler.Width := Page.SurfaceWidth - 30;
  ChkNetWorkEnabler.Height := 21;
  ChkNetWorkEnabler.Checked := WasServiceInstalled('InstalledNetWorkEnabler', 'IPMonitoring_NetWorkEnabler');
  
  TopPos := TopPos + 28;
  
  // QSYSOPR Monitoring
  ChkQSYSOPRMonitor := TNewCheckBox.Create(Page);
  ChkQSYSOPRMonitor.Parent := Page.Surface;
  ChkQSYSOPRMonitor.Caption := 'IBM QSYSOPR Monitor - Monitors QSYSOPR message queue';
  ChkQSYSOPRMonitor.Left := 20;
  ChkQSYSOPRMonitor.Top := TopPos;
  ChkQSYSOPRMonitor.Width := Page.SurfaceWidth - 30;
  ChkQSYSOPRMonitor.Height := 21;
  ChkQSYSOPRMonitor.Checked := WasServiceInstalled('InstalledQSYSOPRMonitor', 'IPMonitoring_QSYSOPRMonitoring');
end;

// ===== WINDOWS MONITORS PAGE CONTROLS =====
procedure CreateWindowsMonitorsControls(Page: TWizardPage);
var
  TopPos, LeftMargin, CheckWidth, LabelWidth, EditLeft, EditWidth: Integer;
begin
  LeftMargin := ScaleX(20);
  CheckWidth := Page.SurfaceWidth - ScaleX(30);
  LabelWidth := ScaleX(140);
  EditLeft := ScaleX(170);
  EditWidth := ScaleX(200);

  TopPos := ScaleY(8);
  
  // Windows FS Error Monitor
  ChkWinFSErrorMonitor := TNewCheckBox.Create(Page);
  ChkWinFSErrorMonitor.Parent := Page.Surface;
  ChkWinFSErrorMonitor.Caption := 'Windows FS Error Monitor - Monitors Windows file system for errors';
  ChkWinFSErrorMonitor.Left := LeftMargin;
  ChkWinFSErrorMonitor.Top := TopPos;
  ChkWinFSErrorMonitor.Width := CheckWidth;
  ChkWinFSErrorMonitor.Height := ScaleY(21);
  ChkWinFSErrorMonitor.Checked := WasServiceInstalled('InstalledWinFSErrorMonitor', 'IPMonitoring_WinFSErrorMonitor');
  
  TopPos := TopPos + ScaleY(28);
  
  // Windows FS Cardinality Monitor
  ChkWinFSCardinalityMonitor := TNewCheckBox.Create(Page);
  ChkWinFSCardinalityMonitor.Parent := Page.Surface;
  ChkWinFSCardinalityMonitor.Caption := 'Windows FS Cardinality Monitor - Monitors file counts and sizes';
  ChkWinFSCardinalityMonitor.Left := LeftMargin;
  ChkWinFSCardinalityMonitor.Top := TopPos;
  ChkWinFSCardinalityMonitor.Width := CheckWidth;
  ChkWinFSCardinalityMonitor.Height := ScaleY(21);
  ChkWinFSCardinalityMonitor.Checked := WasServiceInstalled('InstalledWinFSCardinalityMonitor', 'IPMonitoring_WinFSCardinalityMonitor');
  
  TopPos := TopPos + ScaleY(40);
  
  // Windows Client Name (identifies which server/client is being monitored)
  LblWinClientName := TNewStaticText.Create(Page);
  LblWinClientName.Parent := Page.Surface;
  LblWinClientName.Caption := 'Client/Server Name:';
  LblWinClientName.Left := LeftMargin;
  LblWinClientName.Top := TopPos + ScaleY(3);
  LblWinClientName.AutoSize := True;
  
  WinClientNameEdit := TNewEdit.Create(Page);
  WinClientNameEdit.Parent := Page.Surface;
  WinClientNameEdit.Left := LblWinClientName.Left + LblWinClientName.Width + ScaleX(10);
  WinClientNameEdit.Top := TopPos;
  WinClientNameEdit.Width := EditWidth;
  WinClientNameEdit.Height := ScaleY(23);
  WinClientNameEdit.Text := GetClientRegistryValue('WinClientName', '');
  if WinClientNameEdit.Text = '' then
    WinClientNameEdit.Text := GetClientInstanceId('');
  if WinClientNameEdit.Text = '' then
    WinClientNameEdit.Text := 'DefaultClient';
  
  TopPos := TopPos + ScaleY(40);
  
  // Help text for client name
  with TNewStaticText.Create(Page) do
  begin
    Parent := Page.Surface;
    Caption := '(Used in email alerts to identify which server is being monitored)';
    Left := LeftMargin;
    Top := TopPos;
    Width := Page.SurfaceWidth - ScaleX(40);
    Height := ScaleY(30);
    AutoSize := False;
    WordWrap := True;
    Font.Size := 8;
    Font.Color := clGray;
  end;
end;

procedure CreateServicePortsControls(Page: TWizardPage);
var
  TopPos, LabelWidth, EditLeft, EditWidth: Integer;
  DefaultPort: string;
begin
  LabelWidth := 260;
  EditLeft := 270;
  EditWidth := 80;
  TopPos := 8;
  
  // IFS Error Monitor Port
  LblIFSErrorMonitorPort := TNewStaticText.Create(Page);
  LblIFSErrorMonitorPort.Parent := Page.Surface;
  LblIFSErrorMonitorPort.Caption := 'IFS Error Monitor Port:';
  LblIFSErrorMonitorPort.Left := 0;
  LblIFSErrorMonitorPort.Top := TopPos + 3;
  LblIFSErrorMonitorPort.Width := LabelWidth;
  
  DefaultPort := GetClientRegistryValue('IFSErrorMonitorPort', '');
  if DefaultPort = '' then
  begin
    if (ClientInstanceId <> '') and CheckClientExists(ClientInstanceId) then
      DefaultPort := '3010'
    else
      DefaultPort := GetOffsetPort(3010);
  end;
  
  IFSErrorMonitorPortEdit := TNewEdit.Create(Page);
  IFSErrorMonitorPortEdit.Parent := Page.Surface;
  IFSErrorMonitorPortEdit.Left := EditLeft;
  IFSErrorMonitorPortEdit.Top := TopPos;
  IFSErrorMonitorPortEdit.Width := EditWidth;
  IFSErrorMonitorPortEdit.Height := 23;
  IFSErrorMonitorPortEdit.Text := DefaultPort;
  
  TopPos := TopPos + 32;
  
  // Real-Time IFS Monitor Port
  LblRealTimeIFSMonitorPort := TNewStaticText.Create(Page);
  LblRealTimeIFSMonitorPort.Parent := Page.Surface;
  LblRealTimeIFSMonitorPort.Caption := 'Real-Time IFS Monitor Port:';
  LblRealTimeIFSMonitorPort.Left := 0;
  LblRealTimeIFSMonitorPort.Top := TopPos + 3;
  LblRealTimeIFSMonitorPort.Width := LabelWidth;
  
  DefaultPort := GetClientRegistryValue('RealTimeIFSMonitorPort', '');
  if DefaultPort = '' then
  begin
    if (ClientInstanceId <> '') and CheckClientExists(ClientInstanceId) then
      DefaultPort := '3011'
    else
      DefaultPort := GetOffsetPort(3011);
  end;
  
  RealTimeIFSMonitorPortEdit := TNewEdit.Create(Page);
  RealTimeIFSMonitorPortEdit.Parent := Page.Surface;
  RealTimeIFSMonitorPortEdit.Left := EditLeft;
  RealTimeIFSMonitorPortEdit.Top := TopPos;
  RealTimeIFSMonitorPortEdit.Width := EditWidth;
  RealTimeIFSMonitorPortEdit.Height := 23;
  RealTimeIFSMonitorPortEdit.Text := DefaultPort;
  
  TopPos := TopPos + 32;
  
  // Job Queue Count Monitor Port
  LblJobQueCountMonitorPort := TNewStaticText.Create(Page);
  LblJobQueCountMonitorPort.Parent := Page.Surface;
  LblJobQueCountMonitorPort.Caption := 'Job Queue Count Monitor Port:';
  LblJobQueCountMonitorPort.Left := 0;
  LblJobQueCountMonitorPort.Top := TopPos + 3;
  LblJobQueCountMonitorPort.Width := LabelWidth;
  
  DefaultPort := GetClientRegistryValue('JobQueCountMonitorPort', '');
  if DefaultPort = '' then
  begin
    if (ClientInstanceId <> '') and CheckClientExists(ClientInstanceId) then
      DefaultPort := '3012'
    else
      DefaultPort := GetOffsetPort(3012);
  end;
  
  JobQueCountMonitorPortEdit := TNewEdit.Create(Page);
  JobQueCountMonitorPortEdit.Parent := Page.Surface;
  JobQueCountMonitorPortEdit.Left := EditLeft;
  JobQueCountMonitorPortEdit.Top := TopPos;
  JobQueCountMonitorPortEdit.Width := EditWidth;
  JobQueCountMonitorPortEdit.Height := 23;
  JobQueCountMonitorPortEdit.Text := DefaultPort;
  
  TopPos := TopPos + 32;
  
  // Job Queue Status Monitor Port
  LblJobQueStatusMonitorPort := TNewStaticText.Create(Page);
  LblJobQueStatusMonitorPort.Parent := Page.Surface;
  LblJobQueStatusMonitorPort.Caption := 'Job Queue Status Monitor Port:';
  LblJobQueStatusMonitorPort.Left := 0;
  LblJobQueStatusMonitorPort.Top := TopPos + 3;
  LblJobQueStatusMonitorPort.Width := LabelWidth;
  
  DefaultPort := GetClientRegistryValue('JobQueStatusMonitorPort', '');
  if DefaultPort = '' then
  begin
    if (ClientInstanceId <> '') and CheckClientExists(ClientInstanceId) then
      DefaultPort := '3013'
    else
      DefaultPort := GetOffsetPort(3013);
  end;
  
  JobQueStatusMonitorPortEdit := TNewEdit.Create(Page);
  JobQueStatusMonitorPortEdit.Parent := Page.Surface;
  JobQueStatusMonitorPortEdit.Left := EditLeft;
  JobQueStatusMonitorPortEdit.Top := TopPos;
  JobQueStatusMonitorPortEdit.Width := EditWidth;
  JobQueStatusMonitorPortEdit.Height := 23;
  JobQueStatusMonitorPortEdit.Text := DefaultPort;
  
  TopPos := TopPos + 32;
  
  // Server UpTime Monitor Port
  LblServerUpTimeMonitorPort := TNewStaticText.Create(Page);
  LblServerUpTimeMonitorPort.Parent := Page.Surface;
  LblServerUpTimeMonitorPort.Caption := 'Server UpTime Monitor Port:';
  LblServerUpTimeMonitorPort.Left := 0;
  LblServerUpTimeMonitorPort.Top := TopPos + 3;
  LblServerUpTimeMonitorPort.Width := LabelWidth;
  
  DefaultPort := GetClientRegistryValue('ServerUpTimeMonitorPort', '');
  if DefaultPort = '' then
  begin
    if (ClientInstanceId <> '') and CheckClientExists(ClientInstanceId) then
      DefaultPort := '3014'
    else
      DefaultPort := GetOffsetPort(3014);
  end;
  
  ServerUpTimeMonitorPortEdit := TNewEdit.Create(Page);
  ServerUpTimeMonitorPortEdit.Parent := Page.Surface;
  ServerUpTimeMonitorPortEdit.Left := EditLeft;
  ServerUpTimeMonitorPortEdit.Top := TopPos;
  ServerUpTimeMonitorPortEdit.Width := EditWidth;
  ServerUpTimeMonitorPortEdit.Height := 23;
  ServerUpTimeMonitorPortEdit.Text := DefaultPort;
  
  TopPos := TopPos + 32;
  
  // SubSystem Monitor Port
  LblSubSystemMonitorPort := TNewStaticText.Create(Page);
  LblSubSystemMonitorPort.Parent := Page.Surface;
  LblSubSystemMonitorPort.Caption := 'SubSystem Monitor Port:';
  LblSubSystemMonitorPort.Left := 0;
  LblSubSystemMonitorPort.Top := TopPos + 3;
  LblSubSystemMonitorPort.Width := LabelWidth;
  
  DefaultPort := GetClientRegistryValue('SubSystemMonitorPort', '');
  if DefaultPort = '' then
  begin
    if (ClientInstanceId <> '') and CheckClientExists(ClientInstanceId) then
      DefaultPort := '3015'
    else
      DefaultPort := GetOffsetPort(3015);
  end;
  
  SubSystemMonitorPortEdit := TNewEdit.Create(Page);
  SubSystemMonitorPortEdit.Parent := Page.Surface;
  SubSystemMonitorPortEdit.Left := EditLeft;
  SubSystemMonitorPortEdit.Top := TopPos;
  SubSystemMonitorPortEdit.Width := EditWidth;
  SubSystemMonitorPortEdit.Height := 23;
  SubSystemMonitorPortEdit.Text := DefaultPort;
  
  TopPos := TopPos + 32;
  
  // System Matrix Port
  LblSystemMatrixPort := TNewStaticText.Create(Page);
  LblSystemMatrixPort.Parent := Page.Surface;
  LblSystemMatrixPort.Caption := 'System Matrix Port:';
  LblSystemMatrixPort.Left := 0;
  LblSystemMatrixPort.Top := TopPos + 3;
  LblSystemMatrixPort.Width := LabelWidth;
  
  DefaultPort := GetClientRegistryValue('SystemMatrixPort', '');
  if DefaultPort = '' then
  begin
    if (ClientInstanceId <> '') and CheckClientExists(ClientInstanceId) then
      DefaultPort := '3016'
    else
      DefaultPort := GetOffsetPort(3016);
  end;
  
  SystemMatrixPortEdit := TNewEdit.Create(Page);
  SystemMatrixPortEdit.Parent := Page.Surface;
  SystemMatrixPortEdit.Left := EditLeft;
  SystemMatrixPortEdit.Top := TopPos;
  SystemMatrixPortEdit.Width := EditWidth;
  SystemMatrixPortEdit.Height := 23;
  SystemMatrixPortEdit.Text := DefaultPort;
  
  TopPos := TopPos + 32;
  
  // User Profile Checker Port
  LblUserProfileCheckerPort := TNewStaticText.Create(Page);
  LblUserProfileCheckerPort.Parent := Page.Surface;
  LblUserProfileCheckerPort.Caption := 'User Profile Checker Port:';
  LblUserProfileCheckerPort.Left := 0;
  LblUserProfileCheckerPort.Top := TopPos + 3;
  LblUserProfileCheckerPort.Width := LabelWidth;
  
  DefaultPort := GetClientRegistryValue('UserProfileCheckerPort', '');
  if DefaultPort = '' then
  begin
    if (ClientInstanceId <> '') and CheckClientExists(ClientInstanceId) then
      DefaultPort := '3017'
    else
      DefaultPort := GetOffsetPort(3017);
  end;
  
  UserProfileCheckerPortEdit := TNewEdit.Create(Page);
  UserProfileCheckerPortEdit.Parent := Page.Surface;
  UserProfileCheckerPortEdit.Left := EditLeft;
  UserProfileCheckerPortEdit.Top := TopPos;
  UserProfileCheckerPortEdit.Width := EditWidth;
  UserProfileCheckerPortEdit.Height := 23;
  UserProfileCheckerPortEdit.Text := DefaultPort;
  
  TopPos := TopPos + 32;
  
  // Network Enabler Port
  LblNetWorkEnablerPort := TNewStaticText.Create(Page);
  LblNetWorkEnablerPort.Parent := Page.Surface;
  LblNetWorkEnablerPort.Caption := 'Network Enabler Port:';
  LblNetWorkEnablerPort.Left := 0;
  LblNetWorkEnablerPort.Top := TopPos + 3;
  LblNetWorkEnablerPort.Width := LabelWidth;
  
  DefaultPort := GetClientRegistryValue('NetWorkEnablerPort', '');
  if DefaultPort = '' then
  begin
    if (ClientInstanceId <> '') and CheckClientExists(ClientInstanceId) then
      DefaultPort := '3018'
    else
      DefaultPort := GetOffsetPort(3018);
  end;
  
  NetWorkEnablerPortEdit := TNewEdit.Create(Page);
  NetWorkEnablerPortEdit.Parent := Page.Surface;
  NetWorkEnablerPortEdit.Left := EditLeft;
  NetWorkEnablerPortEdit.Top := TopPos;
  NetWorkEnablerPortEdit.Width := EditWidth;
  NetWorkEnablerPortEdit.Height := 23;
  NetWorkEnablerPortEdit.Text := DefaultPort;
  
  TopPos := TopPos + 32;
  
  // QSYSOPR Monitor Port
  LblQSYSOPRMonitorPort := TNewStaticText.Create(Page);
  LblQSYSOPRMonitorPort.Parent := Page.Surface;
  LblQSYSOPRMonitorPort.Caption := 'QSYSOPR Monitor Port:';
  LblQSYSOPRMonitorPort.Left := 0;
  LblQSYSOPRMonitorPort.Top := TopPos + 3;
  LblQSYSOPRMonitorPort.Width := LabelWidth;
  
  DefaultPort := GetClientRegistryValue('QSYSOPRMonitorPort', '');
  if DefaultPort = '' then
  begin
    if (ClientInstanceId <> '') and CheckClientExists(ClientInstanceId) then
      DefaultPort := '3019'
    else
      DefaultPort := GetOffsetPort(3019);
  end;
  
  QSYSOPRMonitorPortEdit := TNewEdit.Create(Page);
  QSYSOPRMonitorPortEdit.Parent := Page.Surface;
  QSYSOPRMonitorPortEdit.Left := EditLeft;
  QSYSOPRMonitorPortEdit.Top := TopPos;
  QSYSOPRMonitorPortEdit.Width := EditWidth;
  QSYSOPRMonitorPortEdit.Height := 23;
  QSYSOPRMonitorPortEdit.Text := DefaultPort;
  
  TopPos := TopPos + 32;
  
  // Windows FS Error Monitor Port
  LblWinFSErrorMonitorPort := TNewStaticText.Create(Page);
  LblWinFSErrorMonitorPort.Parent := Page.Surface;
  LblWinFSErrorMonitorPort.Caption := 'Windows FS Error Monitor Port:';
  LblWinFSErrorMonitorPort.Left := 0;
  LblWinFSErrorMonitorPort.Top := TopPos + 3;
  LblWinFSErrorMonitorPort.Width := LabelWidth;
  
  DefaultPort := GetClientRegistryValue('WinFSErrorMonitorPort', '');
  if DefaultPort = '' then
  begin
    if (ClientInstanceId <> '') and CheckClientExists(ClientInstanceId) then
      DefaultPort := '3020'
    else
      DefaultPort := GetOffsetPort(3020);
  end;
  
  WinFSErrorMonitorPortEdit := TNewEdit.Create(Page);
  WinFSErrorMonitorPortEdit.Parent := Page.Surface;
  WinFSErrorMonitorPortEdit.Left := EditLeft;
  WinFSErrorMonitorPortEdit.Top := TopPos;
  WinFSErrorMonitorPortEdit.Width := EditWidth;
  WinFSErrorMonitorPortEdit.Height := 23;
  WinFSErrorMonitorPortEdit.Text := DefaultPort;
  
  TopPos := TopPos + 32;
  
  // Windows FS Cardinality Monitor Port
  LblWinFSCardinalityMonitorPort := TNewStaticText.Create(Page);
  LblWinFSCardinalityMonitorPort.Parent := Page.Surface;
  LblWinFSCardinalityMonitorPort.Caption := 'Windows FS Cardinality Monitor Port:';
  LblWinFSCardinalityMonitorPort.Left := 0;
  LblWinFSCardinalityMonitorPort.Top := TopPos + 3;
  LblWinFSCardinalityMonitorPort.Width := LabelWidth;
  
  DefaultPort := GetClientRegistryValue('WinFSCardinalityMonitorPort', '');
  if DefaultPort = '' then
  begin
    if (ClientInstanceId <> '') and CheckClientExists(ClientInstanceId) then
      DefaultPort := '3021'
    else
      DefaultPort := GetOffsetPort(3021);
  end;
  
  WinFSCardinalityMonitorPortEdit := TNewEdit.Create(Page);
  WinFSCardinalityMonitorPortEdit.Parent := Page.Surface;
  WinFSCardinalityMonitorPortEdit.Left := EditLeft;
  WinFSCardinalityMonitorPortEdit.Top := TopPos;
  WinFSCardinalityMonitorPortEdit.Width := EditWidth;
  WinFSCardinalityMonitorPortEdit.Height := 23;
  WinFSCardinalityMonitorPortEdit.Text := DefaultPort;
end;

// ===== LOG AGENT (PROMTAIL) CONFIGURATION CONTROLS =====
procedure CreateLogAgentConfigControls(Page: TWizardPage);
var
  TopPos: Integer;
  SectionHeader, HelperText: TNewStaticText;
  Separator: TBevel;
  HostLabel, PortLabel, AppNameLabel, EnvLabel, LogPathLabel: TNewStaticText;
begin
  TopPos := 0;
  
  // Section Header: Loki Server Connection
  SectionHeader := CreateSectionHeader(Page, 'Loki Server Connection', TopPos);
  Separator := CreateSeparator(Page, TopPos);
  HelperText := CreateHelperText(Page, 'Enter the IP address or hostname of the Monitoring Server where Loki is installed.', TopPos);
  TopPos := TopPos + 4;
  
  // Loki Host
  HostLabel := CreateFieldLabel(Page, 'Monitoring Server Host/IP:', TopPos);
  LogAgentHostEdit := CreateTextEdit(Page, GetClientRegistryValue('LokiHost', 'localhost'), False, TopPos);
  
  // Loki Port
  PortLabel := CreateFieldLabel(Page, 'Loki Port:', TopPos);
  LogAgentPortEdit := CreateTextEdit(Page, GetClientRegistryValue('LokiPort', '3100'), False, TopPos);
  
  TopPos := TopPos + 6;
  
  // Section Header: Application Settings
  SectionHeader := CreateSectionHeader(Page, 'Application Settings', TopPos);
  Separator := CreateSeparator(Page, TopPos);
  HelperText := CreateHelperText(Page, 'These labels help identify logs in Loki/Grafana dashboards.', TopPos);
  TopPos := TopPos + 4;
  
  // Application Name
  AppNameLabel := CreateFieldLabel(Page, 'Application Name:', TopPos);
  LogAgentAppNameEdit := CreateTextEdit(Page, GetClientRegistryValue('LogAgentAppName', 'MyApp'), False, TopPos);
  
  // Environment dropdown
  EnvLabel := CreateFieldLabel(Page, 'Environment:', TopPos);
  
  LogAgentEnvironmentCombo := TNewComboBox.Create(Page);
  LogAgentEnvironmentCombo.Parent := Page.Surface;
  LogAgentEnvironmentCombo.Left := 0;
  LogAgentEnvironmentCombo.Top := TopPos;
  LogAgentEnvironmentCombo.Width := Page.SurfaceWidth;
  LogAgentEnvironmentCombo.Style := csDropDownList;
  LogAgentEnvironmentCombo.Items.Add('DEV');
  LogAgentEnvironmentCombo.Items.Add('QA');
  LogAgentEnvironmentCombo.Items.Add('UAT');
  LogAgentEnvironmentCombo.Items.Add('PROD');
  // Set selected value from registry or default
  case GetClientRegistryValue('LogAgentEnvironment', 'PROD') of
    'DEV': LogAgentEnvironmentCombo.ItemIndex := 0;
    'QA': LogAgentEnvironmentCombo.ItemIndex := 1;
    'UAT': LogAgentEnvironmentCombo.ItemIndex := 2;
  else
    LogAgentEnvironmentCombo.ItemIndex := 3; // PROD
  end;
  TopPos := TopPos + 36;
  
  TopPos := TopPos + 6;
  
  // Section Header: Log Path
  SectionHeader := CreateSectionHeader(Page, 'Log File Path', TopPos);
  Separator := CreateSeparator(Page, TopPos);
  HelperText := CreateHelperText(Page, 'Use forward slashes. Wildcards supported (e.g., C:/logs/*.log or C:/app/logs/**/*.log).', TopPos);
  TopPos := TopPos + 4;
  
  // Log Path
  LogPathLabel := CreateFieldLabel(Page, 'Log Path Pattern:', TopPos);
  LogAgentLogPathEdit := CreateTextEdit(Page, GetClientRegistryValue('LogAgentLogPath', 'C:/logs/*.log'), False, TopPos);
end;

procedure CreateSmtpConfigControls(Page: TWizardPage);
var
  TopPos, EditWidth: Integer;
  HostLabel, PortLabel, UsernameLabel, PasswordLabel: TNewStaticText;
begin
  EditWidth := Page.SurfaceWidth;
  TopPos := 8;
  
  // SMTP Host
  HostLabel := TNewStaticText.Create(Page);
  HostLabel.Parent := Page.Surface;
  HostLabel.Caption := 'SMTP Host:';
  HostLabel.Left := 0;
  HostLabel.Top := TopPos;
  HostLabel.Font.Style := [fsBold];
  
  TopPos := TopPos + 20;
  
  SmtpHostEdit := TNewEdit.Create(Page);
  SmtpHostEdit.Parent := Page.Surface;
  SmtpHostEdit.Left := 0;
  SmtpHostEdit.Top := TopPos;
  SmtpHostEdit.Width := EditWidth;
  SmtpHostEdit.Height := 23;
  SmtpHostEdit.Text := GetClientRegistryValue('SmtpHost', 'smtp.office365.com');
  
  TopPos := TopPos + 45;
  
  // SMTP Port
  PortLabel := TNewStaticText.Create(Page);
  PortLabel.Parent := Page.Surface;
  PortLabel.Caption := 'SMTP Port:';
  PortLabel.Left := 0;
  PortLabel.Top := TopPos;
  PortLabel.Font.Style := [fsBold];
  
  TopPos := TopPos + 20;
  
  SmtpPortEdit := TNewEdit.Create(Page);
  SmtpPortEdit.Parent := Page.Surface;
  SmtpPortEdit.Left := 0;
  SmtpPortEdit.Top := TopPos;
  SmtpPortEdit.Width := EditWidth;
  SmtpPortEdit.Height := 23;
  SmtpPortEdit.Text := GetClientRegistryValue('SmtpPort', '587');
  
  TopPos := TopPos + 45;
  
  // Username
  UsernameLabel := TNewStaticText.Create(Page);
  UsernameLabel.Parent := Page.Surface;
  UsernameLabel.Caption := 'Username:';
  UsernameLabel.Left := 0;
  UsernameLabel.Top := TopPos;
  UsernameLabel.Font.Style := [fsBold];
  
  TopPos := TopPos + 20;
  
  SmtpUsernameEdit := TNewEdit.Create(Page);
  SmtpUsernameEdit.Parent := Page.Surface;
  SmtpUsernameEdit.Left := 0;
  SmtpUsernameEdit.Top := TopPos;
  SmtpUsernameEdit.Width := EditWidth;
  SmtpUsernameEdit.Height := 23;
  SmtpUsernameEdit.Text := GetClientRegistryValue('SmtpUsername', '');
  
  TopPos := TopPos + 45;
  
  // Password
  PasswordLabel := TNewStaticText.Create(Page);
  PasswordLabel.Parent := Page.Surface;
  PasswordLabel.Caption := 'Password:';
  PasswordLabel.Left := 0;
  PasswordLabel.Top := TopPos;
  PasswordLabel.Font.Style := [fsBold];
  
  TopPos := TopPos + 20;
  
  SmtpPasswordEdit := TNewEdit.Create(Page);
  SmtpPasswordEdit.Parent := Page.Surface;
  SmtpPasswordEdit.Left := 0;
  SmtpPasswordEdit.Top := TopPos;
  SmtpPasswordEdit.Width := EditWidth;
  SmtpPasswordEdit.Height := 23;
  SmtpPasswordEdit.PasswordChar := '*';
  SmtpPasswordEdit.Text := GetClientRegistryValue('SmtpPassword', '');
  
  TopPos := TopPos + 48;
  
  // SMTP Auth Checkbox
  SmtpAuthCheckbox := TNewCheckBox.Create(Page);
  SmtpAuthCheckbox.Parent := Page.Surface;
  SmtpAuthCheckbox.Caption := 'Enable SMTP Authentication (mail.smtp.auth)';
  SmtpAuthCheckbox.Left := 0;
  SmtpAuthCheckbox.Top := TopPos;
  SmtpAuthCheckbox.Width := EditWidth;
  SmtpAuthCheckbox.Height := 20;
  SmtpAuthCheckbox.Checked := (GetClientRegistryValue('SmtpAuth', 'true') = 'true');
  
  TopPos := TopPos + 30;
  
  // StartTLS Checkbox
  SmtpStartTlsCheckbox := TNewCheckBox.Create(Page);
  SmtpStartTlsCheckbox.Parent := Page.Surface;
  SmtpStartTlsCheckbox.Caption := 'Enable STARTTLS (mail.smtp.starttls.enable)';
  SmtpStartTlsCheckbox.Left := 0;
  SmtpStartTlsCheckbox.Top := TopPos;
  SmtpStartTlsCheckbox.Width := EditWidth;
  SmtpStartTlsCheckbox.Height := 20;
  SmtpStartTlsCheckbox.Checked := (GetClientRegistryValue('SmtpStartTls', 'true') = 'true');
end;

// OAuth Page 1: Azure AD Application Credentials
procedure CreateOAuthConfigControls1(Page: TWizardPage);
var
  TopPos: Integer;
  TenantLabel, ClientIdLabel, ClientSecretLabel, TokenUrlLabel: TNewStaticText;
  SectionHeader, HelperText, TokenHelper: TNewStaticText;
  Separator: TBevel;
  TenantId: string;
begin
  TopPos := 0;
  
  // Section Header: App Registration
  SectionHeader := CreateSectionHeader(Page, 'Azure App Registration', TopPos);
  Separator := CreateSeparator(Page, TopPos);
  HelperText := CreateHelperText(Page, 'Find these values in Azure Portal > App registrations > Your App > Overview', TopPos);
  TopPos := TopPos + 4;
  
  // Tenant ID
  TenantLabel := CreateFieldLabel(Page, 'Tenant ID (Directory ID):', TopPos);
  TenantId := GetClientRegistryValue('OAuthTenant', '');
  OAuthTenantEdit := CreateTextEdit(Page, TenantId, False, TopPos);
  
  // Client ID
  ClientIdLabel := CreateFieldLabel(Page, 'Client ID (Application ID):', TopPos);
  OAuthClientIdEdit := CreateTextEdit(Page, GetClientRegistryValue('OAuthClientId', ''), False, TopPos);
  
  // Client Secret
  ClientSecretLabel := CreateFieldLabel(Page, 'Client Secret:', TopPos);
  OAuthClientSecretEdit := CreateTextEdit(Page, GetClientRegistryValue('OAuthClientSecret', ''), True, TopPos);
  
  TopPos := TopPos + 6;
  
  // Section Header: Token Endpoint
  SectionHeader := CreateSectionHeader(Page, 'Token Endpoint', TopPos);
  Separator := CreateSeparator(Page, TopPos);
  TokenHelper := CreateHelperText(Page, 'Usually auto-generated. Leave blank to use the default Microsoft endpoint.', TopPos);
  TopPos := TopPos + 4;
  
  // Token URL with smart default
  TokenUrlLabel := CreateFieldLabel(Page, 'Token URL (optional):', TopPos);
  OAuthTokenUrlEdit := CreateTextEdit(Page, GetClientRegistryValue('OAuthTokenUrl', ''), False, TopPos);
  // Set placeholder-style hint
  if (OAuthTokenUrlEdit.Text = '') and (TenantId <> '') then
    OAuthTokenUrlEdit.Text := 'https://login.microsoftonline.com/' + TenantId + '/oauth2/v2.0/token';
end;

// OAuth Page 2: Microsoft Graph API Settings
procedure CreateOAuthConfigControls2(Page: TWizardPage);
var
  TopPos: Integer;
  MailUrlLabel, ScopeLabel, FromUserLabel: TNewStaticText;
  SectionHeader, HelperText, ScopeHelper: TNewStaticText;
  Separator: TBevel;
  FromUser: string;
begin
  TopPos := 0;
  
  // Section Header: Graph API
  SectionHeader := CreateSectionHeader(Page, 'Microsoft Graph API', TopPos);
  Separator := CreateSeparator(Page, TopPos);
  HelperText := CreateHelperText(Page, 'Configure how emails are sent via Microsoft 365.', TopPos);
  TopPos := TopPos + 4;
  
  // From User Email (moved up as it's most important)
  FromUserLabel := CreateFieldLabel(Page, 'Sender Email Address:', TopPos);
  FromUser := GetClientRegistryValue('OAuthFromUser', '');
  OAuthFromUserEdit := CreateTextEdit(Page, FromUser, False, TopPos);
  
  TopPos := TopPos + 6;
  
  // Section Header: Advanced Settings (typically don't need changing)
  SectionHeader := CreateSectionHeader(Page, 'Advanced Settings', TopPos);
  Separator := CreateSeparator(Page, TopPos);
  ScopeHelper := CreateHelperText(Page, 'These defaults work for most Microsoft 365 configurations.', TopPos);
  TopPos := TopPos + 4;
  
  // Mail URL with smart default
  MailUrlLabel := CreateFieldLabel(Page, 'Mail Send URL:', TopPos);
  OAuthMailUrlEdit := CreateTextEdit(Page, GetClientRegistryValue('OAuthMailUrl', ''), False, TopPos);
  // Auto-fill based on From User
  if (OAuthMailUrlEdit.Text = '') and (FromUser <> '') then
    OAuthMailUrlEdit.Text := 'https://graph.microsoft.com/v1.0/users/' + FromUser + '/sendMail';
  
  // Scope with sensible default
  ScopeLabel := CreateFieldLabel(Page, 'OAuth Scope:', TopPos);
  OAuthScopeEdit := CreateTextEdit(Page, GetClientRegistryValue('OAuthScope', 'https://graph.microsoft.com/.default'), False, TopPos);
end;

// Helper function for finish page to show Grafana URL
function GetGrafanaPort(Param: String): String;
begin
  Result := GetClientRegistryValue('GrafanaPort', '3000');
end;

function InitializeSetup(): Boolean;
begin
  // NOTE: ClientInstanceId is now initialized directly in GetClientInstanceId
  // which is called from [Setup] section before this function runs.
  // InitClientInstanceId is kept for backward compatibility with other callers.

  if not IsWin64 then
  begin
    MsgBox('64-bit Windows required.', mbCriticalError, MB_OK);
    Result := False;
    Exit;
  end;
  // Detect if this is an upgrade (previous installation exists)
  IsUpgrade := CheckIsUpgrade;
  
  // For Monitoring Server role: check if Prometheus/Grafana actually exist
  // For other roles: just check if any installation exists
  if IsMonitoringServerRole then
  begin
    // Monitoring Server needs Prometheus/Grafana to reuse existing installation
    // If only Windows/LogAgent installed before, that doesn't count as "base installation"
    IsFirstInstallation := not (WasServiceInstalled('InstalledPrometheus', 'IPMonitoringPrometheus') or
                               WasServiceInstalled('InstalledGrafana', 'IPMonitoringGrafana'));
  end
  else
  begin
    // For Windows/LogAgent roles, just check if any prior installation exists
    IsFirstInstallation := not CheckBaseInstallationExists;
  end;
  UseExistingGrafana := not IsFirstInstallation;
  
  // Calculate port offset based on number of existing client instances
  ClientPortOffset := GetInstalledClientCount * 100;
  // Do not offset ports when performing an upgrade of an existing client
  if IsUpgrade then
    ClientPortOffset := 0;

  // If exactly one client exists, preload its id so registry reads use its values
  LoadDefaultClientInstanceId;
  
  // Check if core services were previously installed (they cannot be uninstalled)
  PrometheusInstalled := WasServiceInstalled('InstalledPrometheus', 'IPMonitoringPrometheus');
  GrafanaInstalled := WasServiceInstalled('InstalledGrafana', 'IPMonitoringGrafana');
  LokiInstalled := WasServiceInstalled('InstalledLoki', 'IPMonitoring-Loki');
  PromtailInstalled := WasServiceInstalled('InstalledLogAgent', 'IPMonitoring-LogAgent');
  
  Result := True;
end;

function IsFileLocked(const FilePath: string): Boolean; forward;
procedure WaitForFileUnlock(const FilePath: string; Attempts, DelayMs: Integer); forward;

function PrepareToInstall(var NeedsRestart: Boolean): String;
var
  ResultCode: Integer;
  ServicesDir, ClientSuffix, AppDir, EscapedAppDir: string;
begin
  Result := '';
  NeedsRestart := False;

  ServicesDir := ExpandConstant('{app}\services');
  AppDir := ExpandConstant('{app}');
  
  // Escape backslashes for WMIC command line filter
  EscapedAppDir := AppDir;
  StringChangeEx(EscapedAppDir, '\', '\\', True);
  
  // Determine client suffix for multi-client services
  if ClientInstanceId <> '' then
    ClientSuffix := '_' + ClientInstanceId
  else
    ClientSuffix := '';

  // ROLE-SPECIFIC SERVICE STOPPING:
  // Only stop services relevant to the current role being installed.
  // This prevents stopping unrelated services from other roles/clients.
  
  // Stop ONLY the role-relevant services for THIS client
  if IsMonitoringServerRole then
  begin
    // Stop core services (Prometheus, Grafana, Loki, Promtail) for monitoring server
    Exec('sc.exe', 'stop IPMonitoringPrometheus' + ClientSuffix, '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
    Exec('sc.exe', 'stop IPMonitoringGrafana' + ClientSuffix, '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
    Exec('sc.exe', 'stop IPMonitoring-Loki' + ClientSuffix, '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
    Exec('sc.exe', 'stop IPMonitoring-LogAgent' + ClientSuffix, '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
    
    // Stop IBM monitor services
    Exec('sc.exe', 'stop IPMonitoring_IBMIFSErrorMonitor' + ClientSuffix, '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
    Exec('sc.exe', 'stop IPMonitoring_IBMRealTimeIFSMonitor' + ClientSuffix, '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
    Exec('sc.exe', 'stop IPMonitoring_IBMJobQueCountMonitor' + ClientSuffix, '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
    Exec('sc.exe', 'stop IPMonitoring_IBMJobQueStatusMonitor' + ClientSuffix, '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
    Exec('sc.exe', 'stop IPMonitoring_ServerUpTimeMonitor' + ClientSuffix, '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
    Exec('sc.exe', 'stop IPMonitoring_IBMSubSystemMonitoring' + ClientSuffix, '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
    Exec('sc.exe', 'stop IPMonitoring_IBMSystemMatrix' + ClientSuffix, '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
    Exec('sc.exe', 'stop IPMonitoring_IBMUserProfileChecker' + ClientSuffix, '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
    Exec('sc.exe', 'stop IPMonitoring_NetWorkEnabler' + ClientSuffix, '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
    Exec('sc.exe', 'stop IPMonitoring_QSYSOPRMonitoring' + ClientSuffix, '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
    
    // Also try legacy non-suffixed names for backward compatibility with older installs
    if IsUpgrade then
    begin
      Exec('sc.exe', 'stop IPMonitoringPrometheus', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
      Exec('sc.exe', 'stop IPMonitoringGrafana', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
      Exec('sc.exe', 'stop IPMonitoring-Loki', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
      Exec('sc.exe', 'stop IPMonitoring-LogAgent', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
      Exec('sc.exe', 'stop IPMonitoring_IBMIFSErrorMonitor', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
      Exec('sc.exe', 'stop IPMonitoring_IBMRealTimeIFSMonitor', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
      Exec('sc.exe', 'stop IPMonitoring_IBMJobQueCountMonitor', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
      Exec('sc.exe', 'stop IPMonitoring_IBMJobQueStatusMonitor', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
      Exec('sc.exe', 'stop IPMonitoring_ServerUpTimeMonitor', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
      Exec('sc.exe', 'stop IPMonitoring_IBMSubSystemMonitoring', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
      Exec('sc.exe', 'stop IPMonitoring_IBMSystemMatrix', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
      Exec('sc.exe', 'stop IPMonitoring_IBMUserProfileChecker', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
      Exec('sc.exe', 'stop IPMonitoring_NetWorkEnabler', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
      Exec('sc.exe', 'stop IPMonitoring_QSYSOPRMonitoring', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
    end;
  end
  else if IsWinAgentRole then
  begin
    // Windows File Agent: only stop Windows FS monitors
    Exec('sc.exe', 'stop IPMonitoring_WinFSErrorMonitor' + ClientSuffix, '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
    Exec('sc.exe', 'stop IPMonitoring_WinFSCardinalityMonitor' + ClientSuffix, '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
    // Do NOT stop IBM, Prometheus, Grafana, Loki, or Promtail on WinAgent installs
  end
  else if IsLogAgentRole then
  begin
    // Log Agent: only stop Promtail
    Exec('sc.exe', 'stop IPMonitoring-LogAgent' + ClientSuffix, '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
    // Do NOT stop any other services on LogAgent installs
  end;

  // Wait for services to stop
  Sleep(3000);

  // Force kill ONLY Java processes running from OUR install directory
  // IMPORTANT: Do NOT use generic taskkill /F /IM java.exe - this would kill unrelated Java apps (ERP, etc.)
  // Use WMIC with strict working directory filter to target only our monitoring services
  // WMIC requires escaped backslashes in path filters
  Exec(ExpandConstant('{cmd}'), '/C wmic process where "commandline like ''%' + EscapedAppDir + '%'' and name like ''java%''" call terminate 2>nul', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
  // Also try with monitoring-services in path (more specific, less escaping issues)
  Exec(ExpandConstant('{cmd}'), '/C wmic process where "commandline like ''%monitoring-services%'' and name like ''java%''" call terminate 2>nul', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
  
  // Extra wait after killing processes
  Sleep(3000);

  // Per-file retry/backoff for all JARs (longer wait for Java to release)
  WaitForFileUnlock(ExpandConstant('{app}\monitoring-services\IBMIFSErrorMonitor\IBMIFSErrorMonitor.jar'), 15, 1000);
  WaitForFileUnlock(ExpandConstant('{app}\monitoring-services\IBMRealTimeIFSMonitor\IBMRealTimeIFSMonitor.jar'), 15, 1000);
  WaitForFileUnlock(ExpandConstant('{app}\monitoring-services\IBMJobQueCountMonitor\IBMJobQueCountMonitor.jar'), 15, 1000);
  WaitForFileUnlock(ExpandConstant('{app}\monitoring-services\IBMJobQueStatusMonitor\IBMJobQueStatusMonitor.jar'), 15, 1000);
  WaitForFileUnlock(ExpandConstant('{app}\monitoring-services\ServerUpTimeMonitor\ServerUpTimeMonitor.jar'), 15, 1000);
  WaitForFileUnlock(ExpandConstant('{app}\monitoring-services\IBMSubSystemMonitoring\IBMSubSystemMonitoring.jar'), 15, 1000);
  WaitForFileUnlock(ExpandConstant('{app}\monitoring-services\IBMSystemMatrix\IBMSystemMatrix.jar'), 15, 1000);
  WaitForFileUnlock(ExpandConstant('{app}\monitoring-services\IBMUserProfileChecker\IBMUserProfileChecker.jar'), 15, 1000);
  WaitForFileUnlock(ExpandConstant('{app}\monitoring-services\NetWorkEnabler\NetWorkEnabler.jar'), 15, 1000);
  WaitForFileUnlock(ExpandConstant('{app}\monitoring-services\QSYSOPRMonitoring\QSYSOPRMonitoring.jar'), 15, 1000);

  // Per-file retry/backoff for service executables
  WaitForFileUnlock(ExpandConstant('{app}\services\IPMonitoring_IBMIFSErrorMonitor.exe'), 8, 500);
  WaitForFileUnlock(ExpandConstant('{app}\services\IPMonitoring_IBMRealTimeIFSMonitor.exe'), 8, 500);
  WaitForFileUnlock(ExpandConstant('{app}\services\IPMonitoring_IBMJobQueCountMonitor.exe'), 8, 500);
  WaitForFileUnlock(ExpandConstant('{app}\services\IPMonitoring_IBMJobQueStatusMonitor.exe'), 8, 500);
  WaitForFileUnlock(ExpandConstant('{app}\services\IPMonitoring_ServerUpTimeMonitor.exe'), 8, 500);
  WaitForFileUnlock(ExpandConstant('{app}\services\IPMonitoring_IBMSubSystemMonitoring.exe'), 8, 500);
  WaitForFileUnlock(ExpandConstant('{app}\services\IPMonitoring_IBMSystemMatrix.exe'), 8, 500);
  WaitForFileUnlock(ExpandConstant('{app}\services\IPMonitoring_IBMUserProfileChecker.exe'), 8, 500);
  WaitForFileUnlock(ExpandConstant('{app}\services\IPMonitoring_NetWorkEnabler.exe'), 8, 500);
  WaitForFileUnlock(ExpandConstant('{app}\services\IPMonitoring_QSYSOPRMonitoring.exe'), 8, 500);
end;

function IsPortInUse(Port: string): Boolean;
var
  Code: Integer;
begin
  // IMPORTANT: Use findstr /R with regex to match exact port number
  // The pattern ':PORT ' (with trailing space) ensures we don't match partial ports
  // e.g., searching for :3000 won't falsely match :30001 or :13000
  // The space after the port separates Address from State/PID in netstat output
  Exec(ExpandConstant('{cmd}'),
    '/C netstat -ano | findstr LISTENING | findstr /R ":' + Port + ' "',
    '', SW_HIDE, ewWaitUntilTerminated, Code);
  Result := (Code = 0);
end;

function CreateFileW(lpFileName: string; dwDesiredAccess, dwShareMode, lpSecurityAttributes: Integer; dwCreationDisposition, dwFlagsAndAttributes, hTemplateFile: Integer): Integer;
  external 'CreateFileW@kernel32.dll stdcall';

function CloseHandle(hObject: Integer): Integer;
  external 'CloseHandle@kernel32.dll stdcall';

function IsFileLocked(const FilePath: string): Boolean;
var
  H: Integer;
begin
  if not FileExists(FilePath) then
  begin
    Result := False;
    Exit;
  end;

  H := CreateFileW(FilePath, GENERIC_WRITE, FILE_SHARE_READ or FILE_SHARE_WRITE,
    0, OPEN_EXISTING, 0, 0);

  if H = INVALID_HANDLE_VALUE then
    Result := True
  else
  begin
    CloseHandle(H);
    Result := False;
  end;
end;

procedure WaitForFileUnlock(const FilePath: string; Attempts, DelayMs: Integer);
var
  I: Integer;
begin
  for I := 1 to Attempts do
  begin
    if not IsFileLocked(FilePath) then
      Exit;
    Sleep(DelayMs);
  end;
end;

procedure ApplyClientSettingsFromRegistry;
var
  Importance: string;
begin
  // Core ports and credentials
  ConfigPage.Values[0] := GetClientRegistryValue('PrometheusPort', '9090');
  ConfigPage.Values[1] := GetClientRegistryValue('GrafanaPort', '3000');
  ConfigPage.Values[2] := GetClientRegistryValue('GrafanaAdminPassword', 'admin');

  // Monitor ports
  IFSErrorMonitorPortEdit.Text := GetClientRegistryValue('IFSErrorMonitorPort', IFSErrorMonitorPortEdit.Text);
  RealTimeIFSMonitorPortEdit.Text := GetClientRegistryValue('RealTimeIFSMonitorPort', RealTimeIFSMonitorPortEdit.Text);
  JobQueCountMonitorPortEdit.Text := GetClientRegistryValue('JobQueCountMonitorPort', JobQueCountMonitorPortEdit.Text);
  JobQueStatusMonitorPortEdit.Text := GetClientRegistryValue('JobQueStatusMonitorPort', JobQueStatusMonitorPortEdit.Text);
  ServerUpTimeMonitorPortEdit.Text := GetClientRegistryValue('ServerUpTimeMonitorPort', ServerUpTimeMonitorPortEdit.Text);
  SubSystemMonitorPortEdit.Text := GetClientRegistryValue('SubSystemMonitorPort', SubSystemMonitorPortEdit.Text);
  SystemMatrixPortEdit.Text := GetClientRegistryValue('SystemMatrixPort', SystemMatrixPortEdit.Text);
  UserProfileCheckerPortEdit.Text := GetClientRegistryValue('UserProfileCheckerPort', UserProfileCheckerPortEdit.Text);
  NetWorkEnablerPortEdit.Text := GetClientRegistryValue('NetWorkEnablerPort', NetWorkEnablerPortEdit.Text);
  QSYSOPRMonitorPortEdit.Text := GetClientRegistryValue('QSYSOPRMonitorPort', QSYSOPRMonitorPortEdit.Text);
  WinFSErrorMonitorPortEdit.Text := GetClientRegistryValue('WinFSErrorMonitorPort', WinFSErrorMonitorPortEdit.Text);
  WinFSCardinalityMonitorPortEdit.Text := GetClientRegistryValue('WinFSCardinalityMonitorPort', WinFSCardinalityMonitorPortEdit.Text);

  // IBM i connection
  IBMiConfigPage.Values[0] := GetClientRegistryValue('IBMiServer', IBMiConfigPage.Values[0]);
  IBMiConfigPage.Values[1] := GetClientRegistryValue('IBMiUser', IBMiConfigPage.Values[1]);
  IBMiConfigPage.Values[2] := GetClientRegistryValue('IBMiPassword', IBMiConfigPage.Values[2]);
  IBMiConfigPage.Values[3] := GetClientRegistryValue('ClientName', IBMiConfigPage.Values[3]);

  // Email auth selection
  if GetClientRegistryValue('EmailAuthMethod', 'SMTP') = 'OAUTH2' then
    EmailAuthMethodPage.SelectedValueIndex := 1
  else
    EmailAuthMethodPage.SelectedValueIndex := 0;

  // SMTP settings
  SmtpHostEdit.Text := GetClientRegistryValue('SmtpHost', SmtpHostEdit.Text);
  SmtpPortEdit.Text := GetClientRegistryValue('SmtpPort', SmtpPortEdit.Text);
  SmtpUsernameEdit.Text := GetClientRegistryValue('SmtpUsername', SmtpUsernameEdit.Text);
  SmtpPasswordEdit.Text := GetClientRegistryValue('SmtpPassword', SmtpPasswordEdit.Text);
  SmtpAuthCheckbox.Checked := (GetClientRegistryValue('SmtpAuth', 'true') = 'true');
  SmtpStartTlsCheckbox.Checked := (GetClientRegistryValue('SmtpStartTls', 'true') = 'true');

  // OAuth settings
  OAuthTenantEdit.Text := GetClientRegistryValue('OAuthTenant', OAuthTenantEdit.Text);
  OAuthClientIdEdit.Text := GetClientRegistryValue('OAuthClientId', OAuthClientIdEdit.Text);
  OAuthClientSecretEdit.Text := GetClientRegistryValue('OAuthClientSecret', OAuthClientSecretEdit.Text);
  OAuthTokenUrlEdit.Text := GetClientRegistryValue('OAuthTokenUrl', OAuthTokenUrlEdit.Text);
  OAuthFromUserEdit.Text := GetClientRegistryValue('OAuthFromUser', OAuthFromUserEdit.Text);
  OAuthMailUrlEdit.Text := GetClientRegistryValue('OAuthMailUrl', OAuthMailUrlEdit.Text);
  OAuthScopeEdit.Text := GetClientRegistryValue('OAuthScope', OAuthScopeEdit.Text);

  // Email recipients/sender
  EmailToEdit.Text := GetClientRegistryValue('EmailTo', EmailToEdit.Text);
  EmailBccEdit.Text := GetClientRegistryValue('EmailBcc', EmailBccEdit.Text);
  EmailFromEdit.Text := GetClientRegistryValue('EmailFrom', EmailFromEdit.Text);
  EmailClientNameEdit.Text := GetClientRegistryValue('ClientNameEmail', EmailClientNameEdit.Text);

  // Email importance
  Importance := GetClientRegistryValue('EmailImportance', 'Normal');
  if Importance = 'High' then EmailImportanceCombo.ItemIndex := 0
  else if Importance = 'Low' then EmailImportanceCombo.ItemIndex := 2
  else EmailImportanceCombo.ItemIndex := 1;

  // Core services selection based on stored flags or service presence
  ChkLoki.Checked := WasServiceInstalled('InstalledLoki', 'IPMonitoring-Loki');
  ChkPromtail.Checked := WasServiceInstalled('InstalledLogAgent', 'IPMonitoring-LogAgent');
  
  // Service selection defaults based on stored flags or service presence
  ChkIFSErrorMonitor.Checked := WasServiceInstalled('InstalledIFSErrorMonitor', 'IPMonitoring_IBMIFSErrorMonitor');
  ChkRealTimeIFSMonitor.Checked := WasServiceInstalled('InstalledRealTimeIFSMonitor', 'IPMonitoring_IBMRealTimeIFSMonitor');
  ChkJobQueCountMonitor.Checked := WasServiceInstalled('InstalledJobQueCountMonitor', 'IPMonitoring_IBMJobQueCountMonitor');
  ChkJobQueStatusMonitor.Checked := WasServiceInstalled('InstalledJobQueStatusMonitor', 'IPMonitoring_IBMJobQueStatusMonitor');
  ChkServerUpTimeMonitor.Checked := WasServiceInstalled('InstalledServerUpTimeMonitor', 'IPMonitoring_ServerUpTimeMonitor');
  ChkSubSystemMonitor.Checked := WasServiceInstalled('InstalledSubSystemMonitor', 'IPMonitoring_IBMSubSystemMonitoring');
  ChkSystemMatrix.Checked := WasServiceInstalled('InstalledSystemMatrix', 'IPMonitoring_IBMSystemMatrix');
  ChkUserProfileChecker.Checked := WasServiceInstalled('InstalledUserProfileChecker', 'IPMonitoring_IBMUserProfileChecker');
  ChkNetWorkEnabler.Checked := WasServiceInstalled('InstalledNetWorkEnabler', 'IPMonitoring_NetWorkEnabler');
  ChkQSYSOPRMonitor.Checked := WasServiceInstalled('InstalledQSYSOPRMonitor', 'IPMonitoring_QSYSOPRMonitoring');
  ChkWinFSErrorMonitor.Checked := WasServiceInstalled('InstalledWinFSErrorMonitor', 'IPMonitoring_WinFSErrorMonitor');
  ChkWinFSCardinalityMonitor.Checked := WasServiceInstalled('InstalledWinFSCardinalityMonitor', 'IPMonitoring_WinFSCardinalityMonitor');
end;

procedure UpdatePrometheusYaml(Port: string); forward;
procedure RemovePrometheusTarget(JobName: string); forward;
procedure GenerateGrafanaIni(Port, Password: string); forward;
procedure UpdateGrafanaDatasource(PrometheusPort: string); forward;
procedure UpdateGrafanaLokiDatasource(LokiPort: string); forward;
procedure UpdateGrafanaDashboardProvider; forward;
procedure CreateEmailCommonControls(Page: TWizardPage); forward;
procedure CreateExtraPortsControls(Page: TWizardPage); forward;
function CheckCustomPortsFree(PortsText: string): string; forward;

procedure InitializeWizard;
var
  ConfigDesc, ServicePortsDesc, ClientInstanceDesc: string;
begin
  // Ensure single-client upgrades preload the existing client id for registry lookups
  LoadDefaultClientInstanceId;
  
  // Initialize install role from registry (default to 'Monitoring' for new installs)
  InstallRole := GetClientRegistryValue('InstallRole', 'Monitoring');

  // Keep the installer window on top so it remains clickable when you switch apps
  WizardForm.FormStyle := fsStayOnTop;

  // ===== INSTALL ROLE SELECTION PAGE =====
  // This page appears first to determine if this is a Monitoring Server or Log Agent
  InstallRolePage := CreateInputOptionPage(wpWelcome,
    'Installation Role Selection', 'What role will this server perform?',
    'Select the role for this installation:'#13#10#13#10 +
    '• Monitoring Server: Central server with Prometheus, Grafana, Loki. Collects metrics/logs from all other servers. Install IBM i monitors here.'#13#10#13#10 +
    '• Windows File Agent: Monitors Windows folders for errors/file counts. Metrics scraped by central Prometheus. No local Prometheus/Grafana needed.'#13#10#13#10 +
    '• Log Agent / Promtail: Ships application logs to central Loki server.',
    True, False);
  InstallRolePage.Add('Monitoring Server (Prometheus + Grafana + Loki + IBM monitors)');
  InstallRolePage.Add('Windows File Agent (WinFS monitors only - no Prometheus/Grafana)');
  InstallRolePage.Add('Log Agent (Promtail only - ships logs to Loki)');
  // Load previous selection
  if GetClientRegistryValue('InstallRole', 'Monitoring') = 'WinAgent' then
    InstallRolePage.SelectedValueIndex := 1
  else if GetClientRegistryValue('InstallRole', 'Monitoring') = 'Agent' then
    InstallRolePage.SelectedValueIndex := 2
  else
    InstallRolePage.SelectedValueIndex := 0;

  // ===== MULTI-CLIENT INSTALLATION PAGE =====
  // First, add Install Mode selection (New vs Modify) - Fix #1 for recursive upgrade bug
  if not IsFirstInstallation then
  begin
    InstallModePage := CreateInputOptionPage(InstallRolePage.ID,
      'Installation Mode', 'New Installation or Modify Existing?',
      'Choose whether to create a new client installation or modify an existing one:'#13#10#13#10 +
      '• New Installation: Creates a completely new client instance with fresh settings.'#13#10 +
      '• Modify Existing: Updates an existing client installation (preserves settings).'#13#10#13#10 +
      'WARNING: Selecting "New Installation" will NOT import settings from existing clients.',
      True, False);
    InstallModePage.Add('New Installation (create new client)');
    InstallModePage.Add('Modify Existing Installation (upgrade/change settings)');
    InstallModePage.SelectedValueIndex := 1; // Default to Modify
  end
  else
  begin
    InstallModePage := nil;
    IsNewClientInstallation := True; // First installation is always "new"
  end;
  
  // Client Instance page - comes after Install Mode page if it exists
  ClientInstanceDesc := 'Enter a unique identifier for this client installation.'#13#10#13#10;
  if CheckBaseInstallationExists then
    ClientInstanceDesc := ClientInstanceDesc + 'A base installation already exists. This will add a new client instance that shares the existing Grafana and Prometheus.'
  else
    ClientInstanceDesc := ClientInstanceDesc + 'This is the first installation. Grafana and Prometheus will be installed as shared services.';
  
  if InstallModePage <> nil then
    ClientInstancePage := CreateInputQueryPage(InstallModePage.ID,
      'Client Instance Configuration', 'Multi-Client Installation Support',
      ClientInstanceDesc)
  else
    ClientInstancePage := CreateInputQueryPage(InstallRolePage.ID,
      'Client Instance Configuration', 'Multi-Client Installation Support',
      ClientInstanceDesc);
  ClientInstancePage.Add('Client Instance ID (e.g., ClientA):', False);
  ClientInstancePage.Add('IBM i Server IP/Hostname for this client:', False);
  
  // Only pre-fill values if NOT explicitly a new installation
  // For new installations, leave blank to avoid accidental overwrites
  ClientInstancePage.Values[0] := '';
  ClientInstancePage.Values[1] := '';

  // ===== LOKI CONFIGURATION PAGE (for Monitoring Server role) =====
  LokiConfigPage := CreateInputQueryPage(ClientInstancePage.ID,
    'Configure Loki Log Server', 'Set up the log aggregation server',
    'Loki collects and stores logs from all application servers.'#13#10#13#10 +
    'Configure the port and data directory for Loki:');
  LokiConfigPage.Add('Loki HTTP Port:', False);
  LokiConfigPage.Add('Loki Data Directory:', False);
  // Load previous values from registry
  // NOTE: Cannot use {app} here as it's not initialized during InitializeWizard
  LokiConfigPage.Values[0] := GetClientRegistryValue('LokiPort', '3100');
  LokiConfigPage.Values[1] := GetClientRegistryValue('LokiDataDir', 'C:\IPMonitoring\data\loki');

  // ===== LOG AGENT (PROMTAIL) CONFIGURATION PAGE (for Application Server role) =====
  LogAgentConfigPage := CreateCustomPage(LokiConfigPage.ID,
    'Configure Log Agent', 'Set up Promtail to ship logs to Loki');
  CreateLogAgentConfigControls(LogAgentConfigPage);

  // ===== CORE SERVICES PAGE (Prometheus, Grafana, Loki, Promtail) =====
  CoreServicesPage := CreateCustomPage(LogAgentConfigPage.ID,
    'Core Services', 'Select the core infrastructure services to install:'#13#10 +
    '(These services are shared across all clients on this machine)');
  CreateCoreServicesControls(CoreServicesPage);

  // ===== IBM i MONITORS PAGE =====
  IBMiMonitorsPage := CreateCustomPage(CoreServicesPage.ID,
    'IBM i Monitors', 'Select IBM i monitoring services to install:'#13#10 +
    '(Each monitor is independent per client and can be toggled on/off separately)');
  CreateIBMiMonitorsControls(IBMiMonitorsPage);

  // ===== WINDOWS MONITORS PAGE =====
  WindowsMonitorsPage := CreateCustomPage(IBMiMonitorsPage.ID,
    'Windows File System Monitors', 'Select Windows monitoring services to install:'#13#10 +
    '(Each monitor is independent per client. Metrics are scraped by the shared central Prometheus)');
  CreateWindowsMonitorsControls(WindowsMonitorsPage);

  // Configuration Page - Prometheus/Grafana
  // Set description based on upgrade status
  ConfigDesc := 'Configure service ports and Grafana admin password';
  if IsUpgrade then
    ConfigDesc := ConfigDesc + #13#10#13#10 + 'NOTE: Upgrade detected. Port changes will take effect after service restart.';
  
  ConfigPage := CreateInputQueryPage(WindowsMonitorsPage.ID,
    'Set Up Your Dashboard Access', 'Configure ports and admin credentials'#13#10 +
    '(Note: These shared core service ports apply to all clients on this machine)',
    ConfigDesc);
  ConfigPage.Add('Prometheus Port (shared):', False);
  ConfigPage.Add('Grafana Port (shared):', False);
  ConfigPage.Add('Grafana Admin Password:', True);
  // Load previous values from registry
  ConfigPage.Values[0] := GetClientRegistryValue('PrometheusPort', '9090');
  ConfigPage.Values[1] := GetClientRegistryValue('GrafanaPort', '3000');
  ConfigPage.Values[2] := GetClientRegistryValue('GrafanaAdminPassword', 'admin');

  // Service Ports Configuration Page
  ServicePortsDesc := 'Configure the metrics port for each monitoring service (used by Prometheus):';
  if IsUpgrade then
    ServicePortsDesc := ServicePortsDesc + #13#10#13#10 + 'NOTE: Upgrade detected. Port changes will take effect after service restart.';
  
  ServicePortsPage := CreateCustomPage(ConfigPage.ID,
    'Configure Monitor Endpoints', ServicePortsDesc + #13#10 +
    '(Per-client monitors each listen on unique ports to prevent conflicts)');
  CreateServicePortsControls(ServicePortsPage);

  // Optional extra ports validation page
  ExtraPortsPage := CreateCustomPage(ServicePortsPage.ID,
    'Optional: Check Additional Ports',
    'Enter any additional port numbers to pre-check for conflicts. Use commas or new lines.');
  CreateExtraPortsControls(ExtraPortsPage);

  // IBM i Configuration Page
  IBMiConfigPage := CreateInputQueryPage(ExtraPortsPage.ID,
    'Connect to Your IBM i Server', 'Enter your IBM i credentials',
    'These credentials will be used by all IBM i monitors to collect metrics from your server.');
  IBMiConfigPage.Add('IBM i Server (IP/Hostname):', False);
  IBMiConfigPage.Add('IBM i User:', False);
  IBMiConfigPage.Add('IBM i Password:', True);
  IBMiConfigPage.Add('Client Name:', False);
  // Load previous values from registry
  IBMiConfigPage.Values[0] := GetClientRegistryValue('IBMiServer', '');
  IBMiConfigPage.Values[1] := GetClientRegistryValue('IBMiUser', '');
  IBMiConfigPage.Values[2] := GetClientRegistryValue('IBMiPassword', '');
  IBMiConfigPage.Values[3] := GetClientRegistryValue('ClientName', 'DefaultClient');

  // ===== EMAIL CONFIGURATION - Split into multiple clean pages =====
  
  // Page 1: Email Authentication Method Selection
  EmailAuthMethodPage := CreateInputOptionPage(IBMiConfigPage.ID,
    'How should alerts be sent?', 'Choose your email authentication method',
    'Select how the monitoring system will send email notifications when issues are detected:'#13#10#13#10 +
    '• SMTP Authentication: Traditional username/password. Works with most email servers.'#13#10 +
    '• OAuth2 (Microsoft 365): Modern, more secure authentication using Azure AD.',
    True, False);
  EmailAuthMethodPage.Add('SMTP Authentication');
  EmailAuthMethodPage.Add('OAuth2 (Microsoft 365)');
  // Load previous selection
  if GetClientRegistryValue('EmailAuthMethod', 'SMTP') = 'OAUTH2' then
    EmailAuthMethodPage.SelectedValueIndex := 1
  else
    EmailAuthMethodPage.SelectedValueIndex := 0;

  // Page 2: SMTP Configuration (shown only if SMTP selected) - Custom page with checkboxes
  SmtpConfigPage := CreateCustomPage(EmailAuthMethodPage.ID,
    'Configure Your Mail Server', 'Enter your SMTP server details');
  CreateSmtpConfigControls(SmtpConfigPage);

  // Page 3: OAuth2 Configuration Part 1 (shown only if OAuth2 selected)
  OAuthConfigPage1 := CreateCustomPage(SmtpConfigPage.ID,
    'Connect to Azure AD', 'Enter your Azure app registration details');
  CreateOAuthConfigControls1(OAuthConfigPage1);

  // Page 4: OAuth2 Configuration Part 2 (shown only if OAuth2 selected)
  OAuthConfigPage2 := CreateCustomPage(OAuthConfigPage1.ID,
    'Configure Graph API', 'Set up Microsoft 365 email sending');
  CreateOAuthConfigControls2(OAuthConfigPage2);

  // Page 5: Common Email Settings (always shown) - Custom page for better layout
  EmailCommonPage := CreateCustomPage(OAuthConfigPage2.ID,
    'Who should receive alerts?', 'Configure alert recipients and sender information');
  CreateEmailCommonControls(EmailCommonPage);
end;

procedure CreateEmailCommonControls(Page: TWizardPage);
var
  TopPos: Integer;
  FromLabel, ToLabel, BccLabel, ClientNameLabel, ImportanceLabel: TNewStaticText;
  SectionHeader, HelperText, BccHelper: TNewStaticText;
  Separator: TBevel;
begin
  TopPos := 0;
  
  // Section Header: Recipients
  SectionHeader := CreateSectionHeader(Page, 'Alert Recipients', TopPos);
  Separator := CreateSeparator(Page, TopPos);
  HelperText := CreateHelperText(Page, 'Enter email addresses. For multiple recipients, separate with commas.', TopPos);
  TopPos := TopPos + 4;
  
  // To Email(s) - Primary recipients first
  ToLabel := CreateFieldLabel(Page, 'Send alerts to:', TopPos);
  EmailToEdit := CreateTextEdit(Page, GetClientRegistryValue('EmailTo', ''), False, TopPos);
  
  // BCC Email(s)
  BccLabel := CreateFieldLabel(Page, 'BCC (optional):', TopPos);
  EmailBccEdit := CreateTextEdit(Page, GetClientRegistryValue('EmailBcc', ''), False, TopPos);
  
  TopPos := TopPos + 6;
  
  // Section Header: Sender Info
  SectionHeader := CreateSectionHeader(Page, 'Sender Information', TopPos);
  Separator := CreateSeparator(Page, TopPos);
  
  // From (Display Name)
  FromLabel := CreateFieldLabel(Page, 'Display Name (From):', TopPos);
  EmailFromEdit := CreateTextEdit(Page, GetClientRegistryValue('EmailFrom', 'Island Pacific Monitoring'), False, TopPos);
  
  // Client Name
  ClientNameLabel := CreateFieldLabel(Page, 'Client Name (appears in subject):', TopPos);
  EmailClientNameEdit := CreateTextEdit(Page, GetClientRegistryValue('ClientNameEmail', ''), False, TopPos);
  
  TopPos := TopPos + 6;
  
  // Section Header: Options
  SectionHeader := CreateSectionHeader(Page, 'Email Options', TopPos);
  Separator := CreateSeparator(Page, TopPos);
  BccHelper := CreateHelperText(Page, 'Set the priority level for alert emails.', TopPos);
  TopPos := TopPos + 4;
  
  // Email Importance dropdown
  ImportanceLabel := CreateFieldLabel(Page, 'Priority:', TopPos);
  
  EmailImportanceCombo := TNewComboBox.Create(Page);
  EmailImportanceCombo.Parent := Page.Surface;
  EmailImportanceCombo.Left := 0;
  EmailImportanceCombo.Top := TopPos;
  EmailImportanceCombo.Width := Page.SurfaceWidth;
  EmailImportanceCombo.Style := csDropDownList;
  EmailImportanceCombo.Items.Add('High');
  EmailImportanceCombo.Items.Add('Normal');
  EmailImportanceCombo.Items.Add('Low');
  // Set selected value from registry or default
  case GetClientRegistryValue('EmailImportance', 'Normal') of
    'High': EmailImportanceCombo.ItemIndex := 0;
    'Low': EmailImportanceCombo.ItemIndex := 2;
  else
    EmailImportanceCombo.ItemIndex := 1; // Normal
  end;
  TopPos := TopPos + 32;
end;

procedure CreateExtraPortsControls(Page: TWizardPage);
var
  Helper: TNewStaticText;
begin
  Helper := TNewStaticText.Create(Page);
  Helper.Parent := Page.Surface;
  Helper.Left := 0;
  Helper.Top := 0;
  Helper.Width := Page.SurfaceWidth;
  Helper.AutoSize := False;
  Helper.WordWrap := True;
  Helper.Caption := 'Optional: enter any extra port numbers to validate (commas or new lines). ' +
    'If any are in use, the installer will block until you choose a free port.';

  ExtraPortsMemo := TNewMemo.Create(Page);
  ExtraPortsMemo.Parent := Page.Surface;
  ExtraPortsMemo.Left := 0;
  ExtraPortsMemo.Top := Helper.Top + Helper.Height + ScaleY(8);
  ExtraPortsMemo.Width := Page.SurfaceWidth;
  ExtraPortsMemo.Height := ScaleY(120);
  ExtraPortsMemo.ScrollBars := ssVertical;
  ExtraPortsMemo.Text := '';
end;

function NeedsServicePortsConfig: Boolean;
begin
  // Any monitoring service needs port config
  Result := ChkIFSErrorMonitor.Checked or
            ChkRealTimeIFSMonitor.Checked or
            ChkJobQueCountMonitor.Checked or
            ChkJobQueStatusMonitor.Checked or
            ChkServerUpTimeMonitor.Checked or
            ChkSubSystemMonitor.Checked or
            ChkSystemMatrix.Checked or
            ChkUserProfileChecker.Checked or
            ChkNetWorkEnabler.Checked or
            ChkQSYSOPRMonitor.Checked or
            ChkWinFSErrorMonitor.Checked or
            ChkWinFSCardinalityMonitor.Checked;
end;

function NeedsIBMiConfig: Boolean;
begin
  // IBM i monitors need IBM i connection
  // Server UpTime Monitor and Network Enabler do NOT need IBM i connection
  Result := ChkIFSErrorMonitor.Checked or
            ChkRealTimeIFSMonitor.Checked or
            ChkJobQueCountMonitor.Checked or
            ChkJobQueStatusMonitor.Checked or
            ChkSubSystemMonitor.Checked or
            ChkSystemMatrix.Checked or
            ChkUserProfileChecker.Checked or
            ChkQSYSOPRMonitor.Checked;
end;

function NeedsEmailConfig: Boolean;
begin
  // Any monitoring service needs email config
  Result := ChkIFSErrorMonitor.Checked or
            ChkRealTimeIFSMonitor.Checked or
            ChkJobQueCountMonitor.Checked or
            ChkJobQueStatusMonitor.Checked or
            ChkServerUpTimeMonitor.Checked or
            ChkSubSystemMonitor.Checked or
            ChkSystemMatrix.Checked or
            ChkUserProfileChecker.Checked or
            ChkNetWorkEnabler.Checked or
            ChkQSYSOPRMonitor.Checked or
            ChkWinFSErrorMonitor.Checked or
            ChkWinFSCardinalityMonitor.Checked;
end;

function IsSmtpAuthSelected: Boolean;
begin
  Result := EmailAuthMethodPage.SelectedValueIndex = 0;
end;

// ===== UpdateReadyMemo: Show selected services on Ready to Install page =====
function UpdateReadyMemo(Space, NewLine, MemoUserInfoInfo, MemoDirInfo, MemoTypeInfo,
  MemoComponentsInfo, MemoGroupInfo, MemoTasksInfo: String): String;
var
  S: String;
  InstallMode: String;
begin
  S := '';
  
  // Installation mode
  if IsUpgrade then
    InstallMode := 'Upgrade'
  else
    InstallMode := 'New Installation';
  
  // Destination
  S := S + MemoDirInfo + NewLine + NewLine;
  
  // Installation Role
  S := S + 'Installation Role:' + NewLine;
  if IsLogAgentRole then
    S := S + Space + 'Application Server (Log Agent only)' + NewLine
  else
    S := S + Space + 'Monitoring Server (Full Suite)' + NewLine;
  S := S + NewLine;
  
  // Client Instance (only for Monitoring Server role)
  if IsMonitoringServerRole then
  begin
    S := S + 'Client Instance:' + NewLine;
    S := S + Space + 'Client ID: ' + ClientInstanceId + NewLine;
    S := S + Space + 'Mode: ' + InstallMode + NewLine;
    S := S + NewLine;
  end;
  
  // Log Agent Configuration (for Application Server role)
  if IsLogAgentRole then
  begin
    S := S + 'Log Agent Configuration:' + NewLine;
    S := S + Space + 'Loki Server: ' + GetLokiHost('') + ':' + LogAgentPortEdit.Text + NewLine;
    S := S + Space + 'Application: ' + GetLogAgentAppName('') + NewLine;
    S := S + Space + 'Environment: ' + GetLogAgentEnvironment('') + NewLine;
    S := S + Space + 'Log Path: ' + GetLogAgentLogPath('') + NewLine;
    S := S + NewLine;
    Result := S;
    Exit;  // Log Agent role doesn't need the rest
  end;
  
  // Core Services (Monitoring Server role only)
  S := S + 'Core Services:' + NewLine;
  if ChkPrometheus.Checked then
  begin
    if PrometheusInstalled then
      S := S + Space + '• Prometheus (Port ' + ConfigPage.Values[0] + ') - Upgrade' + NewLine
    else
      S := S + Space + '• Prometheus (Port ' + ConfigPage.Values[0] + ') - Install' + NewLine;
  end;
  if ChkGrafana.Checked then
  begin
    if GrafanaInstalled then
      S := S + Space + '• Grafana (Port ' + ConfigPage.Values[1] + ') - Upgrade' + NewLine
    else
      S := S + Space + '• Grafana (Port ' + ConfigPage.Values[1] + ') - Install' + NewLine;
  end;
  // Loki
  if IsMonitoringServerRole and ChkLoki.Checked then
  begin
    if WasServiceInstalled('InstalledLoki', 'IPMonitoring-Loki') then
      S := S + Space + '• Loki Log Server (Port ' + GetLokiPort('') + ') - Upgrade' + NewLine
    else
      S := S + Space + '• Loki Log Server (Port ' + GetLokiPort('') + ') - Install' + NewLine;
  end;
  if not ChkPrometheus.Checked and not ChkGrafana.Checked and not ChkLoki.Checked then
    S := S + Space + '(None selected)' + NewLine;
  S := S + NewLine;
  
  // Monitoring Services
  S := S + 'Monitoring Services:' + NewLine;
  if ChkIFSErrorMonitor.Checked then
  begin
    if WasServiceInstalled('InstalledIFSErrorMonitor', 'IPMonitoring_IBMIFSErrorMonitor') then
      S := S + Space + '• IFS Error Monitor (Port ' + IFSErrorMonitorPortEdit.Text + ') - Upgrade' + NewLine
    else
      S := S + Space + '• IFS Error Monitor (Port ' + IFSErrorMonitorPortEdit.Text + ') - Install' + NewLine;
  end;
  if ChkRealTimeIFSMonitor.Checked then
  begin
    if WasServiceInstalled('InstalledRealTimeIFSMonitor', 'IPMonitoring_IBMRealTimeIFSMonitor') then
      S := S + Space + '• Real-Time IFS Monitor (Port ' + RealTimeIFSMonitorPortEdit.Text + ') - Upgrade' + NewLine
    else
      S := S + Space + '• Real-Time IFS Monitor (Port ' + RealTimeIFSMonitorPortEdit.Text + ') - Install' + NewLine;
  end;
  if ChkJobQueCountMonitor.Checked then
  begin
    if WasServiceInstalled('InstalledJobQueCountMonitor', 'IPMonitoring_IBMJobQueCountMonitor') then
      S := S + Space + '• Job Queue Count Monitor (Port ' + JobQueCountMonitorPortEdit.Text + ') - Upgrade' + NewLine
    else
      S := S + Space + '• Job Queue Count Monitor (Port ' + JobQueCountMonitorPortEdit.Text + ') - Install' + NewLine;
  end;
  if ChkJobQueStatusMonitor.Checked then
  begin
    if WasServiceInstalled('InstalledJobQueStatusMonitor', 'IPMonitoring_IBMJobQueStatusMonitor') then
      S := S + Space + '• Job Queue Status Monitor (Port ' + JobQueStatusMonitorPortEdit.Text + ') - Upgrade' + NewLine
    else
      S := S + Space + '• Job Queue Status Monitor (Port ' + JobQueStatusMonitorPortEdit.Text + ') - Install' + NewLine;
  end;
  if ChkServerUpTimeMonitor.Checked then
  begin
    if WasServiceInstalled('InstalledServerUpTimeMonitor', 'IPMonitoring_ServerUpTimeMonitor') then
      S := S + Space + '• Server UpTime Monitor (Port ' + ServerUpTimeMonitorPortEdit.Text + ') - Upgrade' + NewLine
    else
      S := S + Space + '• Server UpTime Monitor (Port ' + ServerUpTimeMonitorPortEdit.Text + ') - Install' + NewLine;
  end;
  if ChkSubSystemMonitor.Checked then
  begin
    if WasServiceInstalled('InstalledSubSystemMonitor', 'IPMonitoring_IBMSubSystemMonitoring') then
      S := S + Space + '• SubSystem Monitor (Port ' + SubSystemMonitorPortEdit.Text + ') - Upgrade' + NewLine
    else
      S := S + Space + '• SubSystem Monitor (Port ' + SubSystemMonitorPortEdit.Text + ') - Install' + NewLine;
  end;
  if ChkSystemMatrix.Checked then
  begin
    if WasServiceInstalled('InstalledSystemMatrix', 'IPMonitoring_IBMSystemMatrix') then
      S := S + Space + '• System Matrix Monitor (Port ' + SystemMatrixPortEdit.Text + ') - Upgrade' + NewLine
    else
      S := S + Space + '• System Matrix Monitor (Port ' + SystemMatrixPortEdit.Text + ') - Install' + NewLine;
  end;
  if ChkUserProfileChecker.Checked then
  begin
    if WasServiceInstalled('InstalledUserProfileChecker', 'IPMonitoring_IBMUserProfileChecker') then
      S := S + Space + '• User Profile Checker (Port ' + UserProfileCheckerPortEdit.Text + ') - Upgrade' + NewLine
    else
      S := S + Space + '• User Profile Checker (Port ' + UserProfileCheckerPortEdit.Text + ') - Install' + NewLine;
  end;
  if ChkNetWorkEnabler.Checked then
  begin
    if WasServiceInstalled('InstalledNetWorkEnabler', 'IPMonitoring_NetWorkEnabler') then
      S := S + Space + '• Network Enabler (Port ' + NetWorkEnablerPortEdit.Text + ') - Upgrade' + NewLine
    else
      S := S + Space + '• Network Enabler (Port ' + NetWorkEnablerPortEdit.Text + ') - Install' + NewLine;
  end;
  if ChkQSYSOPRMonitor.Checked then
  begin
    if WasServiceInstalled('InstalledQSYSOPRMonitor', 'IPMonitoring_QSYSOPRMonitoring') then
      S := S + Space + '• QSYSOPR Monitor (Port ' + QSYSOPRMonitorPortEdit.Text + ') - Upgrade' + NewLine
    else
      S := S + Space + '• QSYSOPR Monitor (Port ' + QSYSOPRMonitorPortEdit.Text + ') - Install' + NewLine;
  end;
  if ChkWinFSErrorMonitor.Checked then
  begin
    if WasServiceInstalled('InstalledWinFSErrorMonitor', 'IPMonitoring_WinFSErrorMonitor') then
      S := S + Space + '• Windows FS Error Monitor (Port ' + WinFSErrorMonitorPortEdit.Text + ') - Upgrade' + NewLine
    else
      S := S + Space + '• Windows FS Error Monitor (Port ' + WinFSErrorMonitorPortEdit.Text + ') - Install' + NewLine;
  end;
  if ChkWinFSCardinalityMonitor.Checked then
  begin
    if WasServiceInstalled('InstalledWinFSCardinalityMonitor', 'IPMonitoring_WinFSCardinalityMonitor') then
      S := S + Space + '• Windows FS Cardinality Monitor (Port ' + WinFSCardinalityMonitorPortEdit.Text + ') - Upgrade' + NewLine
    else
      S := S + Space + '• Windows FS Cardinality Monitor (Port ' + WinFSCardinalityMonitorPortEdit.Text + ') - Install' + NewLine;
  end;
  
  if not (ChkIFSErrorMonitor.Checked or ChkRealTimeIFSMonitor.Checked or 
          ChkJobQueCountMonitor.Checked or ChkJobQueStatusMonitor.Checked or 
          ChkServerUpTimeMonitor.Checked or ChkSubSystemMonitor.Checked or
          ChkSystemMatrix.Checked or ChkUserProfileChecker.Checked or
          ChkNetWorkEnabler.Checked or ChkQSYSOPRMonitor.Checked or
          ChkWinFSErrorMonitor.Checked or ChkWinFSCardinalityMonitor.Checked) then
    S := S + Space + '(None selected)' + NewLine;
  S := S + NewLine;
  
  // IBM i Connection
  if NeedsIBMiConfig then
  begin
    S := S + 'IBM i Connection:' + NewLine;
    S := S + Space + 'Server: ' + IBMiConfigPage.Values[0] + NewLine;
    S := S + Space + 'User: ' + IBMiConfigPage.Values[1] + NewLine;
    S := S + NewLine;
  end;
  
  // Email Configuration
  if NeedsEmailConfig then
  begin
    S := S + 'Email Alerts:' + NewLine;
    if IsSmtpAuthSelected then
      S := S + Space + 'Method: SMTP (' + SmtpHostEdit.Text + ':' + SmtpPortEdit.Text + ')' + NewLine
    else
      S := S + Space + 'Method: OAuth2 (Microsoft 365)' + NewLine;
    S := S + Space + 'Recipients: ' + EmailToEdit.Text + NewLine;
  end;
  
  Result := S;
end;

function ShouldSkipPage(PageID: Integer): Boolean;
begin
  Result := False;
  
  // Skip Install Mode page if this is the first installation (no existing clients)
  if (InstallModePage <> nil) and (PageID = InstallModePage.ID) then
    Result := IsFirstInstallation or IsLogAgentRole or IsWinAgentRole;
  
  // Skip client instance page for Log Agent and WinAgent installs
  if PageID = ClientInstancePage.ID then
    Result := IsLogAgentRole or IsWinAgentRole;
  
  // Skip Loki config page if not Monitoring Server role OR if Loki not selected
  if PageID = LokiConfigPage.ID then
    Result := not IsMonitoringServerRole or not ChkLoki.Checked;
  
  // Skip Log Agent (Promtail) config page if Promtail is not selected AND not Log Agent role
  if PageID = LogAgentConfigPage.ID then
    Result := not ChkPromtail.Checked and not IsLogAgentRole;
  
  // ===== SERVICE SELECTION PAGES - Role-based visibility =====
  // Core Services Page: Only for Monitoring Server role
  if PageID = CoreServicesPage.ID then
    Result := not IsMonitoringServerRole;
  
  // IBM i Monitors Page: Only for Monitoring Server role  
  if PageID = IBMiMonitorsPage.ID then
    Result := not IsMonitoringServerRole;
  
  // Windows Monitors Page: Only for Windows File Agent role
  if PageID = WindowsMonitorsPage.ID then
    Result := not IsWinAgentRole;  // Hide for Monitoring/LogAgent roles
  
  // Skip config page if neither Prometheus nor Grafana selected OR if using existing installation OR if Log Agent/WinAgent role
  if PageID = ConfigPage.ID then
  begin
    if IsLogAgentRole or IsWinAgentRole then
      Result := True
    else if UseExistingGrafana and not IsFirstInstallation then
      Result := True
    else
      Result := not ChkPrometheus.Checked and not ChkGrafana.Checked;
  end;
  
  // Skip service ports config if no monitors selected or Log Agent role
  if PageID = ServicePortsPage.ID then
    Result := IsLogAgentRole or not NeedsServicePortsConfig;
  
  // Skip IBM i config if no monitors that need it are selected or Log Agent/WinAgent role
  if PageID = IBMiConfigPage.ID then
    Result := IsLogAgentRole or IsWinAgentRole or not NeedsIBMiConfig;
  
  // Skip email auth method page if no monitors that need email are selected or Log Agent role
  // Note: Windows monitors CAN send email alerts, so don't skip for WinAgent
  if PageID = EmailAuthMethodPage.ID then
    Result := IsLogAgentRole or not NeedsEmailConfig;
  
  // Skip SMTP config if no monitors selected OR OAuth2 is selected or Log Agent role
  if PageID = SmtpConfigPage.ID then
    Result := IsLogAgentRole or not NeedsEmailConfig or not IsSmtpAuthSelected;
  
  // Skip OAuth config pages if no monitors selected OR SMTP is selected or Log Agent role
  if (PageID = OAuthConfigPage1.ID) or (PageID = OAuthConfigPage2.ID) then
    Result := IsLogAgentRole or not NeedsEmailConfig or IsSmtpAuthSelected;
  
  // Skip common email page if no monitors selected or Log Agent role
  if PageID = EmailCommonPage.ID then
    Result := IsLogAgentRole or not NeedsEmailConfig;
end;

// ===== UPDATE SERVICE PORTS PAGE VISIBILITY =====
// Shows only the port configuration fields for monitors that are selected
procedure UpdateServicePortsVisibility;
var
  TopPos: Integer;
  RowHeight: Integer;
begin
  TopPos := 8;
  RowHeight := 32;
  
  // IBM i Monitors (10 monitors)
  // 1. IFS Error Monitor
  if ChkIFSErrorMonitor.Checked then
  begin
    LblIFSErrorMonitorPort.Visible := True;
    LblIFSErrorMonitorPort.Top := TopPos + 3;
    IFSErrorMonitorPortEdit.Visible := True;
    IFSErrorMonitorPortEdit.Top := TopPos;
    TopPos := TopPos + RowHeight;
  end
  else
  begin
    LblIFSErrorMonitorPort.Visible := False;
    IFSErrorMonitorPortEdit.Visible := False;
  end;
  
  // 2. Real-Time IFS Monitor
  if ChkRealTimeIFSMonitor.Checked then
  begin
    LblRealTimeIFSMonitorPort.Visible := True;
    LblRealTimeIFSMonitorPort.Top := TopPos + 3;
    RealTimeIFSMonitorPortEdit.Visible := True;
    RealTimeIFSMonitorPortEdit.Top := TopPos;
    TopPos := TopPos + RowHeight;
  end
  else
  begin
    LblRealTimeIFSMonitorPort.Visible := False;
    RealTimeIFSMonitorPortEdit.Visible := False;
  end;
  
  // 3. Job Queue Count Monitor
  if ChkJobQueCountMonitor.Checked then
  begin
    LblJobQueCountMonitorPort.Visible := True;
    LblJobQueCountMonitorPort.Top := TopPos + 3;
    JobQueCountMonitorPortEdit.Visible := True;
    JobQueCountMonitorPortEdit.Top := TopPos;
    TopPos := TopPos + RowHeight;
  end
  else
  begin
    LblJobQueCountMonitorPort.Visible := False;
    JobQueCountMonitorPortEdit.Visible := False;
  end;
  
  // 4. Job Queue Status Monitor
  if ChkJobQueStatusMonitor.Checked then
  begin
    LblJobQueStatusMonitorPort.Visible := True;
    LblJobQueStatusMonitorPort.Top := TopPos + 3;
    JobQueStatusMonitorPortEdit.Visible := True;
    JobQueStatusMonitorPortEdit.Top := TopPos;
    TopPos := TopPos + RowHeight;
  end
  else
  begin
    LblJobQueStatusMonitorPort.Visible := False;
    JobQueStatusMonitorPortEdit.Visible := False;
  end;
  
  // 5. Server UpTime Monitor
  if ChkServerUpTimeMonitor.Checked then
  begin
    LblServerUpTimeMonitorPort.Visible := True;
    LblServerUpTimeMonitorPort.Top := TopPos + 3;
    ServerUpTimeMonitorPortEdit.Visible := True;
    ServerUpTimeMonitorPortEdit.Top := TopPos;
    TopPos := TopPos + RowHeight;
  end
  else
  begin
    LblServerUpTimeMonitorPort.Visible := False;
    ServerUpTimeMonitorPortEdit.Visible := False;
  end;
  
  // 6. SubSystem Monitor
  if ChkSubSystemMonitor.Checked then
  begin
    LblSubSystemMonitorPort.Visible := True;
    LblSubSystemMonitorPort.Top := TopPos + 3;
    SubSystemMonitorPortEdit.Visible := True;
    SubSystemMonitorPortEdit.Top := TopPos;
    TopPos := TopPos + RowHeight;
  end
  else
  begin
    LblSubSystemMonitorPort.Visible := False;
    SubSystemMonitorPortEdit.Visible := False;
  end;
  
  // 7. System Matrix
  if ChkSystemMatrix.Checked then
  begin
    LblSystemMatrixPort.Visible := True;
    LblSystemMatrixPort.Top := TopPos + 3;
    SystemMatrixPortEdit.Visible := True;
    SystemMatrixPortEdit.Top := TopPos;
    TopPos := TopPos + RowHeight;
  end
  else
  begin
    LblSystemMatrixPort.Visible := False;
    SystemMatrixPortEdit.Visible := False;
  end;
  
  // 8. User Profile Checker
  if ChkUserProfileChecker.Checked then
  begin
    LblUserProfileCheckerPort.Visible := True;
    LblUserProfileCheckerPort.Top := TopPos + 3;
    UserProfileCheckerPortEdit.Visible := True;
    UserProfileCheckerPortEdit.Top := TopPos;
    TopPos := TopPos + RowHeight;
  end
  else
  begin
    LblUserProfileCheckerPort.Visible := False;
    UserProfileCheckerPortEdit.Visible := False;
  end;
  
  // 9. Network Enabler
  if ChkNetWorkEnabler.Checked then
  begin
    LblNetWorkEnablerPort.Visible := True;
    LblNetWorkEnablerPort.Top := TopPos + 3;
    NetWorkEnablerPortEdit.Visible := True;
    NetWorkEnablerPortEdit.Top := TopPos;
    TopPos := TopPos + RowHeight;
  end
  else
  begin
    LblNetWorkEnablerPort.Visible := False;
    NetWorkEnablerPortEdit.Visible := False;
  end;
  
  // 10. QSYSOPR Monitor
  if ChkQSYSOPRMonitor.Checked then
  begin
    LblQSYSOPRMonitorPort.Visible := True;
    LblQSYSOPRMonitorPort.Top := TopPos + 3;
    QSYSOPRMonitorPortEdit.Visible := True;
    QSYSOPRMonitorPortEdit.Top := TopPos;
    TopPos := TopPos + RowHeight;
  end
  else
  begin
    LblQSYSOPRMonitorPort.Visible := False;
    QSYSOPRMonitorPortEdit.Visible := False;
  end;
  
  // Windows Monitors (2 monitors)
  // 11. Windows FS Error Monitor
  if ChkWinFSErrorMonitor.Checked then
  begin
    LblWinFSErrorMonitorPort.Visible := True;
    LblWinFSErrorMonitorPort.Top := TopPos + 3;
    WinFSErrorMonitorPortEdit.Visible := True;
    WinFSErrorMonitorPortEdit.Top := TopPos;
    TopPos := TopPos + RowHeight;
  end
  else
  begin
    LblWinFSErrorMonitorPort.Visible := False;
    WinFSErrorMonitorPortEdit.Visible := False;
  end;
  
  // 12. Windows FS Cardinality Monitor
  if ChkWinFSCardinalityMonitor.Checked then
  begin
    LblWinFSCardinalityMonitorPort.Visible := True;
    LblWinFSCardinalityMonitorPort.Top := TopPos + 3;
    WinFSCardinalityMonitorPortEdit.Visible := True;
    WinFSCardinalityMonitorPortEdit.Top := TopPos;
    // TopPos := TopPos + RowHeight; // Last item, no need to increment
  end
  else
  begin
    LblWinFSCardinalityMonitorPort.Visible := False;
    WinFSCardinalityMonitorPortEdit.Visible := False;
  end;
end;

procedure CurPageChanged(CurPageID: Integer);
var
  BaseDir, NewDir: string;
begin
  // Update ClientInstancePage to show upgrade warning if modifying existing
  if CurPageID = ClientInstancePage.ID then
  begin
    if not IsNewClientInstallation and (ClientInstanceId <> '') then
    begin
      // Update the subtitle to inform user why Client ID is locked
      WizardForm.PageDescriptionLabel.Caption := 
        'Modifying existing installation: ' + ClientInstanceId + #13#10 +
        'The Client Instance ID cannot be changed during an upgrade to prevent service conflicts.';
    end
    else
    begin
      WizardForm.PageDescriptionLabel.Caption := 
        'Enter a unique identifier for this new client installation.';
    end;
  end;
  
  // Update directory path when entering the Select Directory page
  // This ensures the ClientInstanceId entered on the previous page is reflected
  if CurPageID = wpSelectDir then
  begin
    if ClientInstanceId <> '' then
    begin
      BaseDir := ExpandConstant('{commonpf}') + '\Island Pacific\Operations Monitor';
      NewDir := BaseDir + '\' + ClientInstanceId;
      // Only update if the current dir doesn't already include the client ID
      if Pos('\' + ClientInstanceId, WizardForm.DirEdit.Text) = 0 then
      begin
        WizardForm.DirEdit.Text := NewDir;
      end;
    end;
  end;
  
  // Update Service Ports visibility when entering the ports page
  // Shows only port fields for monitors that were selected
  if CurPageID = ServicePortsPage.ID then
  begin
    UpdateServicePortsVisibility;
  end;
end;

// Validate that no two selected monitors share the same port, and no monitor uses Prometheus/Grafana ports
function CheckCustomPortsFree(PortsText: string): string;
var
  Clean, Token, Port: string;
  i: Integer;
begin
  Result := '';
  Clean := PortsText;
  StringChangeEx(Clean, ';', ',', True);
  StringChangeEx(Clean, #13, ',', True);
  StringChangeEx(Clean, #10, ',', True);
  Clean := Clean + ','; // Sentinel
  Token := '';

  for i := 1 to Length(Clean) do
  begin
    if Clean[i] = ',' then
    begin
      Port := Trim(Token);
      Token := '';
      if Port <> '' then
      begin
        if IsPortInUse(Port) then
        begin
          Result := Port;
          Exit;
        end;
      end;
    end
    else
      Token := Token + Clean[i];
  end;
end;

// Validate that no two selected monitors share the same port, and no monitor uses Prometheus/Grafana ports
function ValidateNoDuplicatePorts: Boolean;
var
  Ports: array of string;
  PortNames: array of string;
  Count, i, j: Integer;
  PromPort, GrafPort: string;
begin
  Result := True;
  Count := 0;
  
  // Get core service ports
  PromPort := ConfigPage.Values[0];
  GrafPort := ConfigPage.Values[1];
  
  // Build array of selected monitor ports
  SetArrayLength(Ports, 12);
  SetArrayLength(PortNames, 12);
  
  if ChkIFSErrorMonitor.Checked then begin
    Ports[Count] := IFSErrorMonitorPortEdit.Text;
    PortNames[Count] := 'IFS Error Monitor';
    Count := Count + 1;
  end;
  if ChkRealTimeIFSMonitor.Checked then begin
    Ports[Count] := RealTimeIFSMonitorPortEdit.Text;
    PortNames[Count] := 'Real-Time IFS Monitor';
    Count := Count + 1;
  end;
  if ChkJobQueCountMonitor.Checked then begin
    Ports[Count] := JobQueCountMonitorPortEdit.Text;
    PortNames[Count] := 'Job Queue Count Monitor';
    Count := Count + 1;
  end;
  if ChkJobQueStatusMonitor.Checked then begin
    Ports[Count] := JobQueStatusMonitorPortEdit.Text;
    PortNames[Count] := 'Job Queue Status Monitor';
    Count := Count + 1;
  end;
  if ChkServerUpTimeMonitor.Checked then begin
    Ports[Count] := ServerUpTimeMonitorPortEdit.Text;
    PortNames[Count] := 'Server UpTime Monitor';
    Count := Count + 1;
  end;
  if ChkSubSystemMonitor.Checked then begin
    Ports[Count] := SubSystemMonitorPortEdit.Text;
    PortNames[Count] := 'SubSystem Monitor';
    Count := Count + 1;
  end;
  if ChkSystemMatrix.Checked then begin
    Ports[Count] := SystemMatrixPortEdit.Text;
    PortNames[Count] := 'System Matrix Monitor';
    Count := Count + 1;
  end;
  if ChkUserProfileChecker.Checked then begin
    Ports[Count] := UserProfileCheckerPortEdit.Text;
    PortNames[Count] := 'User Profile Checker';
    Count := Count + 1;
  end;
  if ChkNetWorkEnabler.Checked then begin
    Ports[Count] := NetWorkEnablerPortEdit.Text;
    PortNames[Count] := 'Network Enabler';
    Count := Count + 1;
  end;
  if ChkQSYSOPRMonitor.Checked then begin
    Ports[Count] := QSYSOPRMonitorPortEdit.Text;
    PortNames[Count] := 'QSYSOPR Monitor';
    Count := Count + 1;
  end;
  if ChkWinFSErrorMonitor.Checked then begin
    Ports[Count] := WinFSErrorMonitorPortEdit.Text;
    PortNames[Count] := 'Windows FS Error Monitor';
    Count := Count + 1;
  end;
  if ChkWinFSCardinalityMonitor.Checked then begin
    Ports[Count] := WinFSCardinalityMonitorPortEdit.Text;
    PortNames[Count] := 'Windows FS Cardinality Monitor';
    Count := Count + 1;
  end;
  
  // Check monitor ports against Prometheus/Grafana/Loki
  for i := 0 to Count - 1 do
  begin
    if Ports[i] = PromPort then
    begin
      MsgBox(PortNames[i] + ' port ' + Ports[i] + ' conflicts with Prometheus port. Please use a different port.', mbError, MB_OK);
      Result := False;
      Exit;
    end;
    if Ports[i] = GrafPort then
    begin
      MsgBox(PortNames[i] + ' port ' + Ports[i] + ' conflicts with Grafana port. Please use a different port.', mbError, MB_OK);
      Result := False;
      Exit;
    end;
    if IsMonitoringServerRole and ChkLoki.Checked and (Ports[i] = GetLokiPort('')) then
    begin
      MsgBox(PortNames[i] + ' port ' + Ports[i] + ' conflicts with Loki port. Please use a different port.', mbError, MB_OK);
      Result := False;
      Exit;
    end;
  end;
  
  // Check for duplicate ports between monitors
  for i := 0 to Count - 2 do
  begin
    for j := i + 1 to Count - 1 do
    begin
      if Ports[i] = Ports[j] then
      begin
        MsgBox(PortNames[i] + ' and ' + PortNames[j] + ' both use port ' + Ports[i] + '. Each monitor must use a unique port.', mbError, MB_OK);
        Result := False;
        Exit;
      end;
    end;
  end;
end;

function NextButtonClick(CurPageID: Integer): Boolean;
var
  AnyIBMMonitorSelected: Boolean;
  CustomPortConflict: string;
begin
  Result := True;
  
  // Capture Install Role selection
  if CurPageID = InstallRolePage.ID then
  begin
    case InstallRolePage.SelectedValueIndex of
      0: InstallRole := 'Monitoring';
      1: InstallRole := 'WinAgent';
      2: InstallRole := 'Agent';
    else
      InstallRole := 'Monitoring';
    end;
    
    // Adjust Promtail visibility/selection based on role
    if IsMonitoringServerRole then
    begin
      ChkPromtail.Checked := False;
      ChkPromtail.Enabled := False;
      ChkPromtail.Visible := False;
    end
    else if IsWinAgentRole then
    begin
      ChkPromtail.Checked := False;
      ChkPromtail.Enabled := False;
      ChkPromtail.Visible := False;
    end
    else if IsLogAgentRole then
    begin
      ChkPromtail.Checked := True;
      ChkPromtail.Enabled := True;
      ChkPromtail.Visible := True;
    end
    else
    begin
      ChkPromtail.Enabled := True;
      ChkPromtail.Visible := True;
    end;
    
    // For WinAgent role, auto-select WinFS monitors and deselect everything else
    if IsWinAgentRole then
    begin
      // Use computer name or 'WinAgent' as default for WinAgent installations
      ClientInstanceId := 'WinAgent';
      
      // Deselect core components (not needed for agent mode)
      ChkPrometheus.Checked := False;
      ChkGrafana.Checked := False;
      ChkLoki.Checked := False;
      ChkPromtail.Checked := False;
      
      // Deselect IBM i monitors
      ChkIFSErrorMonitor.Checked := False;
      ChkRealTimeIFSMonitor.Checked := False;
      ChkJobQueCountMonitor.Checked := False;
      ChkJobQueStatusMonitor.Checked := False;
      ChkServerUpTimeMonitor.Checked := False;
      ChkSubSystemMonitor.Checked := False;
      ChkSystemMatrix.Checked := False;
      ChkUserProfileChecker.Checked := False;
      ChkNetWorkEnabler.Checked := False;
      ChkQSYSOPRMonitor.Checked := False;
      
      // Auto-select Windows FS monitors
      ChkWinFSErrorMonitor.Checked := True;
      ChkWinFSCardinalityMonitor.Checked := True;
    end;
  end;
  
  // Handle Install Mode selection (New vs Modify) - Fix #1
  if (InstallModePage <> nil) and (CurPageID = InstallModePage.ID) then
  begin
    // Always treat WinAgent/LogAgent installs as NEW to avoid reusing the base client AppId,
    // which would trigger an uninstall of IBM services during upgrade.
    if IsWinAgentRole or IsLogAgentRole then
      InstallModePage.SelectedValueIndex := 0;

    IsNewClientInstallation := (InstallModePage.SelectedValueIndex = 0);
    
    if IsNewClientInstallation then
    begin
      // For WinAgent/LogAgent, keep the role-specific client ID so AppId is unique and does not overlap IBM installs
      if IsWinAgentRole then
        ClientInstanceId := 'WinAgent'
      else if IsLogAgentRole then
        ClientInstanceId := 'LogAgent'
      else
        ClientInstanceId := '';

      // Prefill the client ID field when we set a default; otherwise leave blank for Monitoring role
      if ClientInstanceId <> '' then
        ClientInstancePage.Values[0] := ClientInstanceId
      else
        ClientInstancePage.Values[0] := '';

      // Clear IBM host for new installs
      ClientInstancePage.Values[1] := '';

      // Enable editing of Client Instance ID for new installations
      ClientInstancePage.Edits[0].Enabled := True;
      ClientInstancePage.Edits[0].Color := clWindow;
    end
    else
    begin
      // Modify existing - preload the default client ID if only one exists
      LoadDefaultClientInstanceId;
      ClientInstancePage.Values[0] := ClientInstanceId;
      ClientInstancePage.Values[1] := GetClientRegistryValue('IBMiServer', '');
      // CRITICAL: Disable Client Instance ID editing during upgrades to prevent AppId mismatch
      ClientInstancePage.Edits[0].Enabled := False;
      ClientInstancePage.Edits[0].Color := clBtnFace;  // Gray background to indicate read-only
      // Also reload other settings from registry
      ApplyClientSettingsFromRegistry;
      // Set upgrade flag
      IsUpgrade := True;
      
      // Re-apply role-based checkbox selections after loading registry values
      // This ensures WinAgent role keeps WinFS monitors selected even after ApplyClientSettingsFromRegistry
      if IsWinAgentRole then
      begin
        ChkWinFSErrorMonitor.Checked := True;
        ChkWinFSCardinalityMonitor.Checked := True;
      end;
    end;
  end;
  
  // For Log Agent role, set a default client ID
  // Note: WinAgent role ClientInstanceId is set in the role selection block above
  if (CurPageID = InstallRolePage.ID) and IsLogAgentRole then
  begin
    // Use 'LogAgent' as default for Log Agent installations
    ClientInstanceId := 'LogAgent';
    
    // Log Agent only installs Promtail - deselect everything else
    ChkPrometheus.Checked := False;
    ChkGrafana.Checked := False;
    ChkLoki.Checked := False;
    ChkPromtail.Checked := True;  // Promtail is the whole point of Log Agent
    
    // Deselect all monitors
    ChkIFSErrorMonitor.Checked := False;
    ChkRealTimeIFSMonitor.Checked := False;
    ChkJobQueCountMonitor.Checked := False;
    ChkJobQueStatusMonitor.Checked := False;
    ChkServerUpTimeMonitor.Checked := False;
    ChkSubSystemMonitor.Checked := False;
    ChkSystemMatrix.Checked := False;
    ChkUserProfileChecker.Checked := False;
    ChkNetWorkEnabler.Checked := False;
    ChkQSYSOPRMonitor.Checked := False;
    ChkWinFSErrorMonitor.Checked := False;
    ChkWinFSCardinalityMonitor.Checked := False;
  end;
  
  // Validate Client Instance Page
  if CurPageID = ClientInstancePage.ID then
  begin
    // Client Instance ID is required
    ClientInstanceId := Trim(ClientInstancePage.Values[0]);
    if ClientInstanceId = '' then
    begin
      MsgBox('Please enter a Client Instance ID. This uniquely identifies this installation (e.g., ClientA).', mbError, MB_OK);
      Result := False;
      Exit;
    end;
    
    // Check for invalid characters in Client ID
    if (Pos(' ', ClientInstanceId) > 0) or (Pos('\', ClientInstanceId) > 0) or 
       (Pos('/', ClientInstanceId) > 0) or (Pos(':', ClientInstanceId) > 0) then
    begin
      MsgBox('Client Instance ID cannot contain spaces or special characters (\ / :). Use alphanumeric characters and underscores only.', mbError, MB_OK);
      Result := False;
      Exit;
    end;
    
    // Fix #1: Check if this client already exists AND user explicitly chose "New Installation"
    if IsNewClientInstallation and CheckClientExists(ClientInstanceId) then
    begin
      MsgBox('Client Instance "' + ClientInstanceId + '" already exists.'#13#10#13#10 +
             'You selected "New Installation" but this client ID is already in use. '#13#10 +
             'Please choose a different Client Instance ID, or go back and select "Modify Existing Installation".', 
             mbError, MB_OK);
      Result := False;
      Exit;
    end;
    
    // Check if this client already exists (and it's not an upgrade)
    if not IsUpgrade and not IsNewClientInstallation and CheckClientExists(ClientInstanceId) then
    begin
      if MsgBox('Client Instance "' + ClientInstanceId + '" already exists. Do you want to upgrade/modify this installation?', 
                mbConfirmation, MB_YESNO) = IDNO then
      begin
        Result := False;
        Exit;
      end
      else
      begin
        // User chose to upgrade - set IsUpgrade flag so port validation is skipped
        IsUpgrade := True;
      end;
    end;
    
    // If not first installation, disable Prometheus/Grafana selection and use existing
    if not IsFirstInstallation then
    begin
      ChkPrometheus.Checked := True;
      ChkPrometheus.Enabled := False;
      ChkGrafana.Checked := True;
      ChkGrafana.Enabled := False;
      // Use existing ports
      ConfigPage.Values[0] := ExistingPrometheusPort;
      ConfigPage.Values[1] := ExistingGrafanaPort;
    end;

    // Reload all client-scoped defaults now that the client ID is known
    ApplyClientSettingsFromRegistry;
  end;
  
  // Validate Core Services page - IBM monitors require Prometheus/Grafana
  if CurPageID = CoreServicesPage.ID then
  begin
    // Check will be done on IBM monitors page
  end;
  
  // Validate IBM Monitors page - cannot select IBM monitors without Prometheus/Grafana
  if CurPageID = IBMiMonitorsPage.ID then
  begin
    AnyIBMMonitorSelected := ChkIFSErrorMonitor.Checked or
                              ChkRealTimeIFSMonitor.Checked or
                              ChkJobQueCountMonitor.Checked or
                              ChkJobQueStatusMonitor.Checked or
                              ChkServerUpTimeMonitor.Checked or
                              ChkSubSystemMonitor.Checked or
                              ChkSystemMatrix.Checked or
                              ChkUserProfileChecker.Checked or
                              ChkNetWorkEnabler.Checked or
                              ChkQSYSOPRMonitor.Checked;
    
    // Only IBM monitors require local Prometheus and Grafana
    if AnyIBMMonitorSelected then
    begin
      if not ChkPrometheus.Checked then
      begin
        MsgBox('Prometheus is required for IBM i monitoring services. Please go back and select Prometheus on the Core Services page.', mbError, MB_OK);
        Result := False;
        Exit;
      end;
      if not ChkGrafana.Checked then
      begin
        MsgBox('Grafana is required for IBM i monitoring services. Please go back and select Grafana on the Core Services page.', mbError, MB_OK);
        Result := False;
        Exit;
      end;
    end;
  end;
  
  // Skip port validation on upgrade - ports are already in use by our services
  if CurPageID = ConfigPage.ID then
  begin
    if not IsUpgrade then
    begin
      if ChkPrometheus.Checked and IsPortInUse(ConfigPage.Values[0]) then
      begin
        MsgBox('Prometheus port ' + ConfigPage.Values[0] + ' is already in use.', mbError, MB_OK);
        Result := False;
      end
      else if ChkGrafana.Checked and IsPortInUse(ConfigPage.Values[1]) then
      begin
        MsgBox('Grafana port ' + ConfigPage.Values[1] + ' is already in use.', mbError, MB_OK);
        Result := False;
      end;
    end;
  end;
  
  // Validate monitor service ports (only for selected monitors, skip on upgrade)
  if CurPageID = ServicePortsPage.ID then
  begin
    if not IsUpgrade then
    begin
      if ChkIFSErrorMonitor.Checked and IsPortInUse(IFSErrorMonitorPortEdit.Text) then
      begin
        MsgBox('IFS Error Monitor port ' + IFSErrorMonitorPortEdit.Text + ' is already in use.', mbError, MB_OK);
        Result := False;
      end
      else if ChkRealTimeIFSMonitor.Checked and IsPortInUse(RealTimeIFSMonitorPortEdit.Text) then
      begin
        MsgBox('Real-Time IFS Monitor port ' + RealTimeIFSMonitorPortEdit.Text + ' is already in use.', mbError, MB_OK);
        Result := False;
      end
      else if ChkJobQueCountMonitor.Checked and IsPortInUse(JobQueCountMonitorPortEdit.Text) then
      begin
        MsgBox('Job Queue Count Monitor port ' + JobQueCountMonitorPortEdit.Text + ' is already in use.', mbError, MB_OK);
        Result := False;
      end
      else if ChkJobQueStatusMonitor.Checked and IsPortInUse(JobQueStatusMonitorPortEdit.Text) then
      begin
        MsgBox('Job Queue Status Monitor port ' + JobQueStatusMonitorPortEdit.Text + ' is already in use.', mbError, MB_OK);
        Result := False;
      end
      else if ChkServerUpTimeMonitor.Checked and IsPortInUse(ServerUpTimeMonitorPortEdit.Text) then
      begin
        MsgBox('Server UpTime Monitor port ' + ServerUpTimeMonitorPortEdit.Text + ' is already in use.', mbError, MB_OK);
        Result := False;
      end
      else if ChkSubSystemMonitor.Checked and IsPortInUse(SubSystemMonitorPortEdit.Text) then
      begin
        MsgBox('SubSystem Monitor port ' + SubSystemMonitorPortEdit.Text + ' is already in use.', mbError, MB_OK);
        Result := False;
      end
      else if ChkSystemMatrix.Checked and IsPortInUse(SystemMatrixPortEdit.Text) then
      begin
        MsgBox('System Matrix port ' + SystemMatrixPortEdit.Text + ' is already in use.', mbError, MB_OK);
        Result := False;
      end
      else if ChkUserProfileChecker.Checked and IsPortInUse(UserProfileCheckerPortEdit.Text) then
      begin
        MsgBox('User Profile Checker port ' + UserProfileCheckerPortEdit.Text + ' is already in use.', mbError, MB_OK);
        Result := False;
      end
      else if ChkNetWorkEnabler.Checked and IsPortInUse(NetWorkEnablerPortEdit.Text) then
      begin
        MsgBox('Network Enabler port ' + NetWorkEnablerPortEdit.Text + ' is already in use.', mbError, MB_OK);
        Result := False;
      end
      else if ChkQSYSOPRMonitor.Checked and IsPortInUse(QSYSOPRMonitorPortEdit.Text) then
      begin
        MsgBox('QSYSOPR Monitor port ' + QSYSOPRMonitorPortEdit.Text + ' is already in use.', mbError, MB_OK);
        Result := False;
      end
      else if ChkWinFSErrorMonitor.Checked and IsPortInUse(WinFSErrorMonitorPortEdit.Text) then
      begin
        MsgBox('Windows FS Error Monitor port ' + WinFSErrorMonitorPortEdit.Text + ' is already in use.', mbError, MB_OK);
        Result := False;
      end
      else if ChkWinFSCardinalityMonitor.Checked and IsPortInUse(WinFSCardinalityMonitorPortEdit.Text) then
      begin
        MsgBox('Windows FS Cardinality Monitor port ' + WinFSCardinalityMonitorPortEdit.Text + ' is already in use.', mbError, MB_OK);
        Result := False;
      end;
    end;
    
    // Check for duplicate ports between selected monitors and core services
    if Result then
      Result := ValidateNoDuplicatePorts;
  end;

  // Validate optional extra port scan page
  if CurPageID = ExtraPortsPage.ID then
  begin
    if ExtraPortsMemo <> nil then
    begin
      CustomPortConflict := CheckCustomPortsFree(ExtraPortsMemo.Text);
      if CustomPortConflict <> '' then
      begin
        MsgBox('Port ' + CustomPortConflict + ' is already in use. Please choose a free port or remove it from the list.', mbError, MB_OK);
        Result := False;
      end;
    end;
  end;
  
  // Validate IBM i config
  if CurPageID = IBMiConfigPage.ID then
  begin
    if (IBMiConfigPage.Values[0] = '') or (IBMiConfigPage.Values[1] = '') then
    begin
      MsgBox('IBM i Server and User are required.', mbError, MB_OK);
      Result := False;
    end;
  end;
  
  // Validate Loki config
  if CurPageID = LokiConfigPage.ID then
  begin
    if Trim(LokiConfigPage.Values[0]) = '' then
    begin
      MsgBox('Loki port is required.', mbError, MB_OK);
      Result := False;
    end
    else if not IsUpgrade and IsPortInUse(LokiConfigPage.Values[0]) then
    begin
      MsgBox('Loki port ' + LokiConfigPage.Values[0] + ' is already in use.', mbError, MB_OK);
      Result := False;
    end;
  end;
  
  // Validate Log Agent (Promtail) config
  if CurPageID = LogAgentConfigPage.ID then
  begin
    if Trim(LogAgentHostEdit.Text) = '' then
    begin
      MsgBox('Loki host is required.', mbError, MB_OK);
      Result := False;
    end
    else if Trim(LogAgentPortEdit.Text) = '' then
    begin
      MsgBox('Loki port is required.', mbError, MB_OK);
      Result := False;
    end
    else if Trim(LogAgentLogPathEdit.Text) = '' then
    begin
      MsgBox('Log path is required.', mbError, MB_OK);
      Result := False;
    end;
    // Note: Skip directory validation for wildcard patterns (e.g., C:/logs/*.log)
    // Wildcards are valid and expected for log collection
  end;
end;

procedure UpdatePrometheusYaml(Port: string);
var
  Path: string;
  Content: AnsiString;
  ContentStr, Marker: string;
begin
  Path := ExpandConstant('{app}\prometheus\prometheus.yml');
  Marker := '# --- IP Monitor Jobs Below ---';
  
  if LoadStringFromFile(Path, Content) then
  begin
    ContentStr := String(Content);
    StringChangeEx(ContentStr, '["localhost:9090"]', '["localhost:' + Port + '"]', True);
    StringChangeEx(ContentStr, 'localhost:9090', 'localhost:' + Port, True);
    
    // Ensure marker exists for monitor job additions
    if Pos(Marker, ContentStr) = 0 then
      ContentStr := ContentStr + #13#10 + Marker + #13#10;
    
    SaveStringToFile(Path, ContentStr, False);
  end;
end;

procedure GenerateGrafanaIni(Port, Password: string);
begin
  SaveStringToFile(ExpandConstant('{app}\grafana\conf\custom.ini'),
    '[server]'#13#10 +
    'http_port=' + Port + #13#10#13#10 +
    '[security]'#13#10 +
    'admin_password=' + Password, False);
end;

// Update Grafana Prometheus datasource with correct port
procedure UpdateGrafanaDatasource(PrometheusPort: string);
var
  DsFile: string;
  CRLF: string;
begin
  CRLF := #13#10;
  DsFile := ExpandConstant('{app}\grafana\conf\provisioning\datasources\prometheus.yaml');
  SaveStringToFile(DsFile,
    '# Prometheus datasource configuration for Island Pacific Monitoring' + CRLF +
    'apiVersion: 1' + CRLF + CRLF +
    'datasources:' + CRLF +
    '  - name: Prometheus' + CRLF +
    '    type: prometheus' + CRLF +
    '    access: proxy' + CRLF +
    '    orgId: 1' + CRLF +
    '    uid: prometheus_ip' + CRLF +
    '    url: http://localhost:' + PrometheusPort + CRLF +
    '    basicAuth: false' + CRLF +
    '    isDefault: true' + CRLF +
    '    editable: true' + CRLF +
    '    jsonData:' + CRLF +
    '      httpMethod: POST' + CRLF +
    '      manageAlerts: true' + CRLF +
    '      prometheusType: Prometheus' + CRLF,
    False);
end;

// Update Grafana Loki datasource with correct port
procedure UpdateGrafanaLokiDatasource(LokiPort: string);
var
  DsFile: string;
  CRLF: string;
begin
  CRLF := #13#10;
  DsFile := ExpandConstant('{app}\grafana\conf\provisioning\datasources\loki.yaml');
  SaveStringToFile(DsFile,
    '# Loki datasource configuration for Island Pacific Monitoring' + CRLF +
    'apiVersion: 1' + CRLF + CRLF +
    'datasources:' + CRLF +
    '  - name: Loki' + CRLF +
    '    type: loki' + CRLF +
    '    access: proxy' + CRLF +
    '    orgId: 1' + CRLF +
    '    uid: loki_ip' + CRLF +
    '    url: http://localhost:' + LokiPort + CRLF +
    '    basicAuth: false' + CRLF +
    '    isDefault: false' + CRLF +
    '    editable: true' + CRLF +
    '    jsonData:' + CRLF +
    '      maxLines: 1000' + CRLF,
    False);
  Log('Created Loki datasource at: ' + DsFile);
end;

// Update Grafana dashboard provider with correct absolute path
procedure UpdateGrafanaDashboardProvider;
var
  DpFile, AppPath, FolderName, FolderUid: string;
  CRLF: string;
begin
  CRLF := #13#10;
  AppPath := ExpandConstant('{app}');
  // Convert backslashes to forward slashes for YAML compatibility
  StringChangeEx(AppPath, '\', '/', True);
  
  // Use client name for folder if specified, otherwise use default
  if ClientInstanceId <> '' then
  begin
    FolderName := ClientInstanceId;
    FolderUid := LowerCase(ClientInstanceId);
  end
  else
  begin
    FolderName := 'Island Pacific';
    FolderUid := 'island-pacific';
  end;
  
  DpFile := ExpandConstant('{app}\grafana\conf\provisioning\dashboards\ip-dashboards.yaml');
  SaveStringToFile(DpFile,
    'apiVersion: 1' + CRLF + CRLF +
    'providers:' + CRLF +
    '  - name: ''' + FolderName + ' Dashboards''' + CRLF +
    '    orgId: 1' + CRLF +
    '    folder: ''' + FolderName + '''' + CRLF +
    '    folderUid: ''' + FolderUid + '''' + CRLF +
    '    type: file' + CRLF +
    '    disableDeletion: false' + CRLF +
    '    updateIntervalSeconds: 30' + CRLF +
    '    allowUiUpdates: true' + CRLF +
    '    options:' + CRLF +
    '      path: ''' + AppPath + '/grafana/dashboards''' + CRLF,
    False);
end;

procedure GenerateServiceXml(ServiceExeName, ServiceId, ServiceName, Description, Executable, Arguments, WorkDir: string);
var
  D: string;
begin
  D := ExpandConstant('{app}');
  // Include OnFailure restart logic for automatic recovery
  // WinSW handles paths with spaces internally - no quotes needed in XML tags
  SaveStringToFile(D + '\services\' + ServiceExeName + '.xml',
    '<service>'#13#10 +
    '  <id>' + ServiceId + '</id>'#13#10 +
    '  <name>' + ServiceName + '</name>'#13#10 +
    '  <description>' + Description + '</description>'#13#10 +
    '  <executable>' + Executable + '</executable>'#13#10 +
    '  <arguments>' + Arguments + '</arguments>'#13#10 +
    '  <workingdirectory>' + WorkDir + '</workingdirectory>'#13#10 +
    '  <log mode="roll"></log>'#13#10 +
    '  <onfailure action="restart" delay="10 sec"/>'#13#10 +
    '  <onfailure action="restart" delay="20 sec"/>'#13#10 +
    '  <onfailure action="restart" delay="30 sec"/>'#13#10 +
    '  <resetfailure>1 hour</resetfailure>'#13#10 +
    '</service>', False);
end;

// Update IBM i settings in properties file - handles different key names
procedure UpdateIBMiSettings(PropsFile: string);
var
  Lines: TArrayOfString;
  i: Integer;
  Line, Output: string;
  Server, User, Password, Client: string;
begin
  Server := GetIBMiServer('');
  User := GetIBMiUser('');
  Password := GetIBMiPassword('');
  Client := GetClientName('');
  
  if not LoadStringsFromFile(PropsFile, Lines) then Exit;
  
  Output := '';
  for i := 0 to GetArrayLength(Lines) - 1 do
  begin
    Line := Lines[i];
    // Update IBM i connection settings
    if (Pos('ibmi.server=', Line) = 1) then
      Line := 'ibmi.server=' + Server
    else if (Pos('ibmi.hosts=', Line) = 1) then
      Line := 'ibmi.hosts=' + Server
    else if (Pos('ibmi.host=', Line) = 1) then
      Line := 'ibmi.host=' + Server
    else if (Pos('ibmi.user=', Line) = 1) then
      Line := 'ibmi.user=' + User
    else if (Pos('ibmi.password=', Line) = 1) then
      Line := 'ibmi.password=' + Password
    else if (Pos('client.name=', Line) = 1) then
      Line := 'client.name=' + Client
    else if (Pos('client.monitor=', Line) = 1) then
      Line := 'client.monitor=' + Client
    else if (Pos('monitor.server=', Line) = 1) then
      Line := 'monitor.server=' + Client;
    
    if i > 0 then
      Output := Output + #13#10;
    Output := Output + Line;
  end;
  SaveStringToFile(PropsFile, Output, False);
end;

// Generic procedure to update a property value in a file
// Update metrics/exporter port in properties file - handles different key names
procedure UpdateMetricsPort(PropsFile, Port: string);
var
  Lines: TArrayOfString;
  i: Integer;
  Line, Output: string;
begin
  if not LoadStringsFromFile(PropsFile, Lines) then Exit;
  
  Output := '';
  for i := 0 to GetArrayLength(Lines) - 1 do
  begin
    Line := Lines[i];
    // Check for port keys and update value
    if (Pos('metrics.port=', Line) = 1) then
      Line := 'metrics.port=' + Port
    else if (Pos('prometheus.port=', Line) = 1) then
      Line := 'prometheus.port=' + Port
    else if (Pos('exporter.port=', Line) = 1) then
      Line := 'exporter.port=' + Port
    else if (Pos('metrics.exporter.port=', Line) = 1) then
      Line := 'metrics.exporter.port=' + Port;
    
    if i > 0 then
      Output := Output + #13#10;
    Output := Output + Line;
  end;
  SaveStringToFile(PropsFile, Output, False);
end;

// Update client.name in Windows monitor properties file
procedure UpdateWinClientName(PropsFile, ClientName: string);
var
  Lines: TArrayOfString;
  i: Integer;
  Line, Output: string;
  FoundClientName: Boolean;
begin
  if not LoadStringsFromFile(PropsFile, Lines) then Exit;
  
  Output := '';
  FoundClientName := False;
  for i := 0 to GetArrayLength(Lines) - 1 do
  begin
    Line := Lines[i];
    // Check for client.name key and update value
    if (Pos('client.name=', Line) = 1) then
    begin
      Line := 'client.name=' + ClientName;
      FoundClientName := True;
    end;
    
    if i > 0 then
      Output := Output + #13#10;
    Output := Output + Line;
  end;
  
  // If client.name wasn't found, add it at the end
  if not FoundClientName then
    Output := Output + #13#10 + 'client.name=' + ClientName;
  
  SaveStringToFile(PropsFile, Output, False);
end;

// Generate email.properties content - supports both SMTP and OAuth2
procedure GenerateEmailProperties(ServicePath: string);
var
  EmailFile, AuthMethod, TenantId, TokenUrl, GraphUrl, Scope, Content: string;
  CRLF: string;
begin
  CRLF := #13#10;
  EmailFile := ServicePath + '\email.properties';
  AuthMethod := GetEmailAuthMethod('');
  TenantId := GetOAuthTenant('');
  Scope := GetOAuthScope('');
  
  // Use user-provided URLs or fall back to auto-generated ones
  TokenUrl := GetOAuthTokenUrl('');
  GraphUrl := GetOAuthMailUrl('');
  
  Content := '# ===============================' + CRLF;
  Content := Content + '# Email Configuration' + CRLF;
  Content := Content + '# ===============================' + CRLF;
  Content := Content + CRLF;
  Content := Content + '# Authentication Method: SMTP or OAUTH2' + CRLF;
  Content := Content + 'mail.auth.method=' + AuthMethod + CRLF;
  Content := Content + CRLF;
  Content := Content + '# SMTP server details' + CRLF;
  Content := Content + 'mail.smtp.host=' + GetSmtpHost('') + CRLF;
  Content := Content + 'mail.smtp.port=' + GetSmtpPort('') + CRLF;
  Content := Content + CRLF;
  Content := Content + '# SMTP Authentication (used when mail.auth.method=SMTP)' + CRLF;
  Content := Content + 'mail.smtp.auth=' + GetSmtpAuth('') + CRLF;
  Content := Content + 'mail.smtp.starttls.enable=' + GetSmtpStartTls('') + CRLF;
  Content := Content + 'mail.smtp.username=' + GetSmtpUsername('') + CRLF;
  Content := Content + 'mail.smtp.password=' + GetSmtpPassword('') + CRLF;
  Content := Content + CRLF;
  Content := Content + '# OAuth2 Configuration (used when mail.auth.method=OAUTH2)' + CRLF;
  Content := Content + '# Microsoft 365 / Azure AD OAuth2 settings' + CRLF;
  Content := Content + 'mail.oauth2.tenant.id=' + TenantId + CRLF;
  Content := Content + 'mail.oauth2.client.id=' + GetOAuthClientId('') + CRLF;
  Content := Content + 'mail.oauth2.client.secret=' + GetOAuthClientSecret('') + CRLF;
  Content := Content + 'mail.oauth2.scope=' + Scope + CRLF;
  Content := Content + 'mail.oauth2.token.url=' + TokenUrl + CRLF;
  Content := Content + 'mail.oauth2.graph.mail.url=' + GraphUrl + CRLF;
  Content := Content + 'mail.oauth2.from.user=' + GetOAuthFromUser('') + CRLF;
  Content := Content + CRLF;
  Content := Content + '# Sender and recipients' + CRLF;
  Content := Content + 'mail.from=' + GetEmailFrom('') + CRLF;
  Content := Content + 'mail.to=' + GetEmailTo('') + CRLF;
  Content := Content + 'mail.bcc=' + GetEmailBcc('') + CRLF;
  Content := Content + CRLF;
  Content := Content + '# Email importance (High, Normal, Low)' + CRLF;
  Content := Content + 'mail.importance=' + GetEmailImportance('') + CRLF;
  Content := Content + CRLF;
  Content := Content + '# Optional: Client name to appear in subject line' + CRLF;
  Content := Content + 'mail.clientName=' + GetClientNameEmail('') + CRLF;
  Content := Content + CRLF;
  Content := Content + '# ===============================' + CRLF;
  Content := Content + '# Logging Configuration' + CRLF;
  Content := Content + '# ===============================' + CRLF;
  Content := Content + '# Log retention (days) - files older than this will be purged' + CRLF;
  Content := Content + 'log.retention.days=30' + CRLF;
  Content := Content + CRLF;
  Content := Content + '# Log purge interval (hours) - how often to run purge (24 = daily)' + CRLF;
  Content := Content + 'log.purge.interval.hours=24' + CRLF;
  
  SaveStringToFile(EmailFile, Content, False);
end;

// ===== LOKI CONFIGURATION GENERATION =====
procedure GenerateLokiYaml;
var
  D, TemplatePath, OutputPath, DataDir: string;
  Content: AnsiString;
  ContentStr: string;
begin
  D := ExpandConstant('{app}');
  TemplatePath := D + '\loki\loki.yaml.template';
  OutputPath := D + '\loki\loki.yaml';
  DataDir := GetLokiDataDir('');
  
  // Convert backslashes to forward slashes for YAML
  StringChangeEx(DataDir, '\', '/', True);
  
  if LoadStringFromFile(TemplatePath, Content) then
  begin
    ContentStr := String(Content);
    StringChangeEx(ContentStr, '{{LOKI_PORT}}', GetLokiPort(''), True);
    StringChangeEx(ContentStr, '{{DATA_DIR}}', DataDir, True);
    SaveStringToFile(OutputPath, ContentStr, False);
    Log('GenerateLokiYaml: Generated ' + OutputPath);
  end
  else
    Log('GenerateLokiYaml: Failed to load template ' + TemplatePath);
end;

// ===== PROMTAIL CONFIGURATION GENERATION =====
procedure GeneratePromtailYaml;
var
  D, TemplatePath, OutputPath, LogPath, PositionsFile: string;
  Content: AnsiString;
  ContentStr: string;
begin
  D := ExpandConstant('{app}');
  TemplatePath := D + '\promtail\promtail.yaml.template';
  OutputPath := D + '\promtail\promtail.yaml';
  PositionsFile := D + '\promtail\positions.yaml';
  LogPath := GetLogAgentLogPath('');
  
  // Convert backslashes to forward slashes for YAML
  StringChangeEx(PositionsFile, '\', '/', True);
  StringChangeEx(LogPath, '\', '/', True);
  
  if LoadStringFromFile(TemplatePath, Content) then
  begin
    ContentStr := String(Content);
    StringChangeEx(ContentStr, '{{LOKI_HOST}}', GetLokiHost(''), True);
    StringChangeEx(ContentStr, '{{LOKI_PORT}}', LogAgentPortEdit.Text, True);
    StringChangeEx(ContentStr, '{{APP_NAME}}', GetLogAgentAppName(''), True);
    StringChangeEx(ContentStr, '{{ENVIRONMENT}}', GetLogAgentEnvironment(''), True);
    StringChangeEx(ContentStr, '{{LOG_PATH}}', LogPath, True);
    StringChangeEx(ContentStr, '{{POSITIONS_FILE}}', PositionsFile, True);
    SaveStringToFile(OutputPath, ContentStr, False);
    Log('GeneratePromtailYaml: Generated ' + OutputPath);
  end
  else
    Log('GeneratePromtailYaml: Failed to load template ' + TemplatePath);
end;

// ===== LOKI SERVICE XML GENERATION =====
procedure GenerateLokiServiceXml;
var
  D, ServiceId, ServiceName: string;
begin
  D := ExpandConstant('{app}');
  // Use generic core service ID and name (shared across clients)
  ServiceId := 'IPMonitoring-Loki';
  ServiceName := 'IP Monitoring - Loki';
  SaveStringToFile(D + '\services\IPMonitoring-Loki.xml',
    '<service>'#13#10 +
    '  <id>' + ServiceId + '</id>'#13#10 +
    '  <name>' + ServiceName + '</name>'#13#10 +
    '  <description>Loki log aggregation server</description>'#13#10 +
    '  <executable>' + D + '\loki\loki.exe</executable>'#13#10 +
    '  <arguments>-config.file="' + D + '\loki\loki.yaml"</arguments>'#13#10 +
    '  <workingdirectory>' + D + '\loki</workingdirectory>'#13#10 +
    '  <log mode="roll"></log>'#13#10 +
    '  <onfailure action="restart" delay="10 sec"/>'#13#10 +
    '  <onfailure action="restart" delay="20 sec"/>'#13#10 +
    '  <onfailure action="restart" delay="30 sec"/>'#13#10 +
    '</service>', False);
end;

// ===== PROMTAIL/LOG AGENT SERVICE XML GENERATION =====
procedure GenerateLogAgentServiceXml;
var
  D, ServiceId, ServiceName: string;
begin
  D := ExpandConstant('{app}');
  // Use client-specific service ID to prevent collisions
  ServiceId := 'IPMonitoring-LogAgent_' + ClientInstanceId;
  ServiceName := 'IP Monitoring - Log Agent [' + ClientInstanceId + ']';
  SaveStringToFile(D + '\services\IPMonitoring-LogAgent.xml',
    '<service>'#13#10 +
    '  <id>' + ServiceId + '</id>'#13#10 +
    '  <name>' + ServiceName + '</name>'#13#10 +
    '  <description>Promtail log shipping agent for ' + ClientInstanceId + '</description>'#13#10 +
    '  <executable>' + D + '\promtail\promtail.exe</executable>'#13#10 +
    '  <arguments>-config.file="' + D + '\promtail\promtail.yaml"</arguments>'#13#10 +
    '  <workingdirectory>' + D + '\promtail</workingdirectory>'#13#10 +
    '  <log mode="roll"></log>'#13#10 +
    '  <onfailure action="restart" delay="10 sec"/>'#13#10 +
    '  <onfailure action="restart" delay="20 sec"/>'#13#10 +
    '  <onfailure action="restart" delay="30 sec"/>'#13#10 +
    '</service>', False);
end;

// Remove old Prometheus scrape target by exact job name
procedure RemovePrometheusTarget(JobName: string);
var
  Path: string;
  Content: AnsiString;
  ContentStr, SearchStr: string;
  JobStartPos, JobEndPos, SearchPos, LineStart: Integer;
  ResultContent: AnsiString;
begin
  // For multi-client setup, use existing Prometheus installation path
  if not IsFirstInstallation and (ExistingInstallPath <> '') then
    Path := ExistingInstallPath + '\prometheus\prometheus.yml'
  else
    Path := ExpandConstant('{app}\prometheus\prometheus.yml');
  
  if not LoadStringFromFile(Path, Content) then
  begin
    Log('RemovePrometheusTarget: Could not load ' + Path);
    Exit;
  end;
  
  ContentStr := String(Content);
  // Use more specific search pattern with trailing quote and newline to avoid partial matches
  // e.g., 'JobA' should not match 'JobA-Backup'
  SearchStr := 'job_name: "' + JobName + '"';
  JobStartPos := Pos(SearchStr, ContentStr);
  
  // Verify this is an exact match by checking what follows the job name
  // Valid matches should have newline or space after the closing quote
  if JobStartPos > 0 then
  begin
    // Check character after the search string - must be whitespace or newline
    if (JobStartPos + Length(SearchStr) <= Length(ContentStr)) then
    begin
      if not ((ContentStr[JobStartPos + Length(SearchStr)] = #13) or 
              (ContentStr[JobStartPos + Length(SearchStr)] = #10) or
              (ContentStr[JobStartPos + Length(SearchStr)] = ' ')) then
      begin
        // This is a partial match (e.g., 'JobA' matching 'JobA-Backup'), skip it
        Log('RemovePrometheusTarget: Partial match for "' + JobName + '" found but skipped (not exact match)');
        JobStartPos := 0;
      end;
    end;
  end;
  
  if JobStartPos = 0 then
  begin
    Log('RemovePrometheusTarget: Job "' + JobName + '" not found, skipping');
    Exit;
  end;
  
  Log('RemovePrometheusTarget: Found exact job "' + JobName + '" at pos ' + IntToStr(JobStartPos));
  
  // Find start of this job entry (go back to find the beginning of the line)
  LineStart := JobStartPos;
  while (LineStart > 1) and (ContentStr[LineStart - 1] <> #10) do
    LineStart := LineStart - 1;
  
  Log('RemovePrometheusTarget: LineStart at pos ' + IntToStr(LineStart));
  
  // Find end of this job block - look for next '  - job_name:' or end of scrape_configs
  SearchPos := JobStartPos + Length(SearchStr);
  JobEndPos := 0;
  
  while SearchPos <= Length(ContentStr) do
  begin
    // Check for next job entry (must be at start of line after newline)
    if (ContentStr[SearchPos] = #10) and (SearchPos + 15 <= Length(ContentStr)) then
    begin
      if Copy(ContentStr, SearchPos + 1, 14) = '  - job_name:' then
      begin
        JobEndPos := SearchPos + 1;
        Break;
      end;
    end;
    SearchPos := SearchPos + 1;
  end;
  
  // If no next job found, go to end of file (keep trailing newline)
  if JobEndPos = 0 then
    JobEndPos := Length(ContentStr) + 1;
  
  Log('RemovePrometheusTarget: JobEndPos at pos ' + IntToStr(JobEndPos));
  
  // Remove the job block (from LineStart to JobEndPos-1)
  ContentStr := Copy(ContentStr, 1, LineStart - 1) + Copy(ContentStr, JobEndPos, Length(ContentStr) - JobEndPos + 1);
  
  ResultContent := AnsiString(ContentStr);
  if SaveStringToFile(Path, ResultContent, False) then
    Log('RemovePrometheusTarget: Successfully removed job "' + JobName + '"')
  else
    Log('RemovePrometheusTarget: FAILED to save ' + Path);
end;

// Add Prometheus scrape target with client label for multi-client support
procedure AddPrometheusTarget(JobName, Port: string);
var
  Path: string;
  Content: AnsiString;
  ContentStr, NewJob, Marker, ClientLabel: string;
  MarkerPos: Integer;
begin
  // For multi-client setup, use existing Prometheus installation path
  if not IsFirstInstallation and (ExistingInstallPath <> '') then
    Path := ExistingInstallPath + '\prometheus\prometheus.yml'
  else
    Path := ExpandConstant('{app}\prometheus\prometheus.yml');
  
  Log('AddPrometheusTarget: Adding job "' + JobName + '" to ' + Path);
  
  Marker := '# --- IP Monitor Jobs Below ---';
  ClientLabel := ClientInstanceId;
  if ClientLabel = '' then ClientLabel := 'Default';
  
  if LoadStringFromFile(Path, Content) then
  begin
    ContentStr := String(Content);
    // Check if job already exists (with client-specific name)
    if Pos('job_name: "' + JobName + '"', ContentStr) = 0 then
    begin
      // Add client label to metrics for filtering in Grafana dashboards
      NewJob := 
        '  - job_name: "' + JobName + '"'#13#10 +
        '    static_configs:'#13#10 +
        '      - targets: ["localhost:' + Port + '"]'#13#10 +
        '        labels:'#13#10 +
        '          app: "' + JobName + '"'#13#10 +
        '          client: "' + ClientLabel + '"'#13#10 +
        '          instance_id: "' + ClientLabel + '"'#13#10#13#10;
      
      // Check if marker exists
      MarkerPos := Pos(Marker, ContentStr);
      if MarkerPos > 0 then
      begin
        Log('AddPrometheusTarget: Marker found at position ' + IntToStr(MarkerPos));
        // Insert job after the marker line
        MarkerPos := MarkerPos + Length(Marker);
        // Skip past the newline after marker
        while (MarkerPos <= Length(ContentStr)) and ((ContentStr[MarkerPos] = #13) or (ContentStr[MarkerPos] = #10)) do
          MarkerPos := MarkerPos + 1;
        ContentStr := Copy(ContentStr, 1, MarkerPos - 1) + #13#10 + NewJob + Copy(ContentStr, MarkerPos, Length(ContentStr) - MarkerPos + 1);
      end
      else
      begin
        Log('AddPrometheusTarget: Marker NOT found, appending to end');
        // Add marker at end of scrape_configs section and then the job
        ContentStr := ContentStr + #13#10 + Marker + #13#10 + NewJob;
      end;
      
      if SaveStringToFile(Path, ContentStr, False) then
        Log('AddPrometheusTarget: Successfully saved ' + Path)
      else
        Log('AddPrometheusTarget: FAILED to save ' + Path);
    end
    else
      Log('AddPrometheusTarget: Job "' + JobName + '" already exists, skipping');
  end
  else
    Log('AddPrometheusTarget: FAILED to load ' + Path);
end;

procedure InstallAndStartService(ServiceExe: string);
var
  Code: Integer;
  D, FullPath, ServiceName: string;
  ServiceWasInstalled: Boolean;
begin
  D := ExpandConstant('{app}');
  FullPath := D + '\services\' + ServiceExe;
  
  // Check if service executable exists
  if not FileExists(FullPath) then
  begin
    Log('ERROR: Service executable not found: ' + FullPath);
    Exit;
  end;
  
  // Extract service name (remove .exe extension)
  ServiceName := Copy(ServiceExe, 1, Length(ServiceExe) - 4);
  
  // Check if service is currently installed
  ServiceWasInstalled := ServiceExists(ServiceName);
  
  if ServiceWasInstalled then
  begin
    // Stop existing service first (in case of upgrade)
    Log('Stopping existing service: ' + ServiceExe);
    Exec(FullPath, 'stop', '', SW_HIDE, ewWaitUntilTerminated, Code);
    Sleep(500); // Wait for service to stop
    
    Log('Uninstalling existing service: ' + ServiceExe);
    Exec(FullPath, 'uninstall', '', SW_HIDE, ewWaitUntilTerminated, Code);
    Sleep(500); // Wait for uninstall to complete
  end
  else
  begin
    Log('New service installation: ' + ServiceExe);
  end;
  
  // Install the service
  if Exec(FullPath, 'install', '', SW_HIDE, ewWaitUntilTerminated, Code) then
  begin
    Log('Service installed: ' + ServiceExe + ' (Exit code: ' + IntToStr(Code) + ')');
    Sleep(1000); // Wait for service registration to complete
    
    // Start the service
    if Exec(FullPath, 'start', '', SW_HIDE, ewWaitUntilTerminated, Code) then
      Log('Service started: ' + ServiceExe + ' (Exit code: ' + IntToStr(Code) + ')')
    else
      Log('ERROR: Failed to start service: ' + ServiceExe);
  end
  else
    Log('ERROR: Failed to install service: ' + ServiceExe);
end;

procedure RestartService(ServiceExe: string);
var
  Code: Integer;
  D: string;
begin
  D := ExpandConstant('{app}');
  // Stop and restart the service to reload configuration
  Exec(D + '\services\' + ServiceExe, 'stop', '', SW_HIDE, ewWaitUntilTerminated, Code);
  Sleep(1000); // Wait 1 second for service to fully stop
  Exec(D + '\services\' + ServiceExe, 'start', '', SW_HIDE, ewWaitUntilTerminated, Code);
end;

procedure UninstallServiceIfPresent(BaseServiceId: string);
var
  Code: Integer;
  ServicesDir: string;
  ExeGeneric, ExeClient: string;
  NameGeneric, NameClient: string;
  UseGeneric: Boolean;
begin
  ServicesDir := ExpandConstant('{app}\services');
  ExeGeneric := ServicesDir + '\' + BaseServiceId + '.exe';
  ExeClient := ServicesDir + '\' + BaseServiceId + '_' + ClientInstanceId + '.exe';
  NameGeneric := BaseServiceId;
  NameClient := BaseServiceId + '_' + ClientInstanceId;

  // Decide which form exists: prefer client-suffixed when present; otherwise generic.
  UseGeneric := False;
  if (ClientInstanceId <> '') and (FileExists(ExeClient) or ServiceExists(NameClient)) then
    UseGeneric := False
  else if FileExists(ExeGeneric) or ServiceExists(NameGeneric) then
    UseGeneric := True
  else
  begin
    Log('UninstallServiceIfPresent: nothing to uninstall for ' + BaseServiceId + ' (no matching service found)');
    Exit;
  end;

  // Stop/uninstall via WinSW executable when available, else fallback to sc.exe using the resolved name.
  if UseGeneric then
  begin
    Log('UninstallServiceIfPresent: stopping and uninstalling generic service ' + NameGeneric);
    if FileExists(ExeGeneric) then
    begin
      Exec(ExeGeneric, 'stop', '', SW_HIDE, ewWaitUntilTerminated, Code);
      Sleep(300);
      Exec(ExeGeneric, 'uninstall', '', SW_HIDE, ewWaitUntilTerminated, Code);
      Sleep(300);
    end;
    Exec('sc.exe', 'stop "' + NameGeneric + '"', '', SW_HIDE, ewWaitUntilTerminated, Code);
    Exec('sc.exe', 'delete "' + NameGeneric + '"', '', SW_HIDE, ewWaitUntilTerminated, Code);
  end
  else
  begin
    Log('UninstallServiceIfPresent: stopping and uninstalling client-suffixed service ' + NameClient);
    if FileExists(ExeClient) then
    begin
      Exec(ExeClient, 'stop', '', SW_HIDE, ewWaitUntilTerminated, Code);
      Sleep(300);
      Exec(ExeClient, 'uninstall', '', SW_HIDE, ewWaitUntilTerminated, Code);
      Sleep(300);
    end;
    Exec('sc.exe', 'stop "' + NameClient + '"', '', SW_HIDE, ewWaitUntilTerminated, Code);
    Exec('sc.exe', 'delete "' + NameClient + '"', '', SW_HIDE, ewWaitUntilTerminated, Code);
  end;
end;

// Restart a service at a specific path (used for multi-client installs to restart shared Prometheus)
procedure RestartServiceAtPath(ServiceExePath: string);
var
  Code: Integer;
begin
  // Stop and restart the service to reload configuration
  Exec(ServiceExePath, 'stop', '', SW_HIDE, ewWaitUntilTerminated, Code);
  Sleep(1000); // Wait 1 second for service to fully stop
  Exec(ServiceExePath, 'start', '', SW_HIDE, ewWaitUntilTerminated, Code);
end;

// Helper function to install a Java-based monitor service
// Parameters:
//   FolderName: Name of folder under monitoring-services (e.g., 'IBMIFSErrorMonitor')
//   ServiceId: WinSW service ID (e.g., 'IPMonitoring_IBMIFSErrorMonitor')
//   DisplayName: Service display name (e.g., 'IP Monitoring - IFS Error Monitor')
//   Description: Service description
//   JarName: Name of the JAR file (e.g., 'IBMIFSErrorMonitor.jar')
//   PropsFile: Properties file name (e.g., 'monitor.properties')
//   PrometheusJob: Prometheus job name (e.g., 'ibm-ifs-error-monitor')
//   Port: Service port
//   NeedsIBMi: Whether to update IBM i settings
procedure InstallJavaMonitor(FolderName, ServiceId, DisplayName, Description, JarName, PropsFile, PrometheusJob, Port: string; NeedsIBMi: Boolean);
var
  D, ServicePath, ClientSuffix, ClientServiceId, ClientDisplayName, ClientPrometheusJob: string;
  SourceExe, DestExe: string;
begin
  D := ExpandConstant('{app}');
  ServicePath := D + '\monitoring-services\' + FolderName;
  
  // For multi-client support, append client ID to service identifiers
  if ClientInstanceId <> '' then
  begin
    ClientSuffix := '_' + ClientInstanceId;
    ClientServiceId := ServiceId + ClientSuffix;
    ClientDisplayName := DisplayName + ' [' + ClientInstanceId + ']';
    ClientPrometheusJob := PrometheusJob + '-' + LowerCase(ClientInstanceId);
    
    // Copy WinSW.exe with client-specific name for multi-client installations
    SourceExe := D + '\services\' + ServiceId + '.exe';
    DestExe := D + '\services\' + ClientServiceId + '.exe';
    if FileExists(SourceExe) and not FileExists(DestExe) then
      CopyFile(SourceExe, DestExe, False);
  end
  else
  begin
    ClientSuffix := '';
    ClientServiceId := ServiceId;
    ClientDisplayName := DisplayName;
    ClientPrometheusJob := PrometheusJob;
  end;
  
  // Update IBM i settings if needed
  if NeedsIBMi then
    UpdateIBMiSettings(ServicePath + '\' + PropsFile);
  
  // Update metrics port
  UpdateMetricsPort(ServicePath + '\' + PropsFile, Port);
  
  // Generate email properties
  GenerateEmailProperties(ServicePath);
  
  // Generate WinSW service XML with client-specific service ID
  GenerateServiceXml(ClientServiceId, ClientServiceId, ClientDisplayName, Description + ' for ' + ClientInstanceId,
    'java', '-jar "' + ServicePath + '\' + JarName + '"', ServicePath);
  
  // Remove old Prometheus targets (legacy names with underscores and old hyphen names) if upgrading
  if IsUpgrade then
  begin
    // Remove legacy underscore-style job names from original prometheus.yml
    RemovePrometheusTarget('ifs_error_monitor');
    RemovePrometheusTarget('realtime_ifs_monitor');
    RemovePrometheusTarget('job_queue_count_monitor');
    RemovePrometheusTarget('job_queue_status_monitor');
    RemovePrometheusTarget('server_uptime_monitor');
    RemovePrometheusTarget('subsystem_monitor');
    RemovePrometheusTarget('system_matrix_monitor');
    RemovePrometheusTarget('user_profile_checker');
    RemovePrometheusTarget('network_enabler');
    RemovePrometheusTarget('qsysopr_monitor');
    // Also remove non-client-suffixed hyphen versions
    RemovePrometheusTarget(PrometheusJob);
  end;
  
  // Add Prometheus scrape target with client-specific job name
  AddPrometheusTarget(ClientPrometheusJob, Port);
  
  // Install and start the service
  InstallAndStartService(ClientServiceId + '.exe');
end;

// Fix #5: Final port check function to verify ports are still available just before installation
function VerifyPortsStillAvailable: string;
var
  PortConflict: string;
  ExtraConflict: string;
begin
  Result := '';
  PortConflict := '';
  ExtraConflict := '';
  
  // Skip port checks during upgrades (our own services are using the ports)
  if IsUpgrade then Exit;
  
  // Check Prometheus port
  if ChkPrometheus.Checked and IsPortInUse(ConfigPage.Values[0]) then
    PortConflict := 'Prometheus port ' + ConfigPage.Values[0];
  
  // Check Grafana port  
  if (PortConflict = '') and ChkGrafana.Checked and IsPortInUse(ConfigPage.Values[1]) then
    PortConflict := 'Grafana port ' + ConfigPage.Values[1];
  
  // Check Loki port (for Monitoring Server role)
  if (PortConflict = '') and IsMonitoringServerRole and IsPortInUse(GetLokiPort('')) then
    PortConflict := 'Loki port ' + GetLokiPort('');
  
  // Check monitor ports
  if (PortConflict = '') and ChkIFSErrorMonitor.Checked and IsPortInUse(IFSErrorMonitorPortEdit.Text) then
    PortConflict := 'IFS Error Monitor port ' + IFSErrorMonitorPortEdit.Text;
  if (PortConflict = '') and ChkRealTimeIFSMonitor.Checked and IsPortInUse(RealTimeIFSMonitorPortEdit.Text) then
    PortConflict := 'Real-Time IFS Monitor port ' + RealTimeIFSMonitorPortEdit.Text;
  if (PortConflict = '') and ChkJobQueCountMonitor.Checked and IsPortInUse(JobQueCountMonitorPortEdit.Text) then
    PortConflict := 'Job Queue Count Monitor port ' + JobQueCountMonitorPortEdit.Text;
  if (PortConflict = '') and ChkJobQueStatusMonitor.Checked and IsPortInUse(JobQueStatusMonitorPortEdit.Text) then
    PortConflict := 'Job Queue Status Monitor port ' + JobQueStatusMonitorPortEdit.Text;
  if (PortConflict = '') and ChkServerUpTimeMonitor.Checked and IsPortInUse(ServerUpTimeMonitorPortEdit.Text) then
    PortConflict := 'Server UpTime Monitor port ' + ServerUpTimeMonitorPortEdit.Text;
  if (PortConflict = '') and ChkSubSystemMonitor.Checked and IsPortInUse(SubSystemMonitorPortEdit.Text) then
    PortConflict := 'SubSystem Monitor port ' + SubSystemMonitorPortEdit.Text;
  if (PortConflict = '') and ChkSystemMatrix.Checked and IsPortInUse(SystemMatrixPortEdit.Text) then
    PortConflict := 'System Matrix port ' + SystemMatrixPortEdit.Text;
  if (PortConflict = '') and ChkUserProfileChecker.Checked and IsPortInUse(UserProfileCheckerPortEdit.Text) then
    PortConflict := 'User Profile Checker port ' + UserProfileCheckerPortEdit.Text;
  if (PortConflict = '') and ChkNetWorkEnabler.Checked and IsPortInUse(NetWorkEnablerPortEdit.Text) then
    PortConflict := 'Network Enabler port ' + NetWorkEnablerPortEdit.Text;
  if (PortConflict = '') and ChkQSYSOPRMonitor.Checked and IsPortInUse(QSYSOPRMonitorPortEdit.Text) then
    PortConflict := 'QSYSOPR Monitor port ' + QSYSOPRMonitorPortEdit.Text;
  if (PortConflict = '') and ChkWinFSErrorMonitor.Checked and IsPortInUse(WinFSErrorMonitorPortEdit.Text) then
    PortConflict := 'Windows FS Error Monitor port ' + WinFSErrorMonitorPortEdit.Text;
  if (PortConflict = '') and ChkWinFSCardinalityMonitor.Checked and IsPortInUse(WinFSCardinalityMonitorPortEdit.Text) then
    PortConflict := 'Windows FS Cardinality Monitor port ' + WinFSCardinalityMonitorPortEdit.Text;

  // Check user-supplied extra ports
  if (PortConflict = '') and (ExtraPortsMemo <> nil) then
  begin
    ExtraConflict := CheckCustomPortsFree(ExtraPortsMemo.Text);
    if ExtraConflict <> '' then
      PortConflict := 'Custom port ' + ExtraConflict;
  end;
  
  if PortConflict <> '' then
    Result := PortConflict + ' is now in use by another application. Installation cannot proceed. Please close the installer, free the port, and try again.';
end;

procedure CurStepChanged(CurStep: TSetupStep);
var
  D: string;
  PortError: string;
begin
  // Fix #5: Final port verification before installation begins
  if CurStep = ssInstall then
  begin
    PortError := VerifyPortsStillAvailable;
    if PortError <> '' then
    begin
      MsgBox('Port Conflict Detected!'#13#10#13#10 + PortError, mbCriticalError, MB_OK);
      // Note: We can't abort installation at this point in Inno Setup,
      // but the error message will alert the user
      Log('CRITICAL: Port conflict detected during ssInstall: ' + PortError);
    end;
  end;
  
  if CurStep = ssPostInstall then
  begin
    D := ExpandConstant('{app}');

    // Uninstall any monitors that were deselected during upgrade/modify
    // IMPORTANT: Only act on services relevant to the current role to avoid
    // stopping unrelated services installed for other roles/clients.
    if IsMonitoringServerRole then
    begin
      // Uninstall any previously installed services that are now deselected.
      // WasServiceInstalled() guards fresh installs where nothing exists yet.
        if WasServiceInstalled('InstalledIFSErrorMonitor', 'IPMonitoring_IBMIFSErrorMonitor') and not ChkIFSErrorMonitor.Checked then
          UninstallServiceIfPresent('IPMonitoring_IBMIFSErrorMonitor');
        if WasServiceInstalled('InstalledRealTimeIFSMonitor', 'IPMonitoring_IBMRealTimeIFSMonitor') and not ChkRealTimeIFSMonitor.Checked then
          UninstallServiceIfPresent('IPMonitoring_IBMRealTimeIFSMonitor');
        if WasServiceInstalled('InstalledJobQueCountMonitor', 'IPMonitoring_IBMJobQueCountMonitor') and not ChkJobQueCountMonitor.Checked then
          UninstallServiceIfPresent('IPMonitoring_IBMJobQueCountMonitor');
        if WasServiceInstalled('InstalledJobQueStatusMonitor', 'IPMonitoring_IBMJobQueStatusMonitor') and not ChkJobQueStatusMonitor.Checked then
          UninstallServiceIfPresent('IPMonitoring_IBMJobQueStatusMonitor');
        if WasServiceInstalled('InstalledServerUpTimeMonitor', 'IPMonitoring_ServerUpTimeMonitor') and not ChkServerUpTimeMonitor.Checked then
          UninstallServiceIfPresent('IPMonitoring_ServerUpTimeMonitor');
        if WasServiceInstalled('InstalledSubSystemMonitor', 'IPMonitoring_IBMSubSystemMonitoring') and not ChkSubSystemMonitor.Checked then
          UninstallServiceIfPresent('IPMonitoring_IBMSubSystemMonitoring');
        if WasServiceInstalled('InstalledSystemMatrix', 'IPMonitoring_IBMSystemMatrix') and not ChkSystemMatrix.Checked then
          UninstallServiceIfPresent('IPMonitoring_IBMSystemMatrix');
        if WasServiceInstalled('InstalledUserProfileChecker', 'IPMonitoring_IBMUserProfileChecker') and not ChkUserProfileChecker.Checked then
          UninstallServiceIfPresent('IPMonitoring_IBMUserProfileChecker');
        if WasServiceInstalled('InstalledNetWorkEnabler', 'IPMonitoring_NetWorkEnabler') and not ChkNetWorkEnabler.Checked then
          UninstallServiceIfPresent('IPMonitoring_NetWorkEnabler');
        if WasServiceInstalled('InstalledQSYSOPRMonitor', 'IPMonitoring_QSYSOPRMonitoring') and not ChkQSYSOPRMonitor.Checked then
          UninstallServiceIfPresent('IPMonitoring_QSYSOPRMonitoring');
        // Loki only applies on Monitoring Server role
        if WasServiceInstalled('InstalledLoki', 'IPMonitoring-Loki') and not ChkLoki.Checked then
          UninstallServiceIfPresent('IPMonitoring-Loki');
        // Promtail uninstall only if previously installed and now deselected
        if WasServiceInstalled('InstalledLogAgent', 'IPMonitoring-LogAgent') and not ChkPromtail.Checked then
          UninstallServiceIfPresent('IPMonitoring-LogAgent');
    end
    else if IsWinAgentRole then
    begin
      // Windows File Agent role: only manage Windows monitors
      if not ChkWinFSErrorMonitor.Checked then UninstallServiceIfPresent('IPMonitoring_WinFSErrorMonitor');
      if not ChkWinFSCardinalityMonitor.Checked then UninstallServiceIfPresent('IPMonitoring_WinFSCardinalityMonitor');
      // Do NOT touch IBM services or Loki/Prometheus/Grafana on WinAgent installs
    end
    else if IsLogAgentRole then
    begin
      // Log Agent role: only manage Promtail
      if not ChkPromtail.Checked then UninstallServiceIfPresent('IPMonitoring-LogAgent');
    end;
    
    // ===== Store Client Instance ID in a file for reliable uninstaller detection =====
    // This is critical because the uninstaller cannot run Pascal code to determine the client ID
    if ClientInstanceId <> '' then
      SaveStringToFile(D + '\.client_instance_id', ClientInstanceId, False);
    
    // ===== Register this client instance in registry =====
    if ClientInstanceId <> '' then
    begin
      RegWriteStringValue(HKLM, '{#AppRegKeyClients}\' + ClientInstanceId, 'InstallPath', D);
      RegWriteStringValue(HKLM, '{#AppRegKeyClients}\' + ClientInstanceId, 'InstallRole', InstallRole);  // Store role for future upgrade detection
      RegWriteStringValue(HKLM, '{#AppRegKeyClients}\' + ClientInstanceId, 'IBMiServer', IBMiConfigPage.Values[0]);
      RegWriteStringValue(HKLM, '{#AppRegKeyClients}\' + ClientInstanceId, 'IBMiUser', IBMiConfigPage.Values[1]);
      RegWriteStringValue(HKLM, '{#AppRegKeyClients}\' + ClientInstanceId, 'IBMiPassword', IBMiConfigPage.Values[2]);
      RegWriteStringValue(HKLM, '{#AppRegKeyClients}\' + ClientInstanceId, 'ClientName', GetClientName(''));

      // Core ports and credentials
      RegWriteStringValue(HKLM, '{#AppRegKeyClients}\' + ClientInstanceId, 'PrometheusPort', GetPromPort(''));
      RegWriteStringValue(HKLM, '{#AppRegKeyClients}\' + ClientInstanceId, 'GrafanaPort', GetGrafPort(''));
      RegWriteStringValue(HKLM, '{#AppRegKeyClients}\' + ClientInstanceId, 'GrafanaAdminPassword', GetGrafPwd(''));

      // Monitor ports
      RegWriteStringValue(HKLM, '{#AppRegKeyClients}\' + ClientInstanceId, 'IFSErrorMonitorPort', GetIFSErrorMonitorPort(''));
      RegWriteStringValue(HKLM, '{#AppRegKeyClients}\' + ClientInstanceId, 'RealTimeIFSMonitorPort', GetRealTimeIFSMonitorPort(''));
      RegWriteStringValue(HKLM, '{#AppRegKeyClients}\' + ClientInstanceId, 'JobQueCountMonitorPort', GetJobQueCountMonitorPort(''));
      RegWriteStringValue(HKLM, '{#AppRegKeyClients}\' + ClientInstanceId, 'JobQueStatusMonitorPort', GetJobQueStatusMonitorPort(''));
      RegWriteStringValue(HKLM, '{#AppRegKeyClients}\' + ClientInstanceId, 'ServerUpTimeMonitorPort', GetServerUpTimeMonitorPort(''));
      RegWriteStringValue(HKLM, '{#AppRegKeyClients}\' + ClientInstanceId, 'SubSystemMonitorPort', GetSubSystemMonitorPort(''));
      RegWriteStringValue(HKLM, '{#AppRegKeyClients}\' + ClientInstanceId, 'SystemMatrixPort', GetSystemMatrixPort(''));
      RegWriteStringValue(HKLM, '{#AppRegKeyClients}\' + ClientInstanceId, 'UserProfileCheckerPort', GetUserProfileCheckerPort(''));
      RegWriteStringValue(HKLM, '{#AppRegKeyClients}\' + ClientInstanceId, 'NetWorkEnablerPort', GetNetWorkEnablerPort(''));
      RegWriteStringValue(HKLM, '{#AppRegKeyClients}\' + ClientInstanceId, 'QSYSOPRMonitorPort', GetQSYSOPRMonitorPort(''));
      RegWriteStringValue(HKLM, '{#AppRegKeyClients}\' + ClientInstanceId, 'WinFSErrorMonitorPort', GetWinFSErrorMonitorPort(''));
      RegWriteStringValue(HKLM, '{#AppRegKeyClients}\' + ClientInstanceId, 'WinFSCardinalityMonitorPort', GetWinFSCardinalityMonitorPort(''));
      RegWriteStringValue(HKLM, '{#AppRegKeyClients}\' + ClientInstanceId, 'WinClientName', GetWinClientName(''));

      // Loki and Log Agent (Promtail) configuration
      RegWriteStringValue(HKLM, '{#AppRegKeyClients}\' + ClientInstanceId, 'LokiPort', GetLokiPort(''));
      if LogAgentHostEdit <> nil then
        RegWriteStringValue(HKLM, '{#AppRegKeyClients}\' + ClientInstanceId, 'LokiHost', LogAgentHostEdit.Text);
      if LogAgentPortEdit <> nil then
        RegWriteStringValue(HKLM, '{#AppRegKeyClients}\' + ClientInstanceId, 'LokiPortForLogAgent', LogAgentPortEdit.Text);
      RegWriteStringValue(HKLM, '{#AppRegKeyClients}\' + ClientInstanceId, 'LogAgentLogPath', GetLogAgentLogPath(''));
      RegWriteStringValue(HKLM, '{#AppRegKeyClients}\' + ClientInstanceId, 'LogAgentAppName', GetLogAgentAppName(''));
      RegWriteStringValue(HKLM, '{#AppRegKeyClients}\' + ClientInstanceId, 'LogAgentEnvironment', GetLogAgentEnvironment(''));

      // Email auth + SMTP
      RegWriteStringValue(HKLM, '{#AppRegKeyClients}\' + ClientInstanceId, 'EmailAuthMethod', GetEmailAuthMethod(''));
      RegWriteStringValue(HKLM, '{#AppRegKeyClients}\' + ClientInstanceId, 'SmtpHost', GetSmtpHost(''));
      RegWriteStringValue(HKLM, '{#AppRegKeyClients}\' + ClientInstanceId, 'SmtpPort', GetSmtpPort(''));
      RegWriteStringValue(HKLM, '{#AppRegKeyClients}\' + ClientInstanceId, 'SmtpUsername', GetSmtpUsername(''));
      RegWriteStringValue(HKLM, '{#AppRegKeyClients}\' + ClientInstanceId, 'SmtpPassword', GetSmtpPassword(''));
      RegWriteStringValue(HKLM, '{#AppRegKeyClients}\' + ClientInstanceId, 'SmtpAuth', GetSmtpAuth(''));
      RegWriteStringValue(HKLM, '{#AppRegKeyClients}\' + ClientInstanceId, 'SmtpStartTls', GetSmtpStartTls(''));

      // OAuth
      RegWriteStringValue(HKLM, '{#AppRegKeyClients}\' + ClientInstanceId, 'OAuthTenant', GetOAuthTenant(''));
      RegWriteStringValue(HKLM, '{#AppRegKeyClients}\' + ClientInstanceId, 'OAuthClientId', GetOAuthClientId(''));
      RegWriteStringValue(HKLM, '{#AppRegKeyClients}\' + ClientInstanceId, 'OAuthClientSecret', GetOAuthClientSecret(''));
      RegWriteStringValue(HKLM, '{#AppRegKeyClients}\' + ClientInstanceId, 'OAuthTokenUrl', GetOAuthTokenUrl(''));
      RegWriteStringValue(HKLM, '{#AppRegKeyClients}\' + ClientInstanceId, 'OAuthMailUrl', GetOAuthMailUrl(''));
      RegWriteStringValue(HKLM, '{#AppRegKeyClients}\' + ClientInstanceId, 'OAuthScope', GetOAuthScope(''));
      RegWriteStringValue(HKLM, '{#AppRegKeyClients}\' + ClientInstanceId, 'OAuthFromUser', GetOAuthFromUser(''));

      // Email recipients/sender
      RegWriteStringValue(HKLM, '{#AppRegKeyClients}\' + ClientInstanceId, 'EmailFrom', GetEmailFrom(''));
      RegWriteStringValue(HKLM, '{#AppRegKeyClients}\' + ClientInstanceId, 'EmailTo', GetEmailTo(''));
      RegWriteStringValue(HKLM, '{#AppRegKeyClients}\' + ClientInstanceId, 'EmailBcc', GetEmailBcc(''));
      RegWriteStringValue(HKLM, '{#AppRegKeyClients}\' + ClientInstanceId, 'ClientNameEmail', GetClientNameEmail(''));
      RegWriteStringValue(HKLM, '{#AppRegKeyClients}\' + ClientInstanceId, 'EmailImportance', GetEmailImportance(''));
    end;
    
    // ===== Install Prometheus if selected and not already installed =====
    if ChkPrometheus.Checked and not PrometheusInstalled then
    begin
      // Use generic core service ID and name (shared across clients)
      GenerateServiceXml('IPMonitoringPrometheus', 'IPMonitoringPrometheus', 
        'IP Monitoring - Prometheus', 'Prometheus metrics collection',
        D + '\prometheus\prometheus.exe',
        '--config.file="' + D + '\prometheus\prometheus.yml" --storage.tsdb.path="' + D + '\data\prometheus" --web.listen-address=:' + GetPromPort(''),
        D + '\prometheus');
      UpdatePrometheusYaml(GetPromPort(''));
      InstallAndStartService('IPMonitoringPrometheus.exe');
    end;

    // ===== Install Grafana if selected and not already installed =====
    if ChkGrafana.Checked and not GrafanaInstalled then
    begin
      // Use generic core service ID and name (shared across clients)
      GenerateServiceXml('IPMonitoringGrafana', 'IPMonitoringGrafana',
        'IP Monitoring - Grafana', 'Grafana dashboards',
        D + '\grafana\bin\grafana-server.exe',
        '--homepath "' + D + '\grafana" --config "' + D + '\grafana\conf\custom.ini"',
        D + '\grafana');
      GenerateGrafanaIni(GetGrafPort(''), GetGrafPwd(''));
      UpdateGrafanaDatasource(GetPromPort(''));
      // Add Loki datasource if Monitoring Server role (Loki will be installed)
      if IsMonitoringServerRole then
        UpdateGrafanaLokiDatasource(GetLokiPort(''));
      UpdateGrafanaDashboardProvider;
      InstallAndStartService('IPMonitoringGrafana.exe');
    end;

    // ===== Install Loki if Monitoring Server role and Loki selected and not already installed =====
    if IsMonitoringServerRole and ChkLoki.Checked and not LokiInstalled then
    begin
      GenerateLokiYaml;
      GenerateLokiServiceXml;
      // Ensure Loki datasource exists in Grafana
      UpdateGrafanaLokiDatasource(GetLokiPort(''));
      InstallAndStartService('IPMonitoring-Loki.exe');
      Log('Loki log server installed and started');
    end;

    // ===== Install Promtail/Log Agent if Promtail checkbox selected OR Log Agent role and not already installed =====
    if (ChkPromtail.Checked or IsLogAgentRole) and not PromtailInstalled then
    begin
      GeneratePromtailYaml;
      GenerateLogAgentServiceXml;
      InstallAndStartService('IPMonitoring-LogAgent.exe');
      Log('Promtail log agent installed and started');
    end;

    // ===== Install IBM IFS Error Monitor if selected =====
    if ChkIFSErrorMonitor.Checked then
      InstallJavaMonitor('IBMIFSErrorMonitor', 'IPMonitoring_IBMIFSErrorMonitor',
        'IP Monitoring - IFS Error Monitor', 'IBM IFS Error Log Monitor',
        'IBMIFSErrorMonitor.jar', 'monitor.properties',
        'ibm-ifs-error-monitor', GetIFSErrorMonitorPort(''), True);

    // ===== Install IBM Real-Time IFS Monitor if selected =====
    if ChkRealTimeIFSMonitor.Checked then
      InstallJavaMonitor('IBMRealTimeIFSMonitor', 'IPMonitoring_IBMRealTimeIFSMonitor',
        'IP Monitoring - Real-Time IFS Monitor', 'IBM Real-Time IFS File Monitor',
        'IBMRealTimeIFSMonitor.jar', 'ifsmonitor.properties',
        'ibm-realtime-ifs-monitor', GetRealTimeIFSMonitorPort(''), True);

    // ===== Install IBM Job Queue Count Monitor if selected =====
    if ChkJobQueCountMonitor.Checked then
      InstallJavaMonitor('IBMJobQueCountMonitor', 'IPMonitoring_IBMJobQueCountMonitor',
        'IP Monitoring - Job Queue Count Monitor', 'IBM Job Queue Count Monitor',
        'IBMJobQueCountMonitor.jar', 'jobqueuemonitor.properties',
        'ibm-jobqueue-count-monitor', GetJobQueCountMonitorPort(''), True);

    // ===== Install IBM Job Queue Status Monitor if selected =====
    if ChkJobQueStatusMonitor.Checked then
      InstallJavaMonitor('IBMJobQueStatusMonitor', 'IPMonitoring_IBMJobQueStatusMonitor',
        'IP Monitoring - Job Queue Status Monitor', 'IBM Job Queue Status Monitor',
        'IBMJobQueStatusMonitor.jar', 'joblist.properties',
        'ibm-jobqueue-status-monitor', GetJobQueStatusMonitorPort(''), True);

    // ===== Install Server UpTime Monitor if selected =====
    if ChkServerUpTimeMonitor.Checked then
      InstallJavaMonitor('ServerUpTimeMonitor', 'IPMonitoring_ServerUpTimeMonitor',
        'IP Monitoring - Server UpTime Monitor', 'Server UpTime Monitor',
        'ServerUpTimeMonitor.jar', 'serverinfo.properties',
        'server-uptime-monitor', GetServerUpTimeMonitorPort(''), False);
    
    // ===== Install IBM SubSystem Monitor if selected =====
    if ChkSubSystemMonitor.Checked then
      InstallJavaMonitor('IBMSubSystemMonitoring', 'IPMonitoring_IBMSubSystemMonitoring',
        'IP Monitoring - IBM SubSystem Monitor', 'IBM SubSystem Monitor',
        'IBMSubSystemMonitoring.jar', 'subsystem.properties',
        'ibm-subsystem-monitor', GetSubSystemMonitorPort(''), True);
    
    // ===== Install IBM System Matrix Monitor if selected =====
    if ChkSystemMatrix.Checked then
      InstallJavaMonitor('IBMSystemMatrix', 'IPMonitoring_IBMSystemMatrix',
        'IP Monitoring - IBM System Matrix Monitor', 'IBM System Matrix Monitor',
        'IBMSystemMatrix.jar', 'systemmonitor.properties',
        'ibm-system-matrix', GetSystemMatrixPort(''), True);
    
    // ===== Install IBM User Profile Checker if selected =====
    if ChkUserProfileChecker.Checked then
      InstallJavaMonitor('IBMUserProfileChecker', 'IPMonitoring_IBMUserProfileChecker',
        'IP Monitoring - IBM User Profile Checker', 'IBM User Profile Checker',
        'IBMUserProfileChecker.jar', 'userprofilecheck.properties',
        'ibm-user-profile-checker', GetUserProfileCheckerPort(''), True);
    
    // ===== Install Network Enabler Monitor if selected =====
    if ChkNetWorkEnabler.Checked then
      InstallJavaMonitor('NetWorkEnabler', 'IPMonitoring_NetWorkEnabler',
        'IP Monitoring - Network Enabler Monitor', 'Network Enabler Monitor',
        'NetWorkEnabler.jar', 'networkenable.properties',
        'network-enabler', GetNetWorkEnablerPort(''), False);
    
    // ===== Install QSYSOPR Monitor if selected =====
    if ChkQSYSOPRMonitor.Checked then
      InstallJavaMonitor('QSYSOPRMonitoring', 'IPMonitoring_QSYSOPRMonitoring',
        'IP Monitoring - QSYSOPR Monitor', 'QSYSOPR Message Queue Monitor',
        'QSYSOPRMonitoring.jar', 'job_failure.properties',
        'qsysopr-monitor', GetQSYSOPRMonitorPort(''), True);
    
    // ===== Install Windows FS Error Monitor if selected =====
    if ChkWinFSErrorMonitor.Checked then
    begin
      InstallJavaMonitor('WinFSErrorMonitor', 'IPMonitoring_WinFSErrorMonitor',
        'IP Monitoring - Windows FS Error Monitor', 'Windows File System Error Monitor',
        'WinFSErrorMonitor.jar', 'fserrormonitor.properties',
        'win-fs-error-monitor', GetWinFSErrorMonitorPort(''), False);
      // Update client.name for Windows monitor
      UpdateWinClientName(D + '\monitoring-services\WinFSErrorMonitor\fserrormonitor.properties', GetWinClientName(''));
    end;
    
    // ===== Install Windows FS Cardinality Monitor if selected =====
    if ChkWinFSCardinalityMonitor.Checked then
    begin
      InstallJavaMonitor('WinFSCardinalityMonitor', 'IPMonitoring_WinFSCardinalityMonitor',
        'IP Monitoring - Windows FS Cardinality Monitor', 'Windows File System Cardinality Monitor',
        'WinFSCardinalityMonitor.jar', 'fscardinalitymonitor.properties',
        'win-fs-cardinality-monitor', GetWinFSCardinalityMonitorPort(''), False);
      // Update client.name for Windows monitor
      UpdateWinClientName(D + '\monitoring-services\WinFSCardinalityMonitor\fscardinalitymonitor.properties', GetWinClientName(''));
    end;
    
    // ===== Restart Prometheus to pick up new targets =====
    // Only restart Prometheus if it's installed and we added monitors to a Monitoring Server role
    // Use client-suffixed service name to restart the correct instance
    if IsMonitoringServerRole and ChkPrometheus.Checked and 
       (ChkIFSErrorMonitor.Checked or ChkRealTimeIFSMonitor.Checked or 
        ChkJobQueCountMonitor.Checked or ChkJobQueStatusMonitor.Checked or 
        ChkServerUpTimeMonitor.Checked or ChkSubSystemMonitor.Checked or
        ChkSystemMatrix.Checked or ChkUserProfileChecker.Checked or
        ChkNetWorkEnabler.Checked or ChkQSYSOPRMonitor.Checked or
        ChkWinFSErrorMonitor.Checked or ChkWinFSCardinalityMonitor.Checked) then
    begin
      // For multi-client installs, restart Prometheus from existing installation
      if not IsFirstInstallation and (ExistingInstallPath <> '') then
        RestartServiceAtPath(ExistingInstallPath + '\services\IPMonitoringPrometheus.exe')
      else
        RestartService('IPMonitoringPrometheus.exe');
    end;
  end;
end;

// ===== UNINSTALL: Stop and remove client-suffixed services =====
// [UninstallRun] only handles base service names; this procedure handles
// the client-suffixed services created during multi-client installations
procedure CurUninstallStepChanged(CurUninstallStep: TUninstallStep);
var
  ResultCode: Integer;
  ServicesDir, SavedClientId, AppPath: string;
  ServiceBasenames: array of string;
  i: Integer;
  FindRec: TFindRec;
  ServiceExePath: string;
  ClientIdContent: AnsiString;
begin
  if CurUninstallStep = usUninstall then
  begin
    AppPath := ExpandConstant('{app}');
    ServicesDir := AppPath + '\services';
    
    // Try to get ClientInstanceId from multiple sources (most reliable first)
    SavedClientId := '';
    
    // MOST RELIABLE: Read from the identity file we created during installation
    if FileExists(AppPath + '\.client_instance_id') then
    begin
      if LoadStringFromFile(AppPath + '\.client_instance_id', ClientIdContent) then
        SavedClientId := Trim(String(ClientIdContent))
      else
        SavedClientId := '';
    end;
    
    // Second attempt: Try to get from base registry key
    if SavedClientId = '' then
      RegQueryStringValue(HKLM, 'Software\IslandPacific\Monitoring', 'ClientInstanceId', SavedClientId);
    
    // Third attempt: Scan client registry subkeys to find one matching our install path
    // Note: Inno Setup's FindFirst doesn't work on registry, so we check known clients
    if (SavedClientId = '') and DirExists(ServicesDir) then
    begin
      // This is handled by the file scan below
    end;
    
    // Fallback: Extract client ID from service executable names in the services folder
    // Try multiple service patterns to find any client-suffixed service
    if (SavedClientId = '') and DirExists(ServicesDir) then
    begin
      // Try common services in order of likelihood
      if FindFirst(ServicesDir + '\IPMonitoringPrometheus_*.exe', FindRec) then
      begin
        try
          SavedClientId := Copy(FindRec.Name, Length('IPMonitoringPrometheus_') + 1, 
                                Length(FindRec.Name) - Length('IPMonitoringPrometheus_') - 4);
        finally
          FindClose(FindRec);
        end;
      end;
    end;
    
    if (SavedClientId = '') and DirExists(ServicesDir) then
    begin
      if FindFirst(ServicesDir + '\IPMonitoring_IBMIFSErrorMonitor_*.exe', FindRec) then
      begin
        try
          SavedClientId := Copy(FindRec.Name, Length('IPMonitoring_IBMIFSErrorMonitor_') + 1, 
                                Length(FindRec.Name) - Length('IPMonitoring_IBMIFSErrorMonitor_') - 4);
        finally
          FindClose(FindRec);
        end;
      end;
    end;
    
    if (SavedClientId = '') and DirExists(ServicesDir) then
    begin
      if FindFirst(ServicesDir + '\IPMonitoring_WinFSErrorMonitor_*.exe', FindRec) then
      begin
        try
          SavedClientId := Copy(FindRec.Name, Length('IPMonitoring_WinFSErrorMonitor_') + 1, 
                                Length(FindRec.Name) - Length('IPMonitoring_WinFSErrorMonitor_') - 4);
        finally
          FindClose(FindRec);
        end;
      end;
    end;
    
    // If still no client ID, try other service patterns
    if (SavedClientId = '') and DirExists(ServicesDir) then
    begin
      if FindFirst(ServicesDir + '\IPMonitoring_ServerUpTimeMonitor_*.exe', FindRec) then
      begin
        try
          SavedClientId := Copy(FindRec.Name, Length('IPMonitoring_ServerUpTimeMonitor_') + 1, 
                                Length(FindRec.Name) - Length('IPMonitoring_ServerUpTimeMonitor_') - 4);
        finally
          FindClose(FindRec);
        end;
      end;
    end;
    
    // Try Windows FS monitors (for WinAgent-only installations)
    if (SavedClientId = '') and DirExists(ServicesDir) then
    begin
      if FindFirst(ServicesDir + '\IPMonitoring_WinFSErrorMonitor_*.exe', FindRec) then
      begin
        try
          SavedClientId := Copy(FindRec.Name, Length('IPMonitoring_WinFSErrorMonitor_') + 1, 
                                Length(FindRec.Name) - Length('IPMonitoring_WinFSErrorMonitor_') - 4);
        finally
          FindClose(FindRec);
        end;
      end;
    end;
    
    // Define all service base names (without client suffix)
    SetArrayLength(ServiceBasenames, 12);
    ServiceBasenames[0] := 'IPMonitoring_IBMIFSErrorMonitor';
    ServiceBasenames[1] := 'IPMonitoring_IBMRealTimeIFSMonitor';
    ServiceBasenames[2] := 'IPMonitoring_IBMJobQueCountMonitor';
    ServiceBasenames[3] := 'IPMonitoring_IBMJobQueStatusMonitor';
    ServiceBasenames[4] := 'IPMonitoring_ServerUpTimeMonitor';
    ServiceBasenames[5] := 'IPMonitoring_IBMSubSystemMonitoring';
    ServiceBasenames[6] := 'IPMonitoring_IBMSystemMatrix';
    ServiceBasenames[7] := 'IPMonitoring_IBMUserProfileChecker';
    ServiceBasenames[8] := 'IPMonitoring_NetWorkEnabler';
    ServiceBasenames[9] := 'IPMonitoring_QSYSOPRMonitoring';
    ServiceBasenames[10] := 'IPMonitoring_WinFSErrorMonitor';
    ServiceBasenames[11] := 'IPMonitoring_WinFSCardinalityMonitor';
    
    // Stop and uninstall client-suffixed services if we found a client ID
    if SavedClientId <> '' then
    begin
      for i := 0 to GetArrayLength(ServiceBasenames) - 1 do
      begin
        ServiceExePath := ServicesDir + '\' + ServiceBasenames[i] + '_' + SavedClientId + '.exe';
        if FileExists(ServiceExePath) then
        begin
          Exec(ServiceExePath, 'stop', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
          Sleep(300);
          Exec(ServiceExePath, 'uninstall', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
          Sleep(300);
        end;
        // Also use sc.exe as fallback
        Exec('sc.exe', 'stop "' + ServiceBasenames[i] + '_' + SavedClientId + '"', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
        Exec('sc.exe', 'delete "' + ServiceBasenames[i] + '_' + SavedClientId + '"', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
      end;
      
      // Remove client from registry
      RegDeleteKeyIncludingSubkeys(HKLM, 'Software\IslandPacific\Monitoring\Clients\' + SavedClientId);
    end;
    
    // Also try to stop/uninstall any services matching the pattern (brute force for any leftover)
    // This handles cases where we couldn't determine the exact client ID
    for i := 0 to GetArrayLength(ServiceBasenames) - 1 do
    begin
      if FindFirst(ServicesDir + '\' + ServiceBasenames[i] + '_*.exe', FindRec) then
      begin
        try
          repeat
            ServiceExePath := ServicesDir + '\' + FindRec.Name;
            Exec(ServiceExePath, 'stop', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
            Sleep(200);
            Exec(ServiceExePath, 'uninstall', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
            Sleep(200);
            // Extract service name from exe filename and use sc.exe
            Exec('sc.exe', 'stop "' + Copy(FindRec.Name, 1, Length(FindRec.Name) - 4) + '"', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
            Exec('sc.exe', 'delete "' + Copy(FindRec.Name, 1, Length(FindRec.Name) - 4) + '"', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
          until not FindNext(FindRec);
        finally
          FindClose(FindRec);
        end;
      end;
    end;
    
    // Stop and uninstall core services (with client-specific IDs)
    // Note: SavedClientId was loaded from .client_instance_id file in InitializeUninstall
    
    // Prometheus
    if FileExists(ServicesDir + '\IPMonitoringPrometheus.exe') then
    begin
      Exec(ServicesDir + '\IPMonitoringPrometheus.exe', 'stop', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
      Sleep(500);
      Exec(ServicesDir + '\IPMonitoringPrometheus.exe', 'uninstall', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
    end;
    // Stop/delete with client-specific service ID
    if SavedClientId <> '' then
    begin
      Exec('sc.exe', 'stop "IPMonitoringPrometheus_' + SavedClientId + '"', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
      Exec('sc.exe', 'delete "IPMonitoringPrometheus_' + SavedClientId + '"', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
    end;
    // Also try legacy non-suffixed name
    Exec('sc.exe', 'stop "IPMonitoringPrometheus"', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
    Exec('sc.exe', 'delete "IPMonitoringPrometheus"', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
    
    // Grafana
    if FileExists(ServicesDir + '\IPMonitoringGrafana.exe') then
    begin
      Exec(ServicesDir + '\IPMonitoringGrafana.exe', 'stop', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
      Sleep(500);
      Exec(ServicesDir + '\IPMonitoringGrafana.exe', 'uninstall', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
    end;
    if SavedClientId <> '' then
    begin
      Exec('sc.exe', 'stop "IPMonitoringGrafana_' + SavedClientId + '"', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
      Exec('sc.exe', 'delete "IPMonitoringGrafana_' + SavedClientId + '"', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
    end;
    Exec('sc.exe', 'stop "IPMonitoringGrafana"', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
    Exec('sc.exe', 'delete "IPMonitoringGrafana"', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
    
    // Loki service
    if FileExists(ServicesDir + '\IPMonitoring-Loki.exe') then
    begin
      Exec(ServicesDir + '\IPMonitoring-Loki.exe', 'stop', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
      Sleep(500);
      Exec(ServicesDir + '\IPMonitoring-Loki.exe', 'uninstall', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
    end;
    if SavedClientId <> '' then
    begin
      Exec('sc.exe', 'stop "IPMonitoring-Loki_' + SavedClientId + '"', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
      Exec('sc.exe', 'delete "IPMonitoring-Loki_' + SavedClientId + '"', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
    end;
    Exec('sc.exe', 'stop "IPMonitoring-Loki"', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
    Exec('sc.exe', 'delete "IPMonitoring-Loki"', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
    
    // Log Agent (Promtail) service
    if FileExists(ServicesDir + '\IPMonitoring-LogAgent.exe') then
    begin
      Exec(ServicesDir + '\IPMonitoring-LogAgent.exe', 'stop', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
      Sleep(500);
      Exec(ServicesDir + '\IPMonitoring-LogAgent.exe', 'uninstall', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
    end;
    if SavedClientId <> '' then
    begin
      Exec('sc.exe', 'stop "IPMonitoring-LogAgent_' + SavedClientId + '"', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
      Exec('sc.exe', 'delete "IPMonitoring-LogAgent_' + SavedClientId + '"', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
    end;
    Exec('sc.exe', 'stop "IPMonitoring-LogAgent"', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
    Exec('sc.exe', 'delete "IPMonitoring-LogAgent"', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
    
    // Give services time to fully stop before file deletion
    Sleep(2000);
  end;
end;
