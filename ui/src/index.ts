import { VLoading } from '@halo-dev/components'
import { definePlugin } from '@halo-dev/console-shared'
import 'uno.css'
import { defineAsyncComponent } from 'vue'

export default definePlugin({
  extensionPoints: {
    'plugin:self:tabs:create': () => {
      return [
        {
          label: '订阅源列表',
          id: 'sources',
          component: defineAsyncComponent({
            loader: () => import('./components/SourcesTab.vue'),
            loadingComponent: VLoading,
          }),
          permissions: ['*'],
        },
      ]
    },
  },
})
