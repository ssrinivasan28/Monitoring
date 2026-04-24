#define AppName "IBM IFS Error Monitor"
#define AppVersion "1.0.0"
#define AppPublisher "Island Pacific"

[Setup]
AppId={{9E8C9F31-7A9A-4F63-A9F2-IBMIFS000009}}
AppName={#AppName}
AppVersion={#AppVersion}
AppPublisher={#AppPublisher}
DefaultDirName={code:GetInstallDir}
PrivilegesRequired=admin
WizardStyle=modern
DisableDirPage=yes
DisableProgramGroupPage=yes
Compression=lzma
SolidCompression=yes
OutputDir=.\output
OutputBaseFilename=IBMIFSErrorMonitorAddon

[Files]
Source: "..\installer\resources\monitoring-services\IBMIFSErrorMonitor\*"; \
  DestDir: "{code:GetInstallDir}\monitoring-services\IBMIFSErrorMonitor"; \
  Flags: recursesubdirs createallsubdirs ignoreversion

Source: "..\installer\resources\WinSW.exe"; \
  DestDir: "{code:GetInstallDir}\services"; \
  DestName: "IPMonitoring_IBMIFSErrorMonitor.exe"; \
  Flags: ignoreversion

[UninstallRun]
Filename: "{code:GetInstallDir}\services\IPMonitoring_IBMIFSErrorMonitor.exe"; Parameters: "stop"; Flags: runhidden
Filename: "{code:GetInstallDir}\services\IPMonitoring_IBMIFSErrorMonitor.exe"; Parameters: "uninstall"; Flags: runhidden

[Code]
const
  BaseRegKey = 'Software\IslandPacific\Monitoring';
  DefaultBaseDir = 'C:\Program Files\Island Pacific Monitoring Suite';

var
  InstallDir: string;
  AuthMethodPage: TInputOptionWizardPage;
  SMTPPage1: TInputQueryWizardPage;
  SMTPPage2: TInputQueryWizardPage;
  OAuthPage: TInputQueryWizardPage;
  MonitorPage: TInputQueryWizardPage;

// Inno Setup does not provide TryStrToInt by default, so we define it here
function TryStrToInt(const S: String; var Value: Integer): Boolean;
var
  E: Integer;
begin
  Val(S, Value, E);
  Result := E = 0;
end;

function GetInstallDir(Param: string): string;
begin
  if InstallDir = '' then
  begin
    if not RegQueryStringValue(HKLM64, BaseRegKey, 'InstallDir', InstallDir) then
      if not RegQueryStringValue(HKLM32, BaseRegKey, 'InstallDir', InstallDir) then
        if DirExists(DefaultBaseDir) then
          InstallDir := DefaultBaseDir;
  end;
  Result := InstallDir;
end;

function GetProperty(FileName, Key: string): string;
var
  Lines: TStringList;
  I, P: Integer;
begin
  Result := '';
  if not FileExists(FileName) then Exit;
  Lines := TStringList.Create;
  try
    Lines.LoadFromFile(FileName);
    for I := 0 to Lines.Count - 1 do
    begin
      P := Pos(Key + '=', Lines[I]);
      if P = 1 then
      begin
        Result := Copy(Lines[I], Length(Key) + 2, MaxInt);
        Break;
      end;
    end;
  finally
    Lines.Free;
  end;
end;

function InitializeSetup(): Boolean;
begin
  Result := False;
  if GetInstallDir('') = '' then
  begin
    MsgBox('Island Pacific Monitoring Suite was not detected.' + #13#10 + 
           'Please ensure the base suite is installed first.', mbCriticalError, MB_OK);
    Exit;
  end;
  Result := True;
end;

procedure TightenLayout(Page: TInputQueryWizardPage);
var
  I: Integer;
begin
  for I := 0 to 7 do 
  begin
    try
      if I < 8 then 
      begin
        if I = 0 then
        begin
          Page.PromptLabels[I].Top := ScaleY(0);
        end
        else
        begin
          Page.PromptLabels[I].Top := Page.Edits[I-1].Top + Page.Edits[I-1].Height + ScaleY(4);
        end;
        Page.Edits[I].Top := Page.PromptLabels[I].Top + Page.PromptLabels[I].Height + ScaleY(2);
      end;
    except
    end;
  end;
end;

procedure InitializeWizard;
var
  EmailFile, MonitorFile: string;
begin
  EmailFile := InstallDir + '\monitoring-services\IBMIFSErrorMonitor\email.properties';
  MonitorFile := InstallDir + '\monitoring-services\IBMIFSErrorMonitor\monitor.properties';

  AuthMethodPage := CreateInputOptionPage(wpWelcome, 'Email Authentication Method', 'SMTP or OAuth2', 'Select how alert emails should be sent.', True, False);
  AuthMethodPage.Add('SMTP');
  AuthMethodPage.Add('OAuth2');
  
  if CompareText(GetProperty(EmailFile, 'mail.auth.method'), 'OAUTH2') = 0 then 
    AuthMethodPage.Values[1] := True
  else 
    AuthMethodPage.Values[0] := True;

  SMTPPage1 := CreateInputQueryPage(AuthMethodPage.ID, 'SMTP Server Settings', 'Connection Info', 'Provide server host and port.');
  SMTPPage1.Add('SMTP Host:', False);
  SMTPPage1.Add('SMTP Port:', False);
  SMTPPage1.Add('SMTP Auth (true/false):', False);
  SMTPPage1.Values[0] := GetProperty(EmailFile, 'mail.smtp.host');
  SMTPPage1.Values[1] := GetProperty(EmailFile, 'mail.smtp.port');
  SMTPPage1.Values[2] := GetProperty(EmailFile, 'mail.smtp.auth');
  TightenLayout(SMTPPage1);

  SMTPPage2 := CreateInputQueryPage(SMTPPage1.ID, 'SMTP Credentials', 'Account Info', 'Provide credentials and recipients.');
  SMTPPage2.Add('SMTP Username:', False);
  SMTPPage2.Add('SMTP Password:', True);
  SMTPPage2.Add('From Email:', False);
  SMTPPage2.Add('To Email(s):', False);
  SMTPPage2.Add('BCC Email(s):', False);
  SMTPPage2.Values[0] := GetProperty(EmailFile, 'mail.smtp.username');
  SMTPPage2.Values[1] := GetProperty(EmailFile, 'mail.smtp.password');
  SMTPPage2.Values[2] := GetProperty(EmailFile, 'mail.from');
  SMTPPage2.Values[3] := GetProperty(EmailFile, 'mail.to');
  SMTPPage2.Values[4] := GetProperty(EmailFile, 'mail.bcc');
  TightenLayout(SMTPPage2);

  OAuthPage := CreateInputQueryPage(SMTPPage2.ID, 'OAuth2 Configuration', 'Microsoft 365 / Azure AD', 'Fill values for OAuth2 authentication.');
  OAuthPage.Add('Tenant ID:', False);
  OAuthPage.Add('Client ID:', False);
  OAuthPage.Add('Client Secret:', True);
  OAuthPage.Add('From User Email:', False);
  OAuthPage.Add('To Email(s):', False);
  OAuthPage.Add('BCC Email(s):', False);
  OAuthPage.Values[0] := GetProperty(EmailFile, 'mail.oauth2.tenant.id');
  OAuthPage.Values[1] := GetProperty(EmailFile, 'mail.oauth2.client.id');
  OAuthPage.Values[2] := GetProperty(EmailFile, 'mail.oauth2.client.secret');
  OAuthPage.Values[3] := GetProperty(EmailFile, 'mail.oauth2.from.user');
  OAuthPage.Values[4] := GetProperty(EmailFile, 'mail.to');
  OAuthPage.Values[5] := GetProperty(EmailFile, 'mail.bcc');
  TightenLayout(OAuthPage);

  MonitorPage := CreateInputQueryPage(OAuthPage.ID, 'Monitoring Configuration', 'IBM IFS Monitor', 'Provide monitoring and IBMi connection details.');
  MonitorPage.Add('Metrics Port:', False);
  MonitorPage.Add('Monitor Interval (ms):', False);
  MonitorPage.Add('IBMi Server:', False);
  MonitorPage.Add('IBMi User:', False);
  MonitorPage.Add('IBMi Password:', True);
  MonitorPage.Add('Client Name:', False);
  MonitorPage.Values[0] := GetProperty(MonitorFile, 'metrics.port');
  MonitorPage.Values[1] := GetProperty(MonitorFile, 'monitor.interval.ms');
  MonitorPage.Values[2] := GetProperty(MonitorFile, 'ibmi.server');
  MonitorPage.Values[3] := GetProperty(MonitorFile, 'ibmi.user');
  MonitorPage.Values[4] := GetProperty(MonitorFile, 'ibmi.password');
  MonitorPage.Values[5] := GetProperty(MonitorFile, 'client.name');
  TightenLayout(MonitorPage);
end;

function ShouldSkipPage(PageID: Integer): Boolean;
begin
  Result := False;
  if (PageID = SMTPPage1.ID) and (AuthMethodPage.Values[1]) then Result := True;
  if (PageID = SMTPPage2.ID) and (AuthMethodPage.Values[1]) then Result := True;
  if (PageID = OAuthPage.ID) and (AuthMethodPage.Values[0]) then Result := True;
end;

procedure PatchProperty(FileName, Key, Value: string);
var
  Lines: TStringList;
  I: Integer;
  Found: Boolean;
begin
  Lines := TStringList.Create;
  try
    if FileExists(FileName) then Lines.LoadFromFile(FileName);
    Found := False;
    for I := 0 to Lines.Count - 1 do
      if Pos(Key + '=', Lines[I]) = 1 then
      begin
        Lines[I] := Key + '=' + Value;
        Found := True;
        Break;
      end;
    if not Found then Lines.Add(Key + '=' + Value);
    Lines.SaveToFile(FileName);
  finally
    Lines.Free;
  end;
end;

procedure CreateServiceConfig(ServicePath, ExecutablePath: string);
var
  Lines: TStringList;
  XmlPath: string;
  WorkingDir: string;
begin
  XmlPath := ChangeFileExt(ServicePath, '.xml');
  WorkingDir := ExtractFilePath(ExecutablePath);
  Lines := TStringList.Create;
  try
    Lines.Add('<service>');
    Lines.Add('  <id>IPMonitoring_IBMIFSErrorMonitor</id>');
    Lines.Add('  <name>IP Monitoring - IBM IFS Error Monitor</name>');
    Lines.Add('  <description>Island Pacific Monitoring Addon for IBM IFS Error logs.</description>');
    Lines.Add('  <executable>java</executable>');
    Lines.Add('  <arguments>-jar "' + ExecutablePath + '"</arguments>');
    Lines.Add('  <workingdirectory>' + WorkingDir + '</workingdirectory>');
    Lines.Add('  <log mode="roll"></log>');
    Lines.Add('</service>');
    Lines.SaveToFile(XmlPath);
  finally
    Lines.Free;
  end;
end;

procedure CurStepChanged(CurStep: TSetupStep);
var
  EmailFile, MonitorFile, ServicePath, JarPath: string;
  RC, Attempt: Integer;
  Success: Boolean;
begin
  if CurStep = ssPostInstall then
  begin
    EmailFile := InstallDir + '\monitoring-services\IBMIFSErrorMonitor\email.properties';
    MonitorFile := InstallDir + '\monitoring-services\IBMIFSErrorMonitor\monitor.properties';
    ServicePath := InstallDir + '\services\IPMonitoring_IBMIFSErrorMonitor.exe';
    JarPath := InstallDir + '\monitoring-services\IBMIFSErrorMonitor\IBMIFSErrorMonitor.jar';

    { Update Properties }
    if AuthMethodPage.Values[0] then
    begin
      PatchProperty(EmailFile, 'mail.auth.method', 'SMTP');
      PatchProperty(EmailFile, 'mail.smtp.host', Trim(SMTPPage1.Values[0]));
      PatchProperty(EmailFile, 'mail.smtp.port', Trim(SMTPPage1.Values[1]));
      PatchProperty(EmailFile, 'mail.smtp.auth', Trim(SMTPPage1.Values[2]));
      PatchProperty(EmailFile, 'mail.smtp.starttls.enable', 'true');
      PatchProperty(EmailFile, 'mail.smtp.username', Trim(SMTPPage2.Values[0]));
      PatchProperty(EmailFile, 'mail.smtp.password', SMTPPage2.Values[1]);
      PatchProperty(EmailFile, 'mail.from', Trim(SMTPPage2.Values[2]));
      PatchProperty(EmailFile, 'mail.to', Trim(SMTPPage2.Values[3]));
      PatchProperty(EmailFile, 'mail.bcc', Trim(SMTPPage2.Values[4]));
    end
    else
    begin
      PatchProperty(EmailFile, 'mail.auth.method', 'OAUTH2');
      PatchProperty(EmailFile, 'mail.oauth2.tenant.id', Trim(OAuthPage.Values[0]));
      PatchProperty(EmailFile, 'mail.oauth2.client.id', Trim(OAuthPage.Values[1]));
      PatchProperty(EmailFile, 'mail.oauth2.client.secret', OAuthPage.Values[2]);
      PatchProperty(EmailFile, 'mail.oauth2.token.url', 'https://login.microsoftonline.com/' + Trim(OAuthPage.Values[0]) + '/oauth2/v2.0/token');
      PatchProperty(EmailFile, 'mail.oauth2.graph.mail.url', 'https://graph.microsoft.com/v1.0/users/' + Trim(OAuthPage.Values[3]) + '/sendMail');
      PatchProperty(EmailFile, 'mail.oauth2.from.user', Trim(OAuthPage.Values[3]));
      PatchProperty(EmailFile, 'mail.from', Trim(OAuthPage.Values[3]));
      PatchProperty(EmailFile, 'mail.to', Trim(OAuthPage.Values[4]));
      PatchProperty(EmailFile, 'mail.bcc', Trim(OAuthPage.Values[5]));
    end;

    PatchProperty(MonitorFile, 'metrics.port', Trim(MonitorPage.Values[0]));
    PatchProperty(MonitorFile, 'monitor.interval.ms', Trim(MonitorPage.Values[1]));
    PatchProperty(MonitorFile, 'ibmi.server', Trim(MonitorPage.Values[2]));
    PatchProperty(MonitorFile, 'ibmi.user', Trim(MonitorPage.Values[3]));
    PatchProperty(MonitorFile, 'ibmi.password', MonitorPage.Values[4]);
    PatchProperty(MonitorFile, 'client.name', Trim(MonitorPage.Values[5]));

    { Service management }
    CreateServiceConfig(ServicePath, JarPath);

    { Force kill any existing service wrapper process to release file handles }
    Exec('taskkill', '/F /IM IPMonitoring_IBMIFSErrorMonitor.exe /T', '', SW_HIDE, ewWaitUntilTerminated, RC);

    { Stop and Uninstall }
    Exec(ServicePath, 'stop', '', SW_HIDE, ewWaitUntilTerminated, RC);
    Exec(ServicePath, 'uninstall', '', SW_HIDE, ewWaitUntilTerminated, RC);
    
    { Improved loop to handle Windows "Marked for deletion" lag }
    Success := False;
    for Attempt := 1 to 5 do
    begin
      Sleep(2000); 
      if Exec(ServicePath, 'install', '', SW_HIDE, ewWaitUntilTerminated, RC) then
      begin
        Success := True;
        Break;
      end;
    end;

    { Always attempt to start }
    Exec(ServicePath, 'start', '', SW_HIDE, ewWaitUntilTerminated, RC);
    
    if not Success then
    begin
      MsgBox('Service installation reported a conflict. Please check if Services.msc is open and restart the service manually if needed.', mbInformation, MB_OK);
    end;
  end;
end;