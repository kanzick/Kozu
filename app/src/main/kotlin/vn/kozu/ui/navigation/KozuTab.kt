package vn.kozu.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.ui.graphics.vector.ImageVector

enum class KozuTab(val title: String, val icon: ImageVector) {
    HOME("Home", Icons.Rounded.Home),
    EXPLORE("Explore", Icons.Rounded.Explore),
    SEARCH("Search", Icons.Rounded.Search),
    LIBRARY("Library", Icons.Rounded.LibraryMusic),
    GAME("Play as game", Icons.Rounded.SportsEsports)
}
