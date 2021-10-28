package ru.transservice.routemanager.repositories

import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.os.HandlerCompat
import androidx.lifecycle.MutableLiveData
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import kotlinx.coroutines.*
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
import ru.transservice.routemanager.extensions.longFormat
import ru.transservice.routemanager.network.RetrofitClient
import ru.transservice.routemanager.service.LoadResult
import ru.transservice.routemanager.utils.Utils
import java.io.File
import java.io.InputStream
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

    val deviceName: String get() {
        var value = ""
        currentTask.value?.let {
            val vehicleRouteName = if (it.search_type == SearchType.BY_VEHICLE) {
                Utils.vehicleNumToLatin(it.vehicle?.number ?: "")
            }else{
                Utils.transliteration(it.route?.name ?: "")
            }
            value = "$vehicleRouteName ${Utils.transliteration(prefRepository.getRegion()?.name ?: "")}"
        }
        return value
    }

    private val errHandler = CoroutineExceptionHandler{ _, exception ->
        println("Caught $exception")
        Log.e(TAG, "Caught $exception")
        Log.e(TAG, " ${exception.stackTraceToString()}" )

    }

    private val mainThreadHandler: Handler = HandlerCompat.createAsync(Looper.getMainLooper())

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + errHandler)

    private var currentTask: MutableLiveData<Task> = MutableLiveData(prefRepository.getTask())
    private val pointList: MutableLiveData<List<PointItem>> = MutableLiveData()
    private val unloadingAvailable: MutableLiveData<Boolean> = MutableLiveData()

    init {
        setPreferences()
        updateUiState()
    }

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

    fun isTaskLoaded(complete: (isLoaded: Boolean) -> Unit){
        scope.launch {
            complete(dbDao.getAllPointList().isNotEmpty())
        }
    }


    fun syncData(complete: (loadResult: LoadResult<Int>) -> Unit){
        scope.launch {
            try {
                val taskResult = loadTask()
                if (taskResult is LoadResult.Success) {
                    val taskRes = taskResult.data!!
                    if (taskRes.result.status == 1) {
                        //Data received
                        // 1. Write data into local db
                        val insertResult = insertPointRows(taskRes.data)
                        // 2. Write task into local db
                        val task = insertTask(taskRes.data)
                        if (task != null) {
                            //3. Load polygons
                            val polygonsResult = loadPolygons(task.docUid)
                            //4. Set doc status in postgres
                            if (polygonsResult is LoadResult.Success) {
                                val statusResult = setStatus(task, 1)
                                if (statusResult is LoadResult.Success) {
                                    //4. Notify about successful loading
                                    updateUiState()
                                    complete(LoadResult.Success(insertResult))
                                } else {
                                    complete(LoadResult.Error("Загрузка данных завершилась с ошибкой. Ошибка установки статуса"))
                                }
                            } else {
                                complete(LoadResult.Error("Загрузка данных завершилась с ошибкой. Ошибка загрузки полигонов"))
                            }
                        } else {
                            complete(LoadResult.Error("Ошибка записи задания в базу"))
                        }
                    } else {
                        // Error with getting data from server. Show message
                        complete(LoadResult.Error(taskRes.result.message))
                    }
                } else {
                    complete(LoadResult.Error(taskResult.errorMessage ?: "",taskResult.e))
                }
            } catch (e: java.lang.Exception) {
                // Exception error Something goes wrong
                Log.e(TAG, "Error while loading task: $e ${e.stackTraceToString()}")
                complete(LoadResult.Error(e.message ?: "Что-то пошло не так.", e))
            }
        }
    }

    private suspend fun loadTask(): LoadResult<TaskRes> {
        return if (currentTask.value != null) {
            Log.d(TAG, "Loading Task START")
            val taskRequestBody = TaskRequestBody(
                currentTask.value!!.routeDate.longFormat(),
                currentTask.value!!.deviceId,
                currentTask.value!!.vehicle?.uid ?: "",
                currentTask.value!!.route?.uid ?: "",
                currentTask.value!!.search_type.id
            )
            val response = RetrofitClient
                .getPostgrestApi()
                .getTask(taskRequestBody)
            val result = responseResult(response)
            if (result is LoadResult.Success) {
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
        } else {
            Log.e(TAG, "Error loading task: current task is NULL")
            LoadResult.Error("Ошибка получения параметров задания")
        }
    }

    private suspend fun loadPolygons(docUid: String): LoadResult<Int> {
        Log.d(TAG, "Loading Polygons START")
        val polygonRequest = PolygonRequest(docUid)
        val response = RetrofitClient
            .getPostgrestApi()
            .getPolygons(polygonRequest)
        val result = responseResult(response)
        return if (result is LoadResult.Success) {
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

    @RequiresApi(Build.VERSION_CODES.O)
    fun uploadResult(complete: (loadResult: LoadResult<Boolean>) -> Unit) {
        scope.launch {
            Log.d(TAG, "uploading result START")
            delay(2000)
            try {
                //1. upload files first
                val resultFiles = uploadFiles()
                if (resultFiles is LoadResult.Success) {
                    // Files uploaded successfully
                    //2. upload task
                    val resultTask = uploadTask()
                    if (resultTask is LoadResult.Success) {
                        // Task uploaded successfully
                        //3. set status
                        resultTask.data?.get(0)?.let {
                            val resultStatus = setStatus(getTaskFromDB(), 2)
                            if (resultStatus is LoadResult.Success) {
                                Log.d(TAG, "uploading result FINISHED")
                                deleteDataFromDB()
                                updateUiState()
                                complete(resultStatus)
                            } else {
                                Log.e(
                                    TAG,
                                    "uploading result CANCELED with error: ${resultStatus.errorMessage}"
                                )
                                complete(resultStatus)
                            }
                        }
                    } else {
                        Log.e(
                            TAG,
                            "uploading result CANCELED with error: ${resultTask.errorMessage}"
                        )
                        complete(LoadResult.Error("Ошибка при выгрузке задания: ${resultTask.errorMessage}"))
                    }
                } else {
                    Log.e(
                        TAG,
                        "uploading result CANCELED with error: Error while uploading files: : ${resultFiles.errorMessage}"
                    )
                    complete(LoadResult.Error("Ошибка при выгрузке файлов"))
                }
            } catch (e: java.lang.Exception) {
                Log.e(TAG, "Error while uploading task: $e ${e.stackTraceToString()}")
               complete(LoadResult.Error("Ошибка при выгрузке данных", e))
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun uploadFilesOnSchedule(){
        scope.launch {
            uploadFiles(false)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun uploadFile(pointFile: PointFile){
        scope.launch {
            Log.d(TAG, "uploading file ${pointFile.filePath} START")
            if (!pointFile.exists()) {
                Log.i(
                    TAG,
                    " uploading file : " + pointFile.filePath + "FILE NOT FOUND"
                )
                return@launch
            }
            val filesArray: ArrayList<FilesRequestBody> = arrayListOf()
            val photoOrder = if (pointFile.photoOrder == PhotoOrder.PHOTO_BEFORE) {
                0
            } else {
                1
            }
            filesArray.add(
                FilesRequestBody(
                    pointFile.docUID,
                    pointFile.lineUID,
                    pointFile.lat,
                    pointFile.lon,
                    pointFile.fileName,
                    pointFile.fileExtension,
                    pointFile.timeDate.longFormat(),
                    photoOrder,
                    pointFile.getCompresedBase64()
                )
            )
            val response = RetrofitClient
                .getPostgrestApi()
                .uploadFiles(FilesUploadRequest(filesArray))
            if (responseResult(response) is LoadResult.Success) {
                if (response.body() != null) {
                    Log.d(TAG, "uploading file ${pointFile.filePath} FINISHED")
                    dbDao.updatePointFileUploadStatus(arrayListOf(pointFile.id), true)
                }
            }
        }
    }

    fun updatePointOnServer(pointItem: PointItem){
        scope.launch {
            uploadTaskRow(pointItem)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun uploadFiles(deleteUploaded: Boolean = true): LoadResult<Boolean> {

        val data = dbDao.getRouteNotUploadedPointFiles()
        if (data.isNotEmpty()) {
            val portionSize = 20
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

    private suspend fun setStatus(task: Task, status: Int): LoadResult<Boolean>{

            Log.d(TAG, "set status START")
            val dList: ArrayList<StatusUploadBody> = arrayListOf(
                    StatusUploadBody(
                            task.docUid,status,
                            task.deviceId,
                            task.vehicle?.uid ?: "",
                            "",
                            task.dateStart?.longFormat() ?: "",
                            task.dateEnd?.longFormat() ?: "",
                            AppClass.appVersion
                     ))
            val response = RetrofitClient
                .getPostgrestApi()
                .setStatus(StatusUploadRequest(dList))
            val result = responseResult(response)
            return if (result is LoadResult.Success) {
                if (response.body() != null) {
                    Log.d(TAG, "set status FINISHED")
                    LoadResult.Success(true)
                }else{
                    Log.d(TAG, "Error setting status: network result is empty ${result.errorMessage}")
                    LoadResult.Error("Ошибка установки статуса: Вернулся пустой ответ")
                }
            }else{
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

    fun loadSSLCert(complete: (cert: InputStream) -> Unit){
        scope.launch {
            val response = RetrofitClient
                .getApacheConnection()
                .getApk("/cert/apache-selfsigned.crt")
            if (responseResult(response) is LoadResult.Success) {
                response.body()?.let {
                    Log.d(TAG, "Loading cert FINISHED")
                    complete.invoke(it.byteStream())

                }
            }
        }
    }

    //endregion

    //region LocalDatabase

    private fun insertPointRows(pointList: List<TaskRowRes>): Int{
        Log.d(TAG, "Insert point rows START")
        val insertRes = dbDao.insertPointList(pointList.map {
            it.toPointDestination()
        })
        Log.d(TAG, "Insert point rows FINISHED. Inserted $insertRes rows")
        return insertRes

    }

    private fun insertTask(pointList: List<TaskRowRes>): Task? {
        return if (!pointList.isNullOrEmpty()) {
            Log.d(TAG, "Insert Task START")
            val task = Task(pointList[0].docUID, currentTask.value!!.vehicle, currentTask.value!!.route, currentTask.value!!.routeDate,currentTask.value!!.search_type)
                    .also { task ->
                        task.dateStart = pointList[0].dateStart
                        task.dateEnd = pointList[0].dateEnd
                        task.countPoint = pointList.filter { !it.polygon }.size
                        task.countPointDone = dbDao.countPointDone()
                        task.polygonByRow = pointList[0].polygonByRow ?: false
                        task.lastTripNumber = 0
                            //if (pointList[0].polygonByRow == null) false else pointList[0].polygonByRow
                    }
            dbDao.insertTask(task)
            Log.d(TAG, "Insert Task FINISHED")
            task
        } else {
            null
        }
    }

    private fun getTaskFromDB(): Task {
        return dbDao.selectTask()
    }

    fun getTask(): MutableLiveData<Task> {
        return currentTask
    }

    private fun updateTask(task: Task){
        scope.launch {
            dbDao.updateTask(task)
        }
    }

    fun getPointListData(): MutableLiveData<List<PointItem>> {
        return pointList
    }

    fun updatePointListData(){
        scope.launch {
            pointList.postValue(dbDao.getPointList())
            val task = dbDao.selectTask()
            if (task!=null) currentTask.postValue(task) else
                currentTask.postValue(prefRepository.getTask())
        }
    }

    fun getUnloadingAvailable(): MutableLiveData<Boolean>{
        return unloadingAvailable
    }

    private fun updateUnloadingAvailable(){
        scope.launch {
            unloadingAvailable.postValue(dbDao.unloadingAvailable())
        }
    }

    private fun updateUiState(){
        updatePointListData()
        updateUnloadingAvailable()
    }

    fun updatePoint(pointItem: PointItem){
        scope.launch {
            dbDao.updatePointWithRoute(pointItem)
            updateUiState()
        }
    }

    fun addPolygon(pointItem: PointItem, task: Task){
        scope.launch {
            dbDao.addPolygon(pointItem)
            updateTask(task)
            updateUiState()
        }

    }

    fun deletePolygon(pointItem: PointItem){
        scope.launch {
            dbDao.deletePolygon(pointItem)
            updateUiState()
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
            val photoAndFactDone = pointItem.countFact != -1.0 && filesAfter.isNotEmpty() && filesBefore.isNotEmpty()
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

    fun getNextPolygon(complete: (polygon: PolygonItem) -> Unit) {
        scope.launch {
            complete(dbDao.getNextPolygon())
        }
    }

    fun updatePolygon(polygon: PolygonItem, complete: () -> Unit){
        scope.launch {
            dbDao.updatePolygon(polygon)
            updateUiState()
            complete()
        }
    }

    //endregion

}