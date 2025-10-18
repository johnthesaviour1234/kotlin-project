package com.grocery.delivery.di

// Temporarily commented out until repository classes are created
// import com.grocery.delivery.data.repository.AuthRepositoryImpl
// import com.grocery.delivery.data.repository.ProductRepositoryImpl
// import com.grocery.delivery.data.repository.UserRepositoryImpl
// import com.grocery.delivery.domain.repository.AuthRepository
// import com.grocery.delivery.domain.repository.ProductRepository
// import com.grocery.delivery.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for binding repository interfaces to their implementations.
 * This follows the dependency inversion principle of Clean Architecture.
 */
// Temporarily disabled until repository implementations are created
/*
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(
        userRepositoryImpl: UserRepositoryImpl
    ): UserRepository

    @Binds
    @Singleton
    abstract fun bindProductRepository(
        productRepositoryImpl: ProductRepositoryImpl
    ): ProductRepository
}
*/
