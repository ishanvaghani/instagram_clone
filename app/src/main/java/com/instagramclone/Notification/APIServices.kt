package com.instagramclone.Notification

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface APIService {
    @Headers(
        "Content-Type:application/json",
        "Authorization:key=AAAAF1S21Qo:APA91bFonFJV3q2xGj2AJbo_1fGNtu6WPddfPcN5CNvZuirGacwWedUAVNvhGOrpPhxT-3_hOG0lLXNXNYKpYATfZ71O9CGRqr0s9rrE5LkWSjX3ijKo7BwoBXJROy8cgxfvs0pcSOV_"
    )
    @POST("fcm/send")
    fun sendNotification(@Body body: Sender): Call<MyResponse>
}