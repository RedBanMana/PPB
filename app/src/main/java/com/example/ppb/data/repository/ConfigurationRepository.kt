package com.example.ppb.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.ppb.data.model.AppConfiguration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConfigurationRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private object PreferencesKeys {
        val ADULT_PRICE = stringPreferencesKey("adult_price")
        val CHILD_PRICE = stringPreferencesKey("child_price")
        val PLANE_PRICE = stringPreferencesKey("plane_price")
        val CARD_PERCENT_FEE = stringPreferencesKey("card_percent_fee")
        val CARD_FIXED_FEE = stringPreferencesKey("card_fixed_fee")
    }

    val configurationFlow: Flow<AppConfiguration> = dataStore.data.map { preferences ->
        AppConfiguration(
            adultPrice = preferences[PreferencesKeys.ADULT_PRICE] ?: "10",
            childPrice = preferences[PreferencesKeys.CHILD_PRICE] ?: "5",
            planePrice = preferences[PreferencesKeys.PLANE_PRICE] ?: "50",
            cardPercentFee = preferences[PreferencesKeys.CARD_PERCENT_FEE] ?: "3",
            cardFixedFee = preferences[PreferencesKeys.CARD_FIXED_FEE] ?: "0"
        )
    }

    suspend fun updateConfiguration(config: AppConfiguration) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ADULT_PRICE] = config.adultPrice
            preferences[PreferencesKeys.CHILD_PRICE] = config.childPrice
            preferences[PreferencesKeys.PLANE_PRICE] = config.planePrice
            preferences[PreferencesKeys.CARD_PERCENT_FEE] = config.cardPercentFee
            preferences[PreferencesKeys.CARD_FIXED_FEE] = config.cardFixedFee
        }
    }
}
