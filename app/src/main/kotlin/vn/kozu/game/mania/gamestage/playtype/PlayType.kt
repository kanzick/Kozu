package vn.kozu.game.mania.gamestage.playtype

sealed class PlayType(
    val storageKey: String,
    val isLandscape: Boolean,
) {
    companion object {
        fun fromStorageKey(key: String?): PlayType? = when (key) {
            Portrait.storageKey -> Portrait
            LandscapeVerticalScrolling.storageKey -> LandscapeVerticalScrolling
            Landscape3DPerspective.storageKey -> Landscape3DPerspective
            else -> null
        }
    }
}
