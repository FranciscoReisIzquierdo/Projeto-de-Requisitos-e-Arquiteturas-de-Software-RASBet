package com.server_side;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class CurrentAPI implements DataAPI{

    private API api;
    private OkHttpClient client;

    public CurrentAPI(){
        this.api = new API();
        this.client =  new OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build();
    }
    
    public String fetchdata() throws IOException {
       
        
        Request request = new Request.Builder()
               .url("http://ucras.di.uminho.pt/v1/games/").build();
        String body;
        
        Response response = this.client.newCall(request).execute();
        body = response.body().string();
        
        return this.api.fetch(body);
    }
}
