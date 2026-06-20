package com.example.ppb.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.example.ppb.data.local.OrderDatabase
import com.example.ppb.data.local.OrdersDao
import com.example.ppb.data.repository.OrderRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): OrderDatabase {
        return Room.databaseBuilder(
            context,
            OrderDatabase::class.java,
            "order_database"
        )
            .fallbackToDestructiveMigration(true)
            .build()
    }

    @Provides
    fun provideOrdersDao(database: OrderDatabase): OrdersDao {
        return database.ordersDao()
    }

    @Provides
    @Singleton
    fun provideOrderRepository(ordersDao: OrdersDao): OrderRepository {
        return OrderRepository(ordersDao)
    }

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.dataStore
    }
}
