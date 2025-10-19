package com.grocery.customer.di

// import com.grocery.customer.data.repository.AuthRepositoryImpl
// import com.grocery.customer.data.repository.ProductRepositoryImpl
// import com.grocery.customer.data.repository.UserRepositoryImpl
// import com.grocery.customer.domain.repository.AuthRepository
// import com.grocery.customer.domain.repository.ProductRepository
// import com.grocery.customer.domain.repository.UserRepository
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt module for binding repository interfaces to their implementations.
 * This follows the dependency inversion principle of Clean Architecture.
 * TODO: Add repository bindings when repository implementations are created
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    // TODO: Add repository bindings when implementations are ready
    // @Binds
    // @Singleton
    // abstract fun bindAuthRepository(
    //     authRepositoryImpl: AuthRepositoryImpl
    // ): AuthRepository
    //
    // @Binds
    // @Singleton
    // abstract fun bindUserRepository(
    //     userRepositoryImpl: UserRepositoryImpl
    // ): UserRepository
    //
    // @Binds
    // @Singleton
    // abstract fun bindProductRepository(
    //     productRepositoryImpl: ProductRepositoryImpl
    // ): ProductRepository
}
