package com.grocery.customer.di

import android.content.Context
import androidx.room.Room
import com.grocery.customer.data.local.AppDatabase
// import com.grocery.customer.data.local.dao.ProductDao
// import com.grocery.customer.data.local.dao.UserProfileDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing database-related dependencies.
 * Configures Room database and DAOs.
 */
// Temporarily disabled until entities and DAOs are created
/*
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    // TODO: Uncomment when AppDatabase is properly configured with entities
    // @Provides
    // @Singleton
    // fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
    //     return Room.databaseBuilder(
    //         context,
    //         AppDatabase::class.java,
    //         "grocery_customer_database"
    //     )
    //         .fallbackToDestructiveMigration() // For development - remove in production
    //         .build()
    // }

    // TODO: Add DAO providers when entities are implemented
    // @Provides
    // fun provideUserProfileDao(database: AppDatabase): UserProfileDao {
    //     return database.userProfileDao()
    // }
    //
    // @Provides
    // fun provideProductDao(database: AppDatabase): ProductDao {
    //     return database.productDao()
    // }
}
*/
