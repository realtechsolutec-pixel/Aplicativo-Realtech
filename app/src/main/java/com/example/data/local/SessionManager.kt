package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.UserRole
import com.example.data.model.UserSession
import com.example.util.ImageCompressionPreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "tecsec_session")

class SessionManager(private val context: Context) {

    private object Keys {
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        val USER_ID = stringPreferencesKey("user_id")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val USER_NAME = stringPreferencesKey("user_name")
        val EXPIRES_AT = longPreferencesKey("expires_at")
        val USER_ROLE = stringPreferencesKey("user_role")
        val WORKSHOP_OWNER_ID = stringPreferencesKey("workshop_owner_id")
        val COMPRESSION_PRESET = stringPreferencesKey("compression_preset")
    }

    val sessionFlow: Flow<UserSession?> = context.dataStore.data.map { prefs ->
        val token = prefs[Keys.ACCESS_TOKEN]
        val refresh = prefs[Keys.REFRESH_TOKEN]
        val userId = prefs[Keys.USER_ID]
        val email = prefs[Keys.USER_EMAIL]
        if (token.isNullOrBlank() || refresh.isNullOrBlank() || userId.isNullOrBlank()) {
            null
        } else {
            val role = UserRole.fromCode(prefs[Keys.USER_ROLE])
            val ownerId = prefs[Keys.WORKSHOP_OWNER_ID]?.takeIf { it.isNotBlank() } ?: userId
            UserSession(
                accessToken = token,
                refreshToken = refresh,
                userId = userId,
                email = email ?: "",
                fullName = prefs[Keys.USER_NAME] ?: (email?.substringBefore("@") ?: "Técnico"),
                expiresAtEpochSec = prefs[Keys.EXPIRES_AT] ?: 0L,
                role = role,
                workshopOwnerId = ownerId
            )
        }
    }

    val compressionPresetFlow: Flow<ImageCompressionPreset> = context.dataStore.data.map { prefs ->
        ImageCompressionPreset.fromCode(prefs[Keys.COMPRESSION_PRESET])
    }

    suspend fun getCurrentSession(): UserSession? = sessionFlow.first()

    suspend fun getCompressionPreset(): ImageCompressionPreset = compressionPresetFlow.first()

    suspend fun saveCompressionPreset(preset: ImageCompressionPreset) {
        context.dataStore.edit { prefs ->
            prefs[Keys.COMPRESSION_PRESET] = preset.code
        }
    }

    suspend fun saveSession(session: UserSession) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ACCESS_TOKEN] = session.accessToken
            prefs[Keys.REFRESH_TOKEN] = session.refreshToken
            prefs[Keys.USER_ID] = session.userId
            prefs[Keys.USER_EMAIL] = session.email
            prefs[Keys.USER_NAME] = session.fullName
            prefs[Keys.EXPIRES_AT] = session.expiresAtEpochSec
            prefs[Keys.USER_ROLE] = session.role.code
            prefs[Keys.WORKSHOP_OWNER_ID] = session.workshopOwnerId
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.ACCESS_TOKEN)
            prefs.remove(Keys.REFRESH_TOKEN)
            prefs.remove(Keys.USER_ID)
            prefs.remove(Keys.USER_EMAIL)
            prefs.remove(Keys.USER_NAME)
            prefs.remove(Keys.EXPIRES_AT)
            prefs.remove(Keys.USER_ROLE)
            prefs.remove(Keys.WORKSHOP_OWNER_ID)
        }
    }
}
