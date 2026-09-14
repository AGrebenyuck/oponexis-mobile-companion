package com.oponexis.companion.data.di

import android.content.Context
import androidx.room.Room
import com.oponexis.companion.data.local.CallOutcomeDao
import com.oponexis.companion.data.local.MIGRATION_1_2
import com.oponexis.companion.data.local.MIGRATION_2_3
import com.oponexis.companion.data.local.MIGRATION_3_4
import com.oponexis.companion.data.local.MIGRATION_4_5
import com.oponexis.companion.data.local.MIGRATION_5_6
import com.oponexis.companion.data.local.MIGRATION_6_7
import com.oponexis.companion.data.local.MIGRATION_7_8
import com.oponexis.companion.data.local.MIGRATION_8_9
import com.oponexis.companion.data.local.MIGRATION_9_10
import com.oponexis.companion.data.local.DirectSmsDao
import com.oponexis.companion.data.local.RoomCallOutcomeRepository
import com.oponexis.companion.data.local.RoomCompanionRepository
import com.oponexis.companion.data.local.EventOutboxDao
import com.oponexis.companion.data.local.SkeletonDatabase
import com.oponexis.companion.data.local.SmsActivityDao
import com.oponexis.companion.data.local.FileDiagnosticJournal
import com.oponexis.companion.data.local.FileSmsTemplateStore
import com.oponexis.companion.data.network.OpenXApi
import com.oponexis.companion.data.network.CrmNetworkConfig
import com.oponexis.companion.data.network.RetrofitCallerLookupRepository
import com.oponexis.companion.data.network.RetrofitSmsActionRepository
import com.oponexis.companion.data.network.AndroidDeviceSmsReadiness
import com.oponexis.companion.data.network.DeviceSmsReadiness
import com.oponexis.companion.data.preferences.DataStoreSettingsRepository
import com.oponexis.companion.domain.repository.CallerLookupRepository
import com.oponexis.companion.domain.repository.CallOutcomeRepository
import com.oponexis.companion.domain.repository.CompanionRepository
import com.oponexis.companion.domain.repository.SettingsRepository
import com.oponexis.companion.domain.repository.SmsActionRepository
import com.oponexis.companion.domain.repository.OutboxScheduler
import com.oponexis.companion.domain.repository.CallEventDeliveryRepository
import com.oponexis.companion.domain.repository.DiagnosticJournal
import com.oponexis.companion.domain.repository.SmsTemplateStore
import com.oponexis.companion.data.network.RetrofitCallEventDeliveryRepository
import com.oponexis.companion.platform.sync.WorkManagerOutboxScheduler
import com.oponexis.companion.domain.network.NetworkRecoveryWaiter
import com.oponexis.companion.platform.network.AndroidNetworkRecoveryWaiter
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindDeviceSmsReadiness(readiness: AndroidDeviceSmsReadiness): DeviceSmsReadiness

    @Binds
    @Singleton
    abstract fun bindCompanionRepository(repository: RoomCompanionRepository): CompanionRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(repository: DataStoreSettingsRepository): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindCallerLookupRepository(
        repository: RetrofitCallerLookupRepository,
    ): CallerLookupRepository

    @Binds
    @Singleton
    abstract fun bindCallOutcomeRepository(
        repository: RoomCallOutcomeRepository,
    ): CallOutcomeRepository

    @Binds
    @Singleton
    abstract fun bindNetworkRecoveryWaiter(
        waiter: AndroidNetworkRecoveryWaiter,
    ): NetworkRecoveryWaiter

    @Binds
    @Singleton
    abstract fun bindOutboxScheduler(scheduler: WorkManagerOutboxScheduler): OutboxScheduler

    @Binds
    @Singleton
    abstract fun bindCallEventDeliveryRepository(
        repository: RetrofitCallEventDeliveryRepository,
    ): CallEventDeliveryRepository

    @Binds
    @Singleton
    abstract fun bindSmsActionRepository(repository: RetrofitSmsActionRepository): SmsActionRepository

    @Binds
    @Singleton
    abstract fun bindDiagnosticJournal(journal: FileDiagnosticJournal): DiagnosticJournal

    @Binds
    @Singleton
    abstract fun bindSmsTemplateStore(store: FileSmsTemplateStore): SmsTemplateStore
}

@Module
@InstallIn(SingletonComponent::class)
object FoundationModule {
    @Provides
    @Singleton
    fun provideCrmNetworkConfig(): CrmNetworkConfig = CrmNetworkConfig(
        baseUrl = com.oponexis.companion.BuildConfig.CRM_BASE_URL,
        apiToken = com.oponexis.companion.BuildConfig.CRM_API_TOKEN,
    )

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SkeletonDatabase =
        Room.databaseBuilder(context, SkeletonDatabase::class.java, "oponexis.db")
            .addMigrations(MIGRATION_1_2)
            .addMigrations(MIGRATION_2_3)
            .addMigrations(MIGRATION_3_4)
            .addMigrations(MIGRATION_4_5)
			.addMigrations(MIGRATION_5_6)
			.addMigrations(MIGRATION_6_7)
			.addMigrations(MIGRATION_7_8)
			.addMigrations(MIGRATION_8_9)
			.addMigrations(MIGRATION_9_10)
            .build()

    @Provides
    fun provideCallOutcomeDao(database: SkeletonDatabase): CallOutcomeDao =
        database.callOutcomeDao()

    @Provides
    fun provideEventOutboxDao(database: SkeletonDatabase): EventOutboxDao =
        database.eventOutboxDao()

    @Provides
    fun provideSmsActivityDao(database: SkeletonDatabase): SmsActivityDao =
        database.smsActivityDao()

    @Provides
    fun provideDirectSmsDao(database: SkeletonDatabase): DirectSmsDao =
        database.directSmsDao()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(2, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .writeTimeout(3, TimeUnit.SECONDS)
        .callTimeout(6, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(com.oponexis.companion.BuildConfig.CRM_BASE_URL)
        .client(client)
        .build()

    @Provides
    @Singleton
    fun provideOpenXApi(retrofit: Retrofit): OpenXApi = retrofit.create(OpenXApi::class.java)
}
