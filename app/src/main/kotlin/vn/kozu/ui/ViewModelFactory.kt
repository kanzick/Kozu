package vn.kozu.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.ViewModel
import vn.kozu.KozuApplication
import vn.kozu.di.AppContainer

internal fun CreationExtras.container(): AppContainer =
    (this[APPLICATION_KEY] as KozuApplication).container


internal inline fun <reified VM : ViewModel> containerFactory(
    crossinline create: (AppContainer) -> VM
) = viewModelFactory {
    initializer { create(container()) }
}
