package app.semblance.data.repository

import app.semblance.data.local.dao.ProfileDao
import app.semblance.data.local.entity.ProfileEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
    private val profileDao: ProfileDao
) {
    val allProfiles: Flow<List<ProfileEntity>> = profileDao.getAllProfiles()

    fun getProfile(id: Int): Flow<ProfileEntity?> = profileDao.getProfileById(id)

    suspend fun getProfileSync(id: Int): ProfileEntity? = profileDao.getProfileByIdSync(id)

    suspend fun getCount(): Int = profileDao.getProfileCount()

    suspend fun saveProfile(profile: ProfileEntity) = profileDao.insertOrUpdate(profile)

    suspend fun saveAll(profiles: List<ProfileEntity>) = profileDao.insertAll(profiles)

    suspend fun updateProfile(profile: ProfileEntity) = profileDao.update(profile)

    suspend fun deleteProfile(id: Int) = profileDao.deleteById(id)
}
