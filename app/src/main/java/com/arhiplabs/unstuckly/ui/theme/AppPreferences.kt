package com.arhiplabs.unstuckly.ui.theme

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class AppThemeMode(val title: String) {
    SYSTEM("System Default"),
    LIGHT("Light"),
    DARK("Dark")
}

enum class AppLanguage(val code: String, val displayName: String, val flag: String) {
    SYSTEM("system", "System Default", "🌐"),
    EN("en", "English", "🇬🇧"),
    DE("de", "Deutsch", "🇩🇪"),
    UA("uk", "Українська", "🇺🇦"),
    ESP("es", "Español", "🇪🇸"),
    POR("pt", "Português", "🇵🇹"),
    RU("ru", "Русский", "🇷🇺");

    companion object {
        fun fromCode(code: String): AppLanguage {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: SYSTEM
        }
    }
}

class AppPreferences(private val context: Context) {
    private val prefs = context.getSharedPreferences("unstuckly_prefs", Context.MODE_PRIVATE)
    private val systemDefaultLocale: Locale = Locale.getDefault()

    private val _themeMode = MutableStateFlow(
        AppThemeMode.valueOf(prefs.getString("theme_mode", AppThemeMode.SYSTEM.name) ?: AppThemeMode.SYSTEM.name)
    )
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    private val _language = MutableStateFlow(
        AppLanguage.fromCode(prefs.getString("app_language", AppLanguage.SYSTEM.code) ?: AppLanguage.SYSTEM.code)
    )
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    fun setThemeMode(mode: AppThemeMode) {
        prefs.edit().putString("theme_mode", mode.name).apply()
        _themeMode.value = mode
    }

    fun setLanguage(lang: AppLanguage) {
        prefs.edit().putString("app_language", lang.code).apply()
        _language.value = lang

        val targetLocale = if (lang == AppLanguage.SYSTEM) systemDefaultLocale else Locale.forLanguageTag(lang.code)
        Locale.setDefault(targetLocale)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                val localeManager = context.getSystemService(LocaleManager::class.java)
                val localeList = if (lang == AppLanguage.SYSTEM) {
                    LocaleList.getEmptyLocaleList()
                } else {
                    LocaleList.forLanguageTags(lang.code)
                }
                localeManager?.applicationLocales = localeList
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getLocalizedContext(baseContext: Context): Context {
        val currentLang = _language.value
        val locale = if (currentLang == AppLanguage.SYSTEM) {
            systemDefaultLocale
        } else {
            Locale.forLanguageTag(currentLang.code)
        }
        Locale.setDefault(locale)

        val config = Configuration(baseContext.resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(LocaleList(locale))
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
        }
        config.setLayoutDirection(locale)

        return baseContext.createConfigurationContext(config)
    }

    companion object {
        @Volatile
        private var instance: AppPreferences? = null

        fun getInstance(context: Context): AppPreferences {
            return instance ?: synchronized(this) {
                instance ?: AppPreferences(context.applicationContext).also { instance = it }
            }
        }
    }
}

val LocalAppPreferences = compositionLocalOf<AppPreferences> {
    error("LocalAppPreferences not provided")
}
