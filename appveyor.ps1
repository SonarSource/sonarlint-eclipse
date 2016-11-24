$ErrorActionPreference = "Stop"

$PSVersionTable.PSVersion

function InstallAppveyorTools
{
	$travisUtilsVersion = "33"
	$localPath = "$env:USERPROFILE\.local"
	$tmp = [IO.Path]::GetTempFileName() | Rename-Item -NewName { [IO.Path]::ChangeExtension($_,".tmp.zip") } -PassThru
	wget "https://github.com/SonarSource/travis-utils/archive/v$travisUtilsVersion.zip" -OutFile $tmp
	
	Expand-Archive -Force -Path $tmp -DestinationPath $localPath
    
	$mavenLocal = "$env:USERPROFILE\.m2"
	if (-not(Test-Path $mavenLocal))
	{
		mkdir $mavenLocal | Out-Null
	}
	echo "Installing Travis Utils public Maven settings.xml into $mavenLocal"
	Copy-Item "$localPath\travis-utils-$travisUtilsVersion\m2\settings-public.xml" "$mavenLocal\settings.xml"
}

InstallAppveyorTools

mvn verify
