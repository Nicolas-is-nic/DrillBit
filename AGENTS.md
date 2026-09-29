# DrillBit 项目知识库

**生成日期**: 2026-09-29

## 概览

项目：**DrillBit** — 自用安卓刷题 App（通用刷题工具，不绑定主题），配套 Python 后端分发题库。

- 用途：碎片化时间刷选择题强化记忆（背景：用户转行大模型 Agent 方向，非科班）
- 形态：安卓 App（主体）+ 个人服务器 Python 后端（题库分发、笔记备份）
- 状态：需求已澄清（spec.md 前三章完成），设计与编码未开始
- 定位：自用，无上架/账号体系/统计图表/云同步/复习算法（红线，见 spec 2.1 非目标）

## 技术栈（已决策，2026-09-29）

**App 端**：原生 Kotlin + Jetpack Compose，复用 StoryTeller 工程骨架（`/Users/yangming/pycharm-workspace/StoryTeller`）。

验证可用的版本组合（勿随意升级）：
- AGP 8.5.2 / Gradle 8.9 wrapper / Kotlin 2.0.21
- Room 2.6.1 (KSP) / minSdk 28 / targetSdk 35
- JDK 17：`/opt/homebrew/opt/openjdk@17`（gradle.properties 配 org.gradle.java.home）
- Android SDK：`~/Library/Android/sdk`（local.properties 配 sdk.dir）

需新增 Gradle 依赖（无系统级环境安装）：HTTP 客户端（题库拉取、大模型 API、笔记上传）、JSON 解析库。无音频需求，不引 Media3。

**后端**：Python，倾向 FastAPI（题库 JSON 分发 + 笔记上传，公网 + 简单 token 鉴权）。

## 目录结构

- `docs/design-docs/drillbit-app/spec.md`: 需求文档（背景/目标/需求已完成，第 4 章起为待填模板）
- `agent_docs/安卓开发通用经验.md`: 跨项目安卓开发经验（环境、部署、架构模式、技术红线），必读
- `agent_scripts/tmp/handoff-drillbit-app.md`: 项目启动交接文档（历史背景与决策记录）
- `agent_docs/`、`agent_scripts/`: 已被 .gitignore 排除，不进 git

## 常用命令（App 工程建立后）

| 用途 | 命令 |
|------|------|
| 快速语法检查 | `./gradlew compileReleaseKotlin` |
| 完整构建出包 | `./gradlew assembleRelease` |
| 打开产物目录 | `open app/build/outputs/apk/release/` |

## 部署与调试链路（核心约束）

目标手机为华为鸿蒙（卓易通容器）：**无 adb、无 logcat**。部署循环：

```
Mac 构建 → Finder 打开产物 → 微信/隔空投送传手机 → 卓易通安装
→ 用户实测（核对设置页版本号）→ 截图/描述反馈 → 修复
```

崩溃定位：Application 注册 UncaughtExceptionHandler 写 `filesDir/crash_last.txt`，下次启动弹全屏弹窗展示（展示后删除）。

## 工程建立第一天必做

1. 从 StoryTeller 拷工程骨架（build.gradle.kts、gradle/、gradle.properties、local.properties 等），改 applicationId 与项目名
2. keystore 签名：`keytool -genkeypair` + keystore.properties + .gitignore 排除。**keystore 丢失 = 无法覆盖升级**
3. 崩溃捕获装上（见上）
4. 设置页显示版本号；versionCode 单调递增

## 编码规范与红线

- 代码注释与日志仅中文，禁 emoji
- 完整技术红线见 `agent_docs/安卓开发通用经验.md` 第四、六节，要点：
  - 网络请求 + 读响应体全程 `Dispatchers.IO`；文件系统访问一律 IO 线程
  - Room 实体变更必须 version+1 + Migration
  - 安卓禁明文 http，服务器地址优先 https
  - 文件写入 tmp + rename 原子化
  - 一个 State 字段只承担一种语义
  - UI：色值表单一来源（ui/theme/Color.kt），文案全中文

## 当前阶段与下一步

- 已完成：命名决策、技术栈选型（Kotlin + Compose）、需求澄清（spec.md 前三章）
- 待办：抽题权重设计、服务器接口与题库 JSON schema 设计、数据模型与页面结构设计（spec 第 4 章起）、页面设计（用户进行中，产出将放 agent_docs/）
- 设计完成后进入编码：走 workflow-code-generation skill 流程
