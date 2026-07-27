# 发布与贡献说明

## 本地构建

在仓库根目录执行：

```powershell
.\gradlew :chartkit:build
.\gradlew :androidApp:assembleDebug
```

iOS 使用 `iosApp/Podfile` 关联 `chartkit`，OpenHarmony 使用 `ohosApp` 工程。H5 Showcase 可通过其专用 Gradle 任务构建，作为额外验证；CI 确定后补充完全可复制的 iOS/OpenHarmony 构建命令、工具版本与设备要求。

## 版本与变更

模块版本由 `kuiklyBizVersion` 环境变量控制，缺省为 `1.0.0`。遵循语义化版本：修复用补丁版本，兼容的新 DSL/API 用次版本，删除或改变已发布行为用主版本。

每次发布至少包含版本号、变更日志、已知限制、正式平台验证证据、Showcase 截图和录屏、最小接入片段，以及可供 Issue 验收的仓库/制品地址。若升级 Kuikly 以支持导出、候选手势等能力，必须独立记录升级理由和完整回归结果。

## 贡献要求

- 公共模型、DSL 与 Renderer 的改动必须附测试和文档。
- 不将平台原生类型泄漏到 `commonMain` 公共 API。
- 绘制常量集中在 Theme/Style 模型，禁止 Renderer 魔法数字。
- 一次 PR 聚焦一项能力；架构/DSL 改动说明兼容性与迁移路径。
- 提交前构建受影响模块，并在 PR 填写 Android、iOS、OpenHarmony 验证状态；H5 结果作为补充记录。

## Issue 交付清单

- [ ] 折线图、柱状图与官方风格 DSL 已实现。
- [ ] 候选交互和扩展图表不影响 P0 的三端稳定性。
- [ ] API KDoc、文档、示例与实际代码一致。
- [ ] 三端验收矩阵、测试、截图和录屏完整。
- [ ] Showcase 覆盖核心、交互、扩展图表和大数据场景。
- [ ] 在 Issue 中提交最终仓库地址与证据导航。
