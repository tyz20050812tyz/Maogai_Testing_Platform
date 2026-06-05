<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ include file="header.jsp" %>

<div class="card">
    <h2 class="card-title">错题本</h2>
    <p class="section-subtitle">答错的题会自动收录到这里，复盘完成后可以手动移除或清空。</p>
    <div style="margin-bottom:12px;text-align:right;">
        <button class="btn btn-outline btn-small" onclick="clearWrongBook()">清空错题本</button>
    </div>
    <div class="wrong-list" id="wrong-list-container">
        <p style="text-align:center;color:#999;padding:20px;">加载中...</p>
    </div>
</div>

<script>
Quiz.init('${pageContext.request.contextPath}');
Quiz.loadWrongBook('wrong-list-container');

function clearWrongBook() {
    if (!confirm('确定清空所有错题记录？')) return;
    fetch(Quiz.ctx + '/api/wrong/clear', { method: 'POST' })
        .then(function(r) { return r.json(); })
        .then(function(data) {
            if (data.success) {
                Quiz.loadWrongBook('wrong-list-container');
            }
        });
}
</script>

<%@ include file="footer.jsp" %>
