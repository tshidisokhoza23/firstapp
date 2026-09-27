package com.tpsk.tj.media.data

import java.util.Collections

/** Session-only ids the viewer asked not to see again. Not a server delete. */
object HiddenPosts {
    val ids: MutableSet<String> = Collections.synchronizedSet(mutableSetOf())
}
