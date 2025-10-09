package com.kensukeyahata.politics;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.util.Log;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;

public class DiagnosisActivity extends AppCompatActivity {

    private static final String TAG = "DiagnosisActivity";

    private String[] questions = {
            "消費税撤廃に賛成だ",
            "日本が世界で1番住みやすい国だ",
            "新しい道を開拓するのが好きだ",
            "移民受け入れに反対だ",
    };

    // ★ UI部品の変数をクラスのメンバーとして宣言
    private TextView textViewQuestionNumber;
    private TextView textViewQuestion;

    private int questionIndex = 0;
    private ArrayList<String> answers = new ArrayList<>();
    private int right = 0;
    private int left = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diagnosis);
        Log.d(TAG, "--- onCreate: 画面生成開始 ---");

        // ★ UI部品を取得
        textViewQuestionNumber = findViewById(R.id.text_question_number);
        textViewQuestion = findViewById(R.id.text_question);
        Button yesButton = findViewById(R.id.button_yes);
        Button noButton = findViewById(R.id.button_no);

        showQuestion(); // 最初の質問を表示

        // ボタンのクリックイベント
        yesButton.setOnClickListener(v -> handleAnswer("yes"));
        noButton.setOnClickListener(v -> handleAnswer("no"));

        Log.d(TAG, "--- onCreate: 画面生成完了 ---");
    }

    private void showQuestion() {
        Log.d(TAG, "showQuestion: 質問 " + (questionIndex + 1) + " を表示します。");

        // ★ 質問番号のテキストも更新するように追加
        textViewQuestionNumber.setText("質問" + (questionIndex + 1));
        textViewQuestion.setText(questions[questionIndex]);
    }

    // DiagnosisActivity.java の中

    private void handleAnswer(String answer) {
        Log.d(TAG, "--- handleAnswer: 開始 ---");
        Log.d(TAG, "handleAnswer: ユーザーの回答: " + answer);

        // 回答に応じてスコアを計算
        calculateScore(answer, true); // true = 加算モード

        // 回答を履歴リストに追加
        answers.add(answer);

        // 次の質問番号へ
        questionIndex++;

        // ★★まだ次の質問がある場合★★
        if (questionIndex < questions.length) {
            Log.d(TAG, "handleAnswer: 【IF分岐】次の質問へ進みます");

            // ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
            // ★ ここで次の質問を表示する命令を呼び出すのが重要です ★
            // ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
            showQuestion();
        }
        // ★★最後の質問だった場合★★
        else {
            Log.d(TAG, "handleAnswer: 【ELSE分岐】結果画面へ進みます");

            // 結果画面へ遷移する
            Intent intent = new Intent(DiagnosisActivity.this, ResultActivity.class);
            intent.putExtra("ANSWERS", answers); // 最終的な回答リスト
            intent.putExtra("RIGHT_COUNT", right);
            intent.putExtra("LEFT_COUNT", left);
            startActivity(intent);
            finish(); // この画面を終了させる
        }
    }

    private void calculateScore(String answer, boolean isAdding) {
        int value = isAdding ? 1 : -1; // 加算なら1、減算なら-1

        switch (questionIndex) {
            case 0: // 質問1
            case 2: // 質問3
                if (answer.equals("yes")) {
                    left += value;
                } else {
                    right += value;
                }
                break;
            case 1: // 質問2
            case 3: // 質問4
                if (answer.equals("yes")) {
                    right += value;
                } else {
                    left += value;
                }
                break;
        }
        Log.d(TAG, "calculateScore: 更新後のright: " + right + ", left: " + left);
    }

    @Override
    public void onBackPressed() {
        // 最初の質問でなければ、一つ前に戻る
        if (questionIndex > 0) {

            Log.d(TAG, "onBackPressed: 一つ前の質問に戻ります。");

            // 質問番号を一つ戻す
            questionIndex--;

            // 最後の回答を履歴から取得して削除
            String lastAnswer = answers.remove(answers.size() - 1);

            // 直前の回答で加算したカウントを減算（元に戻す）
            calculateScore(lastAnswer, false); // falseを指定して減算モードに

            // 前の質問を再表示
            showQuestion();
        }
        // 最初の質問で戻るボタンが押された場合
        else {
            Log.d(TAG, "onBackPressed: 最初の質問なので、確認ダイアログを表示します。");
            // 確認ダイアログを表示
            new AlertDialog.Builder(this)
                    .setTitle("確認")
                    .setMessage("診断を中止して最初の画面に戻りますか？")
                    .setPositiveButton("はい", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .setNegativeButton("いいえ", null)
                    .show();
        }
    }
}