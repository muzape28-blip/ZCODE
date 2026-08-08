"""
zcode_runner — runner dalam proses Chaquopy untuk ZCODE.

FIX:
- sys.path include user_packages (pip --target)
- cwd = workspace
- stdin/stdout/stderr bridge
"""

import os
import runpy
import sys
import threading


class BridgeStdout:
    def __init__(self, bridge):
        self._bridge = bridge

    def write(self, s):
        if s:
            self._bridge.write(str(s))
        return len(s)

    def flush(self):
        pass

    def isatty(self):
        return True


class BridgeStdin:
    def __init__(self, bridge):
        self._bridge = bridge
        self._buffer = ""

    def readline(self, limit=-1):
        while "\n" not in self._buffer:
            if self._bridge.isInterrupted():
                raise KeyboardInterrupt
            chunk = self._bridge.readLine()
            if chunk is None:
                continue
            self._buffer += chunk
        line, self._buffer = self._buffer.split("\n", 1)
        if limit >= 0:
            line = line[:limit]
        return line + "\n"

    def read(self, size=-1):
        line = self.readline()
        if size >= 0:
            line = line[:size]
        return line

    def isatty(self):
        return True


def _ensure_user_packages_in_path(workspace_dir):
    try:
        up = os.path.join(workspace_dir, "user_packages")
        if os.path.isdir(up) and up not in sys.path:
            sys.path.insert(0, up)
        # juga coba site-packages di dalam workspace
        # untuk kompatibilitas
        return up
    except Exception:
        return None


def run_script(bridge, script_path):
    try:
        try:
            os.chdir(bridge.workspaceDir())
        except OSError:
            pass

        ws = bridge.workspaceDir()
        _ensure_user_packages_in_path(ws)

        sys.stdin = BridgeStdin(bridge)
        sys.stdout = BridgeStdout(bridge)
        sys.stderr = BridgeStdout(bridge)

        # workspace dan user_packages harus di sys.path paling depan
        if ws not in sys.path:
            sys.path.insert(0, ws)
        _ensure_user_packages_in_path(ws)

        worker = threading.current_thread()
        try:
            bridge.setWorkerThread(worker)
        except Exception:
            pass

        runpy.run_path(script_path, run_name="__main__")
        bridge.onExit(0, None)
    except SystemExit as e:
        code = e.code if isinstance(e.code, int) else (0 if e.code is None else 1)
        bridge.onExit(code, None)
    except KeyboardInterrupt:
        bridge.onExit(130, None)
    except BaseException:
        import traceback
        bridge.onExit(1, traceback.format_exc())
