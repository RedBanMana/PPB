package com.example.ppb.data.model

sealed interface Screens {
    data object Menu : Screens
    data object Config : Screens
}
