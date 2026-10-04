package com.kost.project

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.kost.project.ui.navigation.NavKey
import com.kost.project.ui.screen.DashboardScreen
import com.kost.project.ui.screen.TransactionDetailScreen
import com.kost.project.ui.theme.ProjectKostTheme
import com.kost.project.ui.viewmodel.MainViewModel

import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ProjectKostTheme {
                val viewModel: MainViewModel = viewModel()
                AppNavigation(viewModel)
            }
        }
    }
}

@Composable
fun AppNavigation(viewModel: MainViewModel) {
    val backStack = remember { mutableStateListOf<NavKey>(NavKey.Dashboard) }

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = { key: NavKey ->
            when (key) {
                is NavKey.Dashboard -> NavEntry(key) {
                    DashboardScreen(
                        viewModel = viewModel,
                        onPeriodClick = { periodId ->
                            if (periodId == "p4") {
                                backStack.add(NavKey.Classification(periodId))
                            } else {
                                backStack.add(NavKey.TransactionDetail(periodId))
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is NavKey.Classification -> NavEntry(key) {
                    com.kost.project.ui.screen.ClassificationScreen(
                        periodId = key.periodId,
                        viewModel = viewModel,
                        onBack = { backStack.removeLastOrNull() },
                        onCategoryClick = { category ->
                            if (category == "Bumimas") {
                                backStack.add(NavKey.BumimasClassification(key.periodId))
                            } else {
                                backStack.add(NavKey.TransactionDetail(key.periodId, category))
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is NavKey.BumimasClassification -> NavEntry(key) {
                    com.kost.project.ui.screen.BumimasClassificationScreen(
                        periodId = key.periodId,
                        viewModel = viewModel,
                        onBack = { backStack.removeLastOrNull() },
                        onNavigateToDetail = { subCategory ->
                            backStack.add(NavKey.TransactionDetail(key.periodId, subCategory))
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is NavKey.TransactionDetail -> NavEntry(key) {
                    TransactionDetailScreen(
                        periodId = key.periodId,
                        category = key.category,
                        viewModel = viewModel,
                        onBack = { backStack.removeLastOrNull() },
                        onAttendanceClick = {
                            backStack.add(NavKey.Attendance(key.periodId))
                        },
                        onSalarySummaryClick = {
                            backStack.add(NavKey.SalarySummary(key.periodId))
                        },
                        onPrivateKasbonClick = {
                            // Gondes is a category, we can find a worker named "Gondes" 
                            // or just show the ledger screen which handles finding the worker.
                            backStack.add(NavKey.PrivateKasbon())
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is NavKey.Attendance -> NavEntry(key) {
                    com.kost.project.ui.screen.AttendanceScreen(
                        periodId = key.periodId,
                        onBack = { backStack.removeAt(backStack.lastIndex) },
                        onNavigateToSalary = {
                            backStack.removeAt(backStack.lastIndex)
                            backStack.add(NavKey.SalarySummary(key.periodId))
                        },
                        onNavigateToKasbon = {
                            backStack.removeAt(backStack.lastIndex)
                            backStack.add(NavKey.PrivateKasbon())
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is NavKey.SalarySummary -> NavEntry(key) {
                    com.kost.project.ui.screen.SalarySummaryScreen(
                        periodId = key.periodId,
                        onBack = { backStack.removeAt(backStack.lastIndex) },
                        onNavigateToAttendance = {
                            backStack.removeAt(backStack.lastIndex)
                            backStack.add(NavKey.Attendance(key.periodId))
                        },
                        onNavigateToKasbon = {
                            backStack.removeAt(backStack.lastIndex)
                            backStack.add(NavKey.PrivateKasbon())
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is NavKey.PrivateKasbon -> NavEntry(key) {
                    com.kost.project.ui.screen.PrivateKasbonScreen(
                        workerId = key.workerId,
                        onBack = { backStack.removeAt(backStack.lastIndex) },
                        onNavigateToAttendance = {
                            backStack.removeAt(backStack.lastIndex)
                            backStack.add(NavKey.Attendance("p4"))
                        },
                        onNavigateToSalary = {
                            backStack.removeAt(backStack.lastIndex)
                            backStack.add(NavKey.SalarySummary("p4"))
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DashboardPreview() {
    ProjectKostTheme {
        DashboardScreen(
            viewModel = viewModel(),
            onPeriodClick = {}
        )
    }
}
