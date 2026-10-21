package com.shohankhan.hisaabos

import android.app.Application
import com.shohankhan.hisaabos.di.AppContainer

/**
 * Application entry point. HisaabOS uses a lightweight, manually wired object
 * graph ([AppContainer]) instead of a DI framework: the graph is small, fully
 * local and easy to reason about.
 */
class HisaabApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
