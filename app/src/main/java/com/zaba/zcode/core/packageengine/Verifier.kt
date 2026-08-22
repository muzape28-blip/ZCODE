package com.zaba.zcode.core.packageengine

import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.zip.ZipInputStream

/**
 * Verifier — verifikasi & ekstraksi wheel aman (SPEC-001 §8, §20 Security Model).
 *
 * - SHA-256: diverifikasi SEBELUM aktivasi (download → verify → extract → smoke → activate)
 * - Path traversal protection saat ekstraksi:
 *     normalize path → reject ../ → reject absolute → extract inside staging
 * - Validasi metadata: *.dist-info/METADATA + RECORD harus ada
 *
 * Wheel dianggap UNTRUSTED INPUT (dari PyPI/Chaquopy) — semua path dicek.
 */
object Verifier {

    const val MAX_WHEEL_BYTES = 512L * 1024 * 1024 // guard 512MB (storage abuse)

    data class VerifyResult(val ok: Boolean, val error: String?)
    data class WheelMeta(val name: String, val version: String)

    /** Hitung SHA-256 file (streaming — aman untuk wheel besar). */
    fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(64 * 1024)
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                digest.update(buf, 0, n)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun verifySha256(file: File, expectedHex: String): VerifyResult {
        if (expectedHex.isBlank()) return VerifyResult(true, null) // tak ada hash upstream
        val actual = sha256(file)
        return if (actual.equals(expectedHex, ignoreCase = true)) {
            VerifyResult(true, null)
        } else {
            VerifyResult(false, "SHA-256 mismatch: expected $expectedHex, got $actual")
        }
    }

    /**
     * Ekstrak wheel ke destDir (staging) dengan proteksi path traversal.
     * onProgress: jumlah file ter-extract (untuk installation console).
     */
    fun extractWheel(wheel: File, destDir: File, onProgress: ((Int) -> Unit)? = null): VerifyResult {
        if (wheel.length() > MAX_WHEEL_BYTES) {
            return VerifyResult(false, "Wheel terlalu besar (>512MB)")
        }
        try {
            destDir.mkdirs()
            val destCanonical = destDir.canonicalPath
            var count = 0
            wheel.inputStream().use { input ->
                ZipInputStream(input).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        val rawName = entry.name.replace('\\', '/')
                        if (!isSafeEntryName(rawName)) {
                            return VerifyResult(false, "Entry wheel tidak aman: '$rawName' (path traversal/absolute)")
                        }
                        val target = File(destDir, rawName)
                        val targetCanonical = target.canonicalPath
                        if (!targetCanonical.startsWith(destCanonical + File.separator)) {
                            return VerifyResult(false, "Entry wheel keluar dari staging: '$rawName'")
                        }
                        if (entry.isDirectory) {
                            target.mkdirs()
                        } else {
                            target.parentFile?.mkdirs()
                            FileOutputStream(target).use { out ->
                                val buf = ByteArray(64 * 1024)
                                while (true) {
                                    val n = zip.read(buf)
                                    if (n < 0) break
                                    out.write(buf, 0, n)
                                }
                            }
                        }
                        zip.closeEntry()
                        count++
                        if (count % 25 == 0) onProgress?.invoke(count)
                        entry = zip.nextEntry
                    }
                }
            }
            onProgress?.invoke(count)
            return VerifyResult(true, null)
        } catch (e: Exception) {
            return VerifyResult(false, "Ekstraksi wheel gagal: ${e.message}")
        }
    }

    private fun isSafeEntryName(name: String): Boolean {
        if (name.startsWith("/")) return false
        if (Regex("^[A-Za-z]:").containsMatchIn(name)) return false
        val parts = name.split("/")
        return parts.none { it == ".." || it == "." }
    }

    /** Validasi metadata wheel: METADATA + RECORD wajib ada; baca Name/Version. */
    fun validateWheelMeta(extractedDir: File): Pair<VerifyResult, WheelMeta?> {
        val distInfos = extractedDir.listFiles()?.filter {
            it.isDirectory && it.name.endsWith(".dist-info")
        } ?: emptyList()
        if (distInfos.isEmpty()) {
            return VerifyResult(false, "Wheel tidak mengandung *.dist-info (metadata invalid)") to null
        }
        val distInfo = distInfos.first()
        val metadata = File(distInfo, "METADATA")
        val record = File(distInfo, "RECORD")
        if (!metadata.exists()) {
            return VerifyResult(false, "METADATA tidak ditemukan di dist-info") to null
        }
        if (!record.exists()) {
            return VerifyResult(false, "RECORD tidak ditemukan (wheel invalid per PEP 427)") to null
        }
        val metaText = metadata.readText(Charsets.UTF_8)
        val name = Regex("^Name:\\s*(.+)$", RegexOption.MULTILINE).find(metaText)?.groupValues?.get(1)?.trim()
        val version = Regex("^Version:\\s*(.+)$", RegexOption.MULTILINE).find(metaText)?.groupValues?.get(1)?.trim()
        if (name.isNullOrBlank() || version.isNullOrBlank()) {
            return VerifyResult(false, "METADATA tidak memiliki Name/Version yang valid") to null
        }
        val recordRes = verifyRecord(extractedDir)
        if (!recordRes.ok) {
            return recordRes to null
        }
        return VerifyResult(true, null) to WheelMeta(name, version)
    }

    /**
     * Verifikasi integritas RECORD (PEP 427):
     * - Setiap berkas di extractedDir wajib terdaftar di RECORD.
     * - Exceptions per PEP 427: <dist-info>/RECORD dan signature file <dist-info>/RECORD.jws, <dist-info>/RECORD.p7s.
     * - Tolak berkas tak terdaftar, path traversal, serta mismatch hash/size.
     */
    fun verifyRecord(extractedDir: File): VerifyResult {
        val distInfos = extractedDir.listFiles()?.filter {
            it.isDirectory && it.name.endsWith(".dist-info")
        } ?: emptyList()
        if (distInfos.isEmpty()) {
            return VerifyResult(false, "Wheel tidak mengandung *.dist-info")
        }
        val distInfo = distInfos.first()
        val recordFile = File(distInfo, "RECORD")
        if (!recordFile.exists()) {
            return VerifyResult(false, "RECORD tidak ditemukan")
        }

        val recordEntries = mutableMapOf<String, Pair<String?, Long?>>()
        try {
            recordFile.useLines { lines ->
                for (line in lines) {
                    val trimmed = line.trim()
                    if (trimmed.isBlank()) continue
                    val parts = trimmed.split(",")
                    val rawRelPath = parts[0].trim().replace('\\', '/')
                    if (!isSafeEntryName(rawRelPath)) {
                        return VerifyResult(false, "Entry RECORD tidak aman: '$rawRelPath'")
                    }
                    val hashStr = if (parts.size > 1 && parts[1].isNotBlank()) parts[1].trim() else null
                    val sizeVal = if (parts.size > 2 && parts[2].isNotBlank()) parts[2].trim().toLongOrNull() else null
                    recordEntries[rawRelPath] = Pair(hashStr, sizeVal)
                }
            }
        } catch (e: Exception) {
            return VerifyResult(false, "Gagal membaca RECORD: ${e.message}")
        }

        val distInfoRelDir = distInfo.name
        val allowedExceptions = setOf(
            "$distInfoRelDir/RECORD",
            "$distInfoRelDir/RECORD.jws",
            "$distInfoRelDir/RECORD.p7s"
        )

        val baseCanonical = extractedDir.canonicalPath
        val allFiles = extractedDir.walkTopDown().filter { it.isFile }.toList()

        for (file in allFiles) {
            val fileCanonical = file.canonicalPath
            if (!fileCanonical.startsWith(baseCanonical + File.separator)) {
                return VerifyResult(false, "Berkas di luar extractedDir: ${file.name}")
            }
            val relPath = fileCanonical.substring(baseCanonical.length + 1).replace('\\', '/')

            if (relPath in allowedExceptions) {
                continue
            }

            val entry = recordEntries[relPath]
                ?: return VerifyResult(false, "Berkas tak terdaftar di RECORD: '$relPath'")

            val (expectedHash, expectedSize) = entry
            if (expectedSize != null && file.length() != expectedSize) {
                return VerifyResult(false, "Ukuran berkas tidak cocok untuk '$relPath': expected $expectedSize, got ${file.length()}")
            }

            if (expectedHash != null && expectedHash.startsWith("sha256=")) {
                val expectedBase64 = expectedHash.removePrefix("sha256=")
                val actualBase64 = sha256Base64Url(file)
                if (actualBase64 != expectedBase64) {
                    return VerifyResult(false, "Hash SHA-256 tidak cocok untuk '$relPath'")
                }
            }
        }

        return VerifyResult(true, null)
    }

    private fun sha256Base64Url(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(64 * 1024)
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                digest.update(buf, 0, n)
            }
        }
        val bytes = digest.digest()
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}
