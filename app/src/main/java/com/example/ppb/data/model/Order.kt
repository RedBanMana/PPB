package com.example.ppb.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import kotlin.time.Instant

@Serializable(with = Order.Companion::class)
@Entity(tableName = "orders")
data class Order(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "timestamp") val timestamp: Instant,
    @ColumnInfo(name = "adult") val adult: Int,
    @ColumnInfo(name = "child") val child: Int,
    @ColumnInfo(name = "staff") val staff: Int,
    @ColumnInfo(name = "plane") val plane: Int,
    @ColumnInfo(name = "total_cents") val totalCents: Long,
    @ColumnInfo(name = "payment") val payment: String
) {
    companion object : KSerializer<Order> {

        // 1. Define the structural schema for the JSON payload
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Order") {
            element<Int>("id")
            element<String>("timestamp")
            element<Int>("adult")
            element<Int>("child")
            element<Int>("staff")
            element<Int>("plane")
            element<Long>("total_cents")
            element<String>("payment")
        }

        override fun serialize(encoder: Encoder, value: Order) {
            encoder.encodeStructure(descriptor) {
                encodeIntElement(descriptor, 0, value.id)
                // Convert Instant directly to ISO-8601 string (e.g., "2026-06-15T20:22:00Z")
                encodeStringElement(descriptor, 1, value.timestamp.toString())
                encodeIntElement(descriptor, 2, value.adult)
                encodeIntElement(descriptor, 3, value.child)
                encodeIntElement(descriptor, 4, value.staff)
                encodeIntElement(descriptor, 5, value.plane)
                encodeLongElement(descriptor, 6, value.totalCents)
                encodeStringElement(descriptor, 7, value.payment)
            }
        }

        override fun deserialize(decoder: Decoder): Order {
            return decoder.decodeStructure(descriptor) {
                var id = 0
                var timestampStr = ""
                var adult = 0
                var child = 0
                var staff = 0
                var plane = 0
                var totalCents = 0L
                var payment = ""

                while (true) {
                    when (val index = decodeElementIndex(descriptor)) {
                        0 -> id = decodeIntElement(descriptor, 0)
                        1 -> timestampStr = decodeStringElement(descriptor, 1)
                        2 -> adult = decodeIntElement(descriptor, 2)
                        3 -> child = decodeIntElement(descriptor, 3)
                        4 -> staff = decodeIntElement(descriptor, 4)
                        5 -> plane = decodeIntElement(descriptor, 5)
                        6 -> totalCents = decodeLongElement(descriptor, 6)
                        7 -> payment = decodeStringElement(descriptor, 7)
                        CompositeDecoder.DECODE_DONE -> break
                        else -> error("Unknown indexing constraint: $index")
                    }
                }

                Order(
                    id = id,
                    timestamp = Instant.parse(timestampStr),
                    adult = adult,
                    child = child,
                    staff = staff,
                    plane = plane,
                    totalCents = totalCents,
                    payment = payment
                )
            }
        }
    }
}
