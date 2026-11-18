package org.anymodel.chat.core.network

import android.content.Context
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlinx.coroutines.CancellationException
import org.anymodel.chat.R
import retrofit2.HttpException

/** Переводит исключения сети в понятный пользователю текст. */
object ApiErrors {

    fun message(context: Context, error: Throwable): String = when (error) {
        is NoApiKeyException -> context.getString(R.string.error_no_key)
        is HttpException -> when (error.code()) {
            401, 403 -> context.getString(R.string.error_unauthorized)
            429 -> context.getString(R.string.error_rate_limit)
            else -> context.getString(R.string.error_unknown, "HTTP ${error.code()}")
        }
        is ChatHttpException -> when (error.code) {
            401, 403 -> context.getString(R.string.error_unauthorized)
            429 -> context.getString(R.string.error_rate_limit)
            else -> context.getString(
                R.string.error_unknown,
                "HTTP ${error.code}" + (error.serverMessage?.let { ": $it" } ?: "")
            )
        }
        is SocketTimeoutException -> context.getString(R.string.error_timeout)
        is UnknownHostException -> context.getString(R.string.error_no_network)
        is IOException -> context.getString(R.string.error_no_network)
        else -> context.getString(R.string.error_unknown, error.localizedMessage ?: error.javaClass.simpleName)
    }

    /** Не глотаем отмену корутины при runCatching. */
    fun rethrowCancellation(error: Throwable) {
        if (error is CancellationException) throw error
    }
}

/**
 * Наследник IOException намеренно: OkHttp оборачивает любые другие исключения из интерсептора
 * в обычный IOException, и тип ошибки терялся бы до UI.
 */
class NoApiKeyException : IOException("API key is not set")

/** HTTP-ошибка при стриминге (EventSource не бросает retrofit HttpException). */
class ChatHttpException(val code: Int, val serverMessage: String?) :
    IOException("HTTP $code" + (serverMessage?.let { ": $it" } ?: ""))
