# 🧠 Engine Spike Intelligence

**Status:** IMPLEMENTED LOCALLY (v1.0.21)
**Target Platform:** Android ARMv7 32-bit / Chaquopy (CPython 3.11) & Offline IDE Desktop

---

## 🎯 Overview

Engine Spike Intelligence adalah sistem intelijen editor terpadu di Python layer ZCODE (`app/src/main/python/editor/`). Engine ini menggabungkan 5 (lima) sub-engine analisis kode Python terpopuler ke dalam satu arsitektur terstruktur, modular, dan tangguh (*fault-tolerant*):

1. **Jedi Layer** — Autocompletion & Go-to Definition berbasis analisis konteks.
2. **Parso Layer** — AST Parsing & Syntax Error Recovery.
3. **Pyflakes Layer** — Fast, in-memory Static Analysis & Linting.
4. **Cabe Layer** — McCabe Cyclomatic Complexity & Maintainability Index estimation.
5. **Rope Layer** — Safe Code Refactoring (Symbol Rename & Extraction).

---

## 🏗️ Diagram Arsitektur

```
                         +-----------------------------+
                         |     zcode_plugins.py        |
                         | (PyCall / Java Bridge API)  |
                         +--------------+--------------+
                                        |
                                        v
                         +-----------------------------+
                         |   SpikeIntelligenceEngine   |
                         |   (Orchestrator & Lazy Load)|
                         +--------------+--------------+
                                        |
      +-----------------+---------------+---------------+-----------------+
      |                 |               |               |                 |
      v                 v               v               v                 v
+-----------+     +-----------+   +-----------+   +-----------+     +-----------+
| JediLayer |     |ParsoLayer |   |PyflakesL. |   | CabeLayer |     | RopeLayer |
+-----------+     +-----------+   +-----------+   +-----------+     +-----------+
```

---

## ⚠️ Error Hierarchy (`editor/__init__.py`)

Semua exception sub-engine diturunkan dari `SpikeError` untuk memastikan penanganan error hierarkis dan *graceful degradation* tanpa merusak runtime Android ZCODE:

```
SpikeError (Base Exception)
 ├── JediError
 ├── ParsoError
 ├── PyflakesError
 ├── CabeError
 └── RopeError
```

---

## 🛠️ Modul & Cara Penggunaan

### 1. Inisialisasi Orchestrator (Lazy Loaded)
```python
from editor.intelligence_engine import SpikeIntelligenceEngine

engine = SpikeIntelligenceEngine()
print(engine.health_check())
```

### 2. Autocompletion (Jedi Layer)
```python
code = "import os\nos."
completions = engine.autocomplete(code, position=(2, 3))
```

### 3. Static Analysis / Linting (Pyflakes Layer)
```python
code = "def foo():\n    x = 10\n    return 42"
issues = engine.lint(code, filename="main.py")
```

### 4. Cyclomatic Complexity (Cabe Layer)
```python
code = "def complex_fn(x):\n    if x > 0:\n        for i in range(x):\n            print(i)"
analysis = engine.analyze_complexity(code, threshold=3)
```

### 5. Symbol Refactoring (Rope Layer)
```python
code = "def old_fn(): return 1"
new_code = engine.refactor(code, operation="rename", old_name="old_fn", new_name="new_fn")
```

---

## 🧪 Testing & Verifikasi

- **Test Suite**: `test_engine_spike_intelligence.py` (20 unit & integration tests)
- **Eksekusi Test**:
  ```bash
  pytest test_engine_spike_intelligence.py -v
  ```
- **Status Verification**: `IMPLEMENTED LOCALLY` & `LOCALLY VERIFIED`.

---

## 📝 Lisensi & Provenance

Engine Spike Intelligence dikembangkan di bawah standar GPLv3 Option B ZCODE.
