# @jbm7/vue-core

JBM 7.3 的 Vue 3 插件、权限守卫和前端模块注册契约。业务扩展只依赖这里的公共类型，不引用 JBM 管理端内部源码。

```ts
import { defineJbmModule } from '@jbm7/vue-core'

export const industryModule = defineJbmModule({
  id: 'example.industry',
  version: '1.0.0',
  routes: [{ path: 'industry', name: 'example-industry', component: IndustryPage }],
})
```

模块 ID、路由名和菜单码应使用稳定命名空间；平台与行业包独立版本、独立发布。

## 统一设计底座（beta.6）

宿主依次引入 JBM 样式、自身布局样式，最后引入 `@jbm7/vue-core/design.css`。
该入口为工业暗色设计，统一 shadcn 变量、表面、文字、控件、指标卡、图表色和减少动态效果规则。
公共主题只在此维护，宿主不复制变量；地图与大屏保持自己的布局。

Chrome 109 构建基线：Vite 的 `build` 使用 `@jbm7/vue-core/vite-compat` 导出的 `chrome109Build`，PostCSS 使用 `chrome109CssCompatibility()`。宿主需安装 `lightningcss`。兼容处理在构建时解析 Tailwind 固定色板的透明配色，动态 HSL 主题继续随应用切换；自定义运行时混色仍需提供明确的 CSS 回退。禁止依赖 `URLSearchParams.size` 拼接查询参数，使用 `toString()` 判断。

`@jbm7/vue-core/design` 提供 `chartPresentation`、`chartSeriesColor` 和 `formatChartNumber`，不引入 ECharts 依赖。
`JbmSegmentedControl` 是可控按钮组，接收 `modelValue/options/label`，通过 `update:modelValue` 通知选择，带滑动指示器和 reduced-motion 支持。

样式契约：`jbm-panel`、`jbm-control`、`jbm-input`、`jbm-metric`、`jbm-chart`、`jbm-chart-bar`、`jbm-interactive`。
只给真正可交互的对象添加 `jbm-interactive`；不能通过动画判定设备控制成功。
