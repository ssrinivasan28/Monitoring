#define AppName "Island Pacific App Server Monitors"
#define AppVersion "1.0.0"
#define AppPublisher "Island Pacific Retail Systems"
#define Svc1Name "IPMonitoring_LogKeywordMonitor"
#define Svc2Name "IPMonitoring_FolderLogKeywordMonitor"
#define Dir1 "LogKeywordMonitor"
#define Dir2 "FolderLogKeywordMonitor"
#define Jar1 "LogKeywordMonitor.jar"
#define Jar2 "FolderLogKeywordMonitor.jar"

[Setup]
AppId={{IP-AppServerMonitors}}
AppName={#AppName}
AppVersion={#AppVersion}
AppPublisher={#AppPublisher}
AppVerName={#AppName} v{#AppVersion}
AppCopyright=Copyright © 2025 Island Pacific Retail Systems
AppSupportURL=https://www.islandpacific.com/
VersionInfoVersion=1.0.0.0
VersionInfoCompany=Island Pacific Retail Systems
VersionInfoProductName={#AppName}
VersionInfoDescription=Island Pacific App Server Monitors (Log Keyword + Folder Log Keyword)

ArchitecturesInstallIn64BitMode=x64compatible
DefaultDirName={commonpf}\Island Pacific\AppServerMonitors
DisableDirPage=no
DisableProgramGroupPage=yes
PrivilegesRequired=admin
WizardStyle=modern
WizardSizePercent=125
WizardImageFile=installer\resources\wizard_modern.bmp
WizardSmallImageFile=installer\resources\wizard_small_modern.bmp
SetupIconFile=installer\resources\ip-monitoring.ico
LicenseFile=installer\resources\license.txt

AppMutex=IPMonitoring_AppServerMonitors_Mutex
CloseApplications=no

Compression=lzma
SolidCompression=yes

OutputDir=.\installer\output
OutputBaseFilename=AppServerMonitorsSetup

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Dirs]
Name: "{app}\services"
Name: "{app}\monitoring-services\{#Dir1}"
Name: "{app}\monitoring-services\{#Dir2}"
Name: "{app}\logs"; Flags: uninsneveruninstall

[Files]
; WinSW wrappers
Source: "installer\resources\WinSW.exe"; DestDir: "{app}\services"; DestName: "{#Svc1Name}.exe"; Flags: ignoreversion
Source: "installer\resources\WinSW.exe"; DestDir: "{app}\services"; DestName: "{#Svc2Name}.exe"; Flags: ignoreversion

; JARs
Source: "installer\resources\monitoring-services\{#Dir1}\{#Jar1}"; DestDir: "{app}\monitoring-services\{#Dir1}"; Flags: ignoreversion
Source: "installer\resources\monitoring-services\{#Dir2}\{#Jar2}"; DestDir: "{app}\monitoring-services\{#Dir2}"; Flags: ignoreversion

; DPAPI credential encryption tool (always installed; run on this machine to encrypt property values)
Source: "installer\resources\monitoring-services\CredTool\CredTool.jar"; DestDir: "{app}"; Flags: ignoreversion skipifsourcedoesntexist

; Logo for email alerts (referenced by monitoring-services\*\logo.path as ..\..\logo\IPLogo.jpg)
Source: "installer\resources\logo\IPLogo.jpg"; DestDir: "{app}\logo"; Flags: ignoreversion skipifsourcedoesntexist

; Properties files — preserved on upgrade
Source: "installer\resources\monitoring-services\{#Dir1}\logkeywordmonitor.properties"; DestDir: "{app}\monitoring-services\{#Dir1}"; Flags: onlyifdoesntexist skipifsourcedoesntexist
Source: "installer\resources\monitoring-services\{#Dir1}\email.properties"; DestDir: "{app}\monitoring-services\{#Dir1}"; Flags: onlyifdoesntexist skipifsourcedoesntexist
Source: "installer\resources\monitoring-services\{#Dir2}\folderlogkeywordmonitor.properties"; DestDir: "{app}\monitoring-services\{#Dir2}"; Flags: onlyifdoesntexist skipifsourcedoesntexist
Source: "installer\resources\monitoring-services\{#Dir2}\email.properties"; DestDir: "{app}\monitoring-services\{#Dir2}"; Flags: onlyifdoesntexist skipifsourcedoesntexist

[Code]
function IsUpgrade: Boolean;
begin
  Result := RegKeyExists(HKLM, 'SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\{IP-AppServerMonitors}_is1');
end;

var
  MetricsPortPage:  TInputQueryWizardPage; // [0]=LogKeyword port [1]=FolderLogKeyword port
  LogFilePage:      TInputQueryWizardPage; // [0]=path [1]=keywords
  FolderPage:        TInputQueryWizardPage; // [0]=folder path [1]=keywords [2]=recursive
  EmailAuthPage:    TInputOptionWizardPage;
  SmtpPage:         TInputQueryWizardPage;
  OAuth2Page:       TInputQueryWizardPage;

// Strips embedded CR/LF from a single-line value. Wizard text fields can carry a
// trailing newline (e.g. pasted from clipboard content or a corrupted prior file),
// which otherwise splits one properties line into two and corrupts the next key.
function SanitizeLine(S: String): String;
begin
  StringChangeEx(S, #13, '', True);
  StringChangeEx(S, #10, '', True);
  Result := Trim(S);
end;

// Reads a single key=value from an existing properties file. Returns Default if the
// file or key is missing. Used to pre-fill wizard fields with current values on upgrade.
function ReadPropValue(PropsFile, Key, Default: String): String;
var
  P: TStringList;
  I: Integer;
  Line, LineKey: String;
begin
  Result := Default;
  if not FileExists(PropsFile) then Exit;
  P := TStringList.Create;
  try
    P.LoadFromFile(PropsFile);
    for I := 0 to P.Count - 1 do begin
      Line := Trim(P.Strings[I]);
      if (Line = '') or (Line[1] = '#') then Continue;
      if Pos('=', Line) = 0 then Continue;
      LineKey := Trim(Copy(Line, 1, Pos('=', Line) - 1));
      if LineKey = Key then begin
        Result := SanitizeLine(Copy(Line, Pos('=', Line) + 1, MaxInt));
        Exit;
      end;
    end;
  finally
    P.Free;
  end;
end;

// Pre-fills wizard fields from the existing properties/email files so an upgrade
// shows current values instead of blanks; unedited fields are written back unchanged.
procedure PrefillFromExistingConfig;
var
  LogProps, FolderProps, Email1, Email2: String;
begin
  LogProps    := ExpandConstant('{app}\monitoring-services\{#Dir1}\logkeywordmonitor.properties');
  FolderProps := ExpandConstant('{app}\monitoring-services\{#Dir2}\folderlogkeywordmonitor.properties');
  Email1      := ExpandConstant('{app}\monitoring-services\{#Dir1}\email.properties');
  Email2      := ExpandConstant('{app}\monitoring-services\{#Dir2}\email.properties');

  MetricsPortPage.Values[0] := ReadPropValue(LogProps, 'metrics.port', MetricsPortPage.Values[0]);
  MetricsPortPage.Values[1] := ReadPropValue(FolderProps, 'metrics.port', MetricsPortPage.Values[1]);

  LogFilePage.Values[0] := ReadPropValue(LogProps, 'monitor.logfile.1.path', LogFilePage.Values[0]);
  LogFilePage.Values[1] := ReadPropValue(LogProps, 'monitor.logfile.1.keywords', LogFilePage.Values[1]);

  FolderPage.Values[0] := ReadPropValue(FolderProps, 'monitor.folder.paths',
                             ReadPropValue(FolderProps, 'monitor.folder.path', FolderPage.Values[0]));
  FolderPage.Values[1] := ReadPropValue(FolderProps, 'monitor.keywords', FolderPage.Values[1]);
  FolderPage.Values[2] := ReadPropValue(FolderProps, 'monitor.recursive', FolderPage.Values[2]);

  // Email settings: prefer Dir2's email.properties, fall back to Dir1's if absent.
  if not FileExists(Email2) then Email2 := Email1;

  if ReadPropValue(Email2, 'mail.auth.method', 'SMTP') = 'OAUTH2' then begin
    EmailAuthPage.Values[0] := False;
    EmailAuthPage.Values[1] := True;
  end else begin
    EmailAuthPage.Values[0] := True;
    EmailAuthPage.Values[1] := False;
  end;

  SmtpPage.Values[0] := ReadPropValue(Email2, 'mail.smtp.host', SmtpPage.Values[0]);
  SmtpPage.Values[1] := ReadPropValue(Email2, 'mail.smtp.port', SmtpPage.Values[1]);
  SmtpPage.Values[2] := ReadPropValue(Email2, 'mail.from', SmtpPage.Values[2]);
  SmtpPage.Values[3] := ReadPropValue(Email2, 'mail.to', SmtpPage.Values[3]);
  SmtpPage.Values[4] := ReadPropValue(Email2, 'mail.bcc', SmtpPage.Values[4]);

  OAuth2Page.Values[0] := ReadPropValue(Email2, 'mail.oauth2.tenant.id', OAuth2Page.Values[0]);
  OAuth2Page.Values[1] := ReadPropValue(Email2, 'mail.oauth2.client.id', OAuth2Page.Values[1]);
  OAuth2Page.Values[2] := ReadPropValue(Email2, 'mail.oauth2.client.secret', OAuth2Page.Values[2]);
  OAuth2Page.Values[3] := ReadPropValue(Email2, 'mail.oauth2.from.user', ReadPropValue(Email2, 'mail.from', OAuth2Page.Values[3]));
  OAuth2Page.Values[4] := ReadPropValue(Email2, 'mail.to', OAuth2Page.Values[4]);
  OAuth2Page.Values[5] := ReadPropValue(Email2, 'mail.bcc', OAuth2Page.Values[5]);
end;

procedure InitializeWizard;
begin
  MetricsPortPage := CreateInputQueryPage(wpSelectDir,
    'Metrics Ports', 'Prometheus metrics ports',
    'Enter the ports each monitor will expose metrics on:');
  MetricsPortPage.Add('Log Keyword Monitor port:', False);
  MetricsPortPage.Add('Folder Log Keyword Monitor port:', False);
  MetricsPortPage.Values[0] := '4013';
  MetricsPortPage.Values[1] := '4016';

  LogFilePage := CreateInputQueryPage(MetricsPortPage.ID,
    'Log Keyword Monitor', 'Log file to scan',
    'Enter the log file path (or glob pattern) and keywords to search for. Additional log files can be added manually in logkeywordmonitor.properties.');
  LogFilePage.Add('Log file path (e.g. C:\Logs\app.*.log):', False);
  LogFilePage.Add('Keywords (comma-separated):', False);
  LogFilePage.Values[1] := 'ERROR,FATAL,Exception,OutOfMemoryError,SEVERE';

  FolderPage := CreateInputQueryPage(LogFilePage.ID,
    'Folder Log Keyword Monitor', 'Folders to scan',
    'Enter the folder(s) to scan and keywords to search for:');
  FolderPage.Add('Folder path(s) (comma-separated, e.g. C:\Logs\MyFolder,D:\IPSA\Logs):', False);
  FolderPage.Add('Keywords (comma-separated):', False);
  FolderPage.Add('Scan subfolders recursively? (true/false):', False);
  FolderPage.Values[1] := 'ERROR,FATAL,Exception,FAILED';
  FolderPage.Values[2] := 'false';

  EmailAuthPage := CreateInputOptionPage(FolderPage.ID,
    'Email Authentication', 'How to send alert emails',
    'Select the email authentication method (used by both monitors):',
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

  if IsUpgrade then
    PrefillFromExistingConfig;
end;

function ShouldSkipPage(PageID: Integer): Boolean;
begin
  Result := False;
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
  if EmailAuthPage.Values[1] then Result := SanitizeLine(OAuth2Page.Values[3])
  else Result := SanitizeLine(SmtpPage.Values[2]);
end;

function GetEmailTo(Param: String): String;
begin
  if EmailAuthPage.Values[1] then Result := SanitizeLine(OAuth2Page.Values[4])
  else Result := SanitizeLine(SmtpPage.Values[3]);
end;

function GetEmailBcc(Param: String): String;
begin
  if EmailAuthPage.Values[1] then Result := SanitizeLine(OAuth2Page.Values[5])
  else Result := SanitizeLine(SmtpPage.Values[4]);
end;

function GetGraphMailUrl(Param: String): String;
begin
  if EmailAuthPage.Values[1] then
    Result := 'https://graph.microsoft.com/v1.0/users/' + SanitizeLine(OAuth2Page.Values[3]) + '/sendMail'
  else
    Result := '';
end;

procedure WriteEmailProps(DestFile: String);
var
  P: TStringList;
  AuthMethod, ExistingScope, ExistingUser, ExistingPass, ExistingImportance, ExistingClientName, ExistingLogoPath: String;
begin
  AuthMethod := GetAuthMethod('');

  // Fields with no wizard input: carry through the existing file's values untouched.
  ExistingScope       := ReadPropValue(DestFile, 'mail.oauth2.scope', 'https://graph.microsoft.com/.default');
  ExistingUser        := ReadPropValue(DestFile, 'mail.smtp.username', '');
  ExistingPass        := ReadPropValue(DestFile, 'mail.smtp.password', '');
  ExistingImportance  := ReadPropValue(DestFile, 'mail.importance', 'High');
  ExistingClientName  := ReadPropValue(DestFile, 'mail.clientName', '');
  ExistingLogoPath    := ReadPropValue(DestFile, 'logo.path', '..\..\logo\IPLogo.jpg');

  P := TStringList.Create;
  try
    P.Add('# ===============================');
    P.Add('# Email Configuration');
    P.Add('# ===============================');
    P.Add('mail.auth.method=' + AuthMethod);
    if AuthMethod = 'SMTP' then begin
      P.Add('mail.smtp.host=' + SanitizeLine(SmtpPage.Values[0]));
      P.Add('mail.smtp.port=' + SanitizeLine(SmtpPage.Values[1]));
      P.Add('mail.smtp.auth=false');
      P.Add('mail.smtp.starttls.enable=false');
      P.Add('mail.smtp.username=' + ExistingUser);
      P.Add('mail.smtp.password=' + ExistingPass);
      P.Add('#mail.oauth2.tenant.id=');
      P.Add('#mail.oauth2.client.id=');
      P.Add('#mail.oauth2.client.secret=');
      P.Add('#mail.oauth2.token.url=');
      P.Add('#mail.oauth2.graph.mail.url=');
    end else begin
      P.Add('#mail.smtp.host=');
      P.Add('#mail.smtp.port=25');
      P.Add('mail.oauth2.tenant.id=' + SanitizeLine(OAuth2Page.Values[0]));
      P.Add('mail.oauth2.client.id=' + SanitizeLine(OAuth2Page.Values[1]));
      P.Add('mail.oauth2.client.secret=' + SanitizeLine(OAuth2Page.Values[2]));
      P.Add('mail.oauth2.scope=' + ExistingScope);
      P.Add('mail.oauth2.token.url=https://login.microsoftonline.com/' + SanitizeLine(OAuth2Page.Values[0]) + '/oauth2/v2.0/token');
      P.Add('mail.oauth2.graph.mail.url=' + GetGraphMailUrl(''));
      P.Add('mail.oauth2.from.user=' + SanitizeLine(OAuth2Page.Values[3]));
    end;
    P.Add('mail.from=' + GetEmailFrom(''));
    P.Add('mail.to=' + GetEmailTo(''));
    P.Add('mail.bcc=' + GetEmailBcc(''));
    P.Add('mail.importance=' + ExistingImportance);
    P.Add('mail.clientName=' + ExistingClientName);
    P.Add('logo.path=' + ExistingLogoPath);
    P.Add('log.level=INFO');
    P.Add('log.folder=logs');
    P.Add('log.retention.days=30');
    P.Add('log.purge.interval.hours=24');
    P.SaveToFile(DestFile);
  finally
    P.Free;
  end;
end;

procedure WriteLogKeywordProps;
var
  P: TStringList;
begin
  P := TStringList.Create;
  try
    P.Add('# Log Keyword Monitor Configuration');
    P.Add('metrics.port=' + SanitizeLine(MetricsPortPage.Values[0]));
    P.Add('check.interval.minutes=1');
    P.Add('client.name=');
    P.Add('email.config.path=email.properties');
    P.Add('alert.on.first.match=false');
    P.Add('logo.path=..\..\logo\IPLogo.jpg');
    P.Add('');
    P.Add('monitor.logfile.1.name=Application Log');
    P.Add('monitor.logfile.1.path=' + SanitizeLine(LogFilePage.Values[0]));
    P.Add('monitor.logfile.1.keywords=' + SanitizeLine(LogFilePage.Values[1]));
    P.Add('monitor.logfile.1.case.sensitive=false');
    P.Add('monitor.logfile.1.regex.mode=false');
    P.Add('');
    P.Add('# Add more log files below. Copy the block above, increment the number.');
    P.Add('');
    P.Add('log.folder=logs');
    P.Add('log.level=INFO');
    P.Add('log.retention.days=30');
    P.Add('log.purge.interval.hours=24');
    P.SaveToFile(ExpandConstant('{app}\monitoring-services\{#Dir1}\logkeywordmonitor.properties'));
  finally
    P.Free;
  end;
end;

procedure WriteFolderLogKeywordProps;
var
  P: TStringList;
begin
  P := TStringList.Create;
  try
    P.Add('# Folder Log Keyword Monitor Configuration');
    P.Add('metrics.port=' + SanitizeLine(MetricsPortPage.Values[1]));
    P.Add('check.interval.minutes=60');
    P.Add('client.name=');
    P.Add('email.config.path=email.properties');
    P.Add('logo.path=..\..\logo\IPLogo.jpg');
    P.Add('');
    P.Add('monitor.folder.paths=' + SanitizeLine(FolderPage.Values[0]));
    P.Add('monitor.keywords=' + SanitizeLine(FolderPage.Values[1]));
    P.Add('monitor.case.sensitive=false');
    P.Add('monitor.recursive=' + SanitizeLine(FolderPage.Values[2]));
    P.Add('');
    P.Add('log.level=INFO');
    P.Add('log.folder=logs');
    P.Add('log.retention.days=30');
    P.Add('log.purge.interval.hours=24');
    P.SaveToFile(ExpandConstant('{app}\monitoring-services\{#Dir2}\folderlogkeywordmonitor.properties'));
  finally
    P.Free;
  end;
end;

procedure WriteServiceXml(SvcName, SvcDesc, MonDir, JarName, PropsName: String);
var
  X: TStringList;
  InstDir, AppDir: String;
begin
  InstDir := ExpandConstant('{app}');
  AppDir  := InstDir + '\monitoring-services\' + MonDir;
  X := TStringList.Create;
  try
    X.Add('<service>');
    X.Add('  <id>' + SvcName + '</id>');
    X.Add('  <name>' + SvcDesc + '</name>');
    X.Add('  <description>' + SvcDesc + '</description>');
    X.Add('  <executable>java</executable>');
    X.Add('  <arguments>-jar "' + AppDir + '\' + JarName + '" "' + AppDir + '\email.properties" "' + AppDir + '\' + PropsName + '"</arguments>');
    X.Add('  <workingdirectory>' + AppDir + '</workingdirectory>');
    X.Add('  <logmode>rotate</logmode>');
    X.Add('  <log name="' + InstDir + '\logs\' + SvcName + '">');
    X.Add('    <sizeThreshold>10240</sizeThreshold>');
    X.Add('    <keepFiles>8</keepFiles>');
    X.Add('  </log>');
    X.Add('  <onfailure action="restart" delay="10 sec"/>');
    X.Add('  <onfailure action="restart" delay="20 sec"/>');
    X.Add('  <onfailure action="none"/>');
    X.Add('</service>');
    X.SaveToFile(InstDir + '\services\' + SvcName + '.xml');
  finally
    X.Free;
  end;
end;

procedure CurStepChanged(CurStep: TSetupStep);
var
  Svc1Exe, Svc2Exe, Props1, Props2, NL, Msg: String;
  ResultCode: Integer;
begin
  NL := Chr(13) + Chr(10);
  Svc1Exe := ExpandConstant('{app}\services\{#Svc1Name}.exe');
  Svc2Exe := ExpandConstant('{app}\services\{#Svc2Name}.exe');

  if CurStep = ssInstall then begin
    if FileExists(Svc1Exe) then begin
      Exec(Svc1Exe, 'stop', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
    end;
    if FileExists(Svc2Exe) then begin
      Exec(Svc2Exe, 'stop', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
    end;
    Sleep(2000);
  end;

  if CurStep = ssPostInstall then begin
    Props1 := ExpandConstant('{app}\monitoring-services\{#Dir1}\logkeywordmonitor.properties');
    Props2 := ExpandConstant('{app}\monitoring-services\{#Dir2}\folderlogkeywordmonitor.properties');
    WriteServiceXml('{#Svc1Name}', 'Island Pacific Log Keyword Monitor', '{#Dir1}', '{#Jar1}', 'logkeywordmonitor.properties');
    WriteServiceXml('{#Svc2Name}', 'Island Pacific Folder Log Keyword Monitor', '{#Dir2}', '{#Jar2}', 'folderlogkeywordmonitor.properties');

    // Wizard fields are pre-filled from the existing files on upgrade, so re-writing here
    // preserves any value the user did not change and applies any value they did.
    WriteEmailProps(ExpandConstant('{app}\monitoring-services\{#Dir1}\email.properties'));
    WriteEmailProps(ExpandConstant('{app}\monitoring-services\{#Dir2}\email.properties'));
    WriteLogKeywordProps;
    WriteFolderLogKeywordProps;

    if IsUpgrade then begin
      Exec(Svc1Exe, 'start', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
      Exec(Svc2Exe, 'start', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
      Msg := 'Upgrade complete.' + NL + NL +
             'Properties and email settings have been updated with the values you entered ' +
             '(unedited fields kept their existing values). Both services restarted.' + NL + NL +
             Props1 + NL + Props2 + NL + NL +
             'Logs:' + NL +
             '  Service wrapper log (startup/crash): ' + ExpandConstant('{app}\logs\{#Svc1Name}.log') + NL +
             '                                        ' + ExpandConstant('{app}\logs\{#Svc2Name}.log') + NL +
             '  Monitor activity log (scans/matches): ' + ExpandConstant('{app}\monitoring-services\{#Dir1}\logs') + NL +
             '                                         ' + ExpandConstant('{app}\monitoring-services\{#Dir2}\logs');
      MsgBox(Msg, mbInformation, MB_OK);
    end else begin
      if not Exec(Svc1Exe, 'install', '', SW_HIDE, ewWaitUntilTerminated, ResultCode) then
        MsgBox('Warning: Could not register the Log Keyword Monitor service.' + NL +
               'Run manually: "' + Svc1Exe + '" install', mbError, MB_OK);
      if not Exec(Svc2Exe, 'install', '', SW_HIDE, ewWaitUntilTerminated, ResultCode) then
        MsgBox('Warning: Could not register the Folder Log Keyword Monitor service.' + NL +
               'Run manually: "' + Svc2Exe + '" install', mbError, MB_OK);

      Msg := 'Installation complete.' + NL + NL +
             'Both services have been registered but NOT started.' + NL +
             'Please review the properties files before starting:' + NL + NL +
             Props1 + NL + Props2 + NL + NL +
             'When ready, start each from services.msc or run:' + NL +
             '"' + Svc1Exe + '" start' + NL +
             '"' + Svc2Exe + '" start' + NL + NL +
             'Logs (once started):' + NL +
             '  Service wrapper log (startup/crash): ' + ExpandConstant('{app}\logs\{#Svc1Name}.log') + NL +
             '                                        ' + ExpandConstant('{app}\logs\{#Svc2Name}.log') + NL +
             '  Monitor activity log (scans/matches): ' + ExpandConstant('{app}\monitoring-services\{#Dir1}\logs') + NL +
             '                                         ' + ExpandConstant('{app}\monitoring-services\{#Dir2}\logs');
      MsgBox(Msg, mbInformation, MB_OK);
    end;
  end;
end;

procedure CurUninstallStepChanged(CurUninstallStep: TUninstallStep);
var
  ResultCode: Integer;
begin
  if CurUninstallStep = usUninstall then begin
    Exec(ExpandConstant('{app}\services\{#Svc1Name}.exe'), 'stop', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
    Exec(ExpandConstant('{app}\services\{#Svc1Name}.exe'), 'uninstall', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
    Exec(ExpandConstant('{app}\services\{#Svc2Name}.exe'), 'stop', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
    Exec(ExpandConstant('{app}\services\{#Svc2Name}.exe'), 'uninstall', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
  end;
end;

[UninstallDelete]
Type: files;      Name: "{app}\services\*.xml"
Type: files;      Name: "{app}\monitoring-services\{#Dir1}\email.properties"
Type: files;      Name: "{app}\monitoring-services\{#Dir2}\email.properties"
Type: dirifempty; Name: "{app}\services"
Type: dirifempty; Name: "{app}\monitoring-services\{#Dir1}"
Type: dirifempty; Name: "{app}\monitoring-services\{#Dir2}"
Type: dirifempty; Name: "{app}\monitoring-services"
