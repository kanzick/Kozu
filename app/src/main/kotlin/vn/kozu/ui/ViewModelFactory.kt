package vn.kozu.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import vn.kozu.di.AppContainer
import vn.kozu.KozuApplication

internal fun CreationExtras.container(): AppContainer = (this[APPLICATION_KEY] as KozuApplication).container

internal inline fun <reified VM : ViewModel> containerFactory(crossinline create: (AppContainer) -> VM) =
    viewModelFactory {
        initializer { create(container()) }
    }
