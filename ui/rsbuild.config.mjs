import { rsbuildConfig } from '@halo-dev/ui-plugin-bundler-kit'
import { UnoCSSRspackPlugin } from '@unocss/webpack/rspack'

export default rsbuildConfig({
  manifestPath: '../app/src/main/resources/plugin.yaml',
  rsbuild: {
    tools: {
      rspack: {
        plugins: [UnoCSSRspackPlugin()],
      },
    },
  },
})
