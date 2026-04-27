#define AppName "Island Pacific Service Scheduler"
#define AppVersion "1.0.0"
#define AppPublisher "Island Pacific Retail Systems"
#define ServiceName "IPMonitoring_ServiceScheduler"
#define MonitorDir "ServiceScheduler"
#define JarFile "ServiceScheduler.jar"

[Setup]
AppId={{IP-ServiceScheduler}}
AppName={#AppName}
AppVersion={#AppVersion}
AppPublisher={#AppPublisher}
AppVerName={#AppName} v{#AppVersion}
AppCopyright=Copyright © 2025 Island Pacific Retail Systems
AppSupportURL=https://www.islandpacific.com/
VersionInfoVersion=1.0.0.0
VersionInfoCompany=Island Pacific Retail Systems
VersionInfoProductName={#AppName}
VersionInfoDescription=Island Pacific Service Scheduler

ArchitecturesInstallIn64BitMode=x64compatible
DefaultDirName={commonpf}\Island Pacific\ServiceScheduler
DisableDirPage=no
DisableProgramGroupPage=yes
PrivilegesRequired=admin
WizardStyle=modern
WizardSizePercent=125
WizardImageFile=installer\resources\wizard_modern.bmp
WizardSmallImageFile=installer\resources\wizard_small_modern.bmp
SetupIconFile=installer\resources\ip-monitoring.ico
LicenseFile=installer\resources\license.txt

AppMutex=IPMonitoring_ServiceScheduler_Mutex
CloseApplications=no

Compression=lzma
SolidCompression=yes

OutputDir=.\installer\output
OutputBaseFilename=ServiceSchedulerSetup

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Dirs]
Name: "{app}\services"
Name: "{app}\monitoring-services\{#MonitorDir}"
Name: "{app}\monitoring-services\{#MonitorDir}\screenshots"
Name: "{app}\logs"; Flags: uninsneveruninstall

[Files]
; WinSW wrapper
Source: "installer\resources\WinSW.exe"; DestDir: "{app}\services"; DestName: "{#ServiceName}.exe"; Flags: ignoreversion

; JAR
Source: "installer\resources\monitoring-services\{#MonitorDir}\{#JarFile}"; DestDir: "{app}\monitoring-services\{#MonitorDir}"; Flags: ignoreversion

; Properties files — preserved on upgrade
Source: "installer\resources\monitoring-services\{#MonitorDir}\servicescheduler.properties"; DestDir: "{app}\monitoring-services\{#MonitorDir}"; Flags: onlyifdoesntexist skipifsourcedoesntexist
Source: "installer\resources\monitoring-services\{#MonitorDir}\email.properties"; DestDir: "{app}\monitoring-services\{#MonitorDir}"; Flags: onlyifdoesntexist skipifsourcedoesntexist

[Code]
function IsUpgrade: Boolean;
var
  EmailFile: String;
begin
  EmailFile := ExpandConstant('{commonpf}\Island Pacific\ServiceScheduler\monitoring-services\{#MonitorDir}\email.properties');
  Result := FileExists(EmailFile);
end;

var
  // Email auth
  EmailAuthPage:    TInputOptionWizardPage;
  SmtpPage:         TInputQueryWizardPage;
  OAuth2Page:       TInputQueryWizardPage;
  // Job 1
  Job1ServerPage:   TInputQueryWizardPage;  // [0]=server [1]=label
  Job1CredPage:     TInputQueryWizardPage;  // [0]=username [1]=password
  Job1SvcPage:      TInputQueryWizardPage;  // [0]=service name
  Job1UrlPage:      TInputQueryWizardPage;  // [0]=url
  Job1TimesPage:    TInputQueryWizardPage;  // [0]=stop time [1]=start time
  Job1SchedPage:    TInputOptionWizardPage; // schedule type

procedure InitializeWizard;
begin
  // ── Email auth ──────────────────────────────────────────────────────────────
  EmailAuthPage := CreateInputOptionPage(wpSelectDir,
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

  // ── Job 1 config ────────────────────────────────────────────────────────────
  Job1ServerPage := CreateInputQueryPage(OAuth2Page.ID,
    'Job 1 — Server', 'Target server for the scheduled job',
    'Enter the server hostname or IP and a display label for this job:');
  Job1ServerPage.Add('Server (hostname or IP):', False);
  Job1ServerPage.Add('Job label (used in emails):', False);

  Job1CredPage := CreateInputQueryPage(Job1ServerPage.ID,
    'Job 1 — Credentials', 'Windows account for PSRemoting',
    'Enter the Windows account used to stop/start the remote service (DOMAIN\user):');
  Job1CredPage.Add('Username (DOMAIN\user):', False);
  Job1CredPage.Add('Password:', True);

  Job1SvcPage := CreateInputQueryPage(Job1CredPage.ID,
    'Job 1 — Service Name', 'Windows service to restart',
    'Enter the exact Windows service name (as shown in services.msc or sc query):');
  Job1SvcPage.Add('Service name:', False);

  Job1UrlPage := CreateInputQueryPage(Job1SvcPage.ID,
    'Job 1 — Health URL', 'URL to poll after service restart',
    'Enter the URL to poll after the service starts (used to confirm it is ready):');
  Job1UrlPage.Add('URL:', False);
  Job1UrlPage.Values[0] := 'http://';

  Job1TimesPage := CreateInputQueryPage(Job1UrlPage.ID,
    'Job 1 — Schedule Times', 'Stop and start times (HH:mm, 24-hour)',
    'Enter the time to stop the service and the time to start it again:');
  Job1TimesPage.Add('Stop time  (HH:mm):', False);
  Job1TimesPage.Add('Start time (HH:mm):', False);

  Job1SchedPage := CreateInputOptionPage(Job1TimesPage.ID,
    'Job 1 — Schedule Type', 'How often to run this job',
    'Select the schedule frequency:',
    True, False);
  Job1SchedPage.Add('Daily');
  Job1SchedPage.Add('Daily except Saturday');
  Job1SchedPage.Add('Daily except Sunday');
  Job1SchedPage.Add('Weekly (every Monday)');
  Job1SchedPage.Add('Monthly (2nd Sunday)');
  Job1SchedPage.Values[0] := True;
end;

function ShouldSkipPage(PageID: Integer): Boolean;
begin
  Result := False;
  if IsUpgrade then begin
    // Skip all config pages on upgrade — properties files are preserved
    if (PageID = EmailAuthPage.ID) or (PageID = SmtpPage.ID) or
       (PageID = OAuth2Page.ID) or (PageID = Job1ServerPage.ID) or
       (PageID = Job1CredPage.ID) or (PageID = Job1SvcPage.ID) or
       (PageID = Job1UrlPage.ID) or (PageID = Job1TimesPage.ID) or
       (PageID = Job1SchedPage.ID) then
      Result := True;
    Exit;
  end;
  if PageID = SmtpPage.ID then
    Result := EmailAuthPage.Values[1]; // skip SMTP if OAuth2 selected
  if PageID = OAuth2Page.ID then
    Result := EmailAuthPage.Values[0]; // skip OAuth2 if SMTP selected
end;

function NextButtonClick(CurPageID: Integer): Boolean;
var
  T: String;
begin
  Result := True;
  if CurPageID = SmtpPage.ID then begin
    if Trim(SmtpPage.Values[0]) = '' then begin
      MsgBox('Please enter the SMTP host.', mbError, MB_OK);
      Result := False; Exit;
    end;
    if Trim(SmtpPage.Values[2]) = '' then begin
      MsgBox('Please enter the From address.', mbError, MB_OK);
      Result := False; Exit;
    end;
    if Trim(SmtpPage.Values[3]) = '' then begin
      MsgBox('Please enter at least one To address.', mbError, MB_OK);
      Result := False; Exit;
    end;
  end;
  if CurPageID = OAuth2Page.ID then begin
    if Trim(OAuth2Page.Values[0]) = '' then begin
      MsgBox('Please enter the Tenant ID.', mbError, MB_OK);
      Result := False; Exit;
    end;
    if Trim(OAuth2Page.Values[1]) = '' then begin
      MsgBox('Please enter the Client ID.', mbError, MB_OK);
      Result := False; Exit;
    end;
    if Trim(OAuth2Page.Values[2]) = '' then begin
      MsgBox('Please enter the Client Secret.', mbError, MB_OK);
      Result := False; Exit;
    end;
    if Trim(OAuth2Page.Values[3]) = '' then begin
      MsgBox('Please enter the From address (mailbox user).', mbError, MB_OK);
      Result := False; Exit;
    end;
    if Trim(OAuth2Page.Values[4]) = '' then begin
      MsgBox('Please enter at least one To address.', mbError, MB_OK);
      Result := False; Exit;
    end;
  end;
  if CurPageID = Job1ServerPage.ID then begin
    if Trim(Job1ServerPage.Values[0]) = '' then begin
      MsgBox('Please enter the server hostname or IP.', mbError, MB_OK);
      Result := False; Exit;
    end;
    if Trim(Job1ServerPage.Values[1]) = '' then begin
      MsgBox('Please enter a label for this job.', mbError, MB_OK);
      Result := False; Exit;
    end;
  end;
  if CurPageID = Job1SvcPage.ID then begin
    if Trim(Job1SvcPage.Values[0]) = '' then begin
      MsgBox('Please enter the Windows service name.', mbError, MB_OK);
      Result := False; Exit;
    end;
  end;
  if CurPageID = Job1UrlPage.ID then begin
    T := Trim(Job1UrlPage.Values[0]);
    if (T = '') or (T = 'http://') or (T = 'https://') then begin
      MsgBox('Please enter a valid URL for the health check.', mbError, MB_OK);
      Result := False; Exit;
    end;
  end;
  if CurPageID = Job1TimesPage.ID then begin
    if Trim(Job1TimesPage.Values[0]) = '' then begin
      MsgBox('Please enter a stop time (HH:mm).', mbError, MB_OK);
      Result := False; Exit;
    end;
    if Trim(Job1TimesPage.Values[1]) = '' then begin
      MsgBox('Please enter a start time (HH:mm).', mbError, MB_OK);
      Result := False; Exit;
    end;
  end;
end;

function GetEmailFrom: String;
begin
  if EmailAuthPage.Values[1] then Result := OAuth2Page.Values[3]
  else Result := SmtpPage.Values[2];
end;

function GetEmailTo: String;
begin
  if EmailAuthPage.Values[1] then Result := OAuth2Page.Values[4]
  else Result := SmtpPage.Values[3];
end;

function GetEmailBcc: String;
begin
  if EmailAuthPage.Values[1] then Result := OAuth2Page.Values[5]
  else Result := SmtpPage.Values[4];
end;

function GetScheduleString: String;
begin
  if Job1SchedPage.Values[0] then Result := 'DAILY'
  else if Job1SchedPage.Values[1] then Result := 'DAILY_EXCEPT_SATURDAY'
  else if Job1SchedPage.Values[2] then Result := 'DAILY_EXCEPT_SUNDAY'
  else if Job1SchedPage.Values[3] then Result := 'WEEKLY'
  else if Job1SchedPage.Values[4] then Result := 'MONTHLY_NTH_WEEKDAY'
  else Result := 'DAILY';
end;

procedure WriteEmailProps;
var
  P: TStringList;
  DestFile: String;
begin
  DestFile := ExpandConstant('{app}\monitoring-services\{#MonitorDir}\email.properties');
  P := TStringList.Create;
  try
    if EmailAuthPage.Values[1] then begin
      P.Add('mail.auth.method=OAUTH2');
      P.Add('mail.smtp.host=');
      P.Add('mail.smtp.port=25');
      P.Add('mail.oauth2.tenant.id='    + OAuth2Page.Values[0]);
      P.Add('mail.oauth2.client.id='    + OAuth2Page.Values[1]);
      P.Add('mail.oauth2.client.secret=' + OAuth2Page.Values[2]);
      P.Add('mail.oauth2.token.url=');
      P.Add('mail.oauth2.graph.mail.url=https://graph.microsoft.com/v1.0/users/' + OAuth2Page.Values[3] + '/sendMail');
    end else begin
      P.Add('mail.auth.method=SMTP');
      P.Add('mail.smtp.host=' + SmtpPage.Values[0]);
      P.Add('mail.smtp.port=' + SmtpPage.Values[1]);
      P.Add('mail.smtp.auth=false');
      P.Add('mail.smtp.starttls.enable=false');
      P.Add('mail.oauth2.tenant.id=');
      P.Add('mail.oauth2.client.id=');
      P.Add('mail.oauth2.client.secret=');
      P.Add('mail.oauth2.token.url=');
      P.Add('mail.oauth2.graph.mail.url=');
    end;
    P.Add('mail.from='       + GetEmailFrom);
    P.Add('mail.to='         + GetEmailTo);
    P.Add('mail.bcc='        + GetEmailBcc);
    P.Add('mail.importance=High');
    P.Add('log.level=INFO');
    P.Add('log.folder=logs');
    P.Add('log.retention.days=30');
    P.Add('log.purge.interval.hours=24');
    P.SaveToFile(DestFile);
  finally
    P.Free;
  end;
end;

procedure WriteSchedulerProps;
var
  P: TStringList;
  AppDir, SchedStr: String;
begin
  AppDir   := ExpandConstant('{app}\monitoring-services\{#MonitorDir}');
  SchedStr := GetScheduleString;
  P := TStringList.Create;
  try
    // ── File header ─────────────────────────────────────────────────────────
    P.Add('# ===============================');
    P.Add('# Service Scheduler Configuration');
    P.Add('# ===============================');
    P.Add('');
    P.Add('# Where to save screenshots');
    P.Add('screenshot.folder=screenshots');
    P.Add('');
    P.Add('# -----------------------------------------------------------------------');
    P.Add('# HOW THIS SERVICE WORKS');
    P.Add('# -----------------------------------------------------------------------');
    P.Add('# The Service Scheduler automatically stops and restarts Windows services');
    P.Add('# on a timed schedule. For each job it will:');
    P.Add('#');
    P.Add('#   1. Wait until stop.time  — then stop the Windows service on the');
    P.Add('#                              remote server via PowerShell (PSRemoting)');
    P.Add('#   2. Wait until start.time — then start the service again');
    P.Add('#   3. Poll the health URL   — every poll.interval.seconds until the');
    P.Add('#                              application responds or poll.timeout.seconds');
    P.Add('#                              is reached');
    P.Add('#   4. Take a screenshot     — captures the URL in a browser once ready');
    P.Add('#   5. Send an email alert   — with the screenshot attached, confirming');
    P.Add('#                              the service restarted successfully (or');
    P.Add('#                              reporting a timeout if it did not)');
    P.Add('#');
    P.Add('# Job 1 was configured by the installer wizard.');
    P.Add('# To add more jobs, copy a job block below, increment the job number,');
    P.Add('# fill in all fields, then restart this Windows service for changes');
    P.Add('# to take effect.');
    P.Add('#');
    P.Add('# -----------------------------------------------------------------------');
    P.Add('# Job field reference:');
    P.Add('#   label               — display name (used in emails and filenames)');
    P.Add('#   server              — hostname or IP of the application server');
    P.Add('#   username            — Windows account for PSRemoting (DOMAIN\user)');
    P.Add('#   password            — password for the above account');
    P.Add('#   service.name        — exact Windows service name (sc query <name>)');
    P.Add('#   url                 — URL to poll and screenshot after restart');
    P.Add('#   stop.time           — HH:mm time to stop the service (24-hour clock)');
    P.Add('#   start.time          — HH:mm time to start the service (must be after stop.time)');
    P.Add('#   schedule            — DAILY | DAILY_EXCEPT_SATURDAY | DAILY_EXCEPT_SUNDAY | WEEKLY | MONTHLY_NTH_WEEKDAY');
    P.Add('#   schedule.day        — (WEEKLY only) MONDAY..SUNDAY');
    P.Add('#   schedule.nth        — (MONTHLY_NTH_WEEKDAY only) 1=first, 2=second, 3=third, 4=fourth');
    P.Add('#   schedule.weekday    — (MONTHLY_NTH_WEEKDAY only) MONDAY..SUNDAY');
    P.Add('#   poll.interval.seconds — how often to check the URL (default 10)');
    P.Add('#   poll.timeout.seconds  — max wait for URL to become ready (default 120)');
    P.Add('# -----------------------------------------------------------------------');
    P.Add('');
    // ── Job 1 — written from wizard input ───────────────────────────────────
    P.Add('# Job 1: ' + Trim(Job1ServerPage.Values[1]) + ' — configured during install');
    P.Add('job.1.label='        + Trim(Job1ServerPage.Values[1]));
    P.Add('job.1.server='       + Trim(Job1ServerPage.Values[0]));
    P.Add('job.1.username='     + Trim(Job1CredPage.Values[0]));
    P.Add('job.1.password='     + Trim(Job1CredPage.Values[1]));
    P.Add('job.1.service.name=' + Trim(Job1SvcPage.Values[0]));
    P.Add('job.1.url='          + Trim(Job1UrlPage.Values[0]));
    P.Add('job.1.stop.time='    + Trim(Job1TimesPage.Values[0]));
    P.Add('job.1.start.time='   + Trim(Job1TimesPage.Values[1]));
    P.Add('job.1.schedule='     + SchedStr);
    if SchedStr = 'WEEKLY' then
      P.Add('job.1.schedule.day=MONDAY');
    if SchedStr = 'MONTHLY_NTH_WEEKDAY' then begin
      P.Add('job.1.schedule.nth=2');
      P.Add('job.1.schedule.weekday=SUNDAY');
    end;
    P.Add('job.1.poll.interval.seconds=10');
    P.Add('job.1.poll.timeout.seconds=120');
    P.Add('');
    // ── Template for additional jobs ─────────────────────────────────────────
    P.Add('# -----------------------------------------------------------------------');
    P.Add('# Add more jobs below. Copy this block, change job.N to the next number,');
    P.Add('# and fill in all fields. Restart the Windows service when done.');
    P.Add('# -----------------------------------------------------------------------');
    P.Add('');
    P.Add('# Job 2: <label>');
    P.Add('#job.2.label=');
    P.Add('#job.2.server=');
    P.Add('#job.2.username=');
    P.Add('#job.2.password=');
    P.Add('#job.2.service.name=');
    P.Add('#job.2.url=');
    P.Add('#job.2.stop.time=');
    P.Add('#job.2.start.time=');
    P.Add('#job.2.schedule=DAILY');
    P.Add('#job.2.poll.interval.seconds=10');
    P.Add('#job.2.poll.timeout.seconds=120');
    P.SaveToFile(AppDir + '\servicescheduler.properties');
  finally
    P.Free;
  end;
end;

procedure GrantLogonAsService(Username: String);
var
  TempFile, DbFile: String;
  Lines: TStringList;
  I: Integer;
  Found: Boolean;
begin
  TempFile := ExpandConstant('{tmp}\secedit_export.inf');
  DbFile   := ExpandConstant('{tmp}\secedit.sdb');
  // Export current policy
  Exec('secedit.exe', '/export /cfg "' + TempFile + '" /quiet', '',
       SW_HIDE, ewWaitUntilTerminated, I);
  if not FileExists(TempFile) then Exit;
  Lines := TStringList.Create;
  try
    Lines.LoadFromFile(TempFile);
    Found := False;
    for I := 0 to Lines.Count - 1 do begin
      if Pos('SeServiceLogonRight', Lines[I]) > 0 then begin
        if Pos(Username, Lines[I]) = 0 then
          Lines[I] := Lines[I] + ',' + Username;
        Found := True;
        Break;
      end;
    end;
    if not Found then
      Lines.Add('SeServiceLogonRight = ' + Username);
    Lines.SaveToFile(TempFile);
  finally
    Lines.Free;
  end;
  Exec('secedit.exe', '/import /cfg "' + TempFile + '" /db "' + DbFile + '" /overwrite /quiet', '',
       SW_HIDE, ewWaitUntilTerminated, I);
  Exec('secedit.exe', '/configure /db "' + DbFile + '" /quiet', '',
       SW_HIDE, ewWaitUntilTerminated, I);
end;

function XmlEscape(Value: String): String;
begin
  Result := Value;
  StringChangeEx(Result, '&', '&amp;', True);
  StringChangeEx(Result, '<', '&lt;', True);
  StringChangeEx(Result, '>', '&gt;', True);
  StringChangeEx(Result, '"', '&quot;', True);
  StringChangeEx(Result, '''', '&apos;', True);
end;

procedure WriteServiceXml;
var
  X: TStringList;
  InstDir, AppDir, SvcUser, SvcPass: String;
begin
  InstDir := ExpandConstant('{app}');
  AppDir  := InstDir + '\monitoring-services\{#MonitorDir}';
  // Use job 1 credentials to run the service — needed for remote WinRM access
  SvcUser := Trim(Job1CredPage.Values[0]);
  SvcPass := Trim(Job1CredPage.Values[1]);
  X := TStringList.Create;
  try
    X.Add('<service>');
    X.Add('  <id>{#ServiceName}</id>');
    X.Add('  <name>Island Pacific Service Scheduler</name>');
    X.Add('  <description>Schedules service stop/start cycles on a timed basis</description>');
    X.Add('  <executable>java</executable>');
    // SS: args[0]=email.properties  args[1]=servicescheduler.properties
    X.Add('  <arguments>-jar "' + AppDir + '\{#JarFile}" "' + AppDir + '\email.properties" "' + AppDir + '\servicescheduler.properties"</arguments>');
    if SvcUser <> '' then begin
      X.Add('  <username>' + XmlEscape(SvcUser) + '</username>');
      X.Add('  <password>' + XmlEscape(SvcPass) + '</password>');
    end;
    X.Add('  <logmode>rotate</logmode>');
    X.Add('  <log name="' + InstDir + '\logs\{#ServiceName}">');
    X.Add('    <sizeThreshold>10240</sizeThreshold>');
    X.Add('    <keepFiles>8</keepFiles>');
    X.Add('  </log>');
    X.Add('  <onfailure action="restart" delay="10 sec"/>');
    X.Add('  <onfailure action="restart" delay="20 sec"/>');
    X.Add('  <onfailure action="none"/>');
    X.Add('</service>');
    X.SaveToFile(InstDir + '\services\{#ServiceName}.xml');
  finally
    X.Free;
  end;
end;

procedure CurStepChanged(CurStep: TSetupStep);
var
  ServiceExe: String;
  ResultCode: Integer;
begin
  if CurStep = ssInstall then begin
    // Stop existing service before files are copied so the JAR is not locked
    ServiceExe := ExpandConstant('{app}\services\{#ServiceName}.exe');
    if FileExists(ServiceExe) then begin
      Exec(ServiceExe, 'stop', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
      Sleep(10000);
    end;
  end;

  if CurStep = ssPostInstall then begin
    ServiceExe := ExpandConstant('{app}\services\{#ServiceName}.exe');
    WriteServiceXml;

    if IsUpgrade then begin
      // Upgrade: already registered — restart with the new JAR
      Exec(ServiceExe, 'start', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
      if ResultCode <> 0 then
        MsgBox('Warning: Could not restart the Windows service. Please start it manually from services.msc.',
               mbInformation, MB_OK);
    end else begin
      // Fresh install: write config, grant logon right, register
      WriteEmailProps;
      WriteSchedulerProps;
      if Trim(Job1CredPage.Values[0]) <> '' then
        GrantLogonAsService(Trim(Job1CredPage.Values[0]));
      if not Exec(ServiceExe, 'install', '', SW_HIDE, ewWaitUntilTerminated, ResultCode) then
        MsgBox('Warning: Could not register the Windows service.' + #13#10 +
               'Run "' + ServiceExe + ' install" manually.', mbInformation, MB_OK)
      else
        MsgBox('Service Scheduler installed and registered.' + #13#10 + #13#10 +
               'The service has NOT been started automatically.' + #13#10 +
               'Review job configuration at:' + #13#10 +
               ExpandConstant('{app}\monitoring-services\{#MonitorDir}\servicescheduler.properties') + #13#10 + #13#10 +
               'When ready, start it from services.msc or run:' + #13#10 +
               '"' + ServiceExe + ' start"', mbInformation, MB_OK);
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
    Exec(ServiceExe, 'stop',      '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
    Exec(ServiceExe, 'uninstall', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
  end;
end;

[UninstallDelete]
Type: files;      Name: "{app}\services\*.xml"
Type: files;      Name: "{app}\monitoring-services\{#MonitorDir}\email.properties"
Type: files;      Name: "{app}\monitoring-services\{#MonitorDir}\servicescheduler.properties"
Type: dirifempty; Name: "{app}\services"
Type: dirifempty; Name: "{app}\monitoring-services\{#MonitorDir}"
Type: dirifempty; Name: "{app}\monitoring-services"
