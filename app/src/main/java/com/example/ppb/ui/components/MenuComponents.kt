package com.example.ppb.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ppb.data.model.MenuItem
import com.example.ppb.data.model.Payment
import com.example.ppb.ui.util.centsToCostStr

@Composable
fun Menu(menuItems: List<MenuItem>, onChange: (itemCount: MenuItem) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround,
    ) {
        for (menuItem in menuItems) {
            MenuItemComponent(menuItem, onChange = onChange, Modifier.weight(1f))
        }
    }
}

@Composable
fun Totals(totalCents: Long, cardTotalCents: Long, totalItems: Int, onPayment: (payment: Payment) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround,
    ) {
        PaymentComponent(
            "Cash",
            totalCents,
            onClick = onPayment,
            modifier = Modifier.weight(1f),
            enabled = totalItems > 0
        )
        PaymentComponent(
            "Card",
            cardTotalCents,
            onClick = onPayment,
            modifier = Modifier.weight(1f),
            enabled = totalItems > 0 && cardTotalCents > 0
        )
    }
}

@Composable
fun MenuItemComponent(
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
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        Text(
            "${menuItem.count}",
            style = MaterialTheme.typography.displayLarge,
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
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}

@Composable
fun PaymentComponent(
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
