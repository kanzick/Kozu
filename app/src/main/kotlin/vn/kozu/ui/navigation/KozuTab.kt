package vn.kozu.ui.navigation

import androidx.annotation.StringRes
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
import vn.kozu.R

enum class KozuTab(
    @StringRes val titleRes: Int,
    val icon: ImageVector,
    val outlinedIcon: ImageVector,
    val inPill: Boolean = true,
) {
    HOME(R.string.home, Icons.Rounded.Home, Icons.Outlined.Home),
    EXPLORE(R.string.explore, Icons.Rounded.Explore, Icons.Outlined.Explore),
    LIBRARY(R.string.library, Icons.Rounded.LibraryMusic, Icons.Outlined.LibraryMusic),
    GAME(R.string.game, Icons.Rounded.SportsEsports, Icons.Outlined.SportsEsports),
    SEARCH(R.string.search, Icons.Rounded.Search, Icons.Outlined.Search, inPill = false),
}
