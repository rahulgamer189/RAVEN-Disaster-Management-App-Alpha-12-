package com.raven.application

import android.content.Context

object OfflineAssistant {
    fun answer(context: Context, question: String): String {
        val q = question.lowercase()
        val resId = when {
            listOf("bleed", "bleeding", "blood").any(q::contains) -> R.string.assistant_bleeding
            listOf("fracture", "broken", "bone", "injury").any(q::contains) -> R.string.assistant_fracture
            listOf("shock", "pale", "faint", "cold").any(q::contains) -> R.string.assistant_shock
            listOf("water", "thirst", "dehydration").any(q::contains) -> R.string.assistant_water
            listOf("burn", "fire").any(q::contains) -> R.string.assistant_burn
            listOf("cpr", "not breathing", "unconscious").any(q::contains) -> R.string.assistant_cpr
            else -> R.string.assistant_default
        }
        return context.getString(resId)
    }
}
