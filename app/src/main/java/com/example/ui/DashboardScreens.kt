package com.example.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MainFinanceApp(viewModel: FinanceViewModel) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val activePaymentProduct by viewModel.activePaymentProduct.collectAsStateWithLifecycle()

    Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = Modifier.navigationBarsPadding(),
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 8.dp
            ) {
                listOf(
                    Triple("Browse", Icons.Default.Home, "Market"),
                    Triple("Chat", Icons.Default.Email, "Negotiate"),
                    Triple("Sell", Icons.Default.Add, "Sell Item"),
                    Triple("Receipts", Icons.Default.ShoppingCart, "Receipts"),
                    Triple("Profile", Icons.Default.Person, "My Hub")
                ).forEach { (tab, icon, label) ->
                    val isSelected = currentTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { viewModel.selectTab(tab) },
                        icon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        label = { Text(label, fontSize = 11.sp) },
                        modifier = Modifier.testTag("nav_tab_$tab")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (currentTab) {
                "Browse" -> BrowseMarketScreen(viewModel)
                "Chat" -> ChatNegotiateScreen(viewModel)
                "Sell" -> SellItemScreen(viewModel)
                "Receipts" -> ReceiptsScreen(viewModel)
                "Profile" -> ProfileHubScreen(viewModel)
            }

            activePaymentProduct?.let { product ->
                PaymentCheckoutDialog(
                    product = product,
                    viewModel = viewModel,
                    onDismiss = { viewModel.cancelCheckout() }
                )
            }
        }
    }
}

// ============================================
// BROWSE MARKET SCREEN
// ============================================
@Composable
fun BrowseMarketScreen(viewModel: FinanceViewModel) {
    val products by viewModel.products.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val selectedLocation by viewModel.selectedLocation.collectAsStateWithLifecycle()
    val selectedProduct by viewModel.selectedProduct.collectAsStateWithLifecycle()

    val categories = listOf("All", "Yam & Produce", "Electronics & Phones", "Fashion & Apparel", "Local Crafts & Cosmetics", "Home & Appliances")
    val locations = listOf("All", "Salaga, Savannah", "Accra, Greater Accra", "Kumasi, Ashanti", "Tamale, Northern")

    val maxProductPrice = remember(products) {
        val maxInList = products.maxOfOrNull { it.price } ?: 2000.0
        (Math.ceil(maxInList / 100.0) * 100.0).coerceAtLeast(2000.0).toFloat()
    }

    var priceRange by remember(maxProductPrice) { mutableStateOf(0f..maxProductPrice) }

    AnimatedContent(targetState = selectedProduct, label = "ProductTransition") { product ->
        if (product != null) {
            ProductDetailView(product = product, viewModel = viewModel, onBack = { viewModel.selectProduct(null) })
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(ForestGreen, ForestGreen.copy(alpha = 0.85f))
                            )
                        )
                        .padding(horizontal = 20.dp, vertical = 24.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                JmarketLogo(modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Jmarket",
                                    color = SavannahGold,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.testTag("app_header_title")
                                )
                            }
                            Badge(
                                containerColor = SavannahGold,
                                contentColor = Color.Black,
                                modifier = Modifier.padding(2.dp)
                            ) {
                                Text("Ghana P2P", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }

                        Text(
                            text = "Buy & sell directly around Ghanaian cities and all major regions",
                            color = Color(0xFFDCE6DF),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.updateSearchQuery(it) },
                            placeholder = { Text("Search yams, shea butter, electronics...", fontSize = 14.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search icon") },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear text")
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = SavannahGold,
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black,
                                focusedPlaceholderColor = Color.Gray,
                                unfocusedPlaceholderColor = Color.Gray
                            ),
                            shape = RoundedCornerShape(24.dp),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("search_bar_input")
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("Select Category", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            ScrollableChipRow(
                                items = categories,
                                selectedItem = selectedCategory,
                                onSelected = { viewModel.updateCategory(it) }
                            )
                        }
                    }

                    Text("Filter Location", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            ScrollableChipRow(
                                items = locations,
                                selectedItem = selectedLocation,
                                onSelected = { viewModel.updateLocation(it) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Filter by Budget",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.testTag("price_filter_section_title")
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "₵${priceRange.start.toInt()} - ₵${priceRange.endInclusive.toInt()}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Terracotta,
                                modifier = Modifier.testTag("price_filter_range_text")
                            )
                            if (priceRange.start > 0f || priceRange.endInclusive < maxProductPrice) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Reset",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .clickable { priceRange = 0f..maxProductPrice }
                                        .testTag("price_filter_reset_btn")
                                )
                            }
                        }
                    }

                    RangeSlider(
                        value = priceRange,
                        onValueChange = { priceRange = it },
                        valueRange = 0f..maxProductPrice,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                            .testTag("price_range_slider"),
                        colors = SliderDefaults.colors(
                            activeTrackColor = ForestGreen,
                            inactiveTrackColor = ForestGreen.copy(alpha = 0.15f),
                            thumbColor = SavannahGold,
                            activeTickColor = SavannahGold
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "₵0",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.testTag("price_filter_min_label")
                        )
                        Text(
                            text = "₵${maxProductPrice.toInt()}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.testTag("price_filter_max_label")
                        )
                    }
                }

                val filteredProducts = products.filter { product ->
                    val matchesQuery = product.title.contains(searchQuery, ignoreCase = true) ||
                            product.description.contains(searchQuery, ignoreCase = true)
                    val matchesCategory = selectedCategory == "All" || product.category == selectedCategory
                    val matchesLocation = selectedLocation == "All" || product.location == selectedLocation
                    val matchesPrice = product.price >= priceRange.start && product.price <= priceRange.endInclusive
                    matchesQuery && matchesCategory && matchesLocation && matchesPrice
                }

                if (filteredProducts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "No products found",
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No listings matches your filter.",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Try modifying your filters, clearing your search query, or post an ad yourself!",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.outline,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredProducts) { product ->
                            ProductGridItem(product = product, onClick = { viewModel.selectProduct(product) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScrollableChipRow(
    items: List<String>,
    selectedItem: String,
    onSelected: (String) -> Unit
) {
    androidx.compose.foundation.lazy.LazyRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(end = 12.dp)
    ) {
        items(items) { item ->
            val isSelected = item == selectedItem
            FilterChip(
                selected = isSelected,
                onClick = { onSelected(item) },
                label = { Text(item, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = SavannahGold,
                    selectedLabelColor = Color.Black,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.testTag("chip_$item")
            )
        }
    }
}

@Composable
fun ProductGridItem(product: Product, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("product_card_${product.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (product.isPremium) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (product.isPremium) 3.dp else 0.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(115.dp)
                        .background(
                            Brush.linearGradient(
                                colors = when (product.imageId % 4) {
                                    0 -> listOf(Color(0xFFE6D5B8), Color(0xFFC5A880))
                                    1 -> listOf(Color(0xFFD4E2D4), Color(0xFF99A98F))
                                    2 -> listOf(Color(0xFFF3C5C5), Color(0xFFC58B8B))
                                    else -> listOf(Color(0xFFCEE5D0), Color(0xFF94B49F))
                                }
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (product.category) {
                            "Yam & Produce" -> Icons.Default.Home
                            "Electronics & Phones" -> Icons.Default.Settings
                            "Fashion & Apparel" -> Icons.Default.Star
                            "Local Crafts & Cosmetics" -> Icons.Default.Person
                            else -> Icons.Default.Home
                        },
                        contentDescription = "Category Symbol",
                        tint = ForestGreen.copy(alpha = 0.7f),
                        modifier = Modifier.size(42.dp)
                    )
                }

                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = product.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "${"%,.2f".format(product.price)} GHS",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = Terracotta
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = "location",
                            tint = SavannahGold,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = product.location,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.outline,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (product.acceptsMoMo) {
                            PaymentBadge(label = "MoMo", bgColor = Color(0xFFFFF0B3), textColor = Color(0xFF7F6000))
                        }
                        if (product.acceptsCard) {
                            PaymentBadge(label = "Card", bgColor = Color(0xFFD2E3FC), textColor = Color(0xFF174EA6))
                        }
                        if (product.isSold) {
                            PaymentBadge(label = "SOLD", bgColor = Color(0xFFFCE8E6), textColor = Color(0xFFC5221F))
                        }
                    }
                }
            }

            if (product.isPremium && !product.isSold) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 8.dp)
                        .background(PremiumGold, RoundedCornerShape(4.dp))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = "Premium", tint = Color.Black, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("PROMOTED", fontSize = 8.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun PaymentBadge(label: String, bgColor: Color, textColor: Color) {
    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Text(label, fontSize = 8.sp, color = textColor, fontWeight = FontWeight.ExtraBold)
    }
}

// ============================================
// PRODUCT DETAIL VIEW
// ============================================
@Composable
fun ProductDetailView(
    product: Product,
    viewModel: FinanceViewModel,
    onBack: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val reviewsList by viewModel.reviews.collectAsStateWithLifecycle()
    val sellerReviews = reviewsList.filter { it.sellerId == product.sellerId }
    val averageRating = if (sellerReviews.isNotEmpty()) sellerReviews.map { it.rating }.average() else 0.0
    val totalReviews = sellerReviews.size
    var showSellerProfile by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("product_detail_screen")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
            }
            Text(
                text = "Listing Details",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            if (currentUser != null && currentUser?.id == product.sellerId) {
                IconButton(onClick = {
                    viewModel.deleteProduct(product)
                    onBack()
                }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Ad", tint = MaterialTheme.colorScheme.error)
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            item {
                ProductImageCarousel(product = product)

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = product.title,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Place, contentDescription = "Location", tint = SavannahGold, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(product.location, fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                    Text(
                        text = "${"%,.2f".format(product.price)} GHS",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Terracotta,
                        textAlign = TextAlign.End
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                Text("Seller Information", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showSellerProfile = true }
                        .testTag("product_detail_seller_card")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(SavannahGold, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = product.sellerName.take(1).uppercase(),
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(product.sellerName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                if (product.isSellerVerified) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Verified Seller",
                                        tint = RegularGreen,
                                        modifier = Modifier.size(16.dp).testTag("seller_verified_badge_icon")
                                    )
                                }
                            }
                            // Star Rating & Review count line
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 1.dp)
                            ) {
                                repeat(5) { index ->
                                    val isFilled = index < averageRating.toInt()
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = if (isFilled) SavannahGold else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                        modifier = Modifier.size(13.dp).testTag("seller_card_star_$index")
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (totalReviews > 0) "${"%.1f".format(averageRating)} ($totalReviews feedbacks)" else "No reviews yet",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.testTag("seller_card_rating_text")
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "• View Trust Profile",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.testTag("view_seller_profile_link")
                                )
                            }
                            if (product.isSellerVerified) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .background(RegularGreen.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = RegularGreen,
                                                modifier = Modifier.size(10.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = product.sellerVerificationBadge.ifEmpty { "Verified Member" },
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = RegularGreen,
                                                modifier = Modifier.testTag("seller_badge_text")
                                            )
                                        }
                                    }
                                }
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .background(Color.Gray.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Info,
                                                contentDescription = null,
                                                tint = Color.Gray,
                                                modifier = Modifier.size(10.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Self-Declared Account",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color.Gray,
                                                modifier = Modifier.testTag("seller_badge_unverified_text")
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Row {
                            IconButton(onClick = {
                                val intent = Intent(Intent.ACTION_DIAL).apply {
                                    data = Uri.parse("tel:${product.sellerPhone}")
                                }
                                context.startActivity(intent)
                            }) {
                                Icon(Icons.Default.Call, contentDescription = "Call", tint = ForestGreen)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Description", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = product.description,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Accepted Payment Channels", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (product.acceptsMoMo) {
                        PaymentMethodPill(
                            channelName = product.momoCarrier,
                            icon = Icons.Default.Refresh,
                            color = Color(0xFFFFF7C2)
                        )
                    }
                    if (product.acceptsCard) {
                        PaymentMethodPill(
                            channelName = "Bank Debit Card",
                            icon = Icons.Default.Lock,
                            color = Color(0xFFD4E4FC)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (currentUser == null || currentUser?.id != product.sellerId) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.startChatWithProduct(product) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("chat_seller_button")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Send, contentDescription = "Send Message", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Negotiate")
                            }
                        }

                        if (!product.isSold) {
                            Button(
                                onClick = { viewModel.initiateCheckout(product) },
                                colors = ButtonDefaults.buttonColors(containerColor = Terracotta, contentColor = Color.White),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1.1f)
                                    .height(50.dp)
                                    .testTag("buy_item_button")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = "Buy", modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Buy Now (Safe)")
                                }
                            }
                        } else {
                            Button(
                                onClick = {},
                                enabled = false,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1.1f)
                                    .height(50.dp)
                            ) {
                                Text("ALREADY SOLD")
                            }
                        }
                    }
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Manage Your Ad Space", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (!product.isPremium) {
                                    Button(
                                        onClick = { viewModel.promoteProduct(product) },
                                        colors = ButtonDefaults.buttonColors(containerColor = SavannahGold, contentColor = Color.Black),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Boost Ad")
                                    }
                                }

                                if (!product.isSold) {
                                    Button(
                                        onClick = { viewModel.markProductAsSold(product) },
                                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreen, contentColor = Color.White),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Mark as Sold")
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showSellerProfile) {
        SellerProfileReviewsDialog(
            sellerId = product.sellerId,
            sellerName = product.sellerName,
            sellerVerificationBadge = product.sellerVerificationBadge,
            isSellerVerified = product.isSellerVerified,
            viewModel = viewModel,
            onDismiss = { showSellerProfile = false }
        )
    }
}

@Composable
fun PaymentMethodPill(channelName: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Box(
        modifier = Modifier
            .background(color, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = channelName,
                tint = Color.Black,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(channelName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        }
    }
}

// ============================================
// CHAT NEGOTIATIONAL SCREEN
// ============================================
@Composable
fun ChatNegotiateScreen(viewModel: FinanceViewModel) {
    val allMessages by viewModel.allMessages.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val activeChatProductId by viewModel.activeChatProductId.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val isOfflineMode by viewModel.isOfflineMode.collectAsStateWithLifecycle()
    val isSyncingMessages by viewModel.isSyncingMessages.collectAsStateWithLifecycle()

    if (currentUser == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Lock, contentDescription = "Authentication Required", modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                Spacer(modifier = Modifier.height(12.dp))
                Text("P2P Chats Locked", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = "Please log in using email, phone number, or Google directly on the Profile hub page.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
                Button(
                    onClick = { viewModel.selectTab("Profile") },
                    colors = ButtonDefaults.buttonColors(containerColor = SavannahGold, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Go to Sign In / Sign Up")
                }
            }
        }
        return
    }

    val activeGroup = activeChatProductId

    if (activeGroup == null) {
        val messagesByProduct = allMessages.groupBy { it.productId }
        val productMap = products.associateBy { it.id }

        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ForestGreen)
                    .padding(18.dp)
            ) {
                Text(
                    text = "Transaction Negotiations 💬",
                    color = SavannahGold,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            OfflineStatusBar(
                isOffline = isOfflineMode,
                isSyncing = isSyncingMessages,
                onToggleOffline = { viewModel.setOfflineMode(it) }
            )

            if (messagesByProduct.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "No chats",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No negotiating yet. Go browse some yams and click contact!", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(messagesByProduct.keys.toList()) { prodId ->
                        val itemMsgs = messagesByProduct[prodId] ?: emptyList()
                        val latest = itemMsgs.lastOrNull()
                        val product = productMap[prodId]

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.selectProduct(product) }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(SavannahGold, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Home, contentDescription = "store", tint = Color.Black)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = product?.title ?: latest?.productTitle ?: "Negotiation Ad",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = latest?.messageText ?: "",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.outline,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = product?.let { "${"%,.0f".format(it.price)} GHS" } ?: "",
                                    fontWeight = FontWeight.Black,
                                    color = Terracotta,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    }
                }
            }
        }
    } else {
        val product = products.find { it.id == activeGroup }
        val dialogMessages = allMessages.filter { it.productId == activeGroup }

        var textInput by remember { mutableStateOf("") }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .testTag("chat_room_block")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.selectProduct(null) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(ForestGreen, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        product?.sellerName?.take(1)?.uppercase() ?: "S",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        product?.title ?: "Item Chat",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            product?.sellerName?.let { "Talking to $it" } ?: "Direct bargaining",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                        if (product?.isSellerVerified == true) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Verified Seller",
                                tint = RegularGreen,
                                modifier = Modifier.size(12.dp).testTag("chat_seller_verified_icon")
                            )
                        }
                    }
                }
                if (product != null && !product.isSold) {
                    Button(
                        onClick = { viewModel.initiateCheckout(product) },
                        colors = ButtonDefaults.buttonColors(containerColor = Terracotta),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("checkout_chat_btn")
                    ) {
                        Text("Pay Secure", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            OfflineStatusBar(
                isOffline = isOfflineMode,
                isSyncing = isSyncingMessages,
                onToggleOffline = { viewModel.setOfflineMode(it) }
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(dialogMessages) { msg ->
                    val isMyMsg = msg.senderId == currentUser?.id
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = if (isMyMsg) Alignment.CenterEnd else Alignment.CenterStart
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isMyMsg) ForestGreen else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = if (isMyMsg) {
                                RoundedCornerShape(12.dp, 1.dp, 12.dp, 12.dp)
                            } else {
                                RoundedCornerShape(1.dp, 12.dp, 12.dp, 12.dp)
                            },
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = msg.messageText,
                                    color = if (isMyMsg) Color.White else MaterialTheme.colorScheme.onBackground,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.End,
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Text(
                                        text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(msg.timestamp)),
                                        fontSize = 9.sp,
                                        color = if (isMyMsg) Color.White.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline
                                    )
                                    if (isMyMsg) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        if (msg.status == "PENDING") {
                                            Text(
                                                text = "🕒",
                                                fontSize = 9.sp,
                                                color = Color.White.copy(alpha = 0.7f),
                                                modifier = Modifier.testTag("msg_status_pending")
                                            )
                                        } else {
                                            Text(
                                                text = "✓✓",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SavannahGold,
                                                modifier = Modifier.testTag("msg_status_sent")
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = { Text("Barter price, ask for details...") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_text"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SavannahGold,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(10.dp))
                IconButton(
                    onClick = {
                        if (textInput.isNotEmpty()) {
                            viewModel.sendChatMessage(activeGroup, textInput)
                            textInput = ""
                        }
                    },
                    modifier = Modifier
                        .background(SavannahGold, CircleShape)
                        .testTag("send_msg_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send Message",
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

// ============================================
// SELL SCREEN (Ad Listing Form for P2P Sellers)
// ============================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellItemScreen(viewModel: FinanceViewModel) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val isGeneratingDescription by viewModel.isGeneratingDescription.collectAsStateWithLifecycle()
    val generatedDescription by viewModel.generatedDescription.collectAsStateWithLifecycle()

    if (currentUser == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Edit, contentDescription = "Authentication Required", modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                Spacer(modifier = Modifier.height(12.dp))
                Text("Seller Authentication Required", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = "Authentic P2P transactions in Jmarket Ghana require registering email, phone carrier, or direct Google accounts.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
                Button(
                    onClick = { viewModel.selectTab("Profile") },
                    colors = ButtonDefaults.buttonColors(containerColor = SavannahGold, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Go to Sign In / Sign Up")
                }
            }
        }
        return
    }

    var title by remember { mutableStateOf("") }
    var priceStr by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCat by remember { mutableStateOf("Yam & Produce") }
    var selectedLoc by remember { mutableStateOf("Salaga, Savannah") }

    var acceptsMoMo by remember { mutableStateOf(true) }
    var acceptsCard by remember { mutableStateOf(true) }
    var momoCarrier by remember { mutableStateOf("MTN MoMo") }
    var momoNumberBySeller by remember { mutableStateOf(currentUser?.momoNumber ?: "") }

    var formError by remember { mutableStateOf<String?>(null) }
    var briefNotesForAI by remember { mutableStateOf("") }

    var activeAngle0 by remember { mutableStateOf(true) }
    var activeAngle1 by remember { mutableStateOf(true) }
    var activeAngle2 by remember { mutableStateOf(true) }
    var activeAngle3 by remember { mutableStateOf(true) }

    LaunchedEffect(generatedDescription) {
        if (generatedDescription.isNotEmpty() && !generatedDescription.startsWith("Please input")) {
            description = generatedDescription
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("sell_item_form"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ForestGreen, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = "List New Product in Ghana 🇬🇭",
                    color = SavannahGold,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        item {
            Column {
                Text("Ad Title *", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("e.g., Premium Fresh Savannah Yams...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ad_title_input"),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SavannahGold)
                )
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Price (GHS) *", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    OutlinedTextField(
                        value = priceStr,
                        onValueChange = { priceStr = it },
                        placeholder = { Text("e.g. 150") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ad_price_input"),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SavannahGold)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text("Select Category", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    var expandedCat by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expandedCat,
                        onExpandedChange = { expandedCat = !expandedCat }
                    ) {
                        OutlinedTextField(
                            value = selectedCat,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCat) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SavannahGold)
                        )
                        ExposedDropdownMenu(
                            expanded = expandedCat,
                            onDismissRequest = { expandedCat = false }
                        ) {
                            listOf("Yam & Produce", "Electronics & Phones", "Fashion & Apparel", "Local Crafts & Cosmetics", "Home & Appliances").forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        selectedCat = cat
                                        expandedCat = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Column {
                Text("Ghana Location", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                var expandedLoc by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expandedLoc,
                    onExpandedChange = { expandedLoc = !expandedLoc }
                ) {
                    OutlinedTextField(
                        value = selectedLoc,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedLoc) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SavannahGold)
                    )
                    ExposedDropdownMenu(
                        expanded = expandedLoc,
                        onDismissRequest = { expandedLoc = false }
                    ) {
                        listOf("Salaga, Savannah", "Accra, Greater Accra", "Kumasi, Ashanti", "Tamale, Northern").forEach { loc ->
                            DropdownMenuItem(
                                text = { Text(loc) },
                                onClick = {
                                    selectedLoc = loc
                                    expandedLoc = false
                                }
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("⚡ Gemini Product Copywriter", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                    Text("Input rapid bulleted features, and Gemini will craft your polished marketing ad copy description instantly!", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = briefNotesForAI,
                        onValueChange = { briefNotesForAI = it },
                        placeholder = { Text("e.g. freshly brought in, sweet soft, 50 tubers ready") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_brief_notes"),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SavannahGold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.generateListingAI(title, selectedCat, selectedLoc, briefNotesForAI) },
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                        enabled = !isGeneratingDescription,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .align(Alignment.End)
                            .testTag("ai_generate_ad_button")
                    ) {
                        if (isGeneratingDescription) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "AI",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Generate Description", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        item {
            Column {
                Text("Ad Detailed Description *", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { Text("Describe item quality, conditions, and meetup instructions...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .testTag("ad_description_input"),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SavannahGold)
                )
            }
        }

        item {
            Column {
                Text("Enable Carousel Photos", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("Tap to select multiple premium styled slides for your listing carousel:", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val angles = listOf(
                        Triple(0, "Shot 1", "Primary View"),
                        Triple(1, "Shot 2", "Item Details"),
                        Triple(2, "Shot 3", "Registry Seal"),
                        Triple(3, "Shot 4", "Payment Detail")
                    )
                    
                    angles.forEach { (index, shotName, sub) ->
                        val isSelected = when (index) {
                            0 -> activeAngle0
                            1 -> activeAngle1
                            2 -> activeAngle2
                            else -> activeAngle3
                        }
                        
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(85.dp)
                                .clickable {
                                    val newVal = !isSelected
                                    when (index) {
                                        0 -> activeAngle0 = newVal
                                        1 -> activeAngle1 = newVal
                                        2 -> activeAngle2 = newVal
                                        3 -> activeAngle3 = newVal
                                    }
                                }
                                .testTag("angle_selection_card_$index"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) ForestGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.5.dp,
                                color = if (isSelected) ForestGreen else Color.Transparent
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Icon(
                                    imageVector = if (isSelected) Icons.Default.Check else Icons.Default.Add,
                                    contentDescription = null,
                                    tint = if (isSelected) ForestGreen else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(shotName, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isSelected) ForestGreen else MaterialTheme.colorScheme.onSurface)
                                    Text(sub, fontSize = 8.sp, color = MaterialTheme.colorScheme.outline, textAlign = TextAlign.Center, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            HorizontalDivider()
        }

        item {
            Column {
                Text("P2P Accepted Payment Methods", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(checked = acceptsMoMo, onCheckedChange = { acceptsMoMo = it })
                    Text("Accept Mobile Money (MoMo)", fontSize = 13.sp)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(checked = acceptsCard, onCheckedChange = { acceptsCard = it })
                    Text("Accept Credit / Bank Card Inside App", fontSize = 13.sp)
                }
            }
        }

        if (acceptsMoMo) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(modifier = Modifier.weight(1.1f)) {
                        Text("MoMo Registered Number", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                        OutlinedTextField(
                            value = momoNumberBySeller,
                            onValueChange = { momoNumberBySeller = it },
                            placeholder = { Text("e.g., 0241234567") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("seller_momo_number"),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SavannahGold)
                        )
                    }

                    Column(modifier = Modifier.weight(0.9f)) {
                        Text("Carrier Network", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                        var expandedNet by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = expandedNet,
                            onExpandedChange = { expandedNet = !expandedNet }
                        ) {
                            OutlinedTextField(
                                value = momoCarrier,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedNet) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SavannahGold)
                            )
                            ExposedDropdownMenu(
                                expanded = expandedNet,
                                onDismissRequest = { expandedNet = false }
                            ) {
                                listOf("MTN MoMo", "Telecel Cash", "AT Money").forEach { net ->
                                    DropdownMenuItem(
                                        text = { Text(net) },
                                        onClick = {
                                            momoCarrier = net
                                            expandedNet = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            formError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
            }

            Button(
                onClick = {
                    val price = priceStr.toDoubleOrNull()
                    if (title.isBlank() || price == null || description.isBlank()) {
                        formError = "Please execute all required fields successfully with a valid numerical price."
                    } else if (acceptsMoMo && momoNumberBySeller.isBlank()) {
                        formError = "Please declare registered Mobile Money account number."
                    } else {
                        val selectedAnglesList = mutableListOf<String>()
                        if (activeAngle0) selectedAnglesList.add("0")
                        if (activeAngle1) selectedAnglesList.add("1")
                        if (activeAngle2) selectedAnglesList.add("2")
                        if (activeAngle3) selectedAnglesList.add("3")
                        if (selectedAnglesList.isEmpty()) selectedAnglesList.add("0")
                        val extraImageIdsString = selectedAnglesList.joinToString(",")

                        viewModel.postProduct(
                            title = title,
                            description = description,
                            price = price,
                            category = selectedCat,
                            location = selectedLoc,
                            acceptsMoMo = acceptsMoMo,
                            acceptsCard = acceptsCard,
                            momoNumber = momoNumberBySeller,
                            momoCarrier = momoCarrier,
                            extraImageIds = extraImageIdsString
                        )
                        formError = null
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("submit_ad_button"),
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Publish Ghana Ad Now")
            }
        }
    }
}

// ============================================
// ESCROW PAYMENT RECEIPTS SCREEN
// ============================================
@Composable
fun ReceiptsScreen(viewModel: FinanceViewModel) {
    val receipts by viewModel.receipts.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(ForestGreen)
                .padding(18.dp)
        ) {
            Text(
                text = "Secure Payments Ledger",
                color = SavannahGold,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (receipts.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = "Receipts", modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No transactions logged. Safeguard yourself with digital escrow!", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(receipts) { receipt ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("receipt_${receipt.id}"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = receipt.paymentMethod,
                                    color = ForestGreen,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp
                                )
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFE2F3EB), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Success",
                                            tint = Color(0xFF137333),
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(receipt.status, color = Color(0xFF137333), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = receipt.productTitle,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${"%,.2f".format(receipt.amountPaid)} GHS",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp,
                                    color = Terracotta
                                )
                                Text(
                                    text = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(receipt.date)),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                            Row {
                                Text("Ref: ", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                Text(receipt.transactionRef, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Row {
                                Text("Buyer Details: ", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                Text("${receipt.buyerName} (${receipt.buyerContact})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================
// PROFILE / AUTH SCREEN (My Hub)
// ============================================
@Composable
fun ProfileHubScreen(viewModel: FinanceViewModel) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val authError by viewModel.authError.collectAsStateWithLifecycle()

    var nameVal by remember { mutableStateOf("") }
    var idVal by remember { mutableStateOf("") }
    var momoRegisteredVal by remember { mutableStateOf("") }

    var signUpTab by remember { mutableStateOf("Phone") }

    if (currentUser == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .testTag("login_screen_hub"),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                JmarketLogo(modifier = Modifier.size(38.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Welcome to Jmarket",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = ForestGreen
                )
            }
            Text(
                text = "Sign in to list items, negotiate with buyers, and process payments securely in Ghana",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp, horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            TabRow(
                selectedTabIndex = when (signUpTab) {
                    "Phone" -> 0
                    "Email" -> 1
                    else -> 2
                }
            ) {
                Tab(
                    selected = signUpTab == "Phone",
                    onClick = { signUpTab = "Phone" },
                    text = { Text("Phone", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("tab_login_phone")
                )
                Tab(
                    selected = signUpTab == "Email",
                    onClick = { signUpTab = "Email" },
                    text = { Text("Email", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("tab_login_email")
                )
                Tab(
                    selected = signUpTab == "Google",
                    onClick = { signUpTab = "Google" },
                    text = { Text("Google Direct", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("tab_login_google")
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = nameVal,
                    onValueChange = { nameVal = it },
                    label = { Text("Full Name / Store Name") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reg_name_input"),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SavannahGold)
                )

                when (signUpTab) {
                    "Phone" -> {
                        OutlinedTextField(
                            value = idVal,
                            onValueChange = { idVal = it },
                            label = { Text("Ghana Mobile Number") },
                            placeholder = { Text("e.g. 0241234567") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reg_phone_input"),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SavannahGold)
                        )
                    }
                    "Email" -> {
                        OutlinedTextField(
                            value = idVal,
                            onValueChange = { idVal = it },
                            label = { Text("Email Address") },
                            placeholder = { Text("e.g. seller@ghanamail.com") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reg_email_input"),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SavannahGold)
                        )
                    }
                    "Google" -> {
                        OutlinedTextField(
                            value = idVal,
                            onValueChange = { idVal = it },
                            label = { Text("Instant Gmail Address") },
                            placeholder = { Text("e.g. yourname@gmail.com") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reg_gmail_input"),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SavannahGold)
                        )
                    }
                }

                OutlinedTextField(
                    value = momoRegisteredVal,
                    onValueChange = { momoRegisteredVal = it },
                    label = { Text("Registered MoMo Wallet Carrier (Optional)") },
                    placeholder = { Text("e.g. 0241234567 (MTN MoMo)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SavannahGold)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            authError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
            }

            Button(
                onClick = {
                    viewModel.loginOrSignUp(
                        name = nameVal,
                        identifier = idVal,
                        method = signUpTab,
                        phoneVal = if (signUpTab == "Phone") idVal else "",
                        emailVal = if (signUpTab == "Email" || signUpTab == "Google") idVal else ""
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("register_user_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = RegularGreen),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Confirm & Direct Sign In", fontSize = 14.sp)
            }
        }
    } else {
        val user = currentUser ?: return
        val productsList by viewModel.products.collectAsStateWithLifecycle()
        val userAdsCount = productsList.count { it.sellerId == user.id }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .testTag("profile_hub_active")
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(ForestGreen, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        user.name.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(user.name, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("P2P Member since 2026", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(userAdsCount.toString(), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = ForestGreen)
                        Text("Active Ads", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Ghana GHS", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SavannahGold, modifier = Modifier.padding(top = 6.dp))
                        Text("Local Currency", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(user.contactMethod, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ForestGreen, modifier = Modifier.padding(top = 6.dp))
                        Text("Auth Provider", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Verification Records", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                    Row {
                        Text("Email Address: ", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                        Text(user.email.ifEmpty { "Not bound" }, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Row {
                        Text("Phone Number: ", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                        Text(user.phoneNumber.ifEmpty { "Not bound" }, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Row {
                        Text("Default Shop Location: ", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                        Text(user.location, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (user.isVerified) RegularGreen.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = if (user.isVerified) RegularGreen else Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("verification_control_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (user.isVerified) Icons.Default.CheckCircle else Icons.Default.Info,
                            contentDescription = null,
                            tint = if (user.isVerified) RegularGreen else SavannahGold,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = if (user.isVerified) "Verified Trust Status" else "P2P Verification Required",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (user.isVerified) RegularGreen else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (user.isVerified) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Status: SECURE PRO SELLER",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = RegularGreen
                            )
                            Text(
                                text = "Method: ${user.verificationType}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = "All your listed ads have also been automatically upgraded with premium green Verification badges to attract 4x more buyers.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = { viewModel.verifyUser("None") },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .height(26.dp)
                                    .align(Alignment.End)
                                    .testTag("revoke_verification_btn")
                            ) {
                                Text("Reset Verification", fontSize = 10.sp, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Get verified to unlock the premium green badge on all your listings. Verified listings receive 90% higher confidence rating from Ghanaian buyers.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(70.dp)
                                        .clickable { viewModel.verifyUser("Ghana Card Verified") }
                                        .testTag("input_verify_ghana_card"),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, SavannahGold)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize().padding(8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text("🇬🇭 Ghana Card", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ForestGreen)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("National Registry", fontSize = 8.sp, color = MaterialTheme.colorScheme.outline)
                                    }
                                }

                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(70.dp)
                                        .clickable { viewModel.verifyUser("MoMo Identity Match") }
                                        .testTag("input_verify_momo"),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, ForestGreen)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize().padding(8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text("📱 MoMo Wallet", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ForestGreen)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("Mobile Match", fontSize = 8.sp, color = MaterialTheme.colorScheme.outline)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { viewModel.logout() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("logout_button")
            ) {
                Text("Sign Out / Log Out", fontWeight = FontWeight.Bold)
            }
        }
    }
}

val RegularGreen = Color(0xFF2E7D32)

// ============================================
// MOBILE MONEY & CREDIT CARD PAYMENT DIALOG
// ============================================
@Composable
fun PaymentCheckoutDialog(
    product: Product,
    viewModel: FinanceViewModel,
    onDismiss: () -> Unit
) {
    val paymentProcessing by viewModel.paymentProcessing.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    var paymentType by remember { mutableStateOf("MoMo") }

    var momoCarrier by remember { mutableStateOf("MTN MoMo") }
    var momoNumber by remember { mutableStateOf(currentUser?.phoneNumber?.filter { it.isDigit() }?.takeLast(9)?.let { "0$it" } ?: "") }
    var pinVal by remember { mutableStateOf("") }

    var cardNumber by remember { mutableStateOf("") }
    var cardExpiry by remember { mutableStateOf("") }
    var cardCvv by remember { mutableStateOf("") }
    var otpVal by remember { mutableStateOf("") }

    var showPinChallenge by remember { mutableStateOf(false) }
    var showOtpChallenge by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = { if (!paymentProcessing) onDismiss() }) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("checkout_dialog_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Secure P2P Transaction", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = ForestGreen)
                    if (!paymentProcessing) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                }

                HorizontalDivider()

                Text(
                    text = "You are buying: ${product.title}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Price Amount:", fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
                    Text(
                        text = "${"%,.2f".format(product.price)} GHS",
                        fontWeight = FontWeight.ExtraBold,
                        color = Terracotta,
                        fontSize = 15.sp
                    )
                }

                if (!paymentProcessing && !showPinChallenge && !showOtpChallenge) {
                    TabRow(selectedTabIndex = if (paymentType == "MoMo") 0 else 1) {
                        Tab(
                            selected = paymentType == "MoMo",
                            onClick = { paymentType = "MoMo" },
                            text = { Text("Mobile Money") }
                        )
                        Tab(
                            selected = paymentType == "Card",
                            onClick = { paymentType = "Card" },
                            text = { Text("Bank Card") }
                        )
                    }

                    if (paymentType == "MoMo") {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Select Carrier Network", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("MTN MoMo", "Telecel Cash", "AT Money").forEach { carrier ->
                                    val isSelected = momoCarrier == carrier
                                    Button(
                                        onClick = { momoCarrier = carrier },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSelected) SavannahGold else MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Text(carrier, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            OutlinedTextField(
                                value = momoNumber,
                                onValueChange = { momoNumber = it },
                                label = { Text("Ghana MoMo Phone Number") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("checkout_momo_number")
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = cardNumber,
                                onValueChange = { cardNumber = it },
                                label = { Text("Debit/Credit Card Number") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("checkout_card_number")
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = cardExpiry,
                                    onValueChange = { cardExpiry = it },
                                    label = { Text("Expiry (MM/YY)") },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = cardCvv,
                                    onValueChange = { cardCvv = it },
                                    label = { Text("CVV") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(0.9f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = {
                            if (paymentType == "MoMo") {
                                showPinChallenge = true
                            } else {
                                showOtpChallenge = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("pay_confirm_button")
                    ) {
                        Text("Initiate Secure Transfer", fontWeight = FontWeight.Bold)
                    }
                } else if (showPinChallenge) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = "sec", modifier = Modifier.size(48.dp), tint = SavannahGold)
                        Text(
                            text = "Authorise $momoCarrier Debit",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "A prompt has been sent to $momoNumber. Enter your 4-digit MoMo vault PIN to execute escrow transfer of ${"%.2f".format(product.price)} GHS.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center
                        )
                        OutlinedTextField(
                            value = pinVal,
                            onValueChange = { pinVal = it },
                            label = { Text("Enter 4-digit PIN") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            visualTransformation = PasswordVisualTransformation(),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .width(150.dp)
                                .testTag("momo_pin_input"),
                            textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Center),
                            singleLine = true
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { showPinChallenge = false },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel")
                            }
                            Button(
                                onClick = {
                                    showPinChallenge = false
                                    viewModel.processPayment(
                                        product = product,
                                        buyerName = currentUser?.name ?: "Guest",
                                        buyerContact = momoNumber,
                                        paymentMethod = momoCarrier
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                                shape = RoundedCornerShape(8.dp),
                                enabled = pinVal.length >= 4,
                                modifier = Modifier
                                    .weight(1.2f)
                                    .testTag("momo_pin_submit_button")
                            ) {
                                Text("Confirm Transaction")
                            }
                        }
                    }
                } else if (showOtpChallenge) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Email, contentDescription = "sms", modifier = Modifier.size(48.dp), tint = ForestGreen)
                        Text("3D Verified Secure Code", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            text = "Input the standard SMS 6-digit OTP passcode sent to your banking register line to complete payments.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center
                        )
                        OutlinedTextField(
                            value = otpVal,
                            onValueChange = { otpVal = it },
                            label = { Text("SMS OTP Passcode") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .width(180.dp)
                                .testTag("card_otp_input"),
                            textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Center),
                            singleLine = true
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { showOtpChallenge = false },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel")
                            }
                            Button(
                                onClick = {
                                    showOtpChallenge = false
                                    viewModel.processPayment(
                                        product = product,
                                        buyerName = currentUser?.name ?: "Guest",
                                        buyerContact = "Card Ending " + cardNumber.takeLast(4),
                                        paymentMethod = "Visa/Card"
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                                shape = RoundedCornerShape(8.dp),
                                enabled = otpVal.isNotEmpty(),
                                modifier = Modifier
                                    .weight(1.2f)
                                    .testTag("card_otp_submit_button")
                            ) {
                                Text("Authorize Debit")
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = SavannahGold,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Authorising Secure Escrow Channels...",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Connecting securely to Ghanaian payment gateway...",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DecorativeGhanaFlag(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val barH = h / 3f

        drawRect(color = Color(0xFFCE1126), size = androidx.compose.ui.geometry.Size(w, barH))
        drawRect(color = Color(0xFFFCD116), topLeft = androidx.compose.ui.geometry.Offset(0f, barH), size = androidx.compose.ui.geometry.Size(w, barH))
        drawRect(color = Color(0xFF006B3F), topLeft = androidx.compose.ui.geometry.Offset(0f, barH * 2), size = androidx.compose.ui.geometry.Size(w, barH))

        val starCenter = androidx.compose.ui.geometry.Offset(w / 2f, h / 2f)
        val r = barH * 0.40f
        drawCircle(
            color = Color.Black,
            center = starCenter,
            radius = r,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

@Composable
fun JmarketLogo(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF10B981), Color(0xFF047857))
                ),
                shape = RoundedCornerShape(10.dp)
            )
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            
            // Draw shiny gold accent coin at bottom-right of safe area
            drawCircle(
                color = Color(0xFFFCD116),
                radius = w * 0.20f,
                center = androidx.compose.ui.geometry.Offset(w * 0.72f, h * 0.72f)
            )
            
            // Draw logo "J"
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(w * 0.65f, h * 0.26f)
                lineTo(w * 0.45f, h * 0.26f)
                moveTo(w * 0.55f, h * 0.26f)
                lineTo(w * 0.55f, h * 0.68f)
                quadraticBezierTo(
                    w * 0.55f, h * 0.85f,
                    w * 0.42f, h * 0.85f
                )
                quadraticBezierTo(
                    w * 0.28f, h * 0.85f,
                    w * 0.28f, h * 0.72f
                )
            }
            
            drawPath(
                path = path,
                color = Color.White,
                style = Stroke(
                    width = w * 0.11f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    join = androidx.compose.ui.graphics.StrokeJoin.Round
                )
            )
        }
    }
}

@Composable
fun OfflineStatusBar(
    isOffline: Boolean,
    isSyncing: Boolean,
    onToggleOffline: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOffline) Color(0xFFFEF3C7) else Color(0xFFE6F4EA)
        ),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            color = if (isOffline) Color(0xFFD97706) else Color(0xFF137333),
                            shape = CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = if (isOffline) "Offline Mode Active" else "Online (P2P Client)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = if (isOffline) Color(0xFF92400E) else Color(0xFF137333)
                    )
                    Text(
                        text = if (isOffline) "Saved offline inside Room DB" else "Sync active & responder online",
                        fontSize = 9.sp,
                        color = Color.DarkGray
                    )
                }
            }
            if (isSyncing) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 1.8.dp,
                        color = Color(0xFF137333)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
						text = "Syncing...", 
						fontSize = 10.sp, 
						fontWeight = FontWeight.Bold, 
						color = Color(0xFF137333),
						modifier = Modifier.testTag("syncing_spin_text")
					)
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            color = if (isOffline) Color(0xFFD97706) else Color(0xFF137333),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onToggleOffline(!isOffline) }
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                        .testTag("toggle_offline_mode_btn")
                ) {
                    Icon(
                        imageVector = if (isOffline) Icons.Default.Check else Icons.Default.Refresh,
                        contentDescription = "Toggle connection",
                        tint = Color.White,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isOffline) "Go Live" else "Go Offline",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

@Composable
fun ProductImagePage(
    category: String,
    imageIndex: Int,
    imageIdSeed: Int,
    modifier: Modifier = Modifier
) {
    val colors = when ((imageIdSeed + imageIndex) % 5) {
        0 -> listOf(Color(0xFFE6D5B8), Color(0xFFC5A880)) // Sand Gold
        1 -> listOf(Color(0xFF10B981), Color(0xFF047857)) // Forest Emerald
        2 -> listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8)) // Lagoon Blue
        3 -> listOf(Color(0xFFF43F5E), Color(0xFFBE123C)) // Terracotta Rose
        else -> listOf(Color(0xFF8B5CF6), Color(0xFF5B21B6)) // Royal Purple
    }

    Box(
        modifier = modifier
            .background(Brush.linearGradient(colors = colors)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color.White.copy(alpha = 0.08f),
                radius = size.width * 0.45f,
                center = androidx.compose.ui.geometry.Offset(size.width * 0.2f, size.height * 0.2f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.05f),
                radius = size.width * 0.35f,
                center = androidx.compose.ui.geometry.Offset(size.width * 0.8f, size.height * 0.8f)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            val (icon, title, desc) = when (imageIndex) {
                0 -> {
                    val catIcon = when (category) {
                        "Yam & Produce" -> Icons.Default.Home
                        "Electronics & Phones" -> Icons.Default.Settings
                        "Fashion & Apparel" -> Icons.Default.Star
                        "Local Crafts & Cosmetics" -> Icons.Default.Person
                        else -> Icons.Default.Home
                    }
                    Triple(catIcon, "Primary View", "100% Inspected & Direct P2P Listing")
                }
                1 -> {
                    Triple(Icons.Default.Info, "Item Details", "Inspected condition, ready for hand-off")
                }
                2 -> {
                    Triple(Icons.Default.Check, "Ghanaian Authentic", "Ghana local product registry guaranteed")
                }
                else -> {
                    Triple(Icons.Default.Lock, "Secure Trade Badge", "Supports Mobile Money & Safe Escrow")
                }
            }

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(54.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title.uppercase(Locale.ROOT),
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = Color.White.copy(alpha = 0.9f),
                letterSpacing = 1.2.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = desc,
                fontSize = 12.sp,
                color = Color.White,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
    }
}

@Composable
fun ProductImageCarousel(
    product: Product,
    modifier: Modifier = Modifier
) {
    val indices = remember(product.extraImageIds) {
        val parsed = product.extraImageIds.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
        if (parsed.isEmpty()) listOf(0, 1, 2, 3) else parsed
    }

    var activeIndex by remember { mutableStateOf(0) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(16.dp))
            .testTag("product_image_carousel_${product.id}")
    ) {
        val imageIdx = indices.getOrElse(activeIndex) { 0 }
        ProductImagePage(
            category = product.category,
            imageIndex = imageIdx,
            imageIdSeed = product.imageId,
            modifier = Modifier.fillMaxSize()
        )

        if (product.isPremium) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .background(PremiumGold, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("BOOSTED AD", fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }

        if (indices.size > 1) {
            IconButton(
                onClick = {
                    activeIndex = if (activeIndex == 0) indices.size - 1 else activeIndex - 1
                },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 8.dp)
                    .background(Color.Black.copy(alpha = 0.35f), CircleShape)
                    .size(36.dp)
                    .testTag("carousel_prev_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Previous Image",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = {
                    activeIndex = if (activeIndex == indices.size - 1) 0 else activeIndex + 1
                },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp)
                    .background(Color.Black.copy(alpha = 0.35f), CircleShape)
                    .size(36.dp)
                    .testTag("carousel_next_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Next Image",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp).rotate(180f)
                )
            }
        }

        if (indices.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
                    .background(Color.Black.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                indices.indices.forEach { index ->
                    val isSelected = index == activeIndex
                    Box(
                        modifier = Modifier
                            .size(if (isSelected) 8.dp else 6.dp)
                            .background(
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f),
                                shape = CircleShape
                            )
                            .clickable { activeIndex = index }
                            .testTag("carousel_dot_$index")
                    )
                }
            }
        }
    }
}

@Composable
fun SellerProfileReviewsDialog(
    sellerId: String,
    sellerName: String,
    sellerVerificationBadge: String,
    isSellerVerified: Boolean,
    viewModel: FinanceViewModel,
    onDismiss: () -> Unit
) {
    val reviewsList by viewModel.reviews.collectAsStateWithLifecycle()
    val sellerReviews = reviewsList.filter { it.sellerId == sellerId }
    val averageRating = if (sellerReviews.isNotEmpty()) sellerReviews.map { it.rating }.average() else 0.0
    val totalReviews = sellerReviews.size

    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    var ratingSelected by remember { mutableStateOf(5) }
    var reviewTextState by remember { mutableStateOf("") }
    var submitSuccess by remember { mutableStateOf(false) }
    var submitError by remember { mutableStateOf("") }

    val count5 = sellerReviews.count { it.rating == 5 }
    val count4 = sellerReviews.count { it.rating == 4 }
    val count3 = sellerReviews.count { it.rating == 3 }
    val count2 = sellerReviews.count { it.rating == 2 }
    val count1 = sellerReviews.count { it.rating == 1 }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .testTag("seller_profile_dialog_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header (Title & Close Button)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Seller Trust Profile",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("seller_profile_title")
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_seller_profile_dialog")) {
                        Icon(Icons.Default.Close, contentDescription = "Close Detail Sheet")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Profile Header Detail Card
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth().testTag("seller_profile_info_card")
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(SavannahGold, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = sellerName.take(1).uppercase(),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp,
                                        color = Color.Black
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = sellerName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (isSellerVerified) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Verified",
                                                tint = RegularGreen,
                                                modifier = Modifier.size(16.dp).testTag("dialog_seller_verified_badge")
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (isSellerVerified) {
                                            "Status: " + if (sellerVerificationBadge.isNotEmpty()) sellerVerificationBadge else "Verified Merchant"
                                        } else {
                                            "Status: Self-Declared Account (Not Verified)"
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isSellerVerified) RegularGreen else Color.Gray,
                                        modifier = Modifier.testTag("dialog_seller_status_badge")
                                    )
                                }
                            }
                        }
                    }

                    // Rating Summary Card
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth().testTag("seller_profile_rating_distribution_card")
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Reputation & Trust Score",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = if (totalReviews > 0) "%.1f".format(averageRating) else "0.0",
                                            fontSize = 32.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Terracotta,
                                            modifier = Modifier.testTag("dialog_average_score")
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            repeat(5) { index ->
                                                val isFilled = index < averageRating.toInt()
                                                Icon(
                                                    imageVector = Icons.Default.Star,
                                                    contentDescription = null,
                                                    tint = if (isFilled) SavannahGold else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "$totalReviews Feedbacks",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.testTag("dialog_feedbacks_count")
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    Column(
                                        modifier = Modifier.weight(1.8f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                         listOf(
                                             5 to count5,
                                             4 to count4,
                                             3 to count3,
                                             2 to count2,
                                             1 to count1
                                         ).forEach { (star, count) ->
                                             Row(
                                                 verticalAlignment = Alignment.CenterVertically,
                                                 horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                 modifier = Modifier.fillMaxWidth().testTag("rating_bar_row_$star")
                                             ) {
                                                 Text("${star}★", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(14.dp))
                                                 val ratio = if (totalReviews > 0) count.toFloat() / totalReviews else 0f
                                                 Box(
                                                     modifier = Modifier
                                                         .weight(1f)
                                                         .height(6.dp)
                                                         .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), RoundedCornerShape(3.dp))
                                                 ) {
                                                     Box(
                                                         modifier = Modifier
                                                             .fillMaxHeight()
                                                             .fillMaxWidth(ratio)
                                                             .background(SavannahGold, RoundedCornerShape(3.dp))
                                                     )
                                                 }
                                                 Text(
                                                     text = "${count}",
                                                     fontSize = 10.sp,
                                                     color = MaterialTheme.colorScheme.outline,
                                                     modifier = Modifier.width(15.dp),
                                                     textAlign = TextAlign.End
                                                 )
                                             }
                                         }
                                    }
                                }
                            }
                        }
                    }

                    // Reviews List
                    item {
                        Text(
                            text = "Reviews From Ghanaian Buyers ($totalReviews)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    if (sellerReviews.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No reviews yet. Be the first to transact and review!",
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                            }
                        }
                    } else {
                        items(sellerReviews.size) { index ->
                            val review = sellerReviews[index]
                            Card(
                                modifier = Modifier.fillMaxWidth().testTag("review_card_$index"),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = review.buyerName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Row {
                                            repeat(5) { starIndex ->
                                                val isFilled = starIndex < review.rating
                                                Icon(
                                                    imageVector = Icons.Default.Star,
                                                    contentDescription = null,
                                                    tint = if (isFilled) SavannahGold else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                                    modifier = Modifier.size(11.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = review.reviewText,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    val dateText = android.text.format.DateFormat.format("MMM dd, yyyy", review.timestamp).toString()
                                    Text(
                                        text = dateText,
                                        fontSize = 9.sp,
                                        color = Color.Gray,
                                        modifier = Modifier.align(Alignment.End)
                                    )
                                }
                            }
                        }
                    }

                    // Submit feedback form
                    item {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    }

                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Gray.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "Submit Trusted Feedback",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.testTag("submit_feedback_title")
                            )
                            Spacer(modifier = Modifier.height(4.dp))

                            if (currentUser == null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                        .background(SavannahGold.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                        .padding(10.dp)
                                        .testTag("review_locked_message")
                                ) {
                                    Text(
                                        text = "🔒 Review Locked. Please log in first on the Profile page to leave ratings for this seller.",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF886A00)
                                    )
                                }
                            } else if (currentUser?.id == sellerId) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                        .background(Color.Gray.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                        .padding(10.dp)
                                        .testTag("review_self_message")
                                ) {
                                    Text(
                                        text = "ℹ️ You cannot leave a rating review on your own seller profile.",
                                        fontSize = 11.sp,
                                        color = Color.Gray,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            } else {
                                if (submitSuccess) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp)
                                            .background(RegularGreen.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                            .padding(10.dp)
                                            .testTag("review_submission_success_banner")
                                    ) {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = RegularGreen, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Review submitted successfully!",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = ForestGreen
                                                )
                                            }
                                            Text(
                                                text = "Thank you for supporting community trust in the P2P marketplace.",
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                            TextButton(
                                                onClick = { submitSuccess = false },
                                                contentPadding = PaddingValues(0.dp),
                                                modifier = Modifier.height(24.dp).testTag("dismiss_success_review_btn")
                                            ) {
                                                Text("Submit another review", fontSize = 10.sp, color = ForestGreen)
                                            }
                                        }
                                    }
                                } else {
                                    Text("Select Rating Score:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth().testTag("interactive_rating_stars_row")
                                    ) {
                                        repeat(5) { index ->
                                            val starValue = index + 1
                                            val isFilled = starValue <= ratingSelected
                                            IconButton(
                                                onClick = {
                                                    ratingSelected = starValue
                                                },
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .testTag("star_select_$starValue")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Star,
                                                    contentDescription = "$starValue Stars",
                                                    tint = if (isFilled) SavannahGold else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                                    modifier = Modifier.size(28.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))

                                    OutlinedTextField(
                                        value = reviewTextState,
                                        onValueChange = {
                                            reviewTextState = it
                                            if (it.isNotEmpty()) submitError = ""
                                        },
                                        placeholder = { Text("Write about delivery speeds, yam quality, MoMo handling...", fontSize = 12.sp) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(84.dp)
                                            .testTag("review_input_field"),
                                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                                        shape = RoundedCornerShape(8.dp)
                                    )

                                    if (submitError.isNotEmpty()) {
                                        Text(
                                            text = submitError,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.padding(top = 4.dp).testTag("review_text_validation_error")
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Button(
                                        onClick = {
                                            if (reviewTextState.trim().isEmpty()) {
                                                submitError = "Please write a constructive review description."
                                            } else {
                                                viewModel.submitReview(sellerId, ratingSelected, reviewTextState.trim())
                                                reviewTextState = ""
                                                ratingSelected = 5
                                                submitSuccess = true
                                                submitError = ""
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .align(Alignment.End)
                                            .height(36.dp)
                                            .testTag("submit_review_button")
                                    ) {
                                        Text("Submit Review", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
