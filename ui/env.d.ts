/// <reference types="@rsbuild/core/types" />
/// <reference types="unplugin-icons/types/vue" />
/// <reference types="vue-i18n/dist/vue-i18n.d.ts" />

export {}

declare module 'axios' {
  export interface AxiosRequestConfig {
    mute?: boolean
  }
}

declare module '*.vue' {
  import type { ComponentOptions } from 'vue'
  const Component: ComponentOptions
  export default Component
}

declare module '*.md' {
  import type { ComponentOptions } from 'vue'
  const Component: ComponentOptions
  export default Component
}

declare module 'vue' {
  interface ComponentCustomProperties {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    $formkit: any
  }
}
