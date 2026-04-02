#define AppName "Island Pacific Operation Monitor Suite"
#define AppVersion "1.0.0"
#define AppPublisher "Island Pacific Retail Systems"
#define AppRegKey "Software\IslandPacific\Monitoring\{code:GetClientInstanceId}"
#define AppRegKeyClients "Software\IslandPacific\Monitoring\Clients"

[Setup]
; AppId is now dynamic - set in InitializeSetup based on ClientId
; This allows multiple client instances on the same server
AppId={{12345678-1234-1234-1234-123456789ABC}-{code:GetClientInstanceId}}
; Required when AppId includes constants
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
UninstallDisplayIcon={app}\services\IPMonitoringGrafana.exe

ArchitecturesInstallIn64BitMode=x64compatible
DefaultDirName={commonpf}\{#AppName}
; DisableDirPage removed to allow multi-client installations in different folders
DisableDirPage=no
UsePreviousAppDir=yes
DisableProgramGroupPage=yes
PrivilegesRequired=admin
WizardStyle=modern
WizardSizePercent=125
WizardImageFile=installer\resources\wizard_resized.bmp
WizardSmallImageFile=installer\resources\wizard_small_resized.bmp
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
FinishedLabelNoIcons=The Island Pacific Monitoring Suite is now installed and running.%n%n✓ Services are active and collecting data%n✓ Dashboards are ready for viewing
ClickFinish=Your monitoring environment is ready. Click Finish to start using it!

[Dirs]
Name: "{app}\services"
Name: "{app}\logs"; Flags: uninsneveruninstall
Name: "{app}\data"; Flags: uninsneveruninstall
Name: "{app}\data\prometheus"; Flags: uninsneveruninstall
Name: "{app}\grafana\conf"
Name: "{app}\grafana\data"; Flags: uninsneveruninstall
Name: "{app}\monitoring-services"

[Files]
; Core services - Prometheus & Grafana
Source: "installer\resources\WinSW.exe"; DestDir: "{app}\services"; DestName: "IPMonitoringPrometheus.exe"; Flags: ignoreversion
Source: "installer\resources\WinSW.exe"; DestDir: "{app}\services"; DestName: "IPMonitoringGrafana.exe"; Flags: ignoreversion
; Prometheus - exclude data directory to preserve metrics during upgrade
Source: "installer\resources\prometheus\*"; DestDir: "{app}\prometheus"; Excludes: "data\*"; Flags: recursesubdirs createallsubdirs ignoreversion
; Grafana - exclude data directory to preserve dashboards/users during upgrade
Source: "installer\resources\grafana\*"; DestDir: "{app}\grafana"; Excludes: "data\*"; Flags: recursesubdirs createallsubdirs ignoreversion

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

; Monitoring Services - JAR files only (always updated during upgrade)
; Properties files and logs are handled separately to preserve user data
Source: "installer\resources\monitoring-services\IBMIFSErrorMonitor\*.jar"; DestDir: "{app}\monitoring-services\IBMIFSErrorMonitor"; Flags: ignoreversion
Source: "installer\resources\monitoring-services\IBMRealTimeIFSMonitor\*.jar"; DestDir: "{app}\monitoring-services\IBMRealTimeIFSMonitor"; Flags: ignoreversion
Source: "installer\resources\monitoring-services\IBMJobQueCountMonitor\*.jar"; DestDir: "{app}\monitoring-services\IBMJobQueCountMonitor"; Flags: ignoreversion
Source: "installer\resources\monitoring-services\IBMJobQueStatusMonitor\*.jar"; DestDir: "{app}\monitoring-services\IBMJobQueStatusMonitor"; Flags: ignoreversion
Source: "installer\resources\monitoring-services\ServerUpTimeMonitor\*.jar"; DestDir: "{app}\monitoring-services\ServerUpTimeMonitor"; Flags: ignoreversion
Source: "installer\resources\monitoring-services\IBMSubSystemMonitoring\*.jar"; DestDir: "{app}\monitoring-services\IBMSubSystemMonitoring"; Flags: ignoreversion
Source: "installer\resources\monitoring-services\IBMSystemMatrix\*.jar"; DestDir: "{app}\monitoring-services\IBMSystemMatrix"; Flags: ignoreversion
Source: "installer\resources\monitoring-services\IBMUserProfileChecker\*.jar"; DestDir: "{app}\monitoring-services\IBMUserProfileChecker"; Flags: ignoreversion
Source: "installer\resources\monitoring-services\NetWorkEnabler\*.jar"; DestDir: "{app}\monitoring-services\NetWorkEnabler"; Flags: ignoreversion
Source: "installer\resources\monitoring-services\QSYSOPRMonitoring\*.jar"; DestDir: "{app}\monitoring-services\QSYSOPRMonitoring"; Flags: ignoreversion

; Properties files - only copy if they don't exist (preserve user customizations on upgrade)
; email.properties is excluded because it is dynamically generated
Source: "installer\resources\monitoring-services\IBMIFSErrorMonitor\*.properties"; DestDir: "{app}\monitoring-services\IBMIFSErrorMonitor"; Excludes: "email.properties"; Flags: onlyifdoesntexist
Source: "installer\resources\monitoring-services\IBMRealTimeIFSMonitor\*.properties"; DestDir: "{app}\monitoring-services\IBMRealTimeIFSMonitor"; Excludes: "email.properties"; Flags: onlyifdoesntexist
Source: "installer\resources\monitoring-services\IBMJobQueCountMonitor\*.properties"; DestDir: "{app}\monitoring-services\IBMJobQueCountMonitor"; Excludes: "email.properties"; Flags: onlyifdoesntexist
Source: "installer\resources\monitoring-services\IBMJobQueStatusMonitor\*.properties"; DestDir: "{app}\monitoring-services\IBMJobQueStatusMonitor"; Excludes: "email.properties"; Flags: onlyifdoesntexist
Source: "installer\resources\monitoring-services\ServerUpTimeMonitor\*.properties"; DestDir: "{app}\monitoring-services\ServerUpTimeMonitor"; Excludes: "email.properties"; Flags: onlyifdoesntexist
Source: "installer\resources\monitoring-services\IBMSubSystemMonitoring\*.properties"; DestDir: "{app}\monitoring-services\IBMSubSystemMonitoring"; Excludes: "email.properties"; Flags: onlyifdoesntexist
Source: "installer\resources\monitoring-services\IBMSystemMatrix\*.properties"; DestDir: "{app}\monitoring-services\IBMSystemMatrix"; Excludes: "email.properties"; Flags: onlyifdoesntexist
Source: "installer\resources\monitoring-services\IBMUserProfileChecker\*.properties"; DestDir: "{app}\monitoring-services\IBMUserProfileChecker"; Excludes: "email.properties"; Flags: onlyifdoesntexist
Source: "installer\resources\monitoring-services\NetWorkEnabler\*.properties"; DestDir: "{app}\monitoring-services\NetWorkEnabler"; Excludes: "email.properties"; Flags: onlyifdoesntexist
Source: "installer\resources\monitoring-services\QSYSOPRMonitoring\*.properties"; DestDir: "{app}\monitoring-services\QSYSOPRMonitoring"; Excludes: "email.properties"; Flags: onlyifdoesntexist

[Registry]
; Core settings
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "InstallPath"; ValueData: "{app}"; Flags: uninsdeletekey
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
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "SubSystemMonitorPort"; ValueData: "{code:GetSubSystemMonitorPort}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "SystemMatrixPort"; ValueData: "{code:GetSystemMatrixPort}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "UserProfileCheckerPort"; ValueData: "{code:GetUserProfileCheckerPort}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "NetWorkEnablerPort"; ValueData: "{code:GetNetWorkEnablerPort}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "QSYSOPRMonitorPort"; ValueData: "{code:GetQSYSOPRMonitorPort}"; Flags: uninsdeletevalue

[Run]
Filename: "http://localhost:{code:GetGrafPort}"; Description: "Open Grafana Dashboard in browser"; Flags: postinstall shellexec nowait unchecked skipifsilent
Filename: "http://localhost:{code:GetPromPort}"; Description: "Open Prometheus Web UI in browser"; Flags: postinstall shellexec nowait unchecked skipifsilent

[UninstallRun]
Filename: "{app}\services\IPMonitoringGrafana.exe"; Parameters: "stop"; Flags: runhidden; RunOnceId: "StopGrafana"
Filename: "{app}\services\IPMonitoringGrafana.exe"; Parameters: "uninstall"; Flags: runhidden; RunOnceId: "UninstallGrafana"
Filename: "{app}\services\IPMonitoringPrometheus.exe"; Parameters: "stop"; Flags: runhidden; RunOnceId: "StopPrometheus"
Filename: "{app}\services\IPMonitoringPrometheus.exe"; Parameters: "uninstall"; Flags: runhidden; RunOnceId: "UninstallPrometheus"
Filename: "{app}\services\IPMonitoring_IBMIFSErrorMonitor.exe"; Parameters: "stop"; Flags: runhidden; RunOnceId: "StopIFSError"
Filename: "{app}\services\IPMonitoring_IBMIFSErrorMonitor.exe"; Parameters: "uninstall"; Flags: runhidden; RunOnceId: "UninstallIFSError"
Filename: "{app}\services\IPMonitoring_IBMRealTimeIFSMonitor.exe"; Parameters: "stop"; Flags: runhidden; RunOnceId: "StopRealTimeIFS"
Filename: "{app}\services\IPMonitoring_IBMRealTimeIFSMonitor.exe"; Parameters: "uninstall"; Flags: runhidden; RunOnceId: "UninstallRealTimeIFS"
Filename: "{app}\services\IPMonitoring_IBMJobQueCountMonitor.exe"; Parameters: "stop"; Flags: runhidden; RunOnceId: "StopJobQueCount"
Filename: "{app}\services\IPMonitoring_IBMJobQueCountMonitor.exe"; Parameters: "uninstall"; Flags: runhidden; RunOnceId: "UninstallJobQueCount"
Filename: "{app}\services\IPMonitoring_IBMJobQueStatusMonitor.exe"; Parameters: "stop"; Flags: runhidden; RunOnceId: "StopJobQueStatus"
Filename: "{app}\services\IPMonitoring_IBMJobQueStatusMonitor.exe"; Parameters: "uninstall"; Flags: runhidden; RunOnceId: "UninstallJobQueStatus"
Filename: "{app}\services\IPMonitoring_ServerUpTimeMonitor.exe"; Parameters: "stop"; Flags: runhidden; RunOnceId: "StopServerUpTime"
Filename: "{app}\services\IPMonitoring_ServerUpTimeMonitor.exe"; Parameters: "uninstall"; Flags: runhidden; RunOnceId: "UninstallServerUpTime"
Filename: "{app}\services\IPMonitoring_IBMSubSystemMonitoring.exe"; Parameters: "stop"; Flags: runhidden; RunOnceId: "StopSubSystem"
Filename: "{app}\services\IPMonitoring_IBMSubSystemMonitoring.exe"; Parameters: "uninstall"; Flags: runhidden; RunOnceId: "UninstallSubSystem"
Filename: "{app}\services\IPMonitoring_IBMSystemMatrix.exe"; Parameters: "stop"; Flags: runhidden; RunOnceId: "StopSystemMatrix"
Filename: "{app}\services\IPMonitoring_IBMSystemMatrix.exe"; Parameters: "uninstall"; Flags: runhidden; RunOnceId: "UninstallSystemMatrix"
Filename: "{app}\services\IPMonitoring_IBMUserProfileChecker.exe"; Parameters: "stop"; Flags: runhidden; RunOnceId: "StopUserProfile"
Filename: "{app}\services\IPMonitoring_IBMUserProfileChecker.exe"; Parameters: "uninstall"; Flags: runhidden; RunOnceId: "UninstallUserProfile"
Filename: "{app}\services\IPMonitoring_NetWorkEnabler.exe"; Parameters: "stop"; Flags: runhidden; RunOnceId: "StopNetWork"
Filename: "{app}\services\IPMonitoring_NetWorkEnabler.exe"; Parameters: "uninstall"; Flags: runhidden; RunOnceId: "UninstallNetWork"
Filename: "{app}\services\IPMonitoring_QSYSOPRMonitoring.exe"; Parameters: "stop"; Flags: runhidden; RunOnceId: "StopQSYSOPR"
Filename: "{app}\services\IPMonitoring_QSYSOPRMonitoring.exe"; Parameters: "uninstall"; Flags: runhidden; RunOnceId: "UninstallQSYSOPR"

[UninstallDelete]
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
Type: dirifempty; Name: "{app}\monitoring-services"
; Remove prometheus and grafana config folders
Type: dirifempty; Name: "{app}\prometheus"
Type: dirifempty; Name: "{app}\grafana\conf\provisioning\datasources"
Type: dirifempty; Name: "{app}\grafana\conf\provisioning\dashboards"
Type: dirifempty; Name: "{app}\grafana\conf\provisioning"
Type: dirifempty; Name: "{app}\grafana\conf"
Type: dirifempty; Name: "{app}\grafana"
; Remove main app folder if empty
Type: dirifempty; Name: "{app}"

[Code]
var
  ConfigPage: TInputQueryWizardPage;
  ServiceSelectPage: TWizardPage;
  ServicePortsPage: TWizardPage;
  IBMiConfigPage: TInputQueryWizardPage;
  
  // Service selection checkboxes (custom page for enable/disable control)
  ChkPrometheus, ChkGrafana: TNewCheckBox;
  ChkIFSErrorMonitor, ChkRealTimeIFSMonitor: TNewCheckBox;
  ChkJobQueCountMonitor, ChkJobQueStatusMonitor, ChkServerUpTimeMonitor: TNewCheckBox;
  ChkSubSystemMonitor, ChkSystemMatrix, ChkUserProfileChecker: TNewCheckBox;
  ChkNetWorkEnabler, ChkQSYSOPRMonitor: TNewCheckBox;
  
  // Service ports custom page controls
  IFSErrorMonitorPortEdit, RealTimeIFSMonitorPortEdit: TNewEdit;
  JobQueCountMonitorPortEdit, JobQueStatusMonitorPortEdit: TNewEdit;
  ServerUpTimeMonitorPortEdit: TNewEdit;
  SubSystemMonitorPortEdit, SystemMatrixPortEdit, UserProfileCheckerPortEdit: TNewEdit;
  NetWorkEnablerPortEdit, QSYSOPRMonitorPortEdit: TNewEdit;
  
  // Email configuration pages - split into multiple clean pages
  EmailAuthMethodPage: TInputOptionWizardPage;
  SmtpConfigPage: TWizardPage;
  OAuthConfigPage1: TWizardPage;
  OAuthConfigPage2: TWizardPage;
  EmailCommonPage: TWizardPage;
  
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
  PrometheusInstalled, GrafanaInstalled: Boolean;
  
  // Multi-client installation support
  ClientInstancePage: TInputQueryWizardPage;
  ClientInstanceId: string;
  IsFirstInstallation: Boolean;
  UseExistingGrafana: Boolean;
  ExistingGrafanaPort: string;
  ExistingPrometheusPort: string;
  ExistingInstallPath: string;
  ClientPortOffset: Integer;

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
var
  ServiceName: string;
begin
  ServiceName := BaseServiceId;
  // Check client-suffixed service first when applicable
  if (ClientInstanceId <> '') and RegKeyExists(HKLM, SERVICES_REG_PATH + BaseServiceId + '_' + ClientInstanceId) then
  begin
    Result := True;
    Exit;
  end;

  Result := RegKeyExists(HKLM, SERVICES_REG_PATH + ServiceName);
end;

function WasServiceInstalled(ValueName, BaseServiceId: string): Boolean;
begin
  if GetClientRegistryValue(ValueName, '') = 'true' then
    Result := True
  else
    Result := ServiceExists(BaseServiceId);
end;

// Get client instance ID
function GetClientInstanceId(Param: string): string;
begin
  Result := ClientInstanceId;
  if Result = '' then Result := 'Default';
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
function GetClientRegistryValue(ValueName, Default: string): string;
var
  Value: string;
begin
  if (ClientInstanceId <> '') and RegQueryStringValue(HKLM, '{#AppRegKeyClients}\' + ClientInstanceId, ValueName, Value) then
    Result := Value
  else if RegQueryStringValue(HKLM, '{#AppRegKey}', ValueName, Value) then
    Result := Value
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
  if Result = '' then Result := 'DefaultClient';
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

// Helper functions to check service checkbox state
function IsPrometheusSelected: Boolean;
begin
  Result := ChkPrometheus.Checked;
end;

function IsGrafanaSelected: Boolean;
begin
  Result := ChkGrafana.Checked;
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

procedure CreateServiceSelectControls(Page: TWizardPage);
var
  TopPos: Integer;
  CoreLabel, MonitorLabel, NoteLabel: TNewStaticText;
  Separator: TBevel;
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
                         WasServiceInstalled('InstalledQSYSOPRMonitor', 'IPMonitoring_QSYSOPRMonitoring');

  TopPos := 8;
  
  // Core Services Section Header
  CoreLabel := TNewStaticText.Create(Page);
  CoreLabel.Parent := Page.Surface;
  CoreLabel.Caption := 'Core Services (Required):';
  CoreLabel.Left := 0;
  CoreLabel.Top := TopPos;
  CoreLabel.Font.Style := [fsBold];
  CoreLabel.Font.Size := 9;
  
  TopPos := TopPos + 26;
  
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
  
  TopPos := TopPos + 32;
  
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
    TopPos := TopPos + 24;
  end;
  
  // Separator line
  Separator := TBevel.Create(Page);
  Separator.Parent := Page.Surface;
  Separator.Left := 0;
  Separator.Top := TopPos;
  Separator.Width := Page.SurfaceWidth;
  Separator.Height := 2;
  Separator.Shape := bsTopLine;
  
  TopPos := TopPos + 16;
  
  // Monitoring Services Section Header
  MonitorLabel := TNewStaticText.Create(Page);
  MonitorLabel.Parent := Page.Surface;
  MonitorLabel.Caption := 'Monitoring Services (Optional):';
  MonitorLabel.Left := 0;
  MonitorLabel.Top := TopPos;
  MonitorLabel.Font.Style := [fsBold];
  MonitorLabel.Font.Size := 9;
  
  TopPos := TopPos + 26;
  
  // IBM IFS Error Monitor
  ChkIFSErrorMonitor := TNewCheckBox.Create(Page);
  ChkIFSErrorMonitor.Parent := Page.Surface;
  ChkIFSErrorMonitor.Caption := 'IBM IFS Error Monitor - Monitors IFS error logs for issues';
  ChkIFSErrorMonitor.Left := 20;
  ChkIFSErrorMonitor.Top := TopPos;
  ChkIFSErrorMonitor.Width := Page.SurfaceWidth - 30;
  ChkIFSErrorMonitor.Height := 21;
  ChkIFSErrorMonitor.Checked := WasServiceInstalled('InstalledIFSErrorMonitor', 'IPMonitoring_IBMIFSErrorMonitor');
  
  TopPos := TopPos + 28;
  
  // IBM Real-Time IFS Monitor
  ChkRealTimeIFSMonitor := TNewCheckBox.Create(Page);
  ChkRealTimeIFSMonitor.Parent := Page.Surface;
  ChkRealTimeIFSMonitor.Caption := 'IBM Real-Time IFS Monitor - Live file system monitoring';
  ChkRealTimeIFSMonitor.Left := 20;
  ChkRealTimeIFSMonitor.Top := TopPos;
  ChkRealTimeIFSMonitor.Width := Page.SurfaceWidth - 30;
  ChkRealTimeIFSMonitor.Height := 21;
  ChkRealTimeIFSMonitor.Checked := WasServiceInstalled('InstalledRealTimeIFSMonitor', 'IPMonitoring_IBMRealTimeIFSMonitor');
  
  TopPos := TopPos + 28;
  
  // IBM Job Queue Count Monitor
  ChkJobQueCountMonitor := TNewCheckBox.Create(Page);
  ChkJobQueCountMonitor.Parent := Page.Surface;
  ChkJobQueCountMonitor.Caption := 'IBM Job Queue Count Monitor - Tracks job queue depths';
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
  ChkUserProfileChecker.Caption := 'IBM User Profile Checker - Monitors user profiles';
  ChkUserProfileChecker.Left := 20;
  ChkUserProfileChecker.Top := TopPos;
  ChkUserProfileChecker.Width := Page.SurfaceWidth - 30;
  ChkUserProfileChecker.Height := 21;
  ChkUserProfileChecker.Checked := WasServiceInstalled('InstalledUserProfileChecker', 'IPMonitoring_IBMUserProfileChecker');
  
  TopPos := TopPos + 28;
  
  // Network Enabler
  ChkNetWorkEnabler := TNewCheckBox.Create(Page);
  ChkNetWorkEnabler.Parent := Page.Surface;
  ChkNetWorkEnabler.Caption := 'Network Enabler - Network monitoring service';
  ChkNetWorkEnabler.Left := 20;
  ChkNetWorkEnabler.Top := TopPos;
  ChkNetWorkEnabler.Width := Page.SurfaceWidth - 30;
  ChkNetWorkEnabler.Height := 21;
  ChkNetWorkEnabler.Checked := WasServiceInstalled('InstalledNetWorkEnabler', 'IPMonitoring_NetWorkEnabler');
  
  TopPos := TopPos + 28;
  
  // QSYSOPR Monitoring
  ChkQSYSOPRMonitor := TNewCheckBox.Create(Page);
  ChkQSYSOPRMonitor.Parent := Page.Surface;
  ChkQSYSOPRMonitor.Caption := 'QSYSOPR Monitor - Monitors QSYSOPR message queue';
  ChkQSYSOPRMonitor.Left := 20;
  ChkQSYSOPRMonitor.Top := TopPos;
  ChkQSYSOPRMonitor.Width := Page.SurfaceWidth - 30;
  ChkQSYSOPRMonitor.Height := 21;
  ChkQSYSOPRMonitor.Checked := WasServiceInstalled('InstalledQSYSOPRMonitor', 'IPMonitoring_QSYSOPRMonitoring');
end;

procedure CreateServicePortsControls(Page: TWizardPage);
var
  TopPos, LabelWidth, EditLeft, EditWidth: Integer;
  Label1, Label2, Label3, Label4, Label5: TNewStaticText;
  Label6, Label7, Label8, Label9, Label10: TNewStaticText;
  DefaultPort: string;
begin
  LabelWidth := 260;
  EditLeft := 270;
  EditWidth := 80;
  TopPos := 8;
  
  // IFS Error Monitor Port
  // For new client installations, use offset ports to avoid conflicts
  // For upgrades (same client), use previously saved ports from registry
  Label1 := TNewStaticText.Create(Page);
  Label1.Parent := Page.Surface;
  Label1.Caption := 'IFS Error Monitor Port:';
  Label1.Left := 0;
  Label1.Top := TopPos + 3;
  Label1.Width := LabelWidth;
  
  // Check if this is a new installation (no saved port) - apply offset
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
  Label2 := TNewStaticText.Create(Page);
  Label2.Parent := Page.Surface;
  Label2.Caption := 'Real-Time IFS Monitor Port:';
  Label2.Left := 0;
  Label2.Top := TopPos + 3;
  Label2.Width := LabelWidth;
  
  // Real-Time IFS Monitor Port
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
  Label3 := TNewStaticText.Create(Page);
  Label3.Parent := Page.Surface;
  Label3.Caption := 'Job Queue Count Monitor Port:';
  Label3.Left := 0;
  Label3.Top := TopPos + 3;
  Label3.Width := LabelWidth;
  
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
  Label4 := TNewStaticText.Create(Page);
  Label4.Parent := Page.Surface;
  Label4.Caption := 'Job Queue Status Monitor Port:';
  Label4.Left := 0;
  Label4.Top := TopPos + 3;
  Label4.Width := LabelWidth;
  
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
  Label5 := TNewStaticText.Create(Page);
  Label5.Parent := Page.Surface;
  Label5.Caption := 'Server UpTime Monitor Port:';
  Label5.Left := 0;
  Label5.Top := TopPos + 3;
  Label5.Width := LabelWidth;
  
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
  Label6 := TNewStaticText.Create(Page);
  Label6.Parent := Page.Surface;
  Label6.Caption := 'SubSystem Monitor Port:';
  Label6.Left := 0;
  Label6.Top := TopPos + 3;
  Label6.Width := LabelWidth;
  
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
  Label7 := TNewStaticText.Create(Page);
  Label7.Parent := Page.Surface;
  Label7.Caption := 'System Matrix Port:';
  Label7.Left := 0;
  Label7.Top := TopPos + 3;
  Label7.Width := LabelWidth;
  
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
  Label8 := TNewStaticText.Create(Page);
  Label8.Parent := Page.Surface;
  Label8.Caption := 'User Profile Checker Port:';
  Label8.Left := 0;
  Label8.Top := TopPos + 3;
  Label8.Width := LabelWidth;
  
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
  Label9 := TNewStaticText.Create(Page);
  Label9.Parent := Page.Surface;
  Label9.Caption := 'Network Enabler Port:';
  Label9.Left := 0;
  Label9.Top := TopPos + 3;
  Label9.Width := LabelWidth;
  
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
  Label10 := TNewStaticText.Create(Page);
  Label10.Parent := Page.Surface;
  Label10.Caption := 'QSYSOPR Monitor Port:';
  Label10.Left := 0;
  Label10.Top := TopPos + 3;
  Label10.Width := LabelWidth;
  
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
  if not IsWin64 then
  begin
    MsgBox('64-bit Windows required.', mbCriticalError, MB_OK);
    Result := False;
    Exit;
  end;
  // Detect if this is an upgrade (previous installation exists)
  IsUpgrade := CheckIsUpgrade;
  
  // Check if a base installation exists (for multi-client support)
  IsFirstInstallation := not CheckBaseInstallationExists;
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
  
  Result := True;
end;

function IsFileLocked(const FilePath: string): Boolean; forward;
procedure WaitForFileUnlock(const FilePath: string; Attempts, DelayMs: Integer); forward;

function PrepareToInstall(var NeedsRestart: Boolean): String;
var
  ResultCode: Integer;
  ServicesDir, ClientSuffix: string;
begin
  Result := '';
  NeedsRestart := False;

  ServicesDir := ExpandConstant('{app}\services');
  
  // Determine client suffix for multi-client services
  if ClientInstanceId <> '' then
    ClientSuffix := '_' + ClientInstanceId
  else
    ClientSuffix := '';

  // Use sc.exe to reliably stop all services (works even if WinSW exe is missing/locked)
  // Stop core services
  Exec('sc.exe', 'stop IPMonitoringPrometheus', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
  Exec('sc.exe', 'stop IPMonitoringGrafana', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
  
  // Stop base monitor services
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
  
  // Stop client-suffixed services if applicable
  if ClientSuffix <> '' then
  begin
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
  end;

  // Wait for services to stop
  Sleep(3000);

  // Force kill any Java processes running from the install directory
  Exec(ExpandConstant('{cmd}'), '/C wmic process where "commandline like ''%' + ExpandConstant('{app}') + '%''" call terminate 2>nul', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
  Exec(ExpandConstant('{cmd}'), '/C taskkill /F /IM java.exe 2>nul', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
  Exec(ExpandConstant('{cmd}'), '/C taskkill /F /IM javaw.exe 2>nul', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
  
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
  Exec(ExpandConstant('{cmd}'),
    '/C netstat -ano | findstr LISTENING | findstr :' + Port,
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
end;

procedure UpdatePrometheusYaml(Port: string); forward;
procedure RemovePrometheusTarget(JobName: string); forward;
procedure GenerateGrafanaIni(Port, Password: string); forward;
procedure UpdateGrafanaDatasource(PrometheusPort: string); forward;
procedure UpdateGrafanaDashboardProvider; forward;
procedure CreateEmailCommonControls(Page: TWizardPage); forward;

procedure InitializeWizard;
var
  ConfigDesc, ServicePortsDesc, ServiceSelectDesc, ClientInstanceDesc: string;
begin
  // Ensure single-client upgrades preload the existing client id for registry lookups
  LoadDefaultClientInstanceId;

  // Keep the installer window on top so it remains clickable when you switch apps
  WizardForm.FormStyle := fsStayOnTop;

  // ===== MULTI-CLIENT INSTALLATION PAGE =====
  // This page appears first to determine installation mode
  ClientInstanceDesc := 'Enter a unique identifier for this client installation.'#13#10#13#10;
  if CheckBaseInstallationExists then
    ClientInstanceDesc := ClientInstanceDesc + 'A base installation already exists. This will add a new client instance that shares the existing Grafana and Prometheus.'
  else
    ClientInstanceDesc := ClientInstanceDesc + 'This is the first installation. Grafana and Prometheus will be installed as shared services.';
  
  ClientInstancePage := CreateInputQueryPage(wpWelcome,
    'Client Instance Configuration', 'Multi-Client Installation Support',
    ClientInstanceDesc);
  ClientInstancePage.Add('Client Instance ID (e.g., ClientA, Store001):', False);
  ClientInstancePage.Add('IBM i Server IP/Hostname for this client:', False);
  
  // Pre-fill with values from previous installation if upgrading same client
  ClientInstancePage.Values[0] := GetClientRegistryValue('ClientInstanceId', '');
  ClientInstancePage.Values[1] := GetClientRegistryValue('IBMiServer', '');

  // Service Selection Page - Custom page with checkboxes that can be disabled
  ServiceSelectDesc := 'Select the services you want to install:';
  if IsUpgrade then
    ServiceSelectDesc := ServiceSelectDesc + #13#10#13#10 + 'NOTE: Core services (Prometheus and Grafana) cannot be uninstalled once installed.';
  if not IsFirstInstallation then
    ServiceSelectDesc := ServiceSelectDesc + #13#10#13#10 + 'NOTE: Prometheus and Grafana are already installed and will be shared with this client instance.';
  
  ServiceSelectPage := CreateCustomPage(ClientInstancePage.ID,
    'What would you like to monitor?', ServiceSelectDesc);
  CreateServiceSelectControls(ServiceSelectPage);

  // Configuration Page - Prometheus/Grafana
  // Set description based on upgrade status
  ConfigDesc := 'Configure service ports and Grafana admin password';
  if IsUpgrade then
    ConfigDesc := ConfigDesc + #13#10#13#10 + 'NOTE: Upgrade detected. Port changes will take effect after service restart.';
  
  ConfigPage := CreateInputQueryPage(ServiceSelectPage.ID,
    'Set Up Your Dashboard Access', 'Configure ports and admin credentials',
    ConfigDesc);
  ConfigPage.Add('Prometheus Port:', False);
  ConfigPage.Add('Grafana Port:', False);
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
    'Configure Monitor Endpoints', ServicePortsDesc);
  CreateServicePortsControls(ServicePortsPage);

  // IBM i Configuration Page
  IBMiConfigPage := CreateInputQueryPage(ServicePortsPage.ID,
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
            ChkQSYSOPRMonitor.Checked;
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
            ChkQSYSOPRMonitor.Checked;
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
  
  // Client Instance
  S := S + 'Client Instance:' + NewLine;
  S := S + Space + 'Client ID: ' + ClientInstanceId + NewLine;
  S := S + Space + 'Mode: ' + InstallMode + NewLine;
  S := S + NewLine;
  
  // Core Services
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
  if not ChkPrometheus.Checked and not ChkGrafana.Checked then
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
  
  if not (ChkIFSErrorMonitor.Checked or ChkRealTimeIFSMonitor.Checked or 
          ChkJobQueCountMonitor.Checked or ChkJobQueStatusMonitor.Checked or 
          ChkServerUpTimeMonitor.Checked or ChkSubSystemMonitor.Checked or
          ChkSystemMatrix.Checked or ChkUserProfileChecker.Checked or
          ChkNetWorkEnabler.Checked or ChkQSYSOPRMonitor.Checked) then
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
  
  // Skip config page if neither Prometheus nor Grafana selected OR if using existing installation
  if PageID = ConfigPage.ID then
  begin
    if UseExistingGrafana and not IsFirstInstallation then
      Result := True
    else
      Result := not ChkPrometheus.Checked and not ChkGrafana.Checked;
  end;
  
  // Skip service ports config if no monitors selected
  if PageID = ServicePortsPage.ID then
    Result := not NeedsServicePortsConfig;
  
  // Skip IBM i config if no monitors that need it are selected
  if PageID = IBMiConfigPage.ID then
    Result := not NeedsIBMiConfig;
  
  // Skip email auth method page if no monitors selected
  if PageID = EmailAuthMethodPage.ID then
    Result := not NeedsEmailConfig;
  
  // Skip SMTP config if no monitors selected OR OAuth2 is selected
  if PageID = SmtpConfigPage.ID then
    Result := not NeedsEmailConfig or not IsSmtpAuthSelected;
  
  // Skip OAuth config pages if no monitors selected OR SMTP is selected
  if (PageID = OAuthConfigPage1.ID) or (PageID = OAuthConfigPage2.ID) then
    Result := not NeedsEmailConfig or IsSmtpAuthSelected;
  
  // Skip common email page if no monitors selected
  if PageID = EmailCommonPage.ID then
    Result := not NeedsEmailConfig;
end;

procedure CurPageChanged(CurPageID: Integer);
begin
  // No special handling needed for the new page structure
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
  
  // Check monitor ports against Prometheus/Grafana
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
  AnyMonitorSelected: Boolean;
begin
  Result := True;
  
  // Validate Client Instance Page
  if CurPageID = ClientInstancePage.ID then
  begin
    // Client Instance ID is required
    ClientInstanceId := Trim(ClientInstancePage.Values[0]);
    if ClientInstanceId = '' then
    begin
      MsgBox('Please enter a Client Instance ID. This uniquely identifies this installation (e.g., ClientA, Store001).', mbError, MB_OK);
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
    
    // Check if this client already exists (and it's not an upgrade)
    if not IsUpgrade and CheckClientExists(ClientInstanceId) then
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
  
  // Validate service selection - cannot uninstall Prometheus/Grafana if monitors are selected
  if CurPageID = ServiceSelectPage.ID then
  begin
    AnyMonitorSelected := ChkIFSErrorMonitor.Checked or
                          ChkRealTimeIFSMonitor.Checked or
                          ChkJobQueCountMonitor.Checked or
                          ChkJobQueStatusMonitor.Checked or
                          ChkServerUpTimeMonitor.Checked or
                          ChkSubSystemMonitor.Checked or
                          ChkSystemMatrix.Checked or
                          ChkUserProfileChecker.Checked or
                          ChkNetWorkEnabler.Checked or
                          ChkQSYSOPRMonitor.Checked;
    
    // If any monitor is selected, Prometheus and Grafana must also be selected
    if AnyMonitorSelected then
    begin
      if not ChkPrometheus.Checked then
      begin
        MsgBox('Prometheus is required for monitoring services. Please select Prometheus or uncheck all monitoring services first.', mbError, MB_OK);
        Result := False;
        Exit;
      end;
      if not ChkGrafana.Checked then
      begin
        MsgBox('Grafana is required for monitoring services. Please select Grafana or uncheck all monitoring services first.', mbError, MB_OK);
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
      end;
    end;
    
    // Check for duplicate ports between selected monitors and core services
    if Result then
      Result := ValidateNoDuplicatePorts;
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
  SaveStringToFile(D + '\services\' + ServiceExeName + '.xml',
    '<service>'#13#10 +
    '  <id>' + ServiceId + '</id>'#13#10 +
    '  <name>' + ServiceName + '</name>'#13#10 +
    '  <description>' + Description + '</description>'#13#10 +
    '  <executable>' + Executable + '</executable>'#13#10 +
    '  <arguments>' + Arguments + '</arguments>'#13#10 +
    '  <workingdirectory>' + WorkDir + '</workingdirectory>'#13#10 +
    '  <log mode="roll"></log>'#13#10 +
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
  SearchStr := 'job_name: "' + JobName + '"';
  JobStartPos := Pos(SearchStr, ContentStr);
  
  if JobStartPos = 0 then
  begin
    Log('RemovePrometheusTarget: Job "' + JobName + '" not found, skipping');
    Exit;
  end;
  
  Log('RemovePrometheusTarget: Found job "' + JobName + '" at pos ' + IntToStr(JobStartPos));
  
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
  D, FullPath: string;
begin
  D := ExpandConstant('{app}');
  FullPath := D + '\services\' + ServiceExe;
  
  // Check if service executable exists
  if not FileExists(FullPath) then
  begin
    Log('ERROR: Service executable not found: ' + FullPath);
    Exit;
  end;
  
  // Stop existing service first (in case of upgrade)
  Exec(FullPath, 'stop', '', SW_HIDE, ewWaitUntilTerminated, Code);
  Sleep(500); // Wait for service to stop
  
  Exec(FullPath, 'uninstall', '', SW_HIDE, ewWaitUntilTerminated, Code);
  Sleep(500); // Wait for uninstall to complete
  
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
  ServicesDir, ServiceExe, ServiceName: string;
begin
  ServicesDir := ExpandConstant('{app}\services');
  if ClientInstanceId <> '' then
  begin
    ServiceExe := ServicesDir + '\' + BaseServiceId + '_' + ClientInstanceId + '.exe';
    ServiceName := BaseServiceId + '_' + ClientInstanceId;
  end
  else
  begin
    ServiceExe := ServicesDir + '\' + BaseServiceId + '.exe';
    ServiceName := BaseServiceId;
  end;

  if FileExists(ServiceExe) or ServiceExists(BaseServiceId) then
  begin
    Exec(ServiceExe, 'stop', '', SW_HIDE, ewWaitUntilTerminated, Code);
    Sleep(300);
    Exec(ServiceExe, 'uninstall', '', SW_HIDE, ewWaitUntilTerminated, Code);
    Sleep(300);
    // Fallback via sc.exe
    Exec('sc.exe', 'stop "' + ServiceName + '"', '', SW_HIDE, ewWaitUntilTerminated, Code);
    Exec('sc.exe', 'delete "' + ServiceName + '"', '', SW_HIDE, ewWaitUntilTerminated, Code);
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

procedure CurStepChanged(CurStep: TSetupStep);
var
  D: string;
begin
  if CurStep = ssPostInstall then
  begin
    D := ExpandConstant('{app}');

    // Uninstall any monitors that were deselected during upgrade/modify
    if not ChkIFSErrorMonitor.Checked then UninstallServiceIfPresent('IPMonitoring_IBMIFSErrorMonitor');
    if not ChkRealTimeIFSMonitor.Checked then UninstallServiceIfPresent('IPMonitoring_IBMRealTimeIFSMonitor');
    if not ChkJobQueCountMonitor.Checked then UninstallServiceIfPresent('IPMonitoring_IBMJobQueCountMonitor');
    if not ChkJobQueStatusMonitor.Checked then UninstallServiceIfPresent('IPMonitoring_IBMJobQueStatusMonitor');
    if not ChkServerUpTimeMonitor.Checked then UninstallServiceIfPresent('IPMonitoring_ServerUpTimeMonitor');
    if not ChkSubSystemMonitor.Checked then UninstallServiceIfPresent('IPMonitoring_IBMSubSystemMonitoring');
    if not ChkSystemMatrix.Checked then UninstallServiceIfPresent('IPMonitoring_IBMSystemMatrix');
    if not ChkUserProfileChecker.Checked then UninstallServiceIfPresent('IPMonitoring_IBMUserProfileChecker');
    if not ChkNetWorkEnabler.Checked then UninstallServiceIfPresent('IPMonitoring_NetWorkEnabler');
    if not ChkQSYSOPRMonitor.Checked then UninstallServiceIfPresent('IPMonitoring_QSYSOPRMonitoring');
    
    // ===== Register this client instance in registry =====
    if ClientInstanceId <> '' then
    begin
      RegWriteStringValue(HKLM, '{#AppRegKeyClients}\' + ClientInstanceId, 'InstallPath', D);
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
    
    // ===== Install Prometheus if selected (skip if using existing) =====
    if ChkPrometheus.Checked and IsFirstInstallation then
    begin
      GenerateServiceXml('IPMonitoringPrometheus', 'IPMonitoringPrometheus', 
        'IP Monitoring - Prometheus', 'Prometheus metrics collection service',
        D + '\prometheus\prometheus.exe',
        '--config.file="' + D + '\prometheus\prometheus.yml" --storage.tsdb.path="' + D + '\data\prometheus" --web.listen-address=:' + GetPromPort(''),
        D + '\prometheus');
      UpdatePrometheusYaml(GetPromPort(''));
      InstallAndStartService('IPMonitoringPrometheus.exe');
    end;

    // ===== Install Grafana if selected (skip if using existing) =====
    if ChkGrafana.Checked and IsFirstInstallation then
    begin
      GenerateServiceXml('IPMonitoringGrafana', 'IPMonitoringGrafana',
        'IP Monitoring - Grafana', 'Grafana dashboard and visualization service',
        D + '\grafana\bin\grafana-server.exe',
        '--homepath "' + D + '\grafana" --config "' + D + '\grafana\conf\custom.ini"',
        D + '\grafana');
      GenerateGrafanaIni(GetGrafPort(''), GetGrafPwd(''));
      UpdateGrafanaDatasource(GetPromPort(''));
      UpdateGrafanaDashboardProvider;
      InstallAndStartService('IPMonitoringGrafana.exe');
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
    
    // ===== Restart Prometheus to pick up new targets =====
    // Prometheus needs to be restarted after all monitoring services have been added
    // to the prometheus.yml configuration file (even for multi-client installs)
    if (ChkIFSErrorMonitor.Checked or ChkRealTimeIFSMonitor.Checked or 
        ChkJobQueCountMonitor.Checked or ChkJobQueStatusMonitor.Checked or 
        ChkServerUpTimeMonitor.Checked or ChkSubSystemMonitor.Checked or
        ChkSystemMatrix.Checked or ChkUserProfileChecker.Checked or
        ChkNetWorkEnabler.Checked or ChkQSYSOPRMonitor.Checked) then
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
begin
  if CurUninstallStep = usUninstall then
  begin
    AppPath := ExpandConstant('{app}');
    ServicesDir := AppPath + '\services';
    
    // Try to get ClientInstanceId from multiple possible registry locations
    SavedClientId := '';
    
    // First, try to find it by scanning the Clients subkeys
    if RegQueryStringValue(HKLM, 'Software\IslandPacific\Monitoring\Clients', '', SavedClientId) then
      ; // Got it from default value (unlikely)
    
    // Scan through all registered clients to find the one matching our install path
    if SavedClientId = '' then
    begin
      if FindFirst('Software\IslandPacific\Monitoring\Clients\*', FindRec) then
      begin
        // FindFirst doesn't work on registry, so we need another approach
      end;
    end;
    
    // Fallback: Extract client ID from service executable names in the services folder
    if (SavedClientId = '') and DirExists(ServicesDir) then
    begin
      if FindFirst(ServicesDir + '\IPMonitoring_IBMIFSErrorMonitor_*.exe', FindRec) then
      begin
        try
          // Extract client ID from filename like "IPMonitoring_IBMIFSErrorMonitor_CLIENTID.exe"
          SavedClientId := Copy(FindRec.Name, Length('IPMonitoring_IBMIFSErrorMonitor_') + 1, 
                                Length(FindRec.Name) - Length('IPMonitoring_IBMIFSErrorMonitor_') - 4);
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
    
    // Define all service base names (without client suffix)
    SetArrayLength(ServiceBasenames, 10);
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
    
    // Stop and uninstall core services (Prometheus and Grafana)
    if FileExists(ServicesDir + '\IPMonitoringPrometheus.exe') then
    begin
      Exec(ServicesDir + '\IPMonitoringPrometheus.exe', 'stop', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
      Sleep(500);
      Exec(ServicesDir + '\IPMonitoringPrometheus.exe', 'uninstall', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
    end;
    Exec('sc.exe', 'stop "IPMonitoringPrometheus"', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
    Exec('sc.exe', 'delete "IPMonitoringPrometheus"', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
    
    if FileExists(ServicesDir + '\IPMonitoringGrafana.exe') then
    begin
      Exec(ServicesDir + '\IPMonitoringGrafana.exe', 'stop', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
      Sleep(500);
      Exec(ServicesDir + '\IPMonitoringGrafana.exe', 'uninstall', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
    end;
    Exec('sc.exe', 'stop "IPMonitoringGrafana"', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
    Exec('sc.exe', 'delete "IPMonitoringGrafana"', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
    
    // Give services time to fully stop before file deletion
    Sleep(2000);
  end;
end;
