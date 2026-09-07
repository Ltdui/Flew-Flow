package com.example.data.model

enum class AppThemeMode {
    SYSTEM, LIGHT, DARK
}

data class SupportedLanguage(
    val code: String,
    val displayName: String,
    val nativeName: String
) {
    companion object {
        val SUPPORTED_LANGUAGES = listOf(
            SupportedLanguage("auto", "Auto Detection", "স্বয়ংক্রিয় / स्वचालित"),
            SupportedLanguage("en", "English", "English"),
            SupportedLanguage("bn", "Bengali", "বাংলা"),
            SupportedLanguage("hi", "Hindi", "हिन्दी"),
            SupportedLanguage("es", "Spanish", "Español"),
            SupportedLanguage("fr", "French", "Français"),
            SupportedLanguage("de", "German", "Deutsch"),
            SupportedLanguage("ja", "Japanese", "日本語"),
            SupportedLanguage("ar", "Arabic", "العربية"),
            SupportedLanguage("pt", "Portuguese", "Português"),
            SupportedLanguage("zh", "Chinese", "中文")
        )
    }
}

data class TranscriptSettings(
    val languageCode: String = "auto",
    val isAutoLanguageDetection: Boolean = true,
    val isSmartTranscription: Boolean = true,
    val isAutoPunctuation: Boolean = true,
    val isRemoveFillerWords: Boolean = true,
    val customVocabulary: List<String> = listOf(),
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val customApiKey: String = ""
)
