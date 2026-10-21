package com.shohankhan.hisaabos.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer was not provided")
}

@Composable
inline fun <reified T : ViewModel> appViewModel(crossinline create: (AppContainer) -> T): T {
    val container = LocalAppContainer.current
    return viewModel(
        factory = viewModelFactory {
            initializer { create(container) }
        },
    )
}

/** Same as [appViewModel] but scoped to a stable key, e.g. one per record id. */
@Composable
inline fun <reified T : ViewModel> appViewModel(key: String, crossinline create: (AppContainer) -> T): T {
    val container = LocalAppContainer.current
    return viewModel(
        key = key,
        factory = viewModelFactory {
            initializer { create(container) }
        },
    )
}
