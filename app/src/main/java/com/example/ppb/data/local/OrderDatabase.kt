package com.example.ppb.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.ppb.data.model.Order

@Database(entities = [Order::class], version = 3)
@TypeConverters(Converters::class)
abstract class OrderDatabase : RoomDatabase() {
    abstract fun ordersDao(): OrdersDao
}
