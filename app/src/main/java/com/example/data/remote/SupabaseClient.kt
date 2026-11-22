package com.example.data.remote

import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.ConnectionPool
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.math.pow
import kotlin.random.Random

object SupabaseConfig {
    val url: String
        get() = BuildConfig.SUPABASE_URL.trim().trimEnd('/')

    val anonKey: String
        get() = BuildConfig.SUPABASE_ANON_KEY.trim()

    val isConfigured: Boolean
        get() = url.startsWith("https://") &&
            !url.contains("seu-projeto.supabase.co") &&
            anonKey.isNotBlank() &&
            !anonKey.contains("SUA_CHAVE_ANON")
}

object SupabaseClient {

    val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    private val okHttpClient: OkHttpClient by lazy {
        val headersInterceptor = Interceptor { chain ->
            val original = chain.request()
            val builder = original.newBuilder()
                .header("apikey", SupabaseConfig.anonKey)
            if (original.header("Accept") == null) {
                builder.header("Accept", "application/json")
            }
            chain.proceed(builder.build())
        }

        // Backoff exponencial com jitter para suportar picos de 200+ lojas ADM simultâneas + funcionários
        val retryInterceptor = Interceptor { chain ->
            val request = chain.request()
            val maxAttempts = 3
            var attempt = 0
            var lastException: IOException? = null
            var response: Response? = null

            while (attempt < maxAttempts) {
                attempt++
                try {
                    response?.close()
                    response = chain.proceed(request)
                    val code = response.code
                    val shouldRetryStatus = code == 429 || code == 502 || code == 503 || code == 504
                    val canRetryMethod = request.method.equals("GET", ignoreCase = true) ||
                        code == 429 || code == 503
                    if (!shouldRetryStatus || !canRetryMethod || attempt >= maxAttempts) {
                        return@Interceptor response
                    }
                } catch (e: IOException) {
                    lastException = e
                    val isIdempotent = request.method.equals("GET", ignoreCase = true)
                    if (!isIdempotent || attempt >= maxAttempts) {
                        throw e
                    }
                }
                val delayMs = (350.0 * 2.0.pow(attempt - 1) + Random.nextInt(80, 260)).toLong()
                try {
                    Thread.sleep(delayMs)
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    break
                }
            }
            response ?: throw (lastException ?: IOException("Falha na comunicação com o Supabase"))
        }

        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        OkHttpClient.Builder()
            .connectionPool(ConnectionPool(16, 5, TimeUnit.MINUTES))
            .addInterceptor(headersInterceptor)
            .addInterceptor(retryInterceptor)
            .addInterceptor(logging)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(25, TimeUnit.SECONDS)
            .build()
    }

    val api: SupabaseApi by lazy {
        val baseUrl = if (SupabaseConfig.url.startsWith("http")) {
            "${SupabaseConfig.url}/"
        } else {
            "https://seu-projeto.supabase.co/"
        }

        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(SupabaseApi::class.java)
    }
}
