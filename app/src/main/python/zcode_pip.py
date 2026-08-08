"""
zcode_pip — instalasi package Python via pip in-process (Chaquopy).

FIX pip AssetPath bug:
- Chaquopy AssetFinder memberikan AssetPath tanpa .parent, pip 24+ iter_all_distributions fail
- Kita monkey-patch pip._internal.metadata.importlib.envs untuk skip AssetPath
- Juga patch importlib.metadata yang dipakai pip untuk user_agent()
- Install ke user_packages yang writable: --target <workspace>/user_packages
- Set env PIP_DISABLE_PIP_VERSION_CHECK=1, PIP_NO_INPUT=1

Alur: Kotlin -> install_package(bridge, pkg) -> pip_main()
"""

import os
import sys
import traceback

from zcode_runner import BridgeStdout


def _patch_pip_for_chaquopy(bridge):
    """
    Patch pip agar tahan AssetPath tanpa parent (Chaquopy bug).
    Coba beberapa lokasi patch untuk kompatibilitas pip 22-24.
    """
    try:
        # Patch 1: pip._internal.metadata.importlib.envs
        try:
            from pip._internal.metadata.importlib import envs as pip_envs
            # Patch _iter_distributions atau iter_all_distributions
            if hasattr(pip_envs.Environment, "_iter_distributions"):
                orig = pip_envs.Environment._iter_distributions

                def safe_iter(self):
                    try:
                        # orig may yield, so iterate safely
                        for dist in orig(self):
                            try:
                                yield dist
                            except AttributeError as e:
                                if "AssetPath" in str(e) and "parent" in str(e):
                                    continue
                                raise
                    except AttributeError as e:
                        if "AssetPath" in str(e) and "parent" in str(e):
                            # skip entire broken env
                            return
                        raise

                pip_envs.Environment._iter_distributions = safe_iter
                bridge.write("[patch] pip_envs.Environment._iter_distributions patched\n")
        except Exception as e:
            bridge.write(f"[warn] patch envs failed: {e}\n")

        # Patch 2: pip._internal.metadata.base iter_all_distributions
        try:
            from pip._internal.metadata import base as pip_base
            if hasattr(pip_base, "BaseDistribution"):
                # Patch iter_all_distributions jika ada di base
                pass
        except Exception:
            pass

        # Patch 3: importlib.metadata - pip pakai stdlib importlib.metadata untuk user_agent
        try:
            import importlib.metadata as std_meta

            # Patch distributions() untuk skip AssetPath
            orig_dists = getattr(std_meta, "distributions", None)
            if orig_dists is not None:

                def safe_distributions(*args, **kwargs):
                    try:
                        for dist in orig_dists(*args, **kwargs):
                            try:
                                # akses _path atau file yang mungkin trigger parent
                                # force evaluation
                                _ = dist.version
                                yield dist
                            except AttributeError as ae:
                                if "AssetPath" in str(ae) and "parent" in str(ae):
                                    continue
                                # coba tetap skip
                                continue
                            except Exception:
                                continue
                    except AttributeError as ae:
                        if "AssetPath" in str(ae) and "parent" in str(ae):
                            return
                        raise
                    except Exception:
                        # fallback: return kosong
                        return

                std_meta.distributions = safe_distributions
                bridge.write("[patch] importlib.metadata.distributions patched\n")
        except Exception as e:
            bridge.write(f"[warn] patch std_meta failed: {e}\n")

        # Patch 4: pip._internal.network.session.user_agent - jangan panggil get_distribution
        try:
            from pip._internal.network import session as pip_session

            if hasattr(pip_session, "user_agent"):

                def safe_user_agent():
                    return "zcode-pip/1.0 Chaquopy/3.11"

                pip_session.user_agent = safe_user_agent
                bridge.write("[patch] pip_session.user_agent patched\n")
        except Exception as e:
            bridge.write(f"[warn] patch user_agent failed: {e}\n")

    except Exception as e:
        bridge.write(f"[warn] overall pip patch failed: {e}\n{traceback.format_exc()}\n")


def _get_pip_main():
    try:
        from pip._internal.cli.main import main as pip_main
        return pip_main
    except ImportError:
        pass
    try:
        from pip._internal import main as pip_main_mod
        return pip_main_mod
    except ImportError:
        pass
    try:
        from pip import main as pip_main_old
        return pip_main_old
    except ImportError:
        return None


def _get_user_packages_dir(bridge):
    try:
        ws = bridge.workspaceDir()
        up = os.path.join(ws, "user_packages")
        os.makedirs(up, exist_ok=True)
        return up
    except Exception:
        return None


def install_package(bridge, pkg):
    old_out, old_err = sys.stdout, sys.stderr
    out = BridgeStdout(bridge)
    sys.stdout = out
    sys.stderr = out

    # Set env untuk pip
    os.environ["PIP_DISABLE_PIP_VERSION_CHECK"] = "1"
    os.environ["PIP_NO_INPUT"] = "1"
    os.environ["PIP_NO_WARN_SCRIPT_LOCATION"] = "1"
    os.environ["PIP_DISABLE_PIP_VERSION_CHECK"] = "1"
    os.environ["PYTHONIOENCODING"] = "utf-8"

    try:
        pip_main = _get_pip_main()
        if pip_main is None:
            bridge.write("\n❌ pip module tidak tersedia.\n")
            bridge.write("Pastikan build.gradle: pip { install \"pip==23.3.1\" }\n")
            bridge.onExit(1, None)
            return

        # Patch pip untuk Chaquopy AssetPath bug
        _patch_pip_for_chaquopy(bridge)

        user_pkg = _get_user_packages_dir(bridge)
        bridge.write(f"\n[info] workspace: {bridge.workspaceDir()}\n")
        if user_pkg:
            bridge.write(f"[info] target: {user_pkg}\n")

        # Build argv: pakai --target agar writable, --no-build-isolation untuk hindari metadata scan ekstra?
        # --disable-pip-version-check sudah via env
        # --no-warn-script-location sudah via env
        argv = ["pip", "install", "--no-input", "--disable-pip-version-check"]
        if user_pkg:
            # --target memaksa install ke folder writable
            argv += ["--target", user_pkg, "--upgrade"]
        # Untuk hindari build isolation yang butuh internet ekstra
        # argv += ["--no-build-isolation"]  # bisa dicoba jika masih fail

        argv.append(pkg)

        bridge.write(f"[cmd] {' '.join(argv)}\n\n")
        sys.argv = argv

        try:
            code = pip_main()
        except SystemExit as se:
            c = se.code
            code = c if isinstance(c, int) else (0 if c is None else 1)

        if code is None:
            code = 0
        if not isinstance(code, int):
            code = 0 if str(code).lower() in ("0", "none") else 1

        bridge.write(f"\n[exit] code={code}\n")

        # Jika sukses, pastikan user_packages masuk sys.path untuk next run
        if code == 0 and user_pkg:
            if user_pkg not in sys.path:
                sys.path.insert(0, user_pkg)
            bridge.write(f"[ok] {pkg} installed to {user_pkg}\n")
            bridge.write("Tip: restart terminal / run lagi agar import bisa.\n")

        bridge.onExit(code, None)

    except SystemExit as e:
        code = e.code if isinstance(e.code, int) else (0 if e.code is None else 1)
        bridge.onExit(code, None)
    except BaseException as e:
        bridge.onExit(1, traceback.format_exc())
    finally:
        sys.stdout, sys.stderr = old_out, old_err
