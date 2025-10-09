package com.kensukeyahata.politics;

/**
 * MindMapViewでの選択肢の変更をリッスンするためのインターフェース。
 * このインターフェースを実装することで、選択されたトピックを
 * 他のクラス（例: MainActivity）に通知できます。
 */
public interface MindMapSelectionListener {

    /**
     * ノード内の選択肢が選択されたときに呼び出されます。
     *
     * @param selectedTopic 選択された選択肢のテキスト
     */
    void onOptionSelected(String selectedTopic);
}