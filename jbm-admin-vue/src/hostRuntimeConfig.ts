import { configureRuntimeConfig } from '@/runtimeConfig'

configureRuntimeConfig(typeof window === 'undefined' ? {} : window.JBM_ADMIN_CONFIG)
