import unocss from '@unocss/eslint-config/flat'
import skipFormatting from '@vue/eslint-config-prettier/skip-formatting'
import { defineConfigWithVueTs, vueTsConfigs } from '@vue/eslint-config-typescript'
import pluginVue from 'eslint-plugin-vue'
import { globalIgnores } from 'eslint/config'

export default defineConfigWithVueTs(
  globalIgnores(['**/dist/**']),

  pluginVue.configs['flat/essential'],
  vueTsConfigs.recommended,
  unocss,

  {
    name: 'app/files-to-lint',
    files: ['**/*.{ts,mts,tsx,vue}'],
    rules: {
      'unocss/enforce-class-compile': 2,
    },
  },

  skipFormatting,
)
