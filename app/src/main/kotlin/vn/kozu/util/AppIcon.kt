package vn.kozu.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

enum class AppIcon(val alias: String, val label: String) {
    DEFAULT("IconDefault", "Sunset"),
    MIDNIGHT("IconMidnight", "Midnight"),
    MINT("IconMint", "Mint"),
    VIOLET("IconViolet", "Violet");

    companion object {
        fun current(context: Context): AppIcon {
            val pm = context.packageManager
            return entries.firstOrNull { icon ->
                val state = pm.getComponentEnabledSetting(component(context, icon))
                state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED ||
                    (icon == DEFAULT && state == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT)
            } ?: DEFAULT
        }

        fun apply(context: Context, target: AppIcon) {
            val pm = context.packageManager
            entries.forEach { icon ->
                val state = if (icon == target) {
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                } else {
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                }
                pm.setComponentEnabledSetting(component(context, icon), state, PackageManager.DONT_KILL_APP)
            }
        }

        private fun component(context: Context, icon: AppIcon) =
            ComponentName(context.packageName, "vn.kozu.${icon.alias}")
    }
}
