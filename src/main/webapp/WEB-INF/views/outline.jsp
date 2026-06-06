<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ include file="header.jsp" %>

<div class="card">
    <h2 class="card-title">章节大纲</h2>
    <p class="section-subtitle">按课程章节阅读大纲内容，也可以从章节阅读页直接进入对应练习。</p>
    <ul class="chapter-list" id="chapter-list-container">
        <li class="loading-state">加载中...</li>
    </ul>
</div>

<script>
Quiz.init('${pageContext.request.contextPath}');

Quiz.getJson(Quiz.ctx + '/api/outline/list')
    .then(function(data) {
        var html = '';
        if (Array.isArray(data)) {
            data.forEach(function(ch) {
                html += '<li class="chapter-item">';
                html += '<a href="' + Quiz.ctx + '/page/reader?id=' + ch.id + '">' + ch.title + '</a>';
                html += '<span class="chapter-order">第' + ch.order + '章</span>';
                html += '</li>';
            });
        }
        document.getElementById('chapter-list-container').innerHTML = html || '<li class="empty-state">暂无章节数据</li>';
    });
</script>

<%@ include file="footer.jsp" %>
