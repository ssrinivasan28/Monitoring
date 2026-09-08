; IP Sentinel AIOps Platform Standalone Installer

#define AppName "IP Sentinel AIOps Platform"
#define AppVersion "1.0.0"
#define AppPublisher "Island Pacific Retail Systems"
#define AppURL "https://www.islandpacific.com"

[Setup]
AppId={{IP-Sentinel-AIOps-Platform}}
AppName={#AppName}
AppVersion={#AppVersion}
AppPublisher={#AppPublisher}
AppPublisherURL={#AppURL}
AppSupportURL={#AppURL}
AppUpdatesURL={#AppURL}
DefaultDirName={commonpf}\Island Pacific\IP Sentinel
DisableProgramGroupPage=yes
OutputDir=.\installer\output
OutputBaseFilename=IPSentinelSetup
Compression=lzma
SolidCompression=yes
WizardStyle=modern
PrivilegesRequired=admin
LicenseFile=.\installer\resources\license.txt

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Types]
Name: "full"; Description: "Full IP Sentinel Platform (AIOps Platform + Prometheus + Loki + Promtail)"
Name: "platform_only"; Description: "AIOps Platform Server Only"
Name: "custom"; Description: "Custom Installation"; Flags: iscustom

[Components]
Name: "aiops"; Description: "IP Sentinel Central AIOps Platform"; Types: full platform_only
Name: "prometheus"; Description: "Prometheus Server (Metrics Storage)"; Types: full
Name: "loki"; Description: "Loki Log Aggregation Server"; Types: full
Name: "promtail"; Description: "Promtail Log Collector"; Types: full

[Dirs]
Name: "{app}\services"
Name: "{app}\logs"; Flags: uninsneveruninstall
Name: "{app}\data"; Flags: uninsneveruninstall
Name: "{app}\data\prometheus"; Flags: uninsneveruninstall; Components: prometheus
Name: "{app}\data\loki"; Flags: uninsneveruninstall; Components: loki
Name: "{app}\aiops-platform"; Components: aiops
Name: "{app}\prometheus"; Components: prometheus
Name: "{app}\loki"; Components: loki
Name: "{app}\promtail"; Components: promtail

[Files]
; Service Wrappers (WinSW)
Source: "installer\resources\WinSW.exe"; DestDir: "{app}\services"; DestName: "IPMonitoring_AIOpsPlatform.exe"; Flags: ignoreversion; Components: aiops
Source: "installer\resources\WinSW.exe"; DestDir: "{app}\services"; DestName: "IPMonitoringPrometheus.exe"; Flags: ignoreversion; Components: prometheus
Source: "installer\resources\WinSW.exe"; DestDir: "{app}\services"; DestName: "IPMonitoringLoki.exe"; Flags: ignoreversion; Components: loki
Source: "installer\resources\WinSW.exe"; DestDir: "{app}\services"; DestName: "IPMonitoringPromtail.exe"; Flags: ignoreversion; Components: promtail

; AIOps Platform Application & Config (preserve application.yml on upgrade)
Source: "installer\resources\aiops-platform\*.jar"; DestDir: "{app}\aiops-platform"; Flags: ignoreversion; Components: aiops
Source: "installer\resources\aiops-platform\application.yml"; DestDir: "{app}\aiops-platform"; Flags: onlyifdoesntexist; Components: aiops

; Prometheus Server (exclude data directory to preserve metrics)
Source: "installer\resources\prometheus\*"; DestDir: "{app}\prometheus"; Excludes: "data\*"; Flags: recursesubdirs createallsubdirs ignoreversion; Components: prometheus

; Loki Log Aggregator
Source: "installer\resources\loki\*"; DestDir: "{app}\loki"; Flags: recursesubdirs createallsubdirs ignoreversion; Components: loki

; Promtail Log Collector
Source: "installer\resources\promtail\*"; DestDir: "{app}\promtail"; Flags: recursesubdirs createallsubdirs ignoreversion; Components: promtail

[Icons]
Name: "{commonprograms}\{#AppName}\AIOps Platform Dashboard"; Filename: "http://localhost:8080"; Components: aiops
Name: "{commonprograms}\{#AppName}\Prometheus Metrics"; Filename: "http://localhost:9090"; Components: prometheus
Name: "{commonprograms}\{#AppName}\Uninstall {#AppName}"; Filename: "{uninstallexe}"

[Run]
Filename: "netsh"; Parameters: "advfirewall firewall add rule name=""IP Sentinel - AIOps Platform"" dir=in action=allow protocol=TCP localport=8080"; Flags: runhidden waituntilterminated; StatusMsg: "Configuring firewall for AIOps Platform..."; Components: aiops
Filename: "netsh"; Parameters: "advfirewall firewall add rule name=""IP Sentinel - Prometheus"" dir=in action=allow protocol=TCP localport=9090"; Flags: runhidden waituntilterminated; StatusMsg: "Configuring firewall for Prometheus..."; Components: prometheus
Filename: "netsh"; Parameters: "advfirewall firewall add rule name=""IP Sentinel - Loki"" dir=in action=allow protocol=TCP localport=3100"; Flags: runhidden waituntilterminated; StatusMsg: "Configuring firewall for Loki..."; Components: loki

[UninstallRun]
Filename: "netsh"; Parameters: "advfirewall firewall delete rule name=""IP Sentinel - AIOps Platform"""; Flags: runhidden waituntilterminated; RunOnceId: "RemoveFirewallAIOpsPlatform"
Filename: "netsh"; Parameters: "advfirewall firewall delete rule name=""IP Sentinel - Prometheus"""; Flags: runhidden waituntilterminated; RunOnceId: "RemoveFirewallPrometheus"
Filename: "netsh"; Parameters: "advfirewall firewall delete rule name=""IP Sentinel - Loki"""; Flags: runhidden waituntilterminated; RunOnceId: "RemoveFirewallLoki"

[Registry]
Root: HKLM; Subkey: "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\{#AppName}"; ValueType: string; ValueName: "DisplayName"; ValueData: "{#AppName}"
Root: HKLM; Subkey: "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\{#AppName}"; ValueType: string; ValueName: "UninstallString"; ValueData: "{uninstallexe}"
Root: HKLM; Subkey: "Software\IslandPacific\IPSentinel"; ValueType: string; ValueName: "InstallPath"; ValueData: "{app}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "Software\IslandPacific\IPSentinel"; ValueType: string; ValueName: "AIOpsPlatformPort"; ValueData: "8080"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "Software\IslandPacific\IPSentinel"; ValueType: string; ValueName: "PrometheusPort"; ValueData: "9090"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "Software\IslandPacific\IPSentinel"; ValueType: string; ValueName: "LokiPort"; ValueData: "3100"; Flags: uninsdeletevalue

[Code]

procedure GenerateServiceXml(const ServiceExeName, ServiceId, ServiceName, Description, Executable, Arguments, WorkDir: string);
var
	AppDir: string;
begin
	AppDir := ExpandConstant('{app}');
	SaveStringToFile(AppDir + '\services\' + ServiceExeName + '.xml',
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

procedure InstallAndStartService(const ServiceExeName: string);
var
	ServicePath: string;
	ResultCode: Integer;
begin
	ServicePath := ExpandConstant('{app}\services\' + ServiceExeName + '.exe');
	if not FileExists(ServicePath) then Exit;
	Exec(ServicePath, 'stop', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
	Exec(ServicePath, 'uninstall', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
	Exec(ServicePath, 'install', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
	Exec(ServicePath, 'start', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
end;

procedure CurStepChanged(CurStep: TSetupStep);
var
	AppDir: string;
begin
	if CurStep = ssPostInstall then
	begin
		AppDir := ExpandConstant('{app}');

		{ AIOps Platform }
		if IsComponentSelected('aiops') then
		begin
			GenerateServiceXml('IPMonitoring_AIOpsPlatform', 'IPMonitoring_AIOpsPlatform', 'IP Sentinel - AIOps Platform',
				'IP Sentinel Central Multi-Tenant AIOps Platform Service', 'java', '-jar "' + AppDir + '\aiops-platform\aiops-platform-0.0.1-SNAPSHOT.jar"',
				AppDir + '\aiops-platform');
			InstallAndStartService('IPMonitoring_AIOpsPlatform');
		end;

		{ Prometheus with 13-month retention (395d) }
		if IsComponentSelected('prometheus') then
		begin
			GenerateServiceXml('IPMonitoringPrometheus', 'IPMonitoringPrometheus', 'IP Sentinel - Prometheus',
				'Prometheus Metrics Storage Server', AppDir + '\prometheus\prometheus.exe',
				'--config.file="' + AppDir + '\prometheus\prometheus.yml" --storage.tsdb.path="' + AppDir + '\data\prometheus" --storage.tsdb.retention.time=395d --web.listen-address=":9090"',
				AppDir + '\prometheus');
			InstallAndStartService('IPMonitoringPrometheus');
		end;

		{ Loki }
		if IsComponentSelected('loki') then
		begin
			GenerateServiceXml('IPMonitoringLoki', 'IPMonitoringLoki', 'IP Sentinel - Loki',
				'Loki Log Aggregation Server', AppDir + '\loki\loki.exe',
				'-config.file="' + AppDir + '\loki\loki.yaml"',
				AppDir + '\loki');
			InstallAndStartService('IPMonitoringLoki');
		end;

		{ Promtail }
		if IsComponentSelected('promtail') then
		begin
			GenerateServiceXml('IPMonitoringPromtail', 'IPMonitoringPromtail', 'IP Sentinel - Promtail',
				'Promtail Log Collector', AppDir + '\promtail\promtail.exe',
				'-config.file="' + AppDir + '\promtail\promtail.yaml"',
				AppDir + '\promtail');
			InstallAndStartService('IPMonitoringPromtail');
		end;
	end;
end;
