<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ include file="header.jsp" %>

<div class="card" style="padding:0;overflow:hidden;">
    <h2 class="card-title" style="padding:16px 24px;">AI答疑</h2>
    <div class="chat-container">
        <div class="chat-messages" id="chat-messages">
            <div class="chat-message ai">
                <div class="chat-bubble">
                    你好！我是毛概复习助手的AI答疑助手。<br><br>
                    我可以回答关于以下内容的问题：<br>
                    - 毛泽东思想和马克思主义中国化<br>
                    - 新民主主义革命理论<br>
                    - 社会主义改造理论<br>
                    - 邓小平理论、三个代表、科学发展观<br>
                    - 习近平新时代中国特色社会主义思想<br><br>
                    请输入你的问题，我会尽力解答。
                </div>
            </div>
        </div>
        <div class="chat-input-area">
            <textarea id="ai-input" placeholder="输入你的问题..." rows="2" onkeydown="AI.handleKeydown(event)"></textarea>
            <button class="btn btn-primary" onclick="AI.sendQuestion()">发送</button>
        </div>
    </div>
</div>

<script>
AI.init('${pageContext.request.contextPath}');
</script>

<%@ include file="footer.jsp" %>
