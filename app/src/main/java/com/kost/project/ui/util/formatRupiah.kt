package com.kost.project.ui.util

import java.text.NumberFormat
import java.util.Locale

fun formatRupiah(amount: Double): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    return formatter.format(amount).replace("Rp", "Rp ").replace(",00", "")
}

fun formatCompactRupiah(amount: Double): String {
    if (amount == 0.0) return "Rp 0"
    return if (amount >= 1000) {
        "Rp ${(amount / 1000).toInt()} rb"
    } else {
        "Rp ${amount.toInt()}"
    }
}
