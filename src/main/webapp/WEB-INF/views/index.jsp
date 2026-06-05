<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="header.jsp" %>

<section class="hero-panel">
    <div class="hero-main">
        <div class="hero-kicker">课程复习工作台</div>
        <h1>章节学习和考试刷题分开走，复习节奏更清楚</h1>
        <p>先按章节阅读知识要点，再做对应章节练习；考前回到整本书考试题库，进行综合刷题和随机测试。</p>
        <div class="hero-actions">
            <a href="${pageContext.request.contextPath}/page/quiz?bank=exam" class="btn btn-primary">刷考试题库</a>
            <a href="${pageContext.request.contextPath}/page/random?bank=exam" class="btn btn-outline">考试随机测试</a>
            <a href="${pageContext.request.contextPath}/page/outline" class="btn btn-outline">章节学习</a>
        </div>
    </div>
    <aside class="study-panel">
        <h2>推荐复习节奏</h2>
        <ul class="study-list">
            <li><span>1</span><div>进入章节大纲，按知识要点学习每一章内容。</div></li>
            <li><span>2</span><div>学完章节后点击“做本章练习题”，只练当前章节题库。</div></li>
            <li><span>3</span><div>考前使用考试题库，按整本书范围进行综合刷题。</div></li>
        </ul>
    </aside>
</section>

<div class="card">
    <h2 class="card-title">考试题库概览</h2>
    <div class="dashboard">
        <div class="stat-card">
            <div class="stat-number">${stats.total}</div>
            <div class="stat-label">考试题库总数</div>
            <a href="${pageContext.request.contextPath}/page/quiz?bank=exam" class="stat-link">开始刷题</a>
        </div>
        <div class="stat-card">
            <div class="stat-number">${stats.singleCount + stats.multipleCount + stats.judgeCount}</div>
            <div class="stat-label">可用考试题</div>
            <a href="${pageContext.request.contextPath}/page/random?bank=exam" class="stat-link">随机测试</a>
        </div>
        <div class="stat-card">
            <div class="stat-number">${wrongCount}</div>
            <div class="stat-label">错题记录</div>
            <a href="${pageContext.request.contextPath}/page/wrong" class="stat-link">查看错题</a>
        </div>
        <div class="stat-card">
            <div class="stat-number">${chapterList.size()}</div>
            <div class="stat-label">学习章节</div>
            <a href="${pageContext.request.contextPath}/page/outline" class="stat-link">浏览大纲</a>
        </div>
    </div>
</div>

<div class="card">
    <h2 class="card-title">快捷入口</h2>
    <div class="quick-links">
        <a href="${pageContext.request.contextPath}/page/quiz?bank=exam" class="quick-link">
            <div class="quick-link-icon">考</div>
            <div>
                <strong>考试题库</strong>
                <div class="stat-label">整本书综合刷题</div>
            </div>
        </a>
        <a href="${pageContext.request.contextPath}/page/random?bank=exam" class="quick-link">
            <div class="quick-link-icon">测</div>
            <div>
                <strong>考试随机测试</strong>
                <div class="stat-label">考前快速自检</div>
            </div>
        </a>
        <a href="${pageContext.request.contextPath}/page/outline" class="quick-link">
            <div class="quick-link-icon">章</div>
            <div>
                <strong>章节学习</strong>
                <div class="stat-label">学完再做章节练习</div>
            </div>
        </a>
        <a href="${pageContext.request.contextPath}/page/quiz?bank=chapter" class="quick-link">
            <div class="quick-link-icon">练</div>
            <div>
                <strong>章节题库</strong>
                <div class="stat-label">按章节巩固知识点</div>
            </div>
        </a>
        <a href="${pageContext.request.contextPath}/page/wrong" class="quick-link">
            <div class="quick-link-icon">错</div>
            <div>
                <strong>错题本</strong>
                <div class="stat-label">汇总两套题库错题</div>
            </div>
        </a>
        <a href="${pageContext.request.contextPath}/page/import" class="quick-link">
            <div class="quick-link-icon">导</div>
            <div>
                <strong>题目导入</strong>
                <div class="stat-label">选择导入到不同题库</div>
            </div>
        </a>
    </div>
</div>

<div class="card">
    <h2 class="card-title">章节导航</h2>
    <p class="section-subtitle">从章节进入阅读页，阅读后可以直接做本章练习。</p>
    <ul class="chapter-list">
        <c:forEach var="ch" items="${chapterList}">
            <li class="chapter-item">
                <a href="${pageContext.request.contextPath}/page/reader?id=${ch.id}">${ch.title}</a>
                <span class="chapter-order">第${ch.order}章</span>
            </li>
        </c:forEach>
    </ul>
</div>

<div class="card">
    <h2 class="card-title">考试题型分布</h2>
    <div class="stats-grid">
        <div class="stat-card">
            <div class="stat-number" style="color:#1d4ed8">${stats.singleCount}</div>
            <div class="stat-label">单选题</div>
        </div>
        <div class="stat-card">
            <div class="stat-number" style="color:#b7791f">${stats.multipleCount}</div>
            <div class="stat-label">多选题</div>
        </div>
        <div class="stat-card">
            <div class="stat-number" style="color:#15803d">${stats.judgeCount}</div>
            <div class="stat-label">判断题</div>
        </div>
    </div>
</div>

<%@ include file="footer.jsp" %>
