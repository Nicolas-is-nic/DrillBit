# DrillBit 项目知识库

**生成日期**: 2026-09-29
**最近更新**: 2026-10-08（账号云同步上线 v0.1.0(25)，算法回忆卡题型设计定稿）

## 概览

项目：**DrillBit** — 自用安卓刷题 App（通用刷题工具，不绑定主题），配套 Python 后端分发题库。

- 用途：碎片化时间刷选择题强化记忆，算法面试的思路回忆复习（recall 题型设计中，背景：用户转行大模型 Agent 方向，非科班）
- 形态：安卓 App（主体）+ 个人服务器 Python 后端（题库分发、笔记备份、账号云同步）+ iOS 版（独立仓库，SwiftUI 对齐开发）
- 状态：主体功能全部上线（三题型刷题 / 错题 / 收藏 / 笔记 / AI 问答 / 云同步），最新出包 v0.1.0(25)；后端已部署上线；下一批次为算法回忆卡（recall 题型）
- 定位：自用，无上架、无公开注册、无统计图表（红线，见 spec 2.1 非目标）。注意：spec 2.1 原列的「云同步/账号体系」非目标已于 2026-10-07 破例实现（自建单用户账号 + 全量快照多设备同步，方案见 agent_docs/账号与云同步方案.md）

## 技术栈（已决策，2026-09-29）

**App 端**：原生 Kotlin + Jetpack Compose，复用 StoryTeller 工程骨架（`/Users/yangming/pycharm-workspace/StoryTeller`）。

验证可用的版本组合（勿随意升级）：
- AGP 8.5.2 / Gradle 8.9 wrapper / Kotlin 2.0.21
- Room 2.6.1 (KSP) / minSdk 28 / targetSdk 35
- JDK 17：`/opt/homebrew/opt/openjdk@17`（gradle.properties 配 org.gradle.java.home）
- Android SDK：`~/Library/Android/sdk`（local.properties 配 sdk.dir）

已引依赖（版本目录 gradle/libs.versions.toml 单一来源）：OkHttp 4.12（网络/SSE 流式）、org.json（JSON，系统自带）、markdown-renderer-m3 0.28（笔记/AI 回答渲染）、material-icons-extended。待引：Coil（recall 批次题图加载，需用户确认）。

**后端**：Python FastAPI 单文件（`server/main.py`），已部署到用户个人服务器（start.sh 常驻非 systemd + https 反代 + 双轨鉴权[静态 token 或账号 token] + SQLite 账号与快照存储；实际部署目录与启动方式以 `agent_docs/DrillBit服务器部署指南.md` 顶部核实说明为准）。

## 目录结构

- `docs/design-docs/drillbit-app/spec.md`: 需求与系统设计（1-5 章完成：背景/目标/需求/架构/备选方案）
- `docs/design-docs/drillbit-app/tasks.md`: 实施任务清单（阶段 0 已完成，阶段 2 待建）
- `server/main.py` + `server/banks/`: 后端服务与题库文件目录（六个真实题库 JSON 已上线，部署在服务器，仓库内存源码）
- `agent_docs/DrillBit服务器部署指南.md`: 服务器部署/运维指南（题库更新流程、token 轮换、排障）
- `app/src/main/assets/test_banks.json`: 本地测试题库（未配置服务器时点同步即导入，开发期路径）
- `agent_docs/题库JSON格式规范.md`: 题库 JSON 出品规范（三题型示例、校验规则、自检清单，可整份喂给出题大模型；recall 题型章节待补）
- `agent_docs/题库更新操作手册.md`: 服务器题库日常更新 runbook（ssh/scp 流程、三场景、排障，含实际连接与路径）
- `agent_docs/双模型分工开发方案.md`: 双模型协作唯一契约源（路由/色值/组件/UiState/Event，阶段 1 交付物）
- `agent_docs/页面设计完整方案.html`: 视觉唯一基准（22 屏，已按阶段 1 实现回写定稿）
- `agent_docs/账号与云同步方案.md`: 账号与云同步唯一方案源（快照五类数据字段为 iOS 对齐基准）
- `agent_docs/算法回忆卡UI方案.html`: recall 题型交互稿 v4（默想/揭示/自评三阶段、题图、代码 segmented）
- `agent_docs/算法回忆卡实现方案.md`: recall 题型实现方案（schema/契约变更/图片同步/菜单方案，待拍板两项）
- `agent_docs/iOS迁移完整方案.md`: iOS 版方案（独立仓库）
- `agent_docs/阶段1-UI交接说明与遗留问题.md`: UI 模型交接文档（契约缺口、工程坑、阶段 2 接线清单）
- `agent_docs/安卓开发通用经验.md`: 跨项目安卓开发经验（环境、部署、架构模式、技术红线），必读
- `agent_scripts/tmp/handoff-drillbit-app.md`: 项目启动交接文档（历史背景与决策记录）
- `agent_docs/`、`agent_scripts/`、`docs/`: 已被 .gitignore 排除，不进 git（2026-09-30 起 docs 从远端仓库移除，纯本地维护）

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

- 已完成批次（按时间）：多选/判断题型与统一确认作答（v18）→ AI 多轮上下文与按消息保存、删题收藏与我的收藏题集、Room v2（v24）→ 账号登录与多设备云同步（v25，2026-10-08 提交）：快照五类数据（notes/favorites/deletedQuestions/progress/wrongs）上传下载、冷启动静默拉取、云端新快照覆盖警告、服务端 SQLite 账号与双轨鉴权
- 后端：FastAPI 已部署上线（题库分发 + 笔记备份 + 账号云同步 + https）
- 当前批次：算法回忆卡（recall 题型）——2026-10-08 已拍板（菜单方案甲：错题 Tab 改名「复习」图标 Replay；Coil 2.7.0 引入）并完成开发：契约 7.4 扩展（RECALL/RecallUi/RememberedClick/ForgotClick）、Room v3（questions.recallJson）、题图随库同步落盘 filesDir/img/{bankId}/、服务器 /api/img 端点、刷题页 recall 三阶段渲染（默想/揭示/自评/代码卡 segmented/全屏缩放题图）、AI 上下文 recall 分支、本地测试库 v3 与服务器 algo-stack 库（各含 2 道 recall 题）；出包 versionCode 26 待真机验收
- 日常维护：服务器上改题库 JSON（version+1 即生效）、App 内同步、出题攒笔记、多设备间「立即同步」
- keystore 与 keystore.properties 不入 git（用户自行备份），丢失 = 无法覆盖升级
- 后续迭代：判断题双列大按钮渲染（现复用单选纵向，已拍板后议）、笔记搜索（触发条件见 spec 2.1）、流式以外的体验优化按需推进
- 双模型协作机制：UI 模型只写 ui/ 层照契约实现；coding 模型实现契约另一侧（ViewModel 产 state 消费 event）；契约变更必须先改分工文档再改代码
- 出包纪律：每次改动 versionCode+1，装机后先核对设置页版本号再验收；**iOS 版（独立仓库 ~/pycharm-workspace/DrillBit-iOS，SwiftUI 原生重写，方案见 agent_docs/iOS迁移完整方案.md）版本号与安卓完全对齐**：Marketing Version=versionName、Build=versionCode，安卓为基准源，基准以 app/build.gradle.kts 代码为准
- 真机联调遗留：崩溃/异常现象优先查 crash_last.txt 弹窗与设置页「崩溃日志」入口；修复后 versionCode 递增（当前 26，recall 批次）
