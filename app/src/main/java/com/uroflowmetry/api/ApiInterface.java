package com.uroflowmetry.api;

import com.google.gson.JsonObject;
import com.uroflowmetry.models.BaseResult;
import com.uroflowmetry.models.ResultLogin;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface  ApiInterface {


    @POST("app_log_in")
    Call<ResultLogin> doLogin(@Body JsonObject jsonObject);

    @POST("app_sign_up")
    Call<BaseResult> doSignup(@Body JsonObject jsonObject);

    @POST("register_app_data")
    Call<BaseResult> doSendData(@Body JsonObject jsonObject);


}
