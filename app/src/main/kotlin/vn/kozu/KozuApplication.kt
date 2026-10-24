package vn.kozu

import android.app.Application
import vn.kozu.di.AppContainer

class KozuApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
