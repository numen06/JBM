import { transform } from 'lightningcss'

export const chrome109Build = { target: 'chrome109', cssTarget: 'chrome109', cssMinify: 'lightningcss' }

// Tailwind's palette is static; keep dynamic HSL application tokens dynamic.
// Resolving palette opacity during PostCSS also keeps the final asset hash correct.
export function chrome109CssCompatibility() {
  return {
    postcssPlugin: 'jbm-chrome109-colors',
    Once(root) {
      const palette = new Map()
      const cache = new Map()
      root.walkDecls(/^--color-/, declaration => palette.set(declaration.prop, declaration.value))
      root.walkDecls(declaration => {
        // Tailwind stores interpolation mode inside a variable, beyond the CSS
        // compiler's gradient lowering; Chrome 109 rejects the entire image.
        if (declaration.prop === '--tw-gradient-position') {
          declaration.value = declaration.value.replace(/\s+in\s+oklab\b/g, '')
        }
        declaration.value = declaration.value.replace(/linear-gradient\(([^,()]*)\s+in\s+oklab\s*,/g, 'linear-gradient($1,')
        declaration.value = declaration.value.replace(
          /color-mix\(\s*in\s+(srgb|oklab)\s*,\s*(var\((--[\w-]+)\)|hsl\(var\((--[\w-]+)\)\))\s+([\d.]+)%\s*,\s*transparent\s*\)/g,
          (original, space, _color, paletteToken, hslToken, percent) => {
            if (hslToken) return `hsl(var(${hslToken}) / ${Number(percent) / 100})`
            const color = palette.get(paletteToken)
            const themeToken = color?.match(/^hsl\(var\((--[\w-]+)\)\)$/)?.[1]
            if (themeToken) return `hsl(var(${themeToken}) / ${Number(percent) / 100})`
            if (!color || color.includes('var(')) return original
            const key = `${space}:${color}:${percent}`
            if (!cache.has(key)) {
              const result = transform({
                filename: 'palette-opacity.css',
                code: Buffer.from(`.x{color:color-mix(in ${space},${color} ${percent}%,transparent)}`),
                targets: { chrome: 109 << 16 }, minify: true,
              }).code.toString()
              const lowered = result.match(/color:([^;}]+)/)?.[1]
              cache.set(key, lowered && !lowered.includes('color-mix') ? lowered : original)
            }
            return cache.get(key)
          },
        )
      })
      root.walkAtRules('supports', rule => {
        // Once all guarded colors have an equivalent legacy value, retain the
        // utility's alpha instead of falling back to Tailwind's opaque base.
        if (!/^\(\s*color:\s*color-mix\(\s*in\s+lab\s*,\s*red\s*,\s*red\s*\)\s*\)$/.test(rule.params)) return
        let supported = true
        rule.walkDecls(declaration => { if (declaration.value.includes('color-mix(')) supported = false })
        if (supported) rule.replaceWith(...rule.nodes)
      })
    },
  }
}
