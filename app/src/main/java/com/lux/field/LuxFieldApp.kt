package com.lux.field

import android.app.Application
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class LuxFieldApp : Application() {

    override fun onCreate() {
        super.onCreate()
        MapboxNavigationApp.setup(
            NavigationOptions.Builder(this)
                .accessToken(BuildConfig.MAPBOX_PUBLIC_TOKEN)
                .build()
        )
    }
}
