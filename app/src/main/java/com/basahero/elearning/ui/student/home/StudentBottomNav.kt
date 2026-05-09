package com.basahero.elearning.ui.student.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = selectedTab == 0,
            onClick = { onTabSelected(0) },
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
            label = { Text("Home", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = primaryColor,
                selectedTextColor = primaryColor,
                indicatorColor = primaryColor.copy(alpha = 0.1f)
            )
        )
        NavigationBarItem(
            selected = selectedTab == 1,
            onClick = { onTabSelected(1) },
            icon = { Icon(Icons.Default.Widgets, contentDescription = null) }, // Using Widgets icon for Quarters, or keeping MenuBook
            label = { Text("Quarters", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = primaryColor,
                selectedTextColor = primaryColor,
                indicatorColor = primaryColor.copy(alpha = 0.1f)
            )
        )
        NavigationBarItem(
            selected = selectedTab == 2,
            onClick = { onTabSelected(2) },
            icon = { Icon(Icons.Default.SportsEsports, contentDescription = null) },
            label = { Text("Game", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = primaryColor,
                selectedTextColor = primaryColor,
                indicatorColor = primaryColor.copy(alpha = 0.1f)
            )
        )
        NavigationBarItem(
            selected = selectedTab == 3,
            onClick = { onTabSelected(3) },
            icon = { Icon(Icons.Default.Person, contentDescription = null) },
            label = { Text("Profile", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = primaryColor,
                selectedTextColor = primaryColor,
                indicatorColor = primaryColor.copy(alpha = 0.1f)
            )
        )
    }
}
