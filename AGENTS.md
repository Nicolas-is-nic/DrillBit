# DrillBit 项目知识库

**生成日期**: 2026-09-29

## 概览

项目：**DrillBit** — 自用安卓刷题 App（通用刷题工具，不绑定主题），配套 Python 后端分发题库。

- 用途：碎片化时间刷选择题强化记忆（背景：用户转行大模型 Agent 方向，非科班）
- 形态：安卓 App（主体）+ 个人服务器 Python 后端（题库分发、笔记备份）
- 状态：开发全部完成（阶段 0 骨架 / 阶段 1 UI 层 / 阶段 2 数据层与业务接线，T1-T7），v0.1.0(8) 已出包；后端已部署上线；当前在真机联调验收期
- 定位：自用，无上架/账号体系/统计图表/云同步/复习算法（红线，见 spec 2.1 非目标）

## 技术栈（已决策，2026-09-29）

**App 端**：原生 Kotlin + Jetpack Compose，复用 StoryTeller 工程骨架（`/Users/yangming/pycharm-workspace/StoryTeller`）。

验证可用的版本组合（勿随意升级）：
- AGP 8.5.2 / Gradle 8.9 wrapper / Kotlin 2.0.21
- Room 2.6.1 (KSP) / minSdk 28 / targetSdk 35
- JDK 17：`/opt/homebrew/opt/openjdk@17`（gradle.properties 配 org.gradle.java.home）
- Android SDK：`~/Library/Android/sdk`（local.properties 配 sdk.dir）

需新增 Gradle 依赖（无系统级环境安装）：HTTP 客户端（题库拉取、大模型 API、笔记上传）、JSON 解析库。无音频需求，不引 Media3。

**后端**：Python FastAPI 单文件（`server/main.py`），已部署到用户个人服务器（systemd + https 反代 + token 鉴权），部署细节见 `agent_docs/DrillBit服务器部署指南.md`。

## 目录结构

- `docs/design-docs/drillbit-app/spec.md`: 需求与系统设计（1-5 章完成：背景/目标/需求/架构/备选方案）
- `docs/design-docs/drillbit-app/tasks.md`: 实施任务清单（阶段 0 已完成，阶段 2 待建）
- `server/main.py` + `server/banks/`: 后端服务与题库文件目录（部署在服务器，仓库内存源码）
- `agent_docs/DrillBit服务器部署指南.md`: 服务器部署/运维指南（题库更新流程、token 轮换、排障）
- `app/src/main/assets/test_banks.json`: 本地测试题库（未配置服务器时点同步即导入，开发期路径）
- `agent_docs/双模型分工开发方案.md`: 双模型协作唯一契约源（路由/色值/组件/UiState/Event，阶段 1 交付物）
- `agent_docs/页面设计完整方案.html`: 视觉唯一基准（22 屏，已按阶段 1 实现回写定稿）
- `agent_docs/阶段1-UI交接说明与遗留问题.md`: UI 模型交接文档（契约缺口、工程坑、阶段 2 接线清单）
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

- 已完成：需求澄清、系统设计（spec 1-5 章）、页面设计、阶段 0 骨架、阶段 1 UI 层、阶段 2 数据层（Room/DataStore/网络/流式 AI/笔记/备份）已 push；其后一真机联调修复批次（见下条）
- 后端：FastAPI 已部署上线（题库分发 + 笔记备份 + token 鉴权 + https）
- 当前：真机联调修复批次已落地（v0.1.0(10)）：题库导入外键回滚修复（事务内先插 bank 行再插 questions，否则全新库必失败）、刷完一轮断点归零重开、从头重刷等落库后再跳转、同步/更新失败上浮提示条（契约 7.1 加 banner 字段）、深色冷启动白闪修复（values-night 双套 windowBackground + 根部全屏底色）、服务器配置保存反馈、LlmClient 加 User-Agent 与 opencode.ai 会话头、应用图标换题卡造型
- 日常维护仅三件事：服务器上改题库 JSON（version+1 即生效）、App 内同步、出题攒笔记
- keystore 与 keystore.properties 不入 git（用户自行备份），丢失 = 无法覆盖升级
- 后续迭代：多选题型（schema 已预留）、笔记搜索（触发条件见 spec 2.1）、流式以外的体验优化按需推进
- 双模型协作机制：UI 模型只写 ui/ 层照契约实现；coding 模型实现契约另一侧（ViewModel 产 state 消费 event）；契约变更必须先改分工文档再改代码
- 出包纪律：每次改动 versionCode+1，装机后先核对设置页版本号再验收
- 真机联调遗留：崩溃/异常现象优先查 crash_last.txt 弹窗与设置页「崩溃日志」入口；修复后 versionCode 递增（当前 10）
