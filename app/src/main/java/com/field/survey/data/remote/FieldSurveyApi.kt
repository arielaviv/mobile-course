package com.field.survey.data.remote

import com.field.survey.data.remote.dto.LoginRequest
import com.field.survey.data.remote.dto.LoginResponse
import com.field.survey.data.remote.dto.TaskUpdateRequest
import com.field.survey.data.remote.dto.TaskUpdateResponse
import com.field.survey.data.remote.dto.WorkOrderDetailDto
import com.field.survey.data.remote.dto.WorkOrderDto
import com.field.survey.data.remote.dto.WorkOrderTasksDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface FieldSurveyApi {

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
}
