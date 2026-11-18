package org.anymodel.chat.core.network

import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import org.anymodel.chat.BuildConfig

/**
 * Стриминг POST /chat/completions (stream: true) через OkHttp EventSource.
 * Возвращает поток текстовых дельт. Отмена коллектора (кнопка «Стоп») закрывает соединение.
 */
@Singleton
class ChatStreamer @Inject constructor(
    client: OkHttpClient,
    private val json: Json
) {
    // Между чанками модель может «думать» долго — увеличиваем таймаут чтения только для стрима.
    private val sseClient: OkHttpClient = client.newBuilder()
        .readTimeout(2, TimeUnit.MINUTES)
        .build()

    fun stream(request: ChatRequestDto): Flow<String> = callbackFlow {
        val body = json.encodeToString(ChatRequestDto.serializer(), request)
            .toRequestBody("application/json".toMediaType())
        val httpRequest = Request.Builder()
            .url(BuildConfig.API_BASE_URL + "chat/completions")
            .post(body)
            .build()

        val listener = object : EventSourceListener() {
            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                if (data.trim() == "[DONE]") {
                    close()
                    return
                }
                val chunk = runCatching { json.decodeFromString(ChatChunkDto.serializer(), data) }
                    .getOrNull() ?: return
                val text = chunk.choices.firstOrNull()?.delta?.content
                if (!text.isNullOrEmpty()) trySend(text)
            }

            override fun onClosed(eventSource: EventSource) {
                close()
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                val error: Throwable = when {
                    response != null && !response.isSuccessful ->
                        ChatHttpException(response.code, readServerMessage(response))
                    t != null -> t
                    else -> IOException("Stream failed")
                }
                close(error)
            }
        }

        val source = EventSources.createFactory(sseClient).newEventSource(httpRequest, listener)
        awaitClose { source.cancel() }
    }.buffer(Channel.UNLIMITED) // иначе при медленном UI дельты могут потеряться

    private fun readServerMessage(response: Response): String? = try {
        val text = response.body?.string().orEmpty()
        json.decodeFromString(ApiErrorEnvelope.serializer(), text).error?.message
    } catch (_: Exception) {
        null
    }
}
