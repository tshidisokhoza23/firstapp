package com.tpsk.tj.media.model

import androidx.compose.runtime.Immutable

@Immutable
data class FlattenedThreadItem(
    val post: Post,
    val depth: Int,
    /** True when this item is the last reply in its parent's list */
    val isLastInBranch: Boolean,
    /** Whether a vertical connector should continue above this item */
    val hasVerticalLineAbove: Boolean,
    /** Whether a vertical connector should continue below this item */
    val hasVerticalLineBelow: Boolean,
    /** Whether this item itself has children (used for small visual cues) */
    val hasChildren: Boolean
)

/**
 * Flattens a recursive tree into a linear list suitable for LazyColumn
 * while preserving all metadata needed for connector-line drawing.
 */
fun flattenThread(
    root: Post,
    maxDepth: Int = 5
): List<FlattenedThreadItem> {
    val result = mutableListOf<FlattenedThreadItem>()
    val seenIds = mutableSetOf<String>()

    fun walk(
        post: Post,
        depth: Int,
        isLastSibling: Boolean
    ) {
        if (seenIds.contains(post.id)) return
        seenIds.add(post.id)

        val clampedDepth = depth.coerceAtMost(maxDepth)
        val hasChildren = post.replies.isNotEmpty()
        
        // 1. Bottom line: Only if it HAS children AND we want to show connection.
        // Requirement: No vertical thread line from main post to direct replies.
        // Main Post is depth 0. Direct replies are depth 1.
        // So depth 0 should NOT have a line down to depth 1.
        // Nested replies (depth 1+) SHOULD have a line down if they have children.
        val hasVerticalLineBelow = hasChildren && depth > 0

        // 2. Top line: Only if it is a nested reply (depth > 1).
        // Direct replies (depth 1) should have NO top line.
        val hasVerticalLineAbove = depth > 1

        result += FlattenedThreadItem(
            post = post,
            depth = clampedDepth,
            isLastInBranch = isLastSibling,
            hasVerticalLineAbove = hasVerticalLineAbove,
            hasVerticalLineBelow = hasVerticalLineBelow,
            hasChildren = hasChildren
        )

        post.replies.forEachIndexed { index, child ->
            val childIsLast = index == post.replies.lastIndex
            walk(child, depth + 1, childIsLast)
        }
    }

    walk(root, depth = 0, isLastSibling = true)
    return result
}
