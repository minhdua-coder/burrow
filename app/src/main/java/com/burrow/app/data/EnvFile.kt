package com.burrow.app.data

fun envKeyFor(key: String): String {
    val cleaned = key.uppercase().map { c -> if (c.isLetterOrDigit()) c else '_' }.joinToString("")
    val collapsed = Regex("_+").replace(cleaned, "_").trim('_')
    return collapsed.ifEmpty { "VAR" }
}

private fun quoteEnvValue(value: String): String {
    val escaped = value.replace("\\", "\\\\").replace("\"", "\\\"")
    return "\"$escaped\""
}

fun buildEnvFile(variables: List<Variable>): String {
    if (variables.isEmpty()) return "# No variables to export\n"
    val seen = mutableSetOf<String>()
    val sb = StringBuilder("# Exported from Burrow\n")
    variables.forEach { v ->
        val base = envKeyFor(v.key)
        var name = base
        var suffix = 1
        while (!seen.add(name)) {
            suffix++
            name = "${base}_$suffix"
        }
        sb.append(name).append('=').append(quoteEnvValue(v.value)).append('\n')
    }
    return sb.toString()
}

data class ParsedEnvEntry(val key: String, val value: String)

fun parseEnvFile(content: String): List<ParsedEnvEntry> {
    val results = mutableListOf<ParsedEnvEntry>()
    content.lineSequence().forEach { rawLine ->
        val line = rawLine.trim()
        if (line.isEmpty() || line.startsWith("#")) return@forEach
        val withoutExport = if (line.startsWith("export ")) line.removePrefix("export ").trim() else line
        val eqIndex = withoutExport.indexOf('=')
        if (eqIndex <= 0) return@forEach
        val key = withoutExport.substring(0, eqIndex).trim()
        var value = withoutExport.substring(eqIndex + 1).trim()
        if (value.length >= 2 && (value.first() == '"' || value.first() == '\'') && value.last() == value.first()) {
            val quoteChar = value.first()
            value = value.substring(1, value.length - 1)
            if (quoteChar == '"') {
                value = value.replace("\\\"", "\"").replace("\\\\", "\\")
            }
        }
        if (key.isNotEmpty()) results.add(ParsedEnvEntry(key, value))
    }
    return results
}
