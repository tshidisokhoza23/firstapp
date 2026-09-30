package com.tpsk.tj.media.ui.screens.adverts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tpsk.tj.chat.model.Advert
import com.tpsk.tj.media.ui.components.AdvertItem
import com.tpsk.tj.common.ui.theme.AppAccent

@Composable
fun AdvertList(
    adverts: List<Advert>, 
    viewedAdverts: List<Advert> = java.util.Collections.emptyList(),
    header: @Composable () -> Unit = {}, 
    onAdvertClick: (Advert) -> Unit
) {
    if (adverts.size == 0 && viewedAdverts.size == 0) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                Icon(Icons.Default.Celebration, null, tint = AppAccent, modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(16.dp))
                Text("You're all caught up!", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
            item { header() }
            if (adverts.size > 0) {
                item {
                    Text(
                        "New Adverts",
                        color = AppAccent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                items(adverts, key = { "new_" + it.id }) { ad -> 
                    Column(modifier = Modifier.clickable { onAdvertClick(ad) }) { 
                        AdvertItem(ad)
                        HorizontalDivider(modifier = Modifier.padding(start = 88.dp), thickness = 0.5.dp, color = Color(0xFF233138)) 
                    } 
                }
            }
            if (viewedAdverts.size > 0) {
                item {
                    Text(
                        "Viewed Adverts",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                items(viewedAdverts, key = { "viewed_" + it.id }) { ad ->
                    Column(modifier = Modifier.clickable { onAdvertClick(ad) }.alpha(0.6f)) { 
                        AdvertItem(ad)
                        HorizontalDivider(modifier = Modifier.padding(start = 88.dp), thickness = 0.5.dp, color = Color(0xFF233138)) 
                    } 
                }
            }
        }
    }
}
