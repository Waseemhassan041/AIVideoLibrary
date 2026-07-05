package com.ainest.aivideolibrary.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ainest.aivideolibrary.data.SortOption
import com.ainest.aivideolibrary.viewmodel.FilterState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterSortSheet(
    filterState: FilterState,
    sortOption: SortOption,
    categories: List<String>,
    aiModels: List<String>,
    onFilterChange: (FilterState) -> Unit,
    onSortChange: (SortOption) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text("Sort By", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            FlowRow(
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SortOption.entries.forEach { option ->
                    FilterChip(
                        selected = sortOption == option,
                        onClick = { onSortChange(option) },
                        label = { Text(option.label) }
                    )
                }
            }

            Divider()

            if (categories.isNotEmpty()) {
                Text("Category", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    categories.forEach { c ->
                        FilterChip(
                            selected = filterState.category == c,
                            onClick = {
                                onFilterChange(filterState.copy(category = if (filterState.category == c) null else c))
                            },
                            label = { Text(c) }
                        )
                    }
                }
            }

            if (aiModels.isNotEmpty()) {
                Text("AI Model", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    aiModels.forEach { m ->
                        FilterChip(
                            selected = filterState.aiModel == m,
                            onClick = {
                                onFilterChange(filterState.copy(aiModel = if (filterState.aiModel == m) null else m))
                            },
                            label = { Text(m) }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { onFilterChange(FilterState()) }) { Text("Clear Filters") }
                TextButton(onClick = onDismiss) { Text("Done") }
            }
        }
    }
}
