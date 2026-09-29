# Feature: DrillBit 安卓刷题 App（MVP）

**作者**: yangming
**日期**: 2026-09-29
**状态**: Draft

---

## 1. 背景 (Background)

### 1.1 问题描述

- 用户准备转行找大模型 Agent 方向工作（非科班），需要消化大量技术文档，但大段文档看了容易忘
- 缺乏一个能在碎片化时间（通勤、排队等）用手机刷选择题强化记忆的工具
- 题库内容会持续更新，要求 App 不发版即可拿到最新题目
- 遇到理解不到位的题，希望随手问大模型并把有启发的回答沉淀为个人笔记

### 1.2 现状分析

- 本项目为全新项目，无任何现有代码，无 commit
- 技术栈已决策（2026-09-29 与用户对齐）：
  - App：原生 Kotlin + Jetpack Compose，复用 StoryTeller 工程骨架（`/Users/yangming/pycharm-workspace/StoryTeller`），版本组合已验证：AGP 8.5.2 / Gradle 8.9 wrapper / Kotlin 2.0.21 / Room 2.6.1 (KSP) / minSdk 28 / targetSdk 35
  - 需新增的 Gradle 依赖：HTTP 客户端（题库拉取、大模型 API 调用、笔记上传）、JSON 解析库；无系统级环境安装
  - 后端：Python（FastAPI 倾向，接口形态待设计阶段细化）
- 部署与调试环境约束（来自 StoryTeller 经验，`agent_docs/安卓开发通用经验.md`）：
  - 目标手机为华为鸿蒙（卓易通容器）：无 adb、无 logcat
  - 部署链路：Mac 构建 → 传手机 → 卓易通安装 → 用户实测 → 截图反馈
  - 无 adb 崩溃定位方案：Application 注册 UncaughtExceptionHandler 写 `crash_last.txt`，下次启动弹窗展示
  - 第一天必须完成：keystore 签名、崩溃捕获、设置页版本号显示

### 1.3 主要使用场景

- 通勤、排队等碎片时间：打开 App，从上次断点继续刷某个课题的题库
- 阶段性自测：手动勾选多个题库，抽若干道题做混合测试
- 复习强化：查看错题集（含答案解析），或对全部错题做一次重考
- 深入理解：做题时对不理解的知识点直接向大模型提问，将有启发的回答一键存入笔记
- 定期回顾：翻阅个人笔记，必要时让大模型梳理笔记

## 2. 目标 (Goals)

- 提供一个通用刷题工具 App：分课题题库、做题、错题、AI 问答、笔记五大能力，支撑碎片化时间的记忆强化
- 题库内容与 App 版本解耦：题目存放在用户个人服务器，App 拉取到本地后离线可用，手动触发增量更新
- 做题记录、错题、笔记全部本地持久化（Room），断点续刷、随时复习

### 2.1 非目标 (Non-Goals)

- 不做多用户/账号体系：自用 App，无上架、无商标压力
- 不做统计图表（正确率曲线等可视化报表）
- 不做云同步（笔记上传服务器为用户明确要求的备份能力，非全量数据同步）
- 不做间隔复习算法（SM-2 等调度类功能）
- 不做 iOS/跨平台版本
- 多选题、判断题仅做 schema 预留，MVP 只实现单选题
- 不做搜索：题库、错题、笔记均不提供搜索；笔记搜索的触发条件为「笔记超过约 100 条」或「连续两次记得写过但找不到」，届时再议（当前由梳理归纳稿承担检索职能）

## 3. 需求细化 (Requirements)

### 3.1 功能性需求
**全局导航**：底部四 Tab（题库 / 错题 / 笔记 / 设置）；AI 问答不单独占 Tab，仅从做题页「问 AI」进入

**模块一：题库管理**
- 服务器上的题库按课题分库（如 LLM 相关、Agent 相关、模型调优相关；课题内容不固定，App 不绑定主题）
- App 手动拉取题库列表与题目内容，拉取后本地离线可用
- 再次拉取时对比版本，增量更新已下载题库
- 可删除本地指定题库（删除后可重新拉取）
- 题库详情页：显示题数、上次拉取时间，用于确认更新是否生效

**模块二：做题**
- 单库顺序刷：选定题库后一题接一题作答
- 断点续刷：记住每个题库做到第几题，下次打开接着刷
- 混合抽题：手动勾选多个题库，抽选若干道题组成混合卷作答；题数用加减号选择器设定，默认 10、下限 10、步进 ±10（已到 10 时减号不可再减）；抽题按题目相对权重非等概率抽取，每题带权重字段（权重设计待设计阶段确定）；混合卷中每题标注来源题库
- 答题即时反馈：作答后立即展示对/错与解析（无论对错）；需手动点「下一题」按钮才继续，强制停顿强化记忆
- 答对进入下一题；答错自动记入错题集

**模块三：错题**
- 错题集：汇总展示做错的题，含答案与解析
- 错题重考：对全部错题发起一次重新考试；每题带计数器，默认 3，重考答对一次减 1，减到 0 移出错题集，中途再答错则重置为 3

**模块四：AI 问答**
- 模型配置：用户自配 url、apiKey、modelName、接口类型（OpenAI 兼容 或 Anthropic）
- 从题目发起提问：自动携带题干、选项、解析作为上下文
- 回答有启发时，一键保存到个人笔记

**模块五：笔记**
- 笔记浏览与复习
- 大模型梳理：将全部笔记发给模型，输出一篇结构化知识点归纳稿（按主题分组、去重、提炼要点），归纳稿本身也存入笔记
- 笔记上传到个人服务器：单向上传纯备份，防手机丢失/换机丢笔记；不做拉回浏览/恢复功能

**模块六：设置**
- 显示 App 版本号（versionName，无线装机时确认新包的唯一手段）
- 模型配置管理、服务器地址配置
- 深色模式：一键开关，双主题（亮色暖色书卷风 / 暗色护眼风），只换色板不换排版；不做「跟随系统」选项

### 3.2 非功能性需求

- 性能：刷题交互全本地（Room 读取），无网络依赖，滑动/作答不卡顿；AI 问答响应时长可接受即可（取决于用户自配模型）
- 持久性：做题记录、断点位置、错题计数、笔记即时写库（Room），进程被杀/断电不丢数据
- 兼容性：题库 JSON schema 向后兼容，服务端新增字段旧版 App 忽略不报错；题型字段预留单选/多选/判断，MVP 只实现单选
- 升级：Room 实体变更必须 version+1 + Migration，老用户升级不丢数据
- 安全：个人服务器公网可访问，接口加简单 token 鉴权（App 设置页配置）；模型 apiKey 仅本地存储不上传；服务器地址优先 https（安卓默认禁明文 http）
- 可观测：崩溃捕获（UncaughtExceptionHandler 写 crash_last.txt，下次启动弹窗），无 adb 环境下的唯一崩溃定位手段
- 部署：卓易通无线装机链路（Mac 构建→传手机→安装→截图反馈），设置页显示版本号确认新包
- UI 主题：双主题色值单一来源（亮/暗各一套语义变量：bg/card/text/text2/line/primary/ok/bad），页面代码不出现字面色值（详见页面设计稿）

## 4. 设计方案 (Design)

### 4.1 方案概览

**双端形态**：安卓 App（主体，离线优先）+ 个人服务器 Python FastAPI（无状态分发：题库 JSON + 笔记备份）。AI 问答由 App 直连用户自配的大模型端点，不经过自己服务器。

**App 分层**（单向依赖，UI → Repository → 数据源）：

```
ui（Compose 页面 + ViewModel，MVVM）
  → repo（仓库层，业务编排，暴露挂起函数 + Flow）
    → data.db（Room：题库/进度/错题/笔记）
    → data.net（服务器客户端 ServerApi / 大模型客户端 LlmClient）
    → datastore（用户配置）
```

**数据所有权**：题库源数据归服务器，App 本地为缓存（可删可重拉）；做题进度、错题、笔记归 App 本地（笔记单向上传备份）。单一 Activity + Navigation Compose，底部四 Tab（导航已由页面设计稿定稿）。

**关键 trade-off**：库内更新采用全量替换而非题目级 diff（题库百题级、文件 <1MB，全量简单可靠）；题库更新后断点直接重置（不做题目 id 对齐）；AI 回复采用 SSE 流式（体验优先，两套协议解析复杂度可接受）。

### 4.2 组件设计 (Component Design)

#### 4.2.1 核心类/模块设计

包结构 `com.drillbit`：

| 包 | 职责 |
|------|------|
| `ui.banks` | P1-P3：题库列表、详情、更新弹窗 |
| `ui.quiz` | P4-P7：混合抽题配置、刷题页（单库/混合/重考共用） |
| `ui.wrong` | P8-P9：错题集、错题重考 |
| `ui.notes` | P12-P15：笔记列表/编辑、归纳稿、备份 |
| `ui.chat` | P10-P11：AI 问答、保存笔记弹层 |
| `ui.settings` | P16-P18：设置、模型配置、服务器配置 |
| `ui.theme` | 双主题色板（亮/暗各一套语义变量）与 Typography |
| `data.db` | Room 实体 + DAO（五张表，见 4.2.3） |
| `data.net` | ServerApi（题库拉取/笔记上传）、LlmClient 及 OpenAI/Anthropic 两实现 |
| `data.repo` | BankRepository / QuizRepository / WrongRepository / NoteRepository / ChatRepository |
| `model` | QuizSession（做题会话，见下）等领域模型 |
| `util` | 崩溃捕获、时间格式化 |

**QuizSession（做题会话）**：三种做题场景（单库顺序刷 / 混合卷 / 错题重考）统一抽象为内存中的题目队列 + 游标，共用同一刷题页，仅顶部进度与来源标签不同。单库模式的游标每题落库（断点续刷）；混合卷与重考为临时会话，退出即弃（spec 断点续刷仅限单库）。

**LlmClient（策略模式，SSE 流式）**：接口定义 `chat(system, user): Flow<String>`（逐段发射增量文本）与 `testConnection(): 延迟毫秒`；`OpenAiClient`（POST {url}/chat/completions，stream=true，Authorization: Bearer key，解析 data: 行的 choices[].delta.content）与 `AnthropicClient`（POST {url}/v1/messages，stream=true，解析 content_block_delta 事件，x-api-key + anthropic-version 头）两实现。OkHttp 流式读响应体全程 Dispatchers.IO，UI 侧收集 Flow 增量拼接气泡文本；笔记梳理同样走流式。

#### 4.2.2 接口设计

**服务器接口（FastAPI，全部需鉴权：Authorization: Bearer <token>，静态 token 配置在服务器端）**：

| 方法 | 路径 | 说明 | 错误 |
|------|------|------|------|
| GET | `/api/index` | 题库目录：`[{id, name, version, updatedAt, questionCount}]` | 401 token 错 |
| GET | `/api/banks/{bankId}` | 单个题库全量 JSON（schema 见 4.2.3） | 401 / 404 库不存在 |
| POST | `/api/notes` | 全量笔记备份，body 为全部笔记数组，服务器覆盖存储最新快照 | 401 / 4xx body 非法 |

笔记备份采用全量覆盖：笔记量百条内、总量 <1MB，单次 POST 简单幂等；页面设计稿 P15 的逐条进度改为「上传中」状态展示（真进度无必要）。服务器响应统一 JSON，错误含可读 reason。

**App 内对外接口（Repository 层，挂起函数）**：`BankRepository.syncIndex()` / `downloadBank(id)` / `deleteBank(id)`；`QuizRepository.startSession(mode)` / `submitAnswer(...)`；`NoteRepository.summarizeAll()` / `backup()` 等。错误用 sealed Result（Ok / Err(reason)），不抛异常到 UI 层。

#### 4.2.3 数据模型

**题库 JSON schema（服务器 → App，向后兼容：新增字段旧版忽略）**：

```json
// /api/index 返回
[{ "id": "llm-basics", "name": "大模型基础", "version": 3,
   "updatedAt": "2026-09-27", "questionCount": 128 }]

// /api/banks/llm-basics 返回
{ "id": "llm-basics", "name": "大模型基础", "version": 3, "updatedAt": "2026-09-27",
  "questions": [
    { "id": "q-0001", "type": "single",
      "stem": "题干文本",
      "options": ["选项 A", "选项 B", "选项 C", "选项 D"],
      "answers": [2],
      "explanation": "解析文本",
      "weight": 1 }
  ]
}
```

- `type`：`single` / `multi` / `judge`（预留，MVP 仅 single；judge 的 options 仍提供两项，multi 的 answers 含多个下标，解析逻辑统一）
- `answers`：统一为下标数组（single 取 [0]），避免多形态字段
- `weight`：抽题权重，整数 1-5，默认 1，越大越容易被抽中；由出题人静态标注，不与做题表现联动（避免变成复习算法）
- 解析必填；id 库内唯一

**Room 表（database version 1）**：

| 表 | 字段 | 说明 |
|------|------|------|
| `banks` | id PK / name / version / updatedAt / questionCount / lastSyncAt | 本地题库元信息 |
| `questions` | id PK("bankId:qid") / bankId / orderIndex / type / stem / optionsJson / answersJson / explanation / weight | 外键 bankId→banks.id CASCADE；索引 (bankId, orderIndex)；单题几 KB，远低于 CursorWindow 2MB 上限 |
| `progress` | bankId PK / nextIndex / doneCount | 断点续刷，仅单库模式使用 |
| `wrong` | questionId PK / bankId / retryCount / wrongCount / addedAt / lastWrongAt | 错题计数器；外键 questionId→questions.id CASCADE（删题库连带清错题） |
| `notes` | id auto PK / title / content / source(题目/AI问答/归纳稿) / sourceQuestionId可空 / bankName可空 / createdAt / updatedAt | 笔记不随题库删除级联（内容自成一体，来源仅作展示文字） |

**DataStore（用户配置，非 Room）**：serverUrl / serverToken / llmUrl / llmKey / llmModel / llmType(openai|anthropic) / darkTheme / lastBackupAt。llmKey 仅本机存储不上传；自用场景不做加密存储（后续可升级 EncryptedFile）。JSON 序列化用 kotlinx.serialization（ignoreUnknownKeys=true）。DAO 全挂起函数，Flow 驱动 UI。

#### 4.2.4 并发模型

- 线程纪律（红线）：网络请求 + 读响应体全程 `Dispatchers.IO`；Room 挂起 DAO 自切 IO；UI 只在主线程组合
- 无自定义共享可变状态：QuizSession 活在所属 ViewModel（主线程）；全局单例仅崩溃捕获与 DataStore（线程安全）
- 无锁；并发面收敛在协程作用域（viewModelScope / Repository 内 IO）

#### 4.2.5 错误处理

| 失败模式 | 处理 |
|------|------|
| 网络超时/不可达 | 页面 banner 提示含原因（服务器地址、超时等），手动重试，不自动重试 |
| 401 | 提示 token/配置错误，引导去设置页 |
| JSON 解析失败 | 忽略未知字段；结构性缺失（answers 空、id 重复）则该库更新失败并提示，本地旧数据不受影响 |
| 更新中断 | 题库下载全量校验通过后单事务替换（@Transaction），失败不落半截数据 |
| 崩溃 | UncaughtExceptionHandler 写 crash_last.txt，下次启动弹窗（P19） |
| AI 调用失败/流式中断 | 气泡内提示错误与原因，已收到的增量内容保留，可重发 |

### 4.3 核心逻辑实现

**1. 加权抽题（Efraimidis-Spirakis 无放回加权抽样）**

混合抽题时对勾选题库的全部候选题：每题生成 `key = ln(u) / w`（u 为 (0,1) 均匀随机数，w 为题目权重），取 key 最大的 N 题即为按权重比例的无放回样本，按 key 降序作为出题顺序（天然随机）。O(n log n)，约 5 行代码。数学上等价于逐题轮盘赌删除法，但后者 O(n·k) 且实现繁琐，故弃用（见第 5 章）。

**2. 题库增量更新流程**

`GET /api/index` → 与本地 banks.version 对比 → 有差异的库列出（P3 弹窗：新增 X 题）→ 用户确认 → 逐库 `GET /api/banks/{id}` → 校验（id 唯一、answers 合法、type 可识别）→ 事务内：删旧插新、更新 lastSyncAt。「增量」语义为库级（只下载有变化的库）；库内全量替换。

断点处理：题库更新后该库进度直接重置（nextIndex=0、doneCount=0），题库详情页回到从头开始；不做题目 id 映射对齐，避免题库演进时的进度迁移复杂度。

**3. 错题计数状态机（计数变化仅发生在重考场景）**

- 任何做题场景（单库/混合/重考）答错且该题不在错题集：入错题集，retryCount = 3、wrongCount = 1
- 重考场景答错（已入集）：retryCount 重置 3、wrongCount+1；重考场景答对：retryCount - 1，减到 0 删除记录（移出错题集）；重考页顶部状态条展示当前计数与本次变化（P9）
- 单库/混合场景中已入集的题答对或答错：不动计数（答错仅刷新 lastWrongAt 供排序展示）——用户拍板：计数变化仅限重考场景，普通刷题不影响错题生命周期

**4. AI 问答与笔记梳理的提示词构造**

- 题目提问：system 设定中文刷题答疑角色；user 消息拼接题干 + 选项 + 解析 + 用户问题
- 笔记梳理：全部笔记（标题+正文）拼接为 user 消息，system 要求按主题分组、去重、提炼要点输出归纳稿；结果作为 source=归纳稿 的笔记入库

### 4.4 方案优劣分析

**优点**：全链路复用已验证技术组合（Kotlin/Compose/Room/OkHttp），零新环境；五表数据模型简单；后端无状态易部署；离线优先契合碎片化场景；加权抽题、错题计数、更新替换均为小算法低风险。

**局限**：笔记备份全量覆盖（若未来多设备会产生覆盖冲突，当前单机自用无影响）；题库更新后断点重置（需重刷整库，自用可接受）；llmKey 明文存储（自用接受）；库内更新非题目级增量（百题级文件无实际影响）。以上均为有意接受的取舍，触发条件变化时再演进。

## 5. 备选方案 (Alternatives Considered)

- 技术栈选型（2026-09-29 已决策）：对比了原生 Kotlin/Compose、Flutter、React Native；因环境零新增、卓易通部署链路与崩溃定位机制已在 StoryTeller 验证、自用单机场景无跨平台需求，选定 Kotlin + Compose
- 加权抽题算法：对比 Efraimidis-Spirakis（key=ln(u)/w 取 top-N，O(n log n)）与逐题轮盘赌删除法（O(n·k)，需循环改权重）；数学等价，前者代码短且无需维护权重副本，选定 ES 算法
- 笔记备份协议：对比逐条 POST（真进度、可续传）与全量单次 POST（幂等、实现最简）；笔记量 <1MB 逐条无实际收益，选定全量覆盖，页面进度改为上传中状态
- 库内更新策略：对比题目级 diff（需稳定 id 对齐 + 增量协议）与全量替换（下载后事务内整体换）；题库文件 <1MB，选定全量替换；更新后断点直接重置（对比题目 id 映射对齐方案，重置最简且语义清晰）
- AI 回复形态：对比流式 SSE 与非流式；用户拍板直接做流式（体验优先），接受两套协议（OpenAI/Anthropic）的 SSE 解析实现成本
- 错题计数规则：对比「仅重考场景变化」与「任何场景答对均减」；用户拍板仅重考场景（答对减 1、答错重置 3），单库/混合场景不动计数
- AI 回复流式化后，笔记梳理（长输出）同样受益；归纳稿生成过程可见进度

## 6. 业界调研 (Industry Research)

> **注意**：本章节应在完成自主设计后填写，用于验证方案、确保下限，而非作为设计的起点。

### 6.1 业界方案

- 业界其他系统如何解决类似问题？

### 6.2 对比分析

- 我们的方案与业界方案有何异同？

## 7. 测试计划 (Test Plan)

### 7.1 单元测试

### 7.2 集成测试

### 7.3 性能测试（如适用）

## 8. 可观测性 & 运维 (Observability & Operations)

### 8.1 可观测性

- **日志 (Logging)**: 新增的日志输出点、日志级别、关键日志格式
- **崩溃捕获**: Application 注册 UncaughtExceptionHandler，崩溃栈写 `filesDir/crash_last.txt`，下次启动弹全屏弹窗展示（展示后删除）

### 8.2 配置参数 (Configuration)

| 参数名 | 类型 | 默认值 | 说明 | 是否支持动态修改 |
|--------|------|--------|------|------------------|
| 服务器地址 | string | 无 | 题库与笔记接口的服务器地址 | 是 |
| 模型 url | string | 无 | 大模型 API 地址 | 是 |
| 模型 apiKey | string | 无 | 大模型密钥（本地存储，不上传） | 是 |
| 模型名称 | string | 无 | 大模型型号名 | 是 |
| 接口类型 | enum | openai | openai / anthropic | 是 |

### 8.3 运维接口 (Operations Interfaces)

- 服务器端（Python）提供：题库 JSON 分发接口、笔记上传接口（形态待设计阶段细化）

### 8.4 运维注意事项 (Operations Considerations)

- **升级兼容性**: keystore 丢失 = 无法覆盖升级，第一天即生成并妥善保管
- **版本号纪律**: versionCode 单调递增，装机后必查设置页版本号
- **回滚方案**: Room 实体变更必须 version+1 + Migration

## 9. Changelog

| 日期 | 变更内容 | 作者 |
|------|----------|------|
| 2026-09-29 | 创建 spec，填写背景/目标/需求三章（Draft） | yangming |
| 2026-09-29 | 同步页面设计稿拍板决策：深色模式、四 Tab 导航、混合卷来源标注、不做搜索及触发条件 | yangming |
| 2026-09-29 | 完成第 4 章系统设计（架构分层、服务器接口、题库 JSON schema、Room 五表、加权抽题/更新/错题状态机）与第 5 章备选方案 | yangming |
| 2026-09-29 | 用户拍板修订：AI 改流式 SSE、错题计数仅重考场景变化、题库更新后断点重置；其余两项维持（全量备份、weight 1-5） | yangming |

## 10. 参考资料 (References)

- 交接文档：`agent_scripts/tmp/handoff-drillbit-app.md`
- 安卓开发通用经验：`agent_docs/安卓开发通用经验.md`
- StoryTeller 工程骨架：`/Users/yangming/pycharm-workspace/StoryTeller`
- 页面设计完整方案：`agent_docs/页面设计完整方案.html`（19 页面双主题，含需求覆盖对照表与已拍板设计决定）
