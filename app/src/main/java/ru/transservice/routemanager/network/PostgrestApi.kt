package ru.transservice.routemanager.network

import org.json.JSONObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import ru.transservice.routemanager.data.remote.res.RegionRes
import ru.transservice.routemanager.data.remote.res.RouteRes
import ru.transservice.routemanager.data.remote.res.task.TaskRequestBody
import ru.transservice.routemanager.data.remote.res.task.TaskRes
import ru.transservice.routemanager.data.remote.res.VehicleRes

interface PostgrestApi {

    @GET("regions")
    suspend fun getRegionsList(): Response<List<RegionRes>>

    @GET("vehicle")
    suspend fun getAllVehicles(): Response<List<VehicleRes>>

    @GET("vehicle")
    suspend fun getVehiclesByRegion(@Query("regionUID") regionUID: String): Response<List<VehicleRes>>

    @GET("routes")
    suspend fun getRoutesByRegion(@Query("region") regionUID: String): Response<List<RouteRes>>

    @POST("rpc/getTask")
    suspend fun getTask(@Body taskRequestBody: TaskRequestBody): Response<TaskRes>
}