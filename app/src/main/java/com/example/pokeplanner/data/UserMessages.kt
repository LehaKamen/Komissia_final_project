package com.example.pokeplanner.data

import retrofit2.HttpException
import java.io.IOException

fun userMessage(error: Exception): String = when (error) {
    is IncompleteSyncException -> error.message.orEmpty()
    is HttpException -> if (error.code() ==
        404
    ) {
        "Покемон не найден"
    } else {
        "Сервер временно недоступен. Повторите попытку."
    }
    is IOException -> "Не удалось загрузить данные. Проверьте интернет и повторите попытку."
    is IllegalArgumentException -> error.message ?: "Проверьте введённые данные"
    else -> "Не удалось выполнить действие. Повторите попытку."
}
