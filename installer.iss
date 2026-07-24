[Setup]
AppName=frogTimeStats
AppVersion=1.0
AppPublisher=frogilik
DefaultDirName={autopf}\frogTimeStats
DefaultGroupName=frogTimeStats
UninstallDisplayIcon={app}\frogTimeStats.exe
Compression=lzma2/ultra64
SolidCompression=yes
OutputDir=target
OutputBaseFilename=frogTimeStats_Setup

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked

[Files]
; Упаковываем исполнимый EXE файл
Source: "target\frogTimeStats.exe"; DestDir: "{app}"; Flags: ignoreversion
; Упаковываем Fat-JAR
Source: "target\frogTimeStats-1.0-SNAPSHOT.jar"; DestDir: "{app}"; Flags: ignoreversion
; Упаковываем портативную Java
Source: "jre_win\*"; DestDir: "{app}\jre"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{group}\frogTimeStats"; Filename: "{app}\frogTimeStats.exe"
Name: "{autodesktop}\frogTimeStats"; Filename: "{app}\frogTimeStats.exe"; Tasks: desktopicon

[Run]
Filename: "{app}\frogTimeStats.exe"; Description: "{cm:LaunchProgram,frogTimeStats}"; Flags: nowait postinstall skipifsilent