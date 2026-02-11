package com.lux.field.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.lux.field.data.local.entity.LocationPointEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationPointDao {

    @Insert
    suspend fun insert(point: LocationPointEntity)

    @Query("SELECT * FROM location_points WHERE isSynced = 0 ORDER BY timestamp ASC LIMIT :limit")
    suspend fun getUnsynced(limit: Int = 50): List<LocationPointEntity>

    @Query("SELECT COUNT(*) FROM location_points WHERE isSynced = 0")
    suspend fun getUnsyncedCount(): Int

    @Query("UPDATE location_points SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<Long>)

    @Query("DELETE FROM location_points WHERE isSynced = 1")
    suspend fun deleteSynced()

    @Query("DELETE FROM location_points WHERE timestamp < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)

    @Query("SELECT * FROM location_points ORDER BY timestamp DESC LIMIT 1")
    fun observeLatest(): Flow<LocationPointEntity?>
}
