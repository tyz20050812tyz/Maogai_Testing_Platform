# 毛概复习助手

一个面向《毛泽东思想和中国特色社会主义理论体系概论》课程复习的网站。项目采用 Java Servlet/JSP + Maven WAR 结构，不依赖数据库，题库、用户档案、错题和答题统计均通过 JSON 文件持久化。

## 主要功能

- 强制登录：未登录访问任何页面或 API 都会被拦截到登录页。
- 手机号短信注册：使用阿里云市场短信接口发送验证码，注册时设置用户名和密码。
- 已注册用户登录：用户名 + 密码登录，不允许未注册用户直接进入。
- 用户数据隔离：不同用户拥有独立错题本、答题统计和章节掌握度。
- 两类题库：
  - 章节题库：用于学完章节后的针对性练习。
  - 考试题库：用于整本书综合刷题，可按题库文件选择。
- 章节学习：支持阅读大纲和正文。
- 文字高亮：章节阅读页支持选中文字后高亮，并按用户隔离保存在浏览器本地。
- 错题本：答错自动加入，也可手动加入/移出。
- 章节掌握度地图：按答题记录统计章节正确率，生成复习建议。
- 知识点速记卡片：按章节抽认核心概念。
- AI 错因解释：答错后调用 AI 生成错因说明。
- 题库导入：支持文本、JSON、PDF、图片解析导入。

## 技术栈

- Java 11
- Servlet 4.0 / JSP / JSTL
- Maven
- Gson
- Apache POI：DOCX 解析
- PDFBox：PDF 文本解析
- Qwen/OpenAI-compatible API：AI 答疑和错因解释
- 阿里云市场短信 API：手机号验证码注册

## 目录结构

```text
src/main/java/com/maogai
  model/        数据模型
  service/      业务服务、题库、用户、短信、AI
  servlet/      页面和 API Servlet
  util/         工具类、过滤器

src/main/resources
  ai-config.properties       AI 配置
  sms-config.properties      短信配置
  data/
    chapters.json            章节目录
    questions.json           章节题库
    banks.json               考试题库清单
    exam_banks/*.json        考试题库文件
    flashcards.json          速记卡片
    users.json               注册用户列表
    users/{userKey}/         每个用户的独立学习数据

src/main/webapp
  static/css/style.css
  static/js/quiz.js
  static/js/ai.js
  WEB-INF/views/*.jsp
  WEB-INF/web.xml
```

## 登录与注册

网站已启用全站登录拦截：

- 未登录访问 `/`、`/index`、`/page/*` 会跳转到 `/page/login`。
- 未登录访问 `/api/*` 会返回 `401`。
- 公开路径只有：
  - `/page/login`
  - `/auth/*`
  - `/static/*`

注册流程：

1. 打开 `/page/login`。
2. 输入用户名、密码和手机号。
3. 点击发送验证码。
4. 输入验证码并注册。
5. 注册成功后自动登录并进入网站。

登录流程：

- 已注册用户使用用户名和密码登录。
- 未注册用户不能直接登录。

## 短信配置

配置文件：

```text
src/main/resources/sms-config.properties
```

需要填写：

```properties
sms.app.key=你的AppKey
sms.app.secret=你的AppSecret
sms.app.code=你的AppCode
```

短信接口默认配置：

```properties
sms.host=https://gyytz.market.alicloudapi.com
sms.path=/sms/smsSend
sms.smsSignId=2e65b1bb3d054466b82f0c9d125465e2
sms.templateId=908e94ccf08b4476ba6c876d13f084ad
sms.minute=5
```

说明：

- 当前实现按阿里云市场 `APPCODE` 鉴权方式调用。
- `AppKey` 和 `AppSecret` 保留在配置中，当前请求示例实际使用的是 `AppCode`。
- 如果 `sms.app.code` 为空，系统会进入本地测试模式，页面会显示验证码，方便调试注册流程。

## AI 配置

配置文件：

```text
src/main/resources/ai-config.properties
```

典型配置：

```properties
ai.service.type=custom
ai.custom.api.url=https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions
ai.custom.api.key=你的APIKey
ai.custom.model=qwen3-max-preview
```

也支持通过环境变量或系统属性读取密钥：

```properties
ai.custom.api.key=env:DASHSCOPE_API_KEY
```

未配置时会回退到 mock 模式，基础页面仍可运行。

## 数据持久化

本项目不使用数据库，所有数据写入 JSON 文件。

用户列表：

```text
src/main/resources/data/users.json
```

用户表中保存用户名、手机号、用户 key 和密码哈希，不保存明文密码。

每个用户的学习数据：

```text
src/main/resources/data/users/{userKey}/wrong_book.json
src/main/resources/data/users/{userKey}/answer_records.json
```

说明：

- `{userKey}` 由昵称生成，用于文件夹名。
- 错题本、答题统计、章节掌握度均按用户隔离。
- 阅读高亮保存在浏览器 `localStorage`，key 中包含用户 key 和章节 ID。
- 题库文件本身是全站共享数据。

## 题库配置

章节题库：

```text
src/main/resources/data/questions.json
```

考试题库清单：

```text
src/main/resources/data/banks.json
```

考试题库文件：

```text
src/main/resources/data/exam_banks/*.json
```

考试题库和章节题库是两套不同数据：

- 首页“刷题”默认进入考试题库。
- 章节学习后的练习进入章节题库。
- 考试题库页面会显示题库文件选择。
- 章节题库页面会显示章节选择。

题目 JSON 基本格式：

```json
{
  "id": 1,
  "chapter": 1,
  "type": "single",
  "question": "题干",
  "options": ["A. 选项一", "B. 选项二"],
  "answer": "A",
  "explanation": "解析"
}
```

题型：

- `single`：单选题
- `multiple`：多选题
- `judge`：判断题

## 本地构建

在项目根目录执行：

```bash
mvn -e -DskipTests "-Dmaven.compiler.fork=true" package
```

构建成功后生成：

```text
target/maogai-review.war
target/maogai-review/
```

## 部署

将 WAR 部署到支持 Servlet 4.0 的服务器，例如 Tomcat 9。

```text
target/maogai-review.war
```

部署后访问：

```text
http://服务器地址:端口/maogai-review/
```

首次访问会自动跳转到登录页。

## 常见问题

### 1. 为什么未登录访问首页会跳转？

项目启用了 `AuthFilter`，除登录、注册、短信和静态资源外，所有页面/API 都要求登录。

### 2. 为什么短信没有真实发送？

检查：

- `sms-config.properties` 中 `sms.app.code` 是否填写。
- `sms.smsSignId` 和 `sms.templateId` 是否和短信服务商控制台一致。
- 服务器是否能访问外网。

如果 `sms.app.code` 为空，会进入本地测试模式。

### 3. 为什么部署后数据文件会变化？

服务会同时尝试写入运行时 classpath 和源码资源目录：

- `target/classes/data/...`
- `src/main/resources/data/...`

本地开发时这样方便保留数据。正式部署到服务器时，建议定期备份运行目录下的 `data/users` 和题库文件。

### 4. PDF 上传无法识别图片版题库怎么办？

当前 PDFBox 只能稳定解析文本型 PDF。截图版或扫描版 PDF 需要 OCR，项目中图片题库解析会走 AI/图片识别能力。

### 5. AI 错因解释不出现怎么办？

检查：

- 是否配置 `ai-config.properties`
- API Key 是否可用
- 服务器能否访问模型接口
- 浏览器控制台和服务端日志是否有错误

## 维护建议

- 不要把真实 API Key、AppCode 提交到公开仓库。
- 部署前检查 `sms-config.properties` 和 `ai-config.properties`。
- 备份 `src/main/resources/data/users` 或服务器运行目录中的用户数据。
- 更新题库后重新打包部署。
- 如果移动端显示旧样式，清理浏览器缓存或更新 `header.jsp` 中 CSS/JS 版本号。
