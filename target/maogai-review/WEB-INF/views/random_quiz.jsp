<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ include file="header.jsp" %>

<div class="card">
    <h2 class="card-title" id="random-title">考试随机测试</h2>
    <p class="section-subtitle" id="random-subtitle">从考试题库中随机抽题，适合考前快速自检。</p>
    <div class="random-setup" id="random-setup">
        <p style="margin-bottom:16px;font-size:1.08rem;font-weight:800;">选择测试参数</p>
        <label class="exam-random-filter">题库文件：
            <select id="random-exam-bank">
                <option value="">加载中...</option>
            </select>
        </label>
        <label>题目数量：
            <select id="random-count">
                <option value="5">5题</option>
                <option value="10" selected>10题</option>
                <option value="15">15题</option>
                <option value="20">20题</option>
            </select>
        </label>
        <label class="chapter-random-filter">章节范围：
            <select id="random-chapter">
                <option value="">全部章节</option>
            </select>
        </label>
        <div style="margin-top:18px;">
            <button class="btn btn-primary" onclick="startRandomQuiz()">开始测试</button>
        </div>
    </div>
    <div id="random-quiz-container"></div>
</div>

<script>
Quiz.init('${pageContext.request.contextPath}');
var randomParams = new URLSearchParams(window.location.search);
var randomBank = randomParams.get('bank') === 'chapter' ? 'chapter' : 'exam';
var randomExamBank = randomParams.get('examBank') || '';
Quiz.setBank(randomBank);
Quiz.setExamBank(randomExamBank);

if (randomBank === 'chapter') {
    document.getElementById('random-title').textContent = '章节随机测试';
    document.getElementById('random-subtitle').textContent = '从章节题库中随机抽题，适合学完章节后复盘。';
    document.querySelectorAll('.exam-random-filter').forEach(function(el) {
        el.style.display = 'none';
    });
} else {
    document.querySelectorAll('.chapter-random-filter').forEach(function(el) {
        el.style.display = 'none';
    });
}

Quiz.loadChapterOptions('random-chapter', '', true);
Quiz.loadExamBankOptions('random-exam-bank', randomExamBank);

function startRandomQuiz() {
    var count = document.getElementById('random-count').value;
    var chapter = randomBank === 'chapter' ? document.getElementById('random-chapter').value : '';
    var examBank = randomBank === 'exam' ? document.getElementById('random-exam-bank').value : '';
    Quiz.setExamBank(examBank);
    document.getElementById('random-setup').style.display = 'none';
    Quiz.randomQuiz(chapter, count, 'random-quiz-container', randomBank, examBank);
}
</script>

<%@ include file="footer.jsp" %>
