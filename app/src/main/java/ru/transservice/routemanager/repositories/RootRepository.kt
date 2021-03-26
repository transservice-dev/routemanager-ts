package ru.transservice.routemanager.repositories

import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.os.HandlerCompat
import androidx.lifecycle.MutableLiveData
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import kotlinx.coroutines.*
import kotlinx.coroutines.android.HandlerDispatcher
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.HttpException
import retrofit2.Response
import ru.transservice.routemanager.AppClass
import ru.transservice.routemanager.data.local.RegionItem
import ru.transservice.routemanager.data.local.RouteItem
import ru.transservice.routemanager.data.local.VehicleItem
import ru.transservice.routemanager.data.local.entities.PhotoOrder
import ru.transservice.routemanager.data.local.entities.PointFile
import ru.transservice.routemanager.data.local.entities.PointItem
import ru.transservice.routemanager.data.local.entities.Task
import ru.transservice.routemanager.data.remote.res.*
import ru.transservice.routemanager.data.remote.res.task.TaskRequestBody
import ru.transservice.routemanager.data.remote.res.task.TaskRes
import ru.transservice.routemanager.data.remote.res.task.TaskRowRes
import ru.transservice.routemanager.data.remote.res.task.TaskUploadRequest
import ru.transservice.routemanager.database.DaoInterface
import ru.transservice.routemanager.extensions.longFormat
import ru.transservice.routemanager.network.PostgrestApi
import ru.transservice.routemanager.network.RetrofitClient
import ru.transservice.routemanager.service.LoadResult
import java.io.File
import java.io.FileOutputStream
import java.security.Key
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

object RootRepository {

    private const val TAG = "Route_Manager: RootRepository"

    var urlName = ""
    var urlPort = ""
    var baseUrl = ""
    var authPass = ""
    var deviceName = ""
    var currentVehicle: VehicleItem? = null
    var currentRoute: RouteItem? = null
    var currentDate: Date? = null

    private val dbDao: DaoInterface = AppClass.getDatabase()!!.dbDao()
    private val prefRepository = PreferencesRepository

    private val errHandler = CoroutineExceptionHandler{ _, exception ->
        println("Caught $exception")
        Log.e(TAG, "Caught $exception")
        Log.e(TAG, " ${exception.stackTraceToString()}" )
    }

    private val mainThreadHandler: Handler = HandlerCompat.createAsync(Looper.getMainLooper())


    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + errHandler)

    private var currentTask: MutableLiveData<Task> = MutableLiveData(prefRepository.getTask())

    init {
        setPreferences()
        getCurrentTaskFromDB {
            task ->
            if (task!=null) currentTask.postValue(task) else
                currentTask.postValue(prefRepository.getTask())
        }

    }

    fun setPreferences() {
        urlName = prefRepository.getUrlName()
        urlPort = prefRepository.getUrlPort()
        baseUrl = "https://$urlName:$urlPort/mobileapp/"
        generateAuthPass(prefRepository.getUrlPass())
    }

    fun generateAuthPass(password: String) {
        val token = encodeToken(password)
        if (token!=null) {
            authPass = token
        }
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
                Log.e("RootRepository", "$methodName Error: ${response.code()} ${response.errorBody()}")
                LoadResult.Error("Error: ${response.code()} ${response.errorBody()}")
            }
        } catch (e: HttpException) {
            Log.e("RootRepository", "$methodName Exception ${e.message} ${e.stackTraceToString()}")
            LoadResult.Error("Ошибка сети. Http exception ${e.message}")
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
            val response = RetrofitClient.getPostgrestApi().getAllVehicles()
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
            val response = RetrofitClient.getPostgrestApi().getVehiclesByRegion("eq.${regionItem.uid}")
            if (responseResult(response) is LoadResult.Success) {
                if (!response.body().isNullOrEmpty()) {
                    Log.d(TAG, "Loading Vehicles FINISHED")
                    complete.invoke(response.body()!!)
                }
            }
        }
    }

    fun loadRoutesByRegion(regionRes: RegionRes,complete: (vehicles: List<RouteRes>) -> Unit){
        scope.launch {
            Log.d(TAG, "Loading Routes START")
            val response = RetrofitClient.getPostgrestApi().getRoutesByRegion("eq.${regionRes.uid}")
            if (responseResult(response) is LoadResult.Success) {
                if (!response.body().isNullOrEmpty()) {
                    Log.d(TAG, "Loading Routes FINISHED")
                    complete.invoke(response.body()!!)
                }
            }
        }
    }

    fun loadTask(complete: (taskRes: TaskRes) -> Unit) {
        scope.launch {
            Log.d(TAG, "Loading Task START")
            currentTask.value?.let {
                val taskRequestBody = TaskRequestBody(
                    it.routeDate.longFormat(),
                    "",
                    it.vehicle!!.uid ?: "",
                    it.route?.uid ?: "",
                    it.search_type.id
                )
                val response = RetrofitClient
                    .getPostgrestApi()
                    .getTask(taskRequestBody)
                if (responseResult(response) is LoadResult.Success) {
                    if (response.body() != null) {
                        Log.d(TAG, "Loading Task FINISHED")
                        complete.invoke(response.body()!!)
                    }
                }
            }
        }

    }

    fun isTaskLoaded(complete: (isLoaded: Boolean) -> Unit){
        scope.launch {
            complete(dbDao.getAllPointList().isNotEmpty())
        }
    }

    fun setDocStatus(){

    }

    fun syncData(complete: (loadResult: LoadResult<Task>) -> Unit){
        //try {
            scope.launch {
                loadTask { taskRes ->
                    if (taskRes.result.status == 1) {
                        //Data recieved
                        // 1. Write data into local db
                        insertPointRows(taskRes.data) {
                            // 2. Write task into local db
                            insertTask(taskRes.data) {
                                //3. Set doc status in postgres
                                // TODO
                                //4. Notify about successful loading
                                currentTask.postValue(it)
                                complete(LoadResult.Success(it))
                            }
                        }
                    } else {
                        // Error with getting data from server. Show messsage
                        complete(LoadResult.Error(taskRes.result.message))
                    }
                }
            }
        /*}catch (e: java.lang.Exception){
            // Exception error Something goes wrong
            complete(LoadResult.Error(e.message ?: "Что-то пошло не так"))
        }*/
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun uploadResult(complete: (loadResult: LoadResult<Boolean>) -> Unit) {

        scope.launch {
            try {
                Log.d(TAG, "uploading result START")
                delay(2000)
                Looper.prepare()
                val handler = HandlerCompat.createAsync(Looper.myLooper()!!)
                Looper.loop()
                //1. upload files first
                handler.post{uploadFiles(handler) { resultFiles ->
                    if (resultFiles) {
                        // Files uploaded successfully
                        //2. upload task
                        uploadTask { resultTask ->
                            if (resultTask is LoadResult.Success) {
                                // Task uploaded successfully
                                //3. set status
                                resultTask.data?.get(0)?.let {
                                    setStatus(it.docUID, 2) { resultStatus ->
                                        if (resultStatus is LoadResult.Success) {
                                            Log.d(TAG, "uploading result FINISHED")
                                            //deleteDataFromDB()
                                            updateCurrentTask()
                                            complete(resultStatus)
                                        } else {
                                            Log.d(
                                                TAG,
                                                "uploading result CANCELED with error: ${resultStatus.errorMessage}"
                                            )
                                            complete(resultStatus)
                                        }

                                    }
                                }

                            } else {
                                Log.d(
                                    TAG,
                                    "uploading result CANCELED with error: ${resultTask.errorMessage}"
                                )
                                complete(LoadResult.Error("Ошибка при выгрузке задания: ${resultTask.errorMessage}"))
                            }
                        }
                    } else {
                        Log.d(
                            TAG,
                            "uploading result CANCELED with error: Error while uploading files"
                        )
                        complete(LoadResult.Error("Ошибка при выгрузке файлов"))
                    }
                }}
            } catch (e: java.lang.Exception) {
                Log.d(TAG,"Ошибка при выгрузке данных ;{e.message ?: \"Неизвестная ошибка\"}")
                complete(LoadResult.Error("Ошибка при выгрузке данных ;{e.message ?: \"Неизвестная ошибка\"}"))

            }
        }
    }



    @RequiresApi(Build.VERSION_CODES.O)
    fun uploadFiles(handler: Handler =  mainThreadHandler,deleteUploaded: Boolean = true, complete: (result: Boolean) -> Unit) {
        scope.launch {
            val data = dbDao.getRouteNotUploadedPointFiles()
            if (data.isNotEmpty()){
                val portionSize = 20
                val iterationCount = (data.size.toFloat()/portionSize)
                var startPos = 0
                var endPos =  if (portionSize - 1 > (data.size - 1)) {
                    data.size - 1
                } else {portionSize-1}
                var i = 0
                var result: Boolean
                do {
                    i++
                    val uploadedFiles = ArrayList<Long>()
                    val resultPortion = uploadFilesPortion(data,startPos,endPos,uploadedFiles)
                    if (resultPortion is LoadResult.Success) {
                        if (deleteUploaded) {
                            dbDao.deleteFiles(uploadedFiles)
                        }else {
                            dbDao.updatePointFileUploadStatus(uploadedFiles,true)
                        }
                    }
                    result = resultPortion.data ?: false
                    startPos = endPos + 1
                    endPos += portionSize
                    if (endPos > (data.size - 1)) {
                        endPos = data.size - 1
                    }
                } while (i<iterationCount && result)

                handler.post {complete(result)}
            }else{
                handler.post {complete(true)}
            }
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
            false
        )
    }


    fun uploadTask(complete: (result: LoadResult<List<PointItem>>) -> Unit) {
        scope.launch {
            Log.d(TAG, "uploading task START")
            val taskList = dbDao.getPointList()
            val taskUploadRequest = TaskUploadRequest(taskList.map { it.toTaskUploadBody() })
            val response = RetrofitClient
                .getPostgrestApi()
                .uploadTask(taskUploadRequest)
            val result = responseResult(response)
            if (result is LoadResult.Success) {
                if (response.body() != null) {
                    Log.d(TAG, "uploading task FINISHED")
                    mainThreadHandler.post{complete(LoadResult.Success(taskList))}
                } else
                    mainThreadHandler.post{complete(LoadResult.Error("Ошибка при выгрузке задания"))}
            } else
                mainThreadHandler.post{complete(LoadResult.Error("Ошибка при выгрузке задания ${result.errorMessage}"))}
        }
    }

    fun setStatus(docUID: String, status: Int, complete: (result: LoadResult<Boolean>) -> Unit) {
        scope.launch {
            Log.d(TAG, "set status START")
            val dList: ArrayList<StatusUploadBody> = arrayListOf(StatusUploadBody(docUID,status))
            val response = RetrofitClient
                .getPostgrestApi()
                .setStatus(StatusUploadRequest(dList))
            val result = responseResult(response)
            if (result is LoadResult.Success) {
                if (response.body() != null) {
                    Log.d(TAG, "set status FINISHED")
                    mainThreadHandler.post{complete(LoadResult.Success(true))}
                }else
                    mainThreadHandler.post{complete(LoadResult.Error("Ошибка установки статуса: Вернулся пустой ответ"))}
            }else
                mainThreadHandler.post{complete(LoadResult.Error("Ошибка установки статуса: ${result.errorMessage}"))}
        }
    }

    fun loadApkFile(file: File, complete: () -> Unit){
        scope.launch {
            val response = RetrofitClient
                .getPostgrestApi()
                .getApk("/apk/app-release.apk")
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

    fun insertPointRows(pointList: List<TaskRowRes>, complete: () -> Unit){
        scope.launch {
            Log.d(TAG, "Insert point rows START")
            val insertRes = dbDao.insertPointListWithReplace(pointList.map { it.toPointDestination() })
            //if (insertRes>0) {
            Log.d(TAG, "Insert point rows FINISHED")
                complete()
            //}
        }
    }

    fun insertTask(pointList: List<TaskRowRes>, complete: (task: Task) -> Unit){
        scope.launch {
            if (!pointList.isNullOrEmpty()){
                Log.d(TAG, "Insert Task START")
                val task = Task(pointList[0].docUID, currentTask.value!!.vehicle, currentTask.value!!.route, currentTask.value!!.routeDate)
                    .also { task ->
                        task.dateStart = pointList[0].dateStart
                        task.dateEnd = pointList[0].dateEnd
                        task.countPoint = pointList.filter { !it.polygon }.size
                        task.countPointDone = 0
                    }
                dbDao.insertTask(task)
                Log.d(TAG, "Insert Task FINISHED")
                complete(task)
            }

        }
    }

    fun getCurrentTaskFromDB(complete: (task: Task) -> Unit){
        scope.launch {
            complete(dbDao.selectTask())
        }
    }

    fun updateCurrentTask(): MutableLiveData<Task>{
        val taskResult: MutableLiveData<Task> = MutableLiveData(prefRepository.getTask())
        getCurrentTaskFromDB {
            task -> taskResult.postValue(task)
            return@getCurrentTaskFromDB
        }
        currentTask.value = taskResult.value
        return taskResult
    }

    fun getCurrentTask(): MutableLiveData<Task>{
        return currentTask
    }

    fun getPointList(complete: (pointList: List<PointItem>) -> Unit){
        scope.launch {
            complete(dbDao.getPointList())
        }
    }

    fun updatePoint(pointItem: PointItem){
        scope.launch {
            dbDao.updatePoint(pointItem)
        }
    }

    fun insertPointFile(pointFile: PointFile, complete: () -> Unit){
        scope.launch {
            Log.d(TAG, "Insert Point File START ${pointFile.filePath}")
            dbDao.insertPointFile(pointFile)
            Log.d(TAG, "Insert Point File FINISHED ${pointFile.filePath}")
            mainThreadHandler.post{complete()}
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
    }


    //endregion

}