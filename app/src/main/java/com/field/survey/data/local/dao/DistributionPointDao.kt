package com.field.survey.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.field.survey.data.local.entity.DistributionPointEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DistributionPointDao {

    @Query("SELECT * FROM distribution_points ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<DistributionPointEntity>>

    @Query("SELECT * FROM distribution_points ORDER BY createdAt DESC")
    fun pagedAll(): PagingSource<Int, DistributionPointEntity>

    @Query("SELECT * FROM distribution_points WHERE createdBy = :userId ORDER BY createdAt DESC")
    fun observeByUser(userId: String): Flow<List<DistributionPointEntity>>

    @Query(
        "SELECT * FROM distribution_points " +
            "ORDER BY COALESCE(updatedAt, createdAt) DESC " +
            "LIMIT :limit",
    )
    fun observeRecent(limit: Int): Flow<List<DistributionPointEntity>>

    @Query("SELECT * FROM distribution_points WHERE id = :id")
    suspend fun getById(id: String): DistributionPointEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(dp: DistributionPointEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(dps: List<DistributionPointEntity>)

    @Query("DELETE FROM distribution_points WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM distribution_points")
    suspend fun deleteAll()
}
