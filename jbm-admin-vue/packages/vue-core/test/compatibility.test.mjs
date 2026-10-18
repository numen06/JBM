import assert from 'node:assert/strict'
import test from 'node:test'
import postcss from 'postcss'
import { chrome109Build, chrome109CssCompatibility } from '../vite-compat.mjs'

test('palette opacity compiles for Chrome 109 and HSL theme tokens remain dynamic', async () => {
  const source = ':root{--color-blue-500:oklch(.623 .214 259.815)}'
    + '.bar{background:color-mix(in oklab,var(--color-blue-500) 25%,transparent)}'
    + '.selected{background:color-mix(in oklab,hsl(var(--primary)) 16%,transparent)}'
  const { css } = await postcss([chrome109CssCompatibility()]).process(source, { from: undefined })
  assert.doesNotMatch(css, /color-mix/)
  assert.match(css, /hsl\(var\(--primary\) \/ 0\.16\)/)
  assert.match(css, /\.bar\{background:(?:#[\da-f]+|rgba?\()/i)
  assert.equal(chrome109Build.target, 'chrome109')
  assert.equal(chrome109Build.cssTarget, 'chrome109')
})

test('unknown runtime color tokens are preserved for explicit theme fallbacks', async () => {
  const source = '.x{background:color-mix(in srgb,var(--custom-color) 30%,transparent)}'
  const { css } = await postcss([chrome109CssCompatibility()]).process(source, { from: undefined })
  assert.equal(css, source)
})

test('gradient direction variables do not carry unsupported interpolation into Chrome 109', async () => {
  const source = '.bar{--tw-gradient-position:to top in oklab;background-image:linear-gradient(var(--tw-gradient-position),#7652cc,#67e8f9)}'
    + '.direct{background:linear-gradient(to bottom in oklab,#7652cc,#67e8f9)}'
  const { css } = await postcss([chrome109CssCompatibility()]).process(source, { from: undefined })
  assert.doesNotMatch(css, /in oklab/)
  assert.match(css, /--tw-gradient-position:to top;/)
  assert.match(css, /linear-gradient\(to bottom,/)
})

test('resolved opacity utilities stay active in 109 without turning selected controls opaque', async () => {
  const source = ':root{--color-primary:hsl(var(--primary))}'
    + '.selected{background:var(--color-primary);@supports (color:color-mix(in lab,red,red)){background:color-mix(in oklab,var(--color-primary) 15%,transparent)}}'
  const { css } = await postcss([chrome109CssCompatibility()]).process(source, { from: undefined })
  assert.doesNotMatch(css, /@supports|color-mix/)
  assert.match(css, /hsl\(var\(--primary\) \/ 0\.15\)/)
})
