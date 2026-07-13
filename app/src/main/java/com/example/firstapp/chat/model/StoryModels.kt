package com.example.firstapp.chat.model

import com.example.firstapp.media.model.UserStory
import com.google.android.gms.ads.nativead.NativeAd

/**
 * STEP 2: Polymorphic Data Layer
 * Handles mixed lists of User Content and Advertisements.
 */
sealed class StoryItemWrapper {
    data class UserContent(val userStory: UserStory) : StoryItemWrapper()
    data class AdContent(val nativeAd: NativeAd, val id: String = "ad_${System.currentTimeMillis()}") : StoryItemWrapper()
}

object StoryAdInjector {
    /**
     * DYNAMIC INJECTION CALCULATION:
     * Takes raw user stories and drops an initialized AdStory placeholder
     * at defined thresholds (e.g., every 3rd or 4th position).
     */
    fun injectAds(stories: List<UserStory>, ads: List<NativeAd>, interval: Int = 3): List<StoryItemWrapper> {
        val result = mutableListOf<StoryItemWrapper>()
        var adIndex = 0
        
        stories.forEachIndexed { index, userStory ->
            result.add(StoryItemWrapper.UserContent(userStory))
            // Inject an ad every 'interval' stories if pre-warmed ads are available
            if ((index + 1) % interval == 0 && adIndex < ads.size) {
                result.add(StoryItemWrapper.AdContent(ads[adIndex]))
                adIndex++
            }
        }
        return result
    }
}