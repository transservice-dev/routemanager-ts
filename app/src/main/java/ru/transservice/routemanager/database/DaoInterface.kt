package ru.transservice.routemanager.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ru.transservice.routemanager.data.local.entities.*

@Dao
interface DaoInterface {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPointListWithReplace(pointList: List<PointItem>)

    @Transaction
    fun insertPointList(pointList: List<PointItem>): Int {
        val currentList = getOnlyPointsList()
        val newList = insertPointListOnlyNew(pointList)
        return newList.size - currentList.size
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertPointListOnlyNew(pointList: List<PointItem>): Array<Long>

    @Query("SELECT * from pointList_table ORDER BY tripNumberFact, tripNumber, rowNumber")
    fun getAllPointList(): MutableList<PointItem>

    @Query("SELECT * from pointList_table WHERE NOT polygon ORDER BY tripNumberFact, tripNumber, rowNumber")
    fun getOnlyPointsList(): MutableList<PointItem>

    @Query("SELECT * from pointList_table ORDER BY tripNumberFact, tripNumber, rowNumber LIMIT 1")
    fun getFirstPoint(): PointItem

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTask(task: Task): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPolygons(polygons: List<PolygonItem>)

    @Query("SELECT * from currentRoute_table LIMIT 1")
    fun selectTask(): Task

    @Query("SELECT * from pointList_table ORDER BY tripNumberFact, tripNumber, rowNumber")
    fun getPointList(): List<PointItem>

    @Update
    fun updatePoint(point: PointItem)

    @Query("UPDATE currentRoute_table SET countPointDone = :countPointDone, lastTripNumber = :lastTripNumber")
    fun updateCountPointDoneLastTrip(countPointDone: Int,lastTripNumber: Int)

    @Query("SELECT COUNT(1) as countDone from pointList_table where done AND NOT polygon Group By docUID")
    fun countPointDone(): Int

    @Transaction
    fun updatePointWithRoute(point: PointItem) {
        updatePoint(point)
        val countDone = countPointDone()
        val lastTripNumber = getLastTripNumberByPoints()
        updateCountPointDoneLastTrip(countDone,lastTripNumber)
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertPointFile(pointFile: PointFile): Long

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

    @Query("DELETE FROM polygon_table")
    fun deletePolygons()

    @Query("SELECT * from polygon_table ORDER BY name")
    fun getPolygonList(): List<PolygonItem>

    @Query("SELECT * from polygon_table WHERE by_default ORDER BY tripNumber")
    fun getCurrentPolygonList(): List<PolygonItem>

    @Query("SELECT * from polygon_table WHERE by_default and NOT done ORDER BY tripNumber LIMIT 1")
    fun getNextPolygon(): PolygonItem

    @Update
    fun updatePolygon(polygonItem: PolygonItem)

    @Insert
    fun insertPoint(point: PointItem)

    @Query("UPDATE pointList_table SET tripNumberFact = :value WHERE done and NOT polygon and tripNumberFact = 1000")
    fun updateTripNumberFact(value: Int)

    @Transaction
    fun addPolygon(polygon: PointItem){
        insertPoint(polygon)
        updateTripNumberFact(polygon.tripNumberFact)
    }

    @Update
    fun updateTask(task: Task)

    // Number of points that are done but not unload on polygon
    @Query("SELECT COUNT(1) as countDone from pointList_table where done AND NOT polygon AND tripNumberFact = 1000 Group By docUID")
    fun countPointDoneNotUnload(): Int

    //Number of polygon which are not done
    @Query("SELECT COUNT(1) as countDone from pointList_table where NOT done AND polygon Group By docUID")
    fun countPolygonsNotDone(): Int

    @Query("SELECT polygonByRow FROM currentRoute_table")
    fun polygonByRow():Boolean

    @Query("SELECT COUNT(1) FROM polygon_table")
    fun polygonAvailable():Int

    @Transaction
    fun unloadingAvailable(): Boolean{
        val polygonByRow = polygonByRow()
        if (polygonByRow) return false
        val countPoints = countPointDoneNotUnload()
        val countPolygons = countPolygonsNotDone()
        val countPolygonsAvailable = polygonAvailable()
        return countPoints > 0 && countPolygons == 0 && countPolygonsAvailable > 0
    }

    //Delete point with trip number
    @Query("UPDATE pointList_table SET tripNumberFact = 1000 WHERE tripNumberFact = :tripNumber")
    fun updatePointsTripNumber(tripNumber: Int)

    //Delete polygon from the point list
    @Delete
    fun deletePolygonFromPointList(pointItem: PointItem)

    @Query("SELECT MAX(tripNumberFact) from pointList_table WHERE polygon and tripNumberFact < 1000")
    fun getLastTripNumber():Int

    @Query("SELECT MAX(tripNumberFact) from pointList_table WHERE tripNumberFact < 1000")
    fun getLastTripNumberByPoints():Int

    @Query("UPDATE currentRoute_table SET lastTripNumber = :tripNumber")
    fun updateLastTripNumber(tripNumber: Int)

    @Transaction
    fun deletePolygon(pointItem: PointItem) {
        updatePointsTripNumber(pointItem.tripNumberFact)
        deletePolygonFromPointList(pointItem)
        updateLastTripNumber(getLastTripNumber())
    }


    @Query("""
        SELECT *
        FROM PointWithData
        WHERE lineUID=:pointId
    """)
    fun observePointItemStateById(pointId: String): Flow<PointWithData>

    @Query("""
        SELECT *
        FROM pointList_table
        WHERE lineUID=:pointId
    """)
    fun getPointById(pointId: String): PointItem

    @Query("SELECT * from pointList_table ORDER BY tripNumberFact, tripNumber, rowNumber")
    fun observePointList() : Flow<List<PointItem>>

    @Query("""
        SELECT *
        FROM TaskWithData
    """)
    fun observeTaskWithData(): Flow<TaskWithData>

    @Query("""
        SELECT COUNT(1) from pointFiles_table
    """)
    fun countFiles() : Int

    @Query("SELECT * FROM pointFiles_table where id=:id")
    fun getFileById(id: Long): PointFile?

    @Query("SELECT *FROM pointList_table as points INNER JOIN pointFiles_table as files on points.lineUID = files.lineUID ")
    fun getPointAndFiles(): Map<PointItem, List<PointFile>>

    @Query("SELECT *FROM pointList_table as points INNER JOIN pointFiles_table as files on points.lineUID = files.lineUID where points.lineUID=:pointId")
    fun getPointAndFilesByPoint(pointId: String): Map<PointItem, List<PointFile>>
}
