package com.example.ppb.data.model

sealed interface UiEvent {
    data class ShowToast(val message: String) : UiEvent
}
