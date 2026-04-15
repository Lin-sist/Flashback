import { defineConfig } from 'vite'
import uniPluginPackage from '@dcloudio/vite-plugin-uni'

const uniPlugin = (uniPluginPackage as unknown as { default: () => unknown[] }).default

export default defineConfig({
  plugins: uniPlugin(),
})
