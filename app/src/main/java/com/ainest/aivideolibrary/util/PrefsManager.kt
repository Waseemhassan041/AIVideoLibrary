package com.ainest.aivideolibrary.util

import android.content.Context

class PrefsManager(context: Context) {
    private val prefs = context.getSharedPreferences("ai_video_library_prefs", Context.MODE_PRIVATE)

    var hasSkippedSignIn: Boolean
        get() = prefs.getBoolean(KEY_SKIPPED_SIGNIN, false)
        set(value) = prefs.edit().putBoolean(KEY_SKIPPED_SIGNIN, value).apply()

    /** Hours between automatic background syncs: 12, 24, 72, or 168 (7 days). 0 = off. */
    var autoSyncIntervalHours: Int
        get() = prefs.getInt(KEY_AUTO_SYNC_HOURS, 24)
        set(value) = prefs.edit().putInt(KEY_AUTO_SYNC_HOURS, value).apply()

    var lastSyncTimestamp: Long
        get() = prefs.getLong(KEY_LAST_SYNC, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_SYNC, value).apply()

    var darkModeOverride: Boolean?
        get() = if (prefs.contains(KEY_DARK)) prefs.getBoolean(KEY_DARK, false) else null
        set(value) {
            if (value == null) prefs.edit().remove(KEY_DARK).apply()
            else prefs.edit().putBoolean(KEY_DARK, value).apply()
        }

    // --- App lock ---
    var appLockEnabled: Boolean
        get() = prefs.getBoolean(KEY_LOCK_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_LOCK_ENABLED, value).apply()

    /** Stored as a SHA-256 hash, never the raw PIN. */
    var pinHash: String?
        get() = prefs.getString(KEY_PIN_HASH, null)
        set(value) = prefs.edit().putString(KEY_PIN_HASH, value).apply()

    var useBiometricForLock: Boolean
        get() = prefs.getBoolean(KEY_LOCK_BIOMETRIC, true)
        set(value) = prefs.edit().putBoolean(KEY_LOCK_BIOMETRIC, value).apply()

    // --- Encrypted backup ---
    var backupEncryptionEnabled: Boolean
        get() = prefs.getBoolean(KEY_BACKUP_ENCRYPT, false)
        set(value) = prefs.edit().putBoolean(KEY_BACKUP_ENCRYPT, value).apply()

    // --- AI auto-fill ---
    var aiProvider: String
        get() = prefs.getString(KEY_AI_PROVIDER, "GEMINI") ?: "GEMINI"
        set(value) = prefs.edit().putString(KEY_AI_PROVIDER, value).apply()

    var geminiApiKey: String?
        get() = prefs.getString(KEY_GEMINI_KEY, null)
        set(value) = prefs.edit().putString(KEY_GEMINI_KEY, value).apply()

    var claudeApiKey: String?
        get() = prefs.getString(KEY_CLAUDE_KEY, null)
        set(value) = prefs.edit().putString(KEY_CLAUDE_KEY, value).apply()

    var aiPromptTemplate: String?
        get() = prefs.getString(KEY_AI_TEMPLATE, null)
        set(value) = prefs.edit().putString(KEY_AI_TEMPLATE, value).apply()

    companion object {
        private const val KEY_DARK = "dark_mode_override"
        private const val KEY_SKIPPED_SIGNIN = "has_skipped_signin"
        private const val KEY_AUTO_SYNC_HOURS = "auto_sync_interval_hours"
        private const val KEY_LAST_SYNC = "last_sync_timestamp"
        private const val KEY_LOCK_ENABLED = "app_lock_enabled"
        private const val KEY_PIN_HASH = "app_lock_pin_hash"
        private const val KEY_LOCK_BIOMETRIC = "app_lock_use_biometric"
        private const val KEY_BACKUP_ENCRYPT = "backup_encryption_enabled"
        private const val KEY_AI_PROVIDER = "ai_provider"
        private const val KEY_GEMINI_KEY = "gemini_api_key"
        private const val KEY_CLAUDE_KEY = "claude_api_key"
        private const val KEY_AI_TEMPLATE = "ai_prompt_template"

        fun hashPin(pin: String): String {
            val digest = java.security.MessageDigest.getInstance("SHA-256").digest(pin.toByteArray())
            return digest.joinToString("") { "%02x".format(it) }
        }
    }
}
