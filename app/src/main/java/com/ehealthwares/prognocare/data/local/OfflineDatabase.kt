package com.ehealthwares.prognocare.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Database(
    entities = [CachedPatientEntity::class, PendingActionEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class OfflineDatabase : RoomDatabase() {
    abstract fun patientDao(): PatientDao
    abstract fun pendingActionDao(): PendingActionDao
}

@Module
@InstallIn(SingletonComponent::class)
object OfflineDatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): OfflineDatabase =
        Room.databaseBuilder(context, OfflineDatabase::class.java, "prognocare_offline.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun providePatientDao(db: OfflineDatabase): PatientDao = db.patientDao()

    @Provides
    fun providePendingActionDao(db: OfflineDatabase): PendingActionDao = db.pendingActionDao()
}
