#define Version '25.06.02'

[Setup]
AppName=WebGen
AppVersion={#Version}
VersionInfoVersion={#Version}
; AppPublisher=Christoph Theis
DefaultDirName={autopf}\TTM\WebGen
DefaultGroupName=TTM
OutputDir=.\Output
OutputBaseFilename=install
; Mindestens Windows 7
MinVersion=6.1
ArchitecturesInstallIn64BitMode=x64

; Sign installer
; SignTool=MS /d $qWeb Page Generator$q $f


; Close TTM before installation
CloseApplications=force
CloseApplicationsFilter=*.exe,*.dll,*.jar

[Languages]
Name: en; MessagesFile: compiler:Default.isl
Name: de; MessagesFile: compiler:Languages\German.isl           
                                                                                                 
[CustomMessages]
en.Language=en
de.Language=de

[Types]
Name: "client"; Description: "WebGenerator for TTM"

[Components]
Name: "Client"; Description: "WebGenerator for TTM"; Types: client; Flags: fixed


[Tasks]
Name: "desktopicon"; Description: "Create a &desktop icon"; GroupDescription: "Additional icons:";

[Dirs]
; Brauche ich, um die Berechtigungen zu setzen.
; Installation ist vom Admin, ausfuehren soll es aber ein non-admin
Name: {code:GetIniDir}; Permissions: authusers-modify

[Files]
; signtool kann kein jar signieren
; Source: ".\dist\WebGen2.jar"; DestDir: "{app}"; Flags: ignoreversion signonce 
Source: ".\dist\WebGen2.jar"; DestDir: "{app}"; Flags: ignoreversion 
Source: ".\dist\*.dll"; DestDir: "{app}"; Flags: ignoreversion
Source: ".\dist\lib\*.jar"; DestDir: "{app}\lib"; Excludes: "edtftp*.jar"; Flags: ignoreversion
; Copy either the OSS edtftpj.jar or the commercial runtime with our FTPClient
; Source: ".\lib\edtftpj.jar"; DestDir: "{app}\lib"; Flags: ignoreversion
Source: "..\FtpClient\dist\FtpClient.jar"; DestDir: "{app}\lib"; Flags: ignoreversion
Source: "..\FtpClient\lib\edtftpj-pro.jar"; DestDir: "{app}\lib"; Flags: ignoreversion
Source: ".\dist\WebGen.ico"; DestDir: "{app}"; Flags: ignoreversion 
Source: ".\Template\*.html"; DestDir: "{code:GetIniDir}\WebGen\Template\"; Flags: ignoreversion
Source: ".\Template\css\*"; DestDir: "{code:GetIniDir}\WebGen\Template\css\"; Flags: ignoreversion
Source: ".\Template\themes\*"; DestDir: "{code:GetIniDir}\WebGen\Template\themes\"; Flags: ignoreversion
Source: ".\Template\fonts\*"; DestDir: "{code:GetIniDir}\WebGen\Template\fonts\"; Flags: ignoreversion
Source: ".\Template\js\*"; DestDir: "{code:GetIniDir}\WebGen\Template\js\"; Flags: ignoreversion
Source: ".\Template\img\*"; DestDir: "{code:GetIniDir}\WebGen\Template\img\"; Flags: ignoreversion
Source: ".\Template\flags\*.png"; DestDir: "{code:GetIniDir}\WebGen\Template\flags\"; Flags: ignoreversion
Source: ".\changes.html"; DestDir: "{app}"; Flags: ignoreversion
Source: ".\3rdparty.html"; DestDir: "{app}"; Flags: ignoreversion
; Manual still TODO
; Source: "..\TTM Manuals\src\WebGen\WebGen.pdf"; DestDir: "{app}"; Flags: ignoreversion

[INI]
; The following creates the ini file if it doesn't exist yet
Filename: {code:GetIniDir}\TT32.ini; Section: Settings; Key: Language; String: {cm:Language}; Flags: createkeyifdoesntexist

[Registry]
; HKLM\Software\JavaSoft\Prefs should be created by JRE installer, but that does not always happen
Root: HKLM; Subkey: "Software\JavaSoft\Prefs"; Flags: noerror

[Icons]
Name: "{group}\WebGen"; Filename: "{app}\lib\WebGen2.jar"; WorkingDir: "{app}"; IconFilename: "{app}\WebGen.ico";
Name: "{userdesktop}\WebGen"; Filename: "{app}\WebGen2.jar"; WorkingDir: "{app}"; Tasks: desktopicon; IconFilename: "{app}\WebGen.ico";

[Run]
; Alte Files von 3.x loeschen
Filename: {sys}\cmd.exe; Parameters: /c del {app}\lib\WebGen.jar; Check: FileExists(ExpandConstant('{app}\lib\WebGen.jar'));
Filename: {sys}\cmd.exe; Parameters: /c del /Q {code:GetIniDir}\Template\*; Check: FileExists(ExpandConstant('code:GetIniDir}\Template'));
Filename: {sys}\cmd.exe; Parameters: /c rmdir {code:GetIniDir}\Template; Check: FileExists(ExpandConstant('code:GetIniDir}\Template'));


[Code]
(* looks for JDK or JRE version in Registry *)
function getJREVersion(): String;
var
	jreVersion: String;
begin
	jreVersion := '';
  if IsWin64 then begin
	  RegQueryStringValue(HKLM64, 'SOFTWARE\JavaSoft\JRE', 'CurrentVersion', jreVersion);
  end
  else begin
  	RegQueryStringValue(HKLM, 'SOFTWARE\JavaSoft\JRE', 'CurrentVersion', jreVersion);
  end;
	Result := jreVersion;
end;

(* looks for JDK version, in Registry *)
function getJDKVersion(): String;
var
	jdkVersion: String;
begin
	jdkVersion := '';
  if IsWin64 then begin
	  RegQueryStringValue(HKLM64, 'SOFTWARE\JavaSoft\JDK', 'CurrentVersion', jdkVersion);
  end
  else begin
  	RegQueryStringValue(HKLM, 'SOFTWARE\JavaSoft\JDK', 'CurrentVersion', jdkVersion);
  end;
	Result := jdkVersion;
end;

(* Called on setup startup *)
function InitializeSetup(): Boolean;
var
	javaVersion: String;
begin
	javaVersion := GetJDKVersion();
  if Length(javaVersion) = 0 then begin
    javaVersion := GetJREVersion()
  end;

	if javaVersion >= '21' then begin
		(* MsgBox('Found java version' + javaVersion, mbInformation, MB_OK); *)
		Result := true;
	end
	else begin
		MsgBox('Setup is unable to find a Java Development Kit or Java Runtime 21, or higher, installed.' + #13 +
			     'You must have installed at least JDK or JRE, 21 or higher to continue setup.' + #13 +
			     'Please install one from https://AdoptOpenJDK.com and then run this setup again.', mbInformation, MB_OK);
		Result := true;
	end;
end;

{Sucht ein Verzeichnis mit TT32.INI.}
{Erster Versuch ist Installation (app), wie es frueher war.}
{Wird das File dort nicht gefunden dann (commonappdata), falls es dort ist oder setup als Admin ausgefuehrt wird.}
{Letzter default ist (userappdata).}
function GetIniDir(Param: String): String;
begin
  if (FileExists(ExpandConstant( '{app}\TT32.INI' ))) then
  begin
    Result := ExpandConstant('{app}');
  end
  else if ( isAdminLoggedOn OR FileExists(ExpandConstant( '{commonappdata}\TTM\TT32.INI' )) ) then
  begin
    CreateDir(ExpandConstant('{commonappdata}\TTM'));
    Result := ExpandConstant('{commonappdata}\TTM');
  end
  else if ( FileExists(ExpandConstant( '{userappdata}\TTM\TT32.ini' )) ) then
  begin
    Result := ExpandConstant('{userappdata}\TTM');
  end
  else
  begin
    CreateDir(ExpandConstant('{localappdata}\TTM'));
    Result := ExpandConstant('{localappdata}\TTM');
  end
end;




