package com.example.orderservice.controller;

import com.example.orderservice.dto.NewsDto;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
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
import org.jsoup.nodes.Document;
import java.io.IOException;
import java.net.URI;
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
    public ResponseEntity<ArrayList<NewsDto>> getNews() throws Exception{

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
            newsDto.setImg(linkPreview(newsDto.getOriginalling()));

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
    public static String getMetaTagContent(Document document, String metaTagName) {
        Elements metaTags = document.select(metaTagName);
        for (Element metaTag : metaTags) {
            String content = metaTag.attr("content");
            if (!content.isEmpty()) {
                return content;
            }
        }
        return "";
    }

    public static String getMetaTagSrc(Document document, String tagName) {
        Elements elements = document.select(tagName);
        for (Element element : elements) {
            String src = element.attr("src");
            if (!src.isEmpty()) {
                return src;
            }
        }
        return "";
    }

    public String linkPreview(String url) throws Exception {
        if (!url.startsWith("http")) {
            url = "http://" + url;
        }

        Document document = Jsoup.connect(url).get();

        URI uri = new URI(url);
        String domain = uri.getHost();

        String link = url;
        String description = "";
        String thumbnail = "";
        String favicon = "";

        //System.out.println(document.toString());

        if (!domain.startsWith("www")) {
            domain = domain;
        }

        description = getMetaTagContent(document, "meta[name=description]");
        if (description.equals("")) {
            description = getMetaTagContent(document, "meta[property=og:description]");
        }

        thumbnail = getMetaTagContent(document, "meta[property=og:image]");
        if (thumbnail.equals("")) {
            thumbnail = getMetaTagContent(document, "meta[property=twitter:image]");
            if (thumbnail.equals("")) {
                thumbnail = getMetaTagSrc(document, "img");
            }
        }
        if (!thumbnail.startsWith("http")) {
            thumbnail = "http://" + domain + thumbnail;
        }

        Element faviconElem = document.head().select("link[href~=.*\\.(ico|png)]").first();
        if (faviconElem != null) {
            favicon = faviconElem.attr("href");
        } else if (document.head().select("meta[itemprop=image]").first() != null) {
            favicon = document.head().select("meta[itemprop=image]").first().attr("content");
        }

        if (!favicon.startsWith("http")) {
            if (domain.startsWith("www")) {
                favicon = "http://" + domain + favicon;
            } else {
                favicon = "http://www." + domain + favicon;
            }
        }

        JSONObject result = new JSONObject();
        result.put("domain", domain);
        result.put("link", link);
        result.put("description", description);
        result.put("thumbnail", thumbnail);
        result.put("favicon", favicon);

        //System.out.println(result.get("thumbnail"));

        return result.get("thumbnail").toString();
    }

}
