package com.kensukeyahata.politics;

import java.util.ArrayList;
import java.util.List;
import android.text.StaticLayout;

public class MindMap {
    public String id;
    public String label; // ノードの基本ラベル
    public boolean isSearchNode = false;
    public boolean isLineToParentHidden = false;
    public float x;
    public float y;
    public boolean isExpanded;
    public MindMap parent;
    public List<MindMap> children;
    public StaticLayout textLayout = null;      // 自動改行されたテキスト情報を保持します
    public boolean isElectionInputNode = false;
    public boolean isAnalysisNode = false;      // このノードがAI分析ノードかどうかを判定します
    public boolean isCandidateNode = false;
    public boolean isAnalysisKeywordNode = false;
    // --- 選択肢ノード（チェックボックス的な機能）用のフィールド ---
    public boolean isSelectableNode = false;      // このノードが選択肢機能を持つか
    public List<String> options;                // 選択肢の文字列リスト (例: ["はい", "いいえ"])
    public int selectedOptionIndex = -1;       // 現在選択されているオプションのインデックス (-1なら未選択)
    public boolean areOptionsVisible = false;     // 選択肢の表示状態フラグ
    public boolean isSearchCriteriaNode = false;

    public MindMap getParent() {
        return this.parent;
    }

    public MindMap(String id, String label, float x, float y, MindMap parent) {
        this.id = id;
        this.label = label;
        this.x = x;
        this.y = y;
        this.parent = parent;
        this.isExpanded = false; // デフォルトでは折りたたまれた状態
        this.areOptionsVisible = false;
        this.children = new ArrayList<>();
        this.options = new ArrayList<>();
        // isSelectableNode はデフォルトで false のまま
    }

    public MindMap(String id, String label, float x, float y, MindMap parent, List<String> initialOptions) {
        this(id, label, x, y, parent);
        this.isSelectableNode = true;
        this.areOptionsVisible = false;
        if (initialOptions != null) {
            this.options = new ArrayList<>(initialOptions);
        } else {
            this.options = new ArrayList<>(); // nullで渡された場合も空リストで初期化
        }
    }


    public void addChild(MindMap child) {
        if (child != null) {
            this.children.add(child);
        }
    }

    public List<MindMap> getChildren() {
        return children;
    }

    // --- 選択肢関連のメソッド ---

    public boolean areOptionsVisible() {
        return this.areOptionsVisible;
    }

    public void toggleOptionsVisibility() {
        this.areOptionsVisible = !this.areOptionsVisible;
    }

    public void setOptionsVisibility(boolean visible) {
        this.areOptionsVisible = visible;
    }
    public boolean isSelectableNode() {
        return this.isSelectableNode;
    }

    public List<String> getOptions() {
        return this.options;
    }

    public int getSelectedOptionIndex() {
        return this.selectedOptionIndex;
    }

    public void setSelectedOptionIndex(int index) {
        if (index >= -1 && index < options.size()) { // -1 (未選択) または有効なインデックス
            this.selectedOptionIndex = index;
        }
    }

    public String getSelectedOptionText() {
        if (selectedOptionIndex >= 0 && selectedOptionIndex < options.size()) {
            return options.get(selectedOptionIndex);
        }
        return null; // 未選択または無効なインデックスの場合
    }

}
