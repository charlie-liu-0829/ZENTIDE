import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    historyApiFallback: true, // 确保所有路由回退到 index.html
    hmr: true,
    host: '0.0.0.0',
    port: 6001,
    proxy: {
      '/api': {
        target: 'http://localhost:6050/',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '/api'),
      },
    },
  },
  build: {
    chunkSizeWarningLimit: 650,
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (!id.includes('node_modules')) return undefined
          if (id.includes('@tiptap') || id.includes('prosemirror')) return 'editor'
          if (id.includes('element-plus')) return 'element-plus'
          if (id.includes('/vue/') || id.includes('vue-router') || id.includes('pinia')) return 'vue-vendor'
          return 'vendor'
        },
        // 1. 处理入口文件（Entry chunks）名称
        entryFileNames: 'assets/[hash].js', // 所有入口文件使用哈希命名
        // 2. 处理代码分割产生的块文件（Chunk chunks）名称
        chunkFileNames: 'assets/[hash].js', // 所有代码分割块也使用哈希命名
        // 3. 处理静态资源文件（如 CSS、图片）名称
        assetFileNames: (assetInfo) => {
          // 获取文件扩展名
          const extType = assetInfo.name?.split('.').pop()
          // 如果是 CSS 文件
          if (extType === 'css') {
            return 'assets/[hash].[ext]' // CSS 文件使用哈希命名
          }
          // 其他资源（如图片、字体）也使用类似规则，可以按需调整
          return 'assets/[hash].[ext]'
        },
      },
    },
  },
})
