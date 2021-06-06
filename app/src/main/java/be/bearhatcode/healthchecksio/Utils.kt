package be.bearhatcode.healthchecksio

import java.time.Duration

const val ARG_API_KEY = "API_KEY"


fun fmtDuration(seconds: Long): String {
    var duration = Duration.ofSeconds(seconds)
    return buildString {
        if (duration.toDays() > 1) {
            append(duration.toDays().toString() + " days")
        }
        if (duration.toDays() == 1L) {
            append(duration.toDays().toString() + " day")
        }
        duration = duration.minusDays(duration.toDays())
        if (duration.toHours() > 0) {
            if (!isEmpty()) {
                append(" ")
            }
            append(duration.toHours().toString() + "h")
        }
        duration = duration.minusHours(duration.toHours())
        if (duration.toMinutes() > 0) {
            if (!isEmpty()) {
                append(" ")
            }
            append(duration.toMinutes().toString() + "m")
        }
        duration = duration.minusMinutes(duration.toMinutes())
        if (duration.seconds > 0) {
            if (!isEmpty()) {
                append(" ")
            }
            append(duration.seconds.toString() + "s")
        }
    }
}

fun parseAPIKeys(value: String?): List<String> {
    if (value == null) return listOf()

    val parts = value.split("\n")
    val cleaned = parts.map { v -> v.trim() }.filter { v -> v.isNotEmpty() }
    return cleaned.filter { v -> v.matches(Regex("^[^ ]+$")) }
}