package ru.transservice.routemanager.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ru.transservice.routemanager.data.local.entities.PointDestination
import ru.transservice.routemanager.data.local.entities.Task

@Dao
interface DaoInterface {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPointListWithReplace(pointList: List<PointDestination>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertPointListOnlyNew(pointList: List<PointDestination>)

    @Query("SELECT * from pointList_table ORDER BY tripNumber, rowNumber")
    fun getAllPointList(): MutableList<PointDestination>

    @Query("SELECT * from pointList_table ORDER BY tripNumber, rowNumber LIMIT 1")
    fun getFirstPoint(): PointDestination

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTask(task: Task): Long

}
