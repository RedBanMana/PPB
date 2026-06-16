package com.example.ppb

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.ppb.ui.theme.PPBTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.json.Json
import java.text.NumberFormat
import java.util.Locale
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant


data class MenuItem(val name: String, val costCents: Long, val count: Int = 0)
data class Payment(val type: String, val amountCents: Long)

class Converters {
    @TypeConverter
    fun instantToEpochLong(value: Long): Instant {
        return Instant.fromEpochSeconds(value)
    }

    @TypeConverter
    fun epochLongToInstant(instant: Instant): Long {
        return instant.toEpochMilliseconds() / 1000
    }
}
@Serializable(with = Order.Companion::class) // Links directly to the companion object below
@Entity(tableName = "orders")
data class Order(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "timestamp") val timestamp: Instant,
    @ColumnInfo(name = "adult") val adult: Int,
    @ColumnInfo(name = "child") val child: Int,
    @ColumnInfo(name = "staff") val staff: Int,
    @ColumnInfo(name = "plane") val plane: Int,
    @ColumnInfo(name = "total_cents") val totalCents: Long,
    @ColumnInfo(name = "payment") val payment: String
) {
    // Companion object implementing KSerializer to handle the entire object manually
    companion object : KSerializer<Order> {

        // 1. Define the structural schema for the JSON payload
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Order") {
            element<Int>("id")
            element<String>("timestamp") // Maps the Instant object into a clean JSON String
            element<Int>("adult")
            element<Int>("child")
            element<Int>("staff")
            element<Int>("plane")
            element<Long>("total_cents")
            element<String>("payment")
        }

        // 2. Serialize: Object to JSON output format
        override fun serialize(encoder: Encoder, value: Order) {
            encoder.encodeStructure(descriptor) {
                encodeIntElement(descriptor, 0, value.id)
                // Convert java.time.Instant directly to ISO-8601 string (e.g., "2026-06-15T20:22:00Z")
                encodeStringElement(descriptor, 1, value.timestamp.toString())
                encodeIntElement(descriptor, 2, value.adult)
                encodeIntElement(descriptor, 3, value.child)
                encodeIntElement(descriptor, 4, value.staff)
                encodeIntElement(descriptor, 5, value.plane)
                encodeLongElement(descriptor, 6, value.totalCents)
                encodeStringElement(descriptor, 7, value.payment)
            }
        }

        // 3. Deserialize: JSON payload back into a Room-ready Order object
        override fun deserialize(decoder: Decoder): Order {
            return decoder.decodeStructure(descriptor) {
                var id = 0
                var timestampStr = ""
                var adult = 0
                var child = 0
                var staff = 0
                var plane = 0
                var totalCents = 0L
                var payment = ""

                while (true) {
                    when (val index = decodeElementIndex(descriptor)) {
                        0 -> id = decodeIntElement(descriptor, 0)
                        1 -> timestampStr = decodeStringElement(descriptor, 1)
                        2 -> adult = decodeIntElement(descriptor, 2)
                        3 -> child = decodeIntElement(descriptor, 3)
                        4 -> staff = decodeIntElement(descriptor, 4)
                        5 -> plane = decodeIntElement(descriptor, 5)
                        6 -> totalCents = decodeLongElement(descriptor, 6)
                        7 -> payment = decodeStringElement(descriptor, 7)
                        CompositeDecoder.DECODE_DONE -> break
                        else -> error("Unknown indexing constraint: $index")
                    }
                }

                Order(
                    id = id,
                    // Parse the plain text string back into a rich Instant object type
                    timestamp = Instant.parse(timestampStr),
                    adult = adult,
                    child = child,
                    staff = staff,
                    plane = plane,
                    totalCents = totalCents,
                    payment = payment
                )
            }
        }
    }
}

@Dao
interface OrdersDao {
    @Query("SELECT * FROM orders")
    suspend fun getAll(): List<Order>

    @Query("select count(id) as id, UNIXEPOCH(STRFTIME('%Y-%m-%d %H:00:00', timestamp, 'unixepoch')) AS timestamp, SUM(adult) as adult, SUM(child) as child, sum(staff) as staff, sum(plane) as plane, sum(total_cents) as total_cents, 'All' as payment from orders group by STRFTIME('%Y-%m-%d %H:00:00', timestamp, 'unixepoch')")
    fun getOrderSummary(): Flow<List<Order>>

    @Insert
    suspend fun insert(order: Order)
}

@Database(entities = [Order::class], version = 3)
@TypeConverters(Converters::class)
abstract class OrderDatabase : RoomDatabase() {
    abstract fun ordersDao(): OrdersDao

    companion object {
        @Volatile
        private var INSTANCE: OrderDatabase? = null
        fun getDatabase(context: Context): OrderDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    OrderDatabase::class.java,
                    "order_database"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class OrderRepository(private val ordersDao: OrdersDao) {
    suspend fun getAll(): List<Order> = ordersDao.getAll()

    val orderSummary: Flow<List<Order>> = ordersDao.getOrderSummary()
    suspend fun insert(order: Order) {
        ordersDao.insert(order)
    }
}

// Factory class to instantiate the ViewModel with a custom Repository parameter
class OrderViewModelFactory(private val repository: OrderRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OrderPageViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return OrderPageViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

sealed interface UiEvent {
    data class ShowToast(val message: String) : UiEvent
}

class OrderPageViewModel(private val repository: OrderRepository) : ViewModel() {

    private val _eventChannel = Channel<UiEvent>()
    val eventFlow = _eventChannel.receiveAsFlow()

    val orders: StateFlow<List<Order>> = repository.orderSummary.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private fun insert(order: Order) = viewModelScope.launch {
        repository.insert(order)
    }

    private val _menuItems = mutableStateListOf<MenuItem>()
    val menuItems: List<MenuItem> get() = _menuItems

    var totalCents by mutableLongStateOf(0)
        private set
    var totalItems by mutableIntStateOf(0)
        private set

    private var _itemPrice: Map<String, Long>

    init {
        var menuItems = listOf(
            MenuItem("Adult", 1000L),
            MenuItem("Child", 500L),
            MenuItem("Plane", 4500L),
            MenuItem("Staff", 0L)
        )
        _menuItems.addAll(menuItems)
        _itemPrice = menuItems.associate { it.name to it.costCents }
    }

    fun clearOrders() {
        _menuItems.replaceAll { item -> item.copy(count = 0) }
        totalItems = 0
        generateTotal()
    }

    fun updateOrder(menuItem: MenuItem) {
        val index = _menuItems.indexOfFirst { it.name == menuItem.name }
        if (index != -1) {
            _menuItems[index] = _menuItems[index].copy(
                count = menuItem.count
            )
        }
        totalItems = _menuItems.sumOf { it.count }
        generateTotal()
    }


    fun executeOrder(payment: Payment) {
        val order = Order(
            timestamp = Clock.System.now(),
            adult = _menuItems.find { it.name == "Adult" }?.count ?: 0,
            child = _menuItems.find { it.name == "Child" }?.count ?: 0,
            staff = _menuItems.find { it.name == "Staff" }?.count ?: 0,
            plane = _menuItems.find { it.name == "Plane" }?.count ?: 0,
            payment = payment.type,
            totalCents = payment.amountCents
        )
        insert(order)
        clearOrders()
    }

    private fun generateTotal() {
        var total = 0L
        for (menuItem in _menuItems) {
            total += menuItem.costCents * menuItem.count
        }
        totalCents = total
    }

    fun exportOrder(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val content = repository.getAll()
            val success = saveOrdersToDownloads(context, content)
            _eventChannel.send(UiEvent.ShowToast(if (success) "Exported to Downloads" else "Export Failed"))
        }
    }

    fun saveOrdersToDownloads(context: Context, orders: List<Order>): Boolean {
        return try {
            val jsonString = Json.encodeToString(orders)
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "orders.json")
                put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }

            val uri = MediaStore.Downloads.EXTERNAL_CONTENT_URI

            val fileUri = context.contentResolver.insert(uri, values) ?: return false
            context.contentResolver.openOutputStream(fileUri)?.use { outputStream ->
                outputStream.write(jsonString.toByteArray())
            }
            true // Success!
        } catch (e: Exception) {
            e.printStackTrace()
            false // Failed!
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PPBTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MyApp()
                }
            }
        }
    }
}


@Preview(showBackground = true, widthDp = 1280, heightDp = 800)
@Composable
fun MyApp() {
    val context = LocalContext.current
    val db = OrderDatabase.getDatabase(context)
    val repository = OrderRepository(db.ordersDao())
    val orderPageViewModel: OrderPageViewModel =
        viewModel(factory = OrderViewModelFactory(repository))
    val orders by orderPageViewModel.orders.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        orderPageViewModel.eventFlow.collect { event ->
            when (event) {
                is UiEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    Row {
        Column(
            modifier = Modifier
                .weight(4f)
                .padding(8.dp)
        ) {
            Menu(
                menuItems = orderPageViewModel.menuItems,
                onChange = { itemCount -> orderPageViewModel.updateOrder(itemCount) }
            )
            Spacer(modifier = Modifier.padding(vertical = 18.dp))
            Totals(
                orderPageViewModel.totalCents,
                orderPageViewModel.totalItems,
                onPayment = { payment -> orderPageViewModel.executeOrder(payment) }
            )
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = { orderPageViewModel.clearOrders() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Red,
                    contentColor = Color.Black
                )
            ) {
                Text("Clear Order", style = MaterialTheme.typography.headlineMedium)
            }
        }
        VerticalDivider()
        OrderHistoryWrapper(
            orders,
            onExport = { context -> orderPageViewModel.exportOrder(context) },
            modifier = Modifier.weight(2f)
        )
    }
}

@Composable
fun Menu(menuItems: List<MenuItem>, onChange: (itemCount: MenuItem) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround,
    ) {
        for (menuItem in menuItems) {
            MenuItem(menuItem, onChange = onChange, Modifier.weight(1f))
        }
    }
}

@Composable
fun MenuItem(
    menuItem: MenuItem,
    onChange: (itemCount: MenuItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val costStr = centsToCostStr(menuItem.costCents)
    Column(
        modifier = modifier.padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(menuItem.name, style = MaterialTheme.typography.headlineLarge)
        Text(costStr, style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.padding(vertical = 12.dp))
        Button(
            onClick = { onChange(menuItem.copy(count = menuItem.count + 1)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        ) {
            Text(
                "+",
                style = MaterialTheme.typography.displayLarge,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }
        Text(
            "${menuItem.count}",
            style = MaterialTheme.typography.displayMedium,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        Button(
            enabled = menuItem.count > 0,
            onClick = { onChange(menuItem.copy(count = menuItem.count - 1)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        ) {
            Text(
                "-",
                style = MaterialTheme.typography.displayLarge,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }
    }
}

@Composable
fun Totals(totalCents: Long, totalItems: Int, onPayment: (payment: Payment) -> Unit) {
    val card = if (totalCents == 0L) 0L else (totalCents * 1.026).toLong() + 30
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround,
    ) {
        Payment(
            "Cash",
            totalCents,
            onClick = onPayment,
            modifier = Modifier.weight(1f),
            enabled = totalItems > 0
        )
        Payment(
            "Card",
            card,
            onClick = onPayment,
            modifier = Modifier.weight(1f),
            enabled = totalItems > 0 && card > 0
        )
    }
}

@Composable
fun Payment(
    paymentType: String,
    totalCents: Long,
    onClick: (payment: Payment) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val costStr = centsToCostStr(totalCents)
    Button(
        onClick = { onClick(Payment(paymentType, totalCents)) },
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        enabled = enabled
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(paymentType, style = MaterialTheme.typography.headlineLarge)
            Text(costStr, style = MaterialTheme.typography.displayMedium)
        }
    }
}

private fun centsToCostStr(totalCents: Long): String {
    val usFormat = NumberFormat.getCurrencyInstance(Locale.US)
    val amount = totalCents / 100.0
    val amountStr = usFormat.format(amount)
    return amountStr
}

data class DateTimeKey(val date: kotlinx.datetime.LocalDate, val hour: Int) :
    Comparable<DateTimeKey> {
    override fun compareTo(other: DateTimeKey): Int {
        val dateCompare = this.date.compareTo(other.date)
        if (dateCompare != 0) return dateCompare
        return this.hour.compareTo(other.hour)
    }
}


@Preview(showBackground = true)
@Composable
fun HistoryPreview() {
    OrderHistoryWrapper(
        listOf(
            Order(
                1, timestamp = Clock.System.now(),
                1,
                1,
                1,
                1,
                100,
                "test"
            ),
            Order(
                2,
                timestamp = Clock.System.now().plus(1.hours),
                100000000,
                15000,
                16,
                45,
                100,
                "test"
            ),
            Order(
                3,
                timestamp = Clock.System.now().plus(2.hours),
                100000000,
                15000,
                16,
                45,
                100,
                "test"
            )
        ), onExport = { true }
    )
}

@Composable
fun OrderHistoryWrapper(
    order: List<Order>,
    onExport: (Context) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Column(modifier = modifier) {
        OrderHistory(order, Modifier.weight(1f))
        //HorizontalDivider()
        Row(
            Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = { onExport(context) })
            {
                Text("Export", style = MaterialTheme.typography.headlineMedium)
            }
        }
    }
}

@Composable
fun OrderHistory(orders: List<Order>, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        stickyHeader {
            Text(
                "Order History",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp)
            )
        }
        items(
            orders,
            key = { entry -> entry.timestamp },
            contentType = { "hourly_summary_row" }
        ) { order ->
            OrderHistoryRow(order)
            HorizontalDivider()
        }
    }
}

@Composable
private fun OrderHistoryRow(order: Order) {

    val local = order.timestamp.toLocalDateTime(TimeZone.currentSystemDefault())
    Column(
        modifier = Modifier.padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "${local.date} • Hour ${local.hour}:00",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SummaryItem("Adult", order.adult, Modifier.weight(1f))
            SummaryItem("Child", order.child, Modifier.weight(1f))
            SummaryItem("Plane", order.plane, Modifier.weight(1f))
            SummaryItem("Staff", order.staff, Modifier.weight(1f))
        }
    }
}

@Composable
private fun SummaryItem(label: String, count: Int, modifier: Modifier = Modifier) {
    val numberFormat = NumberFormat.getNumberInstance(Locale.US)
    val formattedCount = numberFormat.format(count)
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = formattedCount,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
