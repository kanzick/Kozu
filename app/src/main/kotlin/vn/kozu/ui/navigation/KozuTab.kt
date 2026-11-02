package vn.kozu.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.ui.graphics.vector.ImageVector

enum class KozuTab(
    val title: String,
    val icon: ImageVector,
    val outlinedIcon: ImageVector,
    val inPill: Boolean = true,
) {
    HOME("Home", Icons.Rounded.Home, Icons.Outlined.Home),
    EXPLORE("Explore", Icons.Rounded.Explore, Icons.Outlined.Explore),
    LIBRARY("Library", Icons.Rounded.LibraryMusic, Icons.Outlined.LibraryMusic),
    GAME("Game", Icons.Rounded.SportsEsports, Icons.Outlined.SportsEsports),
    SEARCH("Search", Icons.Rounded.Search, Icons.Outlined.Search, inPill = false),
}
