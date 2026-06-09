package com.example.ppb

import android.os.Bundle
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.VerticalAlignmentLine
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.ppb.ui.theme.PPBTheme
import java.nio.file.WatchEvent
import java.text.NumberFormat
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PPBTheme {
                MyApp()
            }
        }
    }
}

@Composable
fun ClickCounter(clicks: Int, onClick: () -> Unit) {
    Button(onClick = onClick) {
        Text("I've been clicked $clicks times")
    }
}

@Preview(showBackground = true)
@Composable
fun MyApp() {
    Row{
        Checkout(modifier = Modifier.weight(2.5f))
        OrderHistory(modifier = Modifier.weight(1f))
    }
}
@Composable
fun Checkout(modifier: Modifier = Modifier){
    Column(
        modifier = modifier
    ) {
        Menu()
        Totals(cash = 2400005.937, card = 0.015)
    }
}
@Composable
fun Menu(modifier: Modifier = Modifier){
    Row (
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ){
        MenuItem("Adult")
        MenuItem("Child")
        MenuItem("Staff")
        MenuItem("Plane")
    }
}
@Composable
fun MenuItem(item: String){
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ){
        Text(item)
        Button(onClick = {}) { Text("+") }
        Text("0")
        Button(onClick = {}) { Text("-") }
    }
}
@Composable
fun Totals(cash:Double, card:Double, modifier: Modifier = Modifier){
    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        Spacer(modifier = Modifier.weight(0.15f))
        Row(
            modifier = Modifier.weight(0.7f),
            horizontalArrangement = Arrangement.SpaceBetween,
            ) {
            Payment("Cash", cash)
            Payment("Card", card)
        }
        Spacer(modifier = Modifier.weight(0.15f))
    }
}

@Composable
fun Payment(paymentType: String, amount: Double, modifier: Modifier = Modifier) {
    val usFormat = NumberFormat.getCurrencyInstance(Locale.US);
    val amountStr = usFormat.format(amount)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ){
        Text(paymentType)
        Text(amountStr)
    }
}

@Composable
fun OrderHistory(modifier: Modifier = Modifier){
    Column(
        modifier = modifier
    ) {
        Button({}) { Text("Clear") }
        Text("History stuff here")

    }
}
