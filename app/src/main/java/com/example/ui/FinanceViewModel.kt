package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class FinanceViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = MarketplaceRepository(database.marketplaceDao)
    private val geminiService = GeminiService()

    // --- Database Flow Streams ---
    val products: StateFlow<List<Product>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMessages: StateFlow<List<ChatMessage>> = repository.allMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val receipts: StateFlow<List<PaymentReceipt>> = repository.allReceipts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reviews: StateFlow<List<SellerReview>> = repository.allReviews
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Authentication States ---
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    // --- Navigation & Filter States ---
    private val _currentTab = MutableStateFlow("Browse")
    val currentTab: StateFlow<String> = _currentTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _selectedLocation = MutableStateFlow("All")
    val selectedLocation: StateFlow<String> = _selectedLocation.asStateFlow()

    private val _selectedProduct = MutableStateFlow<Product?>(null)
    val selectedProduct: StateFlow<Product?> = _selectedProduct.asStateFlow()

    // --- Chat Active Room States ---
    private val _activeChatProductId = MutableStateFlow<Int?>(null)
    val activeChatProductId: StateFlow<Int?> = _activeChatProductId.asStateFlow()

    private val _isOfflineMode = MutableStateFlow(false)
    val isOfflineMode: StateFlow<Boolean> = _isOfflineMode.asStateFlow()

    private val _isSyncingMessages = MutableStateFlow(false)
    val isSyncingMessages: StateFlow<Boolean> = _isSyncingMessages.asStateFlow()

    // --- Gemini Support States ---
    private val _isGeneratingDescription = MutableStateFlow(false)
    val isGeneratingDescription: StateFlow<Boolean> = _isGeneratingDescription.asStateFlow()

    private val _generatedDescription = MutableStateFlow("")
    val generatedDescription: StateFlow<String> = _generatedDescription.asStateFlow()

    // --- Checkout & Payment States ---
    private val _activePaymentProduct = MutableStateFlow<Product?>(null)
    val activePaymentProduct: StateFlow<Product?> = _activePaymentProduct.asStateFlow()

    private val _paymentProcessing = MutableStateFlow(false)
    val paymentProcessing: StateFlow<Boolean> = _paymentProcessing.asStateFlow()

    init {
        viewModelScope.launch {
            repository.prepopulateInitialProducts()
            
            // Set up a pre-configured active session so user sees a clean state immediately,
            // but can log out and log in with phone / email / Gmail.
            val salagaOwner = User(
                id = "salaga_store",
                name = "Yaro Salaga Store",
                contactMethod = "Google",
                phoneNumber = "+233 24 123 4567",
                email = "salagahub@gmail.com",
                location = "Salaga, Savannah",
                momoNumber = "0241234567",
                momoCarrier = "MTN MoMo",
                isVerified = true,
                verificationType = "Ghana Card"
            )
            repository.insertUser(salagaOwner)

            // Current logged-in user
            val defaultBuyer = User(
                id = "ghana_buyer",
                name = "Kwame Mensah",
                contactMethod = "Phone",
                phoneNumber = "+233 55 987 6543",
                email = "kwame@ghanamail.com",
                location = "Accra, Greater Accra",
                momoNumber = "0559876543",
                momoCarrier = "MTN MoMo",
                isVerified = false,
                verificationType = "None"
            )
            repository.insertUser(defaultBuyer)
            _currentUser.value = defaultBuyer
        }
    }

    // --- Tab Controllers ---
    fun selectTab(tab: String) {
        _currentTab.value = tab
    }

    fun selectProduct(product: Product?) {
        _selectedProduct.value = product
        if (product != null) {
            _activeChatProductId.value = product.id
        }
    }

    fun startChatWithProduct(product: Product) {
        _selectedProduct.value = product
        _activeChatProductId.value = product.id
        _currentTab.value = "Chat"
    }

    fun startNewListing() {
        _generatedDescription.value = ""
        _currentTab.value = "Sell"
    }

    // --- Filtering Logic ---
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateCategory(category: String) {
        _selectedCategory.value = category
    }

    fun updateLocation(location: String) {
        _selectedLocation.value = location
    }

    // --- Seller Operations ---
    fun postProduct(
        title: String,
        description: String,
        price: Double,
        category: String,
        location: String,
        acceptsMoMo: Boolean,
        acceptsCard: Boolean,
        momoNumber: String,
        momoCarrier: String,
        extraImageIds: String
    ) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val product = Product(
                title = title,
                description = description,
                price = price,
                category = category,
                location = location,
                sellerId = user.id,
                sellerName = user.name,
                sellerPhone = user.phoneNumber.ifEmpty { "+233 24 000 0000" },
                sellerEmail = user.email,
                isSold = false,
                isPremium = false,
                imageId = (1..6).random(),
                extraImageIds = extraImageIds,
                acceptsMoMo = acceptsMoMo,
                acceptsCard = acceptsCard,
                momoNumber = momoNumber,
                momoCarrier = momoCarrier,
                isSellerVerified = user.isVerified,
                sellerVerificationBadge = if (user.isVerified) user.verificationType else "Self-Declared Account"
            )
            repository.insertProduct(product)
            _currentTab.value = "Browse"
        }
    }

    fun promoteProduct(product: Product) {
        viewModelScope.launch {
            val updated = product.copy(isPremium = true)
            repository.updateProduct(updated)
            if (_selectedProduct.value?.id == product.id) {
                _selectedProduct.value = updated
            }
        }
    }

    fun markProductAsSold(product: Product) {
        viewModelScope.launch {
            val updated = product.copy(isSold = true)
            repository.updateProduct(updated)
            if (_selectedProduct.value?.id == product.id) {
                _selectedProduct.value = updated
            }
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            repository.deleteProduct(product)
            _selectedProduct.value = null
        }
    }

    // --- Authentication Operations ---
    fun loginOrSignUp(name: String, identifier: String, method: String, phoneVal: String = "", emailVal: String = "") {
        if (name.trim().isEmpty() || identifier.trim().isEmpty()) {
            _authError.value = "Name and login ID details cannot be blank."
            return
        }
        viewModelScope.launch {
            val user = User(
                id = identifier.trim(),
                name = name.trim(),
                contactMethod = method,
                phoneNumber = if (method == "Phone") identifier else phoneVal,
                email = if (method == "Email" || method == "Google") identifier else emailVal,
                location = "Salaga, Savannah"
            )
            repository.insertUser(user)
            _currentUser.value = user
            _authError.value = null
        }
    }

    fun logout() {
        _currentUser.value = null
        _currentTab.value = "Browse"
    }

    fun verifyUser(idType: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val isVerifiedVal = idType != "None"
            val updated = user.copy(
                isVerified = isVerifiedVal,
                verificationType = if (isVerifiedVal) idType else ""
            )
            repository.insertUser(updated)
            _currentUser.value = updated

            // Update all outstanding listings belonging to this seller
            val list = products.value.filter { it.sellerId == user.id }
            list.forEach { prod ->
                val updatedProd = prod.copy(
                    isSellerVerified = isVerifiedVal,
                    sellerVerificationBadge = if (isVerifiedVal) idType else ""
                )
                repository.updateProduct(updatedProd)
            }
        }
    }

    fun submitReview(sellerId: String, rating: Int, reviewText: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val review = SellerReview(
                sellerId = sellerId,
                buyerId = user.id,
                buyerName = user.name.ifEmpty { "Anonymous Buyer" },
                rating = rating,
                reviewText = reviewText
            )
            repository.insertReview(review)
        }
    }

    // --- Chat P2P Operations ---
    fun setOfflineMode(enabled: Boolean) {
        _isOfflineMode.value = enabled
        if (!enabled) {
            syncOfflineMessages()
        }
    }

    fun syncOfflineMessages() {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val pending = allMessages.value.filter { it.status == "PENDING" && it.senderId == user.id }
            if (pending.isEmpty()) return@launch

            _isSyncingMessages.value = true
            delay(1200) // Realistic syncing simulator delay

            pending.forEach { msg ->
                val updatedMsg = msg.copy(status = "SENT")
                repository.insertMessage(updatedMsg)
                simulateSellerResponse(msg.productId, msg.messageText)
            }
            _isSyncingMessages.value = false
        }
    }

    fun sendChatMessage(productId: Int, messageText: String) {
        val user = _currentUser.value ?: return
        val items = products.value
        val product = items.find { it.id == productId } ?: return

        viewModelScope.launch {
            val stat = if (_isOfflineMode.value) "PENDING" else "SENT"
            // Sender is Current User, recipient is Product Seller
            val firstMsg = ChatMessage(
                productId = productId,
                productTitle = product.title,
                senderId = user.id,
                senderName = user.name,
                recipientId = product.sellerId,
                recipientName = product.sellerName,
                messageText = messageText,
                status = stat
            )
            repository.insertMessage(firstMsg)

            if (!_isOfflineMode.value) {
                simulateSellerResponse(productId, messageText)
            }
        }
    }

    private suspend fun simulateSellerResponse(productId: Int, messageText: String) {
        val user = _currentUser.value ?: return
        val items = products.value
        val product = items.find { it.id == productId } ?: return

        // Trigger a realistic Ghanaian seller P2P automated negotiation responder!
        delay(1500)
        val responseString = when {
            messageText.lowercase().contains("last price") || messageText.lowercase().contains("discount") -> {
                "Hello ${user.name}, for this beautiful ${product.title}, the last price I can do is ${"%.2f".format(product.price * 0.9)} GHS. Where are you located so we can arrange delivery?"
            }
            messageText.lowercase().contains("momo") || messageText.lowercase().contains("payment") -> {
                "Yes, you can pay directly. I accept ${product.momoCarrier} on ${product.momoNumber.ifEmpty { "my phone" }}. Or you can pay securely with standard card in the app!"
            }
            messageText.lowercase().contains("salaga") || messageText.lowercase().contains("where") -> {
                "I am currently located in ${product.location}. You can visit my store directly or we can do MTN Mobile Money then I send it via bus / transport driver."
            }
            else -> {
                "Aaafandey! Thanks for your message about ${product.title}. Yes, it is still fully available. When are you ready to pick it up? You can reach me on ${product.sellerPhone}."
            }
        }

        val replyMsg = ChatMessage(
            productId = productId,
            productTitle = product.title,
            senderId = product.sellerId,
            senderName = product.sellerName,
            recipientId = user.id,
            recipientName = user.name,
            messageText = responseString,
            status = "SENT"
        )
        repository.insertMessage(replyMsg)
    }

    // --- Payment Checkout Operations (Mobile Money & Bank Cards) ---
    fun initiateCheckout(product: Product) {
        _activePaymentProduct.value = product
    }

    fun cancelCheckout() {
        _activePaymentProduct.value = null
    }

    fun processPayment(
        product: Product,
        buyerName: String,
        buyerContact: String,
        paymentMethod: String // "MTN MoMo", "Telecel Cash", "AT Money", "Visa/MasterCard"
    ) {
        viewModelScope.launch {
            _paymentProcessing.value = true
            delay(2500) // simulator lag for processing

            val txRef = "TXN-" + UUID.randomUUID().toString().take(8).uppercase()
            val receipt = PaymentReceipt(
                productId = product.id,
                productTitle = product.title,
                amountPaid = product.price,
                buyerName = buyerName,
                buyerContact = buyerContact,
                paymentMethod = paymentMethod,
                transactionRef = txRef
            )
            repository.insertReceipt(receipt)
            
            // Mark the product as sold
            val updated = product.copy(isSold = true)
            repository.updateProduct(updated)
            if (_selectedProduct.value?.id == product.id) {
                _selectedProduct.value = updated
            }

            _activePaymentProduct.value = null
            _paymentProcessing.value = false
            _currentTab.value = "Receipts"
        }
    }

    // --- Gemini Support Action ---
    fun generateListingAI(title: String, category: String, location: String, points: String) {
        if (title.isBlank()) {
            _generatedDescription.value = "Please input a title first before requesting AI assistant help."
            return
        }
        viewModelScope.launch {
            _isGeneratingDescription.value = true
            val desc = geminiService.generateProductDescription(
                title = title,
                category = category,
                location = location,
                details = points
            )
            _generatedDescription.value = desc
            _isGeneratingDescription.value = false
        }
    }
}
