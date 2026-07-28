# 追剧 APP（zhuiju）

> 一句话定位：基于 Android 原生技术栈打造的商用级视频播放应用，集长视频点播、抖音式短视频、本地播放、弹幕互动于一体的全场景追剧体验。

![Kotlin](https://img.shields.io/badge/Kotlin-1.9+-7F52FF?logo=kotlin&logoColor=white)
![ExoPlayer](https://img.shields.io/badge/ExoPlayer-1.2.1-FF6F00?logo=android&logoColor=white)
![Android](https://img.shields.io/badge/Android-5.0%2B-green?logo=android&logoColor=white)
![FFmpeg](https://img.shields.io/badge/FFmpeg-4.4.LTS-007808?logo=ffmpeg&logoColor=white)
![Jetpack](https://img.shields.io/badge/Jetpack-MVVM-03A9F4?logo=android&logoColor=white)
![Version](https://img.shields.io/badge/Version-1.0.0-brightgreen)
![License](https://img.shields.io/badge/License-MIT-blue)

---

## 目录

- [核心特性](#核心特性)
- [截图与演示](#截图与演示)
- [技术栈](#技术栈)
- [项目架构](#项目架构)
- [功能模块](#功能模块)
- [快速开始](#快速开始)
- [项目结构](#项目结构)
- [开发文档索引](#开发文档索引)
- [开发流程](#开发流程)
- [编码规范](#编码规范)
- [测试](#测试)
- [版本管理](#版本管理)
- [常见问题](#常见问题)
- [贡献指南](#贡献指南)
- [许可证](#许可证)
- [联系方式](#联系方式)

---

## 核心特性

- **长视频点播**：影视剧、综艺、动漫等内容点播，支持多清晰度切换、剧集连播、进度记忆。
- **抖音式短视频**：上下滑动的沉浸式短视频 Feed 流，预加载与无缝切换，竖屏全屏体验。
- **本地视频播放**：扫描并播放本地视频文件，支持外挂字幕、音轨切换。
- **实时弹幕互动**：基于 DanmakuFlameMaster 的高性能弹幕引擎，支持滚动/顶部/底部弹幕、屏蔽与样式自定义。
- **离线缓存**：视频分段缓存与下载，断点续传，缓存管理，弱网可用。
- **视频加密**：AES-128-CBC 加密保护视频内容，防盗播，保障内容安全。
- **手势控制**：亮度、音量、进度调节手势，双击点赞，长按倍速。
- **横竖屏自适应**：根据内容类型自动切换横竖屏，传感器与手动锁定结合。
- **五大主页**：首页（短视频 Feed）、发现页、排行榜、找片、我的，覆盖完整消费场景。
- **MVVM + 协程**：现代 Android 架构，Flow 响应式数据流，Repository 分层解耦。
- **FFmpeg 编解码**：硬解软解智能切换，支持多种封装/编码格式，转码与处理能力。

---

## 截图与演示

> 以下为截图与演示占位，正式发布前补充实际素材。

| 首页（短视频 Feed） | 发现页 | 排行榜 |
| :---: | :---: | :---: |
| ![首页截图占位](docs/screenshots/home_placeholder.png) | ![发现页截图占位](docs/screenshots/discover_placeholder.png) | ![排行榜截图占位](docs/screenshots/rank_placeholder.png) |

| 找片 | 我的 | 播放器（弹幕+手势） |
| :---: | :---: | :---: |
| ![找片截图占位](docs/screenshots/search_placeholder.png) | ![我的截图占位](docs/screenshots/mine_placeholder.png) | ![播放器截图占位](docs/screenshots/player_placeholder.png) |

- **演示视频**：`docs/demo/demo.mp4`（占位）
- **APK 下载**：见 [Releases](#) 页面（占位）

---

## 技术栈

| 技术 | 版本 | 用途 |
| :--- | :--- | :--- |
| Kotlin | 1.9+ | 主开发语言，协程 + Flow |
| Android Jetpack (Lifecycle/ViewModel) | 2.6+ | 生命周期管理，UI 状态持有 |
| Android Jetpack (Navigation) | 2.6+ | 单 Activity 多 Fragment 导航 |
| Android Jetpack (DataBinding) | 8.0+ | 视图与数据绑定 |
| Kotlin Coroutines / Flow | 1.7+ | 异步与响应式数据流 |
| ExoPlayer | 2.19+ | 核心视频播放器 |
| FFmpeg | 6.x | 视频编解码、转码、处理 |
| DanmakuFlameMaster | 0.9+ | 弹幕渲染引擎 |
| OkHttp | 4.12+ | 网络请求 |
| Glide / Coil | 最新稳定 | 图片加载 |
| Room | 2.6+ | 本地数据库（缓存/历史） |
| AES-128-CBC | - | 视频内容加密 |
| Material Components | 1.10+ | UI 组件库 |
| Gradle | 8.0+ | 构建工具 |

---

## 项目架构

本项目采用 **MVVM + 协程 + Flow + Repository 分层架构**，单一 Activity + Navigation 管理页面，分层解耦、单向数据流。

```
┌─────────────────────────────────────────────────────────┐
│                       UI Layer                           │
│   Activity / Fragment (DataBinding + ViewModel 观察者)   │
└──────────────────────────┬──────────────────────────────┘
                           │ 持有 / 观察
┌──────────────────────────▼──────────────────────────────┐
│                    ViewModel Layer                       │
│   状态管理 (StateFlow) + 业务逻辑编排 + 协程作用域        │
└──────────────────────────┬──────────────────────────────┘
                           │ 调用
┌──────────────────────────▼──────────────────────────────┐
│                   Repository Layer                       │
│   数据聚合：远程数据源 + 本地数据源 + 缓存策略            │
└────────────┬─────────────────────────┬──────────────────┘
             │                         │
┌────────────▼───────────┐  ┌──────────▼─────────────────┐
│   Remote DataSource    │  │     Local DataSource        │
│ OkHttp / Retrofit / WS │  │ Room / DataStore / FileCache│
└────────────────────────┘  └─────────────────────────────┘
```

**分层职责：**

- **UI Layer**：Activity/Fragment 仅负责视图渲染与用户交互，通过 DataBinding 绑定 ViewModel 状态，零业务逻辑。
- **ViewModel Layer**：持有 UI 状态（`StateFlow`/`SharedFlow`），处理业务编排，通过协程调度 IO/Default/Main，配置变化时保留状态。
- **Repository Layer**：数据统一入口，聚合远程与本地数据源，封装缓存策略（网络优先/缓存优先/仅本地），对上层屏蔽数据来源。
- **DataSource Layer**：远程走 OkHttp，本地走 Room/DataStore/文件缓存，各自独立可替换。

**数据流方向**：UI → ViewModel → Repository → DataSource（单向），数据通过 Flow 自下而上回传。

---

## 功能模块

### 五大主页

| 模块 | 说明 |
| :--- | :--- |
| **首页** | 短视频 Feed 流，上下滑动沉浸式浏览，预加载与无缝切换。 |
| **发现页** | 内容分类、推荐、专题、榜单入口，发现感兴趣的内容。 |
| **排行榜** | 各分类热度榜、飙升榜、新片榜，实时数据更新。 |
| **找片** | 多维度筛选（类型/地区/年份/排序）、关键词搜索、搜索历史。 |
| **我的** | 用户信息、观看历史、收藏、缓存管理、设置。 |

### 播放器模块

- 基于 ExoPlayer 的核心播放器，支持 HLS/MP4/MKV/DASH 等格式。
- 自定义播放控制层：播放/暂停、进度条、倍速、清晰度、音轨/字幕切换。
- 手势控制：左右滑进度、左半屏滑亮度、右半屏滑音量、双击点赞、长按倍速。
- 横竖屏自适应 + 屏幕方向锁定。

### 缓存模块

- 视频分段缓存，边播边缓存，二次播放秒开。
- 离线下载，支持队列管理、断点续传、网络策略（仅 WiFi）。
- 缓存清理与容量管理。

### 加密模块

- AES-128-CBC 对视频内容加密，密钥动态下发。
- 播放时实时解密，防盗链防盗播。
- 本地缓存同样加密存储。

### 弹幕模块

- DanmakuFlameMaster 引擎，高性能渲染。
- 支持滚动/顶部/底部弹幕，字体颜色/大小自定义。
- 弹幕屏蔽（关键词/用户/类型），发送弹幕互动。

---

## 快速开始

### 环境要求

- **JDK**：17 及以上
- **Android Studio**：Hedgehog (2023.1.1) 或更高版本
- **Android SDK**：Compile SDK 34，Min SDK 21（Android 5.0），Target SDK 34
- **Gradle**：8.0+（项目自带 Gradle Wrapper）
- **Kotlin**：1.9+
- **NDK**：如需本地编译 FFmpeg，配置 NDK r25c+（默认已内置预编译库）

### 克隆项目

```bash
git clone https://github.com/your-org/zhuiju.git
cd zhuiju
```

### 依赖配置

1. 在 `local.properties` 中配置 SDK 路径（首次构建会自动生成）：

```properties
sdk.dir=F\:\\Android\\Sdk
```

2. 如需接入后端服务，在 `app/build.gradle` 中配置 API 地址与密钥（占位）：

```gradle
buildConfigField "String", "BASE_URL", "\"https://api.example.com/\""
buildConfigField "String", "AES_KEY", "\"your-aes-key\""
```

### 构建运行

**Debug 构建（开发调试）：**

```bash
# Windows
gradlew.bat assembleDebug

# macOS / Linux
./gradlew assembleDebug
```

安装到已连接设备：

```bash
gradlew.bat installDebug
```

**Release 构建（正式包，需配置签名）：**

```bash
gradlew.bat assembleRelease
```

**清理与重新构建：**

```bash
gradlew.bat clean
gradlew.bat build
```

**运行单元测试：**

```bash
gradlew.bat test
```

> 提示：首次构建会下载依赖，请保持网络畅通。如遇 FFmpeg/ExoPlayer 依赖拉取失败，可配置国内镜像源（如阿里云 Maven 镜像）。

---

## 项目结构

```
zhuiju/
├── app/                            # 应用主模块
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/zhuiju/app/
│   │   │   │   ├── base/           # 基类：BaseActivity / BaseFragment / BaseViewModel
│   │   │   │   ├── data/           # 数据层
│   │   │   │   │   ├── repository/ # Repository 实现
│   │   │   │   │   ├── remote/     # 远程数据源（OkHttp / API）
│   │   │   │   │   ├── local/      # 本地数据源（Room / DataStore / Cache）
│   │   │   │   │   └── model/      # 数据模型
│   │   │   │   ├── ui/             # UI 层
│   │   │   │   │   ├── home/       # 首页（短视频 Feed）
│   │   │   │   │   ├── discover/   # 发现页
│   │   │   │   │   ├── rank/       # 排行榜
│   │   │   │   │   ├── search/     # 找片
│   │   │   │   │   ├── mine/       # 我的
│   │   │   │   │   └── player/     # 播放器（长视频/短视频/本地）
│   │   │   │   ├── player/         # 播放器核心封装（ExoPlayer + 手势 + 弹幕）
│   │   │   │   ├── danmaku/        # 弹幕引擎封装
│   │   │   │   ├── cache/          # 缓存与下载
│   │   │   │   ├── crypto/         # AES 加解密
│   │   │   │   ├── network/        # 网络配置与拦截器
│   │   │   │   ├── utils/          # 工具类
│   │   │   │   └── App.kt          # Application 入口
│   │   │   ├── res/                # 资源（布局/图片/字符串/样式）
│   │   │   └── AndroidManifest.xml
│   │   ├── test/                   # 单元测试
│   │   └── androidTest/            # 插桩测试
│   └── build.gradle
├── docs/                           # 开发文档（详见下方索引）
├── build.gradle                    # 项目级构建脚本
├── settings.gradle
├── gradle.properties
├── gradlew / gradlew.bat
└── README.md
```

> 注：以上为推荐结构占位，实际目录以代码库为准。

---

## 开发文档索引

项目全套开发文档存放于 `docs/` 目录，以下为各文档说明与链接：

- [开发任务文档](docs/开发任务文档.md) — 项目分阶段开发任务清单与进度跟踪。
- [开发记录文档](docs/开发记录文档.md) — 日常开发日志、决策记录与关键问题追溯。
- [软件使用说明](docs/软件使用说明.md) — APP 功能操作指引与使用流程说明。
- [布局说明文档](docs/布局说明文档.md) — 各页面布局方案、控件层级与设计规范。
- [用户使用说明](docs/用户使用说明.md) — 面向终端用户的使用手册与常见操作指引。
- [项目技术文档](docs/项目技术文档.md) — 技术选型、架构设计、核心模块实现与编码规范。
- [Bug 调试记录](docs/Bug调试记录.md) — 历史问题复现、定位与修复记录。
- [测试记录](docs/测试记录.md) — 测试用例、测试结果与质量报告。
- [新增功能记录](docs/新增功能记录.md) — 版本迭代新增功能清单与变更说明。

---

## 开发流程

项目开发分 **5 个阶段**推进，详细任务与里程碑见 [开发任务文档](docs/开发任务文档.md)。

| 阶段 | 内容 | 说明 |
| :---: | :--- | :--- |
| **阶段一** | 项目初始化与基础架构 | 工程搭建、依赖配置、MVVM 骨架、网络与数据层基础。 |
| **阶段二** | 播放器核心与长视频 | ExoPlayer 封装、播放控制、手势、横竖屏、缓存、弹幕接入。 |
| **阶段三** | 短视频与首页 Feed | 抖音式短视频、上下滑动、预加载、首页 Feed 流。 |
| **阶段四** | 五大主页与业务完善 | 发现页、排行榜、找片、我的，搜索、收藏、历史等功能。 |
| **阶段五** | 加密、优化与发布 | AES 加密、性能优化、内存与弱网优化、测试与打包发布。 |

各阶段任务拆解、验收标准与开发记录详见上述文档。

---

## 编码规范

- **语言**：统一使用 Kotlin，禁止 Java/Kotlin 混用新增代码。
- **命名**：类名 PascalCase，方法/变量 camelCase，常量 UPPER_SNAKE_CASE，包名全小写。
- **架构**：严格遵循 MVVM 分层，UI 层不直接访问数据层，禁止在 Fragment/Activity 写业务逻辑。
- **异步**：协程 + Flow，禁止使用 `Thread`/`AsyncTask`/`RxJava`。
- **资源**：字符串/颜色/尺寸统一入 `res/values`，禁止硬编码。
- **注释**：公开 API 使用 KDoc 注释，复杂逻辑需说明 why 而非 what。

详细规范见 [项目技术文档](docs/项目技术文档.md)。

---

## 测试

项目测试覆盖单元测试与插桩测试：

- **单元测试**：Repository、ViewModel、工具类的逻辑测试，运行于 JVM。
- **UI 测试**：关键页面交互与播放器行为测试，运行于设备/模拟器。
- **兼容性测试**：覆盖主流机型与 Android 版本。
- **性能测试**：播放流畅度、内存占用、弱网表现。

测试用例与结果详见 [测试记录](docs/测试记录.md)，问题复现与修复见 [Bug 调试记录](docs/Bug调试记录.md)。

```bash
# 运行单元测试
gradlew.bat test

# 运行插桩测试
gradlew.bat connectedAndroidTest
```

---

## 版本管理

项目采用语义化版本（SemVer）：`MAJOR.MINOR.PATCH`。

- **MAJOR**：不兼容的 API 变更。
- **MINOR**：向后兼容的功能新增。
- **PATCH**：向后兼容的问题修复。

各版本新增功能与变更详见 [新增功能记录](docs/新增功能记录.md)，开发日志见 [开发记录文档](docs/开发记录文档.md)。

---

## 常见问题

**Q1：首次构建拉取依赖失败？**
A：检查网络，建议配置国内 Maven 镜像（阿里云/华为云）。在 `settings.gradle` 的 `pluginManagement` 与 `dependencyResolutionManagement` 中添加镜像仓库地址。

**Q2：FFmpeg 相关库编译报错？**
A：项目默认内置预编译 FFmpeg 库。如需自行编译，确保 NDK 版本为 r25c+，并按 `项目技术文档` 配置编译脚本。

**Q3：播放器无法播放加密视频？**
A：检查 `AES_KEY` 配置是否正确，确认密钥与后端下发一致；查看日志中解密相关错误信息。

**Q4：短视频 Feed 滑动卡顿？**
A：确认预加载数量配置合理，检查缓存策略；低端机建议减少预加载个数，详见性能优化章节。

**Q5：弹幕不显示或卡顿？**
A：检查弹幕数据格式是否符合 DanmakuFlameMaster 规范，确认弹幕渲染未阻塞主线程；大量弹幕时开启防碰撞与合并策略。

更多问题见 [Bug 调试记录](docs/Bug调试记录.md)。

---

## 贡献指南

欢迎参与项目贡献，请遵循以下规范。

### 代码规范

- 严格遵守 [编码规范](#编码规范) 与 [项目技术文档](docs/项目技术文档.md)。
- 新增功能需配套单元测试，保证测试通过。
- 提交前运行 `gradlew.bat lint` 与 `gradlew.bat test` 确保无报错。

### 提交规范

采用 Conventional Commits 规范：

```
<type>(<scope>): <subject>

<body>

<footer>
```

**type 类型：**

| type | 说明 |
| :--- | :--- |
| `feat` | 新功能 |
| `fix` | 修复 Bug |
| `docs` | 文档变更 |
| `style` | 代码格式（不影响功能） |
| `refactor` | 重构（非新增/修复） |
| `perf` | 性能优化 |
| `test` | 测试相关 |
| `chore` | 构建/工具/依赖变更 |

示例：

```
feat(player): 支持倍速播放手势长按触发
fix(cache): 修复断点续传进度计算错误
docs(readme): 更新项目说明文档
```

### PR 流程

1. Fork 本仓库并拉取到本地。
2. 基于 `develop` 分支创建特性分支：`git checkout -b feat/your-feature`。
3. 完成开发并提交，确保 commit 信息符合上述规范。
4. 推送分支并创建 Pull Request 至 `develop`，填写变更说明。
5. 通过 Code Review 与 CI 检查后合并。

---

## 许可证

本项目采用 [MIT License](LICENSE)（占位，正式发布前补充许可证文件）。

---

## 联系方式

- **项目维护者**：待补充
- **邮箱**：待补充
- **问题反馈**：请通过 [Issues](#) 提交（占位）
- **官方文档**：见 [开发文档索引](#开发文档索引)

---

> 本 README 随项目迭代持续更新，如发现内容与代码不符，欢迎提 Issue 或 PR 修正。
