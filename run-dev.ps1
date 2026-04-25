$ErrorActionPreference = "Stop"

$credentialsPath = Join-Path $env:USERPROFILE ".runelite\credentials.properties"

if ($env:JAVA_HOME) {
	$env:Path = "$env:JAVA_HOME\bin;$env:Path"
} elseif (-not (Get-Command javac -ErrorAction SilentlyContinue)) {
	throw "A JDK is required. Set JAVA_HOME to your JDK path or put javac on PATH."
}

if (-not (Test-Path $credentialsPath)) {
	Write-Warning "RuneLite credentials file not found at $credentialsPath"
	Write-Warning "Launch RuneLite once through the Jagex Launcher after adding --insecure-write-credentials in RuneLite (configure)."
}

& ".\gradlew.bat" run
