package com.kost.project.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface NavKey {
    @Serializable
    data object Dashboard : NavKey

    @Serializable
    data class Classification(val periodId: String) : NavKey

    @Serializable
    data class BumimasClassification(val periodId: String) : NavKey

    @Serializable
    data class TransactionDetail(
        val periodId: String,
        val category: String? = null
    ) : NavKey

    @Serializable
    data class Attendance(val periodId: String) : NavKey

    @Serializable
    data class SalarySummary(val periodId: String) : NavKey

    @Serializable
    data class PrivateKasbon(val workerId: String? = null) : NavKey
}
