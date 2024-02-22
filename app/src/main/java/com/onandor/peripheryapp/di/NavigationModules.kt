package com.onandor.peripheryapp.di

import com.onandor.peripheryapp.navigation.INavigationManager
import com.onandor.peripheryapp.navigation.NavigationManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NavigationManagerModule {

    @Singleton
    @Provides
    fun provideNavigationManager(): INavigationManager = NavigationManager()
}