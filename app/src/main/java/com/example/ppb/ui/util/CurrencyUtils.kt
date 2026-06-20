package com.example.ppb.ui.util

import java.text.NumberFormat
import java.util.Locale

fun centsToCostStr(totalCents: Long): String {
    val usFormat = NumberFormat.getCurrencyInstance(Locale.US)
    val amount = totalCents / 100.0
    val amountStr = usFormat.format(amount)
    return amountStr
}
