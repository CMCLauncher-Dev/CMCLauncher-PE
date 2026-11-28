package com.cmc.launcher.setting.enums

import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.cmc.launcher.R

enum class AppLanguage(
    val tag: String,
    @param:StringRes val textRes: Int,
    val availableInUi: Boolean = false
) {

    FOLLOW_SYSTEM("", R.string.generic_follow_system),
    ENGLISH("en", R.string.language_english, true),
    SIMPLIFIED_CHINESE("zh-CN", R.string.language_simplified_chinese, true),
    TRADITIONAL_CHINESE("zh-TW", R.string.language_traditional_chinese, true);

    companion object {
        val available: List<AppLanguage>
            get() = entries.filter { it.availableInUi || it == FOLLOW_SYSTEM }
    }
}

fun applyLanguage(language: AppLanguage) {
    val appLocale = if (language != AppLanguage.FOLLOW_SYSTEM) {
        LocaleListCompat.forLanguageTags(language.tag)
    } else {
        LocaleListCompat.getEmptyLocaleList()
    }
    AppCompatDelegate.setApplicationLocales(appLocale)
}
