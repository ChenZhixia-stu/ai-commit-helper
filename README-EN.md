# AI Commit Helper

AI Commit Helper is an IntelliJ IDEA plugin that generates a commit message from the currently selected changes in the Commit tool window and automatically fills it into the commit message editor.

## Features

- Adds an `AI Generate Git Commit Message` action near the commit message area in the Commit tool window.
- Reads only the changes that are currently selected for commit.
- Uses IntelliJ Platform VCS APIs to collect code changes, without relying on the `git diff` command line.
- Calls the DeepSeek OpenAI-compatible Chat Completions API to generate commit messages.
- Stores the API key in IntelliJ IDEA PasswordSafe. Normal settings such as URL and model are stored as global plugin settings.
- Supports configurable commit message templates for company-specific formats.
- Example output:

```text
Optimize private message feature

- Adjust private message list rendering
- Fix unread conversation state updates
- Add error handling for message APIs
```

## Local Development

This project uses Maven + Java. JetBrains officially recommends Gradle for IntelliJ Platform plugin development, but this project intentionally uses Maven and depends on a locally installed IntelliJ IDEA SDK.

Default IDEA installation path:

```text
F:/software/IntelliJ IDEA 2026.1
```

Compile:

```bash
mvn -q -DskipTests compile
```

When compiling against the IDEA 2026.1 SDK, JDK 21 or later is required because the SDK jars use Java 21 bytecode. The current `pom.xml` forks compilation with:

```text
F:/jdk22/bin/javac
```

So even if the current Maven process runs on Java 8, you can still run:

```bash
mvn -q package
```

If your JDK is not installed at `F:\jdk22`, override it like this:

```bash
mvn -q package -Dbuild.jdk.home="D:/Java/jdk-22"
```

If your IDEA installation path is different, override `idea.home`:

```bash
mvn -q -DskipTests compile -Didea.home="D:/Apps/IntelliJ IDEA"
```

Run tests:

```bash
mvn -q test
```

## Package as an Installable Plugin

First build the plugin jar:

```bash
mvn -q package
```

Jar output:

```text
target/ai-commit-helper-1.0.0-SNAPSHOT.jar
```

IDEA can install a plugin zip from disk. The zip should not simply contain a jar directly. Use the following standard structure:

```text
AI Commit Helper/
  lib/
    ai-commit-helper-1.0.0-SNAPSHOT.jar
```

You can generate the distributable zip with PowerShell:

```powershell
$version = '1.0.0-SNAPSHOT'
$staging = 'target\dist\AI Commit Helper'
$dist = "target/ai-commit-helper-$version.zip"

if (Test-Path 'target\dist') { Remove-Item -LiteralPath 'target\dist' -Recurse -Force }
New-Item -ItemType Directory -Force -Path "$staging\lib" | Out-Null
Copy-Item -LiteralPath "target/ai-commit-helper-$version.jar" -Destination "$staging/lib/ai-commit-helper-$version.jar" -Force
Compress-Archive -Path 'target\dist\AI Commit Helper' -DestinationPath $dist -Force
```

Or run the built-in Windows packaging script:

```powershell
.\package-plugin.ps1
```

If your JDK is not installed at `F:\jdk22`, set this environment variable before running the script:

```powershell
$env:AI_COMMIT_HELPER_JDK_HOME='D:\Java\jdk-22'
.\package-plugin.ps1
```

On macOS / Linux, use the shell packaging script:

```bash
export AI_COMMIT_HELPER_JDK_HOME="/Library/Java/JavaVirtualMachines/jdk-22.jdk/Contents/Home"
export AI_COMMIT_HELPER_IDEA_HOME="/Applications/IntelliJ IDEA.app/Contents"
./package-plugin.sh
```

If your IDEA installation path is different, update `AI_COMMIT_HELPER_IDEA_HOME`.

Generated zip:

```text
target/ai-commit-helper-1.0.0-SNAPSHOT.zip
```

To install the plugin:

1. Open IDEA.
2. Go to `Settings | Plugins`.
3. Click the gear button.
4. Select `Install Plugin from Disk...`.
5. Choose `ai-commit-helper-1.0.0-SNAPSHOT.zip`.
6. Restart IDEA.
7. Go to `Settings | Tools | AI Commit Helper` and configure the DeepSeek URL, model, and API key.

## Compatibility

- `plugin.xml` sets `since-build="221"`, targeting IDEA 2022.1 and later.
- Source and target bytecode are Java 8.
- Before publishing, verify compatibility with IDEA 2022.1, 2022.3, 2023.3, 2024.x, and the latest IDEA version using IDEA Plugin DevKit or JetBrains Plugin Verifier.

## Settings

Settings path:

```text
Settings | Tools | AI Commit Helper
```

Available settings:

- Base URL: default `https://api.deepseek.com`
- Model: default `deepseek-v4-flash`
- API Key: stored in IntelliJ PasswordSafe
- Timeout Seconds: request timeout
- Max Diff Characters: maximum diff characters sent to the model
- Language: output language, default Chinese
- Message Template Preset: built-in commit message template preset
- Message Template: final commit message template
- Template Variables: custom `key=value` variables used by the template

The plugin sends only the currently selected changes in the Commit tool window to the configured API URL.
