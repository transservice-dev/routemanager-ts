package ru.transservice.routemanager.repositories

import android.util.Log
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import kotlinx.coroutines.*
import retrofit2.HttpException
import retrofit2.Response
import ru.transservice.routemanager.AppClass
import ru.transservice.routemanager.data.local.RegionItem
import ru.transservice.routemanager.data.local.RouteItem
import ru.transservice.routemanager.data.local.VehicleItem
import ru.transservice.routemanager.data.local.entities.Task
import ru.transservice.routemanager.data.remote.res.RegionRes
import ru.transservice.routemanager.data.remote.res.RouteRes
import ru.transservice.routemanager.data.remote.res.task.TaskRequestBody
import ru.transservice.routemanager.data.remote.res.task.TaskRes
import ru.transservice.routemanager.data.remote.res.VehicleRes
import ru.transservice.routemanager.data.remote.res.task.TaskRowRes
import ru.transservice.routemanager.database.DaoInterface
import ru.transservice.routemanager.network.RetrofitClient
import java.security.Key
import java.util.*

object RootRepository {

    var urlName = ""
    var urlPort = ""
    var baseUrl = ""
    var authPass = ""
    var deviceName = ""
    var currentVehicle: VehicleItem? = null
    var currentRoute: RouteItem? = null
    var currentDate: Date? = null

    private val dbDao: DaoInterface = AppClass.getDatabase()!!.dbDao()

    private val errHandler = CoroutineExceptionHandler{ _, exception ->
        println("Caught $exception")
        Log.e("", "Caught $exception")
    }

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + errHandler)

    init {
        setPreferences()
        generateAuthPass("z5FYg733jGUwjmabuGdmZvfAkDHnh2Wj")
    }

    fun setPreferences() {
        urlName = "188.234.242.63"
        urlPort = "444"
        baseUrl = "https://$urlName:$urlPort/mobileapp/"
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

    private fun <T>isResponseOk(response: Response<T>, methodName: String): Boolean {
        return try {
            if (response.isSuccessful && response.body()!=null) {
                true
            } else {
                Log.e("RootRepository", "$methodName Error: ${response.code()}")
                false
            }
        } catch (e: HttpException) {
            Log.e("RootRepository", "$methodName Exception ${e.message}")
            false
        } catch (e: Throwable) {
            Log.e("RootRepository", "$methodName Unknown exception")
            false
        }
    }

    //region Load data

    fun getRegions(complete: (regions: List<RegionRes>) -> Unit){
        scope.launch {
            val response = RetrofitClient.getPostgrestApi().getRegionsList()
            if (isResponseOk(response, this::class.java.name)) {
                if (!response.body().isNullOrEmpty()) {
                    complete.invoke(response.body()!!)
                }
            }
        }
    }

    fun getAllVehicles(complete: (vehicles: List<VehicleRes>) -> Unit){
        scope.launch {
            val response = RetrofitClient.getPostgrestApi().getAllVehicles()
            if (isResponseOk(response, this::class.java.name)) {
                if (!response.body().isNullOrEmpty()) {
                    complete.invoke(response.body()!!)
                }
            }
        }
    }

    fun getVehiclesByRegion(regionItem: RegionItem,complete: (vehicles: List<VehicleRes>) -> Unit){
        scope.launch {
            val response = RetrofitClient.getPostgrestApi().getVehiclesByRegion("eq.${regionItem.uid}")
            if (isResponseOk(response, this::class.java.name)) {
                if (!response.body().isNullOrEmpty()) {
                    complete.invoke(response.body()!!)
                }
            }
        }
    }

    fun getRoutesByRegion(regionRes: RegionRes,complete: (vehicles: List<RouteRes>) -> Unit){
        scope.launch {
            val response = RetrofitClient.getPostgrestApi().getRoutesByRegion("eq.${regionRes.uid}")
            if (isResponseOk(response, this::class.java.name)) {
                if (!response.body().isNullOrEmpty()) {
                    complete.invoke(response.body()!!)
                }
            }
        }
    }

    fun getTask(taskRequestBody: TaskRequestBody, complete: (taskRes: TaskRes) -> Unit){
        scope.launch {
            val response = RetrofitClient
                .getPostgrestApi()
                .getTask(taskRequestBody)
            if (isResponseOk(response, this::class.java.name)) {
                if (response.body()!=null) {
                    complete.invoke(response.body()!!)
                }
            }
        }
    }

    fun insertPointRows(pointList: List<TaskRowRes>, complete: () -> Unit){
        scope.launch {
            val insertRes = dbDao.insertPointListWithReplace(pointList.map { it.toPointDestination() })
            //if (insertRes>0) {
                complete()
            //}
        }
    }

    fun insertTask(pointList: List<TaskRowRes>, complete: () -> Unit){
        scope.launch {
            if (!pointList.isNullOrEmpty()){
                val task = Task(pointList[0].docUID, currentVehicle, currentRoute, currentDate!!)
                    .also {
                        it.dateStart = pointList[0].dateStart
                        it.dateEnd = pointList[0].dateEnd
                    }
                dbDao.insertTask(task)
            }

        }
    }


    //endregion

}