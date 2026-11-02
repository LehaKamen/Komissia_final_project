package com.example.pokeplanner.data

import retrofit2.HttpException
import java.io.IOException

class PermanentSyncException(message: String) : IOException(message)

fun isRetryableSyncError(error: Throwable): Boolean = when (error) {
    is IncompleteSyncException -> error.retryable
    is PermanentSyncException -> false
    is HttpException -> isRetryableHttpStatus(error.code())
    is IOException -> true
    else -> false
}

fun isRetryableHttpStatus(code: Int): Boolean = code == 408 || code == 429 || code in 500..599
