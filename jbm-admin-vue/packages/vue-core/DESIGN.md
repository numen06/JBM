# JBM / IoT / 建筑平台统一 UI 规范

本项目的设计约定，不是声称 shadcn 自带完整的业务视觉规范。
实现唯一来源：`design.css`；三端通过 `@jbm7/vue-core/design.css` 引用。

## 依据

- [shadcn 语义主题变量](https://ui.shadcn.com/docs/theming)：组件使用语义 token，页面不另造品牌色。
- [Radix 色阶用途](https://www.radix-ui.com/colors/docs/palette-composition/understanding-the-scale)：区分底色、交互、边框、实色与文字。这里借鉴分工，不冒充 Radix 原始色板。
- [WCAG 文本对比度](https://www.w3.org/WAI/WCAG21/Understanding/contrast-minimum)：普通文字目标至少 4.5:1，大文字至少 3:1。数据图形与面板目标至少 3:1；已增加基础色对比测试，不代表全部页面已经通过无障碍认证。

## 绿色层级

保留品牌绿 `#00ff88`，只作为母色；梯度首先是用途和强弱层级，不是给所有卡片加渐变。

| 场景 | 共享 token | 规则 |
| --- | --- | --- |
| 图标小底、菜单选中底 | accent-soft | 品牌绿 8% |
| 悬停底 | accent-bg-hover | 12% |
| 选中项悬停 | accent-selected | 16% |
| 常规强调边框 | accent-border | 24% |
| 强调细线 | accent-border-strong | 40% |
| 数据排行条 | energy-bar | 70%，不抢主操作 |
| 图标 | accent-ink | 85% |
| 主操作、焦点、选中标识 | accent-solid | 实色，按钮用黑色文字 |
| 主按钮悬停 / 按下 | solid-hover / solid-pressed | 同色提亮 / 压暗，不换色相 |
| 能耗柱体 | energy-bar-top / bottom | 上 82% → 下 50%，仅柱体使用纵向渐变 |

错误、警告、信息使用各自语义色，不按品牌绿强行替换；状态不能只依赖颜色，保留文字或图标。
多系列图表仍用稳定的系列颜色，完整数值和单位不缩写；零值与无数据分开。
禁止大面积彩色卡片、装饰性光斑、彩虹渐变和整页漂浮动画。

## 字体、密度和交互

- 页面标题 22/30，区块标题 16/24，菜单/正文/表格 14/20，按钮与说明 13/20，辅助标签 12/18。
- 页面边距桌面 24、移动端 12；面板圆角 10、控件圆角 8；间距采用 4/8/12/16/24。
- 页面与卡片保持中性深灰；状态强调限定在图标、细线和小标签。
- 普通反馈 140ms，布局与切换 220ms，柱体首次进入 420ms；尊重减少动态效果设置。
- 所有工作区使用共享标题、指标卡、导航组件；同一个 IoT 工作区只由 IoT 前端提供菜单。首入、刷新、旧书签与应用切换结果相同。

## 验收

工作区必须挂载 `JbmWorkspaceShell`，通过 sidebar/context/actions/default/overlays 插槽提供业务内容，禁止各宿主复制 aside/main/折叠按钮。共享 288px 侧栏、64px 顶栏、40px 导航行、稳定滚动条占位；手机使用同一个抽屉，关闭时退出焦点链，Escape 返回打开按钮。集成工作区默认 Logo 统一为 `/images/brand/building-logo.png`。独立 JBM 外壳也使用该组件；主题遵循同一设计底座，不再提供与实际深色底座冲突的独立皮肤切换。

修改共享 token 后同时构建三端，运行共享核心、导航和布局回归；浏览器检查菜单切换、选中/悬停/按下、数据柱图、窄屏和减少动态效果。
独立 ZIP 大屏和地图业务画布不由本 CSS 全局覆盖；单独适配时也应沿用语义原则。
