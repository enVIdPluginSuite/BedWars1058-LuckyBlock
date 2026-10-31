package dev.etran.luckyblock.data

import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Changes single values in config.yml's text without touching anything else, so the comments that explain
 * the file survive (Bukkit's own save drops them on most versions). Works on the lines of the file.
 */
object ConfigEditor {
    private val KEY: Pattern = Pattern.compile("^(\\s*)([A-Za-z0-9_.-]+):(\\s*)(.*)$")

    /** Index of the line holding the key at the dotted path, or -1. */
    private fun find(lines: List<String>, path: String): Int {
        val indents = ArrayList<Int>()
        val keys = ArrayList<String>()
        for (i in lines.indices) {
            val trimmed = lines[i].trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("-")) continue
            val m = KEY.matcher(lines[i])
            if (!m.matches()) continue
            val indent = m.group(1).length
            while (indents.isNotEmpty() && indents[indents.size - 1] >= indent) {
                indents.removeAt(indents.size - 1)
                keys.removeAt(keys.size - 1)
            }
            indents.add(indent)
            keys.add(m.group(2))
            if (keys.joinToString(".") == path) return i
        }
        return -1
    }

    /** Index where a trailing "# comment" starts in the text after the colon, or -1. */
    private fun commentStart(rest: String): Int {
        var single = false
        var dbl = false
        for (i in rest.indices) {
            val c = rest[i]
            if (c == '\'' && !dbl) {
                single = !single
            } else if (c == '"' && !single) {
                dbl = !dbl
            } else if (c == '#' && !single && !dbl && (i == 0 || rest[i - 1] == ' ')) {
                return i
            }
        }
        return -1
    }

    /** Text after the colon without its comment, trimmed. */
    private fun valueOf(rest: String): String {
        val comment = commentStart(rest)
        return (if (comment < 0) rest else rest.substring(0, comment)).trim()
    }

    /** The part after the colon with the value swapped for a new one; the comment and spacing stay. */
    private fun withValue(m: Matcher, newValue: String): String {
        val rest = m.group(4)
        val comment = commentStart(rest)
        var tail = ""
        if (comment >= 0) {
            val beforeComment = rest.substring(0, comment)
            val spaces = beforeComment.substring(beforeComment.replace(Regex("\\s+$"), "").length)
            tail = (if (spaces.isEmpty()) " " else spaces) + rest.substring(comment)
        }
        val gap = if (m.group(3).isEmpty()) " " else m.group(3)
        return m.group(1) + m.group(2) + ":" + gap + newValue + tail
    }

    private fun keyLine(lines: List<String>, at: Int): Matcher? {
        val m = KEY.matcher(lines[at])
        return if (m.matches()) m else null
    }

    private fun indentOf(line: String): Int = line.length - line.replace(Regex("^\\s+"), "").length

    /** Sets a plain value (number, true/false). Returns false if the key is not there or is not a plain value. */
    @JvmStatic
    fun setScalar(lines: MutableList<String>, path: String, value: String): Boolean {
        val at = find(lines, path)
        if (at < 0) return false
        val m = keyLine(lines, at) ?: return false
        val current = valueOf(m.group(4))
        if (current.isEmpty() || current.startsWith("{") || current.startsWith("[")) return false
        lines[at] = withValue(m, value)
        return true
    }

    /** Sets one entry of a one-line map such as "wool: { team: 30, diamond: 10 }". */
    @JvmStatic
    fun setFlowEntry(lines: MutableList<String>, mapPath: String, entry: String, value: String): Boolean {
        val at = find(lines, mapPath)
        if (at < 0) return false
        val m = keyLine(lines, at) ?: return false
        val current = valueOf(m.group(4))
        if (!current.startsWith("{") || !current.endsWith("}")) return false
        val inside = current.substring(1, current.length - 1)
        val e = Pattern.compile("(^|[\\s,])" + Pattern.quote(entry) + "\\s*:\\s*-?\\d+").matcher(inside)
        val replacement: String = if (e.find()) {
            inside.substring(0, e.start()) + e.group(1) + entry + ": " + value + inside.substring(e.end())
        } else {
            " " + (inside.trim() + ", " + entry + ": " + value + " ").trim() + " "
        }
        lines[at] = withValue(m, "{$replacement}")
        return true
    }

    /** Adds "child: value" as the last entry under an existing block-style map. Returns false if the parent is not there. */
    @JvmStatic
    fun insertChild(lines: MutableList<String>, parentPath: String, child: String, value: String): Boolean {
        val at = find(lines, parentPath)
        if (at < 0) return false
        val m = keyLine(lines, at) ?: return false
        if (valueOf(m.group(4)).isNotEmpty()) return false
        var childIndent = m.group(1).length + 2
        var last = at
        for (k in at + 1 until lines.size) {
            val line = lines[k]
            val t = line.trim()
            if (t.isEmpty() || t.startsWith("#")) continue
            val indent = indentOf(line)
            if (indent <= m.group(1).length) break
            childIndent = indent
            last = k
        }
        lines.add(last + 1, " ".repeat(childIndent) + child + ": " + value)
        return true
    }

    /** Replaces a list (block "- a" lines or "[a, b]") with the given items. Returns false if the key is not there. */
    @JvmStatic
    fun setList(lines: MutableList<String>, path: String, items: List<String>): Boolean {
        val at = find(lines, path)
        if (at < 0) return false
        val m = keyLine(lines, at) ?: return false
        val current = valueOf(m.group(4))
        if (current.startsWith("[")) {
            lines[at] = withValue(m, "[" + items.joinToString(", ") + "]")
            return true
        }
        if (current.isNotEmpty()) return false
        var itemIndent = m.group(1).length + 2
        var end = at + 1
        var k = at + 1
        var first = true
        while (k < lines.size) {
            val t = lines[k].trim()
            if (t.startsWith("- ") || t == "-") {
                if (first) {
                    itemIndent = indentOf(lines[k])
                    first = false
                }
                end = k + 1
                k++
            } else if (t.isEmpty() || t.startsWith("#")) {
                k++
            } else {
                break
            }
        }
        for (i in end - 1 downTo at + 1) lines.removeAt(i)
        var insertAt = at + 1
        val pad = " ".repeat(itemIndent)
        for (item in items) lines.add(insertAt++, "$pad- $item")
        return true
    }
}
