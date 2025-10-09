package com.kensukeyahata.politics;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;


public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button chartButton = findViewById(R.id.button_chart);
        Button diagnosisButton = findViewById(R.id.button_diagnosis);
        Button elseButton = findViewById(R.id.button_else);

        // 各政党比較表のボタン押したら
        chartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ChartActivity.class);
                startActivity(intent);
            }
        });

        // 推し診断ボタン押したら
        diagnosisButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("診断を開始します")
                        .setMessage("いくつかの簡単な質問に答えて、あなたのタイプを診断します。よろしいですか？")
                        .setPositiveButton("診断を開始する", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // QuestionActivityを呼び出す
                                Intent intent = new Intent(MainActivity.this, DiagnosisActivity.class);

                                // これから表示する質問の番号（最初は0）を渡す
                                intent.putExtra("QUESTION_INDEX", 0);

                                // 回答を保存するための空のリストを渡す
                                intent.putExtra("ANSWERS", new ArrayList<String>());

                                startActivity(intent);
                            }
                        })
                        .setNegativeButton("キャンセル", null)
                        .show();
            }
        });


        // その他ボタンのロジック
        elseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ElseActivity.class);
                startActivity(intent);
            }
        });
    }
}