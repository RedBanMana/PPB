package com.example.ppb.data.repository

import com.example.ppb.data.local.OrdersDao
import com.example.ppb.data.model.Order
import kotlinx.coroutines.flow.Flow

class OrderRepository(private val ordersDao: OrdersDao) {
    suspend fun getAll(): List<Order> = ordersDao.getAll()

    val orderSummary: Flow<List<Order>> = ordersDao.getOrderSummary()
    suspend fun insert(order: Order) {
        ordersDao.insert(order)
    }
}
