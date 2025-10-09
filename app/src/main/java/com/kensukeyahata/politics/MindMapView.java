package com.kensukeyahata.politics;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


public class MindMapView extends View {
    //ノード設定
    private List<MindMap> allNodes;
    private Map<String, MindMap> nodeMap;
    private float nodeRectWidth = 200f;
    private float searchNodeWidth = 250f;
    private float nodeRectHeight = 80f;
    private float analysisNodeWidth = 1500;
    private float analysisNodeHeight = 170f;
    //レイアウト計算用
    private float candidateNodeWidth = 800f;
    private boolean initialLayoutDone = false;
    private RectF contentRectWorld = new RectF();

    //描画設定
    private Paint nodePaint;
    private Paint linePaint;
    private TextPaint textPaint;
    private Paint gridPaint;

    //操作関連
    //拡大縮小
    private ScaleGestureDetector scaleGestureDetector;
    private float scaleFactor = 1.0f;
    private final float minScaleFactor = 0.8f;
    private final float maxScaleFactor = 2.5f;
    private float scrollX = 0f;
    private float scrollY = 0f;
    private float pivotX_world = 0f;
    private float pivotY_world = 0f;

    //スクロール？
    private float lastTouchX_drag;
    private float lastTouchY_drag;
    //ドラッグ操作
    private boolean isDragging = false;

    //余白？
    private float tapPadding = 0f;

    //使ってない？？
    private MindMap parent;

    // 選択イベントを通知するためのリスナー
    private MindMapSelectionListener selectionListener;
    public void setSelectionListener(MindMapSelectionListener listener) {
        this.selectionListener = listener;
    }

    public void clearAnalysisNodes() {
        List<MindMap> nodesToRemove = new ArrayList<>();
        // 削除対象の分析ノードを収集
        for (MindMap node : allNodes) {
            if (node.isAnalysisNode|| node.isAnalysisKeywordNode) {
                nodesToRemove.add(node);
            }
        }

        if (nodesToRemove.isEmpty()) {
            return; // 削除するノードがなければ何もしない
        }

        // メインのリストから削除
        for (MindMap node : nodesToRemove) {
            allNodes.remove(node);
            nodeMap.remove(node.id);
        }

        // 親ノードの子リストからも削除
        for (MindMap parentNode : allNodes) {
            parentNode.children.removeIf(childNode -> childNode.isAnalysisNode);
        }

        Log.d(DEBUG_TAG, "Cleared " + nodesToRemove.size() + " analysis nodes.");
        invalidate(); // 画面を再描画
        calculateContentBounds(); // マップの境界を再計算
    }

    //AI要約機能からノードを新たに作成
    public void addAnalysisNode(String parentTopic, String analysisResult) {
        MindMap parentNode = findNodeByLabel(parentTopic);
        if (parentNode == null) {
            Log.e(DEBUG_TAG, "Parent node not found for topic: " + parentTopic);
            // 適切な親ノードが見つからない場合の処理
            return;
        }

        // 新しいノードのIDとラベルを生成
        String newNodeId = UUID.randomUUID().toString();
//        String newNodeLabel = parentTopic + "の分析"; // 例：消費税の分析

        float nodeSpacing = 50f;
        // 親ノードの位置から新しいノードの位置を計算
        float newNodeX = parentNode.x + (nodeRectWidth / 2) + (analysisNodeWidth / 2) + nodeSpacing;;
        float newNodeY = parentNode.y;

        // 新しいMindMapノードを生成
        MindMap analysisNode = new MindMap(
                newNodeId,
                "",
                newNodeX,
                newNodeY,
                parentNode,
                null // 分析結果なので選択肢はなし
        );
        analysisNode.isAnalysisNode = true;

        float padding = 25f; // ノード内の余白
        float layoutWidth = analysisNodeWidth - (padding * 2); // テキスト描画領域の幅

        // StaticLayoutを作成して、自動改行されたテキスト情報を生成
        analysisNode.textLayout = new StaticLayout(
                analysisResult,         // 表示するテキスト
                textPaint,              // 使用するPaint
                (int)layoutWidth,       // この幅を超えたら改行
                Layout.Alignment.ALIGN_NORMAL, // テキストを左揃えに
                1.2f,                   // 行間の倍率 (少し広め)
                0.0f,                   // 行間の追加スペース
                false
        );

        // 計算されたテキストの高さに基づいてノード全体の高さを設定
//        analysisNode.dynamicHeight = analysisNode.textLayout.getHeight() + (padding * 2);

        allNodes.add(analysisNode);
        nodeMap.put(newNodeId, analysisNode);
        parentNode.addChild(analysisNode);
        parentNode.isExpanded = true;

        invalidate();
        calculateContentBounds();
        Log.d(DEBUG_TAG, "新しい分析ノードが追加されました: " + parentTopic);
    }

    // AIから受け取った候補者情報を基にノードを作成
    public void addCandidateNode(String parentNodeId, String candidateInfo) {
        MindMap parentNode = nodeMap.get(parentNodeId);
        if (parentNode == null) {
            Log.e(DEBUG_TAG, "Parent node not found for candidate: " + parentNodeId);
            return;
        }

        // ★ 1. AIの返答から、コロン「:」が含まれる行（＝候補者情報）だけを抽出
        List<String> validCandidates = new ArrayList<>();
        for (String line : candidateInfo.split("\n")) {
            if (line.contains(":")) {
                validCandidates.add(line.trim());
            }
        }

        float verticalSpacing = 120f; // 候補者ノード間の垂直方向のスペース

        // ★ 抽出した有効な候補者リストを使ってノードを作成
        for (int i = 0; i < validCandidates.size(); i++) {
            String candidateLine = validCandidates.get(i);
            if (candidateLine.isEmpty()) continue;

            String newNodeId = UUID.randomUUID().toString();

            float newNodeX = parentNode.x + nodeRectWidth + (candidateNodeWidth / 2) +50f;
            float newNodeY = parentNode.y + (i * verticalSpacing) - ((validCandidates.size() - 1) * verticalSpacing / 2);

            MindMap candidateNode = new MindMap(newNodeId, "", newNodeX, newNodeY, parentNode);
            candidateNode.isCandidateNode = true;

            // テキストを自動改行させるための設定
            float padding = 20f;
            candidateNode.textLayout = new StaticLayout(
                    candidateLine, textPaint, (int) (candidateNodeWidth - padding * 2),
                    Layout.Alignment.ALIGN_NORMAL, 1.2f, 0.0f, false
            );

            allNodes.add(candidateNode);
            nodeMap.put(newNodeId, candidateNode);
            parentNode.addChild(candidateNode);
        }

        parentNode.isExpanded = true;
        invalidate();
        calculateContentBounds();
    }
    private MindMap findNodeByLabel(String label) {
        for (MindMap node : allNodes) {
            if (node.label != null && node.label.equals(label)) {
                return node;
            }
        }
        return null;
    }
    public void clearCriteriaAndCandidateNodes() {
        List<MindMap> nodesToRemove = new ArrayList<>();
        // 削除対象のノードを収集
        for (MindMap node : allNodes) {
            if (node.isSearchCriteriaNode || node.isCandidateNode) {
                nodesToRemove.add(node);
            }
        }

        if (nodesToRemove.isEmpty()) {
            return;
        }

        // メインのリストとマップから削除
        for (MindMap node : nodesToRemove) {
            allNodes.remove(node);
            nodeMap.remove(node.id);
            // 親の子リストからも削除
            if (node.parent != null) {
                node.parent.children.remove(node);
            }
        }
        invalidate();
    }

    // ▼▼▼ このメソッドもまるごと追加（今回の主役です）▼▼▼
    public String addSearchCriteriaNode(String parentNodeId, String criteriaText) {
        // 1. 既存の検索結果をクリア
        clearCriteriaAndCandidateNodes();

        // 2. 親ノード（衆議院 or 参議院）を取得
        MindMap parentNode = nodeMap.get(parentNodeId);
        if (parentNode == null) {
            Log.e(DEBUG_TAG, "Parent node for criteria not found with ID: " + parentNodeId);
            return null;
        }

        // 3. 新しい子ノードの位置を計算
        float newNodeWidth = 400f;
        float nodeSpacing = 50f;
        float newNodeX = parentNode.x + (nodeRectWidth / 2) + (newNodeWidth / 2) + nodeSpacing;
        float newNodeY = parentNode.y;

        // 4. 検索条件ノードを生成 (親を指定)
        String newNodeId = UUID.randomUUID().toString();
        MindMap criteriaNode = new MindMap(newNodeId, "", newNodeX, newNodeY, parentNode);
        criteriaNode.isSearchCriteriaNode = true; // ★ MindMap.javaで追加したフラグを立てる

        // テキストレイアウト設定
        float padding = 20f;
        criteriaNode.textLayout = new StaticLayout(
                criteriaText, textPaint, (int) (newNodeWidth - padding * 2),
                Layout.Alignment.ALIGN_NORMAL, 1.2f, 0.0f, false
        );

        // 5. マップと親の子リストに追加
        allNodes.add(criteriaNode);
        nodeMap.put(newNodeId, criteriaNode);
        parentNode.addChild(criteriaNode);
        parentNode.isExpanded = true;

        invalidate();
        return newNodeId; // ★★★ 作成したノードのIDを返す
    }

    public void showKeywordNode(String keyword) {
        // 1. 既存のキーワードノードがあれば削除する
        List<MindMap> nodesToRemove = new ArrayList<>();
        for (MindMap node : allNodes) {
            if (node.isAnalysisKeywordNode) {
                nodesToRemove.add(node);
            }
        }
        for (MindMap nodeToRemove : nodesToRemove) {
            allNodes.remove(nodeToRemove);
            nodeMap.remove(nodeToRemove.id);
            if (nodeToRemove.parent != null) {
                nodeToRemove.parent.children.remove(nodeToRemove);
            }
        }
        // 2. 親となる「社会課題を検索」ノードを見つける
        MindMap parentNode = nodeMap.get("search_node");
        if (parentNode == null) {
            Log.e(DEBUG_TAG, "Parent node 'search_node' not found.");
            return;
        }
        parentNode.isExpanded = true;

        // 3. 新しいキーワードノードを作成して配置する
        String keywordNodeId = UUID.randomUUID().toString();
        float nodeSpacing = 50f;
        float keywordNodeWidth = 250f; // キーワードノードの幅

        // 「社会課題を検索」ノードの右隣に配置
        float keywordNodeX = parentNode.x + (searchNodeWidth / 2) + (keywordNodeWidth / 2) + nodeSpacing;
        float keywordNodeY = parentNode.y; // Y座標は同じ

        MindMap keywordNode = new MindMap(
                keywordNodeId,
                keyword,          // ラベルにキーワードを設定
                keywordNodeX,
                keywordNodeY,
                parentNode        // 親を「社会課題を検索」ノードに設定
        );
        keywordNode.isAnalysisKeywordNode = true; // フラグを立てる
        keywordNode.isLineToParentHidden = false;  // 親ノードとの線は非表示にする

        allNodes.add(keywordNode);
        nodeMap.put(keywordNodeId, keywordNode);
        parentNode.addChild(keywordNode);

        invalidate(); // 画面を再描画
        calculateContentBounds(); // コンテンツ境界を再計算
    }

    private static final String DEBUG_TAG = "MindMapViewDebug";

    public MindMapView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    //変数の初期化
    private void init(Context context) {
        allNodes = new ArrayList<>();
        nodeMap = new HashMap<>();

        nodePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        nodePaint.setColor(Color.BLUE);
        nodePaint.setStyle(Paint.Style.FILL);

        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(Color.GRAY);
        linePaint.setStrokeWidth(3f);

        textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(30f);
        textPaint.setTextAlign(Paint.Align.CENTER);

        gridPaint = new Paint();
        gridPaint.setColor(context.getColor(R.color.gridLineColor)); // ← 色の名前を参照
        gridPaint.setStrokeWidth(1f);

        scaleGestureDetector = new ScaleGestureDetector(context, new ScaleListener());
//        createInitialNodes();
        calculateContentBounds();
    }

    //ビューサイズの変更
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // ビューのサイズがまだ有効でない場合は何もしない
        if (w == 0 || h == 0) {
            return;
        }
        // 最初にノードが作成されていなければ作成
        if (allNodes.isEmpty()) {
            createInitialNodes();
        }
        // 初回のレイアウト時のみ行う、ノードの位置計算
        if (!initialLayoutDone) {
            MindMap rootNode = nodeMap.get("root");
            if (rootNode != null) {

                //ルートノード(親)の位置計算
                float oldRootY = rootNode.y;
                float targetRootY = h / 2f;
                rootNode.y = targetRootY;
                float dy = targetRootY - oldRootY;

                // 他のすべてのノードのY座標も同じ量だけ移動させる
                for (MindMap node : allNodes) {
                    if (node != rootNode) { // ルートノード自身は既に更新済み
                        node.y += dy;
                    }
                }
            }
            initialLayoutDone = true; // 初期レイアウトが完了しましたフラグ
        }

        //中心点計算
        if (pivotX_world == 0 && pivotY_world == 0) {
            pivotX_world = (w / 2f - scrollX) / scaleFactor;
            pivotY_world = (h / 2f - scrollY) / scaleFactor;
        }

        limitScroll();
    }

    //ノード作成
    private void createInitialNodes() {
        allNodes.clear();
        nodeMap.clear();
        float initialX = 150f;
        float initialY = 400f;

        MindMap root = new MindMap("root", "選挙", initialX, initialY, null);
        allNodes.add(root);
        nodeMap.put(root.id, root);
        root.isExpanded = true;

        // child1: 比例代表制
        MindMap child1 = new MindMap("child1", "比例代表制", initialX + 300f, initialY + 400f, root);
        root.addChild(child1);
        allNodes.add(child1);
        nodeMap.put(child1.id, child1);
        child1.isExpanded = false; // ★ 最初は閉じている状態（重要）

        // child2: 小選挙区制
        MindMap child2 = new MindMap("child2", "小選挙区制", initialX + 300f, initialY - 400f, root);
        root.addChild(child2);
        allNodes.add(child2);
        nodeMap.put(child2.id, child2);
        child2.isExpanded = false;

        // Partyノードは child1 (比例代表制) の子
        float partyOffsetX = 300f;
        float partyNodeYIncrement = 200f;

        MindMap party1 = new MindMap("party_jiminto", "自民党", child1.x + partyOffsetX, child1.y, child1);
        child1.addChild(party1);
        allNodes.add(party1);
        nodeMap.put(party1.id, party1);

        // ★★★ ここからが修正・確認のポイントです ★★★

        // 1. 「検索ノード」を「比例代表制」の子として作成します
        MindMap searchNode = new MindMap(
                "search_node",
                "社会課題を検索",   // ★ ラベルの末尾に「...」を追加（重要）
                child1.x + 300f,      // ★ X座標を調整
                child1.y-200f,             // ★ Y座標を「比例代表制」と同じ高さに変更
                child1                // ★ 親を child1 (比例代表制) に変更
        );
        searchNode.isSearchNode = true;
        searchNode.isLineToParentHidden = true; // 親との接続線を非表示に設定
        child1.addChild(searchNode);            // child1 の子として追加
        allNodes.add(searchNode);
        nodeMap.put(searchNode.id, searchNode);

        // 2. 古い「質問」ノードのコードは、このメソッドから完全に削除されていることを確認してください。

        // ★★★ ポイントはここまでです ★★★

        MindMap party2 = new MindMap("party_komeito", "公明党", child1.x + partyOffsetX, child1.y + partyNodeYIncrement, child1);
        child1.addChild(party2); allNodes.add(party2); nodeMap.put(party2.id, party2);
        MindMap party3 = new MindMap("party_rikken", "立憲民主党", child1.x + partyOffsetX, child1.y  + partyNodeYIncrement * 2, child1);
        child1.addChild(party3); allNodes.add(party3); nodeMap.put(party3.id, party3);
        MindMap party4 = new MindMap("party_ishin", "日本維新の会", child1.x + partyOffsetX, child1.y + partyNodeYIncrement * 3, child1);
        child1.addChild(party4); allNodes.add(party4); nodeMap.put(party4.id, party4);
        MindMap party5 = new MindMap("party_kyosan", "共産党", child1.x + partyOffsetX, child1.y  + partyNodeYIncrement * 4, child1);
        child1.addChild(party5); allNodes.add(party5); nodeMap.put(party5.id, party5);
        MindMap party6 = new MindMap("party_kokumin", "国民民主党", child1.x + partyOffsetX, child1.y + partyNodeYIncrement * 5, child1);
        child1.addChild(party6); allNodes.add(party6); nodeMap.put(party6.id, party6);
        MindMap party7 = new MindMap("party_reiwa", "れいわ新選組", child1.x + partyOffsetX, child1.y  + partyNodeYIncrement * 6, child1);
        child1.addChild(party7); allNodes.add(party7); nodeMap.put(party7.id, party7);
        MindMap party8 = new MindMap("party_sanseito", "参政党", child1.x + partyOffsetX, child1.y  + partyNodeYIncrement * 7, child1);
        child1.addChild(party8); allNodes.add(party8); nodeMap.put(party8.id, party8);
        MindMap party9 = new MindMap("party_shamin", "社民党", child1.x + partyOffsetX, child1.y  + partyNodeYIncrement * 8, child1);
        child1.addChild(party9); allNodes.add(party9); nodeMap.put(party9.id, party9);
        MindMap party10 = new MindMap("party_hoshu", "日本保守党", child1.x + partyOffsetX, child1.y  + partyNodeYIncrement * 9, child1);
        child1.addChild(party10); allNodes.add(party10); nodeMap.put(party10.id, party10);
        MindMap party11 = new MindMap("party_mintsuku", "みんつく党", child1.x + partyOffsetX, child1.y  + partyNodeYIncrement * 10, child1);
        child1.addChild(party11); allNodes.add(party11); nodeMap.put(party11.id, party11);
        MindMap party12 = new MindMap("party_shugiin", "衆議院選挙", child2.x + partyOffsetX, child2.y-200f, child2);
        party12.isElectionInputNode = true;
        child2.addChild(party12);
        allNodes.add(party12);
        nodeMap.put(party12.id, party12);
        MindMap party13 = new MindMap("party_sangiin", "参議院選挙", child2.x + partyOffsetX, child2.y+200f, child2);
        party13.isElectionInputNode = true;
        child2.addChild(party13);
        allNodes.add(party13);
        nodeMap.put(party13.id, party13);
    }

    //マップを囲む境界を計算
    private void calculateContentBounds() {
        if (allNodes.isEmpty()) {
            contentRectWorld.setEmpty();
            return;
        }
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = Float.MIN_VALUE;
        float maxY = Float.MIN_VALUE;

        for (MindMap node : allNodes) {
            float halfWidth = (node.isAnalysisNode) ? analysisNodeWidth / 2 : nodeRectWidth / 2;
            float halfHeight = (node.isAnalysisNode) ? analysisNodeHeight / 2 : nodeRectHeight / 2;
            minX = Math.min(minX, node.x - halfWidth);
            minY = Math.min(minY, node.y - halfHeight);
            maxX = Math.max(maxX, node.x + halfWidth);
            maxY = Math.max(maxY, node.y + halfHeight);
//            minX = Math.min(minX, node.x - nodeRectWidth / 2);
//            minY = Math.min(minY, node.y - nodeRectHeight / 2);
//            maxX = Math.max(maxX, node.x + nodeRectWidth / 2);
//            maxY = Math.max(maxY, node.y + nodeRectHeight / 2);
        }
        contentRectWorld.set(minX, minY, maxX, maxY);
    }

    //拡大縮小のクラス
    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        //初期位置の計算
        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            float viewportFocusX = detector.getFocusX();
            float viewportFocusY = detector.getFocusY();
            pivotX_world = (viewportFocusX - scrollX) / scaleFactor;
            pivotY_world = (viewportFocusY - scrollY) / scaleFactor;
            return true;
        }

        //拡大縮小
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float prevScaleFactor = scaleFactor;
            scaleFactor *= detector.getScaleFactor();
            scaleFactor = Math.max(minScaleFactor, Math.min(scaleFactor, maxScaleFactor));

            if (scaleFactor != prevScaleFactor) {
                float viewportFocusX = detector.getFocusX();
                float viewportFocusY = detector.getFocusY();
                scrollX = viewportFocusX - pivotX_world * scaleFactor;
                scrollY = viewportFocusY - pivotY_world * scaleFactor;
                limitScroll();
            }
            invalidate();
            return true;
        }
    }

    //スクロールの制限
    private void limitScroll() {
        if (contentRectWorld.isEmpty() || getWidth() == 0 || getHeight() == 0) {
            return;
        }

        float viewWidth = getWidth();
        float viewHeight = getHeight();

        //  スクロール範囲を広げるための追加の空間（ビューの幅/高さの1倍分を左右/上下に）
        float extraScrollSpaceX = viewWidth * 1.0f;
        float extraScrollSpaceY = viewHeight * 1.0f;

        //  scrollX: ワールド(0,0)がビューポートの(scrollX, scrollY)に来る
        //  ビューの左端(0)にコンテンツの右端が来る時のscrollX: viewWidth - contentRectWorld.right * scaleFactor
        //  ビューの右端(viewWidth)にコンテンツの左端が来る時のscrollX: -contentRectWorld.left * scaleFactor

        //  コンテンツの右端が「ビューの左端 - 仮想余白/2」より左に行かないようにするためのscrollXの最小値
        float minPossibleScrollX = viewWidth - contentRectWorld.right * scaleFactor - extraScrollSpaceX / 2f;
        //  コンテンツの左端が「ビューの右端 + 仮想余白/2」より右に行かないようにするためのscrollXの最大値
        float maxPossibleScrollX = -contentRectWorld.left * scaleFactor + extraScrollSpaceX / 2f;

        float minPossibleScrollY = viewHeight - contentRectWorld.bottom * scaleFactor - extraScrollSpaceY / 2f;
        float maxPossibleScrollY = -contentRectWorld.top * scaleFactor + extraScrollSpaceY / 2f;

        float scaledContentWidth = contentRectWorld.width() * scaleFactor;
        float scaledContentHeight = contentRectWorld.height() * scaleFactor;


        if (scaledContentWidth + extraScrollSpaceX < viewWidth) {

            float targetScrollX = (viewWidth - (scaledContentWidth)) / 2f - contentRectWorld.left * scaleFactor;
            minPossibleScrollX = targetScrollX - extraScrollSpaceX / 2f; // あまり意味がないかもしれないが、範囲として
            maxPossibleScrollX = targetScrollX + extraScrollSpaceX / 2f;
        }

        if (scaledContentHeight + extraScrollSpaceY < viewHeight) {
            float targetScrollY = (viewHeight - (scaledContentHeight)) / 2f - contentRectWorld.top * scaleFactor;
            minPossibleScrollY = targetScrollY - extraScrollSpaceY / 2f;
            maxPossibleScrollY = targetScrollY + extraScrollSpaceY / 2f;
        }

        float clampedScrollX = Math.max(minPossibleScrollX, Math.min(maxPossibleScrollX, scrollX));
        float clampedScrollY = Math.max(minPossibleScrollY, Math.min(maxPossibleScrollY, scrollY));

        if (scrollX != clampedScrollX || scrollY != clampedScrollY) {
            Log.d(DEBUG_TAG, "LimitScroll: Before (" + scrollX + "," + scrollY + ") After (" + clampedScrollX + "," + clampedScrollY + ")");

            scrollX = clampedScrollX;
            scrollY = clampedScrollY;
        }
    }


    //再描画関数
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
//      背景の設定
//        canvas.drawColor(Color.rgb(240, 240, 240)); // 背景色
        canvas.save();
        canvas.translate(scrollX, scrollY);
        canvas.scale(scaleFactor, scaleFactor);
        drawGrid(canvas);

        // 新しいPaintオブジェクトをここで初期化 (onDraw内で毎回作らないようにクラスメンバにするのがベター)
        Paint optionTextPaint = new Paint(textPaint); // textPaintをコピーして少し変更
        optionTextPaint.setTextSize(textPaint.getTextSize() * 0.8f); // 少し小さく
        optionTextPaint.setTextAlign(Paint.Align.LEFT); // 左揃えで描画

        Paint radioPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        radioPaint.setStyle(Paint.Style.STROKE); // 枠線
        radioPaint.setStrokeWidth(3);
        Paint radioSelectedPaint = new Paint(radioPaint);
        radioSelectedPaint.setStyle(Paint.Style.FILL_AND_STROKE); // 塗りつぶし

        float optionLineHeight = textPaint.getTextSize() * 1.5f; // 各選択肢の行の高さ (変更なし)
        float radioRadius = textPaint.getTextSize() * 0.4f;
        float radioTextPadding = radioRadius * 3;
        float optionsBlockPaddingFromNodeTop = textPaint.getTextSize() * 0.5f;


        List<MindMap> visibleNodes = getVisibleNodes();
        for (MindMap node : visibleNodes) {
//            if (node.parent != null && !node.isLineToParentHidden) {
//                float parentWidth = ("root".equals(node.parent.id) || "child1".equals(node.parent.id) || "child2".equals(node.parent.id)) ? nodeRectWidth : nodeRectWidth; // More specific logic might be needed
//                //float childWidth = (node.isAnalysisNode) ? analysisNodeWidth : nodeRectWidth;
//                // 変更後 (候補者ノードの幅を考慮するように修正)
//                float childWidth;
//                if (node.isAnalysisNode) {
//                    childWidth = analysisNodeWidth;
//                } else if (node.isCandidateNode) {
//                    childWidth = candidateNodeWidth; // 候補者ノードの幅を使用
//                } else {
//                    childWidth = nodeRectWidth;
//                }
//                float startX = node.parent.x + parentWidth / 2;
//                float startY = node.parent.y;
//                float endX = node.x - childWidth / 2;
//                float endY = node.y;
//                canvas.drawLine(startX, startY, endX, endY, linePaint);
//            }
            if (node.parent != null && !node.isLineToParentHidden) {
                float startX = node.parent.x;
                float startY = node.parent.y;
                float endX = node.x;
                float endY = node.y;

                // 親ノードが検索条件ノード（isSearchCriteriaNode）の場合
                // または、親が選挙入力ノード（isElectionInputNode）で、子が検索条件ノードの場合
                if ((node.parent.isSearchCriteriaNode && node.isCandidateNode) ||
                        (node.parent.isElectionInputNode && node.isSearchCriteriaNode)) {

                    // 親ノードの右端中央を始点に
                    float parentWidth;
                    if (node.parent.isSearchCriteriaNode) {
                        parentWidth = 400f; // 検索条件ノードの幅
                    } else if (node.parent.isElectionInputNode) {
                        parentWidth = nodeRectWidth; // 衆議院選挙/参議院選挙ノードの幅
                    } else {
                        parentWidth = nodeRectWidth; // デフォルト
                    }
                    startX = node.parent.x + parentWidth / 2;
                    startY = node.parent.y;

                    // 子ノードの左端中央を終点に
                    float childWidth;
                    if (node.isCandidateNode) {
                        childWidth = candidateNodeWidth; // 候補者ノードの幅
                    } else if (node.isSearchCriteriaNode) {
                        childWidth = 400f; // 検索条件ノードの幅
                    } else {
                        childWidth = nodeRectWidth; // デフォルト
                    }
                    endX = node.x - childWidth / 2;
                    endY = node.y;
                }
                else if ("child1".equals(node.parent.id)) {
                    // 親ノード（比例代表制）の右端中央を始点に設定
                    startX = node.parent.x + nodeRectWidth / 2;
                    startY = node.parent.y;
                    // 子ノード（各政党）の左端中央を終点に設定
                    endX = node.x - nodeRectWidth / 2;
                    endY = node.y;
                }
                else if (node.isAnalysisNode) {
                    // 分析ノードはこれまで通り、親の中心から子の中心へ (または調整が必要ならここに追加)
                    startX = node.parent.x;
                    startY = node.parent.y;
                    endX = node.x;
                    endY = node.y;
                }
                // その他のノードはデフォルトで中心から中心へ線を引く

                canvas.drawLine(startX, startY, endX, endY, linePaint);
            }

        }

        // --- 次にノード本体を描画 ---
        for (MindMap node : visibleNodes) {
            if (node.isSearchCriteriaNode) {
                float currentRectWidth = 400f;
                float currentRectHeight = node.textLayout.getHeight() + 40f;
                float cornerRadius = 20f;
                nodePaint.setColor(Color.rgb(232, 245, 233)); // 衆議院選挙、参議院選挙の情報ノード
                canvas.drawRoundRect(node.x - currentRectWidth / 2, node.y - currentRectHeight / 2, node.x + currentRectWidth / 2, node.y + currentRectHeight / 2, cornerRadius, cornerRadius, nodePaint);

                canvas.save();
                canvas.translate(node.x - currentRectWidth / 2 + 20f, node.y - currentRectHeight / 2 + 20f);
                textPaint.setTextAlign(Paint.Align.LEFT);
                textPaint.setColor(Color.BLACK);
                node.textLayout.draw(canvas);
                textPaint.setTextAlign(Paint.Align.CENTER); // 他のノードのために戻す
                canvas.restore();

            } else if (node.isAnalysisNode || node.isCandidateNode) {
                if (node.isCandidateNode) {
                    drawCandidateNode(canvas, node);
                } else {
                    drawAnalysisNode(canvas, node);
                }
                // ★★★上の if-else で描画が完了しているため、重複していた行は削除しました★★★
            } else {
                // --- 通常ノードと選択肢ノードの描画 ---
                // 選択肢の描画
                if (node.isSelectableNode() && node.areOptionsVisible()) {
                    drawOptions(canvas, node, optionTextPaint, radioPaint, radioSelectedPaint);
                }

                // ノード本体の描画
                //float currentWidth = node.isSearchNode ? searchNodeWidth : nodeRectWidth;
                float currentWidth;
                if (node.isSearchNode) {
                    currentWidth = searchNodeWidth;
                } else if (node.isAnalysisKeywordNode) {
                    currentWidth = 250f; // ★ キーワードノードの幅を指定
                } else {
                    currentWidth = nodeRectWidth;
                }

                // ノード本体の描画
                float cornerRadius = 20f;
                if (node.isSearchNode) {
                    nodePaint.setColor(Color.rgb(232, 245, 233)); // 社会課題を検索ノードの色
                } else if("root".equals(node.id)) {
                    nodePaint.setColor(Color.rgb(204, 187, 221));//選挙ノード
                } else if (node.isSelectableNode()) {
                    nodePaint.setColor(Color.rgb(204, 187, 221)); // 選択肢ノードの色
                }else if (node.isAnalysisKeywordNode) { // ★ キーワードノードの色を設定
                    nodePaint.setColor(Color.rgb(232, 245, 233)); // 消費税など
                }else {
                    nodePaint.setColor(Color.rgb(204, 187, 221));//政党ノード、比例代表制
                }

                // ★ 2. 決定した幅(currentWidth)を使って描画します
                canvas.drawRoundRect(
                        node.x - currentWidth / 2, // 変更点
                        node.y - nodeRectHeight / 2,
                        node.x + currentWidth / 2, // 変更点
                        node.y + nodeRectHeight / 2,
                        cornerRadius, cornerRadius, nodePaint);
                // テキストの描画
                textPaint.setColor(Color.BLACK); // 通常ノードのテキストは白
                canvas.drawText(node.label, node.x, node.y + (textPaint.getTextSize() / 3), textPaint);
            }
        }

        canvas.restore();
    }

    private void drawAnalysisNode(Canvas canvas, MindMap node) {
        float currentRectHeight = analysisNodeHeight;
        float currentRectWidth = analysisNodeWidth;
        float cornerRadius = 20f;
        float padding = 25f;

        // ノードの背景を描画
        nodePaint.setColor(Color.rgb(225, 245, 254)); // 水色
        canvas.drawRoundRect(
                node.x - currentRectWidth / 2,
                node.y - currentRectHeight / 2,
                node.x + currentRectWidth / 2,
                node.y + currentRectHeight / 2,
                cornerRadius, cornerRadius, nodePaint);

        // テキストを描画
        canvas.save();
        // 描画基点をノードの左上に移動
        canvas.translate(
                node.x - currentRectWidth / 2 + padding,
                node.y - currentRectHeight / 2 + padding
        );
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setColor(Color.BLACK); // テキストの色を黒に設定
        node.textLayout.draw(canvas);
        textPaint.setTextAlign(Paint.Align.CENTER);
        canvas.restore();
    }

    private void drawCandidateNode(Canvas canvas, MindMap node) {
        // textLayoutから高さを動的に取得
        float currentRectHeight = node.textLayout.getHeight() + 40f;
        float cornerRadius = 15f;

        // ノードの背景を描画
        nodePaint.setColor(Color.rgb(225, 245, 254)); // 水色
        canvas.drawRoundRect(
                node.x - candidateNodeWidth / 2,
                node.y - currentRectHeight / 2,
                node.x + candidateNodeWidth / 2,
                node.y + currentRectHeight / 2,
                cornerRadius, cornerRadius, nodePaint);

        // テキストを描画
        canvas.save();
        canvas.translate(
                node.x - candidateNodeWidth / 2 + 20f,
                node.y - currentRectHeight / 2 + 20f
        );
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setColor(Color.BLACK);
        node.textLayout.draw(canvas);
        textPaint.setTextAlign(Paint.Align.CENTER);
        canvas.restore();
    }

    private void drawOptions(Canvas canvas, MindMap node, Paint optionTextPaint, Paint radioPaint, Paint radioSelectedPaint) {
        List<String> options = node.getOptions();
        if (options == null || options.isEmpty()) return;

        float optionLineHeight = textPaint.getTextSize() * 1.5f;
        float radioRadius = textPaint.getTextSize() * 0.4f;
        float radioTextPadding = radioRadius * 3;
        float optionsBlockPaddingFromNodeTop = textPaint.getTextSize() * 0.5f;
        float totalOptionsHeight = options.size() * optionLineHeight;
        float firstOptionBaselineY = (node.y - nodeRectHeight / 2)
                - optionsBlockPaddingFromNodeTop
                - totalOptionsHeight
                + optionLineHeight * 0.8f;
        float startX = node.x - nodeRectWidth / 2 + radioTextPadding / 2;

        for (int i = 0; i < options.size(); i++) {
            float currentOptionBaselineY = firstOptionBaselineY + (i * optionLineHeight);
            float radioY = currentOptionBaselineY - optionTextPaint.getTextSize() / 3;
            float radioX = startX + radioRadius;

            if (i == node.getSelectedOptionIndex()) {
                radioSelectedPaint.setColor(Color.BLUE);
                canvas.drawCircle(radioX, radioY, radioRadius, radioSelectedPaint);
            } else {
                radioPaint.setColor(Color.DKGRAY);
                canvas.drawCircle(radioX, radioY, radioRadius, radioPaint);
            }
            optionTextPaint.setColor(Color.BLACK);
            canvas.drawText(options.get(i), startX + radioTextPadding, currentOptionBaselineY, optionTextPaint);
        }
    }


    private void drawGrid(Canvas canvas) {
        if (getWidth() == 0 || getHeight() == 0 || scaleFactor == 0) return;
        float gridSize = 100f;
        float viewLeftWorld = (-scrollX) / scaleFactor;
        float viewTopWorld = (-scrollY) / scaleFactor;
        float viewRightWorld = (getWidth() - scrollX) / scaleFactor;
        float viewBottomWorld = (getHeight() - scrollY) / scaleFactor;

        float startX = (float) (Math.floor(viewLeftWorld / gridSize) * gridSize);
        for (float x = startX; x < viewRightWorld; x += gridSize) {
            canvas.drawLine(x, viewTopWorld, x, viewBottomWorld, gridPaint);
        }
        float startY = (float) (Math.floor(viewTopWorld / gridSize) * gridSize);
        for (float y = startY; y < viewBottomWorld; y += gridSize) {
            canvas.drawLine(viewLeftWorld, y, viewRightWorld, y, gridPaint);
        }
    }

    //選択肢を含むノードの設定
    private boolean checkOptionTap(MindMap node, float touchX_world, float touchY_world) {
        if (!node.isSelectableNode() || node.getOptions() == null || node.getOptions().isEmpty()) {
            return false;
        }

        // --- ▼▼▼ onDraw と同じ定数を使用 ▼▼▼ ---
        float optionLineHeight = textPaint.getTextSize() * 1.5f;
        float optionsBlockPaddingFromNodeTop = textPaint.getTextSize() * 0.5f; // onDraw と同じ値
        List<String> options = node.getOptions();
        float totalOptionsHeight = options.size() * optionLineHeight;

        // 最初の選択肢のテキストベースラインのY座標 (onDraw と同じ計算)
        float firstOptionBaselineY_world = (node.y - nodeRectHeight / 2)
                - optionsBlockPaddingFromNodeTop
                - totalOptionsHeight
                + optionLineHeight * 0.8f;

        float nodeContentLeft_world = node.x - nodeRectWidth / 2;
        float nodeContentRight_world = node.x + nodeRectWidth / 2;

        for (int i = 0; i < options.size(); i++) {
            float currentOptionBaselineY_world = firstOptionBaselineY_world + (i * optionLineHeight);

            // 各選択肢の当たり判定領域 (テキストの高さ全体を考慮)
            float optionTop_world = currentOptionBaselineY_world - textPaint.getTextSize(); // テキスト上端あたり
            float optionBottom_world = currentOptionBaselineY_world + (textPaint.getTextSize() * 0.5f); // テキスト下端あたり

            if (touchX_world >= nodeContentLeft_world && touchX_world <= nodeContentRight_world &&
                    touchY_world >= optionTop_world && touchY_world <= optionBottom_world) {

                if (node.getSelectedOptionIndex() == i) {
                    // 同じ選択肢を再度タップした場合、選択解除する
                    node.setSelectedOptionIndex(-1);
                    // リスナーを呼び出し、選択解除を通知する
                    if (selectionListener != null) {
                        selectionListener.onOptionSelected(null); // nullを渡して解除を通知
                    }
                } else {
                    node.setSelectedOptionIndex(i);
                    // 選択されたトピックをリスナーに通知する
                    if (selectionListener != null) {
                        selectionListener.onOptionSelected(node.getSelectedOptionText());
                    }
                }
                return true;
            }
        }
        return false;
    }

    //タッチ操作
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean consumed = false;
        boolean scaleEventHandled = scaleGestureDetector.onTouchEvent(event);

        float currentTouchX = event.getX();
        float currentTouchY = event.getY();

        float touchX_world = (currentTouchX - scrollX) / scaleFactor; // (3) ワールドX座標に変換
        float touchY_world = (currentTouchY - scrollY) / scaleFactor;

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastTouchX_drag = currentTouchX;
                lastTouchY_drag = currentTouchY;
                isDragging = false;
                // 選択肢が表示されていれば、選択肢のタップ判定を優先
                for (MindMap node : allNodes) { // ideally, iterate over visible nodes or nodes near touch
                    if (node.isSelectableNode() && node.areOptionsVisible()) {
                        if (checkOptionTap(node, touchX_world, touchY_world)) {
                            invalidate();
                            consumed = true; // イベント消費
                            break;  // 選択肢がタップされたら他の処理は不要
                        }
                    }
                }

                if (consumed) {
                    return true; // 選択肢タップでイベント消費済みなら終了
                }
                //  ACTION_DOWN でイベントを消費することで、親Viewのスクロールなどを阻害しないようにする
                //  また、後続のMOVEやUPイベントを確実に受け取るため
                return true;

            case MotionEvent.ACTION_MOVE:
                if (scaleGestureDetector.isInProgress()) {
                    isDragging = false; //  スケール中はドラッグとしない
                } else {
                    //  微小な動きはドラッグとしないための閾値 (slop)
                    if (Math.abs(currentTouchX - lastTouchX_drag) > 10 || Math.abs(currentTouchY - lastTouchY_drag) > 10) {
                        isDragging = true;
                    }
                    if (isDragging) {
                        float dx = currentTouchX - lastTouchX_drag;
                        float dy = currentTouchY - lastTouchY_drag;
                        scrollX += dx;
                        scrollY += dy;
                        limitScroll(); //  ドラッグ後にスクロール範囲を制限
                        lastTouchX_drag = currentTouchX;
                        lastTouchY_drag = currentTouchY;
                        invalidate();
                        consumed = true;
                    }
                }
                break;

            case MotionEvent.ACTION_UP:
                if (!isDragging && !scaleGestureDetector.isInProgress()) { // ドラッグでもスケールでもなければタップ
                    //  ビューポート座標をワールド座標に変換
                    float worldX = (currentTouchX - scrollX) / scaleFactor;
                    float worldY = (currentTouchY - scrollY) / scaleFactor;

                    MindMap tappedNode = findTappedNode(worldX, worldY);
                    if (tappedNode != null) {

                        if (tappedNode.isSearchNode) {
                            // 1. 検索ノードがタップされた場合の処理
                            if (selectionListener != null) {
                                selectionListener.onOptionSelected(tappedNode.label);
                            }
                            consumed = true;

                        } else if (tappedNode.isElectionInputNode) {
                            if (!tappedNode.children.isEmpty()) {
                                // 子ノードがいればクリアする
                                clearCriteriaAndCandidateNodes();
                            } else {
                                // 子ノードがいなければダイアログ表示を要求
                                if (selectionListener != null) {
                                    selectionListener.onOptionSelected(tappedNode.label);
                                }
                            }
                            consumed = true;

                        } else if (tappedNode.isSelectableNode()) {
                            // 3. 選択肢を持つノードがタップされた場合
                            tappedNode.toggleOptionsVisibility();
                            consumed = true;

                        } else if (!tappedNode.children.isEmpty()) {
                            // 4. 子を持つ通常のノードがタップされた場合の処理
                            if ("child1".equals(tappedNode.id)) {
                                // 「比例代表制」がタップされた場合
                                MindMap child2 = nodeMap.get("child2"); // もう片方のノードを取得
                                if (child2 != null) {
                                    child2.isExpanded = false; // もう片方を閉じる
                                }
                                tappedNode.isExpanded = !tappedNode.isExpanded; // タップした方を開閉

                            } else if ("child2".equals(tappedNode.id)) {
                                // 「小選挙区制」がタップされた場合
                                MindMap child1 = nodeMap.get("child1"); // もう片方のノードを取得
                                if (child1 != null) {
                                    child1.isExpanded = false; // もう片方を閉じる
                                }
                                tappedNode.isExpanded = !tappedNode.isExpanded; // タップした方を開閉

                                // ▼▼▼ ここからが今回の追加修正点です ▼▼▼
                            } else if ("party_shugiin".equals(tappedNode.id)) {
                                // 「衆議院選挙」がタップされた場合
                                MindMap sangiinNode = nodeMap.get("party_sangiin"); // 参議院ノードを取得
                                if (sangiinNode != null) {
                                    sangiinNode.isExpanded = false; // 参議院ノードを閉じる
                                }
                                tappedNode.isExpanded = !tappedNode.isExpanded; // タップした方を開閉

                            } else if ("party_sangiin".equals(tappedNode.id)) {
                                // 「参議院選挙」がタップされた場合
                                MindMap shugiinNode = nodeMap.get("party_shugiin"); // 衆議院ノードを取得
                                if (shugiinNode != null) {
                                    shugiinNode.isExpanded = false; // 衆議院ノードを閉じる
                                }
                                tappedNode.isExpanded = !tappedNode.isExpanded; // タップした方を開閉
                                // ▲▲▲ ここまでが追加修正点です ▲▲▲

                            } else {
                                // それ以外のノードは、通常通り開閉するだけ
                                tappedNode.isExpanded = !tappedNode.isExpanded;
                            }
                            consumed = true;
                        }

                        if (consumed) {
                            invalidate(); // 状態が変わったので再描画
                        }
                    }
                }
                isDragging = false; // ACTION_UP後はドラッグ状態をリセット
                break;

            case MotionEvent.ACTION_CANCEL:
                isDragging = false; //  イベントキャンセル時もドラッグ状態をリセット
                break;
        }

        //  スケールイベントが処理されたか、このビューがドラッグやタップを処理した場合は true を返す
        if (scaleEventHandled) {
            return true;
        }
        //  ACTION_MOVE中のドラッグは既に上でtrueを返しているので、ここには来ないはずだが念のため
        if (isDragging && event.getActionMasked() == MotionEvent.ACTION_MOVE) {
            return true;
        }
        //  ACTION_DOWNも既にtrueを返している

        return super.onTouchEvent(event); //  上記以外の場合は親にイベントを渡す
    }


    private MindMap findTappedNode(float worldX, float worldY) {
        List<MindMap> nodesToSearch = getVisibleNodes(); //  表示されているノードのみを対象
        //  リストの逆から探索することで、手前に描画されているノードを優先
        for (int i = nodesToSearch.size() - 1; i >= 0; i--) {
            MindMap node = nodesToSearch.get(i);
            // findTappedNode メソッド内

            float currentRectWidth;
            if (node.isAnalysisNode) {
                currentRectWidth = analysisNodeWidth;
            } else if (node.isCandidateNode) { // ▼▼▼ このブロックを追加 ▼▼▼
                currentRectWidth = candidateNodeWidth;
            } else if (node.isSearchNode) {
                currentRectWidth = searchNodeWidth;
            } else {
                currentRectWidth = nodeRectWidth;
            }

            float currentRectHeight = (node.isAnalysisNode) ? analysisNodeHeight : nodeRectHeight;
            float padding = tapPadding / 2f; //  タップ判定領域の拡張

            float nodeLeft = node.x - currentRectWidth / 2 - padding;
            float nodeRight = node.x + currentRectWidth / 2 + padding;
            float nodeTop = node.y - currentRectHeight / 2 - padding;
            float nodeBottom = node.y + currentRectHeight / 2 + padding;

            if (worldX >= nodeLeft && worldX <= nodeRight &&
                    worldY >= nodeTop && worldY <= nodeBottom) {
                return node; //  ヒットしたノードを返す
            }
        }
        return null; //  ヒットしなかった場合
    }

    private List<MindMap> getVisibleNodes() {
        List<MindMap> visible = new ArrayList<>();
        MindMap root = nodeMap.get("root"); //  ID "root" でルートノードを取得
        if (root != null) {
            addVisibleNodesRecursive(root, visible);
        }
        for (MindMap node : allNodes) {
            if (node.getParent() == null && !node.id.equals("root") && !visible.contains(node)) {
                // isSelectableNode() のような、独立して表示すべきノードの目印となる条件を追加するとより良い
                // 例: if (node.getParent() == null && node.isSelectableNode() && !visible.contains(node))
                visible.add(node);
            }
        }
        return visible;
    }

    private void addVisibleNodesRecursive(MindMap node, List<MindMap> visibleList) {
        visibleList.add(node);
        if (node.isExpanded) { //  ノードが展開されている場合のみ子を探索
            for (MindMap child : node.children) {
                //  子ノードがnodeMapに存在することも確認（堅牢性のため）
                if (nodeMap.containsKey(child.id)) {
                    addVisibleNodesRecursive(child, visibleList);
                }
            }
        }
    }
}
