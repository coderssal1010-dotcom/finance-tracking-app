package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import com.example.BuildConfig

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    val text: String
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfig(
    val temperature: Double? = null,
    val responseMimeType: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<GeminiCandidate>?
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    val content: GeminiContent?
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    val service: GeminiApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
        retrofit.create(GeminiApiService::class.java)
    }
}

class GeminiService {
    private val apiKey: String = BuildConfig.GEMINI_API_KEY

    /**
     * Generates a polished, highly-engaging product description (Jiji style) for Ghana sellers.
     */
    suspend fun generateProductDescription(
        title: String,
        category: String,
        location: String,
        details: String
    ): String {
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return fallbackDescription(title, category, location, details)
        }

        val prompt = """
            You are an expert copywriter for P2P trading platforms in Ghana (similar to Jiji.com.gh).
            Generate a detailed, attractive, and polished product description for:
            - Product Title: "$title"
            - Category: "$category"
            - Seller Location: "$location"
            - Bullet Points/Key Details: "$details"

            The ad should:
            1. Sound authentic, professional, and trustworthy.
            2. Be optimized for Ghanaian buyers. Mention local context if relevant (e.g., location $location and standard local currencies GHS).
            3. Highlight safety notes concisely at the physical meetup (e.g., check the item before paying, use Mobile Money MTN MoMo/Telecel Cash/AT Money securely, or pay with card inside the app).
            
            Keep the response structured and easy to scan using standard clear paragraphs. Keep it under 150 words.
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(parts = listOf(GeminiPart(text = prompt)))
            ),
            generationConfig = GeminiGenerationConfig(temperature = 0.7)
        )

        return try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
                ?: fallbackDescription(title, category, location, details)
        } catch (e: Exception) {
            fallbackDescription(title, category, location, details)
        }
    }

    private fun fallbackDescription(title: String, category: String, location: String, details: String): String {
        return """
            Excellent selling $title based in $location!
            
            Details: $details
            - Category: $category
            - Location: $location
            
            Safety Tips: Meet the seller in a secure public place or a well-known landmark. Inspect and verify the item fully before making any payment. Supports Mobile Money (MTN MoMo, Telecel Cash, AT Money) and Credit cards.
        """.trimIndent()
    }
}
