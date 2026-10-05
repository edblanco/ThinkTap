package com.dosparta.trivia.sdk

// Native adapters classify transport failures using TriviaSdkException.
internal actual fun isNetworkFailure(exception: Exception): Boolean = false
