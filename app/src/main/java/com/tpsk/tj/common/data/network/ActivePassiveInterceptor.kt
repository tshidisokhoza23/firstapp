package com.tpsk.tj.common.data.network

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivePassiveInterceptor @Inject constructor(
    private val envManager: NetworkEnvironmentManager
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        
        val newBaseUrl = envManager.currentUrl.toHttpUrl()
        
        val newUrl = request.url.newBuilder()
            .scheme(newBaseUrl.scheme)
            .host(newBaseUrl.host)
            .port(newBaseUrl.port)
            .build()
            
        request = request.newBuilder()
            .url(newUrl)
            .build()
            
        android.util.Log.d("NetworkInterceptor", "Request: ${request.method} ${request.url}")
        return chain.proceed(request)
    }
}
