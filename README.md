# Android Gold Widget 🪙

一个 Android 桌面小组件，实时显示**国际金价**与**国内金价**，并绘制价格走势折线图。

专为个人盯盘设计：黄金买入/卖出的价格波动是实时的，与其频繁打开 App 查看，不如直接在桌面放一个组件，随时瞄一眼。

## 功能特性

- **实时金价**：国际伦敦金（美元/盎司）+ 国内 AU9999（元/克），约 1 秒刷新
- **价格走势折线图**：Y 轴价格刻度 + X 轴时间刻度 + 右侧"未来时间"留白（仿支付宝分时图）
- **历史最高价**：虚线标注历史最高价，当前价为历史新高时高亮变红
- **当前价标签**：折线末端圆点 + 当前价格数值
- **浅色半透明主题**：毛玻璃白效果，可透出桌面壁纸
- **前台服务**：开机自启 + 服务自愈，持续刷新

## 截图

（组件的实际显示效果）

## 技术栈

- **Kotlin**
- **Jetpack Glance**（现代 AppWidget 框架，Compose 语法）
- **OkHttp** + 手写解析（新浪返回 JS 字符串，无需 JSON 库）
- **DataStore Preferences**（状态持久化）
- **Foreground Service**（1 秒实时刷新）

## 数据源

新浪财经公开行情接口（免费、无需注册、无需 API Key）：

```
https://hq.sinajs.cn/list=hf_XAU,gds_AU9999
```

- `hf_XAU`：伦敦现货黄金（国际金价，美元/盎司，24 小时连续）
- `gds_AU9999`：上海黄金交易所 AU9999（国内金价，元/克，交易时段变动）

## 项目结构

```
app/src/main/java/com/unimend/goldwidget/
├── GoldPrice.kt            # 数据模型 + JSON 序列化
├── GoldPriceRepository.kt  # 新浪接口抓取 + 解析
├── GoldHistoryStore.kt     # 历史价格缓存（480 点）
├── GoldChartRenderer.kt    # 折线图绘制（Y轴/X轴/最高价/当前价/新高）
├── GoldChartWidget.kt      # 组合组件 UI（浅色半透明主题）
├── GoldUiUtil.kt           # 配色 + 格式化工具
├── GoldRefreshService.kt   # 前台服务（1 秒轮询 + 自愈）
├── BootReceiver.kt         # 开机自启
└── MainActivity.kt         # 引导页
```

## 构建

```bash
# 需 JDK 17 + Android SDK
./gradlew assembleDebug
```

或直接用 Android Studio 打开工程运行。

## 使用

1. 安装后打开一次 App（启动前台服务 + 授权通知）
2. 桌面长按 → 小组件 → 找到「黄金走势图」→ 拖到桌面
3. 组件约 1 秒自动刷新，点击可手动刷新

## 已知问题与注意

- **刷新依赖网络**：需手机联网，否则组件无数据（排查时先 `ping baidu.com` 确认网络）
- **后台限制**：部分厂商 ROM（如 ColorOS）会杀后台服务，需在「电池 → 应用耗电管理」中将本应用设为"允许后台运行"
- **国内金价休市**：上金所非交易时段（凌晨/周末）AU9999 价格静止，属正常现象
- **半透明限制**：Glance 不支持真正的毛玻璃模糊，仅能做颜色 alpha 半透明

## License

MIT
