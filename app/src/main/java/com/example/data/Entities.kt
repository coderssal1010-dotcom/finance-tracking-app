package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "marketplace_users")
data class User(
    @PrimaryKey val id: String, // email, phone, or google account id
    val name: String,
    val contactMethod: String, // "Email", "Phone", "Google"
    val phoneNumber: String = "",
    val email: String = "",
    val location: String = "Salaga, Savannah",
    val isSeller: Boolean = true,
    val regDate: Long = System.currentTimeMillis(),
    val acceptsMoMo: Boolean = true,
    val acceptsCard: Boolean = true,
    val momoNumber: String = "",
    val momoCarrier: String = "MTN MoMo",
    val isVerified: Boolean = false,
    val verificationType: String = "" // "Ghana Card", "Mobile Money", "None"
)

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val price: Double,
    val category: String, // e.g. "Yams & Produce", "Electronics", "Fashion", "Local Crafts", "Home & Design"
    val location: String, // e.g. "Salaga, Savannah", "Accra, Greater Accra", "Kumasi, Ashanti", "Tamale, Northern"
    val sellerId: String,
    val sellerName: String,
    val sellerPhone: String,
    val sellerEmail: String = "",
    val isSold: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val isPremium: Boolean = false, // Jiji-style promoted ads
    val imageId: Int = 1, // index for dummy image / visual
    val extraImageIds: String = "1,2,3", // comma-separated indexes for carousel
    val acceptsMoMo: Boolean = true,
    val acceptsCard: Boolean = true,
    val momoNumber: String = "",
    val momoCarrier: String = "MTN MoMo", // "MTN MoMo", "Telecel Cash", "AT Money"
    val isSellerVerified: Boolean = false,
    val sellerVerificationBadge: String = "" // "Ghana Card Connected" or "MoMo Identity Match"
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val productId: Int,
    val productTitle: String,
    val senderId: String,
    val senderName: String,
    val recipientId: String,
    val recipientName: String,
    val messageText: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "SENT" // "PENDING" (offline) or "SENT"
)

@Entity(tableName = "payment_receipts")
data class PaymentReceipt(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val productId: Int,
    val productTitle: String,
    val amountPaid: Double,
    val buyerName: String,
    val buyerContact: String,
    val paymentMethod: String, // "MTN MoMo", "Telecel Cash", "AT Money", "Visa Code", "MasterCard"
    val transactionRef: String,
    val date: Long = System.currentTimeMillis(),
    val status: String = "Successful"
)

@Entity(tableName = "seller_reviews")
data class SellerReview(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sellerId: String, // email, store id, or Google profile ID
    val buyerId: String,
    val buyerName: String,
    val rating: Int, // 1 to 5 stars
    val reviewText: String,
    val timestamp: Long = System.currentTimeMillis()
)

