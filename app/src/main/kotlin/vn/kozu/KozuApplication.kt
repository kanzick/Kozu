package vn.kozu

import android.app.Application
import vn.kozu.di.AppContainer

/**
 * Main Application class for Kozu.
 * Holds the singleton instance of [AppContainer] for dependency injection across the app.
 */
class KozuApplication : Application() {
    /** Lazily initialized dependency container holding core app services and stores. */
    val container: AppContainer by lazy { AppContainer(this) }
}
