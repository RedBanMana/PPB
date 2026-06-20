package com.example.ppb.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ppb.data.model.UiEvent
import com.example.ppb.ui.components.Menu
import com.example.ppb.ui.components.OrderHistoryWrapper
import com.example.ppb.ui.components.Totals
import com.example.ppb.ui.viewmodels.MenuPageViewModel

@Preview(showBackground = true, widthDp = 1280, heightDp = 800)
@Composable
fun MenuScreenPreview() {
    MenuScreen(onNavConfig = {})
}

@Composable
fun MenuScreen(onNavConfig: () -> Unit = {}, menuPageViewModel: MenuPageViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val orders by menuPageViewModel.orders.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        menuPageViewModel.eventFlow.collect { event ->
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
                menuItems = menuPageViewModel.menuItems,
                onChange = { itemCount -> menuPageViewModel.updateOrder(itemCount) }
            )
            Spacer(modifier = Modifier.padding(vertical = 18.dp))
            Totals(
                menuPageViewModel.totalCents,
                menuPageViewModel.cardTotalCents,
                menuPageViewModel.totalItems,
                onPayment = { payment -> menuPageViewModel.executeOrder(payment) }
            )
            Spacer(modifier = Modifier.weight(1f))
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { menuPageViewModel.clearOrders() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red,
                        contentColor = Color.Black
                    )
                ) {
                    Text("Clear Order", style = MaterialTheme.typography.headlineMedium)
                }
                Spacer(modifier = Modifier.weight(1f))
                Button(onClick = onNavConfig) {
                    Text("Config", style = MaterialTheme.typography.headlineMedium)
                }
            }
        }
        VerticalDivider()
        OrderHistoryWrapper(
            orders,
            onExport = { ctx -> menuPageViewModel.exportOrder(ctx) },
            modifier = Modifier.weight(2f)
        )
    }
}
