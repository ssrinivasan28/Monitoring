#define AppName "Island Pacific Agent Suite"
#define AppVersion "1.0.0"
#define AppPublisher "Island Pacific Retail Systems"
#define SvcWSM "IPMonitoring_WinServiceMonitor"
#define SvcSS  "IPMonitoring_ServiceScheduler"
#define DirWSM "WinServiceMonitor"
#define DirSS  "ServiceScheduler"
#define JarWSM "WinServiceMonitor.jar"
#define JarSS  "ServiceScheduler.jar"

[Setup]
AppId={{IP-AgentSuite}}
AppName={#AppName}
AppVersion={#AppVersion}
AppPublisher={#AppPublisher}
AppVerName={#AppName} v{#AppVersion}
AppCopyright=Copyright © 2025 Island Pacific Retail Systems
AppSupportURL=https://www.islandpacific.com/
VersionInfoVersion=1.0.0.0
VersionInfoCompany=Island Pacific Retail Systems
VersionInfoProductName={#AppName}
VersionInfoDescription=Island Pacific Agent Suite Installer

ArchitecturesInstallIn64BitMode=x64compatible
DefaultDirName={commonpf}\Island Pacific\Agent Suite
DisableDirPage=no
DisableProgramGroupPage=yes
PrivilegesRequired=admin
WizardStyle=modern
WizardSizePercent=125
WizardImageFile=installer\resources\wizard_modern.bmp
WizardSmallImageFile=installer\resources\wizard_small_modern.bmp
SetupIconFile=installer\resources\ip-monitoring.ico
LicenseFile=installer\resources\license.txt

AppMutex=IPAgentSuite_Mutex
CloseApplications=no

Compression=lzma
SolidCompression=yes

OutputDir=.\installer\output
OutputBaseFilename=IPAgentSuiteSetup

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Messages]
WelcomeLabel2=This wizard will install the Island Pacific Agent Suite.%n%nAvailable services:%n%n  • Win Service Monitor — monitors Windows service states%n  • Service Scheduler — schedules service restarts on a timed basis%n%nSelect which services to install on the next page.

[Dirs]
Name: "{app}\services"
Name: "{app}\logs"; Flags: uninsneveruninstall
Name: "{app}\monitoring-services\{#DirWSM}"; Check: InstallWSM
Name: "{app}\monitoring-services\{#DirSS}";  Check: InstallSS

[Files]
; WinSW wrappers — always copied for selected services
Source: "installer\resources\WinSW.exe"; DestDir: "{app}\services"; DestName: "{#SvcWSM}.exe"; Flags: ignoreversion; Check: InstallWSM
Source: "installer\resources\WinSW.exe"; DestDir: "{app}\services"; DestName: "{#SvcSS}.exe";  Flags: ignoreversion; Check: InstallSS

; JARs
Source: "installer\resources\monitoring-services\{#DirWSM}\{#JarWSM}"; DestDir: "{app}\monitoring-services\{#DirWSM}"; Flags: ignoreversion; Check: InstallWSM
Source: "installer\resources\monitoring-services\{#DirSS}\{#JarSS}";   DestDir: "{app}\monitoring-services\{#DirSS}";  Flags: ignoreversion; Check: InstallSS

; Properties — onlyifdoesntexist preserves config on upgrade
Source: "installer\resources\monitoring-services\{#DirWSM}\winservicemonitor.properties"; DestDir: "{app}\monitoring-services\{#DirWSM}"; Flags: onlyifdoesntexist skipifsourcedoesntexist; Check: InstallWSM
Source: "installer\resources\monitoring-services\{#DirWSM}\email.properties";             DestDir: "{app}\monitoring-services\{#DirWSM}"; Flags: onlyifdoesntexist skipifsourcedoesntexist; Check: InstallWSM
Source: "installer\resources\monitoring-services\{#DirSS}\servicescheduler.properties";   DestDir: "{app}\monitoring-services\{#DirSS}";  Flags: onlyifdoesntexist skipifsourcedoesntexist; Check: InstallSS
Source: "installer\resources\monitoring-services\{#DirSS}\email.properties";              DestDir: "{app}\monitoring-services\{#DirSS}";  Flags: onlyifdoesntexist skipifsourcedoesntexist; Check: InstallSS

[Code]

// ─── State ────────────────────────────────────────────────────────────────────

var
  // Cached install-state flags — set once in InitializeWizard
  WsmWasInstalled: Boolean;
  SsWasInstalled:  Boolean;

  // Service selection page
  ServiceSelPage: TInputOptionWizardPage;

  // WinServiceMonitor config pages
  WsmServersPage:     TInputQueryWizardPage;
  WsmCredPage:        TInputQueryWizardPage;
  WsmServicesPage:    TInputQueryWizardPage;
  WsmIntervalPage:    TInputQueryWizardPage;
  WsmAlertPage:       TInputQueryWizardPage;
  WsmPortPage:        TInputQueryWizardPage;

  // ServiceScheduler Job 1 pages
  SsJob1ServerPage:   TInputQueryWizardPage;
  SsJob1CredPage:     TInputQueryWizardPage;
  SsJob1SvcPage:      TInputQueryWizardPage;
  SsJob1UrlPage:      TInputQueryWizardPage;
  SsJob1TimesPage:    TInputQueryWizardPage;
  SsJob1SchedPage:    TInputOptionWizardPage;
  SsScreenshotPage:   TInputQueryWizardPage;

  // Shared email pages
  EmailAuthPage:  TInputOptionWizardPage;
  SmtpPage:       TInputQueryWizardPage;
  OAuth2Page:     TInputQueryWizardPage;

// ─── Helpers ──────────────────────────────────────────────────────────────────

// Use registry to detect existing installs — {app} is not valid until after directory selection
// GetInstallDir reads InstallLocation from the uninstall registry key.
// Checks HKLM64 first (native 64-bit view), falls back to HKLM32 (WOW node).
function GetInstallDir: String;
var
  Dir: String;
begin
  Dir := '';
  if not RegQueryStringValue(HKLM64, 'SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\{IP-AgentSuite}_is1',
      'InstallLocation', Dir) then
    RegQueryStringValue(HKLM, 'SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\{IP-AgentSuite}_is1',
      'InstallLocation', Dir);
  Result := Dir; // always ends with backslash when set by Inno Setup
end;

function IsWSMInstalled: Boolean;
var
  InstallDir: String;
begin
  InstallDir := GetInstallDir;
  Result := (InstallDir <> '') and FileExists(InstallDir + 'services\{#SvcWSM}.exe');
end;

function IsSSInstalled: Boolean;
var
  InstallDir: String;
begin
  InstallDir := GetInstallDir;
  Result := (InstallDir <> '') and FileExists(InstallDir + 'services\{#SvcSS}.exe');
end;

// Whether the checkbox for each service is ticked
function WantWSM: Boolean;
begin
  Result := (ServiceSelPage <> nil) and ServiceSelPage.Values[0];
end;

function WantSS: Boolean;
begin
  Result := (ServiceSelPage <> nil) and ServiceSelPage.Values[1];
end;

// True if this is a fresh install of the service (not an upgrade)
// Uses cached flags set at InitializeWizard time — avoids repeated registry hits
function WsmIsFresh: Boolean;
begin
  Result := WantWSM and not WsmWasInstalled;
end;

function SsIsFresh: Boolean;
begin
  Result := WantSS and not SsWasInstalled;
end;

// Check functions used by [Files] section — guard against nil before wizard pages exist
function InstallWSM: Boolean;
begin
  Result := (ServiceSelPage <> nil) and ServiceSelPage.Values[0];
end;

function InstallSS: Boolean;
begin
  Result := (ServiceSelPage <> nil) and ServiceSelPage.Values[1];
end;

// Read a value from an existing properties file (returns '' if not found)
function ReadPropValue(FilePath, Key: String): String;
var
  Lines: TArrayOfString;
  I, EqPos: Integer;
  Line, K, V: String;
begin
  Result := '';
  if not LoadStringsFromFile(FilePath, Lines) then Exit;
  for I := 0 to GetArrayLength(Lines) - 1 do begin
    Line := Trim(Lines[I]);
    if (Length(Line) = 0) or (Line[1] = '#') then Continue;
    EqPos := Pos('=', Line);
    if EqPos = 0 then Continue;  // skip lines with no '='
    K := Trim(Copy(Line, 1, EqPos - 1));
    V := Trim(Copy(Line, EqPos + 1, MaxInt));
    if CompareText(K, Key) = 0 then begin
      Result := V;
      Exit;
    end;
  end;
end;

// Find any existing email.properties (from either service) to pre-fill email pages
procedure PreFillEmailFromExisting;
var
  F, InstallDir: String;
begin
  InstallDir := GetInstallDir;
  if InstallDir = '' then Exit;

  F := InstallDir + 'monitoring-services\{#DirWSM}\email.properties';
  if not FileExists(F) then
    F := InstallDir + 'monitoring-services\{#DirSS}\email.properties';
  if not FileExists(F) then Exit;

  if CompareText(ReadPropValue(F, 'mail.auth.method'), 'OAUTH2') = 0 then begin
    EmailAuthPage.Values[0] := False;
    EmailAuthPage.Values[1] := True;
    OAuth2Page.Values[0] := ReadPropValue(F, 'mail.oauth2.tenant.id');
    OAuth2Page.Values[1] := ReadPropValue(F, 'mail.oauth2.client.id');
    OAuth2Page.Values[2] := ReadPropValue(F, 'mail.oauth2.client.secret');
    OAuth2Page.Values[3] := ReadPropValue(F, 'mail.from');
    OAuth2Page.Values[4] := ReadPropValue(F, 'mail.to');
    OAuth2Page.Values[5] := ReadPropValue(F, 'mail.bcc');
  end else begin
    EmailAuthPage.Values[0] := True;
    EmailAuthPage.Values[1] := False;
    SmtpPage.Values[0] := ReadPropValue(F, 'mail.smtp.host');
    SmtpPage.Values[1] := ReadPropValue(F, 'mail.smtp.port');
    SmtpPage.Values[2] := ReadPropValue(F, 'mail.from');
    SmtpPage.Values[3] := ReadPropValue(F, 'mail.to');
    SmtpPage.Values[4] := ReadPropValue(F, 'mail.bcc');
  end;
end;

// ─── Wizard Initialisation ────────────────────────────────────────────────────

procedure InitializeWizard;
begin
  // Cache install state once — IsWSMInstalled/IsSSInstalled do registry lookups
  WsmWasInstalled := IsWSMInstalled;
  SsWasInstalled  := IsSSInstalled;

  // ── Service selection ──────────────────────────────────────────────────────
  ServiceSelPage := CreateInputOptionPage(wpWelcome,
    'Select Services', 'Choose which services to install',
    'Select one or both services. On upgrade, already-installed services will be updated. Unticked services already installed will not be touched.',
    False, False);
  ServiceSelPage.Add('Win Service Monitor  (monitors Windows service states and alerts when stopped)');
  ServiceSelPage.Add('Service Scheduler  (stops and restarts services on a configured schedule)');
  ServiceSelPage.Values[0] := True;
  ServiceSelPage.Values[1] := True;

  // ── WinServiceMonitor config ───────────────────────────────────────────────
  WsmServersPage := CreateInputQueryPage(ServiceSelPage.ID,
    'Win Service Monitor — Servers',
    'Which servers should be monitored?',
    'Enter server hostnames or IPs (comma-separated). Use "localhost" for this machine:');
  WsmServersPage.Add('Servers:', False);
  WsmServersPage.Values[0] := 'localhost';

  WsmCredPage := CreateInputQueryPage(WsmServersPage.ID,
    'Win Service Monitor — Server Credentials',
    'Remote server login (for the first server)',
    'Enter the Windows account used to connect to the monitored server(s). ' +
    'Format: DOMAIN\username. Leave blank if monitoring localhost only.');
  WsmCredPage.Add('Username (DOMAIN\user):', False);
  WsmCredPage.Add('Password:', True);

  WsmServicesPage := CreateInputQueryPage(WsmCredPage.ID,
    'Win Service Monitor — Services to Watch',
    'Which Windows services should trigger an alert if stopped?',
    'Enter exact service names as shown in services.msc (comma-separated):');
  WsmServicesPage.Add('Service names:', False);
  WsmServicesPage.Values[0] := '';

  WsmIntervalPage := CreateInputQueryPage(WsmServicesPage.ID,
    'Win Service Monitor — Poll Interval',
    'How often to check service states',
    'Enter the polling interval in seconds (e.g. 60):');
  WsmIntervalPage.Add('Interval (seconds):', False);
  WsmIntervalPage.Values[0] := '60';

  WsmAlertPage := CreateInputQueryPage(WsmIntervalPage.ID,
    'Win Service Monitor — Alert Window',
    'Consecutive failures before alerting',
    'Number of consecutive down-cycles before an email is sent (avoids false alarms from brief restarts):');
  WsmAlertPage.Add('Alert window size:', False);
  WsmAlertPage.Values[0] := '2';

  WsmPortPage := CreateInputQueryPage(WsmAlertPage.ID,
    'Win Service Monitor — Metrics Port',
    'Prometheus metrics endpoint port',
    'Port this service will expose /metrics on:');
  WsmPortPage.Add('Port:', False);
  WsmPortPage.Values[0] := '3026';

  // ── ServiceScheduler Job 1 ─────────────────────────────────────────────────
  SsJob1ServerPage := CreateInputQueryPage(WsmPortPage.ID,
    'Service Scheduler — Job 1: Server',
    'Target application server',
    'Enter the hostname or IP of the server where the service to be restarted runs:');
  SsJob1ServerPage.Add('Server (hostname or IP):', False);
  SsJob1ServerPage.Add('Job label (display name):', False);
  SsJob1ServerPage.Values[1] := 'Job1';

  SsJob1CredPage := CreateInputQueryPage(SsJob1ServerPage.ID,
    'Service Scheduler — Job 1: Credentials',
    'Windows account for remote PS access',
    'Enter the account used to connect to the target server via PowerShell Remoting. Format: DOMAIN\username:');
  SsJob1CredPage.Add('Username (DOMAIN\user):', False);
  SsJob1CredPage.Add('Password:', True);

  SsJob1SvcPage := CreateInputQueryPage(SsJob1CredPage.ID,
    'Service Scheduler — Job 1: Service Name',
    'Which Windows service should be restarted?',
    'Enter the exact Windows service name as shown in services.msc (sc query <name>):');
  SsJob1SvcPage.Add('Service name:', False);

  SsJob1UrlPage := CreateInputQueryPage(SsJob1SvcPage.ID,
    'Service Scheduler — Job 1: Health URL',
    'URL to poll after restart',
    'URL to check after the service restarts. The scheduler waits until this URL returns HTTP 200 before taking a screenshot and sending confirmation:');
  SsJob1UrlPage.Add('URL:', False);
  SsJob1UrlPage.Values[0] := 'http://';

  SsJob1TimesPage := CreateInputQueryPage(SsJob1UrlPage.ID,
    'Service Scheduler — Job 1: Stop & Start Times',
    'When should the service be stopped and restarted?',
    'Use 24-hour format HH:mm (e.g. 02:00). Start time must be after stop time:');
  SsJob1TimesPage.Add('Stop time  (HH:mm):', False);
  SsJob1TimesPage.Add('Start time (HH:mm):', False);
  SsJob1TimesPage.Values[0] := '02:00';
  SsJob1TimesPage.Values[1] := '02:10';

  SsJob1SchedPage := CreateInputOptionPage(SsJob1TimesPage.ID,
    'Service Scheduler — Job 1: Schedule',
    'How often should this job run?',
    'Select the recurrence pattern:',
    True, False);
  SsJob1SchedPage.Add('Daily');
  SsJob1SchedPage.Add('Daily except Saturday');
  SsJob1SchedPage.Add('Daily except Sunday');
  SsJob1SchedPage.Add('Weekly (every Monday)');
  SsJob1SchedPage.Add('Monthly — 2nd Sunday');
  SsJob1SchedPage.Values[0] := True;

  SsScreenshotPage := CreateInputQueryPage(SsJob1SchedPage.ID,
    'Service Scheduler — Screenshots',
    'Where should screenshots be saved?',
    'Folder path for browser screenshots taken after each service restart (relative or absolute):');
  SsScreenshotPage.Add('Screenshot folder:', False);
  SsScreenshotPage.Values[0] := 'screenshots';

  // ── Shared email ───────────────────────────────────────────────────────────
  EmailAuthPage := CreateInputOptionPage(SsScreenshotPage.ID,
    'Email Alerts — Authentication Method',
    'How should alert emails be sent?',
    'Select the email authentication method:',
    True, False);
  EmailAuthPage.Add('SMTP');
  EmailAuthPage.Add('OAuth2 (Microsoft 365 / Graph API)');
  EmailAuthPage.Values[0] := True;

  SmtpPage := CreateInputQueryPage(EmailAuthPage.ID,
    'Email Alerts — SMTP Settings',
    'SMTP server configuration',
    'Enter your SMTP server details:');
  SmtpPage.Add('SMTP Host:', False);
  SmtpPage.Add('SMTP Port:', False);
  SmtpPage.Add('From address:', False);
  SmtpPage.Add('To address(es) — comma-separated:', False);
  SmtpPage.Add('BCC address(es) — optional:', False);
  SmtpPage.Values[1] := '25';

  OAuth2Page := CreateInputQueryPage(EmailAuthPage.ID,
    'Email Alerts — OAuth2 / Microsoft 365',
    'Azure AD application credentials',
    'Enter your Azure AD app credentials:');
  OAuth2Page.Add('Tenant ID:', False);
  OAuth2Page.Add('Client ID:', False);
  OAuth2Page.Add('Client Secret:', True);
  OAuth2Page.Add('From address (mailbox):', False);
  OAuth2Page.Add('To address(es) — comma-separated:', False);
  OAuth2Page.Add('BCC address(es) — optional:', False);

  // Pre-fill email pages from any existing install
  PreFillEmailFromExisting;
end;

// ─── Page visibility ──────────────────────────────────────────────────────────

function ShouldSkipPage(PageID: Integer): Boolean;
var
  NeedEmail: Boolean;
begin
  Result := False;

  // Service selection never skipped
  if PageID = ServiceSelPage.ID then Exit;

  // WSM config pages — show only if WSM ticked AND fresh install
  if (PageID = WsmServersPage.ID)  or (PageID = WsmCredPage.ID) or
     (PageID = WsmServicesPage.ID) or (PageID = WsmIntervalPage.ID) or
     (PageID = WsmAlertPage.ID)    or (PageID = WsmPortPage.ID) then begin
    Result := not WsmIsFresh;
    Exit;
  end;

  // SS config pages — show only if SS ticked AND fresh install
  if (PageID = SsJob1ServerPage.ID) or (PageID = SsJob1CredPage.ID) or
     (PageID = SsJob1SvcPage.ID)    or (PageID = SsJob1UrlPage.ID)  or
     (PageID = SsJob1TimesPage.ID)  or (PageID = SsJob1SchedPage.ID) or
     (PageID = SsScreenshotPage.ID) then begin
    Result := not SsIsFresh;
    Exit;
  end;

  // Email pages — show only when at least one service is being freshly installed
  NeedEmail := WsmIsFresh or SsIsFresh;
  if PageID = EmailAuthPage.ID then begin
    Result := not NeedEmail;
    Exit;
  end;
  if PageID = SmtpPage.ID then begin
    Result := (not NeedEmail) or EmailAuthPage.Values[1];
    Exit;
  end;
  if PageID = OAuth2Page.ID then begin
    Result := (not NeedEmail) or EmailAuthPage.Values[0];
    Exit;
  end;
end;

// ─── Validation ───────────────────────────────────────────────────────────────

function NextButtonClick(CurPageID: Integer): Boolean;
begin
  Result := True;

  if CurPageID = ServiceSelPage.ID then begin
    if not WantWSM and not WantSS then begin
      MsgBox('Please select at least one service to install.', mbError, MB_OK);
      Result := False;
    end;
    Exit;
  end;

  if CurPageID = WsmServicesPage.ID then begin
    if WsmIsFresh and (Trim(WsmServicesPage.Values[0]) = '') then begin
      MsgBox('Please enter at least one service name to monitor.', mbError, MB_OK);
      Result := False;
    end;
    Exit;
  end;

  if CurPageID = SsJob1ServerPage.ID then begin
    if SsIsFresh and (Trim(SsJob1ServerPage.Values[0]) = '') then begin
      MsgBox('Please enter the target server hostname or IP.', mbError, MB_OK);
      Result := False;
    end;
    Exit;
  end;

  if CurPageID = SsJob1SvcPage.ID then begin
    if SsIsFresh and (Trim(SsJob1SvcPage.Values[0]) = '') then begin
      MsgBox('Please enter the Windows service name.', mbError, MB_OK);
      Result := False;
    end;
    Exit;
  end;

  if CurPageID = SsJob1UrlPage.ID then begin
    if SsIsFresh then begin
      if (Trim(SsJob1UrlPage.Values[0]) = '') or
         (Trim(SsJob1UrlPage.Values[0]) = 'http://') or
         (Trim(SsJob1UrlPage.Values[0]) = 'https://') then begin
        MsgBox('Please enter a valid URL to poll after the service restarts.', mbError, MB_OK);
        Result := False;
      end;
    end;
    Exit;
  end;

  if CurPageID = SsJob1TimesPage.ID then begin
    if SsIsFresh then begin
      if Trim(SsJob1TimesPage.Values[0]) = '' then begin
        MsgBox('Please enter a stop time (HH:mm).', mbError, MB_OK);
        Result := False;
        Exit;
      end;
      if Trim(SsJob1TimesPage.Values[1]) = '' then begin
        MsgBox('Please enter a start time (HH:mm).', mbError, MB_OK);
        Result := False;
      end;
    end;
    Exit;
  end;

  if CurPageID = SmtpPage.ID then begin
    if Trim(SmtpPage.Values[0]) = '' then begin
      MsgBox('Please enter the SMTP host.', mbError, MB_OK);
      Result := False;
      Exit;
    end;
    if Trim(SmtpPage.Values[2]) = '' then begin
      MsgBox('Please enter the From address.', mbError, MB_OK);
      Result := False;
      Exit;
    end;
    if Trim(SmtpPage.Values[3]) = '' then begin
      MsgBox('Please enter at least one To address.', mbError, MB_OK);
      Result := False;
    end;
    Exit;
  end;

  if CurPageID = OAuth2Page.ID then begin
    if Trim(OAuth2Page.Values[0]) = '' then begin
      MsgBox('Please enter the Tenant ID.', mbError, MB_OK);
      Result := False;
      Exit;
    end;
    if Trim(OAuth2Page.Values[1]) = '' then begin
      MsgBox('Please enter the Client ID.', mbError, MB_OK);
      Result := False;
      Exit;
    end;
    if Trim(OAuth2Page.Values[2]) = '' then begin
      MsgBox('Please enter the Client Secret.', mbError, MB_OK);
      Result := False;
      Exit;
    end;
    if Trim(OAuth2Page.Values[3]) = '' then begin
      MsgBox('Please enter the From address (mailbox).', mbError, MB_OK);
      Result := False;
      Exit;
    end;
    if Trim(OAuth2Page.Values[4]) = '' then begin
      MsgBox('Please enter at least one To address.', mbError, MB_OK);
      Result := False;
    end;
    Exit;
  end;
end;

// ─── Property file writers ────────────────────────────────────────────────────

function GetScheduleString: String;
begin
  if SsJob1SchedPage.Values[0] then Result := 'DAILY'
  else if SsJob1SchedPage.Values[1] then Result := 'DAILY_EXCEPT_SATURDAY'
  else if SsJob1SchedPage.Values[2] then Result := 'DAILY_EXCEPT_SUNDAY'
  else if SsJob1SchedPage.Values[3] then Result := 'WEEKLY'
  else if SsJob1SchedPage.Values[4] then Result := 'MONTHLY_NTH_WEEKDAY'
  else Result := 'DAILY'; // fallback — should never happen with radio buttons
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

procedure WriteEmailProps(DestFile: String);
var
  P: TStringList;
begin
  P := TStringList.Create;
  try
    if EmailAuthPage.Values[1] then begin
      P.Add('mail.auth.method=OAUTH2');
      P.Add('mail.smtp.host=');
      P.Add('mail.smtp.port=25');
      P.Add('mail.oauth2.tenant.id='   + OAuth2Page.Values[0]);
      P.Add('mail.oauth2.client.id='   + OAuth2Page.Values[1]);
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

procedure WriteWsmProps;
var
  P: TStringList;
  Servers, FirstServer, AppDir: String;
  CommaPos: Integer;
begin
  AppDir  := ExpandConstant('{app}\monitoring-services\{#DirWSM}');
  Servers := Trim(WsmServersPage.Values[0]);

  // Credential key must use only the first server name, not the whole comma list
  CommaPos := Pos(',', Servers);
  if CommaPos > 0 then
    FirstServer := Trim(Copy(Servers, 1, CommaPos - 1))
  else
    FirstServer := Servers;

  P := TStringList.Create;
  try
    P.Add('monitor.servers=' + Servers);
    P.Add('monitor.services=' + Trim(WsmServicesPage.Values[0]));
    P.Add('monitor.interval.seconds=' + Trim(WsmIntervalPage.Values[0]));
    P.Add('monitor.alert.window.size=' + Trim(WsmAlertPage.Values[0]));
    P.Add('metrics.port=' + Trim(WsmPortPage.Values[0]));
    P.Add('monitor.poll.threads=5');
    // Per-server credentials (first server only — user adds others manually)
    if Trim(WsmCredPage.Values[0]) <> '' then begin
      P.Add('monitor.server.' + FirstServer + '.username=' + Trim(WsmCredPage.Values[0]));
      P.Add('monitor.server.' + FirstServer + '.password=' + Trim(WsmCredPage.Values[1]));
    end;
    P.Add('log.level=INFO');
    P.Add('log.folder=logs');
    P.Add('log.retention.days=30');
    P.Add('log.purge.interval.hours=24');
    P.SaveToFile(AppDir + '\winservicemonitor.properties');
  finally
    P.Free;
  end;
end;

procedure WriteSsProps;
var
  P: TStringList;
  AppDir, SchedStr: String;
begin
  AppDir := ExpandConstant('{app}\monitoring-services\{#DirSS}');
  SchedStr := GetScheduleString;

  P := TStringList.Create;
  try
    P.Add('screenshot.folder=' + Trim(SsScreenshotPage.Values[0]));
    P.Add('');
    P.Add('# Job 1 — configured during install');
    P.Add('job.1.label='            + Trim(SsJob1ServerPage.Values[1]));
    P.Add('job.1.server='           + Trim(SsJob1ServerPage.Values[0]));
    P.Add('job.1.username='         + Trim(SsJob1CredPage.Values[0]));
    P.Add('job.1.password='         + Trim(SsJob1CredPage.Values[1]));
    P.Add('job.1.service.name='     + Trim(SsJob1SvcPage.Values[0]));
    P.Add('job.1.url='              + Trim(SsJob1UrlPage.Values[0]));
    P.Add('job.1.stop.time='        + Trim(SsJob1TimesPage.Values[0]));
    P.Add('job.1.start.time='       + Trim(SsJob1TimesPage.Values[1]));
    P.Add('job.1.schedule='         + SchedStr);
    if SchedStr = 'WEEKLY' then
      P.Add('job.1.schedule.day=MONDAY');
    if SchedStr = 'MONTHLY_NTH_WEEKDAY' then begin
      P.Add('job.1.schedule.nth=2');
      P.Add('job.1.schedule.weekday=SUNDAY');
    end;
    P.Add('job.1.poll.interval.seconds=10');
    P.Add('job.1.poll.timeout.seconds=120');
    P.Add('');
    P.Add('# ---------------------------------------------------------------');
    P.Add('# Add more jobs below. Copy the block above, increment job number.');
    P.Add('# Required fields per job: label, server, username, password,');
    P.Add('#   service.name, url, stop.time, start.time, schedule');
    P.Add('# ---------------------------------------------------------------');
    P.SaveToFile(AppDir + '\servicescheduler.properties');
  finally
    P.Free;
  end;
end;

// ─── WinSW XML writers ────────────────────────────────────────────────────────

procedure WriteWsmXml;
var
  X: TStringList;
  InstDir, AppDir: String;
begin
  InstDir := ExpandConstant('{app}');
  AppDir  := InstDir + '\monitoring-services\{#DirWSM}';
  X := TStringList.Create;
  try
    X.Add('<service>');
    X.Add('  <id>{#SvcWSM}</id>');
    X.Add('  <name>Island Pacific Win Service Monitor</name>');
    X.Add('  <description>Monitors Windows service states and sends email alerts</description>');
    X.Add('  <executable>java</executable>');
    // WSM: args[0]=winservicemonitor.properties  args[1]=email.properties
    X.Add('  <arguments>-jar "' + AppDir + '\{#JarWSM}" "' + AppDir + '\winservicemonitor.properties" "' + AppDir + '\email.properties"</arguments>');
    X.Add('  <logmode>rotate</logmode>');
    X.Add('  <log name="' + InstDir + '\logs\{#SvcWSM}">');
    X.Add('    <sizeThreshold>10240</sizeThreshold>');
    X.Add('    <keepFiles>8</keepFiles>');
    X.Add('  </log>');
    X.Add('  <onfailure action="restart" delay="10 sec"/>');
    X.Add('  <onfailure action="restart" delay="20 sec"/>');
    X.Add('  <onfailure action="none"/>');
    X.Add('</service>');
    X.SaveToFile(InstDir + '\services\{#SvcWSM}.xml');
  finally
    X.Free;
  end;
end;

procedure WriteSsXml;
var
  X: TStringList;
  InstDir, AppDir: String;
begin
  InstDir := ExpandConstant('{app}');
  AppDir  := InstDir + '\monitoring-services\{#DirSS}';
  X := TStringList.Create;
  try
    X.Add('<service>');
    X.Add('  <id>{#SvcSS}</id>');
    X.Add('  <name>Island Pacific Service Scheduler</name>');
    X.Add('  <description>Schedules service stop/start cycles on a timed basis</description>');
    X.Add('  <executable>java</executable>');
    // SS: args[0]=email.properties  args[1]=servicescheduler.properties
    X.Add('  <arguments>-jar "' + AppDir + '\{#JarSS}" "' + AppDir + '\email.properties" "' + AppDir + '\servicescheduler.properties"</arguments>');
    X.Add('  <logmode>rotate</logmode>');
    X.Add('  <log name="' + InstDir + '\logs\{#SvcSS}">');
    X.Add('    <sizeThreshold>10240</sizeThreshold>');
    X.Add('    <keepFiles>8</keepFiles>');
    X.Add('  </log>');
    X.Add('  <onfailure action="restart" delay="10 sec"/>');
    X.Add('  <onfailure action="restart" delay="20 sec"/>');
    X.Add('  <onfailure action="none"/>');
    X.Add('</service>');
    X.SaveToFile(InstDir + '\services\{#SvcSS}.xml');
  finally
    X.Free;
  end;
end;

// ─── Install / upgrade steps ──────────────────────────────────────────────────

procedure CurStepChanged(CurStep: TSetupStep);
var
  WsmExe, SsExe: String;
  RC: Integer;
begin
  if CurStep = ssInstall then begin
    // Stop services before files are copied so JARs are not locked.
    // Use WsmWasInstalled/SsWasInstalled (not WsmIsFresh) — we want to stop
    // existing services regardless of whether the user ticked the checkbox,
    // because the [Files] section may still copy shared files into those dirs.
    WsmExe := ExpandConstant('{app}\services\{#SvcWSM}.exe');
    SsExe  := ExpandConstant('{app}\services\{#SvcSS}.exe');
    if WsmWasInstalled and FileExists(WsmExe) then begin
      Exec(WsmExe, 'stop', '', SW_HIDE, ewWaitUntilTerminated, RC);
      Sleep(2000);
    end;
    if SsWasInstalled and FileExists(SsExe) then begin
      Exec(SsExe, 'stop', '', SW_HIDE, ewWaitUntilTerminated, RC);
      Sleep(2000);
    end;
  end;

  if CurStep = ssPostInstall then begin
    WsmExe := ExpandConstant('{app}\services\{#SvcWSM}.exe');
    SsExe  := ExpandConstant('{app}\services\{#SvcSS}.exe');

    // Write email.properties only for freshly installed services
    // (upgrade preserves existing file via onlyifdoesntexist in [Files])
    if WsmIsFresh then
      WriteEmailProps(ExpandConstant('{app}\monitoring-services\{#DirWSM}\email.properties'));
    if SsIsFresh then
      WriteEmailProps(ExpandConstant('{app}\monitoring-services\{#DirSS}\email.properties'));

    // Rewrite XML for every service being acted on this run.
    // Always done before register/start so WinSW picks up correct paths.
    if WantWSM then WriteWsmXml;
    if WantSS  then WriteSsXml;
    // Also rewrite XML for installed services NOT selected this run — paths may
    // have changed if user moved the install directory.
    if WsmWasInstalled and not WantWSM then WriteWsmXml;
    if SsWasInstalled  and not WantSS  then WriteSsXml;

    if WantWSM then begin
      if WsmIsFresh then begin
        WriteWsmProps;
        if not Exec(WsmExe, 'install', '', SW_HIDE, ewWaitUntilTerminated, RC) then
          MsgBox('Warning: Could not register Win Service Monitor as a Windows service.' + #13#10 +
                 'Run "' + WsmExe + ' install" manually.', mbInformation, MB_OK);
      end;
      // Start for both fresh and upgrade — service was stopped in ssInstall
      if not Exec(WsmExe, 'start', '', SW_HIDE, ewWaitUntilTerminated, RC) then
        MsgBox('Warning: Could not start Win Service Monitor.' + #13#10 +
               'Start it manually from services.msc.', mbInformation, MB_OK);
    end else if WsmWasInstalled then begin
      // Service was stopped in ssInstall but user did not select it this run — restart it
      Exec(WsmExe, 'start', '', SW_HIDE, ewWaitUntilTerminated, RC);
    end;

    if WantSS then begin
      if SsIsFresh then begin
        WriteSsProps;
        if not Exec(SsExe, 'install', '', SW_HIDE, ewWaitUntilTerminated, RC) then
          MsgBox('Warning: Could not register Service Scheduler as a Windows service.' + #13#10 +
                 'Run "' + SsExe + ' install" manually.', mbInformation, MB_OK);
        // Fresh install — do NOT auto-start; job config needs review first
        MsgBox('Service Scheduler installed and registered.' + #13#10 + #13#10 +
               'The service has NOT been started automatically.' + #13#10 +
               'Review the job configuration at:' + #13#10 +
               ExpandConstant('{app}\monitoring-services\{#DirSS}\servicescheduler.properties') + #13#10 + #13#10 +
               'When ready, start it from services.msc or run:' + #13#10 +
               '"' + SsExe + ' start"', mbInformation, MB_OK);
      end else begin
        // Upgrade — service was stopped in ssInstall, restart it now
        if not Exec(SsExe, 'start', '', SW_HIDE, ewWaitUntilTerminated, RC) then
          MsgBox('Warning: Could not restart Service Scheduler.' + #13#10 +
                 'Start it manually from services.msc.', mbInformation, MB_OK);
      end;
    end else if SsWasInstalled then begin
      // Service was stopped in ssInstall but user did not select it this run — restart it
      Exec(SsExe, 'start', '', SW_HIDE, ewWaitUntilTerminated, RC);
    end;
  end;
end;

// ─── Uninstall ────────────────────────────────────────────────────────────────

procedure CurUninstallStepChanged(CurUninstallStep: TUninstallStep);
var
  WsmExe, SsExe: String;
  RC: Integer;
begin
  if CurUninstallStep = usUninstall then begin
    WsmExe := ExpandConstant('{app}\services\{#SvcWSM}.exe');
    SsExe  := ExpandConstant('{app}\services\{#SvcSS}.exe');
    if FileExists(WsmExe) then begin
      Exec(WsmExe, 'stop',      '', SW_HIDE, ewWaitUntilTerminated, RC);
      Exec(WsmExe, 'uninstall', '', SW_HIDE, ewWaitUntilTerminated, RC);
    end;
    if FileExists(SsExe) then begin
      Exec(SsExe, 'stop',      '', SW_HIDE, ewWaitUntilTerminated, RC);
      Exec(SsExe, 'uninstall', '', SW_HIDE, ewWaitUntilTerminated, RC);
    end;
  end;
end;

[UninstallDelete]
Type: files;      Name: "{app}\services\*.xml"
; Properties written by the installer (not in [Files] so not auto-removed)
Type: files;      Name: "{app}\monitoring-services\{#DirWSM}\email.properties"
Type: files;      Name: "{app}\monitoring-services\{#DirWSM}\winservicemonitor.properties"
Type: files;      Name: "{app}\monitoring-services\{#DirSS}\email.properties"
Type: files;      Name: "{app}\monitoring-services\{#DirSS}\servicescheduler.properties"
Type: dirifempty; Name: "{app}\services"
Type: dirifempty; Name: "{app}\monitoring-services\{#DirWSM}"
Type: dirifempty; Name: "{app}\monitoring-services\{#DirSS}"
Type: dirifempty; Name: "{app}\monitoring-services"
