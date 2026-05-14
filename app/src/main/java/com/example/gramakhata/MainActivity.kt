
package com.example.gramakhata

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

data class Customer(
    val id: Long,
    val name: String,
    val phone: String,
    val village: String,
    val note: String
)

data class KhataTransaction(
    val id: Long,
    val customerId: Long,
    val amount: Double,
    val type: String,
    val note: String,
    val date: Long
)

class KhataStore(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("grama_khata_store", Context.MODE_PRIVATE)

    fun loadCustomers(): List<Customer> {
        val arr = JSONArray(prefs.getString("customers", "[]") ?: "[]")
        val list = mutableListOf<Customer>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(
                Customer(
                    id = o.optLong("id"),
                    name = o.optString("name"),
                    phone = o.optString("phone"),
                    village = o.optString("village"),
                    note = o.optString("note")
                )
            )
        }
        return list
    }

    fun loadTransactions(): List<KhataTransaction> {
        val arr = JSONArray(prefs.getString("transactions", "[]") ?: "[]")
        val list = mutableListOf<KhataTransaction>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(
                KhataTransaction(
                    id = o.optLong("id"),
                    customerId = o.optLong("customerId"),
                    amount = o.optDouble("amount"),
                    type = o.optString("type"),
                    note = o.optString("note"),
                    date = o.optLong("date")
                )
            )
        }
        return list
    }

    fun saveCustomers(customers: List<Customer>) {
        val arr = JSONArray()
        customers.forEach {
            arr.put(JSONObject().apply {
                put("id", it.id)
                put("name", it.name)
                put("phone", it.phone)
                put("village", it.village)
                put("note", it.note)
            })
        }
        prefs.edit().putString("customers", arr.toString()).apply()
    }

    fun saveTransactions(transactions: List<KhataTransaction>) {
        val arr = JSONArray()
        transactions.forEach {
            arr.put(JSONObject().apply {
                put("id", it.id)
                put("customerId", it.customerId)
                put("amount", it.amount)
                put("type", it.type)
                put("note", it.note)
                put("date", it.date)
            })
        }
        prefs.edit().putString("transactions", arr.toString()).apply()
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GramaKhataApp()
        }
    }
}

@Composable
fun GramaKhataApp() {
    val context = LocalContext.current
    val store = remember { KhataStore(context) }

    var customers by remember { mutableStateOf(store.loadCustomers()) }
    var transactions by remember { mutableStateOf(store.loadTransactions()) }
    var selectedCustomerId by rememberSaveable { mutableStateOf<Long?>(null) }
    var darkMode by rememberSaveable { mutableStateOf(false) }

    fun saveAll(newCustomers: List<Customer> = customers, newTransactions: List<KhataTransaction> = transactions) {
        customers = newCustomers
        transactions = newTransactions
        store.saveCustomers(newCustomers)
        store.saveTransactions(newTransactions)
    }

    MaterialTheme(
        colorScheme = if (darkMode) darkColorScheme(
            primary = Color(0xFF2DD4BF),
            secondary = Color(0xFF38BDF8),
            background = Color(0xFF0F172A),
            surface = Color(0xFF111827)
        ) else lightColorScheme(
            primary = Color(0xFF0F766E),
            secondary = Color(0xFF2563EB),
            background = Color(0xFFF8FAFC),
            surface = Color.White
        )
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            val selected = customers.firstOrNull { it.id == selectedCustomerId }

            if (selected == null) {
                HomeScreen(
                    customers = customers,
                    transactions = transactions,
                    darkMode = darkMode,
                    onToggleDark = { darkMode = !darkMode },
                    onOpenCustomer = { selectedCustomerId = it.id },
                    onAddCustomer = { name, phone, village, note ->
                        val newCustomer = Customer(
                            id = System.currentTimeMillis(),
                            name = name.trim(),
                            phone = phone.trim(),
                            village = village.trim(),
                            note = note.trim()
                        )
                        saveAll(newCustomers = customers + newCustomer)
                    }
                )
            } else {
                CustomerDetailScreen(
                    customer = selected,
                    transactions = transactions.filter { it.customerId == selected.id },
                    onBack = { selectedCustomerId = null },
                    onDeleteCustomer = {
                        saveAll(
                            newCustomers = customers.filterNot { it.id == selected.id },
                            newTransactions = transactions.filterNot { it.customerId == selected.id }
                        )
                        selectedCustomerId = null
                    },
                    onEditCustomer = { name, phone, village, note ->
                        saveAll(
                            newCustomers = customers.map {
                                if (it.id == selected.id) it.copy(
                                    name = name.trim(),
                                    phone = phone.trim(),
                                    village = village.trim(),
                                    note = note.trim()
                                ) else it
                            }
                        )
                    },
                    onAddTransaction = { amount, type, note ->
                        val txn = KhataTransaction(
                            id = System.currentTimeMillis(),
                            customerId = selected.id,
                            amount = amount,
                            type = type,
                            note = note.trim(),
                            date = System.currentTimeMillis()
                        )
                        saveAll(newTransactions = transactions + txn)
                    },
                    onDeleteTransaction = { txnId ->
                        saveAll(newTransactions = transactions.filterNot { it.id == txnId })
                    },
                    onSmsReminder = {
                        sendSmsReminder(context, selected, balanceOf(selected.id, transactions))
                    },
                    onWhatsAppReminder = {
                        sendWhatsAppReminder(context, selected, balanceOf(selected.id, transactions))
                    },
                    onCallCustomer = {
                        if (selected.phone.isNotBlank()) {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${selected.phone}"))
                            context.startActivity(intent)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun HomeScreen(
    customers: List<Customer>,
    transactions: List<KhataTransaction>,
    darkMode: Boolean,
    onToggleDark: () -> Unit,
    onOpenCustomer: (Customer) -> Unit,
    onAddCustomer: (String, String, String, String) -> Unit
) {
    var showAdd by rememberSaveable { mutableStateOf(false) }
    var search by rememberSaveable { mutableStateOf("") }
    var sortMode by rememberSaveable { mutableStateOf("Balance") }

    val receive = customers.sumOf { maxOf(0.0, balanceOf(it.id, transactions)) }
    val pay = customers.sumOf { maxOf(0.0, -balanceOf(it.id, transactions)) }
    val net = receive - pay

    val filtered = customers
        .filter {
            it.name.contains(search, true) ||
                    it.phone.contains(search, true) ||
                    it.village.contains(search, true)
        }
        .let { list ->
            when (sortMode) {
                "Name" -> list.sortedBy { it.name.lowercase() }
                "Recent" -> list.sortedByDescending { it.id }
                else -> list.sortedByDescending { abs(balanceOf(it.id, transactions)) }
            }
        }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add customer")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                HeaderCard(darkMode = darkMode, onToggleDark = onToggleDark, receive = receive, pay = pay, net = net)
            }

            item {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    label = { Text("Search customer, phone, village") },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp)
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SortChip("Balance", sortMode, Icons.Default.FilterList) { sortMode = "Balance" }
                    SortChip("Name", sortMode, Icons.Default.SortByAlpha) { sortMode = "Name" }
                    SortChip("Recent", sortMode, Icons.Default.AccountBalanceWallet) { sortMode = "Recent" }
                }
            }

            if (filtered.isEmpty()) {
                item {
                    EmptyState(title = "No customers yet", message = "Tap + to add your first customer khata.")
                }
            } else {
                items(filtered, key = { it.id }) { customer ->
                    CustomerCard(
                        customer = customer,
                        balance = balanceOf(customer.id, transactions),
                        txnCount = transactions.count { it.customerId == customer.id },
                        onClick = { onOpenCustomer(customer) }
                    )
                }
            }
        }
    }

    if (showAdd) {
        CustomerDialog(
            title = "Add Customer",
            initialName = "",
            initialPhone = "",
            initialVillage = "",
            initialNote = "",
            onDismiss = { showAdd = false },
            onSave = { name, phone, village, note ->
                if (name.isNotBlank()) {
                    onAddCustomer(name, phone, village, note)
                    showAdd = false
                }
            }
        )
    }
}

@Composable
fun HeaderCard(darkMode: Boolean, onToggleDark: () -> Unit, receive: Double, pay: Double, net: Double) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .background(Brush.linearGradient(listOf(Color(0xFF0F766E), Color(0xFF2563EB), Color(0xFF7C3AED))))
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column {
                        Text("Grama Khata", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                        Text("Smart village credit ledger", color = Color.White.copy(alpha = 0.85f))
                    }
                    AssistChip(onClick = onToggleDark, label = { Text(if (darkMode) "Light" else "Dark") })
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SummaryMiniCard("You will get", rupee(receive), Icons.Default.TrendingUp, Modifier.weight(1f))
                    SummaryMiniCard("You will pay", rupee(pay), Icons.Default.TrendingDown, Modifier.weight(1f))
                }

                Text(text = "Net Balance: ${rupee(net)}", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SummaryMiniCard(title: String, amount: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.18f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
    ) {
        Column(Modifier.padding(12.dp)) {
            Icon(icon, contentDescription = null, tint = Color.White)
            Spacer(Modifier.height(6.dp))
            Text(title, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelMedium)
            Text(amount, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SortChip(label: String, selected: String, icon: ImageVector, onClick: () -> Unit) {
    FilterChip(
        selected = selected == label,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) }
    )
}

@Composable
fun CustomerCard(customer: Customer, balance: Double, txnCount: Int, onClick: () -> Unit) {
    val isReceive = balance >= 0
    val targetScale by animateFloatAsState(if (balance != 0.0) 1f else 0.98f, label = "scale")

    Card(
        modifier = Modifier.fillMaxWidth().scale(targetScale).clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(52.dp).background(if (isReceive) Color(0xFFDCFCE7) else Color(0xFFFEE2E2), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    customer.name.firstOrNull()?.uppercase() ?: "K",
                    color = if (isReceive) Color(0xFF15803D) else Color(0xFFB91C1C),
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(customer.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    listOf(customer.phone, customer.village).filter { it.isNotBlank() }.joinToString(" • ").ifBlank { "$txnCount transactions" },
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (txnCount > 0) {
                    Text("$txnCount transactions", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    rupee(abs(balance)),
                    color = if (isReceive) Color(0xFF15803D) else Color(0xFFDC2626),
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    if (isReceive) "You will get" else "You will pay",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
            }
        }
    }
}

@Composable
fun CustomerDetailScreen(
    customer: Customer,
    transactions: List<KhataTransaction>,
    onBack: () -> Unit,
    onDeleteCustomer: () -> Unit,
    onEditCustomer: (String, String, String, String) -> Unit,
    onAddTransaction: (Double, String, String) -> Unit,
    onDeleteTransaction: (Long) -> Unit,
    onSmsReminder: () -> Unit,
    onWhatsAppReminder: () -> Unit,
    onCallCustomer: () -> Unit
) {
    var showTxn by rememberSaveable { mutableStateOf<String?>(null) }
    var showEdit by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var tab by rememberSaveable { mutableStateOf(0) }

    val sorted = transactions.sortedByDescending { it.date }
    val filtered = when (tab) {
        1 -> sorted.filter { it.type == "GIVE" }
        2 -> sorted.filter { it.type == "TAKE" }
        else -> sorted
    }

    val balance = balanceOf(customer.id, transactions)

    Scaffold(
        topBar = {
            Surface(shadowElevation = 2.dp) {
                Row(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                    Column(Modifier.weight(1f)) {
                        Text(customer.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                        Text(customer.village.ifBlank { "Customer details" }, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                    IconButton(onClick = { showEdit = true }) { Icon(Icons.Default.Edit, contentDescription = "Edit") }
                    IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Default.Delete, contentDescription = "Delete") }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { BalanceDetailCard(balance = balance, customer = customer) }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { showTxn = "GIVE" },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                    ) {
                        Icon(Icons.Default.TrendingUp, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Give")
                    }
                    Button(
                        onClick = { showTxn = "TAKE" },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                    ) {
                        Icon(Icons.Default.TrendingDown, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Take")
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = onSmsReminder,
                        enabled = customer.phone.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Icon(Icons.Default.Sms, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("SMS")
                    }

                    OutlinedButton(
                        onClick = onWhatsAppReminder,
                        enabled = customer.phone.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Icon(Icons.Default.Message, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("WhatsApp")
                    }

                    OutlinedButton(
                        onClick = onCallCustomer,
                        enabled = customer.phone.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Call")
                    }
                }
            }

            item {
                TabRow(selectedTabIndex = tab) {
                    Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("All") })
                    Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Given") })
                    Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("Taken") })
                }
            }

            if (filtered.isEmpty()) {
                item { EmptyState("No transactions", "Add Give or Take entry to start this khata.") }
            } else {
                items(filtered, key = { it.id }) { txn ->
                    TransactionCard(txn = txn, onDelete = { onDeleteTransaction(txn.id) })
                }
            }
        }
    }

    if (showTxn != null) {
        TransactionDialog(
            type = showTxn ?: "GIVE",
            onDismiss = { showTxn = null },
            onSave = { amount, note ->
                if (amount > 0) {
                    onAddTransaction(amount, showTxn ?: "GIVE", note)
                    showTxn = null
                }
            }
        )
    }

    if (showEdit) {
        CustomerDialog(
            title = "Edit Customer",
            initialName = customer.name,
            initialPhone = customer.phone,
            initialVillage = customer.village,
            initialNote = customer.note,
            onDismiss = { showEdit = false },
            onSave = { name, phone, village, note ->
                if (name.isNotBlank()) {
                    onEditCustomer(name, phone, village, note)
                    showEdit = false
                }
            }
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete customer?") },
            text = { Text("This will delete the customer and all their transactions.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDeleteCustomer()
                }) { Text("Delete", color = Color(0xFFDC2626)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun BalanceDetailCard(balance: Double, customer: Customer) {
    val isReceive = balance >= 0
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = if (isReceive) Color(0xFFECFDF5) else Color(0xFFFEF2F2))
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                if (isReceive) "You will receive" else "You will pay",
                color = if (isReceive) Color(0xFF15803D) else Color(0xFFB91C1C),
                fontWeight = FontWeight.Bold
            )
            Text(
                rupee(abs(balance)),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = if (isReceive) Color(0xFF15803D) else Color(0xFFB91C1C)
            )
            AnimatedVisibility(customer.note.isNotBlank()) {
                Text(customer.note, color = Color(0xFF475569))
            }
        }
    }
}

@Composable
fun TransactionCard(txn: KhataTransaction, onDelete: () -> Unit) {
    val isGive = txn.type == "GIVE"
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).background(if (isGive) Color(0xFFDCFCE7) else Color(0xFFFEE2E2), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isGive) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                    contentDescription = null,
                    tint = if (isGive) Color(0xFF15803D) else Color(0xFFDC2626)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(if (isGive) "Given" else "Taken", fontWeight = FontWeight.Bold)
                Text(
                    txn.note.ifBlank { formatDate(txn.date) },
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (txn.note.isNotBlank()) {
                    Text(formatDate(txn.date), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f))
                }
            }
            Text(
                rupee(txn.amount),
                fontWeight = FontWeight.Bold,
                color = if (isGive) Color(0xFF15803D) else Color(0xFFDC2626)
            )
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete transaction", tint = Color(0xFF64748B))
            }
        }
    }
}

@Composable
fun CustomerDialog(
    title: String,
    initialName: String,
    initialPhone: String,
    initialVillage: String,
    initialNote: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    var phone by rememberSaveable { mutableStateOf(initialPhone) }
    var village by rememberSaveable { mutableStateOf(initialVillage) }
    var note by rememberSaveable { mutableStateOf(initialNote) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Customer name *") }, singleLine = true)
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                OutlinedTextField(value = village, onValueChange = { village = it }, label = { Text("Village / Area") }, singleLine = true)
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Note") })
            }
        },
        confirmButton = {
            Button(onClick = { onSave(name, phone, village, note) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun TransactionDialog(type: String, onDismiss: () -> Unit, onSave: (Double, String) -> Unit) {
    var amountText by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    val amount = amountText.toDoubleOrNull() ?: 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (type == "GIVE") "Add Give Entry" else "Add Take Entry") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("Amount *") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Note") })
            }
        },
        confirmButton = {
            Button(enabled = amount > 0, onClick = { onSave(amount, note) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun EmptyState(title: String, message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(42.dp), tint = MaterialTheme.colorScheme.primary)
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text(message, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
    }
}

fun balanceOf(customerId: Long, transactions: List<KhataTransaction>): Double {
    return transactions.filter { it.customerId == customerId }.sumOf {
        if (it.type == "GIVE") it.amount else -it.amount
    }
}

fun rupee(value: Double): String {
    return "₹" + if (value % 1.0 == 0.0) value.toLong().toString() else "%.2f".format(value)
}

fun formatDate(time: Long): String {
    return SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(time))
}

fun reminderMessage(customer: Customer, balance: Double): String {
    return if (balance >= 0) {
        "Namaste ${customer.name}, according to Grama Khata, pending amount is ${rupee(balance)}. Kindly clear it when possible."
    } else {
        "Namaste ${customer.name}, according to Grama Khata, I have to pay you ${rupee(abs(balance))}. I will clear it soon."
    }
}

fun sendSmsReminder(context: Context, customer: Customer, balance: Double) {
    val phone = customer.phone.trim()
    if (phone.isBlank()) return

    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("smsto:$phone")
        putExtra("sms_body", reminderMessage(customer, balance))
    }
    context.startActivity(intent)
}

fun sendWhatsAppReminder(context: Context, customer: Customer, balance: Double) {
    val rawPhone = customer.phone.filter { it.isDigit() }
    if (rawPhone.isBlank()) return

    val phoneWithCountryCode = if (rawPhone.length == 10) "91$rawPhone" else rawPhone
    val message = Uri.encode(reminderMessage(customer, balance))
    val uri = Uri.parse("https://wa.me/$phoneWithCountryCode?text=$message")

    val intent = Intent(Intent.ACTION_VIEW, uri)
    context.startActivity(intent)
}
