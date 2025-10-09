// GoogleSearchClient.java

package com.kensukeyahata.politics;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup; // ★ Jsoupライブラリをインポート

import java.net.URLEncoder;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class GoogleSearchClient {

    private static final String API_KEY = com.kensukeyahata.politics.BuildConfig.GOOGLE_API_KEY;
    private static final String SEARCH_ENGINE_ID = com.kensukeyahata.politics.BuildConfig.GOOGLE_SEARCH_ENGINE_ID;

    private final OkHttpClient client = new OkHttpClient();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public CompletableFuture<String> search(String query) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = "https://www.googleapis.com/customsearch/v1?key=" + API_KEY
                        + "&cx=" + SEARCH_ENGINE_ID + "&q=" + URLEncoder.encode(query, "UTF-8");

                Request request = new Request.Builder().url(url).build();
                Response response = client.newCall(request).execute();

                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    JSONObject json = new JSONObject(responseBody);
                    JSONArray items = json.optJSONArray("items");

                    if (items == null || items.length() == 0) {
                        return "検索結果がありませんでした。";
                    }

                    // ★ 変更点: 検索結果1位のURLを取得
                    JSONObject topResult = items.getJSONObject(0);
                    String pageUrl = topResult.getString("link");

                    // ★ 新しい処理: URLからページの本文全体を取得 (スクレイピング)
                    try {
                        // Jsoupを使ってURLに接続し、HTMLからテキスト情報のみを抽出
                        String pageText = Jsoup.connect(pageUrl).get().text();

                        // AIに渡す情報が長くなりすぎないよう、文字数を制限して返す
                        return pageText.substring(0, Math.min(pageText.length(), 10000));
                    } catch (Exception e) {
                        e.printStackTrace();
                        // 本文の取得に失敗した場合は、従来通りスニペットを返す
                        return "・" + topResult.getString("snippet");
                    }

                } else {
                    return "検索エラー: " + response.code();
                }

            } catch (Exception e) {
                e.printStackTrace();
                return "検索中に例外が発生しました。";
            }
        }, executorService);
    }
}