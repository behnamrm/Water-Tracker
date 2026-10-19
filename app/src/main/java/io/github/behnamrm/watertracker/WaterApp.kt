package io.github.behnamrm.watertracker

import android.app.Application
import android.content.Context
import io.github.behnamrm.watertracker.data.SettingsRepository
import io.github.behnamrm.watertracker.data.WaterDatabase
import io.github.behnamrm.watertracker.data.WaterRepository

class WaterApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/** Simple manual dependency container shared by the whole app. */
class AppContainer(context: Context) {
    val settings = SettingsRepository(context)
    val repository = WaterRepository(WaterDatabase.build(context), settings)
}
