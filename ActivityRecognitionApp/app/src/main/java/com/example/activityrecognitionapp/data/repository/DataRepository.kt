package com.example.activityrecognitionapp.data.repository

import android.util.Log
import com.example.activityrecognitionapp.BuildConfig
import com.example.activityrecognitionapp.data.model.ActivityCount
import com.example.activityrecognitionapp.data.model.ActivityDataSupabase
import com.example.activityrecognitionapp.data.network.SupabaseApiClient
import com.example.activityrecognitionapp.data.network.SupabaseApiService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataRepository@Inject constructor(
    private val supabaseApiService: SupabaseApiService)
{

    fun sendActivityData(data: ActivityDataSupabase,onResult: (Boolean)-> Unit){
        val call = SupabaseApiClient.apiService.insertActivityData(
            authorization = "Bearer ${BuildConfig.supabaseKey}",
            apiKey = BuildConfig.supabaseKey,
            data=data
        )

        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    onResult(true)

                    Log.d("xsaxsa", "Dane aktywności wysłane pomyślnie")
                } else {
                    onResult(false)
                    Log.d("asdas", "Błąd wysyłania danych aktywności: ${response.code()} - ${response.message()} - ${response.errorBody()?.string()}")
                }
            }


            fun getUserActivities(userId: String, onResult: (List<ActivityCount>?) -> Unit) {
                val authHeader = "Bearer ${BuildConfig.supabaseKey}"

                supabaseApiService.getUserActivities(
                    authorization = authHeader,
                    apiKey = BuildConfig.supabaseKey,
                    userId = userId
                ).enqueue(object : Callback<List<ActivityCount>> {
                    override fun onResponse(
                        call: Call<List<ActivityCount>>,
                        response: Response<List<ActivityCount>>
                    ) {
                        if (response.isSuccessful) {
                            onResult(response.body())
                        } else {
                            Log.e("Supabase", "Błąd pobierania danych: ${response.errorBody()?.string()}")
                            onResult(null)
                        }
                    }


                    override fun onFailure(call: Call<List<ActivityCount>>, t: Throwable) {
                        onResult(false)
                        Log.d("dsadsad","Bład połaczenia ${t.message}")
                    }
                })
    }

    }
