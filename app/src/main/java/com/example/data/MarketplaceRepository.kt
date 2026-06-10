package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class MarketplaceRepository(private val dao: MarketplaceDao) {

    val allProducts: Flow<List<Product>> = dao.getAllProducts()
    val allMessages: Flow<List<ChatMessage>> = dao.getAllMessages()
    val allReceipts: Flow<List<PaymentReceipt>> = dao.getAllReceipts()
    val allReviews: Flow<List<SellerReview>> = dao.getAllReviews()

    suspend fun insertProduct(product: Product) {
        dao.insertProduct(product)
    }

    suspend fun updateProduct(product: Product) {
        dao.updateProduct(product)
    }

    suspend fun deleteProduct(product: Product) {
        dao.deleteProduct(product)
    }

    suspend fun getUserById(id: String): User? {
        return dao.getUserById(id).firstOrNull()
    }

    fun observeUserById(id: String): Flow<User?> {
        return dao.getUserById(id)
    }

    suspend fun insertUser(user: User) {
        dao.insertUser(user)
    }

    fun getMessagesForProduct(productId: Int): Flow<List<ChatMessage>> {
        return dao.getMessagesForProduct(productId)
    }

    suspend fun insertMessage(message: ChatMessage) {
        dao.insertMessage(message)
    }

    suspend fun insertReceipt(receipt: PaymentReceipt) {
        dao.insertReceipt(receipt)
    }

    suspend fun clearAllProducts() {
        dao.clearAllProducts()
    }

    suspend fun prepopulateInitialProducts() {
        val current = allProducts.firstOrNull() ?: emptyList()
        if (current.isEmpty()) {
            val now = System.currentTimeMillis()
            val day = 24 * 60 * 60 * 1000L
            val samples = listOf(
                Product(
                    title = "Premium Salaga White Yams (10 Tubers)",
                    description = "Freshly harvested premium grade-A white yams directly from Salaga yams market. Very soft, sweet, and highly rich. Perfect for home cooking, ceremonies, or commercial resale. Delivered securely with flexible payment.",
                    price = 180.0,
                    category = "Yam & Produce",
                    location = "Salaga, Savannah",
                    sellerId = "salaga_store",
                    sellerName = "Salaga Farmers Hub",
                    sellerPhone = "+233 24 123 4567",
                    sellerEmail = "salagahub@gmail.com",
                    isSold = false,
                    createdAt = now - day * 1,
                    isPremium = true,
                    imageId = 1,
                    acceptsMoMo = true,
                    acceptsCard = true,
                    momoNumber = "0241234567",
                    momoCarrier = "MTN MoMo",
                    isSellerVerified = true,
                    sellerVerificationBadge = "Ghana Card Verified"
                ),
                Product(
                    title = "Pure Organic Northern Shea Butter (5kg Container)",
                    description = "100% natural, unrefined shea butter made traditionally in Ghana's Savannah region. Highly moisturizing, rich in vitamins A and E, with no additives. Perfect for skincare, haircare, and cosmetic formulation.",
                    price = 120.0,
                    category = "Local Crafts & Cosmetics",
                    location = "Salaga, Savannah",
                    sellerId = "salaga_store",
                    sellerName = "Salaga Farmers Hub",
                    sellerPhone = "+233 24 123 4567",
                    sellerEmail = "salagahub@gmail.com",
                    isSold = false,
                    createdAt = now,
                    isPremium = true,
                    imageId = 2,
                    acceptsMoMo = true,
                    acceptsCard = true,
                    momoNumber = "0241234567",
                    momoCarrier = "MTN MoMo",
                    isSellerVerified = true,
                    sellerVerificationBadge = "Ghana Card Verified"
                ),
                Product(
                    title = "Infinix Hot 40 Pro - 256GB Dual SIM",
                    description = "Excellent condition Infinix Hot 40 Pro. 8GB RAM, 256GB Storage, Palm Blue color. Battery health is at 98%. Comes with box, charger, and a protective silicon case. Selling within Tamale or Salaga. Meetups allowed.",
                    price = 1850.0,
                    category = "Electronics & Phones",
                    location = "Tamale, Northern",
                    sellerId = "seller_tamale",
                    sellerName = "Baba Tamale Electronics",
                    sellerPhone = "+233 20 987 6543",
                    sellerEmail = "baba_tech@gmail.com",
                    isSold = false,
                    createdAt = now - day * 2,
                    isPremium = false,
                    imageId = 3,
                    acceptsMoMo = true,
                    acceptsCard = false,
                    momoNumber = "0209876543",
                    momoCarrier = "Telecel Cash",
                    isSellerVerified = false,
                    sellerVerificationBadge = "Self-Declared User"
                ),
                Product(
                    title = "Handwoven Smock (Batakari) - Unisex",
                    description = "Beautifully designed royal handwoven smock from northern Ghana. Heavy standard quality threads woven by masters of the craft. Size L, blue-and-white strip style. Ideal for festivals, church, wedding, or formal wear.",
                    price = 350.0,
                    category = "Fashion & Apparel",
                    location = "Kumasi, Ashanti",
                    sellerId = "kumasi_weaver",
                    sellerName = "Kofi Owusu Designs",
                    sellerPhone = "+233 27 555 1212",
                    sellerEmail = "kofidesigns@gmail.com",
                    isSold = false,
                    createdAt = now - day * 3,
                    isPremium = true,
                    imageId = 4,
                    acceptsMoMo = true,
                    acceptsCard = true,
                    momoNumber = "0275551212",
                    momoCarrier = "AT Money",
                    isSellerVerified = true,
                    sellerVerificationBadge = "MoMo Identity Match"
                ),
                Product(
                    title = "Nasco 32-Inch Smart LED Satellite TV",
                    description = "Gently used Nasco 32-inch LED TV. Smart TV capabilities with pre-installed Netflix, YouTube, and prime video. Crystal clear display, HDMI ports, USB. Has built-in satellite decoder. Moving out sale.",
                    price = 950.0,
                    category = "Home & Appliances",
                    location = "Accra, Greater Accra",
                    sellerId = "accra_student",
                    sellerName = "Adjoa Mensah",
                    sellerPhone = "+233 24 333 4444",
                    sellerEmail = "adjoamensah@gmail.com",
                    isSold = false,
                    createdAt = now - day * 4,
                    isPremium = false,
                    imageId = 5,
                    acceptsMoMo = true,
                    acceptsCard = true,
                    momoNumber = "0243334444",
                    momoCarrier = "MTN MoMo",
                    isSellerVerified = true,
                    sellerVerificationBadge = "Ghana Card Verified"
                )
            )
            for (product in samples) {
                dao.insertProduct(product)
            }
            prepopulateInitialReviews()
        }
    }

    fun getReviewsForSeller(sellerId: String): Flow<List<SellerReview>> {
        return dao.getReviewsForSeller(sellerId)
    }

    suspend fun insertReview(review: SellerReview) {
        dao.insertReview(review)
    }

    suspend fun prepopulateInitialReviews() {
        val current = dao.getAllReviews().firstOrNull() ?: emptyList()
        if (current.isEmpty()) {
            val now = System.currentTimeMillis()
            val hour = 60 * 60 * 1000L
            val initialReviews = listOf(
                SellerReview(
                    sellerId = "salaga_store", // Salaga Farmers Hub
                    buyerId = "buyer_kwame",
                    buyerName = "Kwame Mensah",
                    rating = 5,
                    reviewText = "Extremely happy with the fresh yams! Sweet and high quality. Delivered quickly in Accra via VIP Bus. Solid honest farmer.",
                    timestamp = now - hour * 12
                ),
                SellerReview(
                    sellerId = "salaga_store", // Salaga Farmers Hub
                    buyerId = "buyer_ama",
                    buyerName = "Ama Serwaa",
                    rating = 5,
                    reviewText = "The shea butter is indeed pure and rich! Unrefined with genuine natural Savannah scent. Excellent transaction.",
                    timestamp = now - hour * 24
                ),
                SellerReview(
                    sellerId = "salaga_store", // Salaga Farmers Hub
                    buyerId = "buyer_baba",
                    buyerName = "Baba Ibrahim",
                    rating = 4,
                    reviewText = "Reliable food supplies. Answered all my MoMo concerns quickly. Highly recommended.",
                    timestamp = now - hour * 48
                ),
                SellerReview(
                    sellerId = "seller_tamale", // Baba Tamale Electronics
                    buyerId = "buyer_fuseini",
                    buyerName = "Fuseini Yakubu",
                    rating = 4,
                    reviewText = "Good Infinix phone, description was accurate. Met in-person at Tamale Jubilee Park. Safe dealing.",
                    timestamp = now - hour * 36
                ),
                SellerReview(
                    sellerId = "kumasi_weaver", // Kofi Owusu Designs
                    buyerId = "buyer_efua",
                    buyerName = "Efua Gyamfi",
                    rating = 5,
                    reviewText = "Superb Batakari smock! Elegant premium loom design. Fits perfectly as described for our wedding event.",
                    timestamp = now - hour * 18
                ),
                SellerReview(
                    sellerId = "accra_student", // Adjoa Mensah
                    buyerId = "buyer_richard",
                    buyerName = "Richard Darko",
                    rating = 5,
                    reviewText = "The Nasco TV looks exactly like new. Clear smart screen. Polite seller, quick handoff near UG campus.",
                    timestamp = now - hour * 6
                )
            )
            for (review in initialReviews) {
                dao.insertReview(review)
            }
        }
    }
}
