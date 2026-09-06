#define AppName "Island Pacific Windows Server Monitor"
#define AppVersion "1.0.0"
#define AppPublisher "Island Pacific Retail Systems"
#define AppRegKey "Software\IslandPacific\WinMonitor"

[Setup]
AppId={{IP-WinMonitor-Standalone}}
AppName={#AppName}
AppVersion={#AppVersion}
AppPublisher={#AppPublisher}
AppVerName={#AppName} v{#AppVersion}
AppCopyright=Copyright © 2025 Island Pacific Retail Systems
AppSupportURL=https://www.islandpacific.com/
VersionInfoVersion=1.0.0.0
VersionInfoCompany=Island Pacific Retail Systems
VersionInfoProductName={#AppName}
VersionInfoDescription=Island Pacific Windows Server Monitor Installer

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

AppMutex=IP_WinMonitor_Standalone_Mutex
CloseApplications=no

Compression=lzma
SolidCompression=yes

OutputDir=.\installer\output
OutputBaseFilename=WinMonitorSetup

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Messages]
WelcomeLabel1=Welcome to the Island Pacific Windows Server Monitor
WelcomeLabel2=This wizard will install the Windows Server Monitor on your server.%n%nThis agent monitors:%n%n  • CPU utilisation%n  • Memory usage%n  • Disk space%n  • Windows service health%n  • Top processes%n%nThe agent runs as a Windows service and sends email alerts when thresholds are breached.%n%nClick Next to continue, or Cancel to exit.
FinishedHeadingLabel=Installation Complete
FinishedLabel=The Island Pacific Windows Server Monitor has been installed successfully.%n%nThe service is now running. Verify with:%n%n  sc query IPMonitoring_WinMonitor%n%nClick Finish to close this wizard.

[Dirs]
Name: "{app}\services"
Name: "{app}\monitoring-services"
Name: "{app}\monitoring-services\WinMonitor"
Name: "{app}\logs"; Flags: uninsneveruninstall

[Files]
Source: "installer\resources\WinSW.exe"; DestDir: "{app}\services"; DestName: "IPMonitoring_WinMonitor.exe"; Flags: ignoreversion
Source: "installer\resources\monitoring-services\WinMonitor\*.jar"; DestDir: "{app}\monitoring-services\WinMonitor"; Flags: ignoreversion
Source: "installer\resources\monitoring-services\WinMonitor\*.properties"; DestDir: "{app}\monitoring-services\WinMonitor"; Excludes: "email.properties"; Flags: onlyifdoesntexist skipifsourcedoesntexist
Source: "installer\resources\monitoring-services\CredTool\CredTool.jar"; DestDir: "{app}"; Flags: ignoreversion skipifsourcedoesntexist

[Registry]
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "InstallPath"; ValueData: "{app}"; Flags: uninsdeletekey
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "Version"; ValueData: "{#AppVersion}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "ClientName"; ValueData: "{code:GetClientName}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "EmailAuthMethod"; ValueData: "{code:GetEmailAuthMethod}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "SmtpHost"; ValueData: "{code:GetSmtpHost}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "SmtpPort"; ValueData: "{code:GetSmtpPort}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "SmtpUsername"; ValueData: "{code:GetSmtpUsername}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "OAuthTenant"; ValueData: "{code:GetOAuthTenant}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "OAuthClientId"; ValueData: "{code:GetOAuthClientId}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "OAuthFromUser"; ValueData: "{code:GetOAuthFromUser}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "EmailFrom"; ValueData: "{code:GetEmailFrom}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "EmailTo"; ValueData: "{code:GetEmailTo}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "{#AppRegKey}"; ValueType: string; ValueName: "WinMonitorPort"; ValueData: "{code:GetWinMonitorPort}"; Flags: uninsdeletevalue

[UninstallDelete]
Type: files; Name: "{app}\services\IPMonitoring_WinMonitor.xml"
Type: files; Name: "{app}\monitoring-services\WinMonitor\email.properties"
Type: dirifempty; Name: "{app}\monitoring-services\WinMonitor"
Type: dirifempty; Name: "{app}\monitoring-services"
Type: dirifempty; Name: "{app}\services"
Type: dirifempty; Name: "{app}"

[Code]

var
  ClientNamePage:      TInputQueryWizardPage;
  EmailAuthPage:       TInputOptionWizardPage;
  SmtpConfigPage:      TWizardPage;
  OAuthConfigPage:     TWizardPage;
  EmailRecipientsPage: TWizardPage;
  ServicePortsPage:    TWizardPage;

  SmtpHostEdit, SmtpPortEdit, SmtpUsernameEdit, SmtpPasswordEdit: TNewEdit;
  SmtpAuthCheckbox, SmtpStartTlsCheckbox: TNewCheckBox;

  OAuthTenantEdit, OAuthClientIdEdit, OAuthClientSecretEdit: TNewEdit;
  OAuthScopeEdit, OAuthFromUserEdit: TNewEdit;

  EmailFromEdit, EmailToEdit, EmailBccEdit, EmailClientNameEdit: TNewEdit;
  EmailImportanceCombo: TNewComboBox;

  WinMonitorPortEdit: TNewEdit;

  IsUpgrade: Boolean;

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
    Log('DPAPI encryption failed - storing value as entered');
  DeleteFile(OutFile);
  SetEnvironmentVariable('IP_DPAPI_VALUE', '');
end;

function GetClientName(Param: string): string;    begin Result := ClientNamePage.Values[0]; end;
function GetEmailAuthMethod(Param: string): string;
begin if EmailAuthPage.SelectedValueIndex = 0 then Result := 'SMTP' else Result := 'OAUTH2'; end;
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
function GetEmailFrom(Param: string): string;  begin Result := EmailFromEdit.Text; end;
function GetEmailTo(Param: string): string;    begin Result := EmailToEdit.Text; end;
function GetEmailBcc(Param: string): string;   begin Result := EmailBccEdit.Text; end;
function GetEmailImportance(Param: string): string;
begin Result := EmailImportanceCombo.Items[EmailImportanceCombo.ItemIndex]; end;
function GetWinMonitorPort(Param: string): string; begin Result := WinMonitorPortEdit.Text; end;

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
  Content := 'mail.auth.method=' + AuthMethod + CRLF;
  Content := Content + 'mail.smtp.host=' + GetSmtpHost('') + CRLF;
  Content := Content + 'mail.smtp.port=' + GetSmtpPort('') + CRLF;
  Content := Content + 'mail.smtp.auth=' + GetSmtpAuth('') + CRLF;
  Content := Content + 'mail.smtp.starttls.enable=' + GetSmtpStartTls('') + CRLF;
  Content := Content + 'mail.smtp.username=' + GetSmtpUsername('') + CRLF;
  Content := Content + 'mail.smtp.password=' + SmtpPassword + CRLF;
  Content := Content + 'mail.oauth2.tenant.id=' + GetOAuthTenant('') + CRLF;
  Content := Content + 'mail.oauth2.client.id=' + GetOAuthClientId('') + CRLF;
  Content := Content + 'mail.oauth2.client.secret=' + OAuthClientSecret + CRLF;
  Content := Content + 'mail.oauth2.scope=' + GetOAuthScope('') + CRLF;
  Content := Content + 'mail.oauth2.token.url=' + GetOAuthTokenUrl('') + CRLF;
  Content := Content + 'mail.oauth2.graph.mail.url=' + GetOAuthMailUrl('') + CRLF;
  Content := Content + 'mail.oauth2.from.user=' + GetOAuthFromUser('') + CRLF;
  Content := Content + 'mail.from=' + GetEmailFrom('') + CRLF;
  Content := Content + 'mail.to=' + GetEmailTo('') + CRLF;
  Content := Content + 'mail.bcc=' + GetEmailBcc('') + CRLF;
  Content := Content + 'mail.importance=' + GetEmailImportance('') + CRLF;
  Content := Content + 'mail.clientName=' + EmailClientNameEdit.Text + CRLF;
  Content := Content + 'log.retention.days=30' + CRLF;
  Content := Content + 'log.purge.interval.hours=24' + CRLF;
  SaveStringToFile(EmailFile, Content, False);
end;

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
    if Pos(Key + '=', Line) = 1 then begin Line := Key + '=' + Value; Updated := True; end;
    Output := Output + Line + #13#10;
  end;
  if not Updated then Output := Output + Key + '=' + Value + #13#10;
  SaveStringToFile(PropsFile, Output, False);
end;

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
    '  <logpath>' + XmlEscape(D + '\logs') + '</logpath>'#13#10 +
    '  <onfailure action="restart" delay="10 sec"/>'#13#10 +
    '  <onfailure action="restart" delay="20 sec"/>'#13#10 +
    '  <onfailure action="restart" delay="30 sec"/>'#13#10 +
    '  <resetfailure>1 hour</resetfailure>'#13#10 +
    '</service>', False);
end;

procedure InstallAndStartService(ServiceExe: string);
var
  D, ExePath, ServiceId: string;
  ResultCode: Integer;
begin
  D := ExpandConstant('{app}');
  ExePath := D + '\services\' + ServiceExe;
  if not FileExists(ExePath) then Exit;
  ServiceId := Copy(ServiceExe, 1, Length(ServiceExe) - 4);
  if ServiceExists(ServiceId) then
  begin
    Exec(ExePath, 'stop', D + '\services', SW_HIDE, ewWaitUntilTerminated, ResultCode);
    Sleep(1000);
    Exec(ExePath, 'uninstall', D + '\services', SW_HIDE, ewWaitUntilTerminated, ResultCode);
    Sleep(1000);
  end;
  if Exec(ExePath, 'install', D + '\services', SW_HIDE, ewWaitUntilTerminated, ResultCode) and (ResultCode = 0) then
    Exec(ExePath, 'start', D + '\services', SW_HIDE, ewWaitUntilTerminated, ResultCode);
end;

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

function AddCheckBox(Page: TWizardPage; Top: Integer; Caption: string; Checked: Boolean): TNewCheckBox;
begin
  Result := TNewCheckBox.Create(Page);
  Result.Parent := Page.Surface;
  Result.Top := Top; Result.Left := 0; Result.Width := Page.SurfaceWidth;
  Result.Caption := Caption; Result.Checked := Checked;
end;

procedure CreateCustomPages;
var
  Page: TWizardPage;
  SW, EditLeft, EditWidth: Integer;
begin
  SW := WizardForm.InnerPage.Width - ScaleX(16);

  ClientNamePage := CreateInputQueryPage(wpWelcome,
    'Site Identification', 'Identify this monitoring installation',
    'Enter a name for this site or client. This label appears in alert email subject lines.');
  ClientNamePage.Add('Client / Site Name:', False);
  ClientNamePage.Values[0] := GetSavedValue('ClientName', 'My Windows Server');

  EmailAuthPage := CreateInputOptionPage(ClientNamePage.ID,
    'Email Delivery Method', 'Choose how alert emails are sent',
    'Select the email delivery method that matches your organisation''s setup.', True, False);
  EmailAuthPage.Add('SMTP  —  Standard mail server');
  EmailAuthPage.Add('Microsoft 365 (OAuth2)  —  Azure AD application credentials');
  EmailAuthPage.SelectedValueIndex := 0;

  Page := CreateCustomPage(EmailAuthPage.ID, 'SMTP Server Configuration', 'Enter your outgoing mail server settings');
  SmtpConfigPage := Page;
  AddLabel(Page, 0, SW, 'SMTP Host:', False);
  SmtpHostEdit := AddEdit(Page, ScaleY(18), SW, GetSavedValue('SmtpHost', ''), False);
  AddLabel(Page, SmtpHostEdit.Top + SmtpHostEdit.Height + ScaleY(10), SW, 'SMTP Port:', False);
  SmtpPortEdit := AddEdit(Page, SmtpHostEdit.Top + SmtpHostEdit.Height + ScaleY(28), SW div 3, GetSavedValue('SmtpPort', '587'), False);
  SmtpAuthCheckbox := AddCheckBox(Page, SmtpPortEdit.Top + SmtpPortEdit.Height + ScaleY(14), 'Enable SMTP Authentication', True);
  SmtpStartTlsCheckbox := AddCheckBox(Page, SmtpAuthCheckbox.Top + SmtpAuthCheckbox.Height + ScaleY(6), 'Enable STARTTLS', True);
  AddLabel(Page, SmtpStartTlsCheckbox.Top + SmtpStartTlsCheckbox.Height + ScaleY(12), SW, 'SMTP Username:', False);
  SmtpUsernameEdit := AddEdit(Page, SmtpStartTlsCheckbox.Top + SmtpStartTlsCheckbox.Height + ScaleY(30), SW, GetSavedValue('SmtpUsername', ''), False);
  AddLabel(Page, SmtpUsernameEdit.Top + SmtpUsernameEdit.Height + ScaleY(10), SW, 'SMTP Password:', False);
  SmtpPasswordEdit := AddEdit(Page, SmtpUsernameEdit.Top + SmtpUsernameEdit.Height + ScaleY(28), SW, '', True);

  Page := CreateCustomPage(EmailAuthPage.ID, 'Microsoft 365 OAuth2 Configuration', 'Enter your Azure AD application credentials');
  OAuthConfigPage := Page;
  AddLabel(Page, 0, SW, 'Azure AD Tenant ID:', False);
  OAuthTenantEdit := AddEdit(Page, ScaleY(18), SW, GetSavedValue('OAuthTenant', ''), False);
  AddLabel(Page, OAuthTenantEdit.Top + OAuthTenantEdit.Height + ScaleY(10), SW, 'Application (Client) ID:', False);
  OAuthClientIdEdit := AddEdit(Page, OAuthTenantEdit.Top + OAuthTenantEdit.Height + ScaleY(28), SW, GetSavedValue('OAuthClientId', ''), False);
  AddLabel(Page, OAuthClientIdEdit.Top + OAuthClientIdEdit.Height + ScaleY(10), SW, 'Client Secret:', False);
  OAuthClientSecretEdit := AddEdit(Page, OAuthClientIdEdit.Top + OAuthClientIdEdit.Height + ScaleY(28), SW, '', True);
  AddLabel(Page, OAuthClientSecretEdit.Top + OAuthClientSecretEdit.Height + ScaleY(10), SW, 'OAuth2 Scope:', False);
  OAuthScopeEdit := AddEdit(Page, OAuthClientSecretEdit.Top + OAuthClientSecretEdit.Height + ScaleY(28), SW, 'https://graph.microsoft.com/.default', False);
  AddLabel(Page, OAuthScopeEdit.Top + OAuthScopeEdit.Height + ScaleY(10), SW, 'Send-As Mailbox:', False);
  OAuthFromUserEdit := AddEdit(Page, OAuthScopeEdit.Top + OAuthScopeEdit.Height + ScaleY(28), SW, GetSavedValue('OAuthFromUser', ''), False);

  Page := CreateCustomPage(OAuthConfigPage.ID, 'Alert Email Recipients', 'Configure who receives monitoring alerts');
  EmailRecipientsPage := Page;
  AddLabel(Page, 0, SW, 'From Address:', False);
  EmailFromEdit := AddEdit(Page, ScaleY(18), SW, GetSavedValue('EmailFrom', ''), False);
  AddLabel(Page, EmailFromEdit.Top + EmailFromEdit.Height + ScaleY(10), SW, 'To Address(es):', False);
  EmailToEdit := AddEdit(Page, EmailFromEdit.Top + EmailFromEdit.Height + ScaleY(28), SW, GetSavedValue('EmailTo', ''), False);
  AddLabel(Page, EmailToEdit.Top + EmailToEdit.Height + ScaleY(10), SW, 'BCC Address(es) (optional):', False);
  EmailBccEdit := AddEdit(Page, EmailToEdit.Top + EmailToEdit.Height + ScaleY(28), SW, '', False);
  AddLabel(Page, EmailBccEdit.Top + EmailBccEdit.Height + ScaleY(10), SW, 'Client / Site Name in Subject Line:', False);
  EmailClientNameEdit := AddEdit(Page, EmailBccEdit.Top + EmailBccEdit.Height + ScaleY(28), SW, GetSavedValue('ClientName', 'My Windows Server'), False);
  AddLabel(Page, EmailClientNameEdit.Top + EmailClientNameEdit.Height + ScaleY(10), SW, 'Email Importance:', False);
  EmailImportanceCombo := TNewComboBox.Create(Page);
  EmailImportanceCombo.Parent := Page.Surface;
  EmailImportanceCombo.Top := EmailClientNameEdit.Top + EmailClientNameEdit.Height + ScaleY(28);
  EmailImportanceCombo.Left := 0; EmailImportanceCombo.Width := SW div 2;
  EmailImportanceCombo.Style := csDropDownList;
  EmailImportanceCombo.Items.Add('High'); EmailImportanceCombo.Items.Add('Normal'); EmailImportanceCombo.Items.Add('Low');
  EmailImportanceCombo.ItemIndex := 0;

  Page := CreateCustomPage(EmailRecipientsPage.ID, 'Metrics Port', 'Assign a port for the WinMonitor metrics endpoint');
  ServicePortsPage := Page;
  EditLeft := Round(SW * 0.72);
  EditWidth := SW - EditLeft;
  AddLabel(Page, 0, SW, 'WinMonitor exposes live metrics at http://localhost:<port>/metrics.', False);
  AddLabel(Page, ScaleY(32), EditLeft - ScaleX(8), 'WinMonitor port:', False);
  WinMonitorPortEdit := TNewEdit.Create(Page);
  WinMonitorPortEdit.Parent := Page.Surface;
  WinMonitorPortEdit.Top := ScaleY(28); WinMonitorPortEdit.Left := EditLeft;
  WinMonitorPortEdit.Width := EditWidth;
  WinMonitorPortEdit.Text := GetSavedValue('WinMonitorPort', '4010');
end;

procedure InitializeWizard;
begin
  IsUpgrade := CheckIsUpgrade;
  CreateCustomPages;
end;

function ShouldSkipPage(PageID: Integer): Boolean;
begin
  Result := False;
  if PageID = SmtpConfigPage.ID then Result := (EmailAuthPage.SelectedValueIndex <> 0);
  if PageID = OAuthConfigPage.ID then Result := (EmailAuthPage.SelectedValueIndex <> 1);
end;

function NextButtonClick(CurPageID: Integer): Boolean;
begin
  Result := True;
  if CurPageID = EmailRecipientsPage.ID then
  begin
    if Trim(EmailToEdit.Text) = '' then
    begin MsgBox('Please enter at least one recipient email address.', mbError, MB_OK); Result := False; Exit; end;
    if Trim(EmailFromEdit.Text) = '' then
    begin MsgBox('Please enter a From email address.', mbError, MB_OK); Result := False; Exit; end;
  end;
  if CurPageID = ServicePortsPage.ID then
    if not IsValidPort(WinMonitorPortEdit.Text) then
    begin MsgBox('Port must be between 1024 and 65535.', mbError, MB_OK); Result := False; Exit; end;
end;

procedure CurStepChanged(CurStep: TSetupStep);
var
  D, ServicePath, PropsFile: string;
begin
  if CurStep = ssInstall then
  begin
    StopAndUninstallService('IPMonitoring_WinMonitor.exe');
    Exit;
  end;
  if CurStep <> ssPostInstall then Exit;

  D := ExpandConstant('{app}');
  ServicePath := D + '\monitoring-services\WinMonitor';
  PropsFile := ServicePath + '\windowsmonitor.properties';

  UpdatePropsKey(PropsFile, 'metrics.exporter.port', WinMonitorPortEdit.Text);
  UpdatePropsKey(PropsFile, 'client.name', GetClientName(''));
  GenerateEmailProperties(ServicePath);
  GenerateServiceXml('IPMonitoring_WinMonitor', 'IP Monitoring - Windows Server Monitor',
    'Monitors CPU, memory, disk and Windows service health',
    'java', '-jar "' + ServicePath + '\WinMonitor.jar"', ServicePath);
  InstallAndStartService('IPMonitoring_WinMonitor.exe');
end;

procedure CurUninstallStepChanged(CurUninstallStep: TUninstallStep);
begin
  if CurUninstallStep = usUninstall then
    StopAndUninstallService('IPMonitoring_WinMonitor.exe');
end;
