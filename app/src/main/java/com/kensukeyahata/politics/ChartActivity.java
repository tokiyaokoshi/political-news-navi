package com.kensukeyahata.politics;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.util.Log;
import android.widget.Toast;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.CompletableFuture;


public class ChartActivity extends AppCompatActivity implements MindMapSelectionListener {
    private static final String TAG = "ChartActivity";
    private TextView resultTextView;
    //private PolicyAnalyzer policyAnalyzer;//後に削除
    private AISummarizer aiSummarizer;
    private ExecutorService executorService;
    //private Map<String, String> topicUrlMap;//削除予定

    private List<String> partyNames;
    private MindMapView mindMapView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chart); // activity_chart.xmlのレイアウトを使用

        // レイアウトからMindMapViewとTextViewを取得
        mindMapView = findViewById(R.id.mind_map_view);
        resultTextView = findViewById(R.id.textView_result);

        // MindMapViewにリスナーとして自身を登録
        if (mindMapView != null) {
            mindMapView.setSelectionListener(this);
        }

        // 初期化
        //policyAnalyzer = new PolicyAnalyzer();削除予定
        aiSummarizer = new AISummarizer();
        executorService = Executors.newSingleThreadExecutor();

        // トピックとURLのマッピングを初期化
        //initializeTopicUrlMap();//削除予定
        initializePartyData();

    }

    @Override
    public void onOptionSelected(String selectedTopic) {
        Log.d("MINDMAP_DEBUG", "ChartActivity received topic: " + selectedTopic);

        if ("社会課題を検索".equals(selectedTopic)) {
            showSearchDialog();
        } else if ("衆議院選挙".equals(selectedTopic) || "参議院選挙".equals(selectedTopic)) {
            showElectionInputDialog(selectedTopic);
        }
    }

    private void showSearchDialog() {
        // 1. カスタムレイアウトを読み込む
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_search, null);

        // 2. ダイアログ内のUI部品をすべて取得する
        final EditText input = dialogView.findViewById(R.id.dialog_edittext_topic);

        // 既存のボタン
        Button btnTax = dialogView.findViewById(R.id.dialog_button_keyword_tax);
        Button btnChildcare = dialogView.findViewById(R.id.dialog_button_keyword_childcare);
        Button btnEconomy = dialogView.findViewById(R.id.dialog_button_keyword_economy);
        Button btnPrices = dialogView.findViewById(R.id.dialog_button_keyword_prices);

        // ★追加したボタン
        Button btnSecurity = dialogView.findViewById(R.id.dialog_button_keyword_security);
        Button btnPension = dialogView.findViewById(R.id.dialog_button_keyword_pension);
        Button btnConstitution = dialogView.findViewById(R.id.dialog_button_keyword_constitution);
        Button btnRegional = dialogView.findViewById(R.id.dialog_button_keyword_regional);

        // 3. すべてのボタンにクリックイベントを設定する
        View.OnClickListener keywordClickListener = v -> {
            Button b = (Button) v;
            input.setText(b.getText().toString());
        };

        btnTax.setOnClickListener(keywordClickListener);
        btnChildcare.setOnClickListener(keywordClickListener);
        btnEconomy.setOnClickListener(keywordClickListener);
        btnPrices.setOnClickListener(keywordClickListener);

        // ★追加したボタンのイベント設定
        btnSecurity.setOnClickListener(keywordClickListener);
        btnPension.setOnClickListener(keywordClickListener);
        btnConstitution.setOnClickListener(keywordClickListener);
        btnRegional.setOnClickListener(keywordClickListener);

        // 4. ダイアログを作成して表示する
        new AlertDialog.Builder(this)
                .setTitle("社会課題の分析")
                .setView(dialogView)
                .setPositiveButton("分析", (dialog, which) -> {
                    String topic = input.getText().toString().trim();
                    if (!topic.isEmpty()) {
                        startAnalysis(topic);
                    }
                })
                .setNegativeButton("キャンセル", null)
                .show();
    }

    private void startAnalysis(String topic) {
        resultTextView.setText(topic + "について各党の方針をAIに質問中...");
        if (mindMapView != null) {
            mindMapView.clearAnalysisNodes();
            mindMapView.showKeywordNode(topic);
        }
        // 全ての政党に対して処理を実行
        for (String partyName : partyNames) {
            askAiAndAddNodeForParty(partyName, topic);
        }
    }


    private void askAiAndAddNodeForParty(String partyName, String selectedTopic) {
        executorService.execute(() -> {
            try {
                Thread.sleep(1000);
                // ★ 修正点 3: スクレイピングの代わりにAIに直接質問するメソッドを呼び出す
                CompletableFuture<String> analysisFuture = aiSummarizer.getPolicySummary(partyName, selectedTopic);
                String analysisResult = analysisFuture.get(); // 同期的に結果を取得
                Log.d("AI_RESPONSE", partyName + ": " + analysisResult); // AIからの返答
                // マインドマップに新しいノードを追加 (UIスレッドで実行)
                String cleanedResult = analysisResult.replaceAll("(\\s*\\n\\s*){2,}", "\n");

                runOnUiThread(() -> {
                    if (mindMapView != null) {
                        mindMapView.addAnalysisNode(partyName,  cleanedResult);
                    }
                    resultTextView.setText(partyName + "の分析が完了しました。");
                });

            } catch (Exception e) {
                Log.e(TAG, partyName + "の処理中にエラーが発生しました", e);
                runOnUiThread(() -> resultTextView.setText(partyName + "の分析中にエラーが発生しました。"));
            }
        });
    }
    /**
     * 政党リストを初期化します。
     */
    private void initializePartyData() {
        partyNames = Arrays.asList(
                "自民党", "公明党", "立憲民主党", "日本維新の会", "共産党",
                "国民民主党", "れいわ新選組", "参政党", "社民党", "日本保守党", "みんつく党"
        );
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        // アプリ終了時にExecutorServiceとAISummarizerをシャットダウン
        if (executorService != null) {
            executorService.shutdown();
        }
        if (aiSummarizer != null) {
            aiSummarizer.shutdown();
        }
    }

    // ChartActivity.java の末尾などに追加
    private void showElectionInputDialog(String electionType) {
        // 1. カスタムレイアウトを読み込む
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_election_input, null);

        // 2. ダイアログ内のUI部品を取得する
        final EditText editYear = dialogView.findViewById(R.id.dialog_edittext_year);
        final Spinner spinnerPrefecture = dialogView.findViewById(R.id.dialog_spinner_prefecture);
        final EditText editMunicipality = dialogView.findViewById(R.id.dialog_edittext_municipality);

        // 3. Spinnerに都道府県リストを設定する
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.prefectures_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPrefecture.setAdapter(adapter);

        // 4. ダイアログを作成して表示する
        new AlertDialog.Builder(this)
                .setTitle(electionType) // "衆議院選挙" または "参議院選挙"
                .setView(dialogView)
                .setPositiveButton("決定", (dialog, which) -> {
                    String year = editYear.getText().toString().trim();
                    String prefecture = spinnerPrefecture.getSelectedItem().toString();
                    String municipality = editMunicipality.getText().toString().trim();

                    if (year.isEmpty() || municipality.isEmpty()) {
                        Toast.makeText(this, "年と市区町村を入力してください。", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String criteriaText = "投票年: " + year + "年\n"
                            + "都道府県: " + prefecture + "\n"
                            + "市区町村: " + municipality;

                    // 手順2: タップされた親ノードのIDを特定
                    String triggerNodeId = "衆議院選挙".equals(electionType) ? "party_shugiin" : "party_sangiin";
                    String criteriaNodeId = mindMapView.addSearchCriteriaNode(triggerNodeId, criteriaText);

                    if (criteriaNodeId == null) {
                        Toast.makeText(this, "エラー: 検索ノードを作成できませんでした。", Toast.LENGTH_SHORT).show();
                        return;
                    }


                    // AIに候補者情報を問い合わせる
                    resultTextView.setText(prefecture + municipality + "の候補者を検索中...");
                    aiSummarizer.getCandidateInfo(electionType, year, prefecture, municipality)
                            .thenAccept(aiResponse -> {
                                // UIスレッドでマインドマップを更新
                                runOnUiThread(() -> {
                                    String messageForNode;

                                    // ▼▼▼ ここからが変更点 ▼▼▼
                                    if (aiResponse != null && aiResponse.contains(":")) {
                                        // 【成功した場合】候補者ノードを追加
//                                        mindMapView.addCandidateNode(electionType, aiResponse);
                                        messageForNode = aiResponse;
                                        resultTextView.setText("候補者リストの表示が完了しました。");
                                    } else {
                                        // 【失敗した場合】「情報がありません」ノードを追加
                                        String noInfoMessage = "情報がありませんでした。\n選挙が行われていない年か、AIが未対応の地域の可能性があります。";
                                        // 失敗した場合もaddCandidateNodeを使い、メッセージの先頭に「:」を入れることで1つのノードとして表示させる
                                        //mindMapView.addCandidateNode(electionType, "候補者情報: " + noInfoMessage);
                                        messageForNode = "候補者情報: 情報がありませんでした。\n選挙が行われていない年か、AIが未対応の地域の可能性があります。";
                                        resultTextView.setText("候補者情報が見つかりませんでした。");
                                    }
                                    // ▲▲▲ ここまでが変更点 ▲▲▲
                                    // 手順5: 候補者ノードを「検索条件ノードの子」として追加
                                    mindMapView.addCandidateNode(criteriaNodeId, messageForNode);
                                });
                            });
                })
                .setNegativeButton("キャンセル", null)
                .show();
    }
}
