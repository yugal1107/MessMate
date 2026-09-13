#!/usr/bin/env bash
# Locates a JDK 21 install on this machine and prints its JAVA_HOME.
# Prints nothing if none is found. Works regardless of what the default
# `java` on PATH happens to be, so `make build` behaves the same on any
# machine that has a JDK 21 installed somewhere.

set -u

is_java21() {
  local java_bin="$1/bin/java"
  [ -x "$java_bin" ] || return 1
  "$java_bin" -version 2>&1 | grep -q '"21\.'
}

# 1. Already-correct JAVA_HOME
if [ -n "${JAVA_HOME:-}" ] && is_java21 "$JAVA_HOME"; then
  echo "$JAVA_HOME"
  exit 0
fi

# 2. macOS
if command -v /usr/libexec/java_home >/dev/null 2>&1; then
  candidate="$(/usr/libexec/java_home -v 21 2>/dev/null)"
  if [ -n "$candidate" ] && is_java21 "$candidate"; then
    echo "$candidate"
    exit 0
  fi
fi

# 3. Linux distro packages (Fedora/Debian/Ubuntu/RHEL layouts)
for dir in /usr/lib/jvm/*21* /usr/lib/jvm/*-21 /usr/lib/jvm/*-21-*; do
  [ -d "$dir" ] || continue
  if is_java21 "$dir"; then
    echo "$dir"
    exit 0
  fi
done

# 4. SDKMAN-managed installs
for dir in "$HOME"/.sdkman/candidates/java/*21*; do
  [ -d "$dir" ] || continue
  if is_java21 "$dir"; then
    echo "$dir"
    exit 0
  fi
done

# 5. jenv-managed installs
for dir in "$HOME"/.jenv/versions/21*; do
  [ -d "$dir" ] || continue
  if is_java21 "$dir"; then
    echo "$dir"
    exit 0
  fi
done

exit 1
