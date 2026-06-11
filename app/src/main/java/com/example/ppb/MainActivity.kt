package com.example.ppb

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.IntState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.ppb.ui.theme.PPBTheme
import java.text.NumberFormat
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import java.util.Locale
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.collections.set


data class ItemCost(val name:String, val costCents:Long)
data class ItemCount(val name:String, val amount:Int)
data class Payment(val type:String, val amountCents:Long)
data class OrderHistory(
    //val timestamp:LocalDateTime,
    val adult:Int, val child:Int, val staff:Int, val plane:Int, val payment:String, val totalCents:Long)

class OrderCalculator(val items:Map<String, Double>){
    val orders = mutableMapOf<String,Int>()

    fun clearOrders(){
        orders.clear();
    }
    fun addOrUpdateOrder(order:ItemCount){
        orders[order.name] = order.amount;
    }
    fun getTotal():Double{
        var total:Double = 0.0;
        for (order in orders){
            items[order.key]?.times(order.value)?.let { total += it };
        }
        return total;
    }
    fun processOrder(payment:Payment): OrderHistory{
        return OrderHistory( adult = 0, child =0, staff = 0, plane=0, payment = payment.type, totalCents = payment.amountCents)
    }
}

class OrderPageViewModel() : ViewModel(){


    var test by mutableIntStateOf(0)
        private set
    fun Test() = test++

    private val _itemCounts = mutableStateMapOf<String, Int>()
    private var _totalCents by mutableLongStateOf(0)
    private val _history = mutableStateListOf<OrderHistory>()
    private var _itemPrice:Map<String,Long>

    init{
        var itemCosts = listOf(ItemCost("Adult", 10000L))
        _itemCounts.apply { itemCosts.associate { it.name to 0 } }
        _itemPrice = itemCosts.associate {it.name to it.costCents}
    }


    val itemCounts: List<ItemCount> = _itemCounts.map{(key,value) -> ItemCount(key, value) }
    val totalCents: Long = _totalCents
    val history:List<OrderHistory> = _history

    fun clearOrders(){
        for(itemCount in _itemCounts)
            _itemCounts[itemCount.key] = 0
        generateTotal()
    }
    fun updateOrder(itemCount:ItemCount){
        _itemCounts[itemCount.name] = itemCount.amount;
        generateTotal()
    }
    fun executeOrder(payment:Payment){
        _history.add(OrderHistory( adult = 0, child =0, staff = 0, plane=0, payment = payment.type, totalCents = payment.amountCents))
        clearOrders()
    }
    private fun generateTotal(){
        var total = 0L;
        for(itemCount in _itemCounts){
            total += _itemPrice[itemCount.key]!! * itemCount.value
        }
        _totalCents = total;
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PPBTheme {
                Surface(modifier = Modifier.fillMaxSize(),
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
    val itemCosts = mapOf<String, Double>("Adult" to 10.0, "Child" to 5.0, "Staff" to 0.0, "Plane" to 45.0 )
    val orderPageViewModel: OrderPageViewModel = viewModel()



    Row {
        Column() {

            Text("${orderPageViewModel.test}")
            Button(onClick = {orderPageViewModel.Test()}) { Text("Inc") }


            Menu(itemsCounts = orderPageViewModel.itemCounts,
                onChange = { itemCount -> orderPageViewModel.updateOrder(itemCount)},
                modifier = Modifier.weight(2.5f))

            Totals(orderPageViewModel.totalCents,
                onPayment = {payment -> orderPageViewModel.executeOrder(payment) })
        }
        OrderHistory(orderPageViewModel.history, modifier = Modifier.weight(1f))
    }
}
@Composable
fun Menu(itemsCounts: List<ItemCount>, onChange:(itemCount:ItemCount)->Unit, modifier: Modifier = Modifier){
    Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
        Text("hey")
            for (menuItem in itemsCounts) {
                MenuItem(menuItem.name, amount = menuItem.amount, onChange = onChange)
            }
        Text("ehy")
        }
}
@Composable
fun MenuItem(item: String, amount:Int, onChange:(itemCount:ItemCount) ->Unit){
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ){
        Text(item)
        Button(onClick = { onChange(ItemCount(item, amount+1)) }) {
            Text("+")
        }
        Text("$amount")
        Button(
            enabled = amount > 0,
            onClick = {onChange(ItemCount(item, amount-1))}) {
            Text("-")
        }
    }
}
@Composable
fun Totals(totalCents:Long, onPayment:(payment:Payment) -> Unit){
    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        Spacer(modifier = Modifier.weight(0.15f))
        Row(
            modifier = Modifier.weight(0.7f),
            horizontalArrangement = Arrangement.SpaceBetween,
            ) {
            Payment("Cash", totalCents, onClick = onPayment)
            Payment("Card", (totalCents * 1.026).toLong() + 30, onClick = onPayment)
        }
        Spacer(modifier = Modifier.weight(0.15f))
    }
}

@Composable
fun Payment(paymentType: String, totalCents: Long, onClick:(payment:Payment)->Unit) {
    val usFormat = NumberFormat.getCurrencyInstance(Locale.US)
    val amountStr = usFormat.format(totalCents)
    Button(
        onClick = { onClick(Payment(paymentType, totalCents)) }
    ){
        Column(
            modifier = Modifier.background(MaterialTheme.colorScheme.primary),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(paymentType)
            Text(amountStr)
        }
    }
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
