<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ include file="header.jsp" %>

<div class="card">
    <h2 class="card-title">学习统计</h2>
    <p class="section-subtitle">考试题库用于整本书综合复习，章节题库用于学完章节后的针对性练习。</p>
    <div class="dashboard" id="stats-cards">
        <div class="stat-card">
            <div class="stat-number">-</div>
            <div class="stat-label">考试题库总数</div>
        </div>
        <div class="stat-card">
            <div class="stat-number">-</div>
            <div class="stat-label">章节题库总数</div>
        </div>
        <div class="stat-card">
            <div class="stat-number">-</div>
            <div class="stat-label">错题总数</div>
        </div>
        <div class="stat-card">
            <div class="stat-number">-</div>
            <div class="stat-label">考试题库单选</div>
        </div>
        <div class="stat-card">
            <div class="stat-number">-</div>
            <div class="stat-label">考试题库多选</div>
        </div>
    </div>
</div>

<div class="card">
    <h2 class="card-title">章节掌握度地图</h2>
    <p class="section-subtitle" id="stats-mastery-suggestion">根据做题记录计算正确率，低于 60% 标红。</p>
    <div id="stats-mastery-map"></div>
</div>

<div class="card">
    <h2 class="card-title">章节题库分布</h2>
    <table class="stats-table" id="chapter-stats-table">
        <thead>
            <tr><th>章节</th><th>章节练习题数量</th></tr>
        </thead>
        <tbody>
            <tr><td colspan="2" style="color:#999;">加载中...</td></tr>
        </tbody>
    </table>
</div>

<script>
Quiz.init('${pageContext.request.contextPath}');
Quiz.loadMasteryMap('stats-mastery-map', 'stats-mastery-suggestion');

Promise.all([
    Quiz.getJson(Quiz.ctx + '/api/quiz/stats?bank=exam'),
    Quiz.getJson(Quiz.ctx + '/api/quiz/stats?bank=chapter'),
    Quiz.getJson(Quiz.ctx + '/api/outline/list')
]).then(function(results) {
    var exam = results[0] || {};
    var chapterStats = results[1] || {};
    var chapters = Array.isArray(results[2]) ? results[2] : [];

    var cards = document.querySelectorAll('#stats-cards .stat-number');
    cards[0].textContent = exam.total || 0;
    cards[1].textContent = chapterStats.total || 0;
    cards[2].textContent = exam.wrongCount || 0;
    cards[3].textContent = exam.singleCount || 0;
    cards[4].textContent = exam.multipleCount || 0;

    var chapterCount = chapterStats.chapterCount || {};
    var tbody = document.querySelector('#chapter-stats-table tbody');
    var html = '';
    chapters.forEach(function(chapter) {
        html += '<tr><td>' + Quiz.escapeHtml(chapter.title) + '</td><td>' + (chapterCount[chapter.id] || 0) + '</td></tr>';
    });
    tbody.innerHTML = html || '<tr><td colspan="2" style="color:#999;">暂无章节数据</td></tr>';
}).catch(function() {
    document.querySelector('#chapter-stats-table tbody').innerHTML = '<tr><td colspan="2" style="color:#999;">统计加载失败</td></tr>';
});
</script>

<%@ include file="footer.jsp" %>
