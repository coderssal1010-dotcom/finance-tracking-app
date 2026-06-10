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

    suspend fun getCategorization(description: String, categories: List<String>): String {
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return fallbackCategorization(description, categories)
        }

        val prompt = """
            Given the transaction description: "$description"
            And the list of available spending categories: ${categories.joinToString(", ")}
            Choose the single matching category that best fits this transaction.
            Return ONLY the exact name of the category, with no extra text, punctuation, or reasoning.
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(parts = listOf(GeminiPart(text = prompt)))
            ),
            generationConfig = GeminiGenerationConfig(temperature = 0.1)
        )

        return try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            val result = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
            if (!result.isNullOrEmpty() && categories.any { it.equals(result, ignoreCase = true) }) {
                categories.first { it.equals(result, ignoreCase = true) }
            } else {
                fallbackCategorization(description, categories)
            }
        } catch (e: Exception) {
            fallbackCategorization(description, categories)
        }
    }

    suspend fun getFinancialInsights(transactions: List<Transaction>, categories: List<CustomCategory>): String {
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return "Please configure your GEMINI_API_KEY in the Secrets panel to activate personalized AI-powered financial coaching, spending trend analysis, and monthly budgeting recommendations."
        }

        val txData = transactions.joinToString("\n") { tx ->
            "Amount: ${tx.amount}, Desc: ${tx.description}, Category: ${tx.category}, Date: ${tx.date}"
        }

        val prompt = """
            You are an expert personal financial advisor and tracker assistant.
            Analyze the following monthly list of transactions:
            $txData

            Provide a concise, motivating, and actionable breakdown of spending habits, monthly trends, and potential savings recommendations. Keep the analysis structure as:
            1. Spending Summary (high-level overview of total income, spend, and net savings)
            2. Category Callouts (unusual spend behaviors or top categories)
            3. Privacy & Offline tips (praising the offline & security setup of this app)
            Keep it clean and easy to scan using standard Markdown. Do not include excessive details. Limit to 3 short paragraphs / bullet sections.
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(parts = listOf(GeminiPart(text = prompt)))
            ),
            generationConfig = GeminiGenerationConfig(temperature = 0.5)
        )

        return try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "Insights temporarily unavailable. Please retry shortly."
        } catch (e: Exception) {
            "Expert analysis connection interrupted: ${e.message}"
        }
    }

    private fun fallbackCategorization(description: String, categories: List<String>): String {
        val desc = description.lowercase()
        return when {
            desc.contains("payroll") || desc.contains("salary") || desc.contains("deposit") || desc.contains("income") -> 
                categories.find { it.contains("Income", ignoreCase = true) } ?: categories.first()
            desc.contains("starbucks") || desc.contains("coffee") || desc.contains("mcdonald") || desc.contains("restaurant") || desc.contains("food") || desc.contains("cafe") || desc.contains("bites") -> 
                categories.find { it.contains("Food", ignoreCase = true) } ?: categories.first()
            desc.contains("uber") || desc.contains("gas") || desc.contains("chevron") || desc.contains("shell") || desc.contains("car") || desc.contains("transit") -> 
                categories.find { it.contains("Transport", ignoreCase = true) } ?: categories.first()
            desc.contains("netflix") || desc.contains("hulu") || desc.contains("spotify") || desc.contains("game") || desc.contains("cinema") || desc.contains("movie") -> 
                categories.find { it.contains("Entertainment", ignoreCase = true) } ?: categories.first()
            desc.contains("comcast") || desc.contains("electric") || desc.contains("power") || desc.contains("water") || desc.contains("bill") -> 
                categories.find { it.contains("Utilities", ignoreCase = true) } ?: categories.first()
            desc.contains("rent") || desc.contains("house") || desc.contains("apartment") || desc.contains("mortgage") -> 
                categories.find { it.contains("Rent", ignoreCase = true) } ?: categories.first()
            desc.contains("amazon") || desc.contains("target") || desc.contains("grocery") || desc.contains("store") || desc.contains("shopping") -> 
                categories.find { it.contains("Shopping", ignoreCase = true) } ?: categories.first()
            else -> categories.first()
        }
    }
}
