package com.grocery.admin.di

import com.grocery.admin.data.repository.*
import com.grocery.admin.domain.repository.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for binding repository interfaces to their implementations.
 * This follows the dependency inversion principle of Clean Architecture.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindDashboardRepository(
        dashboardRepositoryImpl: DashboardRepositoryImpl
    ): DashboardRepository

    @Binds
    @Singleton
    abstract fun bindOrdersRepository(
        ordersRepositoryImpl: OrdersRepositoryImpl
    ): OrdersRepository

    @Binds
    @Singleton
    abstract fun bindProductsRepository(
        productsRepositoryImpl: ProductsRepositoryImpl
    ): ProductsRepository

    @Binds
    @Singleton
    abstract fun bindInventoryRepository(
        inventoryRepositoryImpl: InventoryRepositoryImpl
    ): InventoryRepository
}
