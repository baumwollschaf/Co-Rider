package com.ralphdietrich.corider

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.ralphdietrich.corider.data.DataStoreAppPreferencesStore
import com.ralphdietrich.corider.data.DefaultRouteRepository
import com.ralphdietrich.corider.data.InternalGpxFileStore
import com.ralphdietrich.corider.domain.RouteRepository

private val Context.coRiderDataStore by preferencesDataStore(name = "co_rider_preferences")

class CoRiderApplication : Application() {
    val routeRepository: RouteRepository by lazy {
        DefaultRouteRepository(
            preferencesStore = DataStoreAppPreferencesStore(coRiderDataStore),
            fileStore = InternalGpxFileStore(this),
        )
    }
}
