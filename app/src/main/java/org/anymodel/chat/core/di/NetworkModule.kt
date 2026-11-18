package org.anymodel.chat.core.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.anymodel.chat.BuildConfig
import org.anymodel.chat.core.network.AnyModelApi
import org.anymodel.chat.core.network.NoApiKeyException
import org.anymodel.chat.settings.domain.ApiKeyStore
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = false
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(keyStore: ApiKeyStore): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val key = keyStore.get()
                if (key.isNullOrBlank()) throw NoApiKeyException()
                val original = chain.request()
                val builder = original.newBuilder()
                    .header("Authorization", "Bearer $key")
                // Не затираем Accept: text/event-stream, который выставляет EventSource.
                if (original.header("Accept") == null) builder.header("Accept", "application/json")
                chain.proceed(builder.build())
            }
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideAnyModelApi(retrofit: Retrofit): AnyModelApi =
        retrofit.create(AnyModelApi::class.java)
}
