#define AppName "Island Pacific IBM i Monitoring Agent"
#define AppVersion "1.0.0"
#define AppPublisher "Island Pacific Retail Systems"
#define AppRegKey "Software\IslandPacific\IBMiMonitoringAgent"

[Setup]
AppId={{IP-IBMiMonitoringAgent}}
AppName={#AppName}
AppVersion={#AppVersion}
AppPublisher={#AppPublisher}
AppVerName={#AppName} v{#AppVersion}
AppCopyright=Copyright © 2025 Island Pacific Retail Systems
AppSupportURL=https://www.islandpacific.com/
AppUpdatesURL=https://www.islandpacific.com/
VersionInfoVersion=1.0.0.0
VersionInfoCompany=Island Pacific Retail Systems
VersionInfoProductName={#AppName}
VersionInfoDescription=Island Pacific IBM i Monitoring Agent Installer

ArchitecturesInstallIn64BitMode=x64compatible
DefaultDirName={commonpf}\Island Pacific\IBM i Monitoring Agent
DisableDirPage=no
DisableProgramGroupPage=yes
PrivilegesRequired=admin
PrivilegesRequiredOverridesAllowed=commandline dialog
UsePreviousPrivileges=no
WizardStyle=modern
WizardSizePercent=125
WizardImageFile=installer\resources\wizard_modern.bmp
WizardSmallImageFile=installer\resources\wizard_small_modern.bmp
SetupIconFile=installer\resources\ip-monitoring.ico
LicenseFile=installer\resources\license.txt

AppMutex=IP_IBMiMonitoringAgent_Mutex
CloseApplications=no

Compression=lzma
SolidCompression=yes

OutputDir=.\installer\output
OutputBaseFilename=IBMiMonitoringAgentSetup

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Messages]
WelcomeLabel1=Welcome to the Island Pacific IBM i Monitoring Agent
WelcomeLabel2=This wizard will install the Island Pacific IBM i Monitoring Agent on your server.%n%nThe following monitoring services are available:%n%n  • IBMIFSErrorMonitor       — IFS folder file count via SMB%n  • IBMRealTimeIFSMonitor    — IFS error detection%n  • IBMJobQueCountMonitor    — Job queue waiting-job counts%n  • IBMJobQueStatusMonitor   — Job queue status changes%n  • IBMSubSystemMonitor      — Subsystem status%n  • IBMMatrixMonitor         — System matrix / performance%n  • IBMQSYSOPRMonitor        — QSYSOPR operator message queue%n  • IBMFileMemberMonitor     — Physical file member record counts%n  • IBMNetworkEnabler        — Network connectivity%n  • IBMUserProfileChecker    — User profile validation%n  • ServerUpTimeMonitor      — Server ping / uptime%n%nAll agents run as Windows services and send email alerts when action is needed.%n%nClick Next to continue, or Cancel to exit.
FinishedHeadingLabel=Installation Complete
FinishedLabel=The Island Pacific IBM i Monitoring Agent has been installed successfully.%n%nInstalled services are now running and monitoring your IBM i environment.%n%nEach agent writes daily log files to its log folder. You can verify service status at any time with:%n%n  sc query IPMonitoring_IBMIFSErrorMonitor%n  sc query IPMonitoring_IBMJobQueCountMonitor%n%nClick Finish to close this wizard.
FinishedLabelNoIcons=The Island Pacific IBM i Monitoring Agent is installed. Selected monitoring services are now active.
ClickFinish=Installation is complete. Click Finish to close this wizard.

[Dirs]
Name: "{app}\services"
Name: "{app}\monitoring-services"
Name: "{app}\logs"; Flags: uninsneveruninstall

[Files]
; WinSW wrappers - one per service
Source: "installer\resources\WinSW.exe"; DestDir: "{app}\services"; DestName: "IPMonitoring_IBMIFSErrorMonitor.exe";     Flags: ignoreversion
Source: "installer\resources\WinSW.exe"; DestDir: "{app}\services"; DestName: "IPMonitoring_IBMRealTimeIFSMonitor.exe";  Flags: ignoreversion
Source: "installer\resources\WinSW.exe"; DestDir: "{app}\services"; DestName: "IPMonitoring_IBMJobQueCountMonitor.exe";  Flags: ignoreversion
Source: "installer\resources\WinSW.exe"; DestDir: "{app}\services"; DestName: "IPMonitoring_IBMJobQueStatusMonitor.exe"; Flags: ignoreversion
Source: "installer\resources\WinSW.exe"; DestDir: "{app}\services"; DestName: "IPMonitoring_IBMSubSystemMonitor.exe";    Flags: ignoreversion
Source: "installer\resources\WinSW.exe"; DestDir: "{app}\services"; DestName: "IPMonitoring_IBMMatrixMonitor.exe";       Flags: ignoreversion
Source: "installer\resources\WinSW.exe"; DestDir: "{app}\services"; DestName: "IPMonitoring_IBMQSYSOPRMonitor.exe";      Flags: ignoreversion
Source: "installer\resources\WinSW.exe"; DestDir: "{app}\services"; DestName: "IPMonitoring_IBMFileMemberMonitor.exe";   Flags: ignoreversion
Source: "installer\resources\WinSW.exe"; DestDir: "{app}\services"; DestName: "IPMonitoring_IBMNetworkEnabler.exe";      Flags: ignoreversion
Source: "installer\resources\WinSW.exe"; DestDir: "{app}\services"; DestName: "IPMonitoring_IBMUserProfileChecker.exe";  Flags: ignoreversion
Source: "installer\resources\WinSW.exe"; DestDir: "{app}\services"; DestName: "IPMonitoring_IBMJobStatusMonitor.exe";     Flags: ignoreversion

; JAR files (always updated on upgrade)
Source: "installer\resources\monitoring-services\IBMIFSErrorMonitor\*.jar";     DestDir: "{app}\monitoring-services\IBMIFSErrorMonitor";     Flags: ignoreversion; Check: IsIFSErrorMonitorSelected
Source: "installer\resources\monitoring-services\IBMRealTimeIFSMonitor\*.jar";  DestDir: "{app}\monitoring-services\IBMRealTimeIFSMonitor";  Flags: ignoreversion; Check: IsRealTimeIFSMonitorSelected
Source: "installer\resources\monitoring-services\IBMJobQueCountMonitor\*.jar";  DestDir: "{app}\monitoring-services\IBMJobQueCountMonitor";  Flags: ignoreversion; Check: IsJobQueCountMonitorSelected
Source: "installer\resources\monitoring-services\IBMJobQueStatusMonitor\*.jar"; DestDir: "{app}\monitoring-services\IBMJobQueStatusMonitor"; Flags: ignoreversion; Check: IsJobQueStatusMonitorSelected
Source: "installer\resources\monitoring-services\IBMSubSystemMonitoring\*.jar"; DestDir: "{app}\monitoring-services\IBMSubSystemMonitoring"; Flags: ignoreversion; Check: IsSubSystemMonitorSelected
Source: "installer\resources\monitoring-services\IBMSystemMatrix\*.jar";        DestDir: "{app}\monitoring-services\IBMSystemMatrix";        Flags: ignoreversion; Check: IsSystemMatrixSelected
Source: "installer\resources\monitoring-services\IBMQSYSOPRMonitor\*.jar";      DestDir: "{app}\monitoring-services\IBMQSYSOPRMonitor";      Flags: ignoreversion; Check: IsQSYSOPRMonitorSelected
Source: "installer\resources\monitoring-services\IBMFileMemberMonitor\*.jar";   DestDir: "{app}\monitoring-services\IBMFileMemberMonitor";   Flags: ignoreversion; Check: IsFileMemberMonitorSelected
Source: "installer\resources\monitoring-services\IBMNetworkEnabler\*.jar";         DestDir: "{app}\monitoring-services\IBMNetworkEnabler";         Flags: ignoreversion; Check: IsNetWorkEnablerSelected
Source: "installer\resources\monitoring-services\IBMUserProfileChecker\*.jar";  DestDir: "{app}\monitoring-services\IBMUserProfileChecker";  Flags: ignoreversion; Check: IsUserProfileCheckerSelected
Source: "installer\resources\monitoring-services\IBMJobStatusMonitor\*.jar";  DestDir: "{app}\monitoring-services\IBMJobStatusMonitor";  Flags: ignoreversion; Check: IsJobStatusMonitorSelected
; DPAPI credential encryption tool
Source: "installer\resources\monitoring-services\CredTool\CredTool.jar"; DestDir: "{app}"; Flags: ignoreversion skipifsourcedoesntexist

; Properties files - only written if not already present (preserves user config on upgrade)
Source: "installer\resources\monitoring-services\IBMIFSErrorMonitor\*.properties";     DestDir: "{app}\monitoring-services\IBMIFSErrorMonitor";     Excludes: "email.properties"; Flags: onlyifdoesntexist skipifsourcedoesntexist; Check: IsIFSErrorMonitorSelected
Source: "installer\resources\monitoring-services\IBMRealTimeIFSMonitor\*.properties";  DestDir: "{app}\monitoring-services\IBMRealTimeIFSMonitor";  Excludes: "email.properties"; Flags: onlyifdoesntexist skipifsourcedoesntexist; Check: IsRealTimeIFSMonitorSelected
Source: "installer\resources\monitoring-services\IBMJobQueCountMonitor\*.properties";  DestDir: "{app}\monitoring-services\IBMJobQueCountMonitor";  Excludes: "email.properties"; Flags: onlyifdoesntexist skipifsourcedoesntexist; Check: IsJobQueCountMonitorSelected
Source: "installer\resources\monitoring-services\IBMJobQueStatusMonitor\*.properties"; DestDir: "{app}\monitoring-services\IBMJobQueStatusMonitor"; Excludes: "email.properties"; Flags: onlyifdoesntexist skipifsourcedoesntexist; Check: IsJobQueStatusMonitorSelected
Source: "installer\resources\monitoring-services\IBMSubSystemMonitoring\*.properties"; DestDir: "{app}\monitoring-services\IBMSubSystemMonitoring"; Excludes: "email.properties"; Flags: onlyifdoesntexist skipifsourcedoesntexist; Check: IsSubSystemMonitorSelected
Source: "installer\resources\monitoring-services\IBMSystemMatrix\*.properties";        DestDir: "{app}\monitoring-services\IBMSystemMatrix";        Excludes: "email.properties"; Flags: onlyifdoesntexist skipifsourcedoesntexist; Check: IsSystemMatrixSelected
Source: "installer\resources\monitoring-services\IBMQSYSOPRMonitor\*.properties";      DestDir: "{app}\monitoring-services\IBMQSYSOPRMonitor";      Excludes: "email.properties"; Flags: onlyifdoesntexist skipifsourcedoesntexist; Check: IsQSYSOPRMonitorSelected
Source: "installer\resources\monitoring-services\IBMFileMemberMonitor\*.properties";   DestDir: "{app}\monitoring-services\IBMFileMemberMonitor";   Excludes: "email.properties"; Flags: onlyifdoesntexist skipifsourcedoesntexist; Check: IsFileMemberMonitorSelected
Source: "installer\resources\monitoring-services\IBMNetworkEnabler\*.properties";         DestDir: "{app}\monitoring-services\IBMNetworkEnabler";         Excludes: "email.properties"; Flags: onlyifdoesntexist skipifsourcedoesntexist; Check: IsNetWorkEnablerSelected
Source: "installer\resources\monitoring-services\IBMUserProfileChecker\*.properties";  DestDir: "{app}\monitoring-services\IBMUserProfileChecker";  Excludes: "email.properties"; Flags: onlyifdoesntexist skipifsourcedoesntexist; Check: IsUserProfileCheckerSelected
Source: "installer\resources\monitoring-services\IBMJobStatusMonitor\*.properties";  DestDir: "{app}\monitoring-services\IBMJobStatusMonitor";  Excludes: "email.properties"; Flags: onlyifdoesntexist skipifsourcedoesntexist; Check: IsJobStatusMonitorSelected

[Registry]
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "InstallPath";    ValueData: "{app}";                        Flags: uninsdeletekey
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "Version";        ValueData: "{#AppVersion}";                Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "ClientName";     ValueData: "{code:GetClientName}";         Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "IBMiServer";     ValueData: "{code:GetIBMiServer}";         Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "IBMiUser";       ValueData: "{code:GetIBMiUser}";           Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "EmailAuthMethod";ValueData: "{code:GetEmailAuthMethod}";    Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "SmtpHost";       ValueData: "{code:GetSmtpHost}";           Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "SmtpPort";       ValueData: "{code:GetSmtpPort}";           Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "SmtpUsername";   ValueData: "{code:GetSmtpUsername}";       Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "EmailFrom";      ValueData: "{code:GetEmailFrom}";          Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "EmailTo";        ValueData: "{code:GetEmailTo}";            Flags: uninsdeletevalue

[UninstallDelete]
Type: files;     Name: "{app}\services\*.xml"
Type: files;     Name: "{app}\monitoring-services\*\email.properties"
Type: dirifempty; Name: "{app}\monitoring-services\IBMIFSErrorMonitor"
Type: dirifempty; Name: "{app}\monitoring-services\IBMRealTimeIFSMonitor"
Type: dirifempty; Name: "{app}\monitoring-services\IBMJobQueCountMonitor"
Type: dirifempty; Name: "{app}\monitoring-services\IBMJobQueStatusMonitor"
Type: dirifempty; Name: "{app}\monitoring-services\IBMSubSystemMonitoring"
Type: dirifempty; Name: "{app}\monitoring-services\IBMSystemMatrix"
Type: dirifempty; Name: "{app}\monitoring-services\IBMQSYSOPRMonitor"
Type: dirifempty; Name: "{app}\monitoring-services\IBMFileMemberMonitor"
Type: dirifempty; Name: "{app}\monitoring-services\IBMNetworkEnabler"
Type: dirifempty; Name: "{app}\monitoring-services\IBMUserProfileChecker"
Type: dirifempty; Name: "{app}\monitoring-services\IBMJobStatusMonitor"
Type: dirifempty; Name: "{app}\monitoring-services"
Type: dirifempty; Name: "{app}\services"
Type: dirifempty; Name: "{app}"

[Code]

// =============================================================================
// Global variables
// =============================================================================
var
  ClientNamePage:      TInputQueryWizardPage;
  MonitorSelectPage:   TWizardPage;
  IBMiConfigPage:      TWizardPage;
  EmailAuthPage:       TInputOptionWizardPage;
  SmtpConfigPage:      TWizardPage;
  OAuthConfigPage:     TWizardPage;
  EmailRecipientsPage: TWizardPage;
  ServicePortsPage:    TWizardPage;
  // Monitor checkboxes
  ChkIFSErrorMonitor:      TNewCheckBox;
  ChkRealTimeIFSMonitor:   TNewCheckBox;
  ChkJobQueCountMonitor:   TNewCheckBox;
  ChkJobQueStatusMonitor:  TNewCheckBox;
  ChkSubSystemMonitor:     TNewCheckBox;
  ChkSystemMatrix:         TNewCheckBox;
  ChkQSYSOPRMonitor:       TNewCheckBox;
  ChkFileMemberMonitor:    TNewCheckBox;
  ChkNetWorkEnabler:       TNewCheckBox;
  ChkUserProfileChecker:   TNewCheckBox;
  ChkJobStatusMonitor:     TNewCheckBox;

  // IBM i connection
  IBMiServerEdit, IBMiUserEdit, IBMiPasswordEdit: TNewEdit;

  // SMTP
  SmtpHostEdit, SmtpPortEdit, SmtpUsernameEdit, SmtpPasswordEdit: TNewEdit;
  SmtpAuthCheckbox, SmtpStartTlsCheckbox: TNewCheckBox;

  // OAuth2
  OAuthTenantEdit, OAuthClientIdEdit, OAuthClientSecretEdit: TNewEdit;
  OAuthScopeEdit, OAuthFromUserEdit: TNewEdit;

  // Email recipients
  EmailFromEdit, EmailToEdit, EmailBccEdit, EmailClientNameEdit: TNewEdit;
  EmailImportanceCombo: TNewComboBox;

  // Ports
  IFSErrorMonitorPortEdit:     TNewEdit;
  RealTimeIFSMonitorPortEdit:  TNewEdit;
  JobQueCountMonitorPortEdit:  TNewEdit;
  JobQueStatusMonitorPortEdit: TNewEdit;
  SubSystemMonitorPortEdit:    TNewEdit;
  SystemMatrixPortEdit:        TNewEdit;
  QSYSOPRMonitorPortEdit:      TNewEdit;
  FileMemberMonitorPortEdit:   TNewEdit;
  JobStatusMonitorPortEdit:    TNewEdit;

  IsUpgrade: Boolean;

// =============================================================================
// Helpers
// =============================================================================
function CheckIsUpgrade: Boolean;
var Path: string;
begin
  Result := RegQueryStringValue(HKLM, '{#AppRegKey}', 'InstallPath', Path) and (Path <> '');
end;

function GetSavedValue(const Name, Default: string): string;
begin
  if not RegQueryStringValue(HKLM, '{#AppRegKey}', Name, Result) then
    Result := Default;
end;

function IsValidPort(const s: string): Boolean;
var N: Integer;
begin
  N := StrToIntDef(s, 0);
  Result := (N >= 1024) and (N <= 65535);
end;

function XmlEscape(Value: string): string;
begin
  Result := Value;
  StringChangeEx(Result, '&', '&amp;', True);
  StringChangeEx(Result, '<', '&lt;', True);
  StringChangeEx(Result, '>', '&gt;', True);
  StringChangeEx(Result, '"', '&quot;', True);
  StringChangeEx(Result, '''', '&apos;', True);
end;

function ServiceExists(ServiceId: string): Boolean;
begin
  Result := RegKeyExists(HKLM, 'SYSTEM\CurrentControlSet\Services\' + ServiceId);
end;

function GetExistingEmailValue(ServicePath, Key, Default: string): string;
var
  Lines: TArrayOfString;
  i, PrefixLen: Integer;
  Prefix: string;
begin
  Result := Default;
  Prefix := Key + '=';
  PrefixLen := Length(Prefix);
  if not LoadStringsFromFile(ServicePath + '\email.properties', Lines) then Exit;
  for i := 0 to GetArrayLength(Lines) - 1 do
    if Copy(Lines[i], 1, PrefixLen) = Prefix then
    begin
      Result := Copy(Lines[i], PrefixLen + 1, Length(Lines[i]) - PrefixLen);
      Exit;
    end;
end;

// =============================================================================
// Accessors
// =============================================================================
function GetClientName(Param: string): string;  begin Result := ClientNamePage.Values[0]; end;
function GetIBMiServer(Param: string): string;  begin Result := IBMiServerEdit.Text; end;
function GetIBMiUser(Param: string): string;    begin Result := IBMiUserEdit.Text; end;

function GetEmailAuthMethod(Param: string): string;
begin
  if EmailAuthPage.SelectedValueIndex = 0 then Result := 'SMTP' else Result := 'OAUTH2';
end;

function GetSmtpHost(Param: string): string;      begin Result := SmtpHostEdit.Text; end;
function GetSmtpPort(Param: string): string;      begin Result := SmtpPortEdit.Text; end;
function GetSmtpUsername(Param: string): string;  begin Result := SmtpUsernameEdit.Text; end;
function GetSmtpPassword(Param: string): string;  begin Result := SmtpPasswordEdit.Text; end;
function GetSmtpAuth(Param: string): string;
begin if SmtpAuthCheckbox.Checked then Result := 'true' else Result := 'false'; end;
function GetSmtpStartTls(Param: string): string;
begin if SmtpStartTlsCheckbox.Checked then Result := 'true' else Result := 'false'; end;

function GetOAuthTenant(Param: string): string;       begin Result := OAuthTenantEdit.Text; end;
function GetOAuthClientId(Param: string): string;     begin Result := OAuthClientIdEdit.Text; end;
function GetOAuthClientSecret(Param: string): string; begin Result := OAuthClientSecretEdit.Text; end;
function GetOAuthScope(Param: string): string;        begin Result := OAuthScopeEdit.Text; end;
function GetOAuthFromUser(Param: string): string;     begin Result := OAuthFromUserEdit.Text; end;
function GetOAuthTokenUrl(Param: string): string;
begin
  if OAuthTenantEdit.Text <> '' then
    Result := 'https://login.microsoftonline.com/' + OAuthTenantEdit.Text + '/oauth2/v2.0/token'
  else Result := '';
end;
function GetOAuthMailUrl(Param: string): string;
begin
  if OAuthFromUserEdit.Text <> '' then
    Result := 'https://graph.microsoft.com/v1.0/users/' + OAuthFromUserEdit.Text + '/sendMail'
  else Result := 'https://graph.microsoft.com/v1.0/me/sendMail';
end;

function GetEmailFrom(Param: string): string;       begin Result := EmailFromEdit.Text; end;
function GetEmailTo(Param: string): string;         begin Result := EmailToEdit.Text; end;
function GetEmailBcc(Param: string): string;        begin Result := EmailBccEdit.Text; end;
function GetClientNameEmail(Param: string): string; begin Result := EmailClientNameEdit.Text; end;
function GetEmailImportance(Param: string): string;
begin Result := EmailImportanceCombo.Items[EmailImportanceCombo.ItemIndex]; end;

function IsIFSErrorMonitorSelected: Boolean;     begin Result := ChkIFSErrorMonitor.Checked; end;
function IsRealTimeIFSMonitorSelected: Boolean;  begin Result := ChkRealTimeIFSMonitor.Checked; end;
function IsJobQueCountMonitorSelected: Boolean;  begin Result := ChkJobQueCountMonitor.Checked; end;
function IsJobQueStatusMonitorSelected: Boolean; begin Result := ChkJobQueStatusMonitor.Checked; end;
function IsSubSystemMonitorSelected: Boolean;    begin Result := ChkSubSystemMonitor.Checked; end;
function IsSystemMatrixSelected: Boolean;        begin Result := ChkSystemMatrix.Checked; end;
function IsQSYSOPRMonitorSelected: Boolean;      begin Result := ChkQSYSOPRMonitor.Checked; end;
function IsFileMemberMonitorSelected: Boolean;   begin Result := ChkFileMemberMonitor.Checked; end;
function IsNetWorkEnablerSelected: Boolean;      begin Result := ChkNetWorkEnabler.Checked; end;
function IsUserProfileCheckerSelected: Boolean;  begin Result := ChkUserProfileChecker.Checked; end;
function IsJobStatusMonitorSelected: Boolean;    begin Result := ChkJobStatusMonitor.Checked; end;

// =============================================================================
// DPAPI encryption
// =============================================================================
function SetEnvironmentVariable(lpName, lpValue: string): Boolean;
  external 'SetEnvironmentVariableW@kernel32.dll stdcall';

function DpapiEncrypt(Value: string): string;
var
  OutFile, Cmd: string;
  ResultCode: Integer;
  Blob: AnsiString;
begin
  Result := Value;
  if (Value = '') or (Copy(Value, 1, 6) = 'DPAPI(') then Exit;
  OutFile := ExpandConstant('{tmp}\dpapi_out.txt');
  DeleteFile(OutFile);
  SetEnvironmentVariable('IP_DPAPI_VALUE', Value);
  SetEnvironmentVariable('IP_DPAPI_OUT', OutFile);
  Cmd := '-NoProfile -ExecutionPolicy Bypass -Command "' +
         'Add-Type -AssemblyName System.Security; ' +
         '[IO.File]::WriteAllText($env:IP_DPAPI_OUT, ''DPAPI('' + ' +
         '[Convert]::ToBase64String([Security.Cryptography.ProtectedData]::Protect(' +
         '[Text.Encoding]::UTF8.GetBytes($env:IP_DPAPI_VALUE), $null, ' +
         '[Security.Cryptography.DataProtectionScope]::LocalMachine)) + '')'')"';
  if Exec('powershell.exe', Cmd, '', SW_HIDE, ewWaitUntilTerminated, ResultCode) and
     (ResultCode = 0) and LoadStringFromFile(OutFile, Blob) then
    Result := Trim(String(Blob))
  else
    Log('DPAPI encryption failed (exit ' + IntToStr(ResultCode) + ') - storing plaintext');
  DeleteFile(OutFile);
  SetEnvironmentVariable('IP_DPAPI_VALUE', '');
end;

// =============================================================================
// Write email.properties
// =============================================================================
procedure GenerateEmailProperties(ServicePath: string);
var
  EmailFile, AuthMethod, Content, SmtpPassword, OAuthClientSecret: string;
  CRLF: string;
begin
  CRLF := #13#10;
  EmailFile := ServicePath + '\email.properties';
  AuthMethod := GetEmailAuthMethod('');
  SmtpPassword := GetSmtpPassword('');
  OAuthClientSecret := GetOAuthClientSecret('');
  if (SmtpPassword = '') and FileExists(EmailFile) then
    SmtpPassword := GetExistingEmailValue(ServicePath, 'mail.smtp.password', '');
  if (OAuthClientSecret = '') and FileExists(EmailFile) then
    OAuthClientSecret := GetExistingEmailValue(ServicePath, 'mail.oauth2.client.secret', '');
  SmtpPassword := DpapiEncrypt(SmtpPassword);
  OAuthClientSecret := DpapiEncrypt(OAuthClientSecret);

  Content := '# ===============================' + CRLF;
  Content := Content + '# Email Configuration' + CRLF;
  Content := Content + '# ===============================' + CRLF + CRLF;
  Content := Content + 'mail.auth.method=' + AuthMethod + CRLF + CRLF;
  Content := Content + 'mail.smtp.host=' + GetSmtpHost('') + CRLF;
  Content := Content + 'mail.smtp.port=' + GetSmtpPort('') + CRLF + CRLF;
  Content := Content + 'mail.smtp.auth=' + GetSmtpAuth('') + CRLF;
  Content := Content + 'mail.smtp.starttls.enable=' + GetSmtpStartTls('') + CRLF;
  Content := Content + 'mail.smtp.username=' + GetSmtpUsername('') + CRLF;
  Content := Content + 'mail.smtp.password=' + SmtpPassword + CRLF + CRLF;
  Content := Content + 'mail.oauth2.tenant.id=' + GetOAuthTenant('') + CRLF;
  Content := Content + 'mail.oauth2.client.id=' + GetOAuthClientId('') + CRLF;
  Content := Content + 'mail.oauth2.client.secret=' + OAuthClientSecret + CRLF;
  Content := Content + 'mail.oauth2.scope=' + GetOAuthScope('') + CRLF;
  Content := Content + 'mail.oauth2.token.url=' + GetOAuthTokenUrl('') + CRLF;
  Content := Content + 'mail.oauth2.graph.mail.url=' + GetOAuthMailUrl('') + CRLF;
  Content := Content + 'mail.oauth2.from.user=' + GetOAuthFromUser('') + CRLF + CRLF;
  Content := Content + 'mail.from=' + GetEmailFrom('') + CRLF;
  Content := Content + 'mail.to=' + GetEmailTo('') + CRLF;
  Content := Content + 'mail.bcc=' + GetEmailBcc('') + CRLF + CRLF;
  Content := Content + 'mail.importance=' + GetEmailImportance('') + CRLF + CRLF;
  Content := Content + 'mail.clientName=' + GetClientNameEmail('') + CRLF + CRLF;
  Content := Content + '# ===============================' + CRLF;
  Content := Content + '# Logging Configuration' + CRLF;
  Content := Content + '# ===============================' + CRLF;
  Content := Content + 'log.retention.days=30' + CRLF;
  Content := Content + 'log.purge.interval.hours=24' + CRLF;
  SaveStringToFile(EmailFile, Content, False);
end;

// =============================================================================
// WinSW service XML
// =============================================================================
procedure GenerateServiceXml(ServiceId, ServiceName, Description, Executable, Arguments, WorkDir: string);
var D: string;
begin
  D := ExpandConstant('{app}');
  SaveStringToFile(D + '\services\' + ServiceId + '.xml',
    '<service>'#13#10 +
    '  <id>' + XmlEscape(ServiceId) + '</id>'#13#10 +
    '  <name>' + XmlEscape(ServiceName) + '</name>'#13#10 +
    '  <description>' + XmlEscape(Description) + '</description>'#13#10 +
    '  <executable>' + XmlEscape(Executable) + '</executable>'#13#10 +
    '  <arguments>' + XmlEscape(Arguments) + '</arguments>'#13#10 +
    '  <workingdirectory>' + XmlEscape(WorkDir) + '</workingdirectory>'#13#10 +
    '  <logmode>rotate</logmode>'#13#10 +
    '  <logpath>' + XmlEscape(WorkDir + '\logs') + '</logpath>'#13#10 +
    '  <onfailure action="restart" delay="10 sec"/>'#13#10 +
    '  <onfailure action="restart" delay="20 sec"/>'#13#10 +
    '  <onfailure action="restart" delay="30 sec"/>'#13#10 +
    '  <resetfailure>1 hour</resetfailure>'#13#10 +
    '</service>', False);
end;

// =============================================================================
// Properties helpers
// =============================================================================
procedure UpdatePropsKey(PropsFile, Key, Value: string);
var
  Lines: TArrayOfString;
  i: Integer;
  Line, Output: string;
  Updated: Boolean;
begin
  if not LoadStringsFromFile(PropsFile, Lines) then Exit;
  Output := ''; Updated := False;
  for i := 0 to GetArrayLength(Lines) - 1 do
  begin
    Line := Lines[i];
    if Pos(Key + '=', Line) = 1 then begin Line := Key + '=' + Value; Updated := True; end;
    Output := Output + Line + #13#10;
  end;
  if not Updated then Output := Output + Key + '=' + Value + #13#10;
  SaveStringToFile(PropsFile, Output, False);
end;

procedure UpdateIBMiCredentials(PropsFile, HostKey, UserKey, PasswordKey: string);
begin
  UpdatePropsKey(PropsFile, HostKey,     IBMiServerEdit.Text);
  UpdatePropsKey(PropsFile, UserKey,     IBMiUserEdit.Text);
  UpdatePropsKey(PropsFile, PasswordKey, DpapiEncrypt(IBMiPasswordEdit.Text));
end;

// =============================================================================
// WinSW service management
// =============================================================================
procedure InstallAndStartService(ServiceExe: string);
var
  D, ExePath, ServiceId: string;
  ResultCode: Integer;
begin
  D := ExpandConstant('{app}');
  ExePath := D + '\services\' + ServiceExe;
  if not FileExists(ExePath) then begin Log('EXE not found: ' + ExePath); Exit; end;
  ServiceId := Copy(ServiceExe, 1, Length(ServiceExe) - 4);
  if ServiceExists(ServiceId) then
  begin
    Exec(ExePath, 'stop',      D + '\services', SW_HIDE, ewWaitUntilTerminated, ResultCode); Sleep(1000);
    Exec(ExePath, 'uninstall', D + '\services', SW_HIDE, ewWaitUntilTerminated, ResultCode); Sleep(1000);
  end;
  if Exec(ExePath, 'install', D + '\services', SW_HIDE, ewWaitUntilTerminated, ResultCode) and (ResultCode = 0) then
    Exec(ExePath, 'start', D + '\services', SW_HIDE, ewWaitUntilTerminated, ResultCode)
  else
    Log('Install failed for ' + ServiceExe + ' (code ' + IntToStr(ResultCode) + ')');
end;

procedure StopAndUninstallService(ServiceExe: string);
var D, ExePath: string; ResultCode: Integer;
begin
  D := ExpandConstant('{app}');
  ExePath := D + '\services\' + ServiceExe;
  if not FileExists(ExePath) then Exit;
  Exec(ExePath, 'stop',      D + '\services', SW_HIDE, ewWaitUntilTerminated, ResultCode);
  Exec(ExePath, 'uninstall', D + '\services', SW_HIDE, ewWaitUntilTerminated, ResultCode);
end;

// Install one IBM i monitor
procedure InstallIBMiMonitor(FolderName, ServiceId, DisplayName, Description,
    JarName, PropsFile, Port, HostKey, UserKey, PasswordKey: string; HasIBMiCredentials: Boolean);
var
  D, ServicePath: string;
begin
  D := ExpandConstant('{app}');
  ServicePath := D + '\monitoring-services\' + FolderName;
  if Port <> '' then UpdatePropsKey(ServicePath + '\' + PropsFile, 'metrics.port', Port);
  UpdatePropsKey(ServicePath + '\' + PropsFile, 'client.name', GetClientName(''));
  if HasIBMiCredentials then UpdateIBMiCredentials(ServicePath + '\' + PropsFile, HostKey, UserKey, PasswordKey);
  GenerateEmailProperties(ServicePath);
  GenerateServiceXml(ServiceId, DisplayName, Description,
    'java', '-jar "' + ServicePath + '\' + JarName + '"', ServicePath);
  InstallAndStartService(ServiceId + '.exe');
end;

// =============================================================================
// Wizard page layout helpers
// =============================================================================
function AddLabel(Page: TWizardPage; Top, Width: Integer; Caption: string; Bold: Boolean): TNewStaticText;
begin
  Result := TNewStaticText.Create(Page);
  Result.Parent := Page.Surface;
  Result.Top := Top; Result.Left := 0; Result.Width := Width;
  Result.Caption := Caption; Result.AutoSize := True;
  if Bold then Result.Font.Style := [fsBold];
end;

function AddEdit(Page: TWizardPage; Top, Width: Integer; DefaultValue: string; Password: Boolean): TNewEdit;
begin
  Result := TNewEdit.Create(Page);
  Result.Parent := Page.Surface;
  Result.Top := Top; Result.Left := 0; Result.Width := Width;
  Result.Text := DefaultValue;
  if Password then Result.PasswordChar := '*';
end;

function AddCheckbox(Page: TWizardPage; Top: Integer; Caption: string; Checked: Boolean): TNewCheckBox;
begin
  Result := TNewCheckBox.Create(Page);
  Result.Parent := Page.Surface;
  Result.Top := Top; Result.Left := 0; Result.Width := Page.SurfaceWidth;
  Result.Caption := Caption; Result.Checked := Checked;
end;

// =============================================================================
// InitializeWizard — build all custom pages
// =============================================================================
procedure InitializeWizard;
var
  W, Row: Integer;
begin
  IsUpgrade := CheckIsUpgrade;
  W := WizardForm.InnerNotebook.Width - 16;

  // --- Client name ---
  ClientNamePage := CreateInputQueryPage(wpWelcome,
    'Client / Site Name', 'Identify this installation',
    'Enter a name for this client or site. It will appear in alert email subject lines.');
  ClientNamePage.Add('Client name:', False);
  ClientNamePage.Values[0] := GetSavedValue('ClientName', '');

  // --- Monitor selection ---
  MonitorSelectPage := CreateCustomPage(ClientNamePage.ID,
    'Select Monitors', 'Choose which IBM i monitors to install');
  Row := 8;
  ChkIFSErrorMonitor     := AddCheckbox(MonitorSelectPage, Row, 'IBMIFSErrorMonitor   — IFS folder file counts via SMB', True);  Row := Row + 24;
  ChkRealTimeIFSMonitor  := AddCheckbox(MonitorSelectPage, Row, 'IBMRealTimeIFSMonitor — IFS error detection', True);             Row := Row + 24;
  ChkJobQueCountMonitor  := AddCheckbox(MonitorSelectPage, Row, 'IBMJobQueCountMonitor — Job queue waiting-job counts', True);     Row := Row + 24;
  ChkJobQueStatusMonitor := AddCheckbox(MonitorSelectPage, Row, 'IBMJobQueStatusMonitor — Job queue status changes', True);       Row := Row + 24;
  ChkSubSystemMonitor    := AddCheckbox(MonitorSelectPage, Row, 'IBMSubSystemMonitor   — Subsystem status', True);                 Row := Row + 24;
  ChkSystemMatrix        := AddCheckbox(MonitorSelectPage, Row, 'IBMMatrixMonitor      — System matrix / performance', True);     Row := Row + 24;
  ChkQSYSOPRMonitor      := AddCheckbox(MonitorSelectPage, Row, 'IBMQSYSOPRMonitor     — QSYSOPR operator message queue', True);  Row := Row + 24;
  ChkFileMemberMonitor   := AddCheckbox(MonitorSelectPage, Row, 'IBMFileMemberMonitor  — Physical file member record counts', True); Row := Row + 24;
  ChkNetWorkEnabler      := AddCheckbox(MonitorSelectPage, Row, 'IBMNetworkEnabler     — Network connectivity', True);            Row := Row + 24;
  ChkUserProfileChecker  := AddCheckbox(MonitorSelectPage, Row, 'IBMUserProfileChecker — User profile validation', False);        Row := Row + 24;
  ChkJobStatusMonitor    := AddCheckbox(MonitorSelectPage, Row, 'IBMJobStatusMonitor   — IBM i jobs in MSGW status', True);

  // --- IBM i connection ---
  IBMiConfigPage := CreateCustomPage(MonitorSelectPage.ID,
    'IBM i Connection', 'Enter the IBM i server credentials used by all IBM i monitors');
  Row := 8;
  AddLabel(IBMiConfigPage, Row, W, 'IBM i Host / IP Address:', False); Row := Row + 20;
  IBMiServerEdit := AddEdit(IBMiConfigPage, Row, W, GetSavedValue('IBMiServer', ''), False); Row := Row + 36;
  AddLabel(IBMiConfigPage, Row, W, 'IBM i User:', False); Row := Row + 20;
  IBMiUserEdit := AddEdit(IBMiConfigPage, Row, W, GetSavedValue('IBMiUser', ''), False); Row := Row + 36;
  AddLabel(IBMiConfigPage, Row, W, 'IBM i Password:', False); Row := Row + 20;
  IBMiPasswordEdit := AddEdit(IBMiConfigPage, Row, W, '', True);

  // --- Email auth ---
  EmailAuthPage := CreateInputOptionPage(IBMiConfigPage.ID,
    'Email Authentication', 'How should monitors send alert emails?',
    'Select the authentication method for outgoing email alerts.', True, False);
  EmailAuthPage.Add('SMTP (username + password)');
  EmailAuthPage.Add('OAuth2 / Microsoft 365 (Azure AD)');
  if GetSavedValue('EmailAuthMethod', 'SMTP') = 'OAUTH2' then
    EmailAuthPage.SelectedValueIndex := 1
  else
    EmailAuthPage.SelectedValueIndex := 0;

  // --- SMTP config ---
  SmtpConfigPage := CreateCustomPage(EmailAuthPage.ID,
    'SMTP Configuration', 'Enter your SMTP server settings');
  Row := 8;
  AddLabel(SmtpConfigPage, Row, W, 'SMTP Host:', False); Row := Row + 20;
  SmtpHostEdit := AddEdit(SmtpConfigPage, Row, W, GetSavedValue('SmtpHost', ''), False); Row := Row + 36;
  AddLabel(SmtpConfigPage, Row, W, 'SMTP Port:', False); Row := Row + 20;
  SmtpPortEdit := AddEdit(SmtpConfigPage, Row, W div 4, GetSavedValue('SmtpPort', '587'), False); Row := Row + 36;
  SmtpAuthCheckbox := AddCheckbox(SmtpConfigPage, Row, 'Enable SMTP authentication', True); Row := Row + 26;
  SmtpStartTlsCheckbox := AddCheckbox(SmtpConfigPage, Row, 'Enable STARTTLS', True); Row := Row + 36;
  AddLabel(SmtpConfigPage, Row, W, 'SMTP Username:', False); Row := Row + 20;
  SmtpUsernameEdit := AddEdit(SmtpConfigPage, Row, W, GetSavedValue('SmtpUsername', ''), False); Row := Row + 36;
  AddLabel(SmtpConfigPage, Row, W, 'SMTP Password:', False); Row := Row + 20;
  SmtpPasswordEdit := AddEdit(SmtpConfigPage, Row, W, '', True);

  // --- OAuth2 config ---
  OAuthConfigPage := CreateCustomPage(SmtpConfigPage.ID,
    'OAuth2 Configuration', 'Microsoft 365 / Azure AD settings');
  Row := 8;
  AddLabel(OAuthConfigPage, Row, W, 'Tenant ID (Directory ID):', False); Row := Row + 20;
  OAuthTenantEdit := AddEdit(OAuthConfigPage, Row, W, GetSavedValue('OAuthTenant', ''), False); Row := Row + 36;
  AddLabel(OAuthConfigPage, Row, W, 'Client ID (Application ID):', False); Row := Row + 20;
  OAuthClientIdEdit := AddEdit(OAuthConfigPage, Row, W, GetSavedValue('OAuthClientId', ''), False); Row := Row + 36;
  AddLabel(OAuthConfigPage, Row, W, 'Client Secret:', False); Row := Row + 20;
  OAuthClientSecretEdit := AddEdit(OAuthConfigPage, Row, W, '', True); Row := Row + 36;
  AddLabel(OAuthConfigPage, Row, W, 'Sender UPN (from user):', False); Row := Row + 20;
  OAuthFromUserEdit := AddEdit(OAuthConfigPage, Row, W, GetSavedValue('OAuthFromUser', ''), False); Row := Row + 36;
  AddLabel(OAuthConfigPage, Row, W, 'OAuth2 Scope:', False); Row := Row + 20;
  OAuthScopeEdit := AddEdit(OAuthConfigPage, Row, W, 'https://graph.microsoft.com/.default', False);

  // --- Email recipients ---
  EmailRecipientsPage := CreateCustomPage(OAuthConfigPage.ID,
    'Email Recipients', 'Who should receive monitoring alerts?');
  Row := 8;
  AddLabel(EmailRecipientsPage, Row, W, 'From address:', False); Row := Row + 20;
  EmailFromEdit := AddEdit(EmailRecipientsPage, Row, W, GetSavedValue('EmailFrom', ''), False); Row := Row + 36;
  AddLabel(EmailRecipientsPage, Row, W, 'To address(es) — comma-separated:', False); Row := Row + 20;
  EmailToEdit := AddEdit(EmailRecipientsPage, Row, W, GetSavedValue('EmailTo', ''), False); Row := Row + 36;
  AddLabel(EmailRecipientsPage, Row, W, 'BCC (optional):', False); Row := Row + 20;
  EmailBccEdit := AddEdit(EmailRecipientsPage, Row, W, '', False); Row := Row + 36;
  AddLabel(EmailRecipientsPage, Row, W, 'Client name in subject (optional):', False); Row := Row + 20;
  EmailClientNameEdit := AddEdit(EmailRecipientsPage, Row, W, GetSavedValue('ClientName', ''), False); Row := Row + 36;
  AddLabel(EmailRecipientsPage, Row, W, 'Email importance:', False); Row := Row + 20;
  EmailImportanceCombo := TNewComboBox.Create(EmailRecipientsPage);
  EmailImportanceCombo.Parent := EmailRecipientsPage.Surface;
  EmailImportanceCombo.Top := Row; EmailImportanceCombo.Left := 0;
  EmailImportanceCombo.Width := W div 3; EmailImportanceCombo.Style := csDropDownList;
  EmailImportanceCombo.Items.Add('Normal'); EmailImportanceCombo.Items.Add('High'); EmailImportanceCombo.Items.Add('Low');
  EmailImportanceCombo.ItemIndex := 0;

  // --- Service ports: 2-column layout to avoid overflow (11 monitors) ---
  ServicePortsPage := CreateCustomPage(EmailRecipientsPage.ID,
    'Metrics Ports', 'Assign a unique port for each selected monitor''s Prometheus metrics endpoint');
  // Column positions
  // Col1: label at 0, edit at ColW+8. Col2: label at HalfW+8, edit at HalfW+8+ColW+8
  // Each row-pair height: 20 (label) + 30 (edit) + 8 (gap) = 58px => 6 rows fit safely
  Row := 4;
  // Row 1
  AddLabel(ServicePortsPage, Row,         W div 2 - 8, 'IBMIFSErrorMonitor:',      False);
  AddLabel(ServicePortsPage, Row, W div 2 - 8, 'IBMRealTimeIFSMonitor:', False).Left := W div 2 + 8;
  Row := Row + 20;
  IFSErrorMonitorPortEdit    := AddEdit(ServicePortsPage, Row, W div 4, GetSavedValue('IFSErrorMonitorPort',    '3010'), False);
  RealTimeIFSMonitorPortEdit := AddEdit(ServicePortsPage, Row, W div 4, GetSavedValue('RealTimeIFSMonitorPort', '3011'), False);
  RealTimeIFSMonitorPortEdit.Left := W div 2 + 8;
  Row := Row + 32;
  // Row 2
  AddLabel(ServicePortsPage, Row, W div 2 - 8, 'IBMJobQueCountMonitor:',  False);
  AddLabel(ServicePortsPage, Row, W div 2 - 8, 'IBMJobQueStatusMonitor:', False).Left := W div 2 + 8;
  Row := Row + 20;
  JobQueCountMonitorPortEdit  := AddEdit(ServicePortsPage, Row, W div 4, GetSavedValue('JobQueCountMonitorPort',  '3012'), False);
  JobQueStatusMonitorPortEdit := AddEdit(ServicePortsPage, Row, W div 4, GetSavedValue('JobQueStatusMonitorPort', '3013'), False);
  JobQueStatusMonitorPortEdit.Left := W div 2 + 8;
  Row := Row + 32;
  // Row 3
  AddLabel(ServicePortsPage, Row, W div 2 - 8, 'IBMSubSystemMonitor:', False);
  AddLabel(ServicePortsPage, Row, W div 2 - 8, 'IBMMatrixMonitor:',    False).Left := W div 2 + 8;
  Row := Row + 20;
  SubSystemMonitorPortEdit := AddEdit(ServicePortsPage, Row, W div 4, GetSavedValue('SubSystemMonitorPort', '3014'), False);
  SystemMatrixPortEdit     := AddEdit(ServicePortsPage, Row, W div 4, GetSavedValue('SystemMatrixPort',     '3015'), False);
  SystemMatrixPortEdit.Left := W div 2 + 8;
  Row := Row + 32;
  // Row 4
  AddLabel(ServicePortsPage, Row, W div 2 - 8, 'IBMQSYSOPRMonitor:',    False);
  AddLabel(ServicePortsPage, Row, W div 2 - 8, 'IBMFileMemberMonitor:', False).Left := W div 2 + 8;
  Row := Row + 20;
  QSYSOPRMonitorPortEdit    := AddEdit(ServicePortsPage, Row, W div 4, GetSavedValue('QSYSOPRMonitorPort',    '3016'), False);
  FileMemberMonitorPortEdit := AddEdit(ServicePortsPage, Row, W div 4, GetSavedValue('FileMemberMonitorPort', '3017'), False);
  FileMemberMonitorPortEdit.Left := W div 2 + 8;
  Row := Row + 32;
  // Row 6
  AddLabel(ServicePortsPage, Row, W div 2 - 8, 'IBMJobStatusMonitor:', False);
  Row := Row + 20;
  JobStatusMonitorPortEdit := AddEdit(ServicePortsPage, Row, W div 4, GetSavedValue('JobStatusMonitorPort', '3018'), False);

end;

// =============================================================================
// Page skip logic
// =============================================================================
function ShouldSkipPage(PageID: Integer): Boolean;
begin
  Result := False;
  if PageID = SmtpConfigPage.ID  then Result := (GetEmailAuthMethod('') = 'OAUTH2');
  if PageID = OAuthConfigPage.ID then Result := (GetEmailAuthMethod('') = 'SMTP');
end;

// =============================================================================
// Validation
// =============================================================================
function NextButtonClick(CurPageID: Integer): Boolean;
var Msg: string;
begin
  Result := True;
  if CurPageID = IBMiConfigPage.ID then
  begin
    if IBMiServerEdit.Text   = '' then begin MsgBox('IBM i Host is required.',     mbError, MB_OK); Result := False; Exit; end;
    if IBMiUserEdit.Text     = '' then begin MsgBox('IBM i User is required.',     mbError, MB_OK); Result := False; Exit; end;
    if IBMiPasswordEdit.Text = '' then begin MsgBox('IBM i Password is required.', mbError, MB_OK); Result := False; Exit; end;
  end;
  if CurPageID = EmailRecipientsPage.ID then
  begin
    if EmailFromEdit.Text = '' then begin MsgBox('From address is required.', mbError, MB_OK); Result := False; Exit; end;
    if EmailToEdit.Text   = '' then begin MsgBox('To address is required.',   mbError, MB_OK); Result := False; Exit; end;
  end;
  if CurPageID = ServicePortsPage.ID then
  begin
    Msg := '';
    if ChkIFSErrorMonitor.Checked     and not IsValidPort(IFSErrorMonitorPortEdit.Text)     then Msg := Msg + 'IBMIFSErrorMonitor port is invalid.'#13#10;
    if ChkRealTimeIFSMonitor.Checked  and not IsValidPort(RealTimeIFSMonitorPortEdit.Text)  then Msg := Msg + 'IBMRealTimeIFSMonitor port is invalid.'#13#10;
    if ChkJobQueCountMonitor.Checked  and not IsValidPort(JobQueCountMonitorPortEdit.Text)  then Msg := Msg + 'IBMJobQueCountMonitor port is invalid.'#13#10;
    if ChkJobQueStatusMonitor.Checked and not IsValidPort(JobQueStatusMonitorPortEdit.Text) then Msg := Msg + 'IBMJobQueStatusMonitor port is invalid.'#13#10;
    if ChkSubSystemMonitor.Checked    and not IsValidPort(SubSystemMonitorPortEdit.Text)    then Msg := Msg + 'IBMSubSystemMonitor port is invalid.'#13#10;
    if ChkSystemMatrix.Checked        and not IsValidPort(SystemMatrixPortEdit.Text)        then Msg := Msg + 'IBMMatrixMonitor port is invalid.'#13#10;
    if ChkQSYSOPRMonitor.Checked      and not IsValidPort(QSYSOPRMonitorPortEdit.Text)      then Msg := Msg + 'IBMQSYSOPRMonitor port is invalid.'#13#10;
    if ChkFileMemberMonitor.Checked   and not IsValidPort(FileMemberMonitorPortEdit.Text)   then Msg := Msg + 'IBMFileMemberMonitor port is invalid.'#13#10;
    if ChkJobStatusMonitor.Checked    and not IsValidPort(JobStatusMonitorPortEdit.Text)    then Msg := Msg + 'IBMJobStatusMonitor port is invalid.'#13#10;
    if Msg <> '' then begin MsgBox(Msg, mbError, MB_OK); Result := False; end;
  end;
end;

// =============================================================================
// Prometheus target file generator
// =============================================================================
procedure GeneratePrometheusTargetsFile;
var
  D, OutFile, Content, ClientLabel: string;
begin
  D := ExpandConstant('{app}');
  OutFile := D + '\prometheus_ibmi_targets.yml';
  ClientLabel := ClientNamePage.Values[0];
  if ClientLabel = '' then ClientLabel := 'ibmi';

  Content :=
    '# ============================================================='#13#10 +
    '# IBM i Prometheus scrape targets'#13#10 +
    '# Generated by Island Pacific IBM i Monitoring Agent installer'#13#10 +
    '# Copy the job blocks below into the scrape_configs section'#13#10 +
    '# of your prometheus.yml on the monitoring server.'#13#10 +
    '# ============================================================='#13#10 +
    '#'#13#10 +
    '# scrape_configs:'#13#10#13#10;

  if ChkIFSErrorMonitor.Checked then
    Content := Content +
      '  - job_name: "ibmi_ifs_error_monitor"'#13#10 +
      '    static_configs:'#13#10 +
      '      - targets: ["<THIS_MACHINE_IP>:' + IFSErrorMonitorPortEdit.Text + '"]'#13#10 +
      '        labels:'#13#10 +
      '          client: "' + ClientLabel + '"'#13#10#13#10;

  if ChkRealTimeIFSMonitor.Checked then
    Content := Content +
      '  - job_name: "ibmi_realtime_ifs_monitor"'#13#10 +
      '    static_configs:'#13#10 +
      '      - targets: ["<THIS_MACHINE_IP>:' + RealTimeIFSMonitorPortEdit.Text + '"]'#13#10 +
      '        labels:'#13#10 +
      '          client: "' + ClientLabel + '"'#13#10#13#10;

  if ChkJobQueCountMonitor.Checked then
    Content := Content +
      '  - job_name: "ibmi_jobque_count_monitor"'#13#10 +
      '    static_configs:'#13#10 +
      '      - targets: ["<THIS_MACHINE_IP>:' + JobQueCountMonitorPortEdit.Text + '"]'#13#10 +
      '        labels:'#13#10 +
      '          client: "' + ClientLabel + '"'#13#10#13#10;

  if ChkJobQueStatusMonitor.Checked then
    Content := Content +
      '  - job_name: "ibmi_jobque_status_monitor"'#13#10 +
      '    static_configs:'#13#10 +
      '      - targets: ["<THIS_MACHINE_IP>:' + JobQueStatusMonitorPortEdit.Text + '"]'#13#10 +
      '        labels:'#13#10 +
      '          client: "' + ClientLabel + '"'#13#10#13#10;

  if ChkSubSystemMonitor.Checked then
    Content := Content +
      '  - job_name: "ibmi_subsystem_monitor"'#13#10 +
      '    static_configs:'#13#10 +
      '      - targets: ["<THIS_MACHINE_IP>:' + SubSystemMonitorPortEdit.Text + '"]'#13#10 +
      '        labels:'#13#10 +
      '          client: "' + ClientLabel + '"'#13#10#13#10;

  if ChkSystemMatrix.Checked then
    Content := Content +
      '  - job_name: "ibmi_matrix_monitor"'#13#10 +
      '    static_configs:'#13#10 +
      '      - targets: ["<THIS_MACHINE_IP>:' + SystemMatrixPortEdit.Text + '"]'#13#10 +
      '        labels:'#13#10 +
      '          client: "' + ClientLabel + '"'#13#10#13#10;

  if ChkQSYSOPRMonitor.Checked then
    Content := Content +
      '  - job_name: "ibmi_qsysopr_monitor"'#13#10 +
      '    static_configs:'#13#10 +
      '      - targets: ["<THIS_MACHINE_IP>:' + QSYSOPRMonitorPortEdit.Text + '"]'#13#10 +
      '        labels:'#13#10 +
      '          client: "' + ClientLabel + '"'#13#10#13#10;

  if ChkFileMemberMonitor.Checked then
    Content := Content +
      '  - job_name: "ibmi_file_member_monitor"'#13#10 +
      '    static_configs:'#13#10 +
      '      - targets: ["<THIS_MACHINE_IP>:' + FileMemberMonitorPortEdit.Text + '"]'#13#10 +
      '        labels:'#13#10 +
      '          client: "' + ClientLabel + '"'#13#10#13#10;


  if ChkJobStatusMonitor.Checked then
    Content := Content +
      '  - job_name: "ibmi_job_status_monitor"'#13#10 +
      '    static_configs:'#13#10 +
      '      - targets: ["<THIS_MACHINE_IP>:' + JobStatusMonitorPortEdit.Text + '"]'#13#10 +
      '        labels:'#13#10 +
      '          client: "' + ClientLabel + '"'#13#10#13#10;

  SaveStringToFile(OutFile, Content, False);
  Log('Prometheus targets written to: ' + OutFile);
end;

// =============================================================================
// CurStepChanged — install services at ssPostInstall
// =============================================================================
// Stop any selected monitor's service BEFORE [Files] copies new JAR/exe content,
// otherwise an upgrade can silently fail to overwrite a locked file.
procedure StopAllServicesBeforeFileCopy;
begin
  if ChkIFSErrorMonitor.Checked    then StopAndUninstallService('IPMonitoring_IBMIFSErrorMonitor.exe');
  if ChkRealTimeIFSMonitor.Checked then StopAndUninstallService('IPMonitoring_IBMRealTimeIFSMonitor.exe');
  if ChkJobQueCountMonitor.Checked then StopAndUninstallService('IPMonitoring_IBMJobQueCountMonitor.exe');
  if ChkJobQueStatusMonitor.Checked then StopAndUninstallService('IPMonitoring_IBMJobQueStatusMonitor.exe');
  if ChkSubSystemMonitor.Checked   then StopAndUninstallService('IPMonitoring_IBMSubSystemMonitor.exe');
  if ChkSystemMatrix.Checked       then StopAndUninstallService('IPMonitoring_IBMMatrixMonitor.exe');
  if ChkQSYSOPRMonitor.Checked     then StopAndUninstallService('IPMonitoring_IBMQSYSOPRMonitor.exe');
  if ChkFileMemberMonitor.Checked  then StopAndUninstallService('IPMonitoring_IBMFileMemberMonitor.exe');
  if ChkNetWorkEnabler.Checked     then StopAndUninstallService('IPMonitoring_IBMNetworkEnabler.exe');
  if ChkUserProfileChecker.Checked then StopAndUninstallService('IPMonitoring_IBMUserProfileChecker.exe');
  if ChkJobStatusMonitor.Checked   then StopAndUninstallService('IPMonitoring_IBMJobStatusMonitor.exe');
end;

procedure CurStepChanged(CurStep: TSetupStep);
var D: string;
begin
  if CurStep = ssInstall then
  begin
    StopAllServicesBeforeFileCopy;
    Exit;
  end;
  if CurStep <> ssPostInstall then Exit;
  D := ExpandConstant('{app}');

  if ChkIFSErrorMonitor.Checked then
    InstallIBMiMonitor('IBMIFSErrorMonitor', 'IPMonitoring_IBMIFSErrorMonitor',
      'IP IBM IFS Error Monitor', 'Monitors IBM i IFS folder file counts via SMB',
      'IBMIFSErrorMonitor.jar', 'ibmrealtimeifsmonitor.properties', IFSErrorMonitorPortEdit.Text,
      'ibmi.host', 'ibmi.user', 'ibmi.password', True);

  if ChkRealTimeIFSMonitor.Checked then
    InstallIBMiMonitor('IBMRealTimeIFSMonitor', 'IPMonitoring_IBMRealTimeIFSMonitor',
      'IP IBM Real-Time IFS Monitor', 'Monitors IBM i IFS for error detection',
      'IBMRealTimeIFSMonitor.jar', 'ibmifsmonitor.properties', RealTimeIFSMonitorPortEdit.Text,
      'ibmi.host', 'ibmi.user', 'ibmi.password', True);

  if ChkJobQueCountMonitor.Checked then
    InstallIBMiMonitor('IBMJobQueCountMonitor', 'IPMonitoring_IBMJobQueCountMonitor',
      'IP IBM Job Queue Count Monitor', 'Monitors IBM i job queue waiting-job counts',
      'IBMJobQueCountMonitor.jar', 'ibmjobqueuemonitor.properties', JobQueCountMonitorPortEdit.Text,
      'ibmi.host', 'ibmi.user', 'ibmi.password', True);

  if ChkJobQueStatusMonitor.Checked then
    InstallIBMiMonitor('IBMJobQueStatusMonitor', 'IPMonitoring_IBMJobQueStatusMonitor',
      'IP IBM Job Queue Status Monitor', 'Monitors IBM i job queue status changes',
      'IBMJobQueStatusMonitor.jar', 'ibmjobquestatusmonitor.properties', JobQueStatusMonitorPortEdit.Text,
      'ibmi.host', 'ibmi.user', 'ibmi.password', True);

  if ChkSubSystemMonitor.Checked then
    InstallIBMiMonitor('IBMSubSystemMonitoring', 'IPMonitoring_IBMSubSystemMonitor',
      'IP IBM SubSystem Monitor', 'Monitors IBM i subsystem status',
      'IBMSubSystemMonitor.jar', 'ibmsubsystemmonitor.properties', SubSystemMonitorPortEdit.Text,
      'ibmi.host', 'ibmi.user', 'ibmi.password', True);

  if ChkSystemMatrix.Checked then
    InstallIBMiMonitor('IBMSystemMatrix', 'IPMonitoring_IBMMatrixMonitor',
      'IP IBM Matrix Monitor', 'Monitors IBM i system matrix and performance',
      'IBMMatrixMonitor.jar', 'ibmmatrixmonitor.properties', SystemMatrixPortEdit.Text,
      'ibmi.host', 'ibmi.user', 'ibmi.password', True);

  if ChkQSYSOPRMonitor.Checked then
    InstallIBMiMonitor('IBMQSYSOPRMonitor', 'IPMonitoring_IBMQSYSOPRMonitor',
      'IP IBM QSYSOPR Monitor', 'Monitors IBM i QSYSOPR operator message queue',
      'IBMQSYSOPRMonitor.jar', 'ibmqsysoprmonitor.properties', QSYSOPRMonitorPortEdit.Text,
      'ibmi.host', 'ibmi.user', 'ibmi.password', True);

  if ChkFileMemberMonitor.Checked then
    InstallIBMiMonitor('IBMFileMemberMonitor', 'IPMonitoring_IBMFileMemberMonitor',
      'IP IBM File Member Monitor', 'Monitors IBM i physical file member record counts',
      'IBMFileMemberMonitor.jar', 'ibmfilemembermonitor.properties', FileMemberMonitorPortEdit.Text,
      'ibmi.host', 'ibmi.user', 'ibmi.password', True);

  if ChkNetWorkEnabler.Checked then
    InstallIBMiMonitor('IBMNetworkEnabler', 'IPMonitoring_IBMNetworkEnabler',
      'IP IBM Network Enabler', 'Monitors IBM i network connectivity',
      'IBMNetworkEnabler.jar', 'ibmnetworkenabler.properties', '',
      'ibmi.host', 'ibmi.user', 'ibmi.password', True);

  if ChkUserProfileChecker.Checked then
    InstallIBMiMonitor('IBMUserProfileChecker', 'IPMonitoring_IBMUserProfileChecker',
      'IP IBM User Profile Checker', 'Validates IBM i user profiles',
      'IBMUserProfileChecker.jar', 'ibmuserprofilechecker.properties', '',
      'ibmi.host', 'ibmi.user', 'ibmi.password', True);

  if ChkJobStatusMonitor.Checked then
    InstallIBMiMonitor('IBMJobStatusMonitor', 'IPMonitoring_IBMJobStatusMonitor',
      'IP IBM Job Status Monitor', 'Monitors IBM i jobs in MSGW status',
      'IBMJobStatusMonitor.jar', 'ibmjobstatusmonitor.properties', JobStatusMonitorPortEdit.Text,
      'ibmi.host', 'ibmi.user', 'ibmi.password', True);


  GeneratePrometheusTargetsFile;
end;

// =============================================================================
// Uninstall — stop all services
// =============================================================================
procedure CurUninstallStepChanged(CurUninstallStep: TUninstallStep);
begin
  if CurUninstallStep <> usUninstall then Exit;
  StopAndUninstallService('IPMonitoring_IBMIFSErrorMonitor.exe');
  StopAndUninstallService('IPMonitoring_IBMRealTimeIFSMonitor.exe');
  StopAndUninstallService('IPMonitoring_IBMJobQueCountMonitor.exe');
  StopAndUninstallService('IPMonitoring_IBMJobQueStatusMonitor.exe');
  StopAndUninstallService('IPMonitoring_IBMSubSystemMonitor.exe');
  StopAndUninstallService('IPMonitoring_IBMMatrixMonitor.exe');
  StopAndUninstallService('IPMonitoring_IBMQSYSOPRMonitor.exe');
  StopAndUninstallService('IPMonitoring_IBMFileMemberMonitor.exe');
  StopAndUninstallService('IPMonitoring_IBMNetworkEnabler.exe');
  StopAndUninstallService('IPMonitoring_IBMUserProfileChecker.exe');
  StopAndUninstallService('IPMonitoring_IBMJobStatusMonitor.exe');
end;
