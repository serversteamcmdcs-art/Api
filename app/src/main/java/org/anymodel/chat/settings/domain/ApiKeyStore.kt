package org.anymodel.chat.settings.domain

/** Хранилище API-ключа. Ключ никогда не попадает в плагины, логи и историю чатов. */
interface ApiKeyStore {
    fun get(): String?
    fun set(key: String)
    fun clear()
    fun hasKey(): Boolean = !get().isNullOrBlank()
}
