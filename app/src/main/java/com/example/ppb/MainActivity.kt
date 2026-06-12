package com.example.ppb

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ppb.ui.theme.PPBTheme
import java.text.NumberFormat
import java.time.LocalDateTime
import java.util.Locale


data class MenuItem(val name:String, val costCents:Long, val count:Int=0)
data class Payment(val type:String, val amountCents:Long)
data class OrderHistory(
    val timestamp:LocalDateTime,
    val adult:Int, val child:Int, val staff:Int, val plane:Int, val payment:String, val totalCents:Long)


class OrderPageViewModel : ViewModel(){

    private val _menuItems = mutableStateListOf<MenuItem>()
    val menuItems: List<MenuItem> get() = _menuItems

    var totalCents by mutableLongStateOf(0)
        private set
    private val _history = mutableStateListOf<OrderHistory>()
    val history:List<OrderHistory> get() = _history

    private var _itemPrice:Map<String,Long>
    init{
        var menuItems = listOf(
            MenuItem("Adult", 1000L),
            MenuItem("Child", 500L),
            MenuItem("Plane", 4500L),
            MenuItem("Staff", 0L)
        )
        _menuItems.addAll(menuItems)
        _itemPrice = menuItems.associate {it.name to it.costCents}
    }

    fun clearOrders(){
        _menuItems.replaceAll{ item -> item.copy(count =0)}
        generateTotal()
    }
    fun updateOrder(menuItem:MenuItem){
        val index = _menuItems.indexOfFirst { it.name == menuItem.name }
        if (index != -1) {
            _menuItems[index] = _menuItems[index].copy(
                count = menuItem.count
            )
        }
        generateTotal()
    }
    fun executeOrder(payment:Payment){
        _history.add(
            OrderHistory(
                timestamp = LocalDateTime.now(),
                adult = _menuItems.find { it.name == "Adult" }?.count ?: 0,
                child = _menuItems.find { it.name == "Child" }?.count ?: 0,
                staff = _menuItems.find { it.name == "Staff" }?.count ?: 0,
                plane = _menuItems.find { it.name == "Plane" }?.count ?: 0,
                payment = payment.type,
                totalCents = payment.amountCents
            )
        )
        clearOrders()
    }
    private fun generateTotal(){
        var total = 0L
        for(menuItem in _menuItems){
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
                Surface(modifier = Modifier.fillMaxSize().padding(20.dp),
                    color = MaterialTheme.colorScheme.background) {
                    MyApp()
                }
            }
        }
    }
}
@Preview(showBackground = true)
@Composable
fun MyApp() {
    val orderPageViewModel: OrderPageViewModel = viewModel()
    Row {
        Column(modifier = Modifier.weight(3f)) {
            Menu(menuItems = orderPageViewModel.menuItems,
                onChange = { itemCount -> orderPageViewModel.updateOrder(itemCount)})

            Totals(orderPageViewModel.totalCents,
                onPayment = {payment -> orderPageViewModel.executeOrder(payment) })
        }
        Column(modifier = Modifier.weight(2f), horizontalAlignment = Alignment.CenterHorizontally) {
            Button(
                onClick = { orderPageViewModel.clearOrders() }
            ) {
                Text("Cancel")
            }
            OrderHistory(orderPageViewModel.history)
        }
    }
}
@Composable
fun Menu(menuItems: List<MenuItem>, onChange:(itemCount:MenuItem)->Unit){
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround,
    ) {
        for (menuItem in menuItems) {
            MenuItem(menuItem, onChange = onChange)
        }
    }
}
@Composable
fun MenuItem(menuItem: MenuItem, onChange:(itemCount:MenuItem) ->Unit){
    val costStr =centsToCostStr(menuItem.costCents)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ){
        Text(menuItem.name)
        Text(costStr)
        Button(onClick = { onChange(menuItem.copy(count = menuItem.count+1)) }) {
            Text("+")
        }
        Text("${menuItem.count}")
        Button(
            enabled = menuItem.count > 0,
            onClick = {onChange(menuItem.copy(count = menuItem.count-1))}) {
            Text("-")
        }
    }
}
@Composable
fun Totals(totalCents:Long, onPayment:(payment:Payment) -> Unit){
    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.weight(0.7f),
            horizontalArrangement = Arrangement.SpaceAround,
            ) {
            Payment("Cash", totalCents, onClick = onPayment)
            Payment("Card", (totalCents * 1.026).toLong() + 30, enabled = totalCents != 0L, onClick = onPayment)
        }
    }
}

@Composable
fun Payment(paymentType: String, totalCents: Long, enabled:Boolean = true, onClick:(payment:Payment)->Unit) {
    val costStr = centsToCostStr(totalCents)
    Button(
        onClick = { onClick(Payment(paymentType, totalCents)) }
        , enabled = enabled
    ){
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

@Composable
fun OrderHistory(orderHistory: List<OrderHistory>, modifier: Modifier = Modifier){
    Column(
        modifier = modifier
    ) {
        Text("History stuff here")
        for(order in orderHistory){
            Text("$order")
        }
    }
}
