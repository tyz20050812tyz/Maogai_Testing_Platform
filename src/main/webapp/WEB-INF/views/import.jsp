<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ include file="header.jsp" %>

<div class="card" style="padding:0;overflow:hidden;">
    <h2 class="card-title" style="padding:16px 24px;">题目导入</h2>
    <p class="section-subtitle" style="padding:0 24px 16px;margin:0;">导入时可以选择写入章节题库或考试题库。</p>

    <div class="import-tabs">
        <button class="import-tab active" data-tab="text" onclick="AI.switchTab('text')">文本粘贴</button>
        <button class="import-tab" data-tab="file" onclick="AI.switchTab('file')">上传文件</button>
        <button class="import-tab" data-tab="image" onclick="AI.switchTab('image')">上传图片</button>
    </div>

    <div class="import-tab-panel" id="tab-text">
        <div class="import-section">
            <p style="margin-bottom:10px;color:#666;font-size:0.9em;">
                支持题目、选项、答案、解析格式，也支持直接粘贴 JSON 题库数据。
            </p>
            <label style="margin-right:10px;">目标题库：
                <select id="import-bank" onchange="updateImportChapterVisibility()">
                    <option value="chapter">章节题库</option>
                    <option value="exam">考试题库</option>
                </select>
            </label>
            <label style="margin-right:10px;" id="import-chapter-wrap">目标章节：
                <select id="import-chapter"></select>
            </label>
        </div>
        <textarea id="import-text-input" class="import-textarea" placeholder="在此粘贴题库文本或 JSON 数据..."></textarea>
        <div style="margin-top:12px;">
            <button class="btn btn-primary" onclick="AI.parseText()">AI解析并预览</button>
            <button class="btn btn-outline" style="margin-left:8px;" onclick="document.getElementById('import-text-input').value='';document.getElementById('import-result').innerHTML='';">清空</button>
        </div>
        <div id="import-result"></div>
    </div>

    <div class="import-tab-panel" id="tab-file" style="display:none;">
        <div class="import-section">
            <p style="margin-bottom:10px;color:#666;font-size:0.9em;">支持上传 .txt 或 .json 文件。</p>
            <label style="margin-right:10px;">目标题库：
                <select id="import-bank-file" onchange="updateImportChapterVisibility()">
                    <option value="chapter">章节题库</option>
                    <option value="exam">考试题库</option>
                </select>
            </label>
            <label style="margin-right:10px;" id="import-chapter-file-wrap">目标章节：
                <select id="import-chapter-file"></select>
            </label>
        </div>
        <form id="file-upload-form" enctype="multipart/form-data">
            <div class="import-upload-zone" onclick="document.getElementById('file-input').click()">
                <div style="font-size:2em;">文件</div>
                <p>点击选择文件或拖拽到此处</p>
                <p style="font-size:0.8em;">支持 .txt / .json</p>
            </div>
            <input type="file" id="file-input" name="file" accept=".txt,.json" style="display:none" onchange="uploadFileToServer()">
        </form>
        <div id="file-result"></div>
    </div>

    <div class="import-tab-panel" id="tab-image" style="display:none;">
        <div class="import-section">
            <p style="margin-bottom:10px;color:#666;font-size:0.9em;">上传包含题目的图片，AI 会尝试识别并提取题目。</p>
            <label style="margin-right:10px;">目标题库：
                <select id="import-bank-image" onchange="updateImportChapterVisibility()">
                    <option value="chapter">章节题库</option>
                    <option value="exam">考试题库</option>
                </select>
            </label>
            <label style="margin-right:10px;" id="import-chapter-image-wrap">目标章节：
                <select id="import-chapter-image"></select>
            </label>
        </div>
        <div class="import-upload-zone" onclick="document.getElementById('image-input').click()">
            <div style="font-size:2em;">图片</div>
            <p>点击选择图片或拖拽到此处</p>
            <p style="font-size:0.8em;">支持 .jpg / .png / .gif / .bmp / .webp</p>
        </div>
        <input type="file" id="image-input" name="image" accept="image/*" style="display:none" onchange="uploadImageToServer(this)">
        <div id="image-result"></div>
    </div>
</div>

<script>
AI.init('${pageContext.request.contextPath}');
Quiz.init('${pageContext.request.contextPath}');
Quiz.loadChapterOptions(['import-chapter', 'import-chapter-file', 'import-chapter-image'], '1', false);

function updateImportChapterVisibility() {
    [
        ['import-bank', 'import-chapter-wrap'],
        ['import-bank-file', 'import-chapter-file-wrap'],
        ['import-bank-image', 'import-chapter-image-wrap']
    ].forEach(function(pair) {
        var bankSelect = document.getElementById(pair[0]);
        var chapterWrap = document.getElementById(pair[1]);
        if (bankSelect && chapterWrap) {
            chapterWrap.style.display = bankSelect.value === 'chapter' ? '' : 'none';
        }
    });
}

updateImportChapterVisibility();

function uploadFileToServer() {
    var fileInput = document.getElementById('file-input');
    var file = fileInput.files[0];
    if (!file) return;

    var chapter = document.getElementById('import-chapter-file').value;
    var bank = document.getElementById('import-bank-file').value;
    var formData = new FormData();
    formData.append('file', file);
    formData.append('chapter', chapter);
    formData.append('bank', bank);

    var resultDiv = document.getElementById('file-result');
    resultDiv.innerHTML = '<div class="loading-state">正在上传解析...</div>';

    fetch(AI.ctx + '/api/import/upload', {
        method: 'POST',
        body: formData
    })
    .then(function(r) { return r.json(); })
    .then(function(data) {
        if (data.success) {
            resultDiv.innerHTML = '<div class="import-result success">' +
                '<p>成功导入 <strong>' + data.count + '</strong> 道题目。</p>' +
                '</div>';
        } else {
            resultDiv.innerHTML = '<div class="import-result error">导入失败：' + (data.message || '未知错误') + '</div>';
        }
    })
    .catch(function(err) {
        resultDiv.innerHTML = '<div class="import-result error">上传失败：' + err + '</div>';
    });
}

function uploadImageToServer(input) {
    var textChapter = document.getElementById('import-chapter');
    var imageChapter = document.getElementById('import-chapter-image');
    var textBank = document.getElementById('import-bank');
    var imageBank = document.getElementById('import-bank-image');
    if (textChapter && imageChapter) textChapter.value = imageChapter.value;
    if (textBank && imageBank) textBank.value = imageBank.value;

    var originalResult = document.getElementById('import-result');
    var imageResult = document.getElementById('image-result');
    if (!originalResult || !imageResult) {
        AI.uploadImage(input);
        return;
    }

    originalResult.id = 'import-result-temp';
    imageResult.id = 'import-result';
    AI.uploadImage(input);
    setTimeout(function() {
        var activeResult = document.getElementById('import-result');
        if (activeResult) activeResult.id = 'image-result';
        var temp = document.getElementById('import-result-temp');
        if (temp) temp.id = 'import-result';
    }, 100);
}
</script>

<%@ include file="footer.jsp" %>
