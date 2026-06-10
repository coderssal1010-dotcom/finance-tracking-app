package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MarketplaceDao {
    // --- Products (Buying & Selling Listings) ---
    @Query("SELECT * FROM products ORDER BY isPremium DESC, createdAt DESC")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE category = :category ORDER BY isPremium DESC, createdAt DESC")
    fun getProductsByCategory(category: String): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    fun getProductById(id: Int): Flow<Product?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product)

    @Update
    suspend fun updateProduct(product: Product)

    @Delete
    suspend fun deleteProduct(product: Product)

    @Query("DELETE FROM products")
    suspend fun clearAllProducts()

    // --- Users (Sellers / Buyers) ---
    @Query("SELECT * FROM marketplace_users WHERE id = :id LIMIT 1")
    fun getUserById(id: String): Flow<User?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    // --- Chat Messages (P2P Negotiations) ---
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessage>>

    @Query("SELECT * FROM chat_messages WHERE productId = :productId ORDER BY timestamp ASC")
    fun getMessagesForProduct(productId: Int): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage)

    // --- Payment Receipts ---
    @Query("SELECT * FROM payment_receipts ORDER BY date DESC")
    fun getAllReceipts(): Flow<List<PaymentReceipt>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReceipt(receipt: PaymentReceipt)

    // --- Seller Reviews & Star Ratings ---
    @Query("SELECT * FROM seller_reviews ORDER BY timestamp DESC")
    fun getAllReviews(): Flow<List<SellerReview>>

    @Query("SELECT * FROM seller_reviews WHERE sellerId = :sellerId ORDER BY timestamp DESC")
    fun getReviewsForSeller(sellerId: String): Flow<List<SellerReview>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: SellerReview)
}

@Database(
    entities = [
        User::class,
        Product::class,
        ChatMessage::class,
        PaymentReceipt::class,
        SellerReview::class
    ],
    version = 7, // increment version for migration
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract val marketplaceDao: MarketplaceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "salaga_market_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
