import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig(({ mode }) => {
    const env = loadEnv(mode, process.cwd(), '')
    const proxy = {
        '/management': {
            target: env.MANAGEMENT_URL || 'http://127.0.0.1:8081',
            changeOrigin: true,
            rewrite: (path) => path.replace(/^\/management/, ''),
        },
        '/processing': {
            target: env.PROCESSING_URL || 'http://127.0.0.1:8082',
            changeOrigin: true,
            rewrite: (path) => path.replace(/^\/processing/, ''),
        },
    }
    return {
        plugins: [vue()],
        server: { port: 5173, strictPort: true, proxy },
        preview: { port: 5173, strictPort: true, proxy },
    }
})
