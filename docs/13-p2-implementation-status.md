# P2 实现与验收记录

> 热力图与雷达图当前也支持 `data { cell(...) }` 和 `data { series { metric(...) } }`。本文件保留 P2 验收快照；完整 API 以 [公共 API](06-public-api-reference.md) 为准。

> 启动日期：2026-07-27  
> 当前批次：P2-1 / 极坐标与矩阵图表、实验性导出封装  
> 结论：热力图、雷达图和公共导出封装已完成代码、完整 JS 测试、H5 打包与 38 项 Android 单元测试验证；图片导出和 Android/iOS/OpenHarmony 真机视觉证据尚未收口，不能作为发布候选能力。

## 1. 本批次交付

| 能力 | 状态 | 实现说明 |
| --- | --- | --- |
| `HeatmapChart` DSL | 已实现 | 新增 `HeatmapEntry`、`HeatmapChartAttr/Event/View` 和 `ViewContainer.HeatmapChart`。 |
| 热力图布局 | 已实现 | 按输入首次出现顺序建立二维 X/Y 分类网格；有限值归一化到离散颜色桶，同坐标重复项在 DSL 构建期拒绝。 |
| 热力图交互 | 已实现 | 单元格包围盒命中，回调保留原始 `itemIndex`；Tooltip 显示系列、X/Y 分类和值。 |
| `RadarChart` DSL | 已实现 | 新增 `RadarEntry`、`RadarChartAttr/Event/View` 和 `ViewContainer.RadarChart`。 |
| 雷达图布局 | 已实现 | 多系列共享至少三个、顺序一致且唯一的维度；从零开始计算漂亮刻度，多边形网格与轴由纯 Kotlin 布局生成。 |
| 雷达图交互 | 已实现 | 仅命中可见顶点；负值或 `NaN/Infinity` 形成断点，不跨断点闭合，回调保留原始索引。 |
| P2 Showcase | 已实现 | `chart_showcase` 新增“客服时段热度”和“服务能力对照”，支持 Light/Dark/Ocean/Sunset 主题和卡片内反馈。 |
| `exportImage` | 实验性已封装 | 通过现有 `toImage` 入口包装为一次性异步回调；默认 `DATA_URI`，ChartKit 不保留缓存 key、回调或结果数据。 |

## 2. 自动化与构建记录

| 检查 | 结果 | 说明 |
| --- | --- | --- |
| `.\gradlew :chartkit:compileKotlinJs` | 通过 | P2 公共模型、布局、Canvas 视图、导出封装和 Showcase 均完成 Kotlin/JS 编译。 |
| `.\gradlew :chartkit:compileTestKotlinJs` | 通过 | P2 布局、命中、DSL 校验和导出响应解析测试源码编译通过。 |
| `.\gradlew :h5App:publishChartShowcase` | 通过 | P2 Showcase 完成生产 JS 链接和 Webpack 打包；仅有既有 bundle 体积建议警告。 |
| `.\gradlew :chartkit:jsTest` | 通过 | Kuikly 插件注入的全局 `ksp` 处理器会为 `jsTest` 生成第二个 `callKotlinMethod` 入口；清空该全局配置并保留目标级 KSP 处理器后，干净构建与完整 JS 测试均通过。 |
| `.\gradlew :chartkit:testDebugUnitTest` | 通过 | 停止两个后台 Gradle daemon 后成功生成 `R.jar`；38 项测试，0 failure、0 error，包含 P2 布局、命中、DSL 和导出解析测试。 |

## 3. P2 逻辑验收覆盖

1. 热力图验证输入分类顺序、有限值过滤、同值中间颜色桶、格子几何和单元格命中。
2. 雷达图验证共享维度、半径与轴方向、完整多边形、负值/坏值断点、顶点命中和原始索引。
3. DSL 验证热力图重复坐标、负间距、雷达维度错序、非法网格数量。
4. 图片导出验证 `sampleSize >= 1`、原生成功/失败响应解析和无可用数据时的稳定错误消息。

## 4. 图片导出门槛

当前工程固定 Kuikly `2.7.0-2.1.21`。本地源码可见 `toImage` 调用入口，但注释没有给出 Android/iOS/OpenHarmony 的统一支持保证。P2 仅提供标注为 `ExperimentalChartImageExportApi` 的包装层，不声明跨端导出。

正式开放前必须完成：

1. 选择并记录兼容的 Kuikly 升级版本、升级原因与完整 P0/P1 回归结果。
2. 在 Android、iOS、OpenHarmony 分别验证 `DATA_URI`、`FILE`、失败回调、连续导出和宿主文件清理。
3. 记录设备型号、系统、Kuikly 版本、构建号、内存观测、截图/录屏与已知差异。
4. 在干净构建环境完成 P2 单元测试、H5 Showcase 和 Android Debug 构建。

## 5. 发布结论

- 热力图和雷达图的公共 API、KDoc、Showcase、纯逻辑布局与 38 项 Android 单元测试已同步通过。
- 当前 P2 不得因代码编译通过而替代 Android、iOS、OpenHarmony 的视觉与交互验收。
- `exportImage` 保持实验性；在升级和三端真机证据补齐前，不得写入正式支持矩阵或发布说明。
