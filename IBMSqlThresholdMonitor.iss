#define AppName "Island Pacific IBM SQL Threshold Monitor"
#define AppVersion "1.0.0"
#define AppPublisher "Island Pacific Retail Systems"
#define ServiceName "IPMonitoring_IBMSqlThresholdMonitor"
#define MonitorDir "IBMSqlThresholdMonitor"
#define JarFile "IBMSqlThresholdMonitor.jar"

[Setup]
AppId={{IP-IBMSqlThresholdMonitor}}
AppName={#AppName}
AppVersion={#AppVersion}
AppPublisher={#AppPublisher}
AppVerName={#AppName} v{#AppVersion}
AppCopyright=Copyright © 2025 Island Pacific Retail Systems
AppSupportURL=https://www.islandpacific.com/
VersionInfoVersion=1.0.0.0
VersionInfoCompany=Island Pacific Retail Systems
VersionInfoProductName={#AppName}
VersionInfoDescription=Island Pacific IBM SQL Threshold Monitor

ArchitecturesInstallIn64BitMode=x64compatible
DefaultDirName={commonpf}\Island Pacific\IBMSqlThresholdMonitor
DisableDirPage=no
DisableProgramGroupPage=yes
PrivilegesRequired=admin
WizardStyle=modern
WizardSizePercent=125
WizardImageFile=installer\resources\wizard_modern.bmp
WizardSmallImageFile=installer\resources\wizard_small_modern.bmp
SetupIconFile=installer\resources\ip-monitoring.ico
LicenseFile=installer\resources\license.txt

AppMutex=IPMonitoring_IBMSqlThresholdMonitor_Mutex
CloseApplications=no

Compression=lzma
SolidCompression=yes

OutputDir=.\installer\output
OutputBaseFilename=IBMSqlThresholdMonitorSetup

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Dirs]
Name: "{app}\services"
Name: "{app}\monitoring-services\{#MonitorDir}"
Name: "{app}\logs"; Flags: uninsneveruninstall

[Files]
; WinSW wrapper
Source: "installer\resources\WinSW.exe"; DestDir: "{app}\services"; DestName: "{#ServiceName}.exe"; Flags: ignoreversion

; JAR
Source: "installer\resources\monitoring-services\{#MonitorDir}\{#JarFile}"; DestDir: "{app}\monitoring-services\{#MonitorDir}"; Flags: ignoreversion

; DPAPI credential encryption tool (always installed; run on this machine to encrypt property values)
Source: "installer\resources\monitoring-services\CredTool\CredTool.jar"; DestDir: "{app}"; Flags: ignoreversion skipifsourcedoesntexist

; Properties files — preserved on upgrade
Source: "installer\resources\monitoring-services\{#MonitorDir}\sqlthresholdmonitor.properties"; DestDir: "{app}\monitoring-services\{#MonitorDir}"; Flags: onlyifdoesntexist skipifsourcedoesntexist
Source: "installer\resources\monitoring-services\{#MonitorDir}\email.properties"; DestDir: "{app}\monitoring-services\{#MonitorDir}"; Flags: onlyifdoesntexist skipifsourcedoesntexist

[Code]
function IsUpgrade: Boolean;
begin
  Result := RegKeyExists(HKLM, 'SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\{IP-IBMSqlThresholdMonitor}_is1');
end;

var
  IbmiPage: TInputQueryWizardPage;
  MetricsPortPage: TInputQueryWizardPage;
  IntervalPage: TInputQueryWizardPage;
  ClientNamePage: TInputQueryWizardPage;
  SqlCheckPage: TInputQueryWizardPage;
  EmailAuthPage: TInputOptionWizardPage;
  SmtpPage: TInputQueryWizardPage;
  OAuth2Page: TInputQueryWizardPage;

procedure InitializeWizard;
begin
  IbmiPage := CreateInputQueryPage(wpSelectDir,
    'IBM i Connection', 'AS/400 JDBC connection details',
    'Enter the IBM i host and credentials used to run the SQL checks:');
  IbmiPage.Add('IBM i Host/IP:', False);
  IbmiPage.Add('User:', False);
  IbmiPage.Add('Password:', True);

  MetricsPortPage := CreateInputQueryPage(IbmiPage.ID,
    'Metrics Port', 'Prometheus metrics port',
    'Enter the port this monitor will expose metrics on:');
  MetricsPortPage.Add('Port:', False);
  MetricsPortPage.Values[0] := '3025';

  IntervalPage := CreateInputQueryPage(MetricsPortPage.ID,
    'Monitor Interval', 'How often to run the SQL checks',
    'Enter the polling interval in minutes:');
  IntervalPage.Add('Interval (minutes):', False);
  IntervalPage.Values[0] := '5';

  ClientNamePage := CreateInputQueryPage(IntervalPage.ID,
    'Client Name', 'Name shown in alert emails',
    'Enter the client name to appear in alert email subjects:');
  ClientNamePage.Add('Client name:', False);
  ClientNamePage.Values[0] := '';

  SqlCheckPage := CreateInputQueryPage(ClientNamePage.ID,
    'SQL Check', 'First SQL row-count threshold check',
    'Enter details for the first SQL check. Additional checks can be added manually in sqlthresholdmonitor.properties.');
  SqlCheckPage.Add('Check name (label):', False);
  SqlCheckPage.Add('SQL query:', False);
  SqlCheckPage.Add('Row count threshold:', False);
  SqlCheckPage.Add('Comparison (gt or gte):', False);
  SqlCheckPage.Values[2] := '1';
  SqlCheckPage.Values[3] := 'gt';

  EmailAuthPage := CreateInputOptionPage(SqlCheckPage.ID,
    'Email Authentication', 'How to send alert emails',
    'Select the email authentication method:',
    True, False);
  EmailAuthPage.Add('SMTP');
  EmailAuthPage.Add('OAuth2 (Microsoft 365 / Graph API)');
  EmailAuthPage.Values[0] := True;

  SmtpPage := CreateInputQueryPage(EmailAuthPage.ID,
    'SMTP Settings', 'Email server configuration',
    'Enter your SMTP server details:');
  SmtpPage.Add('SMTP Host:', False);
  SmtpPage.Add('SMTP Port:', False);
  SmtpPage.Add('From Address:', False);
  SmtpPage.Add('To Address(es) (comma-separated):', False);
  SmtpPage.Add('BCC Address(es) (optional):', False);
  SmtpPage.Values[1] := '25';

  OAuth2Page := CreateInputQueryPage(EmailAuthPage.ID,
    'OAuth2 / Microsoft 365 Settings', 'Azure AD app credentials',
    'Enter your Azure AD application credentials:');
  OAuth2Page.Add('Tenant ID:', False);
  OAuth2Page.Add('Client ID:', False);
  OAuth2Page.Add('Client Secret:', True);
  OAuth2Page.Add('From Address (mailbox user):', False);
  OAuth2Page.Add('To Address(es) (comma-separated):', False);
  OAuth2Page.Add('BCC Address(es) (optional):', False);
end;

function ShouldSkipPage(PageID: Integer): Boolean;
begin
  Result := False;
  if IsUpgrade then begin
    if (PageID = IbmiPage.ID) or (PageID = MetricsPortPage.ID) or (PageID = IntervalPage.ID) or
       (PageID = ClientNamePage.ID) or (PageID = SqlCheckPage.ID) or
       (PageID = EmailAuthPage.ID) or (PageID = SmtpPage.ID) or
       (PageID = OAuth2Page.ID) then
      Result := True;
    Exit;
  end;
  if PageID = SmtpPage.ID then
    Result := EmailAuthPage.Values[1];
  if PageID = OAuth2Page.ID then
    Result := EmailAuthPage.Values[0];
end;

function GetAuthMethod(Param: String): String;
begin
  if EmailAuthPage.Values[1] then Result := 'OAUTH2'
  else Result := 'SMTP';
end;

function GetEmailFrom(Param: String): String;
begin
  if EmailAuthPage.Values[1] then Result := OAuth2Page.Values[3]
  else Result := SmtpPage.Values[2];
end;

function GetEmailTo(Param: String): String;
begin
  if EmailAuthPage.Values[1] then Result := OAuth2Page.Values[4]
  else Result := SmtpPage.Values[3];
end;

function GetEmailBcc(Param: String): String;
begin
  if EmailAuthPage.Values[1] then Result := OAuth2Page.Values[5]
  else Result := SmtpPage.Values[4];
end;

function GetGraphMailUrl(Param: String): String;
begin
  if EmailAuthPage.Values[1] then
    Result := 'https://graph.microsoft.com/v1.0/users/' + OAuth2Page.Values[3] + '/sendMail'
  else
    Result := '';
end;

procedure WritePropertiesFiles;
var
  EmailProps, AppProps: TStringList;
  EmailFile, AppFile: String;
  AuthMethod: String;
  CheckName, CheckKey: String;
begin
  AuthMethod := GetAuthMethod('');
  EmailFile := ExpandConstant('{app}\monitoring-services\{#MonitorDir}\email.properties');
  AppFile   := ExpandConstant('{app}\monitoring-services\{#MonitorDir}\sqlthresholdmonitor.properties');

  EmailProps := TStringList.Create;
  try
    EmailProps.Add('# ===============================');
    EmailProps.Add('# Email Configuration');
    EmailProps.Add('# ===============================');
    EmailProps.Add('');
    EmailProps.Add('# Authentication Method: SMTP or OAUTH2');
    EmailProps.Add('mail.auth.method=' + AuthMethod);
    EmailProps.Add('');
    if AuthMethod = 'SMTP' then begin
      EmailProps.Add('# SMTP server details (used when mail.auth.method=SMTP)');
      EmailProps.Add('mail.smtp.host=' + SmtpPage.Values[0]);
      EmailProps.Add('mail.smtp.port=' + SmtpPage.Values[1]);
      EmailProps.Add('mail.smtp.auth=false');
      EmailProps.Add('mail.smtp.starttls.enable=false');
      EmailProps.Add('');
      EmailProps.Add('# OAuth2 Configuration — fill in if switching to OAUTH2');
      EmailProps.Add('#mail.oauth2.tenant.id=');
      EmailProps.Add('#mail.oauth2.client.id=');
      EmailProps.Add('#mail.oauth2.client.secret=');
      EmailProps.Add('#mail.oauth2.scope=https://graph.microsoft.com/.default');
      EmailProps.Add('#mail.oauth2.token.url=https://login.microsoftonline.com/<tenant-id>/oauth2/v2.0/token');
      EmailProps.Add('#mail.oauth2.graph.mail.url=https://graph.microsoft.com/v1.0/users/<from-address>/sendMail');
    end else begin
      EmailProps.Add('# SMTP server details — fill in if switching to SMTP');
      EmailProps.Add('#mail.smtp.host=');
      EmailProps.Add('#mail.smtp.port=25');
      EmailProps.Add('#mail.smtp.auth=false');
      EmailProps.Add('#mail.smtp.starttls.enable=false');
      EmailProps.Add('');
      EmailProps.Add('# OAuth2 / Microsoft 365 / Azure AD credentials (used when mail.auth.method=OAUTH2)');
      EmailProps.Add('mail.oauth2.tenant.id=' + OAuth2Page.Values[0]);
      EmailProps.Add('mail.oauth2.client.id=' + OAuth2Page.Values[1]);
      EmailProps.Add('mail.oauth2.client.secret=' + OAuth2Page.Values[2]);
      EmailProps.Add('mail.oauth2.scope=https://graph.microsoft.com/.default');
      EmailProps.Add('mail.oauth2.token.url=https://login.microsoftonline.com/' + OAuth2Page.Values[0] + '/oauth2/v2.0/token');
      EmailProps.Add('mail.oauth2.graph.mail.url=' + GetGraphMailUrl(''));
    end;
    EmailProps.Add('');
    EmailProps.Add('# Sender and recipients');
    EmailProps.Add('mail.from=' + GetEmailFrom(''));
    EmailProps.Add('# Comma-separated list of recipient addresses');
    EmailProps.Add('mail.to=' + GetEmailTo(''));
    EmailProps.Add('# Optional BCC addresses (comma-separated, leave blank to disable)');
    EmailProps.Add('mail.bcc=' + GetEmailBcc(''));
    EmailProps.Add('# Email importance level: High, Normal, Low');
    EmailProps.Add('mail.importance=High');
    EmailProps.Add('');
    EmailProps.Add('# ===============================');
    EmailProps.Add('# Logging Configuration');
    EmailProps.Add('# ===============================');
    EmailProps.Add('# Log level: INFO, DEBUG, WARNING, SEVERE');
    EmailProps.Add('log.level=INFO');
    EmailProps.Add('# Folder where log files are written (relative to install dir, or absolute path)');
    EmailProps.Add('log.folder=logs');
    EmailProps.Add('# Number of days to retain log files before purging');
    EmailProps.Add('log.retention.days=30');
    EmailProps.Add('# How often to run the log purge check (hours)');
    EmailProps.Add('log.purge.interval.hours=24');
    EmailProps.SaveToFile(EmailFile);
  finally
    EmailProps.Free;
  end;

  CheckName := SqlCheckPage.Values[0];
  if CheckName = '' then CheckName := 'CHECK1';
  CheckKey := CheckName;

  AppProps := TStringList.Create;
  try
    AppProps.Add('# ===============================');
    AppProps.Add('# IBM SQL Threshold Monitor Configuration');
    AppProps.Add('# ===============================');
    AppProps.Add('');
    AppProps.Add('# IBM i connection');
    AppProps.Add('ibmi.server=' + IbmiPage.Values[0]);
    AppProps.Add('ibmi.user=' + IbmiPage.Values[1]);
    AppProps.Add('ibmi.password=' + IbmiPage.Values[2]);
    AppProps.Add('');
    AppProps.Add('# Prometheus metrics port — must be unique per monitor on this machine');
    AppProps.Add('metrics.port=' + MetricsPortPage.Values[0]);
    AppProps.Add('');
    AppProps.Add('# How often to run the SQL checks (minutes)');
    AppProps.Add('monitor.interval.minutes=' + IntervalPage.Values[0]);
    AppProps.Add('');
    AppProps.Add('# Client name shown in alert email subjects');
    AppProps.Add('client.name=' + ClientNamePage.Values[0]);
    AppProps.Add('');
    AppProps.Add('# ===============================');
    AppProps.Add('# Logging');
    AppProps.Add('# ===============================');
    AppProps.Add('log.level=INFO');
    AppProps.Add('log.folder=logs');
    AppProps.Add('log.retention.days=30');
    AppProps.Add('log.purge.interval.hours=24');
    AppProps.Add('');
    AppProps.Add('# ===============================');
    AppProps.Add('# Monitored SQL Checks');
    AppProps.Add('# ===============================');
    AppProps.Add('# Add one block per check. Key prefix: sql.<CHECK_CODE>');
    AppProps.Add('# Fields:');
    AppProps.Add('#   name              — display name used in alert emails');
    AppProps.Add('#   query             — SQL query to execute');
    AppProps.Add('#   threshold         — row count threshold (default: 1)');
    AppProps.Add('#   comparison        — gt (alert when rows > threshold) or gte (alert when rows >= threshold)');
    AppProps.Add('#   email.importance  — High, Normal, or Low');
    AppProps.Add('');
    AppProps.Add('sql.' + CheckKey + '.name=' + CheckName);
    AppProps.Add('sql.' + CheckKey + '.query=' + SqlCheckPage.Values[1]);
    AppProps.Add('sql.' + CheckKey + '.threshold=' + SqlCheckPage.Values[2]);
    AppProps.Add('sql.' + CheckKey + '.comparison=' + SqlCheckPage.Values[3]);
    AppProps.Add('sql.' + CheckKey + '.email.importance=Normal');
    AppProps.Add('');
    AppProps.Add('# Example: add a second check by duplicating the block below');
    AppProps.Add('# sql.CHECK2.name=My Other Check');
    AppProps.Add('# sql.CHECK2.query=SELECT * FROM MYLIB.MYFILE WHERE STATUS = ''X''');
    AppProps.Add('# sql.CHECK2.threshold=1');
    AppProps.Add('# sql.CHECK2.comparison=gt');
    AppProps.Add('# sql.CHECK2.email.importance=Normal');
    AppProps.SaveToFile(AppFile);
  finally
    AppProps.Free;
  end;
end;

procedure WriteServiceXml;
var
  Xml: TStringList;
  XmlFile: String;
  InstallDir, AppDir: String;
begin
  InstallDir := ExpandConstant('{app}');
  AppDir := InstallDir + '\monitoring-services\{#MonitorDir}';
  XmlFile := InstallDir + '\services\{#ServiceName}.xml';

  Xml := TStringList.Create;
  try
    Xml.Add('<service>');
    Xml.Add('  <id>{#ServiceName}</id>');
    Xml.Add('  <name>Island Pacific IBM SQL Threshold Monitor</name>');
    Xml.Add('  <description>Runs configured SQL row-count checks against IBM i DB2 and exposes Prometheus metrics</description>');
    Xml.Add('  <executable>java</executable>');
    Xml.Add('  <arguments>-jar "' + AppDir + '\{#JarFile}" "' + AppDir + '\email.properties" "' + AppDir + '\sqlthresholdmonitor.properties"</arguments>');
    Xml.Add('  <logmode>rotate</logmode>');
    Xml.Add('  <log name="' + InstallDir + '\logs\{#ServiceName}">');
    Xml.Add('    <sizeThreshold>10240</sizeThreshold>');
    Xml.Add('    <keepFiles>8</keepFiles>');
    Xml.Add('  </log>');
    Xml.Add('  <onfailure action="restart" delay="10 sec"/>');
    Xml.Add('  <onfailure action="restart" delay="20 sec"/>');
    Xml.Add('  <onfailure action="none"/>');
    Xml.Add('</service>');
    Xml.SaveToFile(XmlFile);
  finally
    Xml.Free;
  end;
end;

procedure CurStepChanged(CurStep: TSetupStep);
var
  ServiceExe, InstallDir, PropsFile, NL, Msg: String;
  ResultCode: Integer;
begin
  NL := Chr(13) + Chr(10);

  if CurStep = ssInstall then begin
    ServiceExe := ExpandConstant('{app}\services\{#ServiceName}.exe');
    if FileExists(ServiceExe) then begin
      Exec(ServiceExe, 'stop', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
      Sleep(2000);
    end;
  end;

  if CurStep = ssPostInstall then begin
    InstallDir := ExpandConstant('{app}');
    ServiceExe := InstallDir + '\services\{#ServiceName}.exe';
    PropsFile  := InstallDir + '\monitoring-services\{#MonitorDir}\sqlthresholdmonitor.properties';
    WriteServiceXml;

    if IsUpgrade then begin
      Msg := 'Upgrade complete.' + NL + NL +
             'Your existing properties files have been preserved.' + NL +
             'Please verify the configuration before starting the service:' + NL + NL +
             PropsFile + NL + NL +
             'When ready, start the service from services.msc or run:' + NL +
             '"' + ServiceExe + '" start';
      MsgBox(Msg, mbInformation, MB_OK);
    end else begin
      WritePropertiesFiles;
      if not Exec(ServiceExe, 'install', '', SW_HIDE, ewWaitUntilTerminated, ResultCode) then
        MsgBox('Warning: Could not register the Windows service.' + NL +
               'You may need to run manually: "' + ServiceExe + '" install',
               mbError, MB_OK);
      Msg := 'Installation complete.' + NL + NL +
             'The service has been registered but NOT started.' + NL +
             'Please review and update the properties files before starting:' + NL + NL +
             PropsFile + NL + NL +
             'When ready, start the service from services.msc or run:' + NL +
             '"' + ServiceExe + '" start';
      MsgBox(Msg, mbInformation, MB_OK);
    end;
  end;
end;

procedure CurUninstallStepChanged(CurUninstallStep: TUninstallStep);
var
  ServiceExe: String;
  ResultCode: Integer;
begin
  if CurUninstallStep = usUninstall then begin
    ServiceExe := ExpandConstant('{app}\services\{#ServiceName}.exe');
    Exec(ServiceExe, 'stop', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
    Exec(ServiceExe, 'uninstall', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
  end;
end;

[UninstallDelete]
Type: files; Name: "{app}\services\*.xml"
Type: files; Name: "{app}\monitoring-services\{#MonitorDir}\email.properties"
Type: dirifempty; Name: "{app}\services"
Type: dirifempty; Name: "{app}\monitoring-services\{#MonitorDir}"
Type: dirifempty; Name: "{app}\monitoring-services"
