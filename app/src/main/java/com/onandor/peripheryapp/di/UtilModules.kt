package com.onandor.peripheryapp.di

import android.content.Context
import com.onandor.peripheryapp.utils.PermissionChecker
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class UtilModules {

    @Singleton
    @Provides
    fun providePermissionChecker(@ApplicationContext context: Context): PermissionChecker =
        PermissionChecker(context)
}