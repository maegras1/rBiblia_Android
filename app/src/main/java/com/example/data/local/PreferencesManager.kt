package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.DarkVariant
import com.example.data.model.TextFontFamily
import com.example.data.model.TextSize
import com.example.data.model.ThemeMode

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("rbiblia_prefs", Context.MODE_PRIVATE)

    var appLanguage: String
        get() = prefs.getString("app_language", "pl") ?: "pl"
        set(value) = prefs.edit().putString("app_language", value).apply()

    var selectedTranslation: String
        get() = prefs.getString("selected_translation", "pl_ubg") ?: "pl_ubg"
        set(value) = prefs.edit().putString("selected_translation", value).apply()

    var selectedBook: String
        get() = prefs.getString("selected_book", "gen") ?: "gen"
        set(value) = prefs.edit().putString("selected_book", value).apply()

    var selectedChapter: Int
        get() = prefs.getInt("selected_chapter", 1)
        set(value) = prefs.edit().putInt("selected_chapter", value).apply()

    var isParallelReading: Boolean
        get() = prefs.getBoolean("is_parallel_reading", false)
        set(value) = prefs.edit().putBoolean("is_parallel_reading", value).apply()

    var parallelTranslation: String
        get() = prefs.getString("parallel_translation", "pl_bt5") ?: "pl_bt5"
        set(value) = prefs.edit().putString("parallel_translation", value).apply()

    var parallelLayoutColumns: Boolean
        get() = prefs.getBoolean("parallel_layout_columns", true)
        set(value) = prefs.edit().putBoolean("parallel_layout_columns", value).apply()

    var parallelShowDifferences: Boolean
        get() = prefs.getBoolean("parallel_show_differences", false)
        set(value) = prefs.edit().putBoolean("parallel_show_differences", value).apply()

    var textSize: TextSize
        get() {
            val name = prefs.getString("text_size", TextSize.MEDIUM.name) ?: TextSize.MEDIUM.name
            return try { TextSize.valueOf(name) } catch (e: Exception) { TextSize.MEDIUM }
        }
        set(value) = prefs.edit().putString("text_size", value.name).apply()

    var fontFamily: TextFontFamily
        get() {
            val name = prefs.getString("font_family", TextFontFamily.SERIF.name) ?: TextFontFamily.SERIF.name
            return try { TextFontFamily.valueOf(name) } catch (e: Exception) { TextFontFamily.SERIF }
        }
        set(value) = prefs.edit().putString("font_family", value.name).apply()

    var themeMode: ThemeMode
        get() {
            val name = prefs.getString("theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
            return try { ThemeMode.valueOf(name) } catch (e: Exception) { ThemeMode.SYSTEM }
        }
        set(value) = prefs.edit().putString("theme_mode", value.name).apply()

    var darkVariant: DarkVariant
        get() {
            val name = prefs.getString("dark_variant", DarkVariant.GOLD.name) ?: DarkVariant.GOLD.name
            return try { DarkVariant.valueOf(name) } catch (e: Exception) { DarkVariant.GOLD }
        }
        set(value) = prefs.edit().putString("dark_variant", value.name).apply()

    var zenMode: Boolean
        get() = prefs.getBoolean("zen_mode", false)
        set(value) = prefs.edit().putBoolean("zen_mode", value).apply()

    var continuousText: Boolean
        get() = prefs.getBoolean("continuous_text", false)
        set(value) = prefs.edit().putBoolean("continuous_text", value).apply()

    var hideVerseNumbers: Boolean
        get() = prefs.getBoolean("hide_verse_numbers", false)
        set(value) = prefs.edit().putBoolean("hide_verse_numbers", value).apply()

    var comparisonTranslations: Set<String>
        get() = prefs.getStringSet("comparison_translations", setOf("en_kjv", "pl_ubg", "de_lb")) ?: emptySet()
        set(value) = prefs.edit().putStringSet("comparison_translations", value).apply()
}
