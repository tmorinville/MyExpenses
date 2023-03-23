package org.totschnig.myexpenses.util.locale

import android.content.Context
import org.totschnig.myexpenses.MyApplication.DEFAULT_LANGUAGE
import java.util.*

interface UserLocaleProvider {
    fun getUserPreferredLocale(): Locale
    fun wrapContext(context: Context): Context
    fun getLocalCurrency(context: Context): Currency
    var systemLocale: Locale

    companion object {
        fun resolveLocale(language: String, systemLocale: Locale): Locale = when {
            language == DEFAULT_LANGUAGE -> {
                systemLocale
            }
            language.contains("-") -> {
                val parts = language.split("-")
                Locale(parts[0], parts[1])
            }
            else -> {
                Locale(language)
            }
        }
    }
}