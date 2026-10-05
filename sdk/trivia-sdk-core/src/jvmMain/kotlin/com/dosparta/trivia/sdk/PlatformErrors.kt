package com.dosparta.trivia.sdk

import java.io.IOException

internal actual fun isNetworkFailure(exception: Exception): Boolean = exception is IOException
