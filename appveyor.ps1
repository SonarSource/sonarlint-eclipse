$ErrorActionPreference = "Stop"

$PSVersionTable.PSVersion

function InstallAppveyorTools
{
	$travisUtilsVersion = "632f394e55e23cec5c336ccfbeb525d8577cc3fe"
	$localPath = "$env:USERPROFILE\.local"
	$tmp = [IO.Path]::GetTempFileName() | Rename-Item -NewName { [IO.Path]::ChangeExtension($_,".tmp.zip") } -PassThru
	wget "https://github.com/SonarSource/travis-utils/archive/$travisUtilsVersion.zip" -OutFile $tmp
	
	Expand-Archive -Force -Path $tmp -DestinationPath $localPath
    
	$mavenLocal = "$env:USERPROFILE\.m2"
	if (-not(Test-Path $mavenLocal))
	{
		mkdir $mavenLocal | Out-Null
	}
	echo "Installing Travis Utils public Maven settings.xml into $mavenLocal"
	Copy-Item "$localPath\travis-utils-$travisUtilsVersion\m2\settings-public.xml" "$mavenLocal\settings.xml"
}

function CheckLastExitCode
{
    param ([int[]]$SuccessCodes = @(0))

    if ($SuccessCodes -notcontains $LastExitCode)
	{
        $msg = @"
EXE RETURNED EXIT CODE $LastExitCode
CALLSTACK:$(Get-PSCallStack | Out-String)
"@
        throw $msg
    }
}

InstallAppveyorTools

mvn verify "--batch-mode" "-B" "-e" "-V"
CheckLastExitCode
