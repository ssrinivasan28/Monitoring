#define AppName "Island Pacific Windows Monitoring Agent"
#define AppVersion "1.0.0"
#define AppPublisher "Island Pacific Retail Systems"
#define AppRegKey "Software\IslandPacific\WinMonitoringAgent"

[Setup]
AppId={{IP-WinMonitoringAgent}}
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
VersionInfoDescription=Island Pacific Windows Monitoring Agent Installer

ArchitecturesInstallIn64BitMode=x64compatible
DefaultDirName={commonpf}\Island Pacific\Windows Monitoring Agent
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

AppMutex=IP_WinMonitoringAgent_Mutex
CloseApplications=no

Compression=lzma
SolidCompression=yes

OutputDir=.\installer\output
OutputBaseFilename=WindowsMonitoringAgentSetup

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Messages]
WelcomeLabel1=Welcome to the Island Pacific Windows Monitoring Agent
WelcomeLabel2=This wizard will install the Island Pacific Windows Monitoring Agent on your server.%n%nThe following monitoring services are available:%n%n  • WinMonitor           — CPU, memory, disk and Windows services%n  • WinFSErrorMonitor    — Error file detection in watched folders%n  • WinFSCardinalityMonitor — File count threshold alerting%n  • LogKeywordMonitor    — Application log keyword scanning%n  • ServerUpTimeMonitor  — Network reachability and ping health%n%nAll agents run as Windows services and send email alerts when action is needed.%n%nClick Next to continue, or Cancel to exit.
FinishedHeadingLabel=Installation Complete
FinishedLabel=The Island Pacific Windows Monitoring Agent has been installed successfully.%n%nInstalled services are now running and monitoring your Windows environment.%n%nEach agent writes daily log files to its log folder. You can verify service status at any time with:%n%n  sc query IPMonitoring_WinMonitor%n  sc query IPMonitoring_WinFSErrorMonitor%n  sc query IPMonitoring_WinFSCardinalityMonitor%n  sc query IPMonitoring_LogKeywordMonitor%n  sc query IPMonitoring_ServerUpTimeMonitor%n%nClick Finish to close this wizard.
FinishedLabelNoIcons=The Island Pacific Windows Monitoring Agent has been installed. Selected monitoring services are now active.
ClickFinish=Installation is complete. Click Finish to close this wizard.

[Dirs]
Name: "{app}\services"
Name: "{app}\monitoring-services"
Name: "{app}\logs"; Flags: uninsneveruninstall

[Files]
; WinSW wrappers - one per service
Source: "installer\resources\WinSW.exe"; DestDir: "{app}\services"; DestName: "IPMonitoring_WinMonitor.exe"; Flags: ignoreversion
Source: "installer\resources\WinSW.exe"; DestDir: "{app}\services"; DestName: "IPMonitoring_WinFSErrorMonitor.exe"; Flags: ignoreversion
Source: "installer\resources\WinSW.exe"; DestDir: "{app}\services"; DestName: "IPMonitoring_WinFSCardinalityMonitor.exe"; Flags: ignoreversion
Source: "installer\resources\WinSW.exe"; DestDir: "{app}\services"; DestName: "IPMonitoring_LogKeywordMonitor.exe"; Flags: ignoreversion
Source: "installer\resources\WinSW.exe"; DestDir: "{app}\services"; DestName: "IPMonitoring_ServerUpTimeMonitor.exe"; Flags: ignoreversion

; JAR files (always updated on upgrade)
Source: "installer\resources\monitoring-services\WinMonitor\*.jar"; DestDir: "{app}\monitoring-services\WinMonitor"; Flags: ignoreversion; Check: IsWinMonitorSelected
Source: "installer\resources\monitoring-services\WinFSErrorMonitor\*.jar"; DestDir: "{app}\monitoring-services\WinFSErrorMonitor"; Flags: ignoreversion; Check: IsWinFSErrorMonitorSelected
Source: "installer\resources\monitoring-services\WinFSCardinalityMonitor\*.jar"; DestDir: "{app}\monitoring-services\WinFSCardinalityMonitor"; Flags: ignoreversion; Check: IsWinFSCardinalityMonitorSelected
Source: "installer\resources\monitoring-services\LogKeywordMonitor\*.jar"; DestDir: "{app}\monitoring-services\LogKeywordMonitor"; Flags: ignoreversion; Check: IsLogKeywordMonitorSelected
Source: "installer\resources\monitoring-services\ServerUpTimeMonitor\*.jar"; DestDir: "{app}\monitoring-services\ServerUpTimeMonitor"; Flags: ignoreversion; Check: IsServerUpTimeMonitorSelected

; Properties files - only written if not already present (preserves user config on upgrade)
Source: "installer\resources\monitoring-services\WinMonitor\*.properties"; DestDir: "{app}\monitoring-services\WinMonitor"; Excludes: "email.properties"; Flags: onlyifdoesntexist skipifsourcedoesntexist; Check: IsWinMonitorSelected
Source: "installer\resources\monitoring-services\WinFSErrorMonitor\*.properties"; DestDir: "{app}\monitoring-services\WinFSErrorMonitor"; Excludes: "email.properties"; Flags: onlyifdoesntexist skipifsourcedoesntexist; Check: IsWinFSErrorMonitorSelected
Source: "installer\resources\monitoring-services\WinFSCardinalityMonitor\*.properties"; DestDir: "{app}\monitoring-services\WinFSCardinalityMonitor"; Excludes: "email.properties"; Flags: onlyifdoesntexist skipifsourcedoesntexist; Check: IsWinFSCardinalityMonitorSelected
Source: "installer\resources\monitoring-services\LogKeywordMonitor\*.properties"; DestDir: "{app}\monitoring-services\LogKeywordMonitor"; Excludes: "email.properties"; Flags: onlyifdoesntexist skipifsourcedoesntexist; Check: IsLogKeywordMonitorSelected
Source: "logkeywordmonitor.properties"; DestDir: "{app}\monitoring-services\LogKeywordMonitor"; Flags: onlyifdoesntexist skipifsourcedoesntexist; Check: IsLogKeywordMonitorSelected
Source: "installer\resources\monitoring-services\ServerUpTimeMonitor\*.properties"; DestDir: "{app}\monitoring-services\ServerUpTimeMonitor"; Excludes: "email.properties"; Flags: onlyifdoesntexist skipifsourcedoesntexist; Check: IsServerUpTimeMonitorSelected

[Registry]
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "InstallPath"; ValueData: "{app}"; Flags: uninsdeletekey
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "Version"; ValueData: "{#AppVersion}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "ClientName"; ValueData: "{code:GetClientName}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "EmailAuthMethod"; ValueData: "{code:GetEmailAuthMethod}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "SmtpHost"; ValueData: "{code:GetSmtpHost}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "SmtpPort"; ValueData: "{code:GetSmtpPort}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "SmtpUsername"; ValueData: "{code:GetSmtpUsername}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "SmtpAuth"; ValueData: "{code:GetSmtpAuth}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "SmtpStartTls"; ValueData: "{code:GetSmtpStartTls}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "OAuthTenant"; ValueData: "{code:GetOAuthTenant}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "OAuthClientId"; ValueData: "{code:GetOAuthClientId}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "OAuthFromUser"; ValueData: "{code:GetOAuthFromUser}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "EmailFrom"; ValueData: "{code:GetEmailFrom}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "EmailTo"; ValueData: "{code:GetEmailTo}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "WinMonitorPort"; ValueData: "{code:GetWinMonitorPort}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "WinFSErrorMonitorPort"; ValueData: "{code:GetWinFSErrorMonitorPort}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "WinFSCardinalityMonitorPort"; ValueData: "{code:GetWinFSCardinalityMonitorPort}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "LogKeywordMonitorPort"; ValueData: "{code:GetLogKeywordMonitorPort}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "ServerUpTimeMonitorPort"; ValueData: "{code:GetServerUpTimeMonitorPort}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "InstalledWinMonitor"; ValueData: "{code:GetInstalledWinMonitor}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "InstalledWinFSErrorMonitor"; ValueData: "{code:GetInstalledWinFSErrorMonitor}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "InstalledWinFSCardinalityMonitor"; ValueData: "{code:GetInstalledWinFSCardinalityMonitor}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "InstalledLogKeywordMonitor"; ValueData: "{code:GetInstalledLogKeywordMonitor}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "InstalledServerUpTimeMonitor"; ValueData: "{code:GetInstalledServerUpTimeMonitor}"; Flags: uninsdeletevalue

[UninstallDelete]
Type: files; Name: "{app}\services\*.xml"
Type: files; Name: "{app}\monitoring-services\*\email.properties"
Type: dirifempty; Name: "{app}\monitoring-services\WinMonitor"
Type: dirifempty; Name: "{app}\monitoring-services\WinFSErrorMonitor"
Type: dirifempty; Name: "{app}\monitoring-services\WinFSCardinalityMonitor"
Type: dirifempty; Name: "{app}\monitoring-services\LogKeywordMonitor"
Type: dirifempty; Name: "{app}\monitoring-services\ServerUpTimeMonitor"
Type: dirifempty; Name: "{app}\monitoring-services"
Type: dirifempty; Name: "{app}\services"
Type: dirifempty; Name: "{app}"

[Code]

// =============================================================================
// Global variables
// =============================================================================
var
  // ---- Wizard pages ----
  ClientNamePage:        TInputQueryWizardPage;  // Client/site name
  MonitorSelectPage:     TWizardPage;            // Which monitors to install
  EmailAuthPage:         TInputOptionWizardPage; // SMTP or OAuth2
  SmtpConfigPage:        TWizardPage;            // SMTP settings
  OAuthConfigPage:       TWizardPage;            // OAuth2 settings
  EmailRecipientsPage:   TWizardPage;            // From/To/BCC
  ServicePortsPage:      TWizardPage;            // Metrics ports

  // ---- Monitor selection checkboxes ----
  ChkWinMonitor:             TNewCheckBox;
  ChkWinFSErrorMonitor:      TNewCheckBox;
  ChkWinFSCardinalityMonitor: TNewCheckBox;
  ChkLogKeywordMonitor:      TNewCheckBox;
  ChkServerUpTimeMonitor:    TNewCheckBox;

  // ---- SMTP page controls ----
  SmtpHostEdit, SmtpPortEdit, SmtpUsernameEdit, SmtpPasswordEdit: TNewEdit;
  SmtpAuthCheckbox, SmtpStartTlsCheckbox: TNewCheckBox;

  // ---- OAuth2 page controls ----
  OAuthTenantEdit, OAuthClientIdEdit, OAuthClientSecretEdit: TNewEdit;
  OAuthScopeEdit, OAuthFromUserEdit: TNewEdit;

  // ---- Email recipients page controls ----
  EmailFromEdit, EmailToEdit, EmailBccEdit, EmailClientNameEdit: TNewEdit;
  EmailImportanceCombo: TNewComboBox;

  // ---- Port page controls ----
  WinMonitorPortEdit:             TNewEdit;
  WinFSErrorMonitorPortEdit:      TNewEdit;
  WinFSCardinalityMonitorPortEdit: TNewEdit;
  LogKeywordMonitorPortEdit:      TNewEdit;
  ServerUpTimeMonitorPortEdit:    TNewEdit;

  // ---- Port page labels (for visibility control) ----
  LblWinMonitorPort:             TNewStaticText;
  LblWinFSErrorMonitorPort:      TNewStaticText;
  LblWinFSCardinalityMonitorPort: TNewStaticText;
  LblLogKeywordMonitorPort:      TNewStaticText;
  LblServerUpTimeMonitorPort:    TNewStaticText;

  // ---- State ----
  IsUpgrade: Boolean;

// =============================================================================
// Upgrade detection
// =============================================================================
function CheckIsUpgrade: Boolean;
var
  Path: string;
begin
  Result := RegQueryStringValue(HKLM, '{#AppRegKey}', 'InstallPath', Path) and (Path <> '');
end;

// =============================================================================
// Registry helpers — read previously saved values as defaults
// =============================================================================
function GetSavedValue(const Name, Default: string): string;
begin
  if not RegQueryStringValue(HKLM, '{#AppRegKey}', Name, Result) then
    Result := Default;
end;

// =============================================================================
// Port validation
// =============================================================================
function IsValidPort(const s: string): Boolean;
var
  N: Integer;
begin
  N := StrToIntDef(s, 0);
  Result := (N >= 1024) and (N <= 65535);
end;

// =============================================================================
// Port conflict check (bind attempt)
// =============================================================================
function IsPortInUse(Port: string): Boolean;
begin
  // Simple heuristic: skip check during upgrades (our own services hold the ports)
  if IsUpgrade then
  begin
    Result := False;
    Exit;
  end;
  Result := False; // In production, extend with actual socket check if needed
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
  if not LoadStringsFromFile(ServicePath + '\email.properties', Lines) then
    Exit;

  for i := 0 to GetArrayLength(Lines) - 1 do
  begin
    if Copy(Lines[i], 1, PrefixLen) = Prefix then
    begin
      Result := Copy(Lines[i], PrefixLen + 1, Length(Lines[i]) - PrefixLen);
      Exit;
    end;
  end;
end;

// =============================================================================
// Accessor functions (used in [Registry] section)
// =============================================================================
function GetClientName(Param: string): string;
begin
  Result := ClientNamePage.Values[0];
end;

function GetEmailAuthMethod(Param: string): string;
begin
  if EmailAuthPage.SelectedValueIndex = 0 then Result := 'SMTP'
  else Result := 'OAUTH2';
end;

function GetSmtpHost(Param: string): string;       begin Result := SmtpHostEdit.Text; end;
function GetSmtpPort(Param: string): string;       begin Result := SmtpPortEdit.Text; end;
function GetSmtpUsername(Param: string): string;   begin Result := SmtpUsernameEdit.Text; end;
function GetSmtpPassword(Param: string): string;   begin Result := SmtpPasswordEdit.Text; end;
function GetSmtpAuth(Param: string): string;
begin
  if SmtpAuthCheckbox.Checked then Result := 'true' else Result := 'false';
end;
function GetSmtpStartTls(Param: string): string;
begin
  if SmtpStartTlsCheckbox.Checked then Result := 'true' else Result := 'false';
end;

function GetOAuthTenant(Param: string): string;      begin Result := OAuthTenantEdit.Text; end;
function GetOAuthClientId(Param: string): string;    begin Result := OAuthClientIdEdit.Text; end;
function GetOAuthClientSecret(Param: string): string; begin Result := OAuthClientSecretEdit.Text; end;
function GetOAuthScope(Param: string): string;        begin Result := OAuthScopeEdit.Text; end;
function GetOAuthFromUser(Param: string): string;     begin Result := OAuthFromUserEdit.Text; end;
function GetOAuthTokenUrl(Param: string): string;
begin
  if OAuthTenantEdit.Text <> '' then
    Result := 'https://login.microsoftonline.com/' + OAuthTenantEdit.Text + '/oauth2/v2.0/token'
  else
    Result := '';
end;
function GetOAuthMailUrl(Param: string): string;
begin
  if OAuthFromUserEdit.Text <> '' then
    Result := 'https://graph.microsoft.com/v1.0/users/' + OAuthFromUserEdit.Text + '/sendMail'
  else
    Result := 'https://graph.microsoft.com/v1.0/me/sendMail';
end;

function GetEmailFrom(Param: string): string;         begin Result := EmailFromEdit.Text; end;
function GetEmailTo(Param: string): string;           begin Result := EmailToEdit.Text; end;
function GetEmailBcc(Param: string): string;          begin Result := EmailBccEdit.Text; end;
function GetClientNameEmail(Param: string): string;   begin Result := EmailClientNameEdit.Text; end;
function GetEmailImportance(Param: string): string;
begin
  Result := EmailImportanceCombo.Items[EmailImportanceCombo.ItemIndex];
end;

function GetWinMonitorPort(Param: string): string;             begin Result := WinMonitorPortEdit.Text; end;
function GetWinFSErrorMonitorPort(Param: string): string;      begin Result := WinFSErrorMonitorPortEdit.Text; end;
function GetWinFSCardinalityMonitorPort(Param: string): string; begin Result := WinFSCardinalityMonitorPortEdit.Text; end;
function GetLogKeywordMonitorPort(Param: string): string;      begin Result := LogKeywordMonitorPortEdit.Text; end;
function GetServerUpTimeMonitorPort(Param: string): string;    begin Result := ServerUpTimeMonitorPortEdit.Text; end;

function GetInstalledWinMonitor(Param: string): string;
begin if ChkWinMonitor.Checked then Result := 'true' else Result := 'false'; end;
function GetInstalledWinFSErrorMonitor(Param: string): string;
begin if ChkWinFSErrorMonitor.Checked then Result := 'true' else Result := 'false'; end;
function GetInstalledWinFSCardinalityMonitor(Param: string): string;
begin if ChkWinFSCardinalityMonitor.Checked then Result := 'true' else Result := 'false'; end;
function GetInstalledLogKeywordMonitor(Param: string): string;
begin if ChkLogKeywordMonitor.Checked then Result := 'true' else Result := 'false'; end;
function GetInstalledServerUpTimeMonitor(Param: string): string;
begin if ChkServerUpTimeMonitor.Checked then Result := 'true' else Result := 'false'; end;

// =============================================================================
// [Check] functions used in [Files] section
// =============================================================================
function IsWinMonitorSelected: Boolean;            begin Result := ChkWinMonitor.Checked; end;
function IsWinFSErrorMonitorSelected: Boolean;     begin Result := ChkWinFSErrorMonitor.Checked; end;
function IsWinFSCardinalityMonitorSelected: Boolean; begin Result := ChkWinFSCardinalityMonitor.Checked; end;
function IsLogKeywordMonitorSelected: Boolean;     begin Result := ChkLogKeywordMonitor.Checked; end;
function IsServerUpTimeMonitorSelected: Boolean;   begin Result := ChkServerUpTimeMonitor.Checked; end;

// =============================================================================
// Helper: write email.properties into a monitor service folder
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

  Content := '# ===============================' + CRLF;
  Content := Content + '# Email Configuration' + CRLF;
  Content := Content + '# ===============================' + CRLF + CRLF;
  Content := Content + '# Authentication Method: SMTP or OAUTH2' + CRLF;
  Content := Content + 'mail.auth.method=' + AuthMethod + CRLF + CRLF;
  Content := Content + '# SMTP server details' + CRLF;
  Content := Content + 'mail.smtp.host=' + GetSmtpHost('') + CRLF;
  Content := Content + 'mail.smtp.port=' + GetSmtpPort('') + CRLF + CRLF;
  Content := Content + '# SMTP Authentication' + CRLF;
  Content := Content + 'mail.smtp.auth=' + GetSmtpAuth('') + CRLF;
  Content := Content + 'mail.smtp.starttls.enable=' + GetSmtpStartTls('') + CRLF;
  Content := Content + 'mail.smtp.username=' + GetSmtpUsername('') + CRLF;
  Content := Content + 'mail.smtp.password=' + SmtpPassword + CRLF + CRLF;
  Content := Content + '# OAuth2 Configuration (Microsoft 365)' + CRLF;
  Content := Content + 'mail.oauth2.tenant.id=' + GetOAuthTenant('') + CRLF;
  Content := Content + 'mail.oauth2.client.id=' + GetOAuthClientId('') + CRLF;
  Content := Content + 'mail.oauth2.client.secret=' + OAuthClientSecret + CRLF;
  Content := Content + 'mail.oauth2.scope=' + GetOAuthScope('') + CRLF;
  Content := Content + 'mail.oauth2.token.url=' + GetOAuthTokenUrl('') + CRLF;
  Content := Content + 'mail.oauth2.graph.mail.url=' + GetOAuthMailUrl('') + CRLF;
  Content := Content + 'mail.oauth2.from.user=' + GetOAuthFromUser('') + CRLF + CRLF;
  Content := Content + '# Sender and recipients' + CRLF;
  Content := Content + 'mail.from=' + GetEmailFrom('') + CRLF;
  Content := Content + 'mail.to=' + GetEmailTo('') + CRLF;
  Content := Content + 'mail.bcc=' + GetEmailBcc('') + CRLF + CRLF;
  Content := Content + '# Email importance (High, Normal, Low)' + CRLF;
  Content := Content + 'mail.importance=' + GetEmailImportance('') + CRLF + CRLF;
  Content := Content + '# Client name shown in alert subject lines' + CRLF;
  Content := Content + 'mail.clientName=' + GetClientNameEmail('') + CRLF + CRLF;
  Content := Content + '# ===============================' + CRLF;
  Content := Content + '# Logging Configuration' + CRLF;
  Content := Content + '# ===============================' + CRLF;
  Content := Content + 'log.retention.days=30' + CRLF;
  Content := Content + 'log.purge.interval.hours=24' + CRLF;

  SaveStringToFile(EmailFile, Content, False);
end;

// =============================================================================
// Helper: write WinSW service XML
// =============================================================================
procedure GenerateServiceXml(ServiceId, ServiceName, Description, Executable, Arguments, WorkDir: string);
var
  D: string;
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
    '  <logpath>' + XmlEscape(D + '\logs') + '</logpath>'#13#10 +
    '  <onfailure action="restart" delay="10 sec"/>'#13#10 +
    '  <onfailure action="restart" delay="20 sec"/>'#13#10 +
    '  <onfailure action="restart" delay="30 sec"/>'#13#10 +
    '  <resetfailure>1 hour</resetfailure>'#13#10 +
    '</service>', False);
end;

// =============================================================================
// Helper: update a key=value line in a properties file
// =============================================================================
procedure UpdatePropsKey(PropsFile, Key, Value: string);
var
  Lines: TArrayOfString;
  i: Integer;
  Line, Output: string;
  Updated: Boolean;
begin
  if not LoadStringsFromFile(PropsFile, Lines) then Exit;
  Output := '';
  Updated := False;
  for i := 0 to GetArrayLength(Lines) - 1 do
  begin
    Line := Lines[i];
    if Pos(Key + '=', Line) = 1 then
    begin
      Line := Key + '=' + Value;
      Updated := True;
    end;
    Output := Output + Line + #13#10;
  end;
  if not Updated then
    Output := Output + Key + '=' + Value + #13#10;
  SaveStringToFile(PropsFile, Output, False);
end;

procedure UpdateMonitorPort(FolderName, PropsFile, Port: string);
var
  PortKey: string;
begin
  if FolderName = 'WinMonitor' then
    PortKey := 'metrics.exporter.port'
  else if FolderName = 'ServerUpTimeMonitor' then
    PortKey := 'exporter.port'
  else
    PortKey := 'metrics.port';

  UpdatePropsKey(PropsFile, PortKey, Port);
end;

// =============================================================================
// Helper: install and start a service via WinSW
// =============================================================================
procedure InstallAndStartService(ServiceExe: string);
var
  D, ExePath, ServiceId: string;
  ResultCode: Integer;
begin
  D := ExpandConstant('{app}');
  ExePath := D + '\services\' + ServiceExe;
  if not FileExists(ExePath) then
  begin
    Log('InstallAndStartService: EXE not found: ' + ExePath);
    Exit;
  end;
  ServiceId := Copy(ServiceExe, 1, Length(ServiceExe) - 4);

  if ServiceExists(ServiceId) then
  begin
    Exec(ExePath, 'stop', D + '\services', SW_HIDE, ewWaitUntilTerminated, ResultCode);
    Sleep(1000);
    Exec(ExePath, 'uninstall', D + '\services', SW_HIDE, ewWaitUntilTerminated, ResultCode);
    Sleep(1000);
  end;

  if Exec(ExePath, 'install', D + '\services', SW_HIDE, ewWaitUntilTerminated, ResultCode) and (ResultCode = 0) then
  begin
    if not Exec(ExePath, 'start', D + '\services', SW_HIDE, ewWaitUntilTerminated, ResultCode) then
      Log('InstallAndStartService: start failed for ' + ServiceExe + ' (code ' + IntToStr(ResultCode) + ')');
  end
  else
    Log('InstallAndStartService: install failed for ' + ServiceExe + ' (code ' + IntToStr(ResultCode) + ')');
end;

// =============================================================================
// Helper: stop and uninstall a service gracefully
// =============================================================================
procedure StopAndUninstallService(ServiceExe: string);
var
  D, ExePath: string;
  ResultCode: Integer;
begin
  D := ExpandConstant('{app}');
  ExePath := D + '\services\' + ServiceExe;
  if not FileExists(ExePath) then Exit;
  Exec(ExePath, 'stop', D + '\services', SW_HIDE, ewWaitUntilTerminated, ResultCode);
  Exec(ExePath, 'uninstall', D + '\services', SW_HIDE, ewWaitUntilTerminated, ResultCode);
end;

// =============================================================================
// Helper: install a single Windows monitor
// =============================================================================
procedure InstallWinMonitor(FolderName, ServiceId, DisplayName, Description,
    JarName, PropsFile, Port: string);
var
  D, ServicePath: string;
begin
  D := ExpandConstant('{app}');
  ServicePath := D + '\monitoring-services\' + FolderName;

  // Update metrics port in properties file
  UpdateMonitorPort(FolderName, ServicePath + '\' + PropsFile, Port);

  // Update client name
  UpdatePropsKey(ServicePath + '\' + PropsFile, 'client.name', GetClientName(''));

  // Write email configuration
  GenerateEmailProperties(ServicePath);

  // Generate WinSW service XML
  GenerateServiceXml(ServiceId, DisplayName, Description,
    'java', '-jar "' + ServicePath + '\' + JarName + '"', ServicePath);

  // Install and start the Windows service
  InstallAndStartService(ServiceId + '.exe');
end;

// =============================================================================
// Wizard page layout helpers
// =============================================================================
function AddLabel(Page: TWizardPage; Top, Width: Integer; Caption: string; Bold: Boolean): TNewStaticText;
begin
  Result := TNewStaticText.Create(Page);
  Result.Parent := Page.Surface;
  Result.Top := Top;
  Result.Left := 0;
  Result.Width := Width;
  Result.Caption := Caption;
  Result.AutoSize := True;
  if Bold then
    Result.Font.Style := [fsBold];
end;

function AddEdit(Page: TWizardPage; Top, Width: Integer; DefaultValue: string; Password: Boolean): TNewEdit;
begin
  Result := TNewEdit.Create(Page);
  Result.Parent := Page.Surface;
  Result.Top := Top;
  Result.Left := 0;
  Result.Width := Width;
  Result.Text := DefaultValue;
  if Password then
    Result.PasswordChar := '*';
end;

function AddCheckBox(Page: TWizardPage; Top: Integer; Caption: string; Checked: Boolean): TNewCheckBox;
begin
  Result := TNewCheckBox.Create(Page);
  Result.Parent := Page.Surface;
  Result.Top := Top;
  Result.Left := 0;
  Result.Width := Page.SurfaceWidth;
  Result.Caption := Caption;
  Result.Checked := Checked;
end;

// =============================================================================
// CreateCustomPages — builds all wizard pages
// =============================================================================
procedure CreateCustomPages;
var
  Page: TWizardPage;
  Lbl: TNewStaticText;
  SW: Integer;
begin
  SW := WizardForm.InnerPage.Width - ScaleX(16);

  // ------------------------------------------------------------------
  // 1. Client Name page
  // ------------------------------------------------------------------
  ClientNamePage := CreateInputQueryPage(wpWelcome,
    'Site Identification',
    'Identify this monitoring installation',
    'Enter a name for this site or client. This label appears in all alert email subject lines so you can identify which server sent an alert when monitoring multiple sites.');
  ClientNamePage.Add('Client / Site Name:', False);
  ClientNamePage.Values[0] := GetSavedValue('ClientName', 'My Windows Server');

  // ------------------------------------------------------------------
  // 2. Monitor selection page
  // ------------------------------------------------------------------
  Page := CreateCustomPage(ClientNamePage.ID,
    'Select Monitoring Agents',
    'Choose which Windows monitors to install on this server');
  MonitorSelectPage := Page;

  Lbl := AddLabel(Page, 0, SW,
    'All selected monitors will be installed as Windows services and started automatically.', False);

  ChkWinMonitor := AddCheckBox(Page, Lbl.Top + Lbl.Height + ScaleY(16),
    'WinMonitor  —  CPU, memory, disk usage and critical Windows services', True);
  AddLabel(Page, ChkWinMonitor.Top + ChkWinMonitor.Height + ScaleY(2), SW,
    '    Alerts after 3 consecutive threshold breaches (~15 min at default interval)', False);

  ChkWinFSErrorMonitor := AddCheckBox(Page,
    ChkWinMonitor.Top + ChkWinMonitor.Height + ScaleY(24),
    'WinFSErrorMonitor  —  Detects error files appearing in watched folders', True);
  AddLabel(Page, ChkWinFSErrorMonitor.Top + ChkWinFSErrorMonitor.Height + ScaleY(2), SW,
    '    Immediate alert on first detection of each new file (.err, .wrn, .dmp, etc.)', False);

  ChkWinFSCardinalityMonitor := AddCheckBox(Page,
    ChkWinFSErrorMonitor.Top + ChkWinFSErrorMonitor.Height + ScaleY(24),
    'WinFSCardinalityMonitor  —  File count threshold monitoring in watched folders', True);
  AddLabel(Page, ChkWinFSCardinalityMonitor.Top + ChkWinFSCardinalityMonitor.Height + ScaleY(2), SW,
    '    Alerts when file counts go above max or below min for 3 consecutive checks', False);

  ChkLogKeywordMonitor := AddCheckBox(Page,
    ChkWinFSCardinalityMonitor.Top + ChkWinFSCardinalityMonitor.Height + ScaleY(24),
    'LogKeywordMonitor  —  Scans application log files for error keywords', True);
  AddLabel(Page, ChkLogKeywordMonitor.Top + ChkLogKeywordMonitor.Height + ScaleY(2), SW,
    '    Reads incrementally — never re-processes content already seen', False);

  ChkServerUpTimeMonitor := AddCheckBox(Page,
    ChkLogKeywordMonitor.Top + ChkLogKeywordMonitor.Height + ScaleY(24),
    'ServerUpTimeMonitor  —  Pings servers to detect outages and recovery', True);
  AddLabel(Page, ChkServerUpTimeMonitor.Top + ChkServerUpTimeMonitor.Height + ScaleY(2), SW,
    '    Immediate DOWN alert when a server goes unreachable; UP alert on recovery', False);

  // ------------------------------------------------------------------
  // 3. Email authentication method
  // ------------------------------------------------------------------
  EmailAuthPage := CreateInputOptionPage(MonitorSelectPage.ID,
    'Email Delivery Method',
    'Choose how alert emails are sent from this server',
    'Select the email delivery method that matches your organisation''s setup.',
    True, False);
  EmailAuthPage.Add('SMTP  —  Standard mail server (internal relay, Gmail, etc.)');
  EmailAuthPage.Add('Microsoft 365 (OAuth2)  —  Azure AD application credentials; no user password required');
  EmailAuthPage.SelectedValueIndex := 0;

  // ------------------------------------------------------------------
  // 4. SMTP configuration page
  // ------------------------------------------------------------------
  Page := CreateCustomPage(EmailAuthPage.ID,
    'SMTP Server Configuration',
    'Enter your outgoing mail server settings');
  SmtpConfigPage := Page;

  AddLabel(Page, 0, SW, 'SMTP Host:', False);
  SmtpHostEdit := AddEdit(Page, ScaleY(18), SW, GetSavedValue('SmtpHost', ''), False);

  AddLabel(Page, SmtpHostEdit.Top + SmtpHostEdit.Height + ScaleY(10), SW, 'SMTP Port:', False);
  SmtpPortEdit := AddEdit(Page, SmtpHostEdit.Top + SmtpHostEdit.Height + ScaleY(28), SW div 3,
    GetSavedValue('SmtpPort', '587'), False);

  SmtpAuthCheckbox := AddCheckBox(Page,
    SmtpPortEdit.Top + SmtpPortEdit.Height + ScaleY(14),
    'Enable SMTP Authentication (username and password)', True);

  SmtpStartTlsCheckbox := AddCheckBox(Page,
    SmtpAuthCheckbox.Top + SmtpAuthCheckbox.Height + ScaleY(6),
    'Enable STARTTLS (recommended for authenticated connections)', True);

  AddLabel(Page, SmtpStartTlsCheckbox.Top + SmtpStartTlsCheckbox.Height + ScaleY(12), SW, 'SMTP Username:', False);
  SmtpUsernameEdit := AddEdit(Page,
    SmtpStartTlsCheckbox.Top + SmtpStartTlsCheckbox.Height + ScaleY(30), SW,
    GetSavedValue('SmtpUsername', ''), False);

  AddLabel(Page, SmtpUsernameEdit.Top + SmtpUsernameEdit.Height + ScaleY(10), SW, 'SMTP Password:', False);
  SmtpPasswordEdit := AddEdit(Page,
    SmtpUsernameEdit.Top + SmtpUsernameEdit.Height + ScaleY(28), SW, '', True);

  // ------------------------------------------------------------------
  // 5. OAuth2 configuration page
  // ------------------------------------------------------------------
  Page := CreateCustomPage(EmailAuthPage.ID,
    'Microsoft 365 OAuth2 Configuration',
    'Enter your Azure Active Directory application credentials');
  OAuthConfigPage := Page;

  AddLabel(Page, 0, SW, 'Azure AD Tenant ID:', False);
  OAuthTenantEdit := AddEdit(Page, ScaleY(18), SW, GetSavedValue('OAuthTenant', ''), False);

  AddLabel(Page, OAuthTenantEdit.Top + OAuthTenantEdit.Height + ScaleY(10), SW,
    'Application (Client) ID:', False);
  OAuthClientIdEdit := AddEdit(Page,
    OAuthTenantEdit.Top + OAuthTenantEdit.Height + ScaleY(28), SW,
    GetSavedValue('OAuthClientId', ''), False);

  AddLabel(Page, OAuthClientIdEdit.Top + OAuthClientIdEdit.Height + ScaleY(10), SW,
    'Client Secret:', False);
  OAuthClientSecretEdit := AddEdit(Page,
    OAuthClientIdEdit.Top + OAuthClientIdEdit.Height + ScaleY(28), SW, '', True);

  AddLabel(Page, OAuthClientSecretEdit.Top + OAuthClientSecretEdit.Height + ScaleY(10), SW,
    'OAuth2 Scope (leave blank for default: https://graph.microsoft.com/.default):', False);
  OAuthScopeEdit := AddEdit(Page,
    OAuthClientSecretEdit.Top + OAuthClientSecretEdit.Height + ScaleY(28), SW,
    GetSavedValue('OAuthScope', 'https://graph.microsoft.com/.default'), False);

  AddLabel(Page, OAuthScopeEdit.Top + OAuthScopeEdit.Height + ScaleY(10), SW,
    'Send-As Mailbox (e.g. monitoring@yourcompany.com):', False);
  OAuthFromUserEdit := AddEdit(Page,
    OAuthScopeEdit.Top + OAuthScopeEdit.Height + ScaleY(28), SW,
    GetSavedValue('OAuthFromUser', ''), False);

  // ------------------------------------------------------------------
  // 6. Email recipients page
  // ------------------------------------------------------------------
  Page := CreateCustomPage(OAuthConfigPage.ID,
    'Alert Email Recipients',
    'Configure who receives monitoring alerts');
  EmailRecipientsPage := Page;

  AddLabel(Page, 0, SW, 'From Address:', False);
  EmailFromEdit := AddEdit(Page, ScaleY(18), SW, GetSavedValue('EmailFrom', ''), False);

  AddLabel(Page, EmailFromEdit.Top + EmailFromEdit.Height + ScaleY(10), SW,
    'To Address(es)  (separate multiple addresses with commas):', False);
  EmailToEdit := AddEdit(Page,
    EmailFromEdit.Top + EmailFromEdit.Height + ScaleY(28), SW,
    GetSavedValue('EmailTo', ''), False);

  AddLabel(Page, EmailToEdit.Top + EmailToEdit.Height + ScaleY(10), SW,
    'BCC Address(es)  (optional):', False);
  EmailBccEdit := AddEdit(Page,
    EmailToEdit.Top + EmailToEdit.Height + ScaleY(28), SW,
    GetSavedValue('EmailBcc', ''), False);

  AddLabel(Page, EmailBccEdit.Top + EmailBccEdit.Height + ScaleY(10), SW,
    'Client / Site Name in Subject Line:', False);
  EmailClientNameEdit := AddEdit(Page,
    EmailBccEdit.Top + EmailBccEdit.Height + ScaleY(28), SW,
    GetSavedValue('ClientName', 'My Windows Server'), False);

  AddLabel(Page, EmailClientNameEdit.Top + EmailClientNameEdit.Height + ScaleY(10), SW,
    'Email Importance:', False);
  EmailImportanceCombo := TNewComboBox.Create(Page);
  EmailImportanceCombo.Parent := Page.Surface;
  EmailImportanceCombo.Top := EmailClientNameEdit.Top + EmailClientNameEdit.Height + ScaleY(28);
  EmailImportanceCombo.Left := 0;
  EmailImportanceCombo.Width := SW div 2;
  EmailImportanceCombo.Style := csDropDownList;
  EmailImportanceCombo.Items.Add('High');
  EmailImportanceCombo.Items.Add('Normal');
  EmailImportanceCombo.Items.Add('Low');
  EmailImportanceCombo.ItemIndex := 0;

  // ------------------------------------------------------------------
  // 7. Service metrics ports page
  // ------------------------------------------------------------------
  Page := CreateCustomPage(EmailRecipientsPage.ID,
    'Monitoring Metrics Ports',
    'Assign a network port for each monitoring agent''s internal metrics endpoint');
  ServicePortsPage := Page;

  AddLabel(Page, 0, SW,
    'Each installed agent exposes live metrics on a local HTTP port (e.g. http://localhost:3022/metrics). ' +
    'These ports must be free on this server. Only ports for selected agents are used.', False);

  LblWinMonitorPort := TNewStaticText.Create(Page);
  LblWinMonitorPort.Parent := Page.Surface;
  LblWinMonitorPort.Top := ScaleY(48);
  LblWinMonitorPort.Left := 0;
  LblWinMonitorPort.Width := Round(SW * 0.68);
  LblWinMonitorPort.Caption := 'WinMonitor port:';
  LblWinMonitorPort.AutoSize := True;

  WinMonitorPortEdit := TNewEdit.Create(Page);
  WinMonitorPortEdit.Parent := Page.Surface;
  WinMonitorPortEdit.Top := LblWinMonitorPort.Top;
  WinMonitorPortEdit.Left := Round(SW * 0.70);
  WinMonitorPortEdit.Width := Round(SW * 0.30);
  WinMonitorPortEdit.Text := GetSavedValue('WinMonitorPort', '3022');

  LblWinFSErrorMonitorPort := TNewStaticText.Create(Page);
  LblWinFSErrorMonitorPort.Parent := Page.Surface;
  LblWinFSErrorMonitorPort.Top := WinMonitorPortEdit.Top + WinMonitorPortEdit.Height + ScaleY(8);
  LblWinFSErrorMonitorPort.Left := 0;
  LblWinFSErrorMonitorPort.Width := Round(SW * 0.68);
  LblWinFSErrorMonitorPort.Caption := 'WinFSErrorMonitor port:';
  LblWinFSErrorMonitorPort.AutoSize := True;

  WinFSErrorMonitorPortEdit := TNewEdit.Create(Page);
  WinFSErrorMonitorPortEdit.Parent := Page.Surface;
  WinFSErrorMonitorPortEdit.Top := LblWinFSErrorMonitorPort.Top;
  WinFSErrorMonitorPortEdit.Left := Round(SW * 0.70);
  WinFSErrorMonitorPortEdit.Width := Round(SW * 0.30);
  WinFSErrorMonitorPortEdit.Text := GetSavedValue('WinFSErrorMonitorPort', '3020');

  LblWinFSCardinalityMonitorPort := TNewStaticText.Create(Page);
  LblWinFSCardinalityMonitorPort.Parent := Page.Surface;
  LblWinFSCardinalityMonitorPort.Top := WinFSErrorMonitorPortEdit.Top + WinFSErrorMonitorPortEdit.Height + ScaleY(8);
  LblWinFSCardinalityMonitorPort.Left := 0;
  LblWinFSCardinalityMonitorPort.Width := Round(SW * 0.68);
  LblWinFSCardinalityMonitorPort.Caption := 'WinFSCardinalityMonitor port:';
  LblWinFSCardinalityMonitorPort.AutoSize := True;

  WinFSCardinalityMonitorPortEdit := TNewEdit.Create(Page);
  WinFSCardinalityMonitorPortEdit.Parent := Page.Surface;
  WinFSCardinalityMonitorPortEdit.Top := LblWinFSCardinalityMonitorPort.Top;
  WinFSCardinalityMonitorPortEdit.Left := Round(SW * 0.70);
  WinFSCardinalityMonitorPortEdit.Width := Round(SW * 0.30);
  WinFSCardinalityMonitorPortEdit.Text := GetSavedValue('WinFSCardinalityMonitorPort', '3021');

  LblLogKeywordMonitorPort := TNewStaticText.Create(Page);
  LblLogKeywordMonitorPort.Parent := Page.Surface;
  LblLogKeywordMonitorPort.Top := WinFSCardinalityMonitorPortEdit.Top + WinFSCardinalityMonitorPortEdit.Height + ScaleY(8);
  LblLogKeywordMonitorPort.Left := 0;
  LblLogKeywordMonitorPort.Width := Round(SW * 0.68);
  LblLogKeywordMonitorPort.Caption := 'LogKeywordMonitor port:';
  LblLogKeywordMonitorPort.AutoSize := True;

  LogKeywordMonitorPortEdit := TNewEdit.Create(Page);
  LogKeywordMonitorPortEdit.Parent := Page.Surface;
  LogKeywordMonitorPortEdit.Top := LblLogKeywordMonitorPort.Top;
  LogKeywordMonitorPortEdit.Left := Round(SW * 0.70);
  LogKeywordMonitorPortEdit.Width := Round(SW * 0.30);
  LogKeywordMonitorPortEdit.Text := GetSavedValue('LogKeywordMonitorPort', '3023');

  LblServerUpTimeMonitorPort := TNewStaticText.Create(Page);
  LblServerUpTimeMonitorPort.Parent := Page.Surface;
  LblServerUpTimeMonitorPort.Top := LogKeywordMonitorPortEdit.Top + LogKeywordMonitorPortEdit.Height + ScaleY(8);
  LblServerUpTimeMonitorPort.Left := 0;
  LblServerUpTimeMonitorPort.Width := Round(SW * 0.68);
  LblServerUpTimeMonitorPort.Caption := 'ServerUpTimeMonitor port:';
  LblServerUpTimeMonitorPort.AutoSize := True;

  ServerUpTimeMonitorPortEdit := TNewEdit.Create(Page);
  ServerUpTimeMonitorPortEdit.Parent := Page.Surface;
  ServerUpTimeMonitorPortEdit.Top := LblServerUpTimeMonitorPort.Top;
  ServerUpTimeMonitorPortEdit.Left := Round(SW * 0.70);
  ServerUpTimeMonitorPortEdit.Width := Round(SW * 0.30);
  ServerUpTimeMonitorPortEdit.Text := GetSavedValue('ServerUpTimeMonitorPort', '3014');
end;

// =============================================================================
// InitializeWizard — called once at wizard startup
// =============================================================================
procedure InitializeWizard;
begin
  IsUpgrade := CheckIsUpgrade;
  CreateCustomPages;
end;

// =============================================================================
// ShouldSkipPage — skip SMTP or OAuth2 page based on selection
// =============================================================================
function ShouldSkipPage(PageID: Integer): Boolean;
begin
  Result := False;
  // Skip SMTP page when OAuth2 is selected
  if PageID = SmtpConfigPage.ID then
    Result := (EmailAuthPage.SelectedValueIndex <> 0);
  // Skip OAuth2 page when SMTP is selected
  if PageID = OAuthConfigPage.ID then
    Result := (EmailAuthPage.SelectedValueIndex <> 1);
  // Skip port page if no monitor is selected
  if PageID = ServicePortsPage.ID then
    Result := not (ChkWinMonitor.Checked or ChkWinFSErrorMonitor.Checked or
                   ChkWinFSCardinalityMonitor.Checked or ChkLogKeywordMonitor.Checked or
                   ChkServerUpTimeMonitor.Checked);
end;

// =============================================================================
// NextButtonClick — validation on each page
// =============================================================================
function NextButtonClick(CurPageID: Integer): Boolean;
var
  HasMonitor: Boolean;
begin
  Result := True;

  // At least one monitor must be selected
  if CurPageID = MonitorSelectPage.ID then
  begin
    HasMonitor := ChkWinMonitor.Checked or ChkWinFSErrorMonitor.Checked or
                  ChkWinFSCardinalityMonitor.Checked or ChkLogKeywordMonitor.Checked or
                  ChkServerUpTimeMonitor.Checked;
    if not HasMonitor then
    begin
      MsgBox('Please select at least one monitoring agent to install.', mbError, MB_OK);
      Result := False;
      Exit;
    end;
  end;

  // Email recipients — To is required
  if CurPageID = EmailRecipientsPage.ID then
  begin
    if Trim(EmailToEdit.Text) = '' then
    begin
      MsgBox('Please enter at least one recipient email address in the To field.', mbError, MB_OK);
      Result := False;
      Exit;
    end;
    if Trim(EmailFromEdit.Text) = '' then
    begin
      MsgBox('Please enter a From email address.', mbError, MB_OK);
      Result := False;
      Exit;
    end;
  end;

  // Validate port numbers
  if CurPageID = ServicePortsPage.ID then
  begin
    if ChkWinMonitor.Checked and not IsValidPort(WinMonitorPortEdit.Text) then
    begin
      MsgBox('WinMonitor port must be a number between 1024 and 65535.', mbError, MB_OK);
      Result := False; Exit;
    end;
    if ChkWinFSErrorMonitor.Checked and not IsValidPort(WinFSErrorMonitorPortEdit.Text) then
    begin
      MsgBox('WinFSErrorMonitor port must be a number between 1024 and 65535.', mbError, MB_OK);
      Result := False; Exit;
    end;
    if ChkWinFSCardinalityMonitor.Checked and not IsValidPort(WinFSCardinalityMonitorPortEdit.Text) then
    begin
      MsgBox('WinFSCardinalityMonitor port must be a number between 1024 and 65535.', mbError, MB_OK);
      Result := False; Exit;
    end;
    if ChkLogKeywordMonitor.Checked and not IsValidPort(LogKeywordMonitorPortEdit.Text) then
    begin
      MsgBox('LogKeywordMonitor port must be a number between 1024 and 65535.', mbError, MB_OK);
      Result := False; Exit;
    end;
    if ChkServerUpTimeMonitor.Checked and not IsValidPort(ServerUpTimeMonitorPortEdit.Text) then
    begin
      MsgBox('ServerUpTimeMonitor port must be a number between 1024 and 65535.', mbError, MB_OK);
      Result := False; Exit;
    end;
  end;
end;

// =============================================================================
// CurStepChanged — perform installation at ssPostInstall step
// =============================================================================
procedure CurStepChanged(CurStep: TSetupStep);
begin
  if CurStep = ssInstall then
  begin
    StopAndUninstallService('IPMonitoring_WinMonitor.exe');
    StopAndUninstallService('IPMonitoring_WinFSErrorMonitor.exe');
    StopAndUninstallService('IPMonitoring_WinFSCardinalityMonitor.exe');
    StopAndUninstallService('IPMonitoring_LogKeywordMonitor.exe');
    StopAndUninstallService('IPMonitoring_ServerUpTimeMonitor.exe');
    Exit;
  end;

  if CurStep <> ssPostInstall then Exit;

  if ChkWinMonitor.Checked then
    InstallWinMonitor(
      'WinMonitor', 'IPMonitoring_WinMonitor',
      'IP Monitoring - Windows System Monitor',
      'Monitors CPU, memory, disk and Windows service health',
      'WinMonitor.jar', 'windowsmonitor.properties',
      GetWinMonitorPort(''));

  if ChkWinFSErrorMonitor.Checked then
    InstallWinMonitor(
      'WinFSErrorMonitor', 'IPMonitoring_WinFSErrorMonitor',
      'IP Monitoring - Windows FS Error Monitor',
      'Detects error files appearing in watched folders',
      'WinFSErrorMonitor.jar', 'fserrormonitor.properties',
      GetWinFSErrorMonitorPort(''));

  if ChkWinFSCardinalityMonitor.Checked then
    InstallWinMonitor(
      'WinFSCardinalityMonitor', 'IPMonitoring_WinFSCardinalityMonitor',
      'IP Monitoring - Windows FS Cardinality Monitor',
      'Monitors file counts in folders against configured thresholds',
      'WinFSCardinalityMonitor.jar', 'fscardinalitymonitor.properties',
      GetWinFSCardinalityMonitorPort(''));

  if ChkLogKeywordMonitor.Checked then
    InstallWinMonitor(
      'LogKeywordMonitor', 'IPMonitoring_LogKeywordMonitor',
      'IP Monitoring - Log Keyword Monitor',
      'Scans application log files for configured error keywords',
      'LogKeywordMonitor.jar', 'logkeywordmonitor.properties',
      GetLogKeywordMonitorPort(''));

  if ChkServerUpTimeMonitor.Checked then
    InstallWinMonitor(
      'ServerUpTimeMonitor', 'IPMonitoring_ServerUpTimeMonitor',
      'IP Monitoring - Server UpTime Monitor',
      'Pings servers to detect outages and send recovery alerts',
      'ServerUpTimeMonitor.jar', 'serverinfo.properties',
      GetServerUpTimeMonitorPort(''));
end;

// =============================================================================
// CurUninstallStepChanged — stop and remove all services on uninstall
// =============================================================================
procedure CurUninstallStepChanged(CurUninstallStep: TUninstallStep);
begin
  if CurUninstallStep <> usUninstall then Exit;

  StopAndUninstallService('IPMonitoring_WinMonitor.exe');
  StopAndUninstallService('IPMonitoring_WinFSErrorMonitor.exe');
  StopAndUninstallService('IPMonitoring_WinFSCardinalityMonitor.exe');
  StopAndUninstallService('IPMonitoring_LogKeywordMonitor.exe');
  StopAndUninstallService('IPMonitoring_ServerUpTimeMonitor.exe');
end;
