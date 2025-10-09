package com.kensukeyahata.politics;

import android.util.Log;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.ListenableFuture;


import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AISummarizer {
    private static final String TAG = "AISummarizerDebug";
    private final GenerativeModelFutures modelFutures;
    private final ExecutorService executorService;
    private final GoogleSearchClient searchClient;

    public AISummarizer() {
        GenerativeModel generativeModel = new GenerativeModel(
                "gemini-2.5-flash-lite",
                com.kensukeyahata.politics.BuildConfig.GEMINI_API_KEY
        );
        this.modelFutures = GenerativeModelFutures.from(generativeModel);
        this.executorService = Executors.newFixedThreadPool(4);
        this.searchClient = new GoogleSearchClient();
    }


    public CompletableFuture<String> getPolicySummary(String partyName, String topic) {
        String searchQuery = partyName + " " + topic + " 政策";
        Log.d(TAG, "【1. 検索開始】クエリ: " + searchQuery);

        return searchClient.search(searchQuery).thenCompose(searchResults -> {
            Log.d(TAG, "【2. 検索完了】結果: \n" + searchResults);
            return CompletableFuture.supplyAsync(() -> {
                try {
                    String prompt = String.format(
                            "日本の政党である「%s」の「%s」に関する現在の基本的な方針や立場を、" +
                                    "専門知識がない人にも分かりやすく50字以内で簡潔に説明してください。"+
                                    "またその方針についてメリットデメリットをそれぞれ50字以内で簡潔に説明してください"+
                                    "回答は方針 : ,メリット : ,デメリット : ,の3行のテキストでお願いします\n\n"+
                                    "--- 参考情報 ---\n%s",
                            partyName, topic, searchResults
                    );

                    Log.d(TAG, "【3. AI要約開始】プロンプト: \n" + prompt);
                    Content content = new Content.Builder().addText(prompt).build();


                    ListenableFuture<GenerateContentResponse> future = modelFutures.generateContent(content);
                    GenerateContentResponse response = future.get();
                    String summary = response.getText();
                    Log.d(TAG, "【4. AI要約完了】回答: \n" + summary);
                    return (summary != null && !summary.isEmpty()) ? summary : "情報を生成できませんでした。";
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                    if (e.getCause() != null) {
                        return "エラーが発生しました: " + e.getCause().getMessage();
                    }
                    return "エラーが発生しました: " + e.getMessage();
                }
            }, executorService);
        });
    }

    public CompletableFuture<String> getCandidateInfo(String electionType, String year, String prefecture, String municipality) {
        String searchQuery = year + "年 " + electionType + " " + prefecture + municipality + " 候補者";
        Log.d(TAG, "【1. 検索開始】クエリ: " + searchQuery);
        return searchClient.search(searchQuery).thenCompose(searchResults -> {
            Log.d(TAG, "【2. 検索完了】結果: \n" + searchResults);
            return CompletableFuture.supplyAsync(() -> {
                try {
                    String prompt = String.format(
                            "以下の参考情報を候補者全員の名前を、" +
                                    "まとめて教えてください。" +
                                    "回答は絶対に以下のフォーマットに従ってください。候補者ごとに改行してお願いします:\n\n" +
                                    "候補者名1: 公約1\n" +
                                    "候補者名2: 公約2"+
                                    "--- 参考情報 ---\n%s",
//                            year, electionType, prefecture, municipality,
                            searchResults
                    );
                    Log.d(TAG, "【3. AI要約開始】プロンプト: \n" + prompt);
                    Content content = new Content.Builder().addText(prompt).build();

                    ListenableFuture<GenerateContentResponse> future = modelFutures.generateContent(content);
                    GenerateContentResponse response = future.get();
                    String summary = response.getText();

                    Log.d(TAG, "【4. AI要約完了】回答:  \n" + summary);
                    return (summary != null && !summary.isEmpty()) ? summary : "候補者情報を生成できませんでした。";
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                    if (e.getCause() != null) {
                        return "エラーが発生しました: " + e.getCause().getMessage();
                    }
                    return "エラーが発生しました: " + e.getMessage();
                }
            }, executorService);
        });
    }

    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}