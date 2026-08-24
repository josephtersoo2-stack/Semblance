package app.semblance.di

import app.semblance.data.datastore.SettingsDataStore
import app.semblance.data.datastore.SettingsDataStoreImpl
import app.semblance.engine.EngineClient
import app.semblance.engine.real.RealEngine
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
        realEngine: RealEngine
    ): EngineClient

    @Binds
    @Singleton
    abstract fun bindSettingsDataStore(
        impl: SettingsDataStoreImpl
    ): SettingsDataStore
}
