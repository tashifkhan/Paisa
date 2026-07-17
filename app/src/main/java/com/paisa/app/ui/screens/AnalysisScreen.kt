package com.paisa.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
fun AnalysisScreen(viewModel: PaisaViewModel) {
    val transactions by viewModel.transactions.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        PaisaTopBar(title = "Analytics")
        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(MaterialTheme.spacing.medium),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    title = "No analytics yet",
                    subtitle = "Add a few transactions to see spending insights"
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(MaterialTheme.spacing.medium),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${transactions.size} transactions ready for charts",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}
