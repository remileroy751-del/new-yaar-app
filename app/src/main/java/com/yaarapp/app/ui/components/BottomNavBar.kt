package com.yaarapp.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.yaarapp.app.nav.Routes

data class BottomNavItem(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

// Ordre demandé : Mon profil, Ma boutique, Acheter
val bottomNavItems = listOf(
    BottomNavItem(Routes.PROFILE, "Mon profil", Icons.Filled.Person),
    BottomNavItem(Routes.MY_SHOP, "Ma boutique", Icons.Filled.Storefront),
    BottomNavItem(Routes.MARKETPLACE, "Acheter", Icons.Filled.ShoppingBag)
)

@Composable
fun YaarBottomBar(
    currentRoute: String?,
    cartItemCount: Int,
    onNavigate: (String) -> Unit
) {
    NavigationBar {
        bottomNavItems.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = { onNavigate(item.route) },
                icon = {
                    if (item.route == Routes.MARKETPLACE && cartItemCount > 0) {
                        BadgedBox(badge = { Badge { Text(cartItemCount.toString()) } }) {
                            Icon(item.icon, contentDescription = item.label)
                        }
                    } else {
                        Icon(item.icon, contentDescription = item.label)
                    }
                },
                label = { Text(item.label) }
            )
        }
    }
}
