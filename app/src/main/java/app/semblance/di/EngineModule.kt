package app.semblance.di

import app.semblance.data.datastore.SettingsDataStore
import app.semblance.data.datastore.SettingsDataStoreImpl
import app.semblance.engine.EngineClient
import app.semblance.engine.mock.MockEngine
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class EngineModule {

    @Binds
    @Singleton
    abstract fun bindEngineClient(
        mockEngine: MockEngine
    ): EngineClient

    @Binds
    @Singleton
    abstract fun bindSettingsDataStore(
        impl: SettingsDataStoreImpl
    ): SettingsDataStore
}
