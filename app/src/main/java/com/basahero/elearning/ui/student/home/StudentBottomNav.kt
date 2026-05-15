package com.basahero.elearning.ui.student.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.basahero.elearning.ui.common.LocalAppStrings

@Composable
fun StudentBottomNavBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val strings = LocalAppStrings.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .padding(bottom = 8.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = primaryColor,
            shape = RoundedCornerShape(32.dp),
            shadowElevation = 12.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GamifiedNavItem(
                    selected = selectedTab == 0,
                    icon = Icons.Rounded.Cottage,
                    label = "Home",
                    primaryColor = primaryColor,
                    onClick = { onTabSelected(0) }
                )
                GamifiedNavItem(
                    selected = selectedTab == 1,
                    icon = Icons.Rounded.Extension,
                    label = "Quarters",
                    primaryColor = primaryColor,
                    onClick = { onTabSelected(1) }
                )
                GamifiedNavItem(
                    selected = selectedTab == 2,
                    icon = Icons.Rounded.VideogameAsset,
                    label = "Game",
                    primaryColor = primaryColor,
                    onClick = { onTabSelected(2) }
                )
                GamifiedNavItem(
                    selected = selectedTab == 3,
                    icon = Icons.Rounded.Face,
                    label = "Profile",
                    primaryColor = primaryColor,
                    onClick = { onTabSelected(3) }
                )
            }
        }
    }
}

@Composable
private fun GamifiedNavItem(
    selected: Boolean,
    icon: ImageVector,
    label: String,
    primaryColor: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val targetScale = if (isPressed) 0.85f else if (selected) 1.1f else 1f
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
        label = "nav_scale"
    )
    
    val bgColor by animateColorAsState(
        targetValue = if (selected) Color.White.copy(alpha = 0.35f) else Color.Transparent,
        label = "nav_bg"
    )
    
    val contentColor = if (selected) Color.White else Color.White.copy(alpha = 0.8f)
    
    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                color = contentColor,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Bold
            )
        }
    }
}
