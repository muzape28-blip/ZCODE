# 🧠 Engine Spike Intelligence

**Status:** IMPLEMENTED LOCALLY (v1.0.21)
**Target:** Meningkatkan kemampuan pengembangan dengan integrasi tooling cerdas

## 🎯 Overview

Engine Spike Intelligence adalah sistem intelijen editor yang terintegrasi dalam ZCODE, menggabungkan kekuatan dari 5 (lima) tooling populer:

1. **Jedi** - Autocompletion dan code navigation
2. **Parso** - AST parsing dan syntax analysis
3. **Pyflakes** - Static analysis dan linting
4. **Cabe (McCabe)** - Complexity analysis
5. **Rope** - Code refactoring

## 🔧 Fitur Utama

| Fitur | Deskripsi | Contoh Penggunaan |
|-------|------------|-------------------|
| **Autocompletion** | Suggest code completions | `ctrl+space` untuk melihat saran |
| **Goto Definition** | Navigasi ke definisi simbol | Klik kanan → "Go to Definition" |
| **Static Analysis** | Deteksi error dan warning | Menampilkan peringatan untuk variabel yang tidak digunakan |
| **Complexity Analysis** | Ukur kompleksitas kode | Menampilkan skor McCabe untuk fungsi |
| **Code Refactoring** | Restrukturisasi kode | Ekstrak metode, rename simbol, dll |
| **AST Visualization** | Lihat struktur kode | Debugging struktur kode yang kompleks |

## 🛠️ Cara Menggunakan

### 1. Akses Engine
```python
from editor.intelligence_engine import SpikeIntelligenceEngine

engine = SpikeIntelligenceEngine()
```

### 2. Fitur Autocompletion
```python
completions = engine.autocomplete("""
def hello():
    pass

hello()
""", (5, 0))
```

### 3. Static Analysis
```python
issues = engine.lint("""
def test():
    x = 10  # unused variable
    return 42
""")
```

### 4. Complexity Analysis
```python
analysis = engine.analyze_complexity("""
def complex_function():
    if True:
        for i in range(10):
            if i % 2 == 0:
                print(i)
""")
```

### 5. Code Refactoring
```python
refactored = engine.refactor("""
def old_function():
    x = 10
    return x
""", "rename", old_name="old_function", new_name="new_function", line=1)
```

## 🔧 Integrasi dengan ZCODE

Engine Spike Intelligence terintegrasi sebagai plugin inti ZCODE. Anda dapat mengaksesnya melalui:

```python
from zcode_plugins import plugin_manager

spike_engine = plugin_manager.get_plugin('spike_intelligence')
```

## 📋 Status Implementasi

| Komponen | Status | Catatan |
|----------|--------|---------|
| Jedi | ✅ Implemented | Autocompletion dan navigation |
| Parso | ✅ Implemented | AST parsing dan error detection |
| Pyflakes | ✅ Implemented | Static analysis dan linting |
| Cabe | ✅ Implemented | Complexity analysis |
| Rope | ✅ Implemented | Refactoring tools |
| Integrasi Plugin | ✅ Implemented | Terdaftar sebagai plugin inti |

## 🧪 Testing

Unit tests tersedia di `test_engine_spike_intelligence.py` yang mencakup:
- Inisialisasi engine
- Fungsionalitas setiap komponen
- Integrasi antar komponen
- Skema error handling

## 📦 Dependencies

Engine Spike Intelligence membutuhkan package Python berikut:
```
jedi
parso
pyflakes
mccabe
rope
```

Package ini akan diinstal secara otomatis sebagai bagian dari ZCODE dependencies.

## 🚀 Roadmap

| Versi | Rencana Peningkatan |
|-------|---------------------|
| v1.0.21 | Implementasi dasar + integrasi |
| v1.0.22 | Performance optimization |
| v1.0.23 | UI integration untuk hasil analisis |
| v1.1.0 | Machine learning untuk saran yang lebih cerdas |

## ⚠️ Keterbatasan

1. Beberapa fitur refactoring Rope mungkin membutuhkan konfigurasi tambahan
2. Performance dapat terpengaruh pada file kode yang sangat besar
3. Fitur autocompletion mungkin terbatas pada Python code

## 🤝 Kontribusi

Kami menyambut kontribusi untuk:
- Meningkatkan akurasi analisis
- Menambahkan dukungan untuk bahasa pemrograman lain
- Meningkatkan performa dan efisiensi

Silakan buka issue atau pull request di repository ZCODE.

## 📝 Lisensi

Engine Spike Intelligence adalah bagian dari ZCODE dan dilisensikan di bawah GPLv3.

## 🔄 Pola Integrasi Baru

Engine sekarang menggunakan **lazy loading** dan **graceful degradation**:

```python
# Contoh penggunaan dengan graceful degradation
engine = SpikeIntelligenceEngine()

# Jika Jedi tidak tersedia, akan menggunakan fallback
completions = engine.autocomplete(code, (line, column))

# Cek status komponen
health = engine.health_check()
if not health['jedi']:
    print("Warning: Jedi not available - autocompletion may be limited")
```

## 🔄 Caching dan Performance

Setiap komponen menggunakan **caching** untuk meningkatkan performa:

```python
# Jedi menggunakan caching untuk autocompletion
completions = engine.autocomplete(code, (line, column))  # Cached

# Parso menggunakan caching untuk parsing AST
ast = engine.parse_ast(code)  # Cached
```

## 🔄 Error Handling

Setiap operasi memiliki **error handling** yang komprehensif:

```python
# Error handling untuk linting
issues = engine.lint(code)
if not issues:
    print("No issues found")
```

## 🔄 Dokumentasi API

### `SpikeIntelligenceEngine`

| Method | Deskripsi | Parameter |
|--------|------------|-----------|
| `autocomplete` | Mendapatkan saran kode | `code: str`, `position: Tuple[int, int]` |
| `parse_ast` | Parsing AST | `code: str` |
| `lint` | Static analysis | `code: str` |
| `analyze_complexity` | Analisis kompleksitas | `code: str` |
| `refactor` | Refactoring kode | `code: str`, `operation: str`, `**kwargs` |
| `health_check` | Cek status komponen | - |

### `JediWrapper`

| Method | Deskripsi | Parameter |
|--------|------------|-----------|
| `get_completions` | Mendapatkan saran kode | `code: str`, `position: Tuple[int, int]` |
| `get_definitions` | Mendapatkan definisi simbol | `code: str`, `position: Tuple[int, int]` |

### `ParsoWrapper`

| Method | Deskripsi | Parameter |
|--------|------------|-----------|
| `parse` | Parsing AST | `code: str` |
| `get_errors` | Mendapatkan error sintaks | `code: str` |

### `PyflakesWrapper`

| Method | Deskripsi | Parameter |
|--------|------------|-----------|
| `lint` | Static analysis | `code: str` |

### `CabeWrapper`

| Method | Deskripsi | Parameter |
|--------|------------|-----------|
| `analyze` | Analisis kompleksitas | `code: str` |

### `RopeWrapper`

| Method | Deskripsi | Parameter |
|--------|------------|-----------|
| `refactor` | Refactoring kode | `code: str`, `operation: str`, `**kwargs` |

## 🔄 Contoh Penggunaan Lanjutan

### 1. Autocompletion dengan Fallback
```python
engine = SpikeIntelligenceEngine()

# Coba autocompletion
completions = engine.autocomplete(code, (line, column))

# Jika tidak ada saran, coba dengan fallback
if not completions:
    print("No completions found - trying fallback")
    completions = engine.jedi_provider.get_completions(code, (line, column))
```

### 2. Analisis Kompleksitas dengan Visualisasi
```python
analysis = engine.analyze_complexity(code)

# Visualisasi hasil analisis
print(f"Complexity: {analysis['cyclomatic_complexity']}")
print(f"Maintainability: {analysis['maintainability_index']:.1f}%")
```

### 3. Refactoring dengan Error Handling
```python
try:
    refactored = engine.refactor(
        code,
        "rename",
        old_name="old_function",
        new_name="new_function",
        line=1
    )
    print("Refactoring successful")
except Exception as e:
    print(f"Refactoring failed: {e}")
```

## 🔄 Best Practices

1. **Gunakan `health_check()`** sebelum menggunakan fitur tertentu:
```python
if engine.health_check()['jedi']:
    completions = engine.autocomplete(code, (line, column))
```

2. **Batasi ukuran file** untuk operasi berat (refactoring, complexity analysis):
```python
if len(code) > 10000:  # 10K karakter
    print("Warning: Large file - analysis may be slow")
```

3. **Gunakan caching** untuk operasi berulang:
```python
# Jedi dan Parso sudah menggunakan caching
completions = engine.autocomplete(code, (line, column))  # Cached
ast = engine.parse_ast(code)  # Cached
```

4. **Error handling** untuk semua operasi:
```python
try:
    issues = engine.lint(code)
except Exception as e:
    print(f"Linting failed: {e}")
```

## 🔄 Troubleshooting

### 1. Komponen tidak tersedia
```python
health = engine.health_check()
if not health['jedi']:
    print("Jedi not available - install with: pip install jedi")
```

### 2. Performance lambat
```python
# Untuk file besar, gunakan background thread
import threading

def analyze_in_background():
    analysis = engine.analyze_complexity(large_code)
    print(analysis)

thread = threading.Thread(target=analyze_in_background)
thread.start()
```

### 3. Error pada refactoring
```python
try:
    refactored = engine.refactor(code, "rename", old_name="old", new_name="new", line=1)
except Exception as e:
    print(f"Refactoring failed: {e}")
    print("Trying fallback...")
    refactored = engine.rope_refactorer.refactor(code, "rename", old_name="old", new_name="new", line=1)
```

## 🔄 FAQ

### Q: Mengapa autocompletion lambat?
A: Jedi menggunakan caching untuk meningkatkan performa. Untuk file besar, pertimbangkan untuk menggunakan background thread.

### Q: Bagaimana cara menambahkan dukungan untuk bahasa lain?
A: Saat ini engine hanya mendukung Python. Untuk menambahkan dukungan bahasa lain, Anda perlu mengimplementasikan wrapper untuk tooling yang relevan (seperti Jedi untuk JavaScript, Tree-sitter untuk banyak bahasa).

### Q: Bagaimana cara meningkatkan performa?
A: Gunakan caching, batasi ukuran file, dan gunakan background thread untuk operasi berat.

## 🔄 Kontribusi

Kami menyambut kontribusi untuk:
- Meningkatkan akurasi analisis
- Menambahkan dukungan untuk bahasa pemrograman lain
- Meningkatkan performa dan efisiensi

Silakan buka issue atau pull request di repository ZCODE.

## 📝 Lisensi

Engine Spike Intelligence adalah bagian dari ZCODE dan dilisensikan di bawah GPLv3.

## 📊 Hasil Benchmark Teoretis

### 1. Performa

| Komponen       | Waktu (ms) | Memory (MB) | Keterangan                     |
|----------------|-------------|--------------|--------------------------------|
| Jedi           | 50-500      | 30-100       | Terkadang lambat untuk proyek besar |
| Parso          | 10-200      | 5-50         | Cepat dan ringan               |
| Pyflakes       | 20-300      | 10-30        | Sangat cepat                  |
| Cabe           | 10-100      | <10          | Sangat cepat                  |
| Rope           | 100-2000    | 50-200       | Lambat untuk operasi berat    |

### 2. Kelebihan dan Kekurangan

| Komponen       | Kelebihan                          | Kekurangan                     |
|----------------|------------------------------------|--------------------------------|
| Jedi           | Akurasi tinggi, dukungan Python 3.11+ | Memory usage tinggi           |
| Parso          | Parsing cepat, error recovery baik | Kurang fitur advanced         |
| Pyflakes       | Ringan, integrasi mudah           | Kurang customizable           |
| Cabe           | Metrik McCabe standar             | Tidak mendukung Python 3.12+    |
| Rope           | Refactoring lengkap               | Kompleksitas tinggi            |

### 3. Rekomendasi Akhir

1. **Prioritaskan performa**:
   - Gunakan **Parso** untuk parsing AST (cepat dan ringan).
   - Gunakan **Pyflakes** untuk deteksi error (sangat cepat).
   - Gunakan **Cabe** untuk analisis kompleksitas (sangat cepat).

2. **Untuk fitur lanjutan**:
   - Gunakan **Jedi** untuk autocompletion (akurasi tinggi).
   - Gunakan **Rope** untuk refactoring (lengkap).

3. **Optimasi**:
   - Aktifkan **caching** untuk operasi berulang.
   - Gunakan **background thread** untuk operasi berat.
   - Batasi **ukuran file** untuk menghindari memory overflow.

## 📌 Catatan Penting

- **Performance** adalah prioritas utama. Gunakan komponen yang paling cepat untuk operasi yang sering digunakan.
- **Graceful degradation** memastikan engine tetap berfungsi bahkan jika beberapa komponen tidak tersedia.
- **Caching** sangat penting untuk meningkatkan performa pada operasi berulang.

## 🚀 Langkah Selanjutnya

1. **Optimasi performa** dengan:
   - Menggunakan caching secara agresif
   - Menggunakan background thread untuk operasi berat
   - Membatasi ukuran file untuk analisis

2. **Menambahkan dukungan untuk bahasa lain** dengan:
   - Mengimplementasikan wrapper untuk tooling yang relevan
   - Menambahkan deteksi bahasa secara otomatis

3. **Meningkatkan akurasi analisis** dengan:
   - Menggunakan kombinasi tooling yang lebih cerdas
   - Menambahkan machine learning untuk saran yang lebih baik

## 📝 Dokumentasi Tambahan

### 1. Contoh Kode Lengkap

```python
# Contoh penggunaan lengkap dengan error handling dan caching
from editor.intelligence_engine import SpikeIntelligenceEngine

# Inisialisasi engine
engine = SpikeIntelligenceEngine()

# Cek status komponen
health = engine.health_check()
if not health['jedi']:
    print("Warning: Jedi not available - autocompletion may be limited")

# Autocompletion dengan caching
code = """
def hello():
    pass

hello()
"""
completions = engine.autocomplete(code, (5, 0))

# Parsing AST dengan caching
ast = engine.parse_ast(code)

# Static analysis dengan error handling
try:
    issues = engine.lint(code)
except Exception as e:
    print(f"Linting failed: {e}")

# Analisis kompleksitas dengan visualisasi
analysis = engine.analyze_complexity(code)
print(f"Complexity: {analysis['cyclomatic_complexity']}")
print(f"Maintainability: {analysis['maintainability_index']:.1f}%")

# Refactoring dengan error handling
try:
    refactored = engine.refactor(
        code,
        "rename",
        old_name="hello",
        new_name="greet",
        line=1
    )
except Exception as e:
    print(f"Refactoring failed: {e}")
```

### 2. Contoh Penggunaan dalam Proyek

```python
# Contoh penggunaan dalam proyek ZCODE
from zcode_plugins import plugin_manager

# Akses engine melalui plugin manager
spike_engine = plugin_manager.get_plugin('spike_intelligence')

# Gunakan engine untuk analisis kode
code = """
def calculate_average(numbers):
    total = sum(numbers)
    count = len(numbers)
    return total / count
"""

# Analisis kompleksitas
analysis = spike_engine.analyze_complexity(code)
print(f"Complexity: {analysis['cyclomatic_complexity']}")

# Static analysis
issues = spike_engine.lint(code)
if issues:
    print("Issues found:")
    for issue in issues:
        print(f"Line {issue['line']}: {issue['message']}")
```

## 📌 Kesimpulan

Engine Spike Intelligence telah berhasil diimplementasikan dengan:
- **Lazy loading** untuk efisiensi
- **Graceful degradation** untuk stabilitas
- **Caching** untuk performa
- **Error handling** untuk keandalan

Engine ini siap untuk digunakan dalam ZCODE v1.0.21 dan akan terus dioptimasi untuk versi berikutnya.

Kami menyambut kontribusi untuk:
- Meningkatkan akurasi analisis
- Menambahkan dukungan untuk bahasa pemrograman lain
- Meningkatkan performa dan efisiensi

Silakan buka issue atau pull request di repository ZCODE.

## 📝 Lisensi

Engine Spike Intelligence adalah bagian dari ZCODE dan dilisensikan di bawah GPLv3.