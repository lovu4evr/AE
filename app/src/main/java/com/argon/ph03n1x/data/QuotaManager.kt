package com.argon.ph03n1x.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class QuotaManager(context: Context) {

    companion object {
        private const val PREFS_NAME = "argon_quota_secure"
        private const val KEY_MESSAGE_COUNT = "message_count"
        private const val KEY_USER_API_KEY = "user_api_key"
        private const val FREE_TIER_LIMIT = 20
        private const val FREE_BEARER = "Bearer ARGON_INTERNAL_FREE_POOL"
    }

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun getMessageCount(): Int = prefs.getInt(KEY_MESSAGE_COUNT, 0)

    fun incrementMessageCount() {
        val current = getMessageCount()
        prefs.edit().putInt(KEY_MESSAGE_COUNT, current + 1).apply()
    }

    fun isFreeTierExhausted(): Boolean = getMessageCount() >= FREE_TIER_LIMIT

    fun hasUserApiKey(): Boolean {
        val key = prefs.getString(KEY_USER_API_KEY, null)
        return !key.isNullOrBlank()
    }

    fun getUserApiKey(): String? = prefs.getString(KEY_USER_API_KEY, null)

    fun saveUserApiKey(apiKey: String) {
        prefs.edit().putString(KEY_USER_API_KEY, apiKey.trim()).apply()
    }

    fun clearUserApiKey() {
        prefs.edit().remove(KEY_USER_API_KEY).apply()
    }

    fun resolveBearerToken(): String? {
        if (hasUserApiKey()) {
            return "Bearer ${getUserApiKey()}"
        }
        return if (!isFreeTierExhausted()) FREE_BEARER else null
    }

    fun getQuotaBadgeText(): String {
        return if (hasUserApiKey()) {
            "[ LOCKED // CUSTOM API KEY ACTIVE ]"
        } else if (isFreeTierExhausted()) {
            "[ LOCKED // CUSTOM API KEY REQUIRED ]"
        } else {
            "[ ${getMessageCount()} / $FREE_TIER_LIMIT FREE TIER ACTIVE ]"
        }
    }

    fun resetQuotaForTesting() {
        prefs.edit().putInt(KEY_MESSAGE_COUNT, 0).apply()
    }
}