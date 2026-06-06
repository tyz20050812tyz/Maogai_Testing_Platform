<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ include file="header.jsp" %>

<div class="card">
    <h2 class="card-title">错题本</h2>
    <p class="section-subtitle">答错的题会自动收录到这里，复盘完成后可以手动移除或清空。</p>
    <div class="wrong-toolbar">
        <span class="wrong-count" id="wrong-count"></span>
        <div class="wrong-toolbar-actions">
            <a href="${pageContext.request.contextPath}/page/quiz" class="btn btn-outline btn-small">← 返回练习</a>
            <button class="btn btn-outline btn-small" style="color:#c41e3a;border-color:#c41e3a;" onclick="clearWrongBook()">清空错题本</button>
        </div>
    </div>
    <div class="wrong-list" id="wrong-list-container">
        <p style="text-align:center;color:#999;padding:20px;">加载中...</p>
    </div>
</div>

<script>
Quiz.init('${pageContext.request.contextPath}');

// 加载错题并更新计数
(function() {
    var container = document.getElementById('wrong-list-container');
    Quiz.setLoading(container, '正在加载错题...');
    Quiz.getJson(Quiz.ctx + '/api/wrong/list')
        .then(function(data) {
            var countEl = document.getElementById('wrong-count');
            if (data.success && data.data && data.data.length) {
                countEl.textContent = '共 ' + data.data.length + ' 道错题';
                Quiz.renderWrongBook(data.data, container);
            } else {
                countEl.textContent = '共 0 道错题';
                container.innerHTML = '<div class="empty-state">暂无错题记录，答错的题会自动收录到这里</div>';
            }
        })
        .catch(function() {
            document.getElementById('wrong-count').textContent = '';
            container.innerHTML = '<div class="empty-state">错题加载失败，请稍后重试</div>';
        });
})();

function clearWrongBook() {
    if (!confirm('确定清空所有错题记录？此操作不可撤销。')) return;
    fetch(Quiz.ctx + '/api/wrong/clear', { method: 'POST' })
        .then(function(r) { return r.json(); })
        .then(function(data) {
            if (data.success) {
                location.reload();
            }
        });
}
</script>

<%@ include file="footer.jsp" %>
