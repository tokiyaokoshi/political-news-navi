package com.kensukeyahata.politics;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ResultActivity extends AppCompatActivity {

    // ★ 1. 政党名とその位置(bias)を管理するための小さなクラスを定義
    private static class PartyPosition {
        String name;
        float position; // 0.0 (左派) ~ 1.0 (右派)

        PartyPosition(String name, float position) {
            this.name = name;
            this.position = position;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        // --- UI部品を取得 ---
        TextView resultText = findViewById(R.id.textView_result);
        Button backButton = findViewById(R.id.button_back_to_menu);
        ImageView userIndicator = findViewById(R.id.image_user_result_indicator);
        TextView closestPartiesText = findViewById(R.id.textView_closest_parties); // ★追加したTextViewを取得

        // --- データを受け取る ---
        Intent incomingIntent = getIntent();
        int rightCount = incomingIntent.getIntExtra("RIGHT_COUNT", 0);
        int leftCount = incomingIntent.getIntExtra("LEFT_COUNT", 0);

        // --- スコアを計算し、テキストとインジケータを更新 ---
        int totalAnswers = rightCount + leftCount;
        float score = 0.5f;

        if (totalAnswers > 0) {
            score = (float) rightCount / (float) totalAnswers;
        }

        // インジケータの位置を動かす
        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) userIndicator.getLayoutParams();
        params.horizontalBias = score;
        userIndicator.setLayoutParams(params);

        // パーセンテージ表示
        float rightPercentage = score * 100;
        float leftPercentage = (1 - score) * 100;
        String resultString = String.format(Locale.JAPAN, "左派 %.0f%% / 右派 %.0f%%", leftPercentage, rightPercentage);
        resultText.setText(resultString);

        // ▼▼▼ ここからが今回の修正のメインです ▼▼▼

        // ★ 2. 各政党の位置情報リストを作成 (activity_result.xmlのbias値と一致させる)
        List<PartyPosition> partyPositions = new ArrayList<>();
        partyPositions.add(new PartyPosition("共産党", 0.05f));
        partyPositions.add(new PartyPosition("社民党", 0.15f));
        partyPositions.add(new PartyPosition("公明党", 0.25f));
        partyPositions.add(new PartyPosition("れいわ新選組", 0.35f));
        partyPositions.add(new PartyPosition("立憲民主党", 0.45f));
        partyPositions.add(new PartyPosition("自民党", 0.55f));
        partyPositions.add(new PartyPosition("国民民主党", 0.65f));
        partyPositions.add(new PartyPosition("日本維新の会", 0.75f));
        partyPositions.add(new PartyPosition("参政党", 0.85f));
        partyPositions.add(new PartyPosition("日本保守党", 0.95f));


        // ★ 3. ユーザーのスコアとの距離が近い順に政党を並び替える
        final float userScore = score; // ラムダ式内で使うためにfinalにする
        Collections.sort(partyPositions, (p1, p2) -> {
            float diff1 = Math.abs(p1.position - userScore);
            float diff2 = Math.abs(p2.position - userScore);
            return Float.compare(diff1, diff2);
        });

        // ★ 4. 最も近い3党の名前を取得して、表示用の文字列を作成する
        String closestPartiesString = "あなたの政治的思考は" +
                partyPositions.get(0).name + "・" +
                partyPositions.get(1).name + "・" +
                partyPositions.get(2).name + "に近い傾向にあります！";

        // ★ 5. 作成した文字列をTextViewにセットする
        closestPartiesText.setText(closestPartiesString);

        // ▲▲▲ 修正はここまで ▲▲▲


        // --- 「メニューに戻る」ボタンの処理 ---
        backButton.setOnClickListener(v -> {
            Intent backToMainIntent = new Intent(ResultActivity.this, MainActivity.class);
            backToMainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(backToMainIntent);
        });
    }
}