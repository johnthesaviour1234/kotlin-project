package com.grocery.customer.di

import com.grocery.customer.data.repository.AuthRepositoryImpl
import com.grocery.customer.data.repository.CartRepositoryImpl
import com.grocery.customer.data.repository.ProductRepositoryImpl
import com.grocery.customer.domain.repository.AuthRepository
import com.grocery.customer.domain.repository.CartRepository
import com.grocery.customer.domain.repository.ProductRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for binding repository interfaces to their implementations.
 * This follows the dependency inversion principle of Clean Architecture.
 * TODO: Add repository bindings when repository implementations are created
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
    abstract fun bindProductRepository(
        productRepositoryImpl: ProductRepositoryImpl
    ): ProductRepository

    @Binds
    @Singleton
    abstract fun bindCartRepository(
        cartRepositoryImpl: CartRepositoryImpl
    ): CartRepository

    // TODO: Add other repository bindings when implementations are ready
    // @Binds
    // @Singleton
    // abstract fun bindUserRepository(
    //     userRepositoryImpl: UserRepositoryImpl
    // ): UserRepository
}
