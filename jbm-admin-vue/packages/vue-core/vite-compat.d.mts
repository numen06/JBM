import type { Plugin } from 'postcss'
export const chrome109Build: { target: string; cssTarget: string; cssMinify: 'lightningcss' }
export function chrome109CssCompatibility(): Plugin
