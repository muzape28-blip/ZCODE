#!/usr/bin/env bash
# setup_compile_sandbox.sh — bangun HAKIM KOMPILASI KOTLIN LOKAL di sandbox Arena
#
# Latar (2026-09-10, sesi v1.0.23): klaim lama "sandbox tanpa JDK/SDK tidak
# bisa compile Kotlin" GUGUR setelah diuji — egress ke dl.google.com,
# services.gradle.org, repo.maven.apache.org, dan github.com/releases
# terjangkau. Dengan toolchain ini, error kompilasi tertangkap lokal dalam
# ~2-3 menit (bukti: 2 error run CI 34478996281 ditemukan & difix lokal
# dalam satu giliran; sebelumnya hanya bisa menebak — SKILL 1/28.5).
#
# ATURAN KERAS (SKILL 5):
#   - Semua barang besar di /var/tmp — DI LUAR workspace snapshot.
#   - /var/tmp EPHEMERAL: bisa raib kapan pun (terbukti berkali-kali).
#     Script ini IDEMPOTENT — jalankan ulang untuk membangun kembali
#     (~10 menit, ±1 GB download).
#   - Setelah dipakai: biarkan mati bersama sandbox; JANGAN pindahkan
#     image/SDK ke /home/user (disk workspace = kegagalan clear workspace).
#
# Pinned versions (anti-drift, prinsip F-09):
#   JDK      : Temurin 17.0.20.1+1 (x64 linux)
#   SDK      : cmdline-tools 11076708, platform-34, build-tools 34.0.0
#   buildPy  : CPython 3.11.16+20260901 (python-build-standalone,
#              install_only) — Chaquopy butuh Python 3.11 sebagai buildPython
#              (sandbox default 3.13 TIDAK cocok).
#
# Status: perintah penyusun TERBUKTI manual pada sesi 2026-09-10
# (compileDebugKotlin + testDebugUnitTest 35/35 hijau). Script ini merangkum
# perintah yang sama; smoke-run penuh script sebagai unit = saat pemakaian
# berikutnya (jujur: belum).
set -euo pipefail

VAR_TMP="${VAR_TMP:-/var/tmp}"
SDK="$VAR_TMP/android-sdk"

JDK_URL="https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.20.1%2B1/OpenJDK17U-jdk_x64_linux_hotspot_17.0.20.1_1.tar.gz"
CMDTOOLS_URL="https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
CPY_URL="https://github.com/astral-sh/python-build-standalone/releases/download/20260901/cpython-3.11.16%2B20260901-x86_64-unknown-linux-gnu-install_only.tar.gz"

echo "== [1/4] JDK 17 (Temurin) =="
if ls -d "$VAR_TMP"/jdk-17.* >/dev/null 2>&1; then
  echo "   sudah ada: $(ls -d "$VAR_TMP"/jdk-17.* | head -1) — skip"
else
  curl -sL --retry 3 -o "$VAR_TMP/jdk17.tar.gz" "$JDK_URL"
  tar xzf "$VAR_TMP/jdk17.tar.gz" -C "$VAR_TMP"
  rm -f "$VAR_TMP/jdk17.tar.gz"   # arsip = sampah setelah ekstrak (pelajaran 2026-09-10)
fi

echo "== [2/4] Android SDK (cmdline-tools + platform-34 + build-tools 34) =="
if [ -d "$SDK/platforms/android-34" ]; then
  echo "   platform-34 sudah ada — skip"
else
  curl -sL --retry 3 -o "$VAR_TMP/cmdtools.zip" "$CMDTOOLS_URL"
  mkdir -p "$SDK/cmdline-tools"
  unzip -q "$VAR_TMP/cmdtools.zip" -d "$SDK/cmdline-tools"
  [ -d "$SDK/cmdline-tools/latest" ] || mv "$SDK/cmdline-tools/cmdline-tools" "$SDK/cmdline-tools/latest"
  rm -f "$VAR_TMP/cmdtools.zip"
  export JAVA_HOME
  JAVA_HOME="$(ls -d "$VAR_TMP"/jdk-17.* | head -1)"
  yes | "$SDK/cmdline-tools/latest/bin/sdkmanager" --licenses >/dev/null 2>&1 || true
  "$SDK/cmdline-tools/latest/bin/sdkmanager" "platforms;android-34" "build-tools;34.0.0" "platform-tools" >/dev/null
fi

echo "== [3/4] CPython 3.11 (buildPython Chaquopy) =="
if [ -x "$VAR_TMP/python/bin/python3.11" ]; then
  echo "   sudah ada — skip"
else
  curl -sL --retry 3 -o "$VAR_TMP/cpy311.tar.gz" "$CPY_URL"
  tar xzf "$VAR_TMP/cpy311.tar.gz" -C "$VAR_TMP"   # ekstrak ke python/
  rm -f "$VAR_TMP/cpy311.tar.gz"
fi

echo "== [4/4] Verifikasi =="
JDK_DIR="$(ls -d "$VAR_TMP"/jdk-17.* | head -1)"
"$JDK_DIR/bin/java" -version 2>&1 | head -1
"$VAR_TMP/python/bin/python3.11" --version
[ -d "$SDK/platforms/android-34" ] && echo "platform-34 OK"
[ -d "$SDK/build-tools/34.0.0" ] && echo "build-tools 34.0.0 OK"

cat << USAGE

== SIAP. Cara pakai (dari root repo ZCODE) =====================

  export JAVA_HOME='"$JDK_DIR"'
  export ANDROID_HOME='"$SDK"'
  export PATH='"$VAR_TMP"'/python/bin:$JAVA_HOME/bin:$PATH

  # RAM-safe (sandbox 1.9 GB — SKILL 5.4):
  ./gradlew :app:compileDebugKotlin --max-workers=1 \\
    -Dorg.gradle.jvmargs="-Xmx768m -XX:MaxMetaspaceSize=384m" --no-daemon

  # Unit test JVM (butuh buildPython 3.11 utk task Chaquopy):
  ./gradlew :app:testDebugUnitTest --max-workers=1 \\
    -Dorg.gradle.jvmargs="-Xmx768m -XX:MaxMetaspaceSize=384m" --no-daemon

  # Hasil test: app/build/test-results/testDebugUnitTest/*.xml
================================================================
USAGE
