package com.attendance.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.attendance.app.data.local.AppDatabase
import com.attendance.app.ml.EmbeddingGenerator
import com.attendance.app.ml.FaceMatcher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> = context.dataStore

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "attendance_database"
    ).build()

    @Provides
    @Singleton
    fun provideEmbeddingDao(database: AppDatabase) = database.embeddingDao()

    @Provides
    @Singleton
    fun provideEmbeddingGenerator(
        @ApplicationContext context: Context
    ): EmbeddingGenerator = EmbeddingGenerator(context)

    @Provides
    @Singleton
    fun provideFaceMatcher(): FaceMatcher = FaceMatcher()
}
