package com.zaba.zcode.core.plugins

import com.zaba.zcode.core.editor.Checker

object PluginHost {

    fun beautify(code: String): String {
        val lines = code.split("\n")
        val beautifiedLines = lines.map { line ->
            if (line.trim().startsWith("#") || line.trim().isEmpty()) {
                return@map line
            }

            var temp = line
                .replace("->", " __ARROW__ ")
                .replace("//=", " __FLOORDIV_EQ__ ")
                .replace("==", " __EQ__ ")
                .replace("!=", " __NE__ ")
                .replace("+=", " __ADD_EQ__ ")
                .replace("-=", " __SUB_EQ__ ")
                .replace("*=", " __MUL_EQ__ ")
                .replace("/=", " __DIV_EQ__ ")

            val singleOps = listOf("+", "*", "/", "<", ">", "=")
            singleOps.forEach { op ->
                val escapedOp = java.util.regex.Pattern.quote(op)
                temp = temp.replace(Regex("(?<!_)$escapedOp(?!_)"), " $op ")
            }
            temp = temp.replace(Regex("(?<!_)-(?!_)"), " - ")

            temp = temp
                .replace(Regex("\\s*__ARROW__\\s*"), " -> ")
                .replace(Regex("\\s*__FLOORDIV_EQ__\\s*"), " //= ")
                .replace(Regex("\\s*__EQ__\\s*"), " == ")
                .replace(Regex("\\s*__NE__\\s*"), " != ")
                .replace(Regex("\\s*__ADD_EQ__\\s*"), " += ")
                .replace(Regex("\\s*__SUB_EQ__\\s*"), " -= ")
                .replace(Regex("\\s*__MUL_EQ__\\s*"), " *= ")
                .replace(Regex("\\s*__DIV_EQ__\\s*"), " /= ")

            val leadingSpaces = line.takeWhile { it == ' ' || it == '\t' }
            val remainder = temp.trim()
            val normalizedRemainder = remainder.replace(Regex(" +"), " ")

            if (line.trim().isNotEmpty()) {
                leadingSpaces + normalizedRemainder
            } else {
                line
            }
        }
        return beautifiedLines.joinToString("\n")
    }

    fun optimizeImports(code: String): String {
        val stdLibs = listOf("os", "sys", "math", "json", "time", "random", "datetime")
        val missingImports = mutableListOf<String>()

        val stripped = Checker.stripCommentsAndStrings(code)

        stdLibs.forEach { lib ->
            if (stripped.contains("$lib.") && !code.contains(Regex("\\bimport\\s+$lib\\b")) && !code.contains(Regex("\\bfrom\\s+$lib\\s+import\\b"))) {
                missingImports.add("import $lib")
            }
        }

        if (missingImports.isEmpty()) return code

        val importsBlock = missingImports.joinToString("\n") + "\n"
        return importsBlock + code
    }
}
