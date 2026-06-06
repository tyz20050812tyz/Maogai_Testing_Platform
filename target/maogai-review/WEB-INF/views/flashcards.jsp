<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ include file="header.jsp" %>

<div class="card">
    <h2 class="card-title">知识点速记卡片</h2>
    <p class="section-subtitle">按章节抽认核心概念，点击卡片查看答案。</p>
    <div class="filter-bar">
        <span>章节</span>
        <select id="flashcard-chapter" onchange="loadFlashcards()">
            <option value="">全部章节</option>
        </select>
        <span class="quiz-counter" id="flashcard-counter">0 / 0</span>
    </div>
    <div id="flashcard-container"></div>
    <div class="quiz-nav">
        <button class="btn btn-outline" onclick="prevFlashcard()">上一张</button>
        <button class="btn btn-primary" onclick="flipFlashcard()">翻面</button>
        <button class="btn btn-outline" onclick="nextFlashcard()">下一张</button>
    </div>
</div>

<script>
Quiz.init('${pageContext.request.contextPath}');

var flashcards = [];
var flashcardIndex = 0;
var flashcardFlipped = false;

Quiz.loadChapterOptions('flashcard-chapter', '', true, loadFlashcards);

function loadFlashcards() {
    var chapter = document.getElementById('flashcard-chapter').value;
    var url = Quiz.ctx + '/api/flashcards/list' + (chapter ? '?chapter=' + encodeURIComponent(chapter) : '');
    var container = document.getElementById('flashcard-container');
    Quiz.setLoading(container, '正在加载卡片...');
    fetch(url)
        .then(function(r) { return r.json(); })
        .then(function(data) {
            flashcards = Array.isArray(data) ? data : [];
            flashcardIndex = 0;
            flashcardFlipped = false;
            renderFlashcard();
        })
        .catch(function() {
            container.innerHTML = '<div class="empty-state">卡片加载失败，请稍后重试</div>';
        });
}

function renderFlashcard() {
    var container = document.getElementById('flashcard-container');
    var counter = document.getElementById('flashcard-counter');
    if (!flashcards.length) {
        counter.textContent = '0 / 0';
        container.innerHTML = '<div class="empty-state">当前章节还没有速记卡片</div>';
        return;
    }
    var card = flashcards[flashcardIndex];
    counter.textContent = (flashcardIndex + 1) + ' / ' + flashcards.length;
    var html = '<button class="flashcard-box" onclick="flipFlashcard()" aria-label="翻转卡片">';
    html += '<span class="flashcard-tag">' + Quiz.escapeHtml(card.tag || '速记') + '</span>';
    html += '<strong>' + Quiz.escapeHtml(flashcardFlipped ? '答案' : '问题') + '</strong>';
    html += '<span>' + Quiz.escapeHtml(flashcardFlipped ? card.back : card.front).replace(/\n/g, '<br>') + '</span>';
    html += '</button>';
    container.innerHTML = html;
}

function flipFlashcard() {
    if (!flashcards.length) return;
    flashcardFlipped = !flashcardFlipped;
    renderFlashcard();
}

function prevFlashcard() {
    if (!flashcards.length) return;
    flashcardIndex = (flashcardIndex - 1 + flashcards.length) % flashcards.length;
    flashcardFlipped = false;
    renderFlashcard();
}

function nextFlashcard() {
    if (!flashcards.length) return;
    flashcardIndex = (flashcardIndex + 1) % flashcards.length;
    flashcardFlipped = false;
    renderFlashcard();
}
</script>

<%@ include file="footer.jsp" %>
