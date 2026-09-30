package com.tpsk.tj.chat.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardDoubleArrowDown
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tpsk.tj.common.ui.theme.AppAccent
import com.tpsk.tj.common.ui.theme.AppSecondary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun ScrollToBottomButton(
    state: LazyListState,
    scope: CoroutineScope,
    messageCount: Int,
    animated: Boolean
) {
    FloatingActionButton(
        onClick = {
            scope.launch {
                val index = (messageCount - 1).coerceAtLeast(0)
                if (animated) state.animateScrollToItem(index)
                else state.scrollToItem(index)
            }
        },
        modifier = Modifier.size(44.dp),
        containerColor = AppSecondary,
        contentColor = AppAccent,
        shape = CircleShape,
        elevation = FloatingActionButtonDefaults.elevation(8.dp)
    ) {
        Icon(Icons.Default.KeyboardDoubleArrowDown, null, modifier = Modifier.size(24.dp))
    }
}
