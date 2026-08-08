"""
zcode_pip — instalasi package Python via pip in-process (Chaquopy).

Alur: Kotlin memanggil `install_package(bridge, pkg)`.
- stdout/stderr pip dialihkan ke bridge → log streaming di PipScreen
- paket di-install ke site-packages privat aplikasi (offline-first tetap terjaga)

FIX: pip ModuleNotFoundError
- Chaquopy 3.11 tidak bundle pip by default, sekarang dibundle via build.gradle pip { install "pip" }
- Coba multi-import path (pip >=24 API berubah) + fallback pesan ramah
"""

import sys

from zcode_runner import BridgeStdout


def _get_pip_main():
    # pip 20-23
    try:
        from pip._internal.cli.main import main as pip_main
        return pip_main
    except ImportError:
        pass
    # pip 23+ alternative
    try:
        from pip._internal import main as pip_main_mod
        return pip_main_mod
    except ImportError:
        pass
    # sangat lama pip <19
    try:
        from pip import main as pip_main_old
        return pip_main_old
    except ImportError:
        return None


def install_package(bridge, pkg):
    old_out, old_err = sys.stdout, sys.stderr
    out = BridgeStdout(bridge)
    sys.stdout = out
    sys.stderr = out
    try:
        pip_main = _get_pip_main()
        if pip_main is None:
            bridge.write("\n❌ pip module tidak tersedia.\n")
            bridge.write("Pastikan build.gradle mengandung:\n")
            bridge.write("chaquopy { defaultConfig { pip { install \"pip\" } } }\n")
            bridge.write("Lalu rebuild APK. Jika masih fail, coba pip install via desktop.\n\n")
            bridge.onExit(1, None)
            return

        # Chaquopy: install ke site-packages app
        # argv harus list lengkap
        sys.argv = ["pip", "install", "--no-input", pkg]
        try:
            code = pip_main()
        except SystemExit as se:
            c = se.code
            code = c if isinstance(c, int) else (0 if c is None else 1)

        # Normalisasi exit code None -> 0
        if code is None:
            code = 0
        if not isinstance(code, int):
            code = 0 if str(code).lower() in ("0", "none") else 1

        bridge.onExit(code, None)

    except SystemExit as e:
        code = e.code if isinstance(e.code, int) else (0 if e.code is None else 1)
        bridge.onExit(code, None)
    except BaseException as e:
        import traceback
        bridge.onExit(1, traceback.format_exc())
    finally:
        sys.stdout, sys.stderr = old_out, old_err
