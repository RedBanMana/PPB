package com.example.ppb.ui.util

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.ui.text.input.KeyboardType

object DollarAmountTransformation : InputTransformation {
    // Automatically enforces a numeric keypad when the user taps the field
    override val keyboardOptions: KeyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Number
    )

    override fun TextFieldBuffer.transformInput() {
        val input = asCharSequence().toString()

        // Regex allows: empty string, just digits, or digits with a single optional decimal point up to 2 places
        val regex = Regex("^\\d*\\.?\\d{0,2}$")

        if (!regex.matches(input)) {
            // Rejects the last typed character if it breaks currency rules
            revertAllChanges()
        }
    }
}
