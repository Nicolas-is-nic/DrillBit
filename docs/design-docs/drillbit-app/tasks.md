# 实施任务清单（阶段 0：工程骨架）

> 由 spec.md 与《双模型分工开发方案.md》第三节生成
> 任务总数: 4
> 核心原则: 骨架先行——拷贝已验证的 StoryTeller 工程配置，改造标识后建最小可编译壳，交给 UI 模型

## 依赖关系总览

```
Task 1 (Gradle 工程骨架拷贝与标识改造)
  ↓
Task 2 (app 模块壳：Manifest/res/崩溃捕获/导航/主题)
  ↓
Task 3 (keystore 签名生成与接线 + .gitignore)
  ↓
Task 4 (编译与出包验证)
```

## 变更影响概览

### 文件变更清单

| 文件 | 操作 | 涉及任务 | 说明 |
|------|------|---------|------|
| `settings.gradle.kts` | 新建(拷贝改造) | Task 1 | rootProject.name = DrillBit |
| `build.gradle.kts` / `gradle.properties` / `local.properties` / `gradlew*` / `gradle/` | 新建(拷贝) | Task 1 | 原样拷贝，libs.versions.toml 去掉 media3/jsoup |
| `app/build.gradle.kts` | 新建(拷贝改造) | Task 2, 3 | namespace=com.drillbit，v0.1.0(1)，依赖精简 |
| `app/src/main/AndroidManifest.xml` | 新建 | Task 2 | 仅 INTERNET 权限 |
| `app/src/main/res/**` | 新建(拷贝改造) | Task 2 | themes/strings/图标/霞鹜文楷字体 |
| `app/src/main/java/com/drillbit/DrillBitApplication.kt` | 新建 | Task 2 | 崩溃捕获（照 StoryTeller 方案） |
| `app/src/main/java/com/drillbit/MainActivity.kt` | 新建 | Task 2 | 崩溃弹窗 + 导航壳 + 四 Tab |
| `app/src/main/java/com/drillbit/ui/theme/*.kt` | 新建 | Task 2 | 双主题色板（分工方案第五节 15 变量） |
| `app/src/main/java/com/drillbit/ui/components/BottomNavBar.kt` | 新建 | Task 2 | 四 Tab 空壳 |
| `app/src/main/java/com/drillbit/ui/Placeholder.kt` | 新建 | Task 2 | 13 路由占位页 |
| `drillbit.keystore` + `keystore.properties` | 生成 | Task 3 | 不入 git |
| `.gitignore` | 修改 | Task 3 | 追加安卓条目 |

### 受影响接口

全新工程，无既有接口。

### 构建系统变更

- 全新 Gradle 工程（AGP 8.5.2 / Gradle 8.9 / Kotlin 2.0.21，与 StoryTeller 一致）
- 依赖：去掉 Media3、jsoup；保留 Compose/Room/OkHttp/DataStore/Navigation（版本已验证）

## 风险与假设

| # | 描述 | 影响任务 | 假设/处理 |
|---|------|---------|----------|
| 1 | 标题衬线字体在鸿蒙卓易通上表现未知 | Task 2 | 拷贝 StoryTeller 已验证的霞鹜文楷作为标题字体，不用系统 serif |
| 2 | keystore 密码来源 | Task 3 | 随机生成强密码写入 keystore.properties（不入 git），告知用户妥善备份 |
| 3 | 深色模式骨架阶段无持久化 | Task 2 | MainActivity 内存态持有 darkTheme，默认亮色；阶段 2 接 DataStore |

## 任务列表

### 任务 1: [x] Gradle 工程骨架拷贝与标识改造

- 文件: `settings.gradle.kts`、`build.gradle.kts`、`gradle.properties`、`local.properties`、`gradlew`、`gradlew.bat`、`gradle/`（均新建）
- 依赖: 无
- spec 映射: 分工方案第三节、spec 1.2（技术栈版本组合）
- 说明: 从 StoryTeller 拷贝工程级配置；rootProject.name 改 DrillBit；libs.versions.toml 删除 media3/jsoup 条目（含对应 library 声明）
- context:
  - `/Users/yangming/pycharm-workspace/StoryTeller/settings.gradle.kts` — 拷贝源
  - `/Users/yangming/pycharm-workspace/StoryTeller/gradle/libs.versions.toml` — 需裁剪的版本目录
- 验收标准:
  - [ ] DrillBit 根目录存在上述文件，rootProject.name = "DrillBit"
  - [ ] libs.versions.toml 无 media3/jsoup 引用（grep 无匹配）
  - [ ] gradle-wrapper.properties 为 8.9

### 任务 2: [x] app 模块壳（Manifest/res/崩溃捕获/导航/主题）

- 文件: `app/build.gradle.kts`、`app/proguard-rules.pro`、`app/src/main/AndroidManifest.xml`、`app/src/main/res/**`、`com/drillbit/DrillBitApplication.kt`、`com/drillbit/MainActivity.kt`、`com/drillbit/ui/theme/{Color,Theme,Type}.kt`、`com/drillbit/ui/components/BottomNavBar.kt`、`com/drillbit/ui/Placeholder.kt`（均新建）
- 依赖: Task 1
- spec 映射: 分工方案第三、四、五节；spec 3.2（崩溃捕获、部署）
- 说明:
  - namespace/applicationId = com.drillbit，versionCode 1 / versionName 0.1.0，buildConfig=true（阶段 2 版本号显示用）
  - 崩溃捕获照 StoryTellerApplication 方案（crash_last.txt + 启动弹窗）
  - 主题：15 个语义色变量亮暗两套（分工方案第五节色值表），DrillBitTheme(darkTheme) + CompositionLocal；标题霞鹜文楷
  - MainActivity：NavHost 注册分工方案第四节 13 条路由，全部指向 PlaceholderScreen；四 Tab（题库/错题/笔记/设置）可切换
- context:
  - StoryTeller `StoryTellerApplication.kt` — 崩溃捕获实现范本
  - StoryTeller `MainActivity.kt` — 导航与底部栏结构范本
  - StoryTeller `ui/theme/` — 主题组织方式范本
- 验收标准:
  - [ ] 13 条路由全部注册且可导航（代码检查路由字符串与分工方案第四节一致）
  - [ ] 色值与分工方案第五节逐项一致（grep 核对 hex）
  - [ ] 页面代码无字面 hex 色值（仅 theme 文件允许）

### 任务 3: [x] keystore 签名生成与接线

- 文件: `drillbit.keystore`、`keystore.properties`（生成，不入 git）、`app/build.gradle.kts`（签名引用）、`.gitignore`（追加）
- 依赖: Task 2
- spec 映射: spec 8.4（keystore 纪律）
- 说明: keytool -genkeypair 生成 RSA 2048 / 100 年有效期 / alias drillbit；随机强密码写入 keystore.properties；.gitignore 追加安卓条目（keystore、keystore.properties、local.properties、.gradle/、build 产物、.idea 等，已有 Python 条目保留）
- context:
  - StoryTeller `app/build.gradle.kts` signingConfigs 段 — 签名接线范本
- 验收标准:
  - [ ] `keytool -list` 能读取 drillbit.keystore
  - [ ] .gitignore 含 drillbit.keystore、keystore.properties、local.properties
  - [ ] git status 不显示 keystore 与 keystore.properties

### 任务 4: [x] 编译与出包验证

- 文件: 无新增（验证任务）
- 依赖: Task 3
- spec 映射: 分工方案第三节（compileReleaseKotlin 通过）
- 说明: 依次执行 compileReleaseKotlin、assembleRelease；确认产物路径
- 验收标准:
  - [ ] `./gradlew compileReleaseKotlin` 成功
  - [ ] `./gradlew assembleRelease` 成功产出签名 APK
  - [ ] app/build/outputs/apk/release/ 下存在 apk 文件

## Spec 覆盖映射

| Spec 章节 | 任务 | 说明 |
|-----------|------|------|
| 分工方案第三节（阶段 0 清单） | Task 1-4 | 全覆盖 |
| 分工方案第四节（路由契约） | Task 2 | 13 路由注册 |
| 分工方案第五节（主题契约） | Task 2 | 色板三件套 |
| spec 3.2 崩溃捕获 | Task 2 | Application + 启动弹窗 |
| spec 8.4 keystore 纪律 | Task 3 | 第一天签名 |
