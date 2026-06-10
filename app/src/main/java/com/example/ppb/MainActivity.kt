package com.example.ppb

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import kotlinx.datetime.*

data class MenuItem(val name:String, val cost:Double)
data class ItemOrder(val name:String, val amount:Int)
data class Payment(val type:String, val amount:Double)
data class OrderHistory(
    //val timestamp:LocalDateTime,
    val adult:Int, val child:Int, val staff:Int, val plane:Int, val payment:String, val total:Double)

class OrderCalculator(val items:Map<String, Double>){
    val orders = mutableMapOf<String,Int>()

    fun clearOrders(){
        orders.clear();
    }
    fun addOrUpdateOrder(order:ItemOrder){
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
        return OrderHistory( adult = 0, child =0, staff = 0, plane=0, payment = payment.type, total = payment.amount)
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
    val items = itemCosts.keys
    val calculator = OrderCalculator(itemCosts)
    val orderHistory = rememberSaveable { mutableStateListOf<OrderHistory>() }
    Row {
        Menu(calculator, items, onPayment = {payment ->
            orderHistory.add(calculator.processOrder(payment))
        }, modifier = Modifier.weight(2.5f))
        OrderHistory(orderHistory, modifier = Modifier.weight(1f))
    }
}
@Composable
fun Menu(calculator: OrderCalculator, menuItems:Set<String>, onPayment:(payment:Payment) -> Unit, modifier: Modifier = Modifier){
    var total by rememberSaveable {mutableDoubleStateOf(0.00)}

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            for (menuItem in menuItems) {
                MenuItem(menuItem, onChange = { itemOrder ->
                    calculator.addOrUpdateOrder(itemOrder)
                    total = calculator.getTotal()
                })
            }
        }
        Totals(total, onPayment)
    }
}
@Composable
fun MenuItem(item: String, onChange:(itemOrder:ItemOrder) ->Unit){
    var amount by rememberSaveable { mutableIntStateOf(0) }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ){
        Text(item)
        Button(onClick = {
            amount++
            onChange(ItemOrder(item, amount))
        }) { Text("+") }
        Text("${amount}")
        Button(onClick = {
            if(amount>0){
                amount--
                onChange(ItemOrder(item, amount))
            }
        }) { Text("-") }
    }
}
@Composable
fun Totals(total:Double, onPayment:(payment:Payment) -> Unit){
    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        Spacer(modifier = Modifier.weight(0.15f))
        Row(
            modifier = Modifier.weight(0.7f),
            horizontalArrangement = Arrangement.SpaceBetween,
            ) {
            Payment("Cash", total, onClick = onPayment)
            Payment("Card", total * 1.026 + .30, onClick = onPayment)
        }
        Spacer(modifier = Modifier.weight(0.15f))
    }
}

@Composable
fun Payment(paymentType: String, amount: Double, onClick:(payment:Payment)->Unit) {
    val usFormat = NumberFormat.getCurrencyInstance(Locale.US)
    val amountStr = usFormat.format(amount)
    Button(
        onClick = { onClick(Payment(paymentType, amount)) }
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
