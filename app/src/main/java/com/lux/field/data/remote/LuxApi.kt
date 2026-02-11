package com.lux.field.data.remote

import com.lux.field.data.remote.dto.LocationBatchRequest
import com.lux.field.data.remote.dto.LocationBatchResponse
import com.lux.field.data.remote.dto.LoginRequest
import com.lux.field.data.remote.dto.LoginResponse
import com.lux.field.data.remote.dto.PhotoUploadResponse
import com.lux.field.data.remote.dto.TaskUpdateRequest
import com.lux.field.data.remote.dto.TaskUpdateResponse
import com.lux.field.data.remote.dto.WorkOrderDetailDto
import com.lux.field.data.remote.dto.WorkOrderDto
import com.lux.field.data.remote.dto.WorkOrderTasksDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface LuxApi {

    @POST("api/ops/auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @GET("api/cortex/work-orders")
    suspend fun getWorkOrders(@Query("status") status: String = "assigned"): List<WorkOrderDto>

    @GET("api/cortex/work-orders/{id}")
    suspend fun getWorkOrderDetail(@Path("id") id: String): WorkOrderDetailDto

    @GET("api/cortex/work-orders/{id}/tasks")
    suspend fun getWorkOrderTasks(@Path("id") id: String): WorkOrderTasksDto

    @POST("api/ops/task-update")
    suspend fun updateTaskStatus(@Body request: TaskUpdateRequest): TaskUpdateResponse

    @POST("api/ops/location/batch")
    suspend fun uploadLocationBatch(@Body request: LocationBatchRequest): LocationBatchResponse

    @Multipart
    @POST("api/ops/photos/upload")
    suspend fun uploadPhoto(
        @Part photo: MultipartBody.Part,
        @Part("photoId") photoId: RequestBody,
        @Part("taskId") taskId: RequestBody,
        @Part("workOrderId") workOrderId: RequestBody,
        @Part("cameraFacing") cameraFacing: RequestBody,
        @Part("capturedAt") capturedAt: RequestBody,
        @Part("latitude") latitude: RequestBody?,
        @Part("longitude") longitude: RequestBody?,
    ): PhotoUploadResponse
}
