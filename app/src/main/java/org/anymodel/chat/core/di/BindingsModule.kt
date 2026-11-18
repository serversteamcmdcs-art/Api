package org.anymodel.chat.core.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import org.anymodel.chat.core.network.ApiModelsRepository
import org.anymodel.chat.settings.data.DataStoreSettingsRepository
import org.anymodel.chat.settings.data.SecureApiKeyStore
import org.anymodel.chat.settings.domain.ApiKeyStore
import org.anymodel.chat.settings.domain.ModelsRepository
import org.anymodel.chat.settings.domain.SettingsRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class BindingsModule {

    @Binds
    @Singleton
    abstract fun bindSettings(impl: DataStoreSettingsRepository): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindApiKeyStore(impl: SecureApiKeyStore): ApiKeyStore

    @Binds
    @Singleton
    abstract fun bindModels(impl: ApiModelsRepository): ModelsRepository
}
