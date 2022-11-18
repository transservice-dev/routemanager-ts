package ru.transservice.routemanager.repositories

import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.os.HandlerCompat
import androidx.lifecycle.MutableLiveData
import androidx.work.CoroutineWorker
import androidx.work.Data
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.*
import retrofit2.HttpException
import retrofit2.Response
import ru.transservice.routemanager.AppClass
import ru.transservice.routemanager.data.local.RegionItem
import ru.transservice.routemanager.data.local.entities.*
import ru.transservice.routemanager.data.remote.res.*
import ru.transservice.routemanager.data.remote.res.task.TaskRequestBody
import ru.transservice.routemanager.data.remote.res.task.TaskRes
import ru.transservice.routemanager.data.remote.res.task.TaskRowRes
import ru.transservice.routemanager.data.remote.res.task.TaskUploadRequest
import ru.transservice.routemanager.database.DaoInterface
import ru.transservice.routemanager.extensions.WorkInfoKeys
import ru.transservice.routemanager.extensions.longFormat
import ru.transservice.routemanager.extensions.tag
import ru.transservice.routemanager.extensions.updateProgressValue
import ru.transservice.routemanager.network.RetrofitClient
import ru.transservice.routemanager.service.LoadResult
import ru.transservice.routemanager.utils.Utils
import ru.transservice.routemanager.workmanager.UploadResultWorker
import java.io.File
import java.net.SocketTimeoutException
import java.security.Key
import java.util.*
import kotlin.collections.ArrayList


object RootRepository {

    private const val TAG = "${AppClass.TAG}: RootRepository"

    private var urlName = ""
    private var urlPort = ""
    var baseUrl = ""
    var authPass = ""

    private val dbDao: DaoInterface = AppClass.getDatabase()!!.dbDao()
    private val prefRepository = PreferencesRepository

    //Coroutines params
    private val errHandler = CoroutineExceptionHandler{ _, exception ->
        println("Caught $exception")
        Log.e(TAG, "Caught $exception")
        Log.e(TAG, " ${exception.stackTraceToString()}" )
        Firebase.crashlytics.recordException(exception)

    }
    private val mainThreadHandler: Handler = HandlerCompat.createAsync(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + errHandler)

    // Task flow
    private var currentTaskValue = prefRepository.getTaskWithData()
    private var taskFlow = dbDao.observeTaskWithData()
        .combine(prefRepository.getTaskDataFlow()) { taskDb, taskPref ->
            if (taskDb == null) taskPref else taskDb
        }

    // device name depends on current task value
    val deviceName: String get() {
        return generateDeviceName()
    }

    private val unloadingAvailable: MutableLiveData<Boolean> = MutableLiveData()

    init {
        setPreferences()
        scope.launch {
            taskFlow.collect {
                currentTaskValue = it
                unloadingAvailable.postValue(dbDao.unloadingAvailable())
                Log.d(TAG, "collect task value $it")
            }
        }
    }

    private fun generateDeviceName(): String {
        var value = ""
        val task = getTaskValue()
        val vehicleRouteName = if (task.search_type == SearchType.BY_VEHICLE) {
            Utils.vehicleNumToLatin(task.vehicle?.number ?: "")
        } else {
            Utils.transliteration(task.route?.name ?: "")
        }
        value = "$vehicleRouteName ${Utils.transliteration(prefRepository.getRegion()?.name ?: "")}"
        return  value
    }

    fun getUnloadingAvailable(): MutableLiveData<Boolean>{
        return unloadingAvailable
    }

    fun getTaskValue(): Task = currentTaskValue.task

    fun getTaskData(): TaskWithData = currentTaskValue

    fun setPreferences() {
        urlName = prefRepository.getUrlName()
        urlPort = prefRepository.getUrlPort()
        baseUrl = "https://$urlName:$urlPort/mobileapp/"
        generateAuthPass(prefRepository.getUrlPass())
    }


    private fun generateAuthPass(password: String) {
        val token = encodeToken(password)
        authPass = token ?: "1"
    }

    private fun encodeToken(authPass: String): String? {
        var token:String? = null
        if (authPass.toByteArray().size < 32) return token
        try {
            val key: Key = Keys.hmacShaKeyFor(authPass.toByteArray(charset("UTF-8")))
            token = Jwts.builder()
                .claim("role", "api_user")
                .signWith(key)
                .compact()
        } catch (e: Exception) {
            // TODO обработать исключение некорректный формат пароля для токена
        }
        return token
    }

    private fun <T>responseResult(response: Response<T>): LoadResult<Boolean> {
        val methodName = ""
        return try {
            if (response.isSuccessful && response.body()!=null) {
                LoadResult.Success(true)
            } else {
                var errorMessage = ""
                response.errorBody()?.let { errorMessage = it.byteStream().bufferedReader().use { reader -> reader.readText() }  }
                Log.e("RootRepository", "$methodName Error: ${response.code()} $errorMessage")
                if (response.code() == 401)  {
                    LoadResult.Error("Ошибка авторизации. Проверьте правильность ввода пароля.",SecurityException())
                }else{
                    LoadResult.Error("Error: ${response.code()} $errorMessage")
                }
            }
        } catch (e: HttpException) {
            Log.e("RootRepository", "$methodName Exception ${e.message} ${e.stackTraceToString()}")
            LoadResult.Error("Ошибка сети. Http exception ${e.message}",e)
        } catch (e: Throwable) {
            Log.e("RootRepository", "$methodName Unknown exception ${e.message} ${e.stackTraceToString()}")
            LoadResult.Error("Неизвестная ошибка ${e.message}")
        }
    }

    private suspend fun <T: CoroutineWorker> updateWorkerProgress(worker: T, data: Data){
        worker.setProgress(data)
    }

    //region RemoteConnections

    fun loadRegions(complete: (regions: List<RegionRes>) -> Unit){
        Log.d(TAG, "Loading Regions START")
        scope.launch {
            val response = RetrofitClient.getPostgrestApi().getRegionsList()
            if (responseResult(response) is LoadResult.Success) {
                if (!response.body().isNullOrEmpty()) {
                    Log.d(TAG, "Loading Regions FINISHED")
                    complete.invoke(response.body()!!)
                }
            }
        }
    }

    fun loadAllVehicles(complete: (vehicles: List<VehicleRes>) -> Unit){
        scope.launch {
            val response = RetrofitClient.getPostgrestApi().getVehicles(RegionParam())
            if (responseResult(response) is LoadResult.Success) {
                if (!response.body().isNullOrEmpty()) {
                    complete.invoke(response.body()!!)
                }
            }
        }
    }

    fun loadVehiclesByRegion(regionItem: RegionItem,complete: (vehicles: List<VehicleRes>) -> Unit){
        Log.d(TAG, "Loading Vehicles START")
        scope.launch {
            val response = RetrofitClient.getPostgrestApi().getVehicles(RegionParam(regionItem.uid))
            if (responseResult(response) is LoadResult.Success) {
                if (!response.body().isNullOrEmpty()) {
                    Log.d(TAG, "Loading Vehicles FINISHED")
                    complete.invoke(response.body()!!)
                }else{
                    complete.invoke(listOf())
                }
            }
        }
    }

    fun loadRoutesByRegion(regionItem: RegionItem,complete: (vehicles: List<RouteRes>) -> Unit){
        scope.launch {
            Log.d(TAG, "Loading Routes START")
            val response = RetrofitClient.getPostgrestApi().getRoutes(RegionParam(regionItem.uid))
            if (responseResult(response) is LoadResult.Success) {
                if (!response.body().isNullOrEmpty()) {
                    Log.d(TAG, "Loading Routes FINISHED")
                    complete.invoke(response.body()!!)
                }
            }
        }
    }

    fun testCoroutines() {
        val scope1 = CoroutineScope(Dispatchers.Default + errHandler)
        val scope2 = CoroutineScope(Dispatchers.Default + errHandler)
        val job = scope1.launch {
            scope1.launch {
                delay(100)
                Log.d(tag(),"task 1")
                throw SocketTimeoutException()
            }
            scope2.launch {
                delay(200)
                Log.d(tag(),"task 2")
            }
            //delay(1000)
        }
    }

    fun syncData(task: Task, complete: (loadResult: LoadResult<Int>) -> Unit){
        scope.launch {
            try {
                val taskResult = loadTask(task)
                if (taskResult !is LoadResult.Success) {
                    complete(LoadResult.Error(taskResult.errorMessage ?: "", taskResult.e))
                }
                taskResult.data?.let { taskData ->
                    if (taskData.result.status != 1) {
                        // Error with getting data from server. Show message
                        complete(LoadResult.Error(taskData.result.message))
                        return@launch
                    }
                    //Data received
                    // 1. Write data into local db
                    val insertResult = insertPointRows(taskData.data)
                    // 2. Write task into local db
                    val task = insertTask(task, taskData.data)
                    if (task == null) {
                        complete(LoadResult.Error("Ошибка записи задания в базу"))
                        return@launch
                    }
                    //3. Load polygons
                    val polygonsResult = loadPolygons(task!!.docUid)
                    if (polygonsResult !is LoadResult.Success) {
                        complete(LoadResult.Error("Загрузка данных завершилась с ошибкой. Ошибка загрузки полигонов"))
                        return@launch
                    }
                    //4. Set doc status in postgres
                    val statusResult = setStatus(task, 1)
                    if (statusResult !is LoadResult.Success) {
                        complete(LoadResult.Error("Загрузка данных завершилась с ошибкой. Ошибка установки статуса"))
                        return@launch
                    }
                    //Set default mode for list, TODO move to another layer
                    prefRepository.putValue(prefRepository.FULL_TASK_LIST to false)
                    //4. Notify about successful loading
                    complete(LoadResult.Success(insertResult))
                }
            } catch (e: java.lang.Exception) {
                // Exception error Something goes wrong
                Log.e(TAG, "Error while loading task: $e ${e.stackTraceToString()}")
                complete(LoadResult.Error(e.message ?: "Что-то пошло не так.", e))
            }
        }
    }

    private suspend fun loadTask(task: Task): LoadResult<TaskRes> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Loading Task START")

        val taskRequestBody = TaskRequestBody(
            task.routeDate.longFormat(),
            task.deviceId,
            task.vehicle?.uid ?: "",
            task.route?.uid ?: "",
            task.search_type.id
        )

        val response = RetrofitClient
            .getPostgrestApi()
            .getTask(taskRequestBody)
        val result = responseResult(response)
        return@withContext if (result is LoadResult.Success) {
            if (response.body() != null) {
                Log.d(TAG, "Loading Task FINISHED")
                LoadResult.Success(response.body()!!)
            } else {
                Log.d(TAG, "Loading Task FINISHED. Response body is Empty")
                LoadResult.Error("Пустой ответ от сервера")
            }
        } else {
            Log.e(TAG, "Loading Task CANCELED with error. Network request is NOT successful. ${result.errorMessage}")
            LoadResult.Error("Ошибка получения данных ${result.errorMessage}",result.e)
        }
    }

    private suspend fun loadPolygons(docUid: String): LoadResult<Int> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Loading Polygons START")
        val polygonRequest = PolygonRequest(docUid)
        val response = RetrofitClient
            .getPostgrestApi()
            .getPolygons(polygonRequest)
        val result = responseResult(response)
        return@withContext if (result is LoadResult.Success) {
            if (response.body() != null) {
                Log.d(TAG, "Loading Polygons FINISHED")
                dbDao.insertPolygons(response.body()!!.map { it.toPolygonItem() })
                LoadResult.Success(1)
            } else {
                Log.d(TAG, "Loading Polygons FINISHED. Response body is Empty")
                LoadResult.Success(1)
            }
        } else {
            Log.e(TAG, "Loading Polygons CANCELED with error. Network request is NOT successful. ${result.errorMessage}")
            LoadResult.Error("Ошибка получения данных ${result.errorMessage}",result.e)
        }
    }

    suspend fun uploadResult(worker: UploadResultWorker): LoadResult<Boolean>{
        Log.d(TAG, "uploading result START")
        //return LoadResult.Success(true)
        //return LoadResult.Error("Ошибка!", SocketTimeoutException())
        try {
            //1. upload files first
            val resultFiles = uploadFiles(worker)
            if (resultFiles !is LoadResult.Success){
                Log.e(
                    TAG,
                    "uploading result CANCELED with error: Error while uploading files: : ${resultFiles.errorMessage}"
                )
                return (LoadResult.Error("Ошибка при выгрузке файлов"))
            }
            // Files uploaded successfully
            //2. upload task
            val resultTask = uploadTask()
            if (resultTask !is LoadResult.Success) {
                Log.e(
                    TAG,
                    "uploading result CANCELED with error: ${resultTask.errorMessage}"
                )
                return (LoadResult.Error("Ошибка при выгрузке задания: ${resultTask.errorMessage}"))
            }
            // Task uploaded successfully
            //3. set status
            resultTask.data?.get(0)?.let {
                val resultStatus = setStatus(currentTaskValue.task, 2)
                if (resultStatus !is LoadResult.Success) {
                    Log.e(
                        TAG,
                        "uploading result CANCELED with error: ${resultStatus.errorMessage}"
                    )
                    return (resultStatus)
                }

                Log.d(TAG, "uploading result FINISHED")
                deleteDataFromDB()
                return (resultStatus)
            }
            return (LoadResult.Error("Ошибка при выгрузке данных"))

        } catch (e: java.lang.Exception) {
            Log.e(TAG, "Error while uploading task: $e ${e.stackTraceToString()}")
            return (LoadResult.Error("Ошибка при выгрузке данных", e))
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun uploadFiles(worker: CoroutineWorker,deleteUploaded: Boolean = true, pointFile: PointFile? = null): LoadResult<Boolean> {

        val data = if (pointFile != null) {
            listOf(pointFile)
        }else{
            dbDao.getRouteNotUploadedPointFiles()
        }.filter {
            deleteUploaded || it.lat != 0.0 // when uploading picture before finishing the route check for location
        }

        //calculating worker progress
        val countFiles = dbDao.countFiles()
        var countUploadedFiles = countFiles - data.size
        worker.updateProgressValue(WorkInfoKeys.Progress,(countUploadedFiles.toFloat()/countFiles*100).toInt())



        if (data.isNotEmpty()) {
            // uploading in portion
            val portionSize = 1
            val iterationCount = (data.size.toFloat() / portionSize)
            var startPos = 0
            var endPos = if (portionSize - 1 > (data.size - 1)) {
                data.size - 1
            } else {
                portionSize - 1
            }

            var i = 0
            var result: Boolean
            do {
                i++
                val uploadedFiles = ArrayList<Long>()
                val resultPortion = uploadFilesPortion(data, startPos, endPos, uploadedFiles)
                if (resultPortion is LoadResult.Success) {
                    if (deleteUploaded) {
                        dbDao.deleteFiles(uploadedFiles)
                    } else {
                        dbDao.updatePointFileUploadStatus(uploadedFiles, true)
                    }
                    countUploadedFiles += portionSize
                    worker.updateProgressValue(WorkInfoKeys.Progress,(countUploadedFiles.toFloat()/countFiles*100).toInt())
                }
                result = resultPortion.data ?: false
                startPos = endPos + 1
                endPos += portionSize
                if (endPos > (data.size - 1)) {
                    endPos = data.size - 1
                }
            } while (i < iterationCount && result)

            return if (result) LoadResult.Success(true) else LoadResult.Error("Ошибка выгрузки файлов")
        } else {
            return LoadResult.Success(true)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun uploadFilesPortion(
        data: List<PointFile>,
        startPos: Int,
        endPos: Int,
        deletedFiles: ArrayList<Long>
    ): LoadResult<Boolean> {
        Log.d(TAG, "uploading files portion START")
        val warningMessage = arrayListOf<String>()
        val filesArray: ArrayList<FilesRequestBody> = arrayListOf()
        for (j in startPos..endPos) {
            val currentFile = data[j]
            if (!currentFile.exists()) {
                Log.i(
                    TAG,
                    " uploadFilesPortion file: " + currentFile.filePath + "FILE NOT FOUND"
                )
                warningMessage.add("File not found ${currentFile.filePath}")
                continue
            }
            //log
            Log.i(TAG, " uploadFilesPortion file: " + currentFile.filePath)
            val photoOrder = if (currentFile.photoOrder == PhotoOrder.PHOTO_BEFORE) {
                0
            } else {
                1
            }
            filesArray.add(
                FilesRequestBody(
                    currentFile.docUID,
                    currentFile.lineUID,
                    currentFile.lat,
                    currentFile.lon,
                    currentFile.fileName,
                    currentFile.fileExtension,
                    currentFile.timeDate.longFormat(),
                    photoOrder,
                    currentFile.getCompresedBase64()
                )
            )
            deletedFiles.add(currentFile.id)
        }

        val response = RetrofitClient
            .getPostgrestApi()
            .uploadFiles(FilesUploadRequest(filesArray))
        var result = false
        if (responseResult(response) is LoadResult.Success) {
            if (response.body() != null) {
                Log.d(TAG, "uploading files portion FINISHED")
                result = true
            }
        }
        return if (result) LoadResult.Success(true) else LoadResult.Error(
            "Ошибка при выгрузке файлов",
            data = false
        )
    }

    private suspend fun uploadTask():LoadResult<List<PointItem>> {

            Log.d(TAG, "uploading task START")
            val taskList = dbDao.getPointList()
            val taskUploadRequest = TaskUploadRequest(taskList.map { it.toTaskUploadBody() })
            //Log.d(TAG,taskUploadRequest.trackList.toString())
            val response = RetrofitClient
                .getPostgrestApi()
                .uploadTask(taskUploadRequest)
            val result = responseResult(response)
            return if (result is LoadResult.Success) {
                if (response.body() != null) {
                    Log.d(TAG, "uploading task FINISHED")
                    LoadResult.Success(taskList)
                } else
                    LoadResult.Error("Ошибка при выгрузке задания")
            } else
                LoadResult.Error("Ошибка при выгрузке задания ${result.errorMessage}")
    }

    fun updatePointOnServer(pointItem: PointItem){
        scope.launch {
            uploadTaskRow(pointItem)
        }
    }
    private suspend fun uploadTaskRow(pointItem: PointItem):LoadResult<List<PointItem>>{
        Log.d(TAG, "update task row ${pointItem.addressName} START")
        val taskList = listOf(pointItem)
        val taskUploadRequest = TaskUploadRequest(taskList.map { it.toTaskUploadBody() })
        val response = RetrofitClient
            .getPostgrestApi()
            .uploadTask(taskUploadRequest)
        val result = responseResult(response)
        return if (result is LoadResult.Success) {
            if (response.body() != null) {
                Log.d(TAG, "update task row ${pointItem.addressName} FINISHED")
                LoadResult.Success(taskList)
            } else
                LoadResult.Error("Ошибка при выгрузке задания")
        } else
            LoadResult.Error("Ошибка при выгрузке задания ${result.errorMessage}")
    }

    private suspend fun setStatus(task: Task, status: Int): LoadResult<Boolean> {
        Log.d(TAG, "set status START")
        val dList: ArrayList<StatusUploadBody> = arrayListOf(
            StatusUploadBody(
                task.docUid, status,
                task.deviceId,
                task.vehicle?.uid ?: "",
                "",
                task.dateStart?.longFormat() ?: "",
                task.dateEnd?.longFormat() ?: "",
                AppClass.appVersion,
                vehicle_defects = task.defects
            )
        )
        val response = RetrofitClient
            .getPostgrestApi()
            .setStatus(StatusUploadRequest(dList))
        val result = responseResult(response)
        return if (result is LoadResult.Success) {
            if (response.body() != null) {
                Log.d(TAG, "set status FINISHED")
                LoadResult.Success(true)
            } else {
                Log.d(TAG, "Error setting status: network result is empty ${result.errorMessage}")
                LoadResult.Error("Ошибка установки статуса: Вернулся пустой ответ")
            }
        } else {
            Log.d(TAG, "Error setting status: network result is incorrect ${result.errorMessage}")
            LoadResult.Error("Ошибка установки статуса: ${result.errorMessage}")
        }

    }

    fun loadApkFile(file: File, complete: () -> Unit){
        scope.launch {
            val response = RetrofitClient
                .getApacheConnection()
                .getApk("/apk/app-release_2.apk")
            if (responseResult(response) is LoadResult.Success) {
                response.body()?.let {
                    file.outputStream().use { fileOut -> it.byteStream().copyTo(fileOut) }
                    Log.d(TAG, "Loading Apk FINISHED")
                    mainThreadHandler.post(complete)
                }
            }
        }
    }

    //endregion

    //region LocalDatabase

    fun observePointList(): Flow<List<PointItem>>{
        return dbDao.observePointList()
    }

    fun observePointItemState(pointId: String): Flow<PointWithData>{
        return dbDao.observePointItemStateById(pointId)
    }

    fun observeTask() : Flow<TaskWithData>{
       return taskFlow
    }

    fun getPointById(pointId: String, complete: (pointItem: PointItem) -> Unit) {
        scope.launch {
            complete(dbDao.getPointById(pointId))
        }
    }

    private fun insertPointRows(pointList: List<TaskRowRes>): Int{
        Log.d(TAG, "Insert point rows START")
        val insertRes = dbDao.insertPointList(pointList.map {
            it.toPointDestination()
        })
        Log.d(TAG, "Insert point rows FINISHED. Inserted $insertRes rows")
        return insertRes

    }

    private fun insertTask(taskParams: Task, pointList: List<TaskRowRes>): Task? {
        return if (!pointList.isNullOrEmpty()) {
            Log.d(TAG, "Insert Task START")
            val task = taskParams.copy(docUid = pointList[0].docUID)
                    .also { it ->
                        it.dateStart = pointList[0].dateStart
                        it.dateEnd = pointList[0].dateEnd
                        it.countPoint = pointList.filter { !it.polygon }.size
                        it.countPointDone = dbDao.countPointDone()
                        it.polygonByRow = pointList[0].polygonByRow ?: false
                        it.lastTripNumber = 0
                    }
            dbDao.insertTask(task)
            Log.d(TAG, "Insert Task FINISHED")
            task
        } else {
            null
        }
    }


    fun updatePoint(pointItem: PointItem){
        scope.launch {
            dbDao.updatePointWithRoute(pointItem)
        }
    }

    suspend fun updateTask(task: Task) = withContext(IO) {
        dbDao.updateTask(task)
    }

    fun addPolygon(pointItem: PointItem, task: Task){
        scope.launch {
            dbDao.addPolygon(pointItem)
        }

    }

    fun deletePolygon(pointItem: PointItem){
        scope.launch {
            dbDao.deletePolygon(pointItem)
        }
    }

    fun insertPointFile(pointFile: PointFile, complete: () -> Unit){
        scope.launch {
            Log.d(TAG, "Insert Point File START ${pointFile.filePath}")
            val id = dbDao.insertPointFile(pointFile)
            pointFile.id = id
            Log.d(TAG, "Insert Point File FINISHED ${pointFile.filePath}")
            mainThreadHandler.post{complete()}
        }
    }

    fun checkPointForCompletion(pointItem: PointItem, complete: (canBeDone: Boolean) -> Unit) {
        scope.launch {
            val filesBefore = dbDao.getPointFilesByOrder(pointItem.lineUID,PhotoOrder.PHOTO_BEFORE)
            val filesAfter = dbDao.getPointFilesByOrder(pointItem.lineUID,PhotoOrder.PHOTO_AFTER)
            val photoAndFactDone = pointItem.countFact > 0 && filesAfter.isNotEmpty() && filesBefore.isNotEmpty()
            val canBeDone = if (pointItem.polygonByRow) {
                !pointItem.polygonNotFilled() && photoAndFactDone
            }else{
                photoAndFactDone
            }
            complete(canBeDone)
        }
    }

    fun getPointFilesForGallery(pointItem: PointItem, order: PhotoOrder, complete: (pointFilesList: List<PointFile>) -> Unit) {
        scope.launch {
            val result =
                if (order == PhotoOrder.DONT_SET)
                    dbDao.getAllPointFiles(pointItem.lineUID)
                else
                    dbDao.getPointFilesByOrder(
                    pointItem.lineUID,
                    order
                )
            mainThreadHandler.post { complete(result) }
        }
    }

    fun getPointFilesData(pointItem: PointItem?, order: PhotoOrder, complete: (pointFiles: Map<PointItem,List<PointFile>>) -> Unit) {
        scope.launch {
            if (pointItem == null) {
                complete(dbDao.getPointAndFiles())
            }else{
                complete(dbDao.getPointAndFilesByPoint(pointItem.lineUID))
            }
        }
    }


    fun getPointFilesByOrder(pointItem: PointItem, order: PhotoOrder, complete: (pointFilesList: List<PointFile>) -> Unit){
        scope.launch {
            complete(dbDao.getPointFilesByOrder(pointItem.lineUID,order))
        }
    }

    fun getGeolessPointFiles(pointItem: PointItem, complete: (pointFilesList: List<PointFile>) -> Unit){
        scope.launch {
            complete(dbDao.getGeolessPointFiles(pointItem.lineUID))
        }
    }

    fun updatePointFileLocation(pointFile: PointFile, lat: Double, lon: Double, complete: () -> Unit){
        scope.launch {
            dbDao.updatePointFileLocation(lat,lon,pointFile.id)
            complete()
        }
    }

    fun getPointsWithFiles(complete: (pointList: List<PointItem>) -> Unit) {
        scope.launch {
            complete(dbDao.getPointsWithFiles())
        }
    }

    fun getAllFiles(complete: (pointFilesList: List<PointFile>) -> Unit){
        scope.launch {
            val resultList = dbDao.getAllFiles()
            mainThreadHandler.post{
                complete(resultList)
            }
        }
    }

    private fun deleteDataFromDB() {
        dbDao.deletePointList()
        dbDao.deleteCurrentRoute()
        dbDao.deletePolygons()
    }

    fun getPolygonList(complete: (polygonList: List<PolygonItem>) -> Unit) {
        scope.launch {
            complete(dbDao.getPolygonList())
        }
    }

    fun getPolygonCurrentList(complete: (polygonList: List<PolygonItem>) -> Unit) {
        scope.launch {
            complete(dbDao.getCurrentPolygonList())
        }
    }

    fun getNextPolygon(complete: (polygon: PolygonItem?) -> Unit) {
        scope.launch {
            complete(dbDao.getNextPolygon())
        }
    }

    fun updatePolygon(polygon: PolygonItem, complete: () -> Unit){
        scope.launch {
            dbDao.updatePolygon(polygon)
            complete()
        }
    }

    suspend fun getPointFileById(id: Long): PointFile? {
        return dbDao.getFileById(id)
    }

    //endregion

}