package com.dosparta.core.network.interceptors

import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class LoggingInterceptor @Inject constructor() : Interceptor {
    private val delegate = HttpLoggingInterceptor().apply {
        // SDK hosts must not leak session tokens or response bodies through logging.
        level = HttpLoggingInterceptor.Level.NONE
    }

    override fun intercept(chain: Interceptor.Chain): Response =
        delegate.intercept(chain)
}
