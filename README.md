# ZCODE — Zabacode Kotlin Edition

ZCODE is Zabacode rebuilt from scratch with **Kotlin + Android Native**.
It combines the simplicity of **Pydroid**, the depth and Workbench architecture of **VS Code**, and the mobile-touch optimization of **Acode** into a high-performance, offline-first, true-black OLED mobile editor.

Reference project: [muzape28-blip/ZABACODE](https://github.com/muzape28-blip/ZABACODE)

---

## 🚀 Fitur yang Selesai di-Build (Fase 1 & Fase 2)

### 🎨 1. Desain Total & Estetika Premium
*   **True-Black OLED Background (`#050806`):** Seluruh ruang coding dan terminal output didesain true-black untuk menghemat baterai AMOLED dan kenyamanan mata saat begadang.
*   **Tema Sinkron Adaptif:** Menyediakan 3 skema warna: **Dracula, Tokyo Night, dan Retro Green**. Mengubah warna sidebar, drawer, dialog, dan topbar secara harmonis sementara area ketik tetap hitam legas OLED.
*   **Touch Optimized Tab Bar:** Tab bar multi-file minimalis dengan ukuran font 12. Menutup file cukup dengan gerakan **Long-press/Hold Tab** (bebas salah pencet!).
*   **QuickTools & FAB Melayang:** Baris tombol pembantu (`Tab`, `:`, `;`, `'`, `#`, `(`, `)`, `[`, `]`, `def`, `return`, `import`) dengan border melengkung halus, di atas handle terdapat tombol FAB Jalankan (`▶`) melayang ergonomis.

### 📁 2. File Manager & Persistensi Workspace
*   **CRUD Berkecepatan Tinggi:** Membuat, menyimpan, mengubah nama (*Rename*), dan menghapus file langsung di dalam path aman `filesDir/files/` (terlindungi dari double nesting folder).
*   **Frictionless Creation:** Membuat file langsung dinamai `untitled_N.py` tanpa popup nama di awal.
*   **Workspace Recovery:** Status tab terbuka dan teks draf di editor tersimpan secara otomatis, sehingga langsung pulih sempurna bahkan ketika aplikasi ditutup paksa atau di-*swipe up* oleh OS.

### 💻 3. Terminal PTY Interaktif Full-Screen
*   **Spawning Python Real-Time:** Menjalankan python3 dengan unbuffered JNI/Subprocess mode (`python3 -u`), mengalirkan logs live karakter demi karakter lengkap dengan kursor blok phosphor.
*   **Ketik Langsung (No stdin box):** Sentuh area terminal untuk membuka keyboard standar dan langsung mengetik. Menekan tombol `Enter` mengirimkan baris ke standard input (`stdin`) proses Python secara instan.
*   **Interupsi Ctrl+C:** Menyediakan tombol merah Ctrl+C yang responsif di toolbar bawah untuk mengirimkan sinyal SIGINT dan mematikan loop tak terbatas secara paksa.
*   **Exit:** Tombol `◀ Back` di pojok kiri atas untuk kembali ke Workspace Editor secara anggun.

### 📦 4. Pip Package Manager Layer
*   **Real-time Log Stream:** Mencari dan menginstal package Python via pip, mengalirkan logs unduhan dan ekstraksi secara langsung ke terminal log hitam di layar pengaturan.

### 🔍 5. Command Palette & Quick Open (Fase 2)
*   **Topbar Touch Access:** Tombol ikon pencarian `🔍` di Topbar membuka Command Palette kustom tanpa memerlukan shortcut keyboard fisik.
*   **Unified Search Dialog:** Ketik nama file untuk membuka file secara instan (*Quick Open*) atau gunakan prefix `>` untuk mencari dan memicu aksi editor secara instan.

### ⚡ 6. Real-time Syntax Diagnostic (Fase 2)
*   **Non-Intrusive Warning Banner:** Menganalisis keseimbangan tanda kurung `()`, `[]`, `{}` dan string secara asinkron (debounce 800ms). Menampilkan banner peringatan merah soft yang informatif di bawah tab bar jika terjadi kesalahan ketik.

### 🔧 7. 5 Plugin Transformasi Kode (Fase 2)
*   **Beautifier Pro:** Mengatur spasi operator secara rapi dengan prioritas `longest-first` (menjamin anotasi tipe data `->` tidak rusak menjadi `- >`).
*   **Optimize Auto-Imports:** Otomatis mendeteksi dan mengimpor modul standar Python (`os`, `sys`, `math`, `json`, `time`, `random`, `datetime`) saat digunakan.
*   **Duplicate Line:** Menduplikasi baris aktif di editor secara instan.
*   **Comment Toggle:** Memasang/melepas simbol komentar `#` pada baris aktif.
*   **Clear Drafts:** Mengosongkan file draf sementara.

---

## 🎯 Target Pengembangan Masa Depan (Future Roadmap)

*   [ ] **Visual Problems Panel:** Menampilkan seluruh daftar error sintaksis file di dalam panel lembar bawah (Problems view) yang visual dan terorganisir.
*   [ ] **Matplotlib Inline Image Support:** Mengabadikan grafik keluaran Matplotlib (`plt.savefig`) dan menampilkannya sebagai overlay/inline image yang *expandable* di dalam layar terminal.
*   [ ] **CRT Scanlines Mode Toggle:** Opsi efek monitor tabung retro (CRT scanlines) di dalam pengaturan.
*   [ ] **Encrypted Keystore:** Integrasi AndroidX Security Crypto untuk menyimpan data kredensial dan provider kunci API AI/Oracle tingkat lanjut secara terenkripsi penuh.
*   [ ] **Privacy Toggle:** Pengaturan privasi ketat untuk mematikan draf lokal teks polos demi keamanan data sensitif pengguna.
*   [ ] **Alpine Linux proot terminal:** Dukungan shell terminal Linux penuh di dalam panel bawah untuk mendukung perintah `apk add`, `git`, dan kompilasi modul binary.

---

**Motto kami: GANBARUUUU! 🚀🔥**
