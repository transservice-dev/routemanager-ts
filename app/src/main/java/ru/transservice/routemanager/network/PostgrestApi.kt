package ru.transservice.routemanager.network

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*
import ru.transservice.routemanager.data.remote.res.*
import ru.transservice.routemanager.data.remote.res.task.TaskRequestBody
import ru.transservice.routemanager.data.remote.res.task.TaskRes
import ru.transservice.routemanager.data.remote.res.task.TaskUploadRequest

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

    @GET("{urlString}")
    suspend fun getApk(@Path("urlString" , encoded = true) urlString: String): Response<ResponseBody>

    @POST("rpc/loadFiles")
    suspend fun uploadFiles(@Body filesRequest: FilesUploadRequest ): Response<ResponseBody>

    @POST("rpc/loadTaskResult")
    suspend fun uploadTask(@Body taskUploadRequest: TaskUploadRequest): Response<ResponseBody>

    @POST("rpc/updateDocStatus_v2")
    suspend fun setStatus(@Body statusUploadRequest: StatusUploadRequest): Response<ResponseBody>

}