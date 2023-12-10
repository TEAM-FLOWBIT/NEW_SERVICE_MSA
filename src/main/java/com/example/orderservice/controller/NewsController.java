package com.example.orderservice.controller;

import com.example.orderservice.dto.NewsDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.web.bind.annotation.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

@RestController
@Slf4j
@RequestMapping("/news-service")
public class NewsController {
    Environment env;
    @Autowired
    public NewsController(Environment env){
        this.env = env;
    }

    @GetMapping("/health_check")
    public String status(){

        return String.format("It's Working in Order Service on PORT %s"
                , env.getProperty("local.server.port"));

    }

    @GetMapping("/news")
    public ResponseEntity<ArrayList<NewsDto>> getNews(){

        ArrayList<NewsDto> newsDtos = new ArrayList<>();

        String responseData = "";
        String clientId = env.getProperty("naver.clientId");
        String clientSecret = env.getProperty("naver.clientSecret");
        String apiUrl = "https://openapi.naver.com/v1/search/news.json?query=비트코인&display=100&start=1&sort=sim";

        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(apiUrl)
                .addHeader("X-Naver-Client-Id", clientId)
                .addHeader("X-Naver-Client-Secret", clientSecret)
                .build();
        try {
            Response response = client.newCall(request).execute();
            System.out.println(response.body().getClass()
                    .getName());

            responseData = response.body().string();

            // 여기에서 responseData를 원하는 대로 처리합니다.
            //System.out.println(responseData);

        } catch (IOException e) {
            e.printStackTrace();
        }

        String jsonString = responseData;
        // JSON 문자열을 JSONObject로 파싱
        JSONObject jsonObject = new JSONObject(jsonString);

        // 필요한 데이터 추출
        int total = jsonObject.getInt("total");
        JSONArray items = jsonObject.getJSONArray("items");

        System.out.println("Total: " + total);

        // items 배열 순회
        for (int i = 0; i < items.length(); i++) {

            NewsDto newsDto = new NewsDto();

            JSONObject item = items.getJSONObject(i);
            newsDto.setTitle(item.getString("title"));
            newsDto.setOriginalling(item.getString("originallink"));
            newsDto.setLink(item.getString("link"));
            newsDto.setDescription(item.getString("description"));
            newsDto.setPubDate(item.getString("pubDate"));

            newsDtos.add(newsDto);
//            // 각 객체의 필요한 데이터 추출
//            String title = item.getString("title");
//            String originallink = item.getString("originallink");
//            String link = item.getString("link");
//            String description = item.getString("description");
//            String pubDate = item.getString("pubDate");
//
//            // 추출한 데이터 출력 또는 활용
//            System.out.println("Title: " + title);
//            System.out.println("Link: " + link);
//            System.out.println("Description: " + description);
//            System.out.println("PubDate: " + pubDate);
//            System.out.println();
        }
        return ResponseEntity.status(HttpStatus.OK).body(newsDtos);

    }

}
