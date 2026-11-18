package org.anymodel.chat.core.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import org.anymodel.chat.chat.data.ApiChatRepository
import org.anymodel.chat.chat.domain.ChatRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class ChatBindingsModule {

    @Binds
    @Singleton
    abstract fun bindChatRepository(impl: ApiChatRepository): ChatRepository
}
