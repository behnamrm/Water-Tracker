package io.github.behnamrm.watertracker.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.behnamrm.watertracker.WaterApp
import io.github.behnamrm.watertracker.ui.home.HomeViewModel
import io.github.behnamrm.watertracker.ui.settings.SettingsViewModel
import io.github.behnamrm.watertracker.ui.stats.StatsViewModel

val AppViewModelFactory: ViewModelProvider.Factory = viewModelFactory {
    initializer { HomeViewModel(app().container.repository, app().container.settings) }
    initializer { StatsViewModel(app().container.repository, app().container.settings) }
    initializer {
        val c = app().container
        SettingsViewModel(app(), c.repository, c.settings, c.driveAuthorizer, c.driveBackup)
    }
}

private fun CreationExtras.app(): WaterApp =
    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as WaterApp
