#define AppName "Island Pacific Win Service Monitor"
#define AppVersion "1.0.0"
#define AppPublisher "Island Pacific Retail Systems"
#define ServiceName "IPMonitoring_WinServiceMonitor"
#define MonitorDir "WinServiceMonitor"
#define JarFile "WinServiceMonitor.jar"

[Setup]
AppId={{IP-WinServiceMonitor}}
AppName={#AppName}
AppVersion={#AppVersion}
AppPublisher={#AppPublisher}
AppVerName={#AppName} v{#AppVersion}
AppCopyright=Copyright © 2025 Island Pacific Retail Systems
AppSupportURL=https://www.islandpacific.com/
VersionInfoVersion=1.0.0.0
VersionInfoCompany=Island Pacific Retail Systems
VersionInfoProductName={#AppName}
VersionInfoDescription=Island Pacific Win Service Monitor

ArchitecturesInstallIn64BitMode=x64compatible
DefaultDirName={commonpf}\Island Pacific\WinServiceMonitor
DisableDirPage=no
DisableProgramGroupPage=yes
PrivilegesRequired=admin
WizardStyle=modern
WizardSizePercent=125
WizardImageFile=installer\resources\wizard_modern.bmp
WizardSmallImageFile=installer\resources\wizard_small_modern.bmp
SetupIconFile=installer\resources\ip-monitoring.ico
LicenseFile=installer\resources\license.txt

; Upgrade support — same AppId means Inno Setup detects existing install
; and offers upgrade in-place without asking for a new directory
AppMutex=IPMonitoring_WinServiceMonitor_Mutex
CloseApplications=no

Compression=lzma
SolidCompression=yes

OutputDir=.\installer\output
OutputBaseFilename=WinServiceMonitorSetup

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

; Properties files — preserved on upgrade
Source: "installer\resources\monitoring-services\{#MonitorDir}\winservicemonitor.properties"; DestDir: "{app}\monitoring-services\{#MonitorDir}"; Flags: onlyifdoesntexist skipifsourcedoesntexist
Source: "installer\resources\monitoring-services\{#MonitorDir}\email.properties"; DestDir: "{app}\monitoring-services\{#MonitorDir}"; Flags: onlyifdoesntexist skipifsourcedoesntexist

[Code]
function IsUpgrade: Boolean;
begin
  Result := RegKeyExists(HKLM, 'SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\{IP-WinServiceMonitor}_is1');
end;

var
  MetricsPortPage: TInputQueryWizardPage;
  IntervalPage: TInputQueryWizardPage;
  AlertWindowPage: TInputQueryWizardPage;
  ServersPage: TInputQueryWizardPage;
  ServicesPage: TInputQueryWizardPage;
  EmailAuthPage: TInputOptionWizardPage;
  SmtpPage: TInputQueryWizardPage;
  OAuth2Page: TInputQueryWizardPage;
  EmailAddrPage: TInputQueryWizardPage;

procedure InitializeWizard;
begin
  MetricsPortPage := CreateInputQueryPage(wpSelectDir,
    'Metrics Port', 'Prometheus metrics port',
    'Enter the port this monitor will expose metrics on:');
  MetricsPortPage.Add('Port:', False);
  MetricsPortPage.Values[0] := '3026';

  IntervalPage := CreateInputQueryPage(MetricsPortPage.ID,
    'Monitor Interval', 'How often to check services',
    'Enter the polling interval in seconds:');
  IntervalPage.Add('Interval (seconds):', False);
  IntervalPage.Values[0] := '60';

  AlertWindowPage := CreateInputQueryPage(IntervalPage.ID,
    'Alert Window', 'Consecutive failures before alerting',
    'Enter the number of consecutive down cycles before an alert is sent:');
  AlertWindowPage.Add('Alert window size:', False);
  AlertWindowPage.Values[0] := '2';

  ServersPage := CreateInputQueryPage(AlertWindowPage.ID,
    'Servers to Monitor', 'Comma-separated list of servers',
    'Enter the Windows servers to monitor (use "localhost" for this machine):');
  ServersPage.Add('Servers:', False);
  ServersPage.Values[0] := 'localhost';

  ServicesPage := CreateInputQueryPage(ServersPage.ID,
    'Services to Monitor', 'Comma-separated list of Windows service names',
    'Enter the Windows service names to monitor (as shown in services.msc):');
  ServicesPage.Add('Services:', False);
  ServicesPage.Values[0] := '';

  EmailAuthPage := CreateInputOptionPage(ServicesPage.ID,
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
  OAuth2Page.Add('Client Secret:', False);
  OAuth2Page.Add('From Address (mailbox user):', False);
  OAuth2Page.Add('To Address(es) (comma-separated):', False);
  OAuth2Page.Add('BCC Address(es) (optional):', False);

  EmailAddrPage := CreateInputQueryPage(OAuth2Page.ID,
    'Email Addresses', 'Alert recipients',
    'Confirm alert email addresses (pre-filled from above):');
  EmailAddrPage.Add('From:', False);
  EmailAddrPage.Add('To:', False);
  EmailAddrPage.Add('BCC (optional):', False);
end;

function ShouldSkipPage(PageID: Integer): Boolean;
begin
  Result := False;
  // On upgrade skip all config pages — existing properties files are preserved
  if IsUpgrade then begin
    if (PageID = MetricsPortPage.ID) or (PageID = IntervalPage.ID) or
       (PageID = AlertWindowPage.ID) or (PageID = ServersPage.ID) or
       (PageID = ServicesPage.ID) or (PageID = EmailAuthPage.ID) or
       (PageID = SmtpPage.ID) or (PageID = OAuth2Page.ID) or
       (PageID = EmailAddrPage.ID) then
      Result := True;
    Exit;
  end;
  if PageID = SmtpPage.ID then
    Result := EmailAuthPage.Values[1]; // skip SMTP if OAuth2 selected
  if PageID = OAuth2Page.ID then
    Result := EmailAuthPage.Values[0]; // skip OAuth2 if SMTP selected
  if PageID = EmailAddrPage.ID then
    Result := True; // always skip — addresses collected per-method above
end;

function GetMetricsPort(Param: String): String;
begin Result := MetricsPortPage.Values[0]; end;

function GetInterval(Param: String): String;
begin Result := IntervalPage.Values[0]; end;

function GetAlertWindow(Param: String): String;
begin Result := AlertWindowPage.Values[0]; end;

function GetServers(Param: String): String;
begin Result := ServersPage.Values[0]; end;

function GetServices(Param: String): String;
begin Result := ServicesPage.Values[0]; end;

function GetAuthMethod(Param: String): String;
begin
  if EmailAuthPage.Values[1] then Result := 'OAUTH2'
  else Result := 'SMTP';
end;

function GetSmtpHost(Param: String): String;
begin Result := SmtpPage.Values[0]; end;

function GetSmtpPort(Param: String): String;
begin Result := SmtpPage.Values[1]; end;

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

function GetOAuthTenant(Param: String): String;
begin Result := OAuth2Page.Values[0]; end;

function GetOAuthClientId(Param: String): String;
begin Result := OAuth2Page.Values[1]; end;

function GetOAuthClientSecret(Param: String): String;
begin Result := OAuth2Page.Values[2]; end;

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
begin
  AuthMethod := GetAuthMethod('');
  EmailFile := ExpandConstant('{app}\monitoring-services\{#MonitorDir}\email.properties');
  AppFile   := ExpandConstant('{app}\monitoring-services\{#MonitorDir}\winservicemonitor.properties');

  EmailProps := TStringList.Create;
  try
    EmailProps.Add('mail.auth.method=' + AuthMethod);
    if AuthMethod = 'SMTP' then begin
      EmailProps.Add('mail.smtp.host=' + GetSmtpHost(''));
      EmailProps.Add('mail.smtp.port=' + GetSmtpPort(''));
      EmailProps.Add('mail.smtp.auth=false');
      EmailProps.Add('mail.smtp.starttls.enable=false');
    end else begin
      EmailProps.Add('mail.smtp.host=');
      EmailProps.Add('mail.smtp.port=25');
      EmailProps.Add('mail.oauth2.tenant.id=' + GetOAuthTenant(''));
      EmailProps.Add('mail.oauth2.client.id=' + GetOAuthClientId(''));
      EmailProps.Add('mail.oauth2.client.secret=' + GetOAuthClientSecret(''));
      EmailProps.Add('mail.oauth2.token.url=');
      EmailProps.Add('mail.oauth2.graph.mail.url=' + GetGraphMailUrl(''));
    end;
    EmailProps.Add('mail.from=' + GetEmailFrom(''));
    EmailProps.Add('mail.to=' + GetEmailTo(''));
    EmailProps.Add('mail.bcc=' + GetEmailBcc(''));
    EmailProps.Add('mail.importance=High');
    EmailProps.Add('log.level=INFO');
    EmailProps.Add('log.folder=logs');
    EmailProps.Add('log.retention.days=30');
    EmailProps.Add('log.purge.interval.hours=24');
    EmailProps.SaveToFile(EmailFile);
  finally
    EmailProps.Free;
  end;

  AppProps := TStringList.Create;
  try
    AppProps.Add('monitor.servers=' + GetServers(''));
    AppProps.Add('monitor.services=' + GetServices(''));
    AppProps.Add('monitor.interval.seconds=' + GetInterval(''));
    AppProps.Add('monitor.alert.window.size=' + GetAlertWindow(''));
    AppProps.Add('metrics.port=' + GetMetricsPort(''));
    AppProps.Add('monitor.poll.threads=5');
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
    Xml.Add('  <name>Island Pacific Win Service Monitor</name>');
    Xml.Add('  <description>Monitors Windows services and exposes Prometheus metrics</description>');
    Xml.Add('  <executable>java</executable>');
    Xml.Add('  <arguments>-jar "' + AppDir + '\{#JarFile}" "' + AppDir + '\winservicemonitor.properties" "' + AppDir + '\email.properties"</arguments>');
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
  ServiceExe: String;
  ResultCode: Integer;
begin
  if CurStep = ssInstall then begin
    // Before files are copied: stop the running service so the JAR is not locked
    ServiceExe := ExpandConstant('{app}\services\{#ServiceName}.exe');
    if FileExists(ServiceExe) then begin
      Exec(ServiceExe, 'stop', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
      Sleep(2000); // give the JVM time to exit cleanly
    end;
  end;

  if CurStep = ssPostInstall then begin
    ServiceExe := ExpandConstant('{app}\services\{#ServiceName}.exe');

    WriteServiceXml;

    if IsUpgrade then begin
      // Upgrade: service already registered — just restart it with the new JAR
      Exec(ServiceExe, 'start', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
      if ResultCode <> 0 then
        MsgBox('Warning: Could not restart the Windows service. Please start it manually from services.msc.', mbInformation, MB_OK);
    end else begin
      // Fresh install: write config then register and start the service
      WritePropertiesFiles;
      if not Exec(ServiceExe, 'install', '', SW_HIDE, ewWaitUntilTerminated, ResultCode) then
        MsgBox('Warning: Could not install the Windows service. You may need to run "' + ServiceExe + ' install" manually.', mbInformation, MB_OK);
      if not Exec(ServiceExe, 'start', '', SW_HIDE, ewWaitUntilTerminated, ResultCode) then
        MsgBox('Warning: Could not start the Windows service. You may need to start it manually from services.msc.', mbInformation, MB_OK);
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
