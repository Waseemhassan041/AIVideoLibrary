package com.ainest.aivideolibrary.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ainest.aivideolibrary.data.DashboardFilter
import com.ainest.aivideolibrary.viewmodel.DashboardStats

private data class Stat(val label: String, val value: Int, val color: Color, val filter: DashboardFilter)

@Composable
fun DashboardRow(
    stats: DashboardStats,
    activeFilter: DashboardFilter,
    onFilterClick: (DashboardFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        Stat("Total", stats.total, Color(0xFF7C4DFF), DashboardFilter.NONE),
        Stat("FB", stats.facebook, Color(0xFF1877F2), DashboardFilter.FACEBOOK),
        Stat("TT", stats.tiktok, Color(0xFF111111), DashboardFilter.TIKTOK),
        Stat("YT", stats.youtube, Color(0xFFFF0000), DashboardFilter.YOUTUBE),
        Stat("IG", stats.instagram, Color(0xFFC13584), DashboardFilter.INSTAGRAM),
        Stat("Fav", stats.favorites, Color(0xFFE74C3C), DashboardFilter.FAVORITES)
    )

    Row(
        modifier = modifier.padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items.forEach { stat ->
            val isSelected = if (stat.filter == DashboardFilter.NONE) activeFilter == DashboardFilter.NONE else activeFilter == stat.filter
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSelected && stat.filter != DashboardFilter.NONE)
                            stat.color.copy(alpha = 0.85f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                    .clickable { onFilterClick(stat.filter) }
                    .padding(vertical = 8.dp, horizontal = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stat.value.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected && stat.filter != DashboardFilter.NONE) Color.White else stat.color,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stat.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected && stat.filter != DashboardFilter.NONE) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
