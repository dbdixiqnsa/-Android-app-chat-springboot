package com.example.chat.network;

import java.util.Map;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface VersionApiService {
    @POST("api/users/verify-version")
    Call<Map<String, Object>> verifyVersion(@Body Map<String, String> payload);
}