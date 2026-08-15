package com.neon.emulator.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neon.emulator.model.OpenTab

@Composable
fun TabsBar(
    openTabs: List<OpenTab>,
    activeTabId: String,
    onTabSelected: (String) -> Unit,
    onCloseTab: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .background(Color(0xFF0F172A))
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(openTabs) { tab ->
            val isActive = (tab.id == activeTabId)
            Surface(
                modifier = Modifier
                    .padding(end = 4.dp)
                    .clickable { onTabSelected(tab.id) },
                color = if (isActive) Color(0xFF1E293B) else Color(0xFF0B0F19),
                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                border = if (isActive) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8)) else null
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (tab.isEmulator) Icons.Default.PhoneAndroid else Icons.Default.InsertDriveFile,
                        contentDescription = null,
                        tint = if (tab.isEmulator) Color(0xFF10B981) else Color(0xFFEC4899),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = tab.title,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                    )

                    if (!tab.isEmulator) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar Pestaña",
                            tint = Color.Gray,
                            modifier = Modifier
                                .size(14.dp)
                                .clickable { onCloseTab(tab.id) }
                        )
                    }
                }
            }
        }
    }
}
