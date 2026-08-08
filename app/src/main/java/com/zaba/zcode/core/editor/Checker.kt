package com.zaba.zcode.core.editor

/**
 * Checker — offline lightweight Python syntax validator (Fase 2).
 * Highly efficient single-pass string/comment scanner.
 */
object Checker {

    fun stripCommentsAndStrings(code: String): String {
        val sb = StringBuilder()
        var i = 0
        val len = code.length

        while (i < len) {
            val char = code[i]

            if (i + 2 < len && (code.substring(i, i + 3) == "\"\"\"" || code.substring(i, i + 3) == "'''")) {
                val quoteType = code.substring(i, i + 3)
                sb.append("   ")
                i += 3
                while (i < len) {
                    if (i + 2 < len && code.substring(i, i + 3) == quoteType) {
                        sb.append("   ")
                        i += 3
                        break
                    }
                    val c = code[i]
                    if (c == '\n') sb.append('\n') else sb.append(' ')
                    i++
                }
                continue
            }

            if (char == '"' || char == '\'') {
                val quote = char
                sb.append(' ')
                i++
                while (i < len) {
                    val c = code[i]
                    if (c == '\\') {
                        sb.append("  ")
                        i += 2
                        continue
                    }
                    if (c == quote) {
                        sb.append(' ')
                        i++
                        break
                    }
                    if (c == '\n') sb.append('\n') else sb.append(' ')
                    i++
                }
                continue
            }

            if (char == '#') {
                while (i < len && code[i] != '\n') {
                    sb.append(' ')
                    i++
                }
                continue
            }

            sb.append(char)
            i++
        }
        return sb.toString()
    }

    fun checkBrackets(code: String): String? {
        val stripped = stripCommentsAndStrings(code)
        val stack = mutableListOf<Pair<Char, Int>>()

        var lineNum = 1
        for (i in stripped.indices) {
            val char = stripped[i]
            if (char == '\n') {
                lineNum++
                continue
            }

            if (char == '(' || char == '[' || char == '{') {
                stack.add(char to lineNum)
            } else if (char == ')' || char == ']' || char == '}') {
                if (stack.isEmpty()) {
                    return "Unexpected closed bracket '$char' on line $lineNum"
                }
                val (open, openLine) = stack.removeAt(stack.size - 1)
                if ((char == ')' && open != '(') ||
                    (char == ']' && open != '[') ||
                    (char == '}' && open != '{')) {
                    return "Mismatched bracket on line $lineNum: '$char' does not match '$open' on line $openLine"
                }
            }
        }

        if (stack.isNotEmpty()) {
            val (open, openLine) = stack.last()
            return "Unbalanced brackets: '$open' on line $openLine has no matching closed bracket"
        }

        return null
    }

    fun checkSyntax(code: String): String? {
        return checkBrackets(code)
    }
}
