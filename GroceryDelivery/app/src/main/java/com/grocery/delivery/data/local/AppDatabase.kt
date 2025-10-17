package com.grocery.delivery.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.grocery.delivery.data.local.dao.ProductDao
import com.grocery.delivery.data.local.dao.UserProfileDao
import com.grocery.delivery.data.local.entity.ProductEntity
import com.grocery.delivery.data.local.entity.UserProfileEntity

/**
 * Room database for local data storage.
 * Provides offline-first functionality with automatic synchronization.
 */
@Database(
    entities = [
        UserProfileEntity::class,
        ProductEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userProfileDao(): UserProfileDao

    abstract fun productDao(): ProductDao
}
