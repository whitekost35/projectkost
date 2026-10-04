package com.kost.project.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kost.project.R

enum class WorkerTab {
    ABSENSI, GAJIAN, KASBON
}

@Composable
fun WorkerBottomNavigation(
    selectedTab: WorkerTab,
    onTabSelected: (WorkerTab) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 20.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp),
            color = Color.White,
            shape = RoundedCornerShape(45.dp),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                WorkerNavItem(
                    label = "Absensi",
                    iconRes = R.drawable.icon_abseni_new,
                    isSelected = selectedTab == WorkerTab.ABSENSI,
                    activeColor = Color(0xFF007AFF),
                    onClick = { onTabSelected(WorkerTab.ABSENSI) }
                )
                WorkerNavItem(
                    label = "Gajian",
                    iconRes = R.drawable.icon_gajian_new,
                    isSelected = selectedTab == WorkerTab.GAJIAN,
                    activeColor = Color(0xFF34C759),
                    onClick = { onTabSelected(WorkerTab.GAJIAN) }
                )
                WorkerNavItem(
                    label = "Kasbon",
                    iconRes = R.drawable.icon_kasbon_new,
                    isSelected = selectedTab == WorkerTab.KASBON,
                    activeColor = Color(0xFFFF3B30),
                    onClick = { onTabSelected(WorkerTab.KASBON) }
                )
            }
        }
    }
}

@Composable
fun WorkerNavItem(
    label: String,
    iconRes: Int,
    isSelected: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    val activeBg = activeColor.copy(alpha = 0.1f)
    val inactiveColor = Color(0xFF8E8E93)
    val interactionSource = remember { MutableInteractionSource() }
    
    Column(
        modifier = Modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .width(90.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon with Pill/Circle Background
        Box(
            modifier = Modifier
                .size(width = 60.dp, height = 36.dp)
                .background(
                    color = if (isSelected) activeBg else Color.Transparent,
                    shape = RoundedCornerShape(18.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = label,
                tint = if (isSelected) activeColor else inactiveColor,
                modifier = Modifier.size(26.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(6.dp))
        
        // Label
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            ),
            color = if (isSelected) activeColor else inactiveColor
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Indicator Dot
        Box(
            modifier = Modifier
                .size(5.dp)
                .background(
                    color = if (isSelected) activeColor else Color.Transparent,
                    shape = CircleShape
                )
        )
    }
}
