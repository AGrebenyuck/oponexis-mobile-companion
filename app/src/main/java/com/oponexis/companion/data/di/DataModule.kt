package com.oponexis.companion.data.di

import android.content.Context
import androidx.room.Room
import com.oponexis.companion.data.local.SkeletonDatabase
import com.oponexis.companion.data.mock.MockCompanionRepository
import com.oponexis.companion.data.network.OpenXApi
import com.oponexis.companion.data.preferences.DataStoreSettingsRepository
import com.oponexis.companion.domain.repository.CompanionRepository
import com.oponexis.companion.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindCompanionRepository(repository: MockCompanionRepository): CompanionRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(repository: DataStoreSettingsRepository): SettingsRepository
}

@Module
@InstallIn(SingletonComponent::class)
object FoundationModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SkeletonDatabase =
        Room.databaseBuilder(context, SkeletonDatabase::class.java, "oponexis.db").build()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder().build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl("https://invalid.local/")
        .client(client)
        .build()

    @Provides
    @Singleton
    fun provideOpenXApi(retrofit: Retrofit): OpenXApi = retrofit.create(OpenXApi::class.java)
}

