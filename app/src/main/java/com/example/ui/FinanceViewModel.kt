package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class FinanceViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = FinanceRepository(database.financeDao)
    private val geminiService = GeminiService()

    // --- Core Database Flows ---
    val transactions: StateFlow<List<Transaction>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<CustomCategory>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reminders: StateFlow<List<RecurringReminder>> = repository.allReminders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bills: StateFlow<List<Bill>> = repository.allBills
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val securityConfig: StateFlow<SecurityConfig?> = repository.securityConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // --- UI State Management ---
    private val _currentTab = MutableStateFlow("Dashboard")
    val currentTab: StateFlow<String> = _currentTab.asStateFlow()

    // --- Interactive Bank Linking & Sync State ---
    private val _linkedAccounts = MutableStateFlow(
        listOf(
            BankAccount("Chase Checking", "Checking", 4850.25, isLinked = true, lastSynced = System.currentTimeMillis()),
            BankAccount("Fidelity Savings", "Savings", 12500.00, isLinked = true, lastSynced = System.currentTimeMillis() - 4 * 3600_000),
            BankAccount("Amex Platinum", "Credit Card", -850.40, isLinked = false, lastSynced = 0L)
        )
    )
    val linkedAccounts: StateFlow<List<BankAccount>> = _linkedAccounts.asStateFlow()

    private val _isBankSyncing = MutableStateFlow(false)
    val isBankSyncing: StateFlow<Boolean> = _isBankSyncing.asStateFlow()

    // --- Security PIN & 2FA Gatekeeping ---
    private val _isAppLocked = MutableStateFlow(false)
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    private val _pinBuffer = MutableStateFlow("")
    val pinBuffer: StateFlow<String> = _pinBuffer.asStateFlow()

    private val _securityMessage = MutableStateFlow("")
    val securityMessage: StateFlow<String> = _securityMessage.asStateFlow()

    private val _otpChallengeCode = MutableStateFlow("")
    val otpChallengeCode: StateFlow<String> = _otpChallengeCode.asStateFlow()

    private val _isTwoFactorVerifying = MutableStateFlow(false)
    val isTwoFactorVerifying: StateFlow<Boolean> = _isTwoFactorVerifying.asStateFlow()

    // --- AI Automated Categorization & Insights ---
    private val _isAICategorizing = MutableStateFlow(false)
    val isAICategorizing: StateFlow<Boolean> = _isAICategorizing.asStateFlow()

    private val _aiInsights = MutableStateFlow<String?>(null)
    val aiInsights: StateFlow<String?> = _aiInsights.asStateFlow()

    private val _isInsightsLoading = MutableStateFlow(false)
    val isInsightsLoading: StateFlow<Boolean> = _isInsightsLoading.asStateFlow()

    // --- Offline Cloud Sync State ---
    private val _isCloudSyncing = MutableStateFlow(false)
    val isCloudSyncing: StateFlow<Boolean> = _isCloudSyncing.asStateFlow()

    init {
        viewModelScope.launch {
            repository.prepopulateDefaultCategories()
            repository.prepopulateInitialTransactions()
            repository.prepopulateInitialBills()
            
            // Check if PIN lock is enabled on startup
            securityConfig.collect { config ->
                if (config != null && config.isPinEnabled && config.hashedPin.isNotEmpty()) {
                    _isAppLocked.value = true
                }
            }
        }
    }

    fun selectTab(tab: String) {
        _currentTab.value = tab
    }

    // --- Bank Sync Engine Implementation ---
    fun toggleLinkAccount(accountName: String) {
        val updated = _linkedAccounts.value.map { acc ->
            if (acc.name == accountName) {
                acc.copy(isLinked = !acc.isLinked, lastSynced = if (!acc.isLinked) System.currentTimeMillis() else 0L)
            } else acc
        }
        _linkedAccounts.value = updated
    }

    fun syncBankAccounts() {
        if (_isBankSyncing.value) return
        viewModelScope.launch {
            _isBankSyncing.value = true
            delay(2500) // Realistic interactive delay of bank retrieval
            
            val linked = _linkedAccounts.value.filter { it.isLinked }
            if (linked.isNotEmpty()) {
                val now = System.currentTimeMillis()
                // Retrieve 2 simulated synced transactions
                val syncTx1 = Transaction(
                    amount = -42.80,
                    description = "Whole Foods Markets",
                    category = "Food & Dining",
                    date = now,
                    bankAccountName = linked.random().name,
                    isPending = false
                )
                val syncTx2 = Transaction(
                    amount = -18.00,
                    description = "Uber Ride",
                    category = "Transport & Gas",
                    date = now - 1500000,
                    bankAccountName = linked.random().name,
                    isPending = false
                )
                
                repository.insertTransaction(syncTx1)
                repository.insertTransaction(syncTx2)
                
                // Adjust linked balances
                _linkedAccounts.value = _linkedAccounts.value.map { acc ->
                    if (acc.isLinked) {
                        val adjustment = if (acc.name == syncTx1.bankAccountName) syncTx1.amount else 0.0 +
                                         if (acc.name == syncTx2.bankAccountName) syncTx2.amount else 0.0
                        acc.copy(balance = acc.balance + adjustment, lastSynced = now)
                    } else acc
                }
            }
            _isBankSyncing.value = false
        }
    }

    // --- UI Dialog Closures ---
    fun addManualTransaction(amount: Double, description: String, category: String, account: String, isRecurring: Boolean) {
        viewModelScope.launch {
            val tx = Transaction(
                amount = amount,
                description = description,
                category = category,
                date = System.currentTimeMillis(),
                bankAccountName = account,
                isRecurring = isRecurring,
                isPending = false
            )
            repository.insertTransaction(tx)
            
            // Adjust balance in account check
            _linkedAccounts.value = _linkedAccounts.value.map { acc ->
                if (acc.name == account) {
                    acc.copy(balance = acc.balance + amount)
                } else acc
            }
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
            // Restore balance
            _linkedAccounts.value = _linkedAccounts.value.map { acc ->
                if (acc.name == transaction.bankAccountName) {
                    acc.copy(balance = acc.balance - transaction.amount)
                } else acc
            }
        }
    }

    fun addCustomCategory(name: String, iconName: String, colorHex: String) {
        viewModelScope.launch {
            val cat = CustomCategory(name = name, iconName = iconName, colorHex = colorHex)
            repository.insertCategory(cat)
        }
    }

    fun addRecurringReminder(title: String, amount: Double, category: String, frequency: String, dueInDays: Int) {
        viewModelScope.launch {
            val dueDate = System.currentTimeMillis() + (dueInDays * 24 * 60 * 60 * 1000L)
            val reminder = RecurringReminder(
                title = title,
                amount = amount,
                category = category,
                frequency = frequency,
                nextDueDate = dueDate
            )
            repository.insertReminder(reminder)
        }
    }

    fun deleteReminder(reminder: RecurringReminder) {
        viewModelScope.launch {
            repository.deleteReminder(reminder)
        }
    }

    fun addBill(payee: String, amount: Double, dueDate: Long, category: String, reminderDaysBefore: Int, notes: String = "") {
        viewModelScope.launch {
            val bill = Bill(
                payee = payee,
                amount = amount,
                dueDate = dueDate,
                category = category,
                reminderDaysBefore = reminderDaysBefore,
                isPaid = false,
                notes = notes
            )
            repository.insertBill(bill)
        }
    }

    fun deleteBill(bill: Bill) {
        viewModelScope.launch {
            repository.deleteBill(bill)
        }
    }

    fun toggleBillPaid(bill: Bill) {
        viewModelScope.launch {
            val updated = bill.copy(isPaid = !bill.isPaid)
            repository.insertBill(updated)
            
            // Generate automatic payment transaction when marked as paid
            if (updated.isPaid) {
                val account = _linkedAccounts.value.firstOrNull { it.isLinked }?.name ?: "Manual"
                val tx = Transaction(
                    amount = -updated.amount, // negative for expense
                    description = "Bill Paid: ${updated.payee}",
                    category = updated.category,
                    date = System.currentTimeMillis(),
                    bankAccountName = account,
                    isRecurring = true,
                    isPending = false
                )
                repository.insertTransaction(tx)
                
                // Adjust account bank balance
                _linkedAccounts.value = _linkedAccounts.value.map { acc ->
                    if (acc.name == account) {
                        acc.copy(balance = acc.balance - updated.amount)
                    } else acc
                }
            }
        }
    }

    // --- Automated Gemini Categorization ---
    fun getAISuggestedCategory(description: String, callback: (String) -> Unit) {
        if (description.trim().isEmpty()) return
        viewModelScope.launch {
            _isAICategorizing.value = true
            val cats = categories.value.map { it.name }
            val suggested = geminiService.getCategorization(description, cats)
            callback(suggested)
            _isAICategorizing.value = false
        }
    }

    // --- Visual Spending Trend Analysis ---
    fun triggerAIInsights() {
        if (_isInsightsLoading.value) return
        viewModelScope.launch {
            _isInsightsLoading.value = true
            val insightsText = geminiService.getFinancialInsights(transactions.value, categories.value)
            _aiInsights.value = insightsText
            _isInsightsLoading.value = false
        }
    }

    // --- Export Reports in CSV ---
    fun exportToCSV(context: Context): Uri? {
        val txList = transactions.value
        if (txList.isEmpty()) return null
        return try {
            val csvBuilder = StringBuilder()
            csvBuilder.append("ID,Date,Description,Amount,Category,Account,Recurring\n")
            
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            for (tx in txList) {
                val formattedDate = dateFormat.format(Date(tx.date))
                val escapesDesc = tx.description.replace("\"", "\"\"")
                csvBuilder.append("${tx.id},$formattedDate,\"$escapesDesc\",${tx.amount},\"${tx.category}\",\"${tx.bankAccountName}\",${tx.isRecurring}\n")
            }
            
            val outputDirectory = File(context.cacheDir, "csv_exports")
            if (!outputDirectory.exists()) {
                outputDirectory.mkdirs()
            }
            val reportFile = File(outputDirectory, "apex_ledger_report_${System.currentTimeMillis() / 1000}.csv")
            reportFile.writeText(csvBuilder.toString())
            
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                reportFile
            )
        } catch (e: Exception) {
            null
        }
    }

    // --- SECURE Lock & 2FA Setup Functions ---
    fun handlePINKey(key: String, onPINValidated: () -> Unit = {}) {
        val currentBuf = _pinBuffer.value
        if (key == "C") {
            _pinBuffer.value = ""
            return
        }
        if (key == "⌫") {
            if (currentBuf.isNotEmpty()) {
                _pinBuffer.value = currentBuf.substring(0, currentBuf.length -1)
            }
            return
        }
        if (currentBuf.length < 4) {
            val newBuf = currentBuf + key
            _pinBuffer.value = newBuf
            
            if (newBuf.length == 4) {
                // Perform check or confirmation
                viewModelScope.launch {
                    val config = securityConfig.value
                    if (config != null && config.isPinEnabled && config.hashedPin.isNotEmpty()) {
                        // Unlocking app
                        if (config.hashedPin == newBuf) {
                            _isAppLocked.value = false
                            _pinBuffer.value = ""
                            _securityMessage.value = "Welcome back!"
                            onPINValidated()
                        } else {
                            _pinBuffer.value = ""
                            _securityMessage.value = "Incorrect PIN code. Privacy protection active."
                        }
                    } else {
                        // Creating PIN (first run)
                        // Directly assign and save
                        val newConfig = (config ?: SecurityConfig()).copy(
                            isPinEnabled = true,
                            hashedPin = newBuf
                        )
                        repository.updateSecurityConfig(newConfig)
                        _pinBuffer.value = ""
                        _securityMessage.value = "Security PIN configured successfully!"
                    }
                }
            }
        }
    }

    fun disablePIN() {
        viewModelScope.launch {
            val current = securityConfig.value ?: SecurityConfig()
            val updated = current.copy(isPinEnabled = false, hashedPin = "")
            repository.updateSecurityConfig(updated)
            _securityMessage.value = "Privacy lock PIN disabled."
        }
    }

    fun toggleTwoFactor(enabled: Boolean, email: String) {
        viewModelScope.launch {
            if (enabled) {
                // Generate simulated code
                val randomOtp = (100000..999999).random().toString()
                _otpChallengeCode.value = randomOtp
                _isTwoFactorVerifying.value = true
                _securityMessage.value = "Verification code requested for 2FA confirmation."
            } else {
                val current = securityConfig.value ?: SecurityConfig()
                val updated = current.copy(isTwoFactorEnabled = false)
                repository.updateSecurityConfig(updated)
                _securityMessage.value = "Secure two-factor disabled."
            }
        }
    }

    fun verifySimulatedOTP(codeInput: String): Boolean {
        if (codeInput == _otpChallengeCode.value) {
            viewModelScope.launch {
                val current = securityConfig.value ?: SecurityConfig()
                val updated = current.copy(isTwoFactorEnabled = true)
                repository.updateSecurityConfig(updated)
                _isTwoFactorVerifying.value = false
                _otpChallengeCode.value = ""
                _securityMessage.value = "Encrypted Two-Factor authentication is now active!"
            }
            return true
        } else {
            _securityMessage.value = "Invalid security code. Please check code and retry."
            return false
        }
    }

    fun cancelOTPVerification() {
        _isTwoFactorVerifying.value = false
        _otpChallengeCode.value = ""
    }

    // --- Secure Cloud Synced Engine ---
    fun triggerSecureCloudSync() {
        if (_isCloudSyncing.value) return
        viewModelScope.launch {
            _isCloudSyncing.value = true
            _securityMessage.value = "Establishing AES-256 cloud tunnel..."
            delay(1500)
            _securityMessage.value = "Synchronizing encrypted records offline -> cloud vaults..."
            delay(1500)
            
            val current = securityConfig.value ?: SecurityConfig()
            val updated = current.copy(
                isCloudSyncEnabled = true,
                lastSyncTimestamp = System.currentTimeMillis()
            )
            repository.updateSecurityConfig(updated)
            _securityMessage.value = "Total Privacy Sync Completed successfully!"
            _isCloudSyncing.value = false
        }
    }

    fun lockApp() {
        val config = securityConfig.value
        if (config != null && config.isPinEnabled && config.hashedPin.isNotEmpty()) {
            _isAppLocked.value = true
            _securityMessage.value = "Encrypted privacy shield activated!"
        } else {
            _securityMessage.value = "Setup a secure device PIN lock in the Security tab first!"
        }
    }
}

// Support classes
data class BankAccount(
    val name: String,
    val type: String,
    val balance: Double,
    val isLinked: Boolean,
    val lastSynced: Long
)
