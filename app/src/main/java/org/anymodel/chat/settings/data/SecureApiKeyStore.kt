package org.anymodel.chat.settings.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import org.anymodel.chat.settings.domain.ApiKeyStore

@Singleton
class SecureApiKeyStore @Inject constructor(
    @ApplicationContext private val context: Context
) : ApiKeyStore {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    override fun get(): String? = prefs.getString(KEY_API, null)

    override fun set(key: String) = prefs.edit { putString(KEY_API, key.trim()) }

    override fun clear() = prefs.edit { remove(KEY_API) }

    private companion object {
        const val FILE_NAME = "secure_api_prefs"
        const val KEY_API = "api_key"
    }
}
