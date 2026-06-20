package com.example.ppb.ui.viewmodels

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ppb.data.model.AppConfiguration
import com.example.ppb.data.model.UiEvent
import com.example.ppb.data.repository.ConfigurationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConfigurationViewModel @Inject constructor(
    private val repository: ConfigurationRepository
) : ViewModel() {

    private val _eventChannel = Channel<UiEvent>()
    val eventFlow = _eventChannel.receiveAsFlow()

    val adultPrice = TextFieldState()
    val childPrice = TextFieldState()
    val planePrice = TextFieldState()
    val cardPercentFee = TextFieldState()
    val cardFixedFee = TextFieldState()
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()


    fun loadConfiguration() {
        viewModelScope.launch {
            val config = repository.configurationFlow.first()
            adultPrice.setTextAndPlaceCursorAtEnd(config.adultPrice)
            childPrice.setTextAndPlaceCursorAtEnd(config.childPrice)
            planePrice.setTextAndPlaceCursorAtEnd(config.planePrice)
            cardPercentFee.setTextAndPlaceCursorAtEnd(config.cardPercentFee)
            cardFixedFee.setTextAndPlaceCursorAtEnd(config.cardFixedFee)
        }
    }

    fun saveConfiguration() {
        viewModelScope.launch {
            repository.updateConfiguration(
                AppConfiguration(
                    adultPrice = adultPrice.text.toString(),
                    childPrice = childPrice.text.toString(),
                    planePrice = planePrice.text.toString(),
                    cardPercentFee = cardPercentFee.text.toString(),
                    cardFixedFee = cardFixedFee.text.toString()
                )
            )
            _eventChannel.send(UiEvent.ShowToast("Configuration Saved"))
        }
    }
}
