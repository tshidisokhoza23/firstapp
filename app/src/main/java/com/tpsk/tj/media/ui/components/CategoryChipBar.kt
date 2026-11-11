package com.tpsk.tj.media.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tpsk.tj.common.ui.components.AppFilterChip
import com.tpsk.tj.media.model.Category

@Composable
fun CategoryChipBar(
    selectedCategoryId: String?,
    onCategorySelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
    onTrendingClick: () -> Unit = {},
    onContestsClick: () -> Unit = {}
) {
    val primaryTabs = remember {
        listOf(
            Category("for_you", "For You"),
            Category("following", "Following"),
            Category("trending", "Trending"),
            Category("contests", "🏆 Contests")
        )
    }

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(primaryTabs) { category ->
            val activeTab = selectedCategoryId ?: "for_you"
            val isSelected = activeTab == category.id

            AppFilterChip(
                text = category.name,
                isSelected = isSelected,
                onClick = {
                    when (category.id) {
                        "trending" -> onTrendingClick()
                        "contests" -> onContestsClick()
                        else -> onCategorySelected(category.id)
                    }
                }
            )
        }
    }
}
