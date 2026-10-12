# Android Gold Widget

v2.0.1：与 Nothing FM、指南针统一设计的黄金行情桌面组件。

- 国际 XAU（美元/盎司）与国内 AU9999（人民币/克），并列显示价格和相对昨收的涨跌幅。
- 黑色版采用纯黑、14dp 圆角、Doto 700 和自适应等比画布；保留浅色版。
- 3×5 桌面建议放置为 3×2。不同网格以实际宿主尺寸绘制，文字不会非等比拉伸。
- 亮屏时每次刷新完成后等待 5 秒再请求新浪行情；点击组件立即刷新。熄屏暂停请求，无组件时停止服务。实际间隔包含网络耗时，数据源未更新时价格不变。
- 请求失败保留缓存并显示 STALE。顶部时间为成功抓取时间，不能用来判断市场仍在交易。
- 图表记录本机采样历史，LOCAL HIGH 仅为缓存范围最高价。

## 安装

安装 APK 后手动添加「黄金走势图」。添加后打开一次应用，允许设备需要的通知权限以启动刷新。ColorOS 若冻结服务，可点击组件刷新或打开应用恢复。

## 构建和验证

需要 JDK 17、Gradle 8.6、Android SDK 34。原仓库未提交完整 Gradle wrapper，所以使用已安装 Gradle：

```sh
gradle :app:assembleDebug :app:testDebugUnitTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

此版在 Mac 的临时目录下载 Gradle/JDK 并构建，不改变系统环境。字体许可证位于 app/src/main/assets/DOTO-OFL.txt。

## 架构

- GoldPriceRepository：复用网络客户端、校验报价、解析昨收。
- GoldRefreshService：约 5 秒刷新、屏幕状态检查、互斥请求、失败缓存、IO 渲染。
- GoldDashboardRenderer：按宿主比例绘制 Doto 价格、状态和走势图。
- GoldHistoryStore：有限采样缓存，同价五分钟补点。
- GoldChartWidget / GoldLightWidget：桌面宿主与点击动作。

详细改动和踩坑记录见 PROJECT_NOTES.md；本组件设计约束见 DESIGN_SPEC.md。

数据源：https://hq.sinajs.cn/list=hf_XAU,gds_AU9999。第三方报价可延迟或暂停，本组件不提供交易执行功能。

MIT；Doto 使用 SIL OFL 1.1。
