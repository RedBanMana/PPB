package com.example.ppb

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ppb.ui.theme.PPBTheme
import java.text.NumberFormat
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Locale
import java.util.SortedMap
import kotlin.time.Duration.Companion.minutes


data class MenuItem(val name: String, val costCents: Long, val count: Int = 0)
data class Payment(val type: String, val amountCents: Long)
data class OrderHistory(
    val timestamp: LocalDateTime,
    val adult: Int,
    val child: Int,
    val staff: Int,
    val plane: Int,
    val payment: String,
    val totalCents: Long
)


class OrderPageViewModel : ViewModel() {

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

    var offset = Duration.ofMinutes(0)

    fun executeOrder(payment: Payment) {
        _history.add(
            OrderHistory(
                timestamp = LocalDateTime.now().plus(offset),
                adult = _menuItems.find { it.name == "Adult" }?.count ?: 0,
                child = _menuItems.find { it.name == "Child" }?.count ?: 0,
                staff = _menuItems.find { it.name == "Staff" }?.count ?: 0,
                plane = _menuItems.find { it.name == "Plane" }?.count ?: 0,
                payment = payment.type,
                totalCents = payment.amountCents
            )
        )
        clearOrders()
        offset += Duration.ofMinutes(25)
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
                        .fillMaxSize()
                        .padding(20.dp),
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
    val orderPageViewModel: OrderPageViewModel = viewModel()
    Row {
        Column(modifier = Modifier.weight(3f)) {
            Menu(
                menuItems = orderPageViewModel.menuItems,
                onChange = { itemCount -> orderPageViewModel.updateOrder(itemCount) })

            Totals(
                orderPageViewModel.totalCents,
                onPayment = { payment -> orderPageViewModel.executeOrder(payment) })
        }
        Column(modifier = Modifier.weight(2f), horizontalAlignment = Alignment.CenterHorizontally) {
            Button(
                onClick = { orderPageViewModel.clearOrders() }
            ) {
                Text("Cancel Order")
            }
            OrderHistory(orderPageViewModel.history)
        }
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
fun MenuItem(menuItem: MenuItem, onChange: (itemCount: MenuItem) -> Unit, modifier: Modifier = Modifier) {
    val costStr = centsToCostStr(menuItem.costCents)
    Column(
        modifier = modifier.padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(menuItem.name, style = MaterialTheme.typography.headlineMedium)
        Text(costStr, style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.padding(8.dp))
        Button(
            onClick = { onChange(menuItem.copy(count = menuItem.count + 1)) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
        ) {
            Text("+", style = MaterialTheme.typography.headlineLarge)
        }
        Text(
            "${menuItem.count}",
            style = MaterialTheme.typography.displaySmall,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        Button(
            enabled = menuItem.count > 0,
            onClick = { onChange(menuItem.copy(count = menuItem.count - 1)) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
        ) {
            Text("-", style = MaterialTheme.typography.headlineLarge)
        }
    }
}

@Composable
fun Totals(totalCents: Long, onPayment: (payment: Payment) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.weight(0.7f),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            Payment("Cash", totalCents, onClick = onPayment)
            Payment(
                "Card",
                (totalCents * 1.026).toLong() + 30,
                enabled = totalCents != 0L,
                onClick = onPayment
            )
        }
    }
}

@Composable
fun Payment(
    paymentType: String,
    totalCents: Long,
    enabled: Boolean = true,
    onClick: (payment: Payment) -> Unit
) {
    val costStr = centsToCostStr(totalCents)
    Button(
        onClick = { onClick(Payment(paymentType, totalCents)) }, enabled = enabled
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(paymentType)
            Text(costStr)
        }
    }
}

private fun centsToCostStr(totalCents: Long): String {
    val usFormat = NumberFormat.getCurrencyInstance(Locale.US)
    val amount = totalCents / 100.0
    val amountStr = usFormat.format(amount)
    return amountStr
}

data class HistoryGroup(val date: LocalDate, val hour: Int)
data class DateTimeKey(val date: LocalDate, val hour: Int) : Comparable<DateTimeKey> {
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
            OrderHistory(timestamp = LocalDateTime.now(), 1, 1, 1, 1, "test", 100),
            OrderHistory(
                timestamp = LocalDateTime.now().plus(Duration.ofHours(1)),
                100000000,
                15000,
                16,
                45,
                "test",
                100
            ),
            OrderHistory(
                timestamp = LocalDateTime.now().plus(Duration.ofHours(-1)),
                100000000,
                15000,
                16,
                45,
                "test",
                100
            )
        )
    )
}

data class HourlyOrderSummary(val date: LocalDate, val hour:Int, val totalAdults: Int, val totalChildren: Int, val totalPlanes: Int, val totalStaff: Int)
@Composable
fun OrderHistory(orderHistory: List<OrderHistory>, modifier: Modifier = Modifier) {

    val orderedGroups = orderHistory.groupingBy { order ->
        DateTimeKey(order.timestamp.toLocalDate(), order.timestamp.hour)
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
    }.sortedWith( compareBy<HourlyOrderSummary> { it.date }.thenBy{ it.hour })

    LazyColumn(
        modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text("Order History")
        }
        items(orderedGroups,
            key = {entry -> "${entry.date} ${entry.hour}"},
            contentType = { "hourly_summary_row" }
        ) { order ->
            OrderHistoryRow(order)
        }
    }
}

@Composable
private fun OrderHistoryRow(order: HourlyOrderSummary) {
    Row() {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${order.date} - Hour: ${order.hour}")
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(
                    Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Adults")
                    Text("${order.totalAdults}")
                }
                Column(
                    Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Children")
                    Text("${order.totalChildren}")
                }
                Column(
                    Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Planes")
                    Text("${order.totalPlanes}")
                }
                Column(
                    Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Staff")
                    Text("${order.totalStaff}")
                }
            }
            Spacer(Modifier.padding(10.dp))
        }
    }
}
