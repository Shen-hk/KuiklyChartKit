# 发布与贡献说明

## 本地构建

Android 的基础检查在仓库根目录执行：

```powershell
.\gradlew :chartkit:build
.\gradlew :androidApp:assembleDebug
```

H5 宿主已接入官方 Kuikly Web Render。生产站点构建与本地运行命令为：

```powershell
.\gradlew :h5App:publishChartShowcase
.\gradlew :h5App:jsBrowserProductionRun
```

生产产物位于 `h5App/build/dist/js/productionExecutable`，本地访问地址为 `http://localhost:8080/`。任务会自动先构建 `chartkit` 业务 JS，再按“业务包先注册页面、Web Render 后挂载”的顺序生成站点，无需手工复制文件。

当前 M0 的推荐发布前门禁：

```powershell
.\gradlew :chartkit:testDebugUnitTest
.\gradlew :chartkit:compileDebugKotlinAndroid
.\gradlew :chartkit:compileKotlinJs
.\gradlew :androidApp:assembleDebug
.\gradlew :h5App:publishChartShowcase
```

Kotlin 2.1 的 Android 构建要求 D8/R8 8.6.17 或更高。本项目在根 `build.gradle.kts` 覆盖 AGP 7.4.2 自带的旧 R8，只替换 D8/R8 编译器，不升级 Kuikly、Kotlin、Gradle 或公共 API。`androidApp-debug.apk` 已按上述门禁成功产出。

## 版本与变更

模块版本由 `kuiklyBizVersion` 环境变量控制，缺省为 `1.0.0`。遵循语义化版本：修复使用补丁版本，兼容的新 DSL/API 使用次版本，删除或改变已发布行为使用主版本。

涉及 `ChartViewport`、Tracker 回调、K 线数据校验或数据更新语义的改动，必须在变更日志中说明兼容性、默认行为与迁移方式。新增缩放、K 线或 H5 能力前，先完成 Android/H5 探针和回归，不以“理论可行”代替验证。

## 贡献要求

- 公共模型、DSL、Renderer、手势或数据更新逻辑的改动必须附测试、KDoc 和相关文档。
- 不将平台原生类型泄漏到 `commonMain` 公共 API；数据模型必须能被 Android 与 H5 共用。
- 价格、成交量、十字光标和 Tooltip 的绘制常量集中在 Theme/Style 模型，禁止 Renderer 魔法数字。
- K 线新逻辑必须覆盖合法 OHLC、非法 OHLC、重复时间戳、动态更新和视口边界。
- 一次 PR 聚焦一项能力；DSL 或回调改动需说明兼容性与示例迁移。
- 提交前构建受影响模块，并在 PR 填写 Android 与 H5 验证状态；未验证的端必须明确标注。

## 候选发布清单

- [ ] 折线图、柱状图和 Kuikly 风格 DSL 已实现并有 KDoc。
- [ ] 十字光标、Tooltip、滑动选点、平移和缩放的能力与降级策略已验证。
- [ ] K 线与成交量组合的 OHLC、视口和 Tracker 语义通过 Android/H5 测试。
- [ ] 数据更新、边界数据、密集数据及视觉回归完成。
- [ ] Android 与 H5 Showcase 均可按文档从干净环境运行。
- [ ] 公共 API、示例、KDoc、变更日志和实际实现一致。
- [ ] Kuikly AI 股票行情 Demo 已在组件通过门禁后真实接入，并附运行证据。
- [ ] Issue/README 提供仓库地址、双端截图/录屏、测试记录和已知限制导航。
