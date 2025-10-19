package com.grocery.customer.data.local

import androidx.room.RoomDatabase

/**
 * Room database for local data storage.
 * Provides offline-first functionality with automatic synchronization.
 * TODO: Add entities and DAOs when implementing local storage features
 */
// @Database(
//     entities = [
//         UserProfileEntity::class,
//         ProductEntity::class
//     ],
//     version = 1,
//     exportSchema = false
// )
abstract class AppDatabase : RoomDatabase() {

    // TODO: Add abstract DAOs when entities are implemented
    // abstract fun userProfileDao(): UserProfileDao
    // abstract fun productDao(): ProductDao
}
