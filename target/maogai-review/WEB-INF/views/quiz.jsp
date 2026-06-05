<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ include file="header.jsp" %>

<div class="card">
    <h2 class="card-title" id="quiz-title">考试题库练习</h2>
    <p class="section-subtitle" id="quiz-subtitle">面向整本书考试复习，适合集中刷题和考前自测。</p>
    <div class="filter-bar">
        <a class="btn btn-small btn-primary" id="exam-bank-link" href="${pageContext.request.contextPath}/page/quiz?bank=exam">考试题库</a>
        <a class="btn btn-small btn-outline" id="chapter-bank-link" href="${pageContext.request.contextPath}/page/quiz?bank=chapter">章节题库</a>
        <span class="exam-bank-filter">题库文件</span>
        <select id="filter-exam-bank" class="exam-bank-filter" onchange="applyFilter()">
            <option value="">加载中...</option>
        </select>
        <span class="chapter-filter-label">章节</span>
        <select id="filter-chapter" class="chapter-filter-label" onchange="applyFilter()">
            <option value="">全部章节</option>
        </select>
        <span>题型</span>
        <select id="filter-type" onchange="applyFilter()">
            <option value="">全部题型</option>
            <option value="single">单选题</option>
            <option value="multiple">多选题</option>
            <option value="judge">判断题</option>
        </select>
    </div>
    <div id="question-list-container"></div>
</div>

<script>
Quiz.init('${pageContext.request.contextPath}');

var urlParams = new URLSearchParams(window.location.search);
var initBank = urlParams.get('bank') === 'chapter' ? 'chapter' : 'exam';
var initChapter = urlParams.get('chapter') || '';
var initType = urlParams.get('type') || '';
var initExamBank = urlParams.get('examBank') || '';
Quiz.setBank(initBank);
Quiz.setExamBank(initExamBank);

document.getElementById('filter-type').value = initType;

function configureQuizPage() {
    var isChapter = initBank === 'chapter';
    document.getElementById('quiz-title').textContent = isChapter ? '章节题库练习' : '考试题库练习';
    document.getElementById('quiz-subtitle').textContent = isChapter
        ? '按章节巩固刚学过的知识点，适合阅读章节后立即练习。'
        : '面向整本书考试复习，适合集中刷题和考前自测。';
    document.getElementById('exam-bank-link').className = isChapter ? 'btn btn-small btn-outline' : 'btn btn-small btn-primary';
    document.getElementById('chapter-bank-link').className = isChapter ? 'btn btn-small btn-primary' : 'btn btn-small btn-outline';
    document.querySelectorAll('.chapter-filter-label').forEach(function(el) {
        el.style.display = isChapter ? '' : 'none';
    });
    document.querySelectorAll('.exam-bank-filter').forEach(function(el) {
        el.style.display = isChapter ? 'none' : '';
    });
}

function applyFilter() {
    var chapter = initBank === 'chapter' ? document.getElementById('filter-chapter').value : '';
    var type = document.getElementById('filter-type').value;
    var examBank = initBank === 'exam' ? document.getElementById('filter-exam-bank').value : '';
    Quiz.setExamBank(examBank);
    Quiz.loadQuestions(chapter, type, 'question-list-container', initBank, examBank);
}

configureQuizPage();
Quiz.loadChapterOptions('filter-chapter', initChapter, true, function() {
    if (initBank === 'chapter') {
        applyFilter();
    }
});
Quiz.loadExamBankOptions('filter-exam-bank', initExamBank, function() {
    if (initBank === 'exam') {
        applyFilter();
    }
});
</script>

<%@ include file="footer.jsp" %>
