package com.lux.field.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lux.field.data.local.entity.DistributionPointEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DistributionPointDao {

    @Query("SELECT * FROM distribution_points ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<DistributionPointEntity>>

    @Query("SELECT * FROM distribution_points WHERE id = :id")
    suspend fun getById(id: String): DistributionPointEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(dp: DistributionPointEntity)
}
