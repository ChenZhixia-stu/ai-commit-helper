#!/usr/bin/env bash
set -euo pipefail

VERSION="1.0.0-SNAPSHOT"
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JDK_HOME="${AI_COMMIT_HELPER_JDK_HOME:-${JAVA_HOME:-}}"
IDEA_HOME="${AI_COMMIT_HELPER_IDEA_HOME:-/Applications/IntelliJ IDEA.app/Contents}"

if [[ -z "$JDK_HOME" ]]; then
  echo "AI_COMMIT_HELPER_JDK_HOME or JAVA_HOME must be set to JDK 21+." >&2
  exit 1
fi

if [[ ! -x "$JDK_HOME/bin/javac" ]]; then
  echo "javac not found at: $JDK_HOME/bin/javac" >&2
  exit 1
fi

if [[ ! -d "$IDEA_HOME" ]]; then
  echo "IntelliJ IDEA home not found: $IDEA_HOME" >&2
  echo "Set AI_COMMIT_HELPER_IDEA_HOME to your IDEA installation home." >&2
  exit 1
fi

cd "$PROJECT_ROOT"

export JAVA_HOME="$JDK_HOME"
export PATH="$JDK_HOME/bin:$PATH"

mvn -q package -Dbuild.jdk.home="$JDK_HOME" -Didea.home="$IDEA_HOME"

STAGING="target/dist/AI Commit Helper"
ZIP="target/ai-commit-helper-$VERSION.zip"
JAR="target/ai-commit-helper-$VERSION.jar"

rm -rf "target/dist"
mkdir -p "$STAGING/lib"
cp "$JAR" "$STAGING/lib/ai-commit-helper-$VERSION.jar"
rm -f "$ZIP"

(
  cd "target/dist"
  zip -qr "../ai-commit-helper-$VERSION.zip" "AI Commit Helper"
)

echo "Plugin package created: $PROJECT_ROOT/$ZIP"
