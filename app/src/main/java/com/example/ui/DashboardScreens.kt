package com.example.ui

import android.content.Context
import android.content.Intent
import java.text.SimpleDateFormat
import java.text.NumberFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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
import com.example.data.CustomCategory
import com.example.data.RecurringReminder
import com.example.data.Transaction
import com.example.ui.theme.*
import java.util.*

@Composable
fun MainFinanceApp(viewModel: FinanceViewModel) {
    val isLocked by viewModel.isAppLocked.collectAsState()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (isLocked) {
            PINLockScreen(viewModel)
        } else {
            FinanceWorkspace(viewModel)
        }
    }
}

// --- SECURE PIN KEYBOARD LOCK OVERLAY ---
@Composable
fun PINLockScreen(viewModel: FinanceViewModel) {
    val pinBuffer by viewModel.pinBuffer.collectAsState()
    val securityMsg by viewModel.securityMessage.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground) // Lockscreen is always high privacy dark
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // App Logo Icon and Title
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 40.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(MintPrimary, MintTertiary)))
                    .drawBehind {
                        // Drawing decorative vaults grid
                        drawCircle(Color.White.copy(alpha = 0.15f), radius = 60f, center = Offset(40f, 40f))
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Security Active",
                    tint = DarkBackground,
                    modifier = Modifier.size(40.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "APEX PRIVACY LOCK",
                color = MintPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Text(
                text = "Cryptographic biometric & pin sandbox",
                color = Color.Gray,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }

        // Selected PIN display
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until 4) {
                    val active = i < pinBuffer.length
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .border(2.dp, MintPrimary, CircleShape)
                            .background(if (active) MintPrimary else Color.Transparent)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = securityMsg.ifEmpty { "Enter your 4-digit security PIN to unlock" },
                color = if (securityMsg.contains("Incorrect", ignoreCase = true)) ExpenseColor else Color.White,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }

        // Custom Numeric Keyboard Dial Matrix
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val rows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("C", "0", "⌫")
            )
            
            for (row in rows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    for (key in row) {
                        Button(
                            onClick = { viewModel.handlePINKey(key) },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .testTag("pin_key_$key"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (key == "C" || key == "⌫") Color.White.copy(0.04f) else Color.White.copy(0.08f),
                                contentColor = if (key == "C") ExpenseColor else if (key == "⌫") MintPrimary else Color.White
                            )
                        ) {
                            Text(
                                text = key,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- WORKSPACE AFTER PIN CODE VALIDATION ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceWorkspace(viewModel: FinanceViewModel) {
    val currentTab by viewModel.currentTab.collectAsState()
    val context = LocalContext.current
    
    // Quick notification banners
    val securityMsg by viewModel.securityMessage.collectAsState()
    val isBankSyncing by viewModel.isBankSyncing.collectAsState()
    val isCloudSyncing by viewModel.isCloudSyncing.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "V",
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                        Column {
                            Text(
                                text = "Vault",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFB2F2BB))
                                )
                                Text(
                                    text = "SECURE SYNC ACTIVE",
                                    fontSize = 9.sp,
                                    color = Color(0xFFC9C5D0),
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                },
                actions = {
                    // Sync Badges & Quick CSV Exporter
                    if (isBankSyncing || isCloudSyncing) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    
                    IconButton(
                        onClick = {
                            val uri = viewModel.exportToCSV(context)
                            if (uri != null) {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/csv"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share CSV Financial Report"))
                            }
                        },
                        modifier = Modifier
                            .testTag("export_csv_btn")
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Export CSV Data",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = { viewModel.lockApp() },
                        modifier = Modifier
                            .testTag("lock_app_btn")
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Lock Device Securely",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            val navItemColors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF21005D),
                selectedTextColor = MaterialTheme.colorScheme.onBackground,
                indicatorColor = Color(0xFFEADDFF),
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            )

            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.border(
                    width = 0.5.dp, 
                    color = MaterialTheme.colorScheme.surfaceVariant, 
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                )
            ) {
                NavigationBarItem(
                    selected = currentTab == "Dashboard",
                    onClick = { viewModel.selectTab("Dashboard") },
                    icon = { Icon(Icons.Default.Home, "Overview") },
                    label = { Text("Dash", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    colors = navItemColors,
                    modifier = Modifier.testTag("tab_dashboard")
                )
                NavigationBarItem(
                    selected = currentTab == "Transactions",
                    onClick = { viewModel.selectTab("Transactions") },
                    icon = { Icon(Icons.Default.List, "Ledger") },
                    label = { Text("Ledger", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    colors = navItemColors,
                    modifier = Modifier.testTag("tab_transactions")
                )
                NavigationBarItem(
                    selected = currentTab == "Reminders",
                    onClick = { viewModel.selectTab("Reminders") },
                    icon = { Icon(Icons.Default.Refresh, "Reminders") },
                    label = { Text("Reminders", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    colors = navItemColors,
                    modifier = Modifier.testTag("tab_reminders")
                )
                NavigationBarItem(
                    selected = currentTab == "Security",
                    onClick = { viewModel.selectTab("Security") },
                    icon = { Icon(Icons.Default.Lock, "Settings") },
                    label = { Text("Security", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    colors = navItemColors,
                    modifier = Modifier.testTag("tab_security")
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Success/Notification Toast Banner
            if (securityMsg.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.handlePINKey("C") } // custom clear banner
                ) {
                    Text(
                        text = securityMsg,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 16.dp)
                    )
                }
            }

            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                },
                label = "TabTransitions"
            ) { targetTab ->
                when (targetTab) {
                    "Dashboard" -> DashboardScreen(viewModel)
                    "Transactions" -> TransactionsScreen(viewModel)
                    "Reminders" -> RemindersScreen(viewModel)
                    "Security" -> SecurityScreen(viewModel)
                }
            }
        }
    }
}

// ==================== SCREEN 1: DASHBOARD OVERVIEW ====================
@Composable
fun DashboardScreen(viewModel: FinanceViewModel) {
    val transactionList by viewModel.transactions.collectAsState()
    val accountsList by viewModel.linkedAccounts.collectAsState()
    val categoriesList by viewModel.categories.collectAsState()
    val isBankSyncing by viewModel.isBankSyncing.collectAsState()
    val billsList by viewModel.bills.collectAsState()
    
    // Derive summary details
    val totalIncome = transactionList.filter { it.amount > 0 }.sumOf { it.amount }
    val totalExpense = transactionList.filter { it.amount < 0 }.sumOf { it.amount }
    val netCashFlow = totalIncome + totalExpense
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
    ) {
        
        // Active automated reminder system for unpaid bills approaching due date
        val unpaidBills = billsList.filter { !it.isPaid }
        val now = System.currentTimeMillis()
        val dueSoonBills = unpaidBills.filter { bill ->
            val daysLeft = ((bill.dueDate - now) / (24 * 3600 * 1000L)).toInt()
            daysLeft in 0..bill.reminderDaysBefore
        }
        val overdueBills = unpaidBills.filter { bill -> bill.dueDate < now && ((now - bill.dueDate) / (24 * 3600 * 1000L)).toInt() >= 1 }

        if (dueSoonBills.isNotEmpty() || overdueBills.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, ExpenseColor.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(
                        containerColor = ExpenseColor.copy(alpha = 0.08f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(ExpenseColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Dues Alert",
                                tint = ExpenseColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(10.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (overdueBills.isNotEmpty()) "Bill Payment Overdue!" else "Upcoming Bill Reminders",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            val descText = when {
                                overdueBills.isNotEmpty() && dueSoonBills.isNotEmpty() -> 
                                    "${overdueBills.size} bills overdue, ${dueSoonBills.size} due soon."
                                overdueBills.isNotEmpty() -> 
                                    "${overdueBills.first().payee} (${formatCurrency(overdueBills.first().amount)}) was due on ${SimpleDateFormat("MMM d", Locale.US).format(Date(overdueBills.first().dueDate))}."
                                else -> 
                                    "${dueSoonBills.first().payee} (${formatCurrency(dueSoonBills.first().amount)}) is due in ${((dueSoonBills.first().dueDate - now) / (24 * 3600 * 1000L)).toInt()} days."
                            }
                            Text(
                                text = descText,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
                            )
                        }
                        
                        TextButton(
                            onClick = { viewModel.selectTab("Reminders") }
                        ) {
                            Text(
                                text = "View",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
        
        // Dynamic Quick Card Balance Details / Polish Vault Card
        item {
            val linked = accountsList.filter { it.isLinked }
            val trendPct = if (totalIncome > 0.0) {
                String.format(Locale.US, "%+.1f%%", (netCashFlow / totalIncome) * 100)
            } else {
                "+0.0%"
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Total Balance",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Medium
                    )

                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = formatCurrency(netCashFlow),
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            letterSpacing = (-1).sp
                        )
                        
                        Box(
                            modifier = Modifier
                                .padding(bottom = 6.dp)
                                .clip(RoundedCornerShape(100.dp))
                                .background(MaterialTheme.colorScheme.onPrimaryContainer)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = trendPct,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: visual initials outline stack
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy((-8).dp),
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                if (linked.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .border(1.5.dp, MaterialTheme.colorScheme.primaryContainer, CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Ø",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                } else {
                                    linked.take(3).forEach { acc ->
                                        val initial = acc.name.split(" ")
                                            .filter { it.isNotEmpty() }
                                            .joinToString("") { it.take(1) }
                                            .take(2)
                                            .uppercase()
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .border(1.5.dp, MaterialTheme.colorScheme.primaryContainer, CircleShape)
                                                .background(MaterialTheme.colorScheme.surfaceVariant),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = initial,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                            Text(
                                text = "${linked.size} Accounts Synced",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Right: Small details showing net inflow details
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "In: " + formatCurrency(totalIncome),
                                color = IncomeColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Out: " + formatCurrency(Math.abs(totalExpense)),
                                color = ExpenseColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
        
        // Monthly Budget Tracker Card matching HTML pattern
        item {
            val budgetLimit = 2500.0
            val spentAmount = Math.abs(totalExpense)
            val pctPlayed = (spentAmount / budgetLimit).coerceIn(0.0, 1.0)
            val pctString = String.format(Locale.US, "%.0f%%", pctPlayed * 100)
            
            // Days remaining in month
            val calendar = Calendar.getInstance()
            val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            val currDay = calendar.get(Calendar.DAY_OF_MONTH)
            val daysRemaining = (maxDay - currDay).coerceAtLeast(1)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .border(width = 1.dp, color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(28.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Monthly Budget",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = formatCurrency(spentAmount) + " / " + formatCurrency(budgetLimit),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Progress Track Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(100.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(pctPlayed.toFloat())
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(100.dp))
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Spent " + pctString,
                            fontSize = 11.sp,
                            color = Color(0xFFC9C5D0),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "$daysRemaining Days Remaining",
                            fontSize = 11.sp,
                            color = Color(0xFFC9C5D0),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Custom Bezier Trend Graph Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .border(width = 1.dp, color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(28.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Monthly Budget Spending Trends",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Aggregated visual timeline based on local Room logs",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    // Bezier Drawing Canvas
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                    ) {
                        TrendBezierGraph(transactions = transactionList)
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Day 1 (Start)", fontSize = 9.sp, color = Color.Gray)
                        Text("Timeline Distribution", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Text("Day 30 (End)", fontSize = 9.sp, color = Color.Gray)
                    }
                }
            }
        }

        // Category Budget Proportion Rings
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .border(width = 1.dp, color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(28.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1.3f)) {
                        Text(
                            text = "Custom Category Matrix",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Local composition index",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        // Categories proportional breakdown
                        val activeSpentCats = transactionList.filter { it.amount < 0 }
                        val sumSpendGlobal = activeSpentCats.sumOf { Math.abs(it.amount) }
                        
                        if (sumSpendGlobal == 0.0) {
                            Text("No outflow recorded to calculate relative shares.", fontSize = 11.sp, color = Color.Gray)
                        } else {
                            val categoryAggregations = activeSpentCats
                                .groupBy { it.category }
                                .mapValues { entry -> entry.value.sumOf { Math.abs(it.amount) } }
                                .toList()
                                .sortedByDescending { it.second }
                                .take(3)
                            
                            categoryAggregations.forEach { (catName, amt) ->
                                val pct = (amt / sumSpendGlobal * 100).toInt()
                                val catConfig = categoriesList.find { it.name == catName }
                                val color = Color(android.graphics.Color.parseColor(catConfig?.colorHex ?: "#94A3B8"))
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 3.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "$catName ($pct%)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // Interactive Custom ring drawing
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CategoryDonutChart(transactions = transactionList, categories = categoriesList)
                    }
                }
            }
        }
        
        // TWO-WAY SYNCHRONIZATION WITH BANK ACCOUNTS
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .border(width = 1.dp, color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(28.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Linked Bank Accounts",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Encrypt local sandbox feeds",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                        
                        Button(
                            onClick = { viewModel.syncBankAccounts() },
                            enabled = !isBankSyncing,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(0.12f),
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("bank_sync_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isBankSyncing) "Syncing..." else "Sync", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    accountsList.forEach { acc ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (acc.isLinked) MaterialTheme.colorScheme.primary.copy(0.04f) else MaterialTheme.colorScheme.surfaceVariant.copy(0.2f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1.5f)) {
                                    Text(
                                        text = acc.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "${acc.type} • ${if (acc.isLinked) "Synced Securely" else "Link Pending"}",
                                        fontSize = 11.sp,
                                        color = if (acc.isLinked) MaterialTheme.colorScheme.primary else Color.Gray
                                    )
                                }
                                
                                Text(
                                    text = formatCurrency(acc.balance),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    color = if (acc.balance >= 0) IncomeColor else ExpenseColor
                                )
                                
                                Switch(
                                    checked = acc.isLinked,
                                    onCheckedChange = { viewModel.toggleLinkAccount(acc.name) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primary.copy(0.3f)
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // AI FINANCIER COACH INSIGHTS GATED SEGMENT
        item {
            val insightsText by viewModel.aiInsights.collectAsState()
            val isInsightsLoading by viewModel.isInsightsLoading.collectAsState()

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .border(width = 1.dp, color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(28.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(MintSecondary)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "AI Financial Coach",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "Secure, confidential budgets auditing",
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }

                        Button(
                            onClick = { viewModel.triggerAIInsights() },
                            enabled = !isInsightsLoading,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("ai_insights_btn")
                        ) {
                            if (isInsightsLoading) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                            } else {
                                Text("Ask Coach", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (insightsText != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = insightsText!!,
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==================== SCREEN 2: TRANSACTIONS LIST LEDGER ====================
@Composable
fun TransactionsScreen(viewModel: FinanceViewModel) {
    val transactionList by viewModel.transactions.collectAsState()
    val categoriesList by viewModel.categories.collectAsState()
    val accountsList by viewModel.linkedAccounts.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }
    
    // Filtered ledger rows
    val filteredTx = if (searchQuery.trim().isEmpty()) {
        transactionList
    } else {
        transactionList.filter {
            it.description.contains(searchQuery, ignoreCase = true) ||
            it.category.contains(searchQuery, ignoreCase = true)
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search transactions...", fontSize = 13.sp) },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("tx_search_bar"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .size(50.dp)
                    .testTag("add_tx_fab"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Add, "Add Transaction")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (filteredTx.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No transactions logged yet.", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Text("Use the Floating Action Button, or link systems above.", fontSize = 11.sp, color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(filteredTx) { tx ->
                    val isExpense = tx.amount < 0
                    val config = categoriesList.find { it.name == tx.category }
                    val colorHex = config?.colorHex ?: "#94A3B8"
                    val parsedColor = Color(android.graphics.Color.parseColor(colorHex))
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { }
                            .testTag("tx_card_${tx.id}"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Category Bubble Circle
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(parsedColor.copy(alpha = 0.15f))
                                    .align(Alignment.CenterVertically),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = getCleanCategoryEmoji(tx.category),
                                    fontSize = 18.sp
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tx.description,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(tx.category, fontSize = 11.sp, color = parsedColor, fontWeight = FontWeight.Bold)
                                    Text(" • ", fontSize = 11.sp, color = Color.Gray)
                                    Text(tx.bankAccountName, fontSize = 11.sp, color = Color.Gray)
                                }
                            }
                            
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = (if (!isExpense) "+" else "") + formatCurrency(tx.amount),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    color = if (isExpense) ExpenseColor else IncomeColor
                                )
                                Text(
                                    text = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(tx.date)),
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            IconButton(
                                onClick = { viewModel.deleteTransaction(tx) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Remove Transaction",
                                    tint = ExpenseColor.copy(0.7f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    // --- DIALOG: ADD TRANSACTION MANUAL + AI WRAPPERS ---
    if (showDialog) {
        var descInput by remember { mutableStateOf("") }
        var amountInput by remember { mutableStateOf("") }
        var isInvoice by remember { mutableStateOf(false) } // Expense or Income
        var selectedCategory by remember { mutableStateOf(categoriesList.firstOrNull()?.name ?: "Food & Dining") }
        var selectedAccount by remember { mutableStateOf(accountsList.firstOrNull() { it.isLinked }?.name ?: "Manual Cash") }
        var autoRecur by remember { mutableStateOf(false) }
        val isCategorizing by viewModel.isAICategorizing.collectAsState()
        
        Dialog(onDismissRequest = { showDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .border(width = 1.dp, color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(28.dp)),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Add Transaction",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    
                    // Expense vs Inflow selector
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { isInvoice = false },
                            modifier = Modifier.weight(1f).padding(end = 4.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!isInvoice) ExpenseColor else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (!isInvoice) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Text("Expense", fontSize = 12.sp)
                        }
                        
                        Button(
                            onClick = { isInvoice = true },
                            modifier = Modifier.weight(1f).padding(start = 4.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isInvoice) IncomeColor else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isInvoice) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Text("Inflow", fontSize = 12.sp)
                        }
                    }
                    
                    // Description Input with Gemini Predictive Tag
                    Text("Description", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = descInput,
                            onValueChange = { descInput = it },
                            placeholder = { Text("Starbucks, Payroll Deposit...") },
                            modifier = Modifier.weight(1f).testTag("add_tx_desc"),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                        
                        Spacer(modifier = Modifier.width(6.dp))
                        
                        // Suggest category with Gemini AI
                        IconButton(
                            onClick = {
                                viewModel.getAISuggestedCategory(descInput) { result ->
                                    selectedCategory = result
                                }
                            },
                            enabled = descInput.isNotBlank() && !isCategorizing,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        ) {
                            if (isCategorizing) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "AI Suggest category",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Amount ($)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            OutlinedTextField(
                                value = amountInput,
                                onValueChange = { amountInput = it },
                                placeholder = { Text("0.00") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true,
                                modifier = Modifier.testTag("add_tx_amount")
                            )
                        }
                        
                        Column(modifier = Modifier.weight(1.2f)) {
                            Text("Category", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            // Clean Selection List fallback simulated simple drop selector
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(0.1f))
                                    .clickable {
                                        // Spin cycle categories list to make manual pick
                                        val idx = categoriesList.indexOfFirst { it.name == selectedCategory }
                                        val nxt = if (idx == -1 || idx == categoriesList.size - 1) 0 else idx + 1
                                        selectedCategory = categoriesList[nxt].name
                                    },
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = getCleanCategoryEmoji(selectedCategory) + " " + selectedCategory,
                                    modifier = Modifier.padding(start = 12.dp),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1.2f)) {
                            Text("Bank Account", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        val available = accountsList.filter { it.isLinked }.map { it.name } + listOf("Manual Cash")
                                        val idx = available.indexOf(selectedAccount)
                                        val nxt = if (idx == -1 || idx == available.size - 1) 0 else idx + 1
                                        selectedAccount = available[nxt]
                                    },
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(selectedAccount, modifier = Modifier.padding(start = 12.dp), fontSize = 13.sp)
                            }
                        }
                        
                        Column(
                            modifier = Modifier.weight(0.8f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("Is Recurring", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            Spacer(modifier = Modifier.height(6.dp))
                            Checkbox(
                                checked = autoRecur,
                                onCheckedChange = { autoRecur = it }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showDialog = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Cancel")
                        }
                        
                        Button(
                            onClick = {
                                val cleanAmt = amountInput.toDoubleOrNull() ?: 0.0
                                val finalizedAmount = if (isInvoice) cleanAmt else -cleanAmt
                                if (descInput.isNotBlank() && cleanAmt > 0.0) {
                                    viewModel.addManualTransaction(
                                        finalizedAmount,
                                        descInput,
                                        selectedCategory,
                                        selectedAccount,
                                        autoRecur
                                    )
                                    showDialog = false
                                }
                            },
                            modifier = Modifier.weight(1f).testTag("save_tx_btn"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Save Ledger")
                        }
                    }
                }
            }
        }
    }
}

// ==================== SCREEN 3: RECURRING TRANSACTIONS / REMINDERS ====================
@Composable
fun RemindersScreen(viewModel: FinanceViewModel) {
    val remindersList by viewModel.reminders.collectAsState()
    val categoriesList by viewModel.categories.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Recurring Bill Reminders",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Track upcoming recurring transactions offline",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
            
            Button(
                onClick = { showAddDialog = true },
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                modifier = Modifier.testTag("add_reminder_btn")
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (remindersList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(50.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("No bills or remittances configured.", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Text("Configured dues project monthly totals seamlessly.", fontSize = 11.sp, color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(remindersList) { reminder ->
                    val config = categoriesList.find { it.name == reminder.category }
                    val colorHex = config?.colorHex ?: "#94A3B8"
                    val parsedColor = Color(android.graphics.Color.parseColor(colorHex))
                    
                    // Days left calculator
                    val daysLeft = ((reminder.nextDueDate - System.currentTimeMillis()) / (24 * 3600 * 1000L)).toInt()
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reminder_card_${reminder.id}"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (daysLeft <= 3) MaterialTheme.colorScheme.primaryContainer.copy(0.1f) else MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(parsedColor.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(getCleanCategoryEmoji(reminder.category), fontSize = 16.sp)
                            }
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = reminder.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "${reminder.frequency} • ${reminder.category}",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                            
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = formatCurrency(reminder.amount),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (daysLeft <= 3) ExpenseColor.copy(0.12f) else MaterialTheme.colorScheme.primary.copy(0.1f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (daysLeft <= 0) "Due Today" else "In $daysLeft days",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (daysLeft <= 3) ExpenseColor else MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            IconButton(onClick = { viewModel.deleteReminder(reminder) }) {
                                Icon(Icons.Default.Delete, "Remove Reminder", tint = ExpenseColor.copy(0.7f), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
    
    // --- DIALOG: ADD RECURRING REMINDER ---
    if (showAddDialog) {
        var title by remember { mutableStateOf("") }
        var amount by remember { mutableStateOf("") }
        var category by remember { mutableStateOf(categoriesList.firstOrNull()?.name ?: "Food & Dining") }
        var frequency by remember { mutableStateOf("Monthly") }
        var dueDays by remember { mutableStateOf("5") }
        
        Dialog(onDismissRequest = { showAddDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .border(width = 1.dp, color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(28.dp)),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Add Bill Reminder",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Bill Description / Title") },
                        modifier = Modifier.fillMaxWidth().testTag("add_reminder_title"),
                        singleLine = true
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = amount,
                            onValueChange = { amount = it },
                            label = { Text("Amount ($)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).testTag("add_reminder_amount"),
                            singleLine = true
                        )
                        
                        OutlinedTextField(
                            value = dueDays,
                            onValueChange = { dueDays = it },
                            label = { Text("Due in (Days)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Category", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        val idx = categoriesList.indexOfFirst { it.name == category }
                                        val nxt = if (idx == -1 || idx == categoriesList.size - 1) 0 else idx + 1
                                        category = categoriesList[nxt].name
                                    },
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(category, modifier = Modifier.padding(start = 12.dp), fontSize = 13.sp)
                            }
                        }
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Frequency", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        val freqs = listOf("Weekly", "Monthly", "Yearly")
                                        val idx = freqs.indexOf(frequency)
                                        val nxt = if (idx == -1 || idx == freqs.size - 1) 0 else idx + 1
                                        frequency = freqs[nxt]
                                    },
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(frequency, modifier = Modifier.padding(start = 12.dp), fontSize = 13.sp)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showAddDialog = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                val amtNum = amount.toDoubleOrNull() ?: 0.0
                                val dueDaysNum = dueDays.toIntOrNull() ?: 5
                                if (title.isNotBlank() && amtNum > 0.0) {
                                    viewModel.addRecurringReminder(
                                        title,
                                        amtNum,
                                        category,
                                        frequency,
                                        dueDaysNum
                                    )
                                    showAddDialog = false
                                }
                            },
                            modifier = Modifier.weight(1f).testTag("save_reminder_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Save Reminder")
                        }
                    }
                }
            }
        }
    }
}

// ==================== SCREEN 4: PRIVACY, 2FA LOCK, CLOUD SYNC ====================
@Composable
fun SecurityScreen(viewModel: FinanceViewModel) {
    val securityConfig by viewModel.securityConfig.collectAsState()
    val isCloudSyncing by viewModel.isCloudSyncing.collectAsState()
    val is2FAVerifying by viewModel.isTwoFactorVerifying.collectAsState()
    
    var emailInput by remember { mutableStateOf("") }
    var showPinSetup by remember { mutableStateOf(false) }
    var otpInput by remember { mutableStateOf("") }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
        
        // PRIVACY STATEMENT BANNER
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(0.04f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Zero-Knowledge Local Storage",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "All ledger files, keys, and details are kept in your localized encrypted database context. Total Privacy Assured.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }
        
        // MASTER PIN PRIVACY SHIELD SETTING
        item {
            val isPinActive = securityConfig?.isPinEnabled == true
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Master PIN Authentication Lock",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Secures launching the tracker with a mandatory localized padlock",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isPinActive) "PADLOCK ENABLED" else "UNPROTECTED SECURED",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isPinActive) IncomeColor else ExpenseColor
                        )
                        
                        Button(
                            onClick = {
                                if (isPinActive) {
                                    viewModel.disablePIN()
                                } else {
                                    showPinSetup = true
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("toggle_pin_lock_btn")
                        ) {
                            Text(if (isPinActive) "Disable PIN" else "Set Security PIN", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
        
        // TWO-FACTOR AUTHENTICATION SETUP (DATA PROTECTION)
        item {
            val is2fActive = securityConfig?.isTwoFactorEnabled == true
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Encrypted Two-Factor OTP Gateway",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Require visual OTP codes when requesting external cloud synchronizations",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    if (!is2fActive && !is2FAVerifying) {
                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = { emailInput = it },
                            placeholder = { Text("Enter account email address") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("tfa_email_field"),
                            shape = RoundedCornerShape(10.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                        )
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        Button(
                            onClick = { viewModel.toggleTwoFactor(true, emailInput) },
                            enabled = emailInput.isNotBlank() && emailInput.contains("@"),
                            modifier = Modifier.align(Alignment.End).testTag("request_2fa_btn"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Enable Two-Factor", fontSize = 12.sp)
                        }
                    } else if (is2FAVerifying) {
                        // OTP verification form
                        val challengeCode by viewModel.otpChallengeCode.collectAsState()
                        
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "SIMULATED EMAIL INBOX MESSAGE:\nUse OTP code $challengeCode to confirm 2FA setup.",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        OutlinedTextField(
                            value = otpInput,
                            onValueChange = { otpInput = it },
                            placeholder = { Text("Enter 6-digit OTP code") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("tfa_otp_field"),
                            shape = RoundedCornerShape(10.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { viewModel.cancelOTPVerification() }) {
                                Text("Cancel")
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Button(
                                onClick = {
                                    val success = viewModel.verifySimulatedOTP(otpInput)
                                    if (success) {
                                        otpInput = ""
                                        emailInput = ""
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("verify_otp_btn")
                            ) {
                                Text("Verify Code")
                            }
                        }
                    } else {
                        // 2FA is active
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Check, "Active", tint = IncomeColor)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("2FA Gateway Configured!", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = IncomeColor)
                            }
                            
                            TextButton(
                                onClick = { viewModel.toggleTwoFactor(false, "") },
                                colors = ButtonDefaults.textButtonColors(contentColor = ExpenseColor),
                                modifier = Modifier.testTag("disable_2fa_btn")
                            ) {
                                Text("Revoke 2FA")
                            }
                        }
                    }
                }
            }
        }
        
        // SECURE CLOUD SYNCHRONIZATION (OFFLINE-FIRST)
        item {
            val isCloudActive = securityConfig?.isCloudSyncEnabled == true
            val is2faEnabled = securityConfig?.isTwoFactorEnabled == true
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Secure Cloud Synchronization",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Uploads localized SQLite data into an encrypted vault (Requires 2FA if active)",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isCloudActive) "Last Synced" else "No Cloud Backups",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                            if (isCloudActive) {
                                val lastSync = securityConfig?.lastSyncTimestamp ?: 0L
                                Text(
                                    text = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(lastSync)),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        
                        Button(
                            onClick = { viewModel.triggerSecureCloudSync() },
                            enabled = !isCloudSyncing,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("cloud_sync_now_btn")
                        ) {
                            if (isCloudSyncing) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(12.dp))
                            } else {
                                Text("Sync Ledger Cloud", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
    
    // --- PIN LOCK CONFIG DESIGN SELECTOR POPUP ---
    if (showPinSetup) {
        var pin1 by remember { mutableStateOf("") }
        
        Dialog(onDismissRequest = { showPinSetup = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .border(width = 1.dp, color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(28.dp)),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Set Security Padlock PIN",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Please enter 4 digits to lock your local database access.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    
                    OutlinedTextField(
                        value = pin1,
                        onValueChange = { if (it.length <= 4) pin1 = it },
                        placeholder = { Text("4 Digits Code") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("setup_pin_field"),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showPinSetup = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                        
                        Button(
                            onClick = {
                                if (pin1.length == 4) {
                                    // Submit digits one by one to simulate matrix entries
                                    pin1.forEach { char ->
                                        viewModel.handlePINKey(char.toString())
                                    }
                                    showPinSetup = false
                                }
                            },
                            enabled = pin1.length == 4,
                            modifier = Modifier.weight(1f).testTag("confirm_pin_btn")
                        ) {
                            Text("Confirm PIN")
                        }
                    }
                }
            }
        }
    }
}


// ==================== DRAWING: monthly budget Bezier Curve ====================
@Composable
fun TrendBezierGraph(transactions: List<Transaction>) {
    val strokeColor = MaterialTheme.colorScheme.primary
    val isLight = !isSystemInDarkTheme()
    
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp)
    ) {
        val width = size.width
        val height = size.height
        
        // Draw helpful gridlines
        val gridLines = 4
        for (i in 0..gridLines) {
            val y = (height / gridLines) * i
            drawLine(
                color = if (isLight) Color(0xFFE2E8F0) else Color(0xFF1E293B),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1f
            )
        }
        
        if (transactions.isEmpty()) return@Canvas
        
        // Sort and map values around coordinates
        val points = transactions.sortedBy { it.date }
        val expensesOnly = points.filter { it.amount < 0 }
        
        if (expensesOnly.size < 2) {
            // Draw simple flat coordinate path with some style
            val path = Path().apply {
                moveTo(0f, height * 0.7f)
                lineTo(width, height * 0.3f)
            }
            drawPath(path, color = strokeColor, style = Stroke(width = 6f))
            return@Canvas
        }
        
        val maxExpenseVal = expensesOnly.maxOf { Math.abs(it.amount) }.toFloat()
        val minExpenseVal = expensesOnly.minOf { Math.abs(it.amount) }.toFloat()
        val deltaExpense = (maxExpenseVal - minExpenseVal).coerceAtLeast(1f)
        
        val stepX = width / (expensesOnly.size - 1)
        val pathOfBezier = Path()
        
        expensesOnly.forEachIndexed { idx, tx ->
            val currVal = Math.abs(tx.amount).toFloat()
            // Map into bounds height
            val fractionY = (currVal - minExpenseVal) / deltaExpense
            val targetY = height - (fractionY * (height * 0.7f) + (height * 0.15f))
            val targetX = idx * stepX
            
            if (idx == 0) {
                pathOfBezier.moveTo(targetX, targetY)
            } else {
                val prevVal = Math.abs(expensesOnly[idx - 1].amount).toFloat()
                val prevFracY = (prevVal - minExpenseVal) / deltaExpense
                val prevY = height - (prevFracY * (height * 0.7f) + (height * 0.15f))
                val prevX = (idx - 1) * stepX
                
                // Control handles for smooth bezier curve
                val controlX1 = prevX + (stepX / 2f)
                val controlY1 = prevY
                val controlX2 = prevX + (stepX / 2f)
                val controlY2 = targetY
                
                pathOfBezier.cubicTo(controlX1, controlY1, controlX2, controlY2, targetX, targetY)
            }
        }
        
        drawPath(
            path = pathOfBezier,
            color = strokeColor,
            style = Stroke(width = 5f)
        )
        
        // Fill area below trend with dynamic visual gradient
        val fillOfPath = Path().apply {
            addPath(pathOfBezier)
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }
        
        drawPath(
            path = fillOfPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    strokeColor.copy(alpha = 0.25f),
                    Color.Transparent
                )
            )
        )
    }
}


// ==================== DRAWING: Category Proportions Donut Chart ====================
@Composable
fun CategoryDonutChart(transactions: List<Transaction>, categories: List<CustomCategory>) {
    val spentRecords = transactions.filter { it.amount < 0 }
    val totalExpenseSum = spentRecords.sumOf { Math.abs(it.amount) }
    
    val isLight = !isSystemInDarkTheme()
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        val sizeSide = Math.min(size.width, size.height)
        val rectBounds = Size(sizeSide, sizeSide)
        val offsetCenter = Offset((size.width - sizeSide) / 2f, (size.height - sizeSide) / 2f)
        
        if (totalExpenseSum == 0.0) {
            drawArc(
                color = if (isLight) Color(0xFFE2E8F0) else Color(0xFF334155),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = 24f),
                topLeft = offsetCenter,
                size = rectBounds
            )
            return@Canvas
        }
        
        val aggregations = spentRecords
            .groupBy { it.category }
            .mapValues { Math.abs(it.value.sumOf { tx -> tx.amount }) }
        
        var cumulativeAngle = -90f
        aggregations.forEach { (catName, amt) ->
            val ratio = (amt / totalExpenseSum).toFloat()
            val sweepAngle = ratio * 360f
            
            val catMeta = categories.find { it.name == catName }
            val colorHex = catMeta?.colorHex ?: "#94A3B8"
            val parsedColor = Color(android.graphics.Color.parseColor(colorHex))
            
            drawArc(
                color = parsedColor,
                startAngle = cumulativeAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = 24f),
                topLeft = offsetCenter,
                size = rectBounds
            )
            cumulativeAngle += sweepAngle
        }
    }
}


// --- HELPERS & COMPATIBILITY LAYER ---
fun formatCurrency(amount: Double): String {
    val cleaner = amount.coerceIn(-1_000_000.0, 1_000_000.0)
    val formatter = NumberFormat.getCurrencyInstance(Locale.US)
    return formatter.format(cleaner)
}

fun getCleanCategoryEmoji(catName: String): String {
    return when {
        catName.contains("Food", ignoreCase = true) -> "🍔"
        catName.contains("Rent", ignoreCase = true) -> "🏠"
        catName.contains("Transport", ignoreCase = true) -> "🚗"
        catName.contains("Utility", ignoreCase = true) -> "💡"
        catName.contains("Entertainment", ignoreCase = true) -> "🎮"
        catName.contains("Shopping", ignoreCase = true) -> "🛍️"
        catName.contains("Healthcare", ignoreCase = true) -> "🩺"
        catName.contains("Income", ignoreCase = true) -> "💵"
        else -> "📝"
    }
}
