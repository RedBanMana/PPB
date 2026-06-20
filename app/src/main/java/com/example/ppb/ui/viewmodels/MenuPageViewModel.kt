package com.example.ppb.ui.viewmodels

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ppb.data.model.AppConfiguration
import com.example.ppb.data.model.MenuItem
import com.example.ppb.data.model.Order
import com.example.ppb.data.model.Payment
import com.example.ppb.data.model.UiEvent
import com.example.ppb.data.repository.ConfigurationRepository
import com.example.ppb.data.repository.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import kotlin.time.Clock

@HiltViewModel
class MenuPageViewModel @Inject constructor(
    private val repository: OrderRepository,
    private val configRepository: ConfigurationRepository
) : ViewModel() {

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
    val menuItems: List<MenuItem> = _menuItems

    var totalCents by mutableLongStateOf(0)
        private set
    var totalItems by mutableIntStateOf(0)
        private set

    var cardTotalCents by mutableLongStateOf(0)
        private set

    private var _currentConfig: AppConfiguration? = null

    init {
        val initialItems = listOf(
            MenuItem("Adult", 1000L),
            MenuItem("Child", 500L),
            MenuItem("Plane", 5000L),
            MenuItem("Staff", 0L)
        )
        _menuItems.addAll(initialItems)

        viewModelScope.launch {
            configRepository.configurationFlow.collect { config ->
                _currentConfig = config
                updateMenuItemsCosts(config)
            }
        }
    }

    private fun updateMenuItemsCosts(config: AppConfiguration) {
        fun String.toCents(): Long {
            val amount = this.toDoubleOrNull() ?: 0.0
            return (amount * 100).toLong()
        }

        _menuItems.replaceAll { item ->
            when (item.name) {
                "Adult" -> item.copy(costCents = config.adultPrice.toCents())
                "Child" -> item.copy(costCents = config.childPrice.toCents())
                "Plane" -> item.copy(costCents = config.planePrice.toCents())
                else -> item
            }
        }
        generateTotal()
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
        calculateCardTotal(total)
    }

    private fun calculateCardTotal(total: Long) {
        val config = _currentConfig ?: return
        if (total == 0L) {
            cardTotalCents = 0
            return
        }
        val percent = config.cardPercentFee.toDoubleOrNull() ?: 0.0
        val fixed = config.cardFixedFee.toDoubleOrNull() ?: 0.0
        val fee = (total * (percent / 100.0)) + (fixed * 100)
        cardTotalCents = total + fee.toLong()
    }

    fun exportOrder(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val content = repository.getAll()
            val success = saveOrdersToDownloads(context, content)
            _eventChannel.send(UiEvent.ShowToast(if (success) "Exported to Downloads" else "Export Failed"))
        }
    }

    private fun saveOrdersToDownloads(context: Context, orders: List<Order>): Boolean {
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
