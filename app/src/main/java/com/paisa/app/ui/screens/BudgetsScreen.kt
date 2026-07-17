package com.paisa.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.paisa.app.data.PaisaViewModel
import com.paisa.app.ui.components.EmptyState
import com.paisa.app.ui.components.PaisaTopBar
import com.paisa.app.ui.theme.spacing

@Composable
fun BudgetsScreen(viewModel: PaisaViewModel) {
    val budgets by viewModel.budgets.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        PaisaTopBar(title = "Budgets")
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(MaterialTheme.spacing.medium),
            contentAlignment = Alignment.Center
        ) {
            if (budgets.isEmpty()) {
                EmptyState(
                    title = "No budgets yet",
                    subtitle = "Set monthly limits to stay on track"
                )
            } else {
                EmptyState(
                    title = "${budgets.size} budget(s)",
                    subtitle = "Budget detail UI coming next"
                )
            }
        }
    }
}
