# PROJECT_NOTES.md

本文件是「黄金实时价格小组件」（AndroidElement）项目唯一的注意事项文档。
所有坑、决策、改动、复现步骤都记录在这里，不另建重复文件。

---

## 2026-10-12 - v2.1.1 中置切换按钮

- 将 48dp 的 ⇄ 切换按钮放在两个价格之间，按同一逻辑坐标 y=148 居中；使用系统文字并提供当前曲线及切换目标的无障碍描述。
- 删除底部操作行，图表下方预留由 175 降到 60 个逻辑单位，增加 115 个逻辑单位的纵向空间；行情单位与 LOCAL HISTORY 保留在底部。
- 100ms 动画建议（尚未实现）：成功收到行情后，末端低亮度红色光环以 10 FPS 淡出，持续约 1 秒，不改变文字和真实曲线坐标。行情请求仍为约 5 秒；失败、熄屏不播放。实施应采用小尺寸独立装饰层、缓存静态画布，不通过每帧写 DataStore 或重绘整张高分辨率图来动画。
- 图标在同一 Canvas 使用实际 glyph 边界居中，避免系统 TextView 基线造成与 Doto 主价格错位。构建与 2 项解析测试通过，覆盖安装成功，桌面截图确认中置按钮、加高图表无文本挤压。

## 2026-10-12 - v2.1.0 清晰绘制与双行情曲线

- 发糊原因：旧图像按 2 px/dp 输出，在本机 560 dpi（3.5 px/dp）显示时放大；嵌套图表也先低分辨率绘制再放大。改用实际密度、DENSITY_NONE，并将图表直接绘制到最终像素尺寸。
- Doto 保留于主价格和标题，小号说明改用无衬线字体，以免小尺寸圆点字难辨。
- 黑色组件新增底部切换按钮，国际 XAU 与国内 AU9999 使用同一份双价格采样历史；选择存入单个组件的 DataStore 状态，不会被刷新覆盖。
- 最高价放入顶部独立区域，末端价格有黑底遮罩且在新高附近向下避让，防止虚线穿过文字。
- 更快数据源调查：Twelve Data 官方 commodities 页面称 WebSocket 延迟可低至 170ms，REST 按分钟更新；需 API Key 和对应权限，未实测。gold-api.com 提供免费实时接口，但没有明确逐 tick 延迟保证。国内 AU9999 与国际价格换算不同，不能用 XAU 换算人民币来替代。
- 当前保留新浪，不自动购买套餐、不录入密钥、不在未经测速下宣称新接口更快。参考：https://twelvedata.com/commodities 、https://gold-api.com/docs 、https://sge.com.cn/sjzx/rzxxs 。
- GoldAPI 国内候选需要 AppKey，单品通道默认允许 1 次/秒；请求限速不代表报价每秒更新，也尚未核实授权和 AU9999 延迟。文档：https://gold-api.cn/docs.php 。
- 验证：assembleDebug 与 2 项解析测试通过；真机覆盖安装成功。点击切换后曲线刻度由约 4190 改为约 904，标识变为 AU9999/CNY-G，后续周期刷新保留国内选择。真机截图覆盖新高状态，标签与虚线分离；按钮触摸区 48dp。日志持续刷新，无相关崩溃。

## 2026-10-12 - v2.0.1 桌面验收与刷新提速

- 用户放置后，截图确认黑色组件与 FM 左右边缘对齐，双价格区及图表未挤压。
- 亮屏刷新改为请求完成后等待 5 秒；熄屏继续跳过请求，保留互斥避免并发堆积。不是交易所每 5 秒必有新报价。
- 图表整数刻度在小幅波动时全部显示同一个数；改为两位小数，并扩大刻度留白以免文字与曲线重叠。
- 旧版 30 秒条目保留为历史决策，不代表当前刷新周期。
- 最高价标签移到图表左侧，避免创新高时与右侧当前价格标签重叠。
- 验证：构建成功，2 项解析测试通过；覆盖安装成功，刷新日志间隔约 5.25 秒，桌面组件保留。

## 2026-10-12 - v2.0.0 系列组件重构

- 基于 FM、指南针规范统一黑底、14dp 圆角、Doto 700、对称双价格区和 3×5 桌面下的 3×2 基准。画布按 Launcher 实际大小生成并等比显示。
- 修正行情解析：新浪 hf_XAU 与 gds_AU9999 的索引 7 为昨收；旧版把索引 1 或买价当昨收，国内字段 1 为零时会显示虚假零涨跌。新增解析回归测试。
- 请求周期由 1 秒降为 30 秒；熄屏跳过请求，无组件时停止服务。复用 HTTP 客户端，刷新互斥，避免手动与周期请求重叠。
- 图表生成和写盘移到 IO 协程；相同价格最多每五分钟补点。失败保留缓存并标注 STALE，WAITING 不显示虚构价格。
- LOCAL HIGH 仅表示本机缓存范围内最高价，不是市场历史最高价。顶部时间是抓取成功时间，不是交易所报价时间。
- 通知渠道由服务自行创建，补齐 targetSdk 34 的 dataSync 权限，取消独立无生命周期点击协程。
- 保留浅色组件和原包名；尚需用户手动放置新组件后进行 Launcher 对齐验收，不能将 APK 安装成功等同于桌面验收通过。
- 临时构建环境：/tmp/gold-build-tools（Gradle 8.6）、/tmp/gold-jdk17（JDK 17）、/tmp/gold-gradle-cache（依赖缓存），不修改全局 JDK。
- 原笔记关于 sdkmanager 官方结构为扁平目录的结论不应复用：那是旧机的异常解压情况，官方安装应按下载包实际结构处理。
- 验证：assembleDebug 成功；2 项解析回归测试通过；ADB 安装 Success；MainActivity 启动 Status: ok，相关启动日志无崩溃。桌面放置验收待用户添加组件。


## 2026-10-01 - cmdline-tools 的 sdkmanager 报 ClassNotFoundException

### 现象
运行 `sdkmanager.bat --version` 或 `--install` 时报错：
```
java.lang.ClassNotFoundException: com.android.sdklib.tool.sdkmanager.SdkManagerCli
```

### 原因（根因，已确认）
Android `cmdline-tools` 解压后是**扁平结构**：所有 `.bat`、`.jar` 以及依赖**目录**（`sdklib\`、`common\`、`external\` 等）都平铺在 `cmdline-tools\latest\` 同一层。

其中 `sdkmanager-classpath.jar` 只是一个**约 3KB 的引导 jar**，它通过 `META-INF/MANIFEST.MF` 的 `Class-Path:` 字段引用同目录下的一堆相对路径依赖（如 `sdklib/libsdkmanager_lib.jar`、`common/tools.common.jar` 等）。

> 尝试「整理」目录 —— 把 `.bat` 移到 `bin\`、`.jar` 移到 `lib\` —— 会**破坏**这些相对路径引用，导致 JVM 找不到 `SdkManagerCli` 类。

### 正确结构（必须保留）
```
cmdline-tools/latest/
├── sdkmanager.bat            ← .bat 就在这一层
├── sdkmanager-classpath.jar  ← 引导 jar 也在这一层
├── sdklib/                   ← 依赖目录，保持平铺
├── common/
├── external/
└── ...
```
**不要**自行创建 `bin/`、`lib/` 子目录去"规整"它。

### 结论 / 避免方式
- Google 官方 cmdline-tools 解压后就是扁平的，**不要动它的内部结构**。
- 只要保证 `sdkmanager.bat` 和所有 jar/目录在同一层，`sdkmanager` 就能正常运行。
- 环境变量 `ANDROID_HOME` 指向 `C:\Android` 即可，Gradle 会自动找 `cmdline-tools/latest/bin/sdkmanager`（实际 Gradle 用它自己的方式定位，扁平结构也 OK）。

### 已验证有效的方案
- 恢复扁平结构后，`java -cp <正确的 classpath>` 能加载到 `SdkManagerCli`。
- 用 `--sdk_root=C:\Android` 参数显式指定 SDK 根目录，避免环境变量未生效的问题。

---

## 环境搭建记录（本机）

| 组件 | 位置 | 版本 |
|------|------|------|
| JDK | `C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot` | 17.0.20.1 |
| Android SDK 根 | `C:\Android` | cmdline-tools 11076708 |
| cmdline-tools | `C:\Android\cmdline-tools\latest` | 扁平结构（勿动） |

环境变量（用户级，已持久设置）：
- `JAVA_HOME` = `C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot`
- `ANDROID_HOME` = `C:\Android`
- `ANDROID_SDK_ROOT` = `C:\Android`

---

## 网络环境坑：curl 下载 SSL 吊销检查失败

### 现象
```
curl: (35) schannel: next InitializeSecurityContext failed: CRYPT_E_REVOCATION_OFFLINE
```

### 原因
本机网络环境无法访问证书吊销服务器（离线/被墙），Windows schannel 默认会校验 SSL 证书吊销状态。

### 解决
给 curl 加 `--ssl-no-revoke` 参数跳过吊销检查（只对可信的官方域名，如 `dl.google.com` 使用）。

---

## 2026-10-01 - MainActivity 崩溃：Theme.AppCompat 主题不匹配

### 现象
App 安装后首次启动即崩溃，闪退回桌面。logcat 报错：
```
FATAL EXCEPTION: main
java.lang.IllegalStateException: You need to use a Theme.AppCompat theme (or descendant) with this activity.
    at com.unimend.goldwidget.MainActivity.onCreate(MainActivity.kt:21)
```

### 原因（已确认）
`MainActivity` 继承了 `AppCompatActivity`（AndroidX），但 `themes.xml` 里主题 parent 写成了
`android:Theme.Material.Light.NoActionBar`（系统原生 Material 主题）。
`AppCompatActivity` 强制要求配套 `Theme.AppCompat.*` 主题，否则 `setContentView` 抛异常。

### 复现步骤
1. adb 安装 app-debug.apk
2. adb shell am start -n com.unimend.goldwidget/.MainActivity
3. 观察到应用闪退，logcat 出现上述 IllegalStateException

### 已做改动
- 修改 `app/src/main/res/values/themes.xml`：
  parent 从 `android:Theme.Material.Light.NoActionBar` 改为 `Theme.AppCompat.Light.NoActionBar`

### 避免方式
- 用 `AppCompatActivity` 时，主题 parent 必须用 `Theme.AppCompat.*`（而非 `android:Theme.*`）。
- 若想用系统原生主题，Activity 应继承普通的 `Activity` 而非 `AppCompatActivity`。

---

## 待办 / 后续优化建议
- [x] 完成 platform-tools / build-tools / platforms 的 SDK 组件下载（当前卡在 sdkmanager 目录结构问题，已修复）
- [x] 首次 Gradle 构建（会自动下载 Gradle 8.6 + 依赖）
- [x] 一加 7T Pro 解锁、开 USB 调试、连接 adb
- [x] 编译出 APK 并安装测试
- [ ] 用户在桌面添加小组件（手动操作）
- [ ] ColorOS 白名单设置（允许后台运行 + 忽略电池优化，保证 30 秒刷新不被杀）

---

## 验证结论（2026-10-01）

数据链路已全程打通，验证通过：
- 设备：OnePlus 7T Pro (HD1910)，Android 12 / SDK 31，ColorOS
- 前台服务 `GoldRefreshService` 正常前台运行（isForeground=true）
- 每 30 秒从新浪接口抓取金价，成功写入 DataStore
- 实测数据：国际金价 4156.29 美元/盎司（-0.61%），国内金价 907.61 元/克（+0.067%）

注意：GoldWidget 的 state 存储位置为 `files/datastore/appWidget-<id>.preferences_pb`（DataStore Preferences，非 shared_prefs）。

---

## 最终组件形态（2026-10-01 定稿）

从"单个组件"历经多次迭代，最终定稿为**一个完整组合组件** `GoldChartWidget`：

```
┌─ 大底（半透明白 #80FFFFFF，圆角22）────────┐
│  标题「实时黄金价格」（深金）              │
│  ┌国际金价(半透明白)┐ ┌国内金价(半透明白)┐ │
│  │ iOS蓝价格         │ │ 深黑价格         │ │
│  └──────────────────┘ └──────────────────┘ │
│  ┌─ 折线图（半透明白，圆角12）──────────┐  │
│  │  Y轴价格 | 金色折线 | X轴时间 | 最高价虚线 | 当前价圆点 │
│  └───────────────────────────────────────┘  │
└──────────────────────────────────────────────┘
```

关键设计决策：
- **单组件**（不用拆分）：经历"拆成3个组件 → 又合并回1个"的反复，最终确定单组件体验最好
- **折线图**：单条国际金价折线，右侧留 16% "未来时间"空白（仿支付宝分时图）
- **历史最高价**：虚线 + 数值标签，当前价是新高时变红高亮
- **主题**：浅色半透明（毛玻璃白），可透出桌面壁纸

---

## 折线图渲染的关键坑（Bitmap + ContentScale）

### 现象
折线图（用 `Image` 显示 Bitmap）出现三种错误，反复折腾：
1. 数字变形（拉伸）
2. 上下/左右被裁剪
3. Y轴数字没贴边

### 根因（逐个确认）
1. **数字变形**：`Image` 的 `contentScale` 默认是 `ContentScale.Fit`（等比缩放留白），改成 `FillBounds` 又导致非等比拉伸变形。
2. **裁剪**：`Crop` 等比缩放填满会裁掉超出的边缘。
3. **Y轴没贴边**：`Fit` 等比缩放时，bitmap 比例 ≠ 显示区比例，自动居中留白。

### 最终方案（三者兼得：不变形 + 不裁剪 + 贴边）
- **bitmap 比例精确匹配显示区**：组件实际 min=319×131dp，曲线图显示区约 4:1，故 bitmap 设 `1600×400`（4:1）。
- **contentScale 用 `Fit`**：比例对了，Fit 就不会留白、不裁剪、不变形。

### 重要 API 备忘
- `ContentScale` 的正确包路径是 **`androidx.glance.layout.ContentScale`**（不是 unit、不是 appwidget）
- 三个取值：`Crop`（等比填满裁边）、`Fit`（等比留白，默认）、`FillBounds`（非等比拉伸填满，会变形）

### X 轴时间文字被裁的坑
`drawText` 的 y 坐标是**基线**。若设 `timeY = plotBottom + 17`，文字主体会超出 bitmap 底边被裁。正确做法是基线贴近底部但留出 descender 空间，如 `timeY = height - 3`。

---

## 网络问题的排查（重要教训）

### 现象
组件"没数据"，`fetch()` 持续报 `UnknownHostException: Unable to resolve host hq.sinajs.cn`。

### 排查过程（教训：先查网络，别先怀疑代码）
1. 电脑能访问新浪接口 → 接口没问题
2. 手机 `ping baidu.com` 也失败 → **全盘 DNS 故障**，不是单个域名
3. `ping 8.8.8.8`（IP直连）也失败 → **手机根本没网**
4. 查 WiFi 状态：`DISCONNECTED`，移动数据 `OUT_OF_SERVICE`

### 真正根因
手机重置后 **WiFi 没连上**（一加 7T Pro 对路由器 5G 频段兼容性差，反复掉线/拒绝接入）。

### 解决
换 **2.4G WiFi 或手机热点**（干净 2.4G 信号）后立刻稳定。组件代码、权限、服务全程无问题。

**教训**：排查"组件没数据"时，先确认 `ping baidu.com` 是否通，能省大量时间。

---

## 环境搭建记录（补充）

| 组件 | 位置 | 版本 |
|------|------|------|
| JDK | `C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot` | 17.0.20.1 |
| Android SDK | `C:\Android` | cmdline-tools 11076708 |
| Gradle | `C:\Gradle\gradle-8.6` | 8.6（下载自腾讯云镜像） |

Gradle 下载注意：`services.gradle.org` 在本机网络下极慢，改用腾讯云镜像 `https://mirrors.cloud.tencent.com/gradle/gradle-8.6-bin.zip`。

---

## 待办 / 后续优化建议
- [x] 完成 SDK 组件下载
- [x] 首次 Gradle 构建
- [x] 一加 7T Pro 连接 adb
- [x] 编译出 APK 并安装测试
- [x] 组件 UI 定稿（单组件 + 折线图 + 最高价 + 新高 + 浅色半透明主题）
- [ ] 清理遗留冗余（GoldSinglePriceWidget 两个单格组件已不用，待清理）
- [ ] ColorOS 白名单设置（允许后台运行，保证刷新不被杀）
- [ ] 后续扩展候选：价格预警提醒 / 多贵金属品种 / 深色浅色自动切换
