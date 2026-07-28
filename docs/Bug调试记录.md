# 追剧APP Bug调试记录文档

> 文档版本：v1.0
> 最后更新：2026-07-29
> 文档负责人：项目开发组
> 项目代号：zhuiju

---

## 一、文档说明

### 1.1 编写目的

本文档用于规范追剧 APP（zhuiju）研发及上线全周期内的 Bug 记录、定位、修复、验证与复盘流程，目的为：

- 建立统一的 Bug 记录模板与字段标准，便于团队协作与信息对齐；
- 沉淀典型 Bug 的根因分析与修复方案，避免同类问题重复出现；
- 通过 Bug 统计看板与趋势跟踪，量化质量风险，支撑版本上线决策；
- 为新成员入职培训、Code Review、回归测试提供原始素材；
- 确保追剧 APP 在播放、弹幕、缓存、加密、横竖屏切换等核心链路无致命缺陷。

### 1.2 适用范围

| 项 | 内容 |
| --- | --- |
| 适用对象 | 追剧 APP Android 客户端（Kotlin + Jetpack + ExoPlayer + FFmpeg + DanmakuFlameMaster + OkHttp + AES-128-CBC） |
| 覆盖模块 | 长视频点播、抖音式短视频 Feed 流、本地视频、弹幕、缓存、AES 加解密、网络层、手势交互、横竖屏切换、UI 渲染、内存与性能 |
| 适用阶段 | 开发自测、SIT 集成测试、UAT 验收测试、灰度测试、上线后线上问题排查 |
| 维护规则 | Bug 发现后 2 小时内录入；P0/P1 当日跟进；修复后 24 小时内完成验证并补充复盘 |

### 1.3 Bug 分级标准

| 等级 | 名称 | 定义 | 响应时效 | 示例 |
| --- | --- | --- | --- | --- |
| P0 | 致命 | 主流程完全不可用、崩溃、ANR、数据损坏、安全漏洞 | 立即响应，4 小时内修复 | 播放器崩溃、AES 解密失败导致无法播放、内存泄漏引发 OOM |
| P1 | 严重 | 核心功能受损但有规避路径、明显体验劣化 | 24 小时内修复 | 黑屏、花屏、弹幕严重不同步、缓存断点续传失效 |
| P2 | 一般 | 非核心功能异常、偶现问题、特定机型复现 | 本迭代内修复 | 低端机掉帧、横竖屏切换偶发进度丢失 |
| P3 | 轻微 | UI 细节、文案、不影响功能的提示异常 | 排期修复 | 按钮 margin 偏移、Toast 文案错误 |

### 1.4 状态取值约定

- **待处理**：已录入，未分配责任人。
- **处理中**：已分配责任人，正在定位或修复。
- **已修复**：代码已合入，待测试验证。
- **已验证**：测试通过，关闭。
- **已挂起**：暂无法复现或依赖外部条件，挂起待跟进。
- **不予修复**：评估后确认无需修复（如已废弃功能）。

### 1.5 优先级取值约定

`紧急 / 高 / 中 / 低`。优先级与严重等级联动但允许独立调整（如 P3 但用户感知强可设为高）。

---

## 二、Bug 记录模板

> 每条 Bug 按以下模板完整填写，字段缺失需在对应栏注明「无」或「待补充」。

```
### BUG-XXXX  <Bug 标题>

| 字段 | 内容 |
| --- | --- |
| BugID | BUG-XXXX（四位流水号，全局唯一） |
| 标题 | 一句话描述问题现象 |
| 模块 | 长视频播放 / 短视频Feed / 弹幕 / 缓存 / 加密 / 网络 / UI / 性能 / 其他 |
| 严重等级 | P0 / P1 / P2 / P3 |
| 优先级 | 紧急 / 高 / 中 / 低 |
| 发现时间 | YYYY-MM-DD HH:mm |
| 发现人 | 姓名 / 工号 |
| 环境 | 机型 / 系统版本 / APP版本 / 网络 / 是否复现机型范围 |
| 复现步骤 | 1. ... 2. ... 3. ... |
| 预期结果 | 期望发生的行为 |
| 实际结果 | 实际发生的行为 |
| 日志/截图 | Logcat 关键日志 / Crash 堆栈 / 截图路径 / 视频录屏路径 |
| 根因分析 | 定位到的代码位置、设计缺陷、第三方库问题等 |
| 修复方案 | 具体修改点、修改文件、修改方式、是否需回归 |
| 修复人 | 姓名 / 工号 |
| 修复时间 | YYYY-MM-DD HH:mm |
| 验证状态 | 已验证 / 已修复待验证 / 待处理 / 已挂起 |
| 关联任务ID | TASK-XXXX（关联开发任务文档中的任务编号） |
```

---

## 三、Bug 统计看板

> 看板数据随 Bug 录入与状态变更实时刷新，由测试组每周一同步。当前数据为示例基线。

### 3.1 按模块统计

| 模块 | P0 | P1 | P2 | P3 | 合计 | 已修复 | 遗留 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 长视频播放 | 1 | 1 | 0 | 0 | 2 | 2 | 0 |
| 短视频Feed | 1 | 1 | 0 | 0 | 2 | 2 | 0 |
| 弹幕 | 0 | 2 | 0 | 0 | 2 | 2 | 0 |
| 缓存 | 0 | 2 | 0 | 0 | 2 | 2 | 0 |
| 加密 | 1 | 0 | 0 | 0 | 1 | 1 | 0 |
| 网络 | 0 | 1 | 0 | 0 | 1 | 1 | 0 |
| UI/横竖屏 | 0 | 1 | 1 | 0 | 2 | 2 | 0 |
| 性能/内存 | 1 | 1 | 1 | 0 | 3 | 3 | 0 |
| 后台/系统 | 0 | 1 | 0 | 0 | 1 | 1 | 0 |
| 存储 | 0 | 1 | 0 | 0 | 1 | 1 | 0 |
| 合计 | 4 | 11 | 2 | 0 | 17 | 17 | 0 |

### 3.2 按等级统计

| 等级 | 总数 | 已修复 | 遗留 | 修复率 |
| --- | --- | --- | --- | --- |
| P0 致命 | 4 | 4 | 0 | 100% |
| P1 严重 | 11 | 11 | 0 | 100% |
| P2 一般 | 2 | 2 | 0 | 100% |
| P3 轻微 | 0 | 0 | 0 | - |
| 合计 | 17 | 17 | 0 | 100% |

### 3.3 按状态统计

| 状态 | 数量 | 占比 |
| --- | --- | --- |
| 已验证 | 17 | 100% |
| 已修复待验证 | 0 | 0% |
| 处理中 | 0 | 0% |
| 待处理 | 0 | 0% |
| 已挂起 | 0 | 0% |
| 不予修复 | 0 | 0% |

---

## 四、典型 Bug 示例记录

> 以下为基于项目常见 bug 类型整理的示例记录，覆盖播放器、弹幕、缓存、加密、UI、性能、网络、存储等核心链路，供模板使用参考。

### BUG-0001  ExoPlayer 播放长视频偶发黑屏（TextureView 层级冲突）

| 字段 | 内容 |
| --- | --- |
| BugID | BUG-0001 |
| 标题 | ExoPlayer 播放长视频偶发黑屏（TextureView 层级冲突） |
| 模块 | 长视频播放 |
| 严重等级 | P1 |
| 优先级 | 高 |
| 发现时间 | 2026-08-04 14:20 |
| 发现人 | 张三 / T001 |
| 环境 | 小米 11 / Android 13 / zhuiju v1.0.0 / Wi-Fi / 同时复现机型：OPPO Find X6、vivo X90 |
| 复现步骤 | 1. 进入长视频详情页播放；2. 快速连续点击「弹幕开关」3 次；3. 切换全屏；4. 观察画面 |
| 预期结果 | 弹幕层显隐切换后画面正常显示 |
| 实际结果 | 画面黑屏，仅有声音，进度条仍走动 |
| 日志/截图 | `ExoPlayerImpl: Video size changed but surface is null`；截图：/logs/bug0001_screen.png |
| 根因分析 | 弹幕层使用 `FrameLayout` 叠加在 `TextureView` 之上，频繁显隐触发 `TextureView.SurfaceTexture` 被回收但 ExoPlayer 未重新绑定 `setSurface`；`PlayerView` 与自定义弹幕容器层级未隔离，`onSurfaceTextureAvailable` 回调时序错乱 |
| 修复方案 | 1. `PlayerView` 改用 `SurfaceView` 兜底并开启 `use_controller=false`；2. 弹幕层独立为 `DanmakuLayer`，通过 `addView` 挂载到 `PlayerView` 之上而非替换 `TextureView` 父容器；3. 在 `onSurfaceTextureAvailable` 回调中主动调用 `player.setVideoSurfaceView()` 重新绑定；4. 移除弹幕开关时的 `removeView` 操作，改用 `visibility` 控制 |
| 修复人 | 李四 / T002 |
| 修复时间 | 2026-08-04 18:50 |
| 验证状态 | 已验证 |
| 关联任务ID | TASK-203 |

### BUG-0002  弹幕与播放进度不同步（Seek 后弹幕堆积）

| 字段 | 内容 |
| --- | --- |
| BugID | BUG-0002 |
| 标题 | 弹幕与播放进度不同步（Seek 后弹幕堆积） |
| 模块 | 弹幕 |
| 严重等级 | P1 |
| 优先级 | 高 |
| 发现时间 | 2026-08-05 10:05 |
| 发现人 | 王五 / T003 |
| 环境 | 华为 Mate 50 / Android 12 / zhuiju v1.0.0 / Wi-Fi |
| 复现步骤 | 1. 播放长视频开启弹幕；2. 拖动进度条向前跳转 30 分钟；3. 观察弹幕 |
| 预期结果 | 弹幕立即从目标时间点继续滚动 |
| 实际结果 | 旧弹幕堆积 3-5 秒后突然涌出，新弹幕延迟出现 |
| 日志/截图 | `DanmakuFlameMaster: danmaku list size=1280 before seek clear`；录屏：/logs/bug0002_record.mp4 |
| 根因分析 | `DanmakuView.seekTo()` 未在 `Player.Listener.onSeekProcessed` 回调中触发，而是放在 `onPositionDiscontinuity` 中提前调用，导致弹幕时间轴未对齐；同时未调用 `danmakuView.clear()` 清空已入队弹幕 |
| 修复方案 | 1. 将 `danmakuView.seekTo(positionMs)` 移至 `onSeekProcessed` 回调内执行；2. Seek 前先调用 `danmakuView.clear()` 清空当前弹幕；3. 弹幕数据源 `DanmakuParser` 增加 `seekTo` 后按时间区间重新切片的逻辑；4. 协程订阅 `player.currentPositionFlow` 使用 `sample(200ms)` 而非 `collectLatest`，避免高频刷新 |
| 修复人 | 李四 / T002 |
| 修复时间 | 2026-08-05 16:30 |
| 验证状态 | 已验证 |
| 关联任务ID | TASK-301 |

### BUG-0003  缓存断点续传失败（Range 请求被服务端拒绝）

| 字段 | 内容 |
| --- | --- |
| BugID | BUG-0003 |
| 标题 | 缓存断点续传失败（Range 请求被服务端拒绝） |
| 模块 | 缓存 |
| 严重等级 | P1 |
| 优先级 | 高 |
| 发现时间 | 2026-08-06 09:30 |
| 发现人 | 张三 / T001 |
| 环境 | 三星 S22 / Android 13 / zhuiju v1.0.0 / 4G 弱网 |
| 复现步骤 | 1. 播放长视频缓存至 30%；2. 杀进程；3. 弱网下重新进入播放；4. 观察是否从 30% 续传 |
| 预期结果 | 从 30% 处继续缓存下载 |
| 实际结果 | 重新从 0 开始下载，原缓存文件被覆盖 |
| 日志/截图 | `Cache: server returned 200 instead of 206, fallback to full download`；日志：/logs/bug0003.log |
| 根因分析 | 自定义 `CacheDataSource` 未校验服务端是否支持 `Range`，直接发起 `Range: bytes=31457280-` 请求，服务端返回 `200 OK` 全量响应时未走续传分支反而覆盖原文件；同时 `.tmp` 临时文件命名规则与正式文件一致，导致覆盖 |
| 修复方案 | 1. 发起 Range 请求前先 `HEAD` 探测 `Accept-Ranges: bytes`；2. 不支持 Range 时降级为全量下载并保留旧缓存文件；3. 临时文件统一加 `.download` 后缀，下载完成后 `renameTo` 原子替换；4. 缓存元信息（已下载字节数、ETag）持久化到 `cache.meta` 文件，续传前校验 ETag 一致性 |
| 修复人 | 赵六 / T004 |
| 修复时间 | 2026-08-06 17:10 |
| 验证状态 | 已验证 |
| 关联任务ID | TASK-302 |

### BUG-0004  AES 解密失败导致播放中断（IV 偏移量计算错误）

| 字段 | 内容 |
| --- | --- |
| BugID | BUG-0004 |
| 标题 | AES 解密失败导致播放中断（IV 偏移量计算错误） |
| 模块 | 加密 |
| 严重等级 | P0 |
| 优先级 | 紧急 |
| 发现时间 | 2026-08-07 11:00 |
| 发现人 | 王五 / T003 |
| 环境 | 全机型 / Android 8-14 / zhuiju v1.0.0 / Wi-Fi |
| 复现步骤 | 1. 播放加密 HLS 分片视频；2. 播放至第 3 个分片；3. 观察画面 |
| 预期结果 | 分片无缝切换，持续播放 |
| 实际结果 | 第 3 分片加载后画面卡死，2 秒后报错 `ExoPlayer PlaybackException` |
| 日志/截图 | `AesDataSource: BadPaddingException: pad block corrupted`；`errorCode = ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE` |
| 根因分析 | HLS 每个分片 `#EXT-X-KEY` 的 `IV` 字段在缺省时应使用分片序列号（`MEDIA-SEQUENCE + index`）作为 IV，自定义 `AesDataSource` 固定使用 `0x00` 填充 IV，导致非首分片解密失败；同时密钥缓存未按分片 URL 区分，命中错误密钥 |
| 修复方案 | 1. 严格遵循 RFC 8216：`IV` 缺省时取 `sequence_number` 转 16 字节大端序；2. `AesDataSource` 构造参数增加 `segmentIndex`，由 `HlsMediaSource` 传入；3. 密钥缓存 Key 改为 `uri + segmentIndex`；4. 解密失败时不再吞掉异常，向上抛出并触发重试 1 次后降级清晰度 |
| 修复人 | 赵六 / T004 |
| 修复时间 | 2026-08-07 14:40 |
| 验证状态 | 已验证 |
| 关联任务ID | TASK-401 |

### BUG-0005  横竖屏切换播放进度丢失（ViewModel 未复用）

| 字段 | 内容 |
| --- | --- |
| BugID | BUG-0005 |
| 标题 | 横竖屏切换播放进度丢失（ViewModel 未复用） |
| 模块 | UI / 横竖屏 |
| 严重等级 | P2 |
| 优先级 | 中 |
| 发现时间 | 2026-08-08 15:20 |
| 发现人 | 张三 / T001 |
| 环境 | 红米 Note 11 / Android 12 / zhuiju v1.0.0 |
| 复现步骤 | 1. 播放长视频至 05:30；2. 旋转屏幕至横屏；3. 观察进度 |
| 预期结果 | 横屏后从 05:30 继续播放 |
| 实际结果 | 横屏后从 00:00 重新开始播放 |
| 日志/截图 | `DetailActivity onDestroy: player released`；`DetailActivity onCreate: player new instance` |
| 根因分析 | `Manifest` 未配置 `android:configChanges="orientation|screenSize|keyboardHidden"`，导致旋转时 Activity 重建；同时 `PlayerViewModel` 通过 `ViewModelProvider(this)` 创建而非 `by viewModels()` + `SavedStateHandle`，重建后 ViewModel 被回收，`ExoPlayer` 实例被 `release()`，仅靠 `SavedStateHandle` 保存 `positionMs` 但恢复逻辑在 `onCreate` 之前未读取 |
| 修复方案 | 1. `AndroidManifest` 中 `DetailActivity` 增加 `configChanges` 配置，禁止重建；2. 改用 `by viewModels()` + `SavedStateHandle` 持久化 `positionMs / windowIndex / playWhenReady`；3. `onConfigurationChanged` 中仅调整 `PlayerView` 布局参数，不重建 Player；4. 增加 `onSaveInstanceState` 兜底保存进度至 `outState` |
| 修复人 | 李四 / T002 |
| 修复时间 | 2026-08-08 17:00 |
| 验证状态 | 已验证 |
| 关联任务ID | TASK-501 |

### BUG-0006  短视频滑动爆音（AudioTrack 未及时释放）

| 字段 | 内容 |
| --- | --- |
| BugID | BUG-0006 |
| 标题 | 短视频滑动爆音（AudioTrack 未及时释放） |
| 模块 | 短视频Feed |
| 严重等级 | P1 |
| 优先级 | 高 |
| 发现时间 | 2026-08-09 19:30 |
| 发现人 | 王五 / T003 |
| 环境 | 一加 11 / Android 13 / zhuiju v1.0.0 / 蓝牙耳机 |
| 复现步骤 | 1. 进入首页短视频 Feed；2. 快速上滑切换 5 条视频；3. 听音效 |
| 预期结果 | 切换瞬间静音，无爆音 |
| 实际结果 | 切换瞬间出现「啪」的爆音，蓝牙耳机尤为明显 |
| 日志/截图 | `AudioTrack: release() called but still 1024 frames written`；`ExoPlayer: AudioSink underrun` |
| 根因分析 | `ViewPager2` 的 `onPageSelected` 回调中旧 Player 调用 `stop()` 但未调用 `setPlayWhenReady(false)` 与 `clearVideoSurface()`，AudioTrack 仍在写入最后一帧；新 Player `prepare` 后立即 `play()`，两个 AudioTrack 短暂重叠；`MediaCodecAudioRenderer` 的 `audioSink` 未配置 `AudioCapabilities` 与 `enableAudioTrackPlaybackParams=false` |
| 修复方案 | 1. 切换时按顺序：旧 Player `setPlayWhenReady(false)` → `clearVideoSurface()` → `stop()` → `release()`；2. 新 Player 复用播放器池实例，`prepare` 后延迟 100ms 再 `play()`；3. `DefaultRenderersFactory` 设置 `setEnableAudioTrackPlaybackParams(false)` 与 `setExtensionRendererMode(EXTENSION_RENDERER_MODE_PREFER)`；4. 增加切换音量淡入淡出（80ms 线性 ramp） |
| 修复人 | 李四 / T002 |
| 修复时间 | 2026-08-10 10:20 |
| 验证状态 | 已验证 |
| 关联任务ID | TASK-502 |

### BUG-0007  短视频滑动黑屏（播放器池未复用）

| 字段 | 内容 |
| --- | --- |
| BugID | BUG-0007 |
| 标题 | 短视频滑动黑屏（播放器池未复用） |
| 模块 | 短视频Feed |
| 严重等级 | P0 |
| 优先级 | 紧急 |
| 发现时间 | 2026-08-10 20:10 |
| 发现人 | 张三 / T001 |
| 环境 | 红米 Note 11 / Android 12 / zhuiju v1.0.0 |
| 复现步骤 | 1. 进入首页短视频 Feed；2. 连续上滑 10 次以上；3. 观察画面 |
| 预期结果 | 每条视频均可正常播放 |
| 实际结果 | 第 8 条后画面黑屏，仅有声音，持续 2-3 秒后恢复 |
| 日志/截图 | `PlayerPool: no available player, create new`；`TextureView: SurfaceTexture released` |
| 根因分析 | 播放器池容量为 3，但 `RecyclerView.Recycler` 缓存的 `ViewHolder` 中的 `PlayerView` 在 `onViewRecycled` 时未解绑 Player，导致池中 Player 被 ViewHolder 持有无法复用，新 `ViewHolder` 申请时池为空走 `create new` 分支，`ExoPlayer` 实例化耗时导致首帧延迟；同时 `setVideoSurface(null)` 时序晚于 `release()` |
| 修复方案 | 1. `onViewRecycled` 中先 `player.clearVideoSurface()` 再 `playerPool.release(player)` 归还；2. 播放器池容量提升至 5（1 当前 + 2 预加载 + 2 缓冲）；3. 预加载使用 `ExoPlayer.Builder().setUseLazyPreparation(true)`；4. `PlayerView` 改为共用一个 `Surface`，通过 `setVideoSurface` 切换而非切换 `PlayerView` 实例 |
| 修复人 | 李四 / T002 |
| 修复时间 | 2026-08-11 09:40 |
| 验证状态 | 已验证 |
| 关联任务ID | TASK-502 |

### BUG-0008  内存泄漏（ExoPlayer 未释放导致 OOM）

| 字段 | 内容 |
| --- | --- |
| BugID | BUG-0008 |
| 标题 | 内存泄漏（ExoPlayer 未释放导致 OOM） |
| 模块 | 性能 / 内存 |
| 严重等级 | P0 |
| 优先级 | 紧急 |
| 发现时间 | 2026-08-12 14:00 |
| 发现人 | 自动化测试 / LeakCanary |
| 环境 | 全机型 / Android 13 / zhuiju v1.0.0 |
| 复现步骤 | 1. 进入长视频详情页播放；2. 返回首页；3. 重复 10 次；4. 观察内存与 LeakCanary 通知 |
| 预期结果 | 内存平稳，无泄漏 |
| 实际结果 | 每次返回后内存增长约 30MB，第 8 次触发 OOM 崩溃，LeakCanary 报告 `DetailActivity leaked ExoPlayerImpl` |
| 日志/截图 | `LeakCanary: DetailActivity is leaking ExoPlayerImpl (3.2 MB)`；堆 dump：/logs/bug0008.hprof |
| 根因分析 | `DetailActivity.onDestroy()` 中仅调用 `player.stop()` 未调用 `player.release()`，且 `PlayerViewModel` 持有 `player` 引用但 `onCleared()` 未触发（Activity 被销毁但 ViewModel 因 `ViewModelStore` 持有未释放）；`Player.Listener` 匿名内部类持有 Activity 引用 |
| 修复方案 | 1. `PlayerViewModel.onCleared()` 中调用 `player.release()` 并置空；2. `Player.Listener` 改为静态内部类或弱引用持有 Activity；3. `onDestroy` 中先 `playerView.player = null` 解绑再 `release()`；4. 接入 LeakCanary 在 Debug 包持续监控，CI 流水线增加泄漏检测卡点 |
| 修复人 | 赵六 / T004 |
| 修复时间 | 2026-08-12 18:00 |
| 验证状态 | 已验证 |
| 关联任务ID | TASK-503 |

### BUG-0009  ANR（FFmpeg 命令在主线程执行）

| 字段 | 内容 |
| --- | --- |
| BugID | BUG-0009 |
| 标题 | ANR（FFmpeg 命令在主线程执行） |
| 模块 | 性能 / 内存 |
| 严重等级 | P0 |
| 优先级 | 紧急 |
| 发现时间 | 2026-08-13 10:15 |
| 发现人 | 王五 / T003 |
| 环境 | 红米 Note 11 / Android 12 / zhuiju v1.0.0 |
| 复现步骤 | 1. 播放本地视频；2. 点击「截取片段」生成 GIF；3. 等待生成 |
| 预期结果 | 生成过程中 UI 流畅，可取消 |
| 实际结果 | 点击后界面卡死 5 秒，系统弹窗「应用无响应」 |
| 日志/截图 | `/data/anr/traces.txt` 主线程堆栈停留在 `FFmpeg.run()`；`main tid=1 systag=Native` |
| 根因分析 | `GifMaker.makeGif()` 直接在 `lifecycleScope.launch` 默认 `Dispatchers.Main` 中调用 `FFmpeg.run()` 同步阻塞主线程；FFmpeg 单条命令耗时 3-5 秒（截取 10s + 编码 GIF）；未设置超时与取消机制 |
| 修复方案 | 1. 所有 FFmpeg 调用切换至 `Dispatchers.IO` 或专用 `FFmpegDispatcher`；2. 使用 `FFmpeg.runAsync()` + 回调，配合协程 `suspendCancellableCoroutine` 包装；3. 增加进度回调 `ProgressListener` 更新 UI；4. 用户可点击取消时调用 `FFmpeg.cancel()`；5. CI 增加主线程阻塞检测（StrictMode） |
| 修复人 | 赵六 / T004 |
| 修复时间 | 2026-08-13 16:30 |
| 验证状态 | 已验证 |
| 关联任务ID | TASK-303 |

### BUG-0010  网络请求未取消导致空指针崩溃

| 字段 | 内容 |
| --- | --- |
| BugID | BUG-0010 |
| 标题 | 网络请求未取消导致空指针崩溃 |
| 模块 | 网络 |
| 严重等级 | P1 |
| 优先级 | 高 |
| 发现时间 | 2026-08-14 11:00 |
| 发现人 | 张三 / T001 |
| 环境 | 华为 Mate 50 / Android 12 / zhuiju v1.0.0 / 弱网 |
| 复现步骤 | 1. 进入详情页触发视频信息请求；2. 请求未返回时快速返回首页；3. 等待请求返回 |
| 预期结果 | 请求自动取消，无异常 |
| 实际结果 | 应用崩溃 `NullPointerException: detailBinding.videoTitle.text` |
| 日志/截图 | `Crash: NullPointerException at DetailActivity.showInfo(DetailActivity.kt:187)`；`OkHttp: request returned after activity destroyed` |
| 根因分析 | 详情页 `viewModel.loadVideoInfo()` 使用 `viewModelScope.launch` 但未在 `onDestroy` 中取消协程；网络回调中直接操作 `ViewBinding`，Activity 已销毁时 `detailBinding.videoTitle` 为 null；OkHttp `Call` 未跟随生命周期取消 |
| 修复方案 | 1. 网络请求统一走 `ViewModel.viewModelScope`，Activity 销毁时自动取消；2. UI 更新前增加 `viewLifecycleOwner.lifecycle.currentState.isAtLeast(STARTED)` 判断；3. `OkHttp` `Call` 注册到 `LifecycleCoroutineScope`，`onDestroy` 时 `call.cancel()`；4. `ViewBinding` 改为 `lateinit var` 并在 `onDestroyView` 置空，访问前判空 |
| 修复人 | 李四 / T002 |
| 修复时间 | 2026-08-14 15:20 |
| 验证状态 | 已验证 |
| 关联任务ID | TASK-204 |

### BUG-0011  弹幕堆积卡顿（未限制并发弹幕数量）

| 字段 | 内容 |
| --- | --- |
| BugID | BUG-0011 |
| 标题 | 弹幕堆积卡顿（未限制并发弹幕数量） |
| 模块 | 弹幕 |
| 严重等级 | P1 |
| 优先级 | 中 |
| 发现时间 | 2026-08-15 14:30 |
| 发现人 | 王五 / T003 |
| 环境 | 红米 Note 11 / Android 12 / zhuiju v1.0.0 / 热门番剧高弹幕密度 |
| 复现步骤 | 1. 播放热门番剧；2. 开启弹幕并选择「全部」；3. 播放 1 分钟 |
| 预期结果 | 弹幕滚动流畅，帧率 ≥ 30 |
| 实际结果 | 弹幕越积越多，画面掉帧至 15fps，5 分钟后 ANR |
| 日志/截图 | `DanmakuView: rendering 320 danmakus per frame`；`Choreographer: Skipped 60 frames` |
| 根因分析 | `DanmakuContext` 未设置 `setMaximumVisibleSize`，所有同时间戳弹幕全部入队渲染；`DanmakuView` 使用默认 `SyncPlayer` 每帧重绘全部弹幕；弹幕 `Drawable` 未做对象池复用，频繁 GC |
| 修复方案 | 1. `DanmakuContext.setMaximumVisibleSize(80)` 限制同屏弹幕数；2. 开启「智能屏蔽」按类型/颜色/用户去重；3. `DanmakuView` 切换为 `FlippingDrawable` + 硬件加速；4. 弹幕对象池化，预分配 200 个 `DanmakuItem`；5. 高密度场景自动降级为 30% 透明度并隐藏底部弹幕 |
| 修复人 | 李四 / T002 |
| 修复时间 | 2026-08-15 18:00 |
| 验证状态 | 已验证 |
| 关联任务ID | TASK-301 |

### BUG-0012  低端机掉帧（解码器未适配硬解优先）

| 字段 | 内容 |
| --- | --- |
| BugID | BUG-0012 |
| 标题 | 低端机掉帧（解码器未适配硬解优先） |
| 模块 | 性能 / 内存 |
| 严重等级 | P2 |
| 优先级 | 中 |
| 发现时间 | 2026-08-16 10:00 |
| 发现人 | 张三 / T001 |
| 环境 | 红米 9A / Android 10 / zhuiju v1.0.0 / 720p 视频 |
| 复现步骤 | 1. 在红米 9A 播放 720p 长视频；2. 播放 30 秒；3. 观察帧率 |
| 预期结果 | 帧率稳定 24fps 以上 |
| 实际结果 | 帧率波动 12-18fps，画面卡顿 |
| 日志/截图 | `MediaCodecVideoRenderer: fallback to software decoder (avc)`；`VideoFrameProcessor: dropped 8 frames` |
| 根因分析 | `DefaultRenderersFactory` 未设置 `setExtensionRendererMode(EXTENSION_RENDERER_MODE_PREFER)`，硬解失败时回退到软解；红米 9A 的 `MediaCodec` 对 `avc` 编码支持但 `HEVC` 不支持，APP 未按机型能力选择清晰度；解码缓冲区 `setVideoChangeFrameRateStrategy` 未开启 |
| 修复方案 | 1. `DefaultRenderersFactory` 开启 `EXTENSION_RENDERER_MODE_PREFER` 优先使用 FFmpeg 扩展解码器；2. 启动时检测 `MediaCodecList` 能力，HEVC 不支持的机型默认请求 720p 以下；3. `TrackSelector` 参数 `setMaxVideoSize(1280, 720)`；4. 开启 `ExoPlayer.setVideoChangeFrameRateStrategy(CM_CHANGE_FRAME_RATE_ONLY)`；5. 低端机降级弹幕密度至 30 |
| 修复人 | 赵六 / T004 |
| 修复时间 | 2026-08-16 16:40 |
| 验证状态 | 已验证 |
| 关联任务ID | TASK-504 |

### BUG-0013  后台播放被系统杀死（前台服务未启动）

| 字段 | 内容 |
| --- | --- |
| BugID | BUG-0013 |
| 标题 | 后台播放被系统杀死（前台服务未启动） |
| 模块 | 后台 / 系统 |
| 严重等级 | P1 |
| 优先级 | 高 |
| 发现时间 | 2026-08-17 09:20 |
| 发现人 | 王五 / T003 |
| 环境 | 小米 13 / Android 13 / zhuiju v1.0.0 / 锁屏 1 分钟 |
| 复现步骤 | 1. 播放长视频；2. 按电源键锁屏；3. 等待 2 分钟 |
| 预期结果 | 后台继续播放音频 |
| 实际结果 | 锁屏 30 秒后播放停止，再次进入需重新加载 |
| 日志/截图 | `BackgroundService: killed by system (low memory)`；`JobScheduler: job service finished` |
| 根因分析 | 后台播放未启动 `ForegroundService`，仅依赖 Activity 生命周期；Android 12+ 后台执行限制导致进程被回收；未申请 `FOREGROUND_SERVICE` 权限与 `POST_NOTIFICATIONS` 权限；`MediaSession` 未与 `MediaController` 绑定 |
| 修复方案 | 1. 新建 `PlaybackService extends MediaSessionService`，播放时 `startForeground` 显示通知栏控件；2. `AndroidManifest` 声明 `FOREGROUND_SERVICE_MEDIA_PLAYBACK` 与 `POST_NOTIFICATIONS`；3. 接入 `MediaSession` 与 `MediaController`，锁屏控件走系统 `MediaStyle`；4. `onTaskRemoved` 中保留播放，仅用户主动停止时 `stopSelf()`；5. 适配 Android 14 前台服务类型声明 |
| 修复人 | 李四 / T002 |
| 修复时间 | 2026-08-17 17:30 |
| 验证状态 | 已验证 |
| 关联任务ID | TASK-505 |

### BUG-0014  分区存储缓存写入失败（Android 10+ Scoped Storage）

| 字段 | 内容 |
| --- | --- |
| BugID | BUG-0014 |
| 标题 | 分区存储缓存写入失败（Android 10+ Scoped Storage） |
| 模块 | 存储 / 缓存 |
| 严重等级 | P1 |
| 优先级 | 高 |
| 发现时间 | 2026-08-18 11:40 |
| 发现人 | 张三 / T001 |
| 环境 | 华为 P40 / Android 11 / zhuiju v1.0.0 |
| 复现步骤 | 1. 在 Android 11 机型点击「缓存到本地」；2. 观察缓存进度 |
| 预期结果 | 缓存成功写入 Movies/zhuiju 目录 |
| 实际结果 | 提示「缓存失败：权限不足」，未生成文件 |
| 日志/截图 | `IOException: EACCES (Permission denied) at /storage/emulated/0/Movies/zhuiju/xxx.mp4`；`MediaStore: insert returned null` |
| 根因分析 | Android 10+ 启用分区存储，直接 `File` 写入公共目录失败；未使用 `MediaStore` API；`requestLegacyExternalStorage` 在 Android 11+ 失效；缓存路径硬编码为 `/sdcard/Movies/zhuiju` |
| 修复方案 | 1. Android 10+ 统一使用 `MediaStore.Downloads` 与 `MediaStore.Video` 写入；2. `targetSdkVersion` 升级至 30+ 并适配分区存储；3. 缓存路径切换至 `context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)/zhuiju`（应用专属目录无需权限）；4. 提供「导出到相册」二次操作走 `MediaStore`；5. 兼容旧版本保留 `requestLegacyExternalStorage=true` |
| 修复人 | 赵六 / T004 |
| 修复时间 | 2026-08-18 16:10 |
| 验证状态 | 已验证 |
| 关联任务ID | TASK-304 |

### BUG-0015  OkHttp 连接池耗尽导致请求超时

| 字段 | 内容 |
| --- | --- |
| BugID | BUG-0015 |
| 标题 | OkHttp 连接池耗尽导致请求超时 |
| 模块 | 网络 |
| 严重等级 | P1 |
| 优先级 | 中 |
| 发现时间 | 2026-08-19 15:00 |
| 发现人 | 王五 / T003 |
| 环境 | 三星 S22 / Android 13 / zhuiju v1.0.0 / 弱网切换 Wi-Fi |
| 复现步骤 | 1. 进入短视频 Feed 快速滑动 20 条；2. 同时预加载 3 条长视频封面；3. 观察网络请求 |
| 预期结果 | 所有请求正常完成 |
| 实际结果 | 部分请求超时 30 秒后失败，弹幕与封面加载不出来 |
| 日志/截图 | `OkHttp: ConnectionPool is full (5/5), waiting...`；`SocketTimeoutException: timeout after 30000ms` |
| 根因分析 | OkHttp 默认 `ConnectionPool` 容量 5 连接 / 5 分钟，但每个视频域名独立（CDN 域名分片），导致连接池被不同 host 占满；弹幕、封面、视频分片、统计上报共用同一 `OkHttpClient` 未做隔离；未配置 `maxIdleConnections` 与 `keepAliveDuration` |
| 修复方案 | 1. 自定义 `ConnectionPool(20, 5, TimeUnit.MINUTES)` 扩容；2. 按业务隔离 `OkHttpClient`：`videoClient` / `imageClient` / `danmakuClient` / `statsClient`，共享 `Dispatcher` 但独立连接池；3. 视频下载走 `ExoPlayer` 内置 `HttpDataSource` 而非 `OkHttpClient`；4. CDN 域名收敛至 3 个以内；5. 增加 `Interceptor` 对失败请求自动重试 2 次并降级 |
| 修复人 | 李四 / T002 |
| 修复时间 | 2026-08-19 18:30 |
| 验证状态 | 已验证 |
| 关联任务ID | TASK-205 |

---

## 五、调试技巧与工具

### 5.1 日志规范

- **统一 Tag**：按模块取 Tag，如 `ZJ-Player`、`ZJ-Danmaku`、`ZJ-Cache`、`ZJ-Aes`、`ZJ-Net`，便于 Logcat 过滤。
- **日志分级**：`Log.v` 调试细节、`Log.d` 关键流程、`Log.i` 业务节点、`Log.w` 可恢复异常、`Log.e` 不可恢复异常。
- **禁止**：日志中输出 AES 密钥、用户 token、完整 URL（需脱敏 query 参数）。
- **结构化**：关键路径日志携带 `traceId`（一次播放会话唯一），便于跨模块串联。
- ** release 包**：仅保留 `Log.i/w/e`，`Log.v/d` 通过 `BuildConfig.DEBUG` 控制。

### 5.2 LeakCanary 内存检测

- 依赖：`debugImplementation 'com.squareup.leakcanary:leakcanary-android:2.12'`
- 自动监控 `Activity`、`Fragment`、`ViewModel`、`View` 泄漏。
- 关键配置：在 `Application` 中 `LeakCanary.config = LeakCanary.config.copy(dumpHeap = true)`。
- 排查步骤：
  1. 复现泄漏场景后等待 LeakCanary 通知；
  2. 点击通知查看 hprof 分析结果；
  3. 定位引用链最上层业务对象；
  4. 在对应生命周期补充释放逻辑。
- CI 集成：使用 `leakcanary-android-instrumentation` 在 UI 自动化测试中采集泄漏，失败阻断流水线。

### 5.3 Android Studio Profiler 性能分析

| 分析类型 | 适用场景 | 关键指标 |
| --- | --- | --- |
| CPU Profiler | ANR、卡顿、掉帧 | 主线程方法耗时、火焰图、System Call |
| Memory Profiler | OOM、内存增长 | Java/Kotlin 堆、Native 内存、对象分配、GC 频次 |
| Network Profiler | 请求异常、超时 | 请求时序、payload 大小、耗时分布 |
| Energy Profiler | 耗电、发热 | CPU/WakeLock/GPS/Network 耗电 |
| Layout Inspector | UI 卡顿、层级冲突 | View 树深度、测量/布局/绘制耗时 |

排查掉帧：`CPU Profiler` → `Trace System Calls` → 录制 5 秒 → 查看主线程 `Systrace`，定位 `measure/layout/draw` 耗时方法。

### 5.4 adb 常用命令

```bash
# 查看日志（按 Tag 过滤）
adb logcat -s ZJ-Player:V ZJ-Danmaku:V *:E

# 抓取 ANR traces
adb pull /data/anr/traces.txt ./traces.txt

# 抓取崩溃堆栈
adb logcat -b crash

# 查看内存占用
adb shell dumpsys meminfo com.zhuiju.app

# 查看前台 Activity
adb shell dumpsys activity activities | grep mResumedActivity

# 触发 GC
adb shell am send-trim-memory com.zhuiju.app 80

# 模拟弱网（需 emulator 或 root）
adb shell settings put global captive_portal_mode 0

# 录屏（最多 180 秒）
adb shell screenrecord /sdcard/bug.mp4
adb pull /sdcard/bug.mp4

# 查看网络连接
adb shell dumpsys netstats | grep com.zhuiju.app

# 强制停止
adb shell am force-stop com.zhuiju.app
```

### 5.5 抓包工具

- **Charles**：HTTPS 抓包，需在 `network_security_config.xml` 配置 Charles 证书信任；用于排查接口请求/响应。
- **mitmproxy**：命令行抓包，适合自动化脚本；可对 HLS 分片请求做断言。
- **Wireshark**：TCP 层抓包，用于排查连接池、断点续传、Range 请求等底层网络问题。
- **Stetho / Facebook Profiler**：Debug 包查看 OkHttp 请求队列与 SQLite 缓存。

### 5.6 ExoPlayer 调试日志

```kotlin
// 开启 ExoPlayer 详细日志
AdTAG = "ExoPlayer"
// 在 Application onCreate 中
Logger.setLogLevels(Logger.LEVEL_ALL)

// 监听播放事件
player.addAnalytics(object : AnalyticsEventListener {
    override fun onEvents(player: Player, events: Player.Events) {
        events.forEach { eventTime, event ->
            Log.d("ZJ-Player", "event=${event}")
        }
    }
    override fun onVideoInputFormatChanged(eventTime: AnalyticsListener.EventTime, format: Format) {
        Log.d("ZJ-Player", "format=${format.sampleMimeType} ${format.width}x${format.height}")
    }
    override fun onDroppedVideoFrames(eventTime: AnalyticsListener.EventTime, droppedFrames: Int, elapsedMs: Long) {
        Log.w("ZJ-Player", "dropped $droppedFrames frames in ${elapsedMs}ms")
    }
})
```

关键日志关注点：
- `onPlayerStateChanged`：状态流转是否异常（如长时间停留在 BUFFERING）；
- `onPlayerError`：错误码与 cause 链；
- `onDroppedVideoFrames`：掉帧统计；
- `onVideoSizeChanged`：分辨率切换，排查黑屏；
- `onLoadError`：网络/缓存加载失败。

### 5.7 Logcat 过滤技巧

```bash
# 仅看本项目日志
adb logcat --pid=$(adb shell pidof com.zhuiju.app)

# 排除 noisy Tag
adb logcat ZJ-Player:V ZJ-Danmaku:V Choreographer:S OpenGLRenderer:S *:E

# 关键字过滤
adb logcat | grep -E "(ExoPlayer|ZJ-|PlaybackException|ANR|SIGSEGV)"

# 时间范围过滤
adb logcat -T "2026-08-19 15:00:00.000"

# 输出到文件
adb logcat -v threadtime > logs/full.log
```

---

## 六、Bug 复盘记录模板

> P0/P1 Bug 修复验证通过后 3 个工作日内完成复盘并归档，复盘由修复人主导，测试与架构师参与。

```
### 复盘记录 BUG-XXXX

| 字段 | 内容 |
| --- | --- |
| BugID | BUG-XXXX |
| 复盘时间 | YYYY-MM-DD HH:mm |
| 复盘主持 | 姓名 / 工号 |
| 参与人 | 开发 / 测试 / 架构 / 产品 |
| 问题摘要 | 一句话回顾问题现象与影响 |
| 影响范围 | 用户量、功能范围、时长 |
| 根因总结 | 直接原因 + 深层原因（设计/流程/认知） |
| 时间线 | 发现 → 响应 → 定位 → 修复 → 验证 各节点时间 |
| 处置评估 | 响应是否及时、修复方案是否最优、是否存在过度修复 |
| 暴露的流程问题 | 例如：缺少某类测试用例、Code Review 未覆盖、监控缺失 |
| 改进措施 | 短期：补充单测/回归用例；长期：架构调整/工具建设/规范补充 |
| 知识沉淀 | 是否需更新开发文档、注意事项、Code Review checklist |
| 防回归机制 | 回归用例编号、监控告警阈值、CI 卡点 |
| 关联文档 | 开发任务文档 / 测试记录 / 技术文档 |
```

---

## 七、Bug 趋势跟踪表

> 按自然周统计新增 / 修复 / 遗留数量，用于评估质量趋势与上线风险。遗留 = 历史遗留 + 新增 - 修复。

| 周次 | 起止日期 | 新增 | 修复 | 遗留 | P0 新增 | P0 遗留 | 备注 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| W1 | 2026-08-04 ~ 2026-08-10 | 7 | 6 | 1 | 2 | 1 | 阶段二、三启动，播放器与弹幕问题集中 |
| W2 | 2026-08-11 ~ 2026-08-17 | 7 | 8 | 0 | 2 | 0 | 阶段四加密落地，AES 与 ANR 集中修复 |
| W3 | 2026-08-18 ~ 2026-08-24 | 3 | 3 | 0 | 0 | 0 | 阶段五优化收尾，存储与网络问题 |
| W4 | 2026-08-25 ~ 2026-08-31 | 0 | 0 | 0 | 0 | 0 | 上线前回归，无新增 |
| 合计 | - | 17 | 17 | 0 | 4 | 0 | 累计修复率 100% |

### 7.1 趋势分析

- **新增趋势**：W1-W2 为问题高发期，集中于播放器与加密核心链路；W3 后明显收敛，符合「核心模块先暴露后稳定」规律。
- **修复效率**：P0 平均修复时长 3.2 小时，P1 平均修复时长 1.5 工作日，满足 SLA。
- **遗留风险**：截至 W4 遗留 0，可进入上线评审。
- **改进建议**：
  1. 阶段二、三启动前增加播放器池与弹幕同步的专项 Code Review；
  2. AES 加解密单测覆盖率提升至 90% 以上；
  3. 低端机（红米 9A 级别）纳入冒烟测试基线机型；
  4. LeakCanary + StrictMode 在 CI 中常态化卡点。

---

## 八、附则

- 本文档由项目开发组与测试组共同维护，Bug 录入后 2 小时内更新，修复验证后 24 小时内补充复盘。
- 字段如有调整，需在「文档说明」中记录变更历史。
- 与《开发任务文档》《测试记录》《项目技术文档》双向关联，关联 ID 需保持一致。
- 涉及线上事故的 Bug 需同步录入《开发记录文档》风险与问题章节。

### 变更历史

| 版本 | 日期 | 修改人 | 修改内容 |
| --- | --- | --- | --- |
| v1.0 | 2026-07-29 | 项目开发组 | 初始版本，建立模板、看板、15 条示例与趋势表 |
