package com.example.ppb

import android.content.Context
import android.os.Bundle
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.text.NumberFormat
import java.util.Locale
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant


data class MenuItem(val name: String, val costCents: Long, val count: Int = 0)
data class Payment(val type: String, val amountCents: Long)
data class OrderHistory(
    val timestamp: Instant,
    val adult: Int,
    val child: Int,
    val staff: Int,
    val plane: Int,
    val payment: String,
    val totalCents: Long
)

class Converters {
    @TypeConverter
    fun instantToEpochLong(value: Long): Instant {
        return Instant.fromEpochMilliseconds(value)
    }

    @TypeConverter
    fun epochLongToInstant(instant: Instant): Long {
        return instant.toEpochMilliseconds()
    }
}

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
)

@Dao
interface OrdersDao {
    @Query("SELECT * FROM orders")
    fun getAll(): Flow<List<Order>>

    @Insert
    suspend fun insert(order: Order)
}

@Database(entities = [Order::class], version = 2)
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
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class OrderRepository(private val ordersDao: OrdersDao) {
    val allOrders: Flow<List<Order>> = ordersDao.getAll()
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

class OrderPageViewModel(private val repository: OrderRepository) : ViewModel() {

    private val _orders: StateFlow<List<Order>> = repository.allOrders.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    val orders: List<Order> get() = _orders.value

    private fun insert(order: Order) = viewModelScope.launch {
        repository.insert(order)
    }

    private val _menuItems = mutableStateListOf<MenuItem>()
    val menuItems: List<MenuItem> get() = _menuItems

    var totalCents by mutableLongStateOf(0)
        private set
    private val _history = mutableStateListOf<OrderHistory>()
    val history: List<OrderHistory> get() = _history

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
        generateTotal()
    }

    fun updateOrder(menuItem: MenuItem) {
        val index = _menuItems.indexOfFirst { it.name == menuItem.name }
        if (index != -1) {
            _menuItems[index] = _menuItems[index].copy(
                count = menuItem.count
            )
        }
        generateTotal()
    }

    var offset: Duration = 0.minutes

    fun executeOrder(payment: Payment) {

        insert(Order(
            timestamp = Clock.System.now().plus(offset),
            adult = _menuItems.find { it.name == "Adult" }?.count ?: 0,
            child = _menuItems.find { it.name == "Child" }?.count ?: 0,
            staff = _menuItems.find { it.name == "Staff" }?.count ?: 0,
            plane = _menuItems.find { it.name == "Plane" }?.count ?: 0,
            payment = payment.type,
            totalCents = payment.amountCents
        ))
        clearOrders()
        offset += 25.minutes
    }

    private fun generateTotal() {
        var total = 0L
        for (menuItem in _menuItems) {
            total += menuItem.costCents * menuItem.count
        }
        totalCents = total
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
    val orderPageViewModel: OrderPageViewModel = viewModel(factory = OrderViewModelFactory(repository))

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
            Spacer(modifier = Modifier.padding(vertical = 12.dp))
            Totals(
                orderPageViewModel.totalCents,
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
        OrderHistory(orderPageViewModel.orders, Modifier.weight(2f))
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
            Text("+", style = MaterialTheme.typography.displayLarge)
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
            Text("-", style = MaterialTheme.typography.displayLarge)
        }
    }
}

@Composable
fun Totals(totalCents: Long, onPayment: (payment: Payment) -> Unit) {
    val card = if (totalCents == 0L) 0L else (totalCents * 1.026).toLong() + 30
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround,
    ) {
        Payment("Cash", totalCents, onClick = onPayment, Modifier.weight(1f))
        Payment(
            "Card",
            card,
            onClick = onPayment,
            modifier = Modifier.weight(1f),
            enabled = card != 0L
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

data class GroupedTotals(
    val totalAdults: Int, val totalChildren: Int, val totalStaff: Int,
    val totalPlanes: Int, val totalCents: Long
)

@Preview(showBackground = true)
@Composable
fun HistoryPreview() {
    OrderHistory(
        listOf(
            Order(1, timestamp = Clock.System.now(),
                1,
                1,
                1,
                1,
                100,
                "test"
            ),
            Order(2,
                timestamp = Clock.System.now().plus(1.hours),
                100000000,
                15000,
                16,
                45,
                100,
                "test"
            ),
            Order(3,
                timestamp = Clock.System.now().plus(2.hours),
                100000000,
                15000,
                16,
                45,
                100,
                "test"
            )
        )
    )
}

data class HourlyOrderSummary(
    val date: kotlinx.datetime.LocalDate,
    val hour: Int,
    val totalAdults: Int,
    val totalChildren: Int,
    val totalPlanes: Int,
    val totalStaff: Int
)

@Composable
fun OrderHistory(orderHistory: List<Order>, modifier: Modifier = Modifier) {

    val orderedGroups = orderHistory.groupingBy { order ->
        val local = order.timestamp.toLocalDateTime(TimeZone.currentSystemDefault())
        DateTimeKey(local.date, local.hour)
    }.fold(
        initialValueSelector = { _, _ -> GroupedTotals(0, 0, 0, 0, 0L) },
        operation = { _, acc, order ->
            GroupedTotals(
                totalAdults = acc.totalAdults + order.adult,
                totalChildren = acc.totalChildren + order.child,
                totalStaff = acc.totalStaff + order.staff,
                totalPlanes = acc.totalPlanes + order.plane,
                totalCents = acc.totalCents + order.totalCents
            )
        }
    ).map { (key, totals) ->
        HourlyOrderSummary(
            key.date,
            key.hour,
            totals.totalAdults,
            totals.totalChildren,
            totals.totalPlanes,
            totals.totalStaff
        )
    }.sortedWith(compareBy<HourlyOrderSummary> { it.date }.thenBy { it.hour })

    LazyColumn(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item {
            Text("Order History", style = MaterialTheme.typography.headlineMedium)
        }
        items(
            orderedGroups,
            key = { entry -> "${entry.date} ${entry.hour}" },
            contentType = { "hourly_summary_row" }
        ) { order ->
            OrderHistoryRow(order)
            HorizontalDivider()
        }
    }
}

@Composable
private fun OrderHistoryRow(order: HourlyOrderSummary) {

    Column(
        modifier = Modifier.padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "${order.date} • Hour ${order.hour}:00",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SummaryItem("Adult", order.totalAdults, Modifier.weight(1f))
            SummaryItem("Child", order.totalChildren, Modifier.weight(1f))
            SummaryItem("Plane", order.totalPlanes, Modifier.weight(1f))
            SummaryItem("Staff", order.totalStaff, Modifier.weight(1f))
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
            style = MaterialTheme.typography.titleSmall
        )
        Text(
            text = formattedCount,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
