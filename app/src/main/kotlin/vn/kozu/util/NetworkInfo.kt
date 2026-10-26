package vn.kozu.util

import android.content.Context
import android.net.ConnectivityManager

object NetworkInfo {
    fun isMetered(context: Context): Boolean {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        return manager.isActiveNetworkMetered
    }
}
