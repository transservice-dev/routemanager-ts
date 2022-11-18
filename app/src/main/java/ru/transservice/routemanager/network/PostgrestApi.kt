package ru.transservice.routemanager.network

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*
import ru.transservice.routemanager.data.remote.res.*
import ru.transservice.routemanager.data.remote.res.task.TaskRequestBody
import ru.transservice.routemanager.data.remote.res.task.TaskRes
import ru.transservice.routemanager.data.remote.res.task.TaskUploadRequest

interface PostgrestApi {

    @POST("rpc/getRegions")
    suspend fun getRegionsList(): Response<List<RegionRes>>

    @POST("rpc/getVehicles")
    suspend fun getVehicles(@Body params: RegionParam): Response<List<VehicleRes>>

    @POST("rpc/getRoutes")
    suspend fun getRoutes(@Body params: RegionParam): Response<List<RouteRes>>

    @POST("rpc/getTask")
    suspend fun getTask(@Body taskRequestBody: TaskRequestBody): Response<TaskRes>

    @POST("rpc/getPolygons")
    suspend fun getPolygons(@Body polygonBody: PolygonRequest): Response<List<PolygonRes>>

    @GET("{urlString}")
    suspend fun getApk(@Path("urlString" , encoded = true) urlString: String): Response<ResponseBody>

    @POST("rpc/loadFiles")
    suspend fun uploadFiles(@Body filesRequest: FilesUploadRequest ): Response<ResponseBody>

    @POST("rpc/loadTaskResult_v2")
    suspend fun uploadTask(@Body taskUploadRequest: TaskUploadRequest): Response<ResponseBody>

    @POST("rpc/updateDocStatus_v3")
    suspend fun setStatus(@Body statusUploadRequest: StatusUploadRequest): Response<ResponseBody>

}