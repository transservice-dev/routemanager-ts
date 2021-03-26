package ru.transservice.routemanager.database

import androidx.room.*
import ru.transservice.routemanager.data.local.entities.PhotoOrder
import ru.transservice.routemanager.data.local.entities.PointFile
import ru.transservice.routemanager.data.local.entities.PointItem
import ru.transservice.routemanager.data.local.entities.Task

@Dao
interface DaoInterface {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPointListWithReplace(pointList: List<PointItem>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertPointListOnlyNew(pointList: List<PointItem>)

    @Query("SELECT * from pointList_table ORDER BY tripNumber, rowNumber")
    fun getAllPointList(): MutableList<PointItem>

    @Query("SELECT * from pointList_table ORDER BY tripNumber, rowNumber LIMIT 1")
    fun getFirstPoint(): PointItem

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTask(task: Task): Long

    @Query("SELECT * from currentRoute_table LIMIT 1")
    fun selectTask(): Task

    @Query("SELECT * from pointList_table ORDER BY tripNumber, rowNumber")
    fun getPointList(): List<PointItem>

    @Update
    fun updatePoint(point: PointItem)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertPointFile(pointFile: PointFile)

    @Query("SELECT * from pointFiles_table where lineUID = :lineUID AND photoOrder =:photoOrder") //ORDER BY addressName ASC")
    fun getPointFilesByOrder(lineUID: String, photoOrder: PhotoOrder): List<PointFile>

    @Query("SELECT * from pointFiles_table where lineUID = :lineUID") //ORDER BY addressName ASC")
    fun getAllPointFiles(lineUID: String): List<PointFile>

    @Query("SELECT * from pointFiles_table where lineUID = :lineUID AND (lat = 0.0 OR lon = 0.0)")  //ORDER BY addressName ASC")
    fun getGeolessPointFiles(lineUID: String): MutableList<PointFile>

    @Query("UPDATE pointFiles_table SET lat = :lat, lon = :lon WHERE id = :id")
    fun updatePointFileLocation(lat: Double, lon: Double, id: Long)

    @Query("SELECT * from pointFiles_table") //ORDER BY addressName ASC")
    fun getRoutePointFiles(): List<PointFile>

    @Query("SELECT * from pointFiles_table where NOT uploaded") //ORDER BY addressName ASC")
    fun getRouteNotUploadedPointFiles(): List<PointFile>

    @Transaction
    fun getPointFiles(lineUID: String, photoOrder: PhotoOrder?): List<PointFile>{
        return if (photoOrder == null) {
            getAllPointFiles(lineUID)
        } else {
            getPointFilesByOrder(lineUID,photoOrder)
        }
    }

    @Query("SELECT * from pointFiles_table")
    fun getAllFiles(): List<PointFile>

    @Query("SELECT DISTINCT pointList_table.* from pointList_table INNER JOIN pointFiles_table on pointList_table.docUID = pointFiles_table.docUID AND pointList_table.lineUID = pointFiles_table.lineUID ORDER BY pointList_table.rowNumber")
    fun getPointsWithFiles(): List<PointItem>

    @Query("DELETE FROM pointFiles_table where id in (:idList)")
    fun deleteFiles(idList: ArrayList<Long>)

    @Query("UPDATE pointFiles_table SET uploaded = :status WHERE id in (:idList)")
    fun updatePointFileUploadStatus(idList: ArrayList<Long>, status: Boolean)

    @Query("DELETE FROM pointList_table")
    fun deletePointList()

    @Query("DELETE FROM currentRoute_table")
    fun deleteCurrentRoute()
}
