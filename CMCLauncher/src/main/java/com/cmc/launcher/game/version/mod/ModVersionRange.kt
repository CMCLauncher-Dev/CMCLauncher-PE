package com.cmc.launcher.game.version.mod

object ModVersionRange {

    fun matches(version: String?, requirement: String?): Boolean {
        if (requirement.isNullOrBlank() || requirement == "*") return true
        if (version.isNullOrBlank()) return false
        val alternatives = requirement.split("||")
        return alternatives.any { alternative ->
            matchesAll(version, alternative.trim())
        }
    }

    private fun matchesAll(version: String, requirement: String): Boolean {
        if (requirement.isBlank()) return true
        bracketRange(requirement)?.let { (lower, lowerInclusive, upper, upperInclusive) ->
            if (lower != null) {
                val compared = compare(version, lower)
                if (compared < 0 || (compared == 0 && !lowerInclusive)) return false
            }
            if (upper != null) {
                val compared = compare(version, upper)
                if (compared > 0 || (compared == 0 && !upperInclusive)) return false
            }
            return true
        }
        val tokens = requirement.replace(",", " ").split(Regex("\\s+")).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return true
        return tokens.all { token -> matchToken(version, token) }
    }

    private fun bracketRange(requirement: String): Bracket? {
        if (requirement.length < 2) return null
        val first = requirement.first()
        val last = requirement.last()
        if (first != '[' && first != '(') return null
        if (last != ']' && last != ')') return null
        val body = requirement.substring(1, requirement.length - 1)
        val parts = body.split(",")
        val lower = parts.getOrNull(0)?.trim()?.takeIf { it.isNotEmpty() }
        val upper = parts.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() }
        return Bracket(lower, first == '[', upper, last == ']')
    }

    private data class Bracket(
        val lower: String?,
        val lowerInclusive: Boolean,
        val upper: String?,
        val upperInclusive: Boolean
    )

    private fun matchToken(version: String, token: String): Boolean {
        val trimmed = token.trim()
        if (trimmed.isEmpty()) return true
        return when {
            trimmed == "*" -> true
            trimmed.startsWith(">=") -> compare(version, trimmed.drop(2)) >= 0
            trimmed.startsWith("<=") -> compare(version, trimmed.drop(2)) <= 0
            trimmed.startsWith("!=") -> compare(version, trimmed.drop(2)) != 0
            trimmed.startsWith(">") -> compare(version, trimmed.drop(1)) > 0
            trimmed.startsWith("<") -> compare(version, trimmed.drop(1)) < 0
            trimmed.startsWith("=") -> compare(version, trimmed.drop(1)) == 0
            trimmed.startsWith("^") -> caret(version, trimmed.drop(1))
            trimmed.startsWith("~") -> tilde(version, trimmed.drop(1))
            trimmed.endsWith(".*") -> version.startsWith(trimmed.dropLast(1))
            trimmed.endsWith(".x") || trimmed.endsWith(".X") -> {
                val prefix = trimmed.dropLast(2).split(".").mapNotNull { it.toIntOrNull() }
                val target = numbers(version)
                prefix.indices.all { index -> target.getOrElse(index) { 0 } == prefix[index] }
            }
            else -> compare(version, trimmed) == 0
        }
    }

    private fun caret(version: String, base: String): Boolean {
        val numbers = numbers(base)
        val upper = when {
            numbers.isEmpty() -> return true
            numbers[0] > 0 -> "${numbers[0] + 1}.0.0"
            numbers.size > 1 && numbers[1] > 0 -> "0.${numbers[1] + 1}.0"
            numbers.size > 2 -> "0.0.${numbers[2] + 1}"
            else -> "1.0.0"
        }
        return compare(version, base) >= 0 && compare(version, upper) < 0
    }

    private fun tilde(version: String, base: String): Boolean {
        val numbers = numbers(base)
        if (numbers.isEmpty()) return true
        val upper = if (numbers.size > 1) "${numbers[0]}.${numbers[1] + 1}.0" else "${numbers[0] + 1}.0.0"
        return compare(version, base) >= 0 && compare(version, upper) < 0
    }

    fun compare(left: String, right: String): Int {
        val a = numbers(left)
        val b = numbers(right)
        val size = maxOf(a.size, b.size)
        for (index in 0 until size) {
            val x = a.getOrElse(index) { 0 }
            val y = b.getOrElse(index) { 0 }
            if (x != y) return x.compareTo(y)
        }
        return 0
    }

    private fun numbers(version: String): List<Int> {
        return version.trim()
            .removePrefix("v")
            .split(Regex("[^0-9]+"))
            .filter { it.isNotEmpty() }
            .mapNotNull { it.toIntOrNull() }
    }
}