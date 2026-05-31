$ErrorActionPreference = 'Stop'

$version = '1.0.0-SNAPSHOT'
$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$jdkHome = if ($env:AI_COMMIT_HELPER_JDK_HOME) { $env:AI_COMMIT_HELPER_JDK_HOME } else { 'F:\jdk22' }

Push-Location $projectRoot
try {
    $env:JAVA_HOME = $jdkHome
    $env:Path = "$jdkHome\bin;$env:Path"

    mvn -q package

    $staging = "target\dist\AI Commit Helper"
    $zip = "target\ai-commit-helper-$version.zip"

    if (Test-Path 'target\dist') {
        Remove-Item -LiteralPath 'target\dist' -Recurse -Force
    }
    New-Item -ItemType Directory -Force -Path "$staging\lib" | Out-Null
    Copy-Item -LiteralPath "target\ai-commit-helper-$version.jar" `
        -Destination "$staging\lib\ai-commit-helper-$version.jar" `
        -Force

    if (Test-Path $zip) {
        Remove-Item -LiteralPath $zip -Force
    }
    Compress-Archive -Path 'target\dist\AI Commit Helper' -DestinationPath $zip -Force

    Write-Host "Plugin package created: $projectRoot\$zip"
} finally {
    Pop-Location
}
