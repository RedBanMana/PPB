package com.example.ppb.ui.components

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ppb.data.model.Order
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.text.NumberFormat
import java.util.Locale

@Composable
fun OrderHistoryWrapper(
    order: List<Order>,
    onExport: (Context) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Column(modifier = modifier) {
        OrderHistory(order, Modifier.weight(1f))
        Row(
            Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = { onExport(context) }) {
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
        item {
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
