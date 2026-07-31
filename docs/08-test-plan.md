# 测试计划

## 测试分层

| 层级 | 位置/方式 | 关注点 |
| --- | --- | --- |
| 单元测试 | `chartkit/src/commonTest` | 数据域、漂亮刻度、坐标映射、标签取舍、坏数据过滤、命中测试。 |
| 组件测试 | 组件模块或平台测试 | DSL 到 RenderPlan、状态更新、回调与 Theme 优先级。 |
| 视觉回归 | 正式平台截图对比 | 轴、网格、文字、裁剪、颜色、Tooltip 和暗色主题。 |
| 人工验收 | 真机/模拟器 | 触摸、生命周期、性能和 Showcase 完整性。 |

## 必测用例

| ID | 场景 | 预期 |
| --- | --- | --- |
| TC-01 | 正常多系列折线 | 点位、连线、轴、网格、图例与标签正确。 |
| TC-02 | 单点/空数据 | 不崩溃；按规则展示单点或空态。 |
| TC-03 | 相同值、负值、跨零 | 不除零；基线和刻度正确。 |
| TC-04 | 柱色与数值标签 | 指定颜色和格式化文本生效。 |
| TC-05 | 超长/中英文标签 | 无重叠、无越界，按规则隐藏或截断。 |
| TC-06 | 点击首、末和相邻项 | 命中项和原始索引准确，Tooltip 不越界。 |
| TC-07 | 数据更新/替换 | 图形更新，失效选择被清除。 |
| TC-08 | 非法数据与 DSL 参数 | 外部坏数据安全处理；配置错误说明字段。 |
| TC-09 | 尺寸变化、前后台 | 重布局后无错位、无崩溃。 |
| TC-10 | 长按 `start/move/end` | 追踪线、Tooltip 和 `onTrackerChanged` 的 X 槽一致；已完成纯逻辑、Android/JS 编译与 Showcase 手势验收。 |
| TC-11 | 水平平移与外层滚动 | 仅超出容量且显式开启时平移，窗口在首尾钳制，不抢普通滚动；已完成纯逻辑、Android/JS 编译与 Showcase 手势验收。 |
| TC-12 | 分组/堆叠/负值柱 | 宽度、总值、零基线与标签正确。 |
| TC-13 | 扩展图表 | 面积、饼环、组合图和 Sparkline 已覆盖 DSL、布局、命中、默认值与坏值。 |
| TC-14 | 100/1,000/5,000 点 | 已完成 JVM 自动化基线：绘制点数为 100/240/240，峰谷、顺序、坏点分段、全系列上限与原始索引均有断言。100 次平均采样为 4/346/446 μs；该结果不是设备帧率。 |
| TC-15 | P2 热力图与雷达图 | 热力图分类顺序、颜色归一化、单元格命中和坏值过滤正确；雷达共享维度、极坐标边界、断点、顶点命中和原始索引正确。 |
| TC-16 | 实验性图片导出 | `sampleSize` 校验、成功/失败回调解析、一次性回调不保留数据；Android/iOS/OpenHarmony 分别验证导出、分享、内存和宿主文件清理。 |
| TC-17 | 简洁数据 DSL | Point/Bar/Pie/Radar 的嵌套构建器生成正确不可变快照，自动 X 坐标正确，且仍复用空名称等现有校验。 |

## 当前自动化证据

- `.\gradlew :chartkit:jsTest`：78 项浏览器测试，0 failure、0 error；包含简洁数据 DSL、P2 热力图、雷达图和图片导出解析覆盖。
- `.\gradlew :h5App:publishChartShowcase`：通过，生成实验性 H5 Showcase。
- `.\gradlew :androidApp:assembleDebug`：通过，生成 Android Debug APK。
- Performance Lab 可切换 100、1,000、5,000 点，并显示输入点数、实际绘制点数、采样状态；点击回调显示采样前的原始索引。
- 异常数据回归卡包含 `NaN` 和正无穷值，用于人工确认断线且不崩溃。
- P2 公共代码、测试源码与完整 JS 测试已分别通过 `.\gradlew :chartkit:compileKotlinJs`、`.\gradlew :chartkit:compileTestKotlinJs` 和 `.\gradlew :chartkit:jsTest`；包含 P2 卡片的 `.\gradlew :h5App:publishChartShowcase` 和完整 `.\gradlew :chartkit:testDebugUnitTest` 也已通过。移除 Kuikly 插件注入的全局 `ksp` 处理器后，`jsTest` 不再生成第二个 `callKotlinMethod` 入口；Android 单测已在停止旧 Gradle daemon 后恢复，38 项 JVM 测试全部通过。

## 发布门禁

Android、iOS、OpenHarmony 均跑完 TC-01 至 TC-17。记录设备、系统、Kuikly 版本、构建号、截图/录屏和已知差异；任一端缺证据即不能称为发布候选。所有单元测试、Showcase 页面、API/KDoc 和变更说明必须同步通过。

H5 验证可作为额外工程证据，但不替代正式平台门禁；若后续扩大为正式支持，先补充浏览器矩阵、自动化测试和验收条目。
