package com.example.ppb.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.ppb.data.model.Order
import kotlinx.coroutines.flow.Flow

@Dao
interface OrdersDao {
    @Query("SELECT * FROM orders")
    suspend fun getAll(): List<Order>

    @Query("select count(id) as id, UNIXEPOCH(STRFTIME('%Y-%m-%d %H:00:00', timestamp, 'unixepoch')) AS timestamp, SUM(adult) as adult, SUM(child) as child, sum(staff) as staff, sum(plane) as plane, sum(total_cents) as total_cents, 'All' as payment from orders group by STRFTIME('%Y-%m-%d %H:00:00', timestamp, 'unixepoch')")
    fun getOrderSummary(): Flow<List<Order>>

    @Insert
    suspend fun insert(order: Order)
}
