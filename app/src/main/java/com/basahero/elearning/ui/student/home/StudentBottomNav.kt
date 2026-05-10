package com.basahero.elearning.ui.student.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 16.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GamifiedNavItem(
                selected = selectedTab == 0,
                icon = Icons.Default.Home,
                label = "Home",
                primaryColor = primaryColor,
                onClick = { onTabSelected(0) }
            )
            GamifiedNavItem(
                selected = selectedTab == 1,
                icon = Icons.Default.Widgets,
                label = "Quarters",
                primaryColor = primaryColor,
                onClick = { onTabSelected(1) }
            )
            GamifiedNavItem(
                selected = selectedTab == 2,
                icon = Icons.Default.SportsEsports,
                label = "Game",
                primaryColor = primaryColor,
                onClick = { onTabSelected(2) }
            )
            GamifiedNavItem(
                selected = selectedTab == 3,
                icon = Icons.Default.Person,
                label = "Profile",
                primaryColor = primaryColor,
                onClick = { onTabSelected(3) }
            )
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
    
    val targetScale = if (isPressed) 0.85f else if (selected) 1.05f else 1f
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "nav_scale"
    )
    
    val bgColor by animateColorAsState(
        targetValue = if (selected) primaryColor.copy(alpha = 0.15f) else Color.Transparent,
        label = "nav_bg"
    )
    
    val contentColor = if (selected) primaryColor else Color(0xFF94A3B8)
    
    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(24.dp))
            .background(bgColor)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = if (selected) 20.dp else 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(28.dp)
            )
            if (selected) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label,
                    color = contentColor,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp
                )
            }
        }
    }
}
