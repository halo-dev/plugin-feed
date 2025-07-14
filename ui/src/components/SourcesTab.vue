<script lang="ts" setup>
import { axiosInstance } from '@halo-dev/api-client'
import {
  IconInformation,
  VEntity,
  VEntityContainer,
  VEntityField,
  VLoading,
} from '@halo-dev/components'
import { useQuery } from '@tanstack/vue-query'

interface Source {
  displayName: string
  description: string
  pattern: string
  example: string
}

const { data, isLoading } = useQuery({
  queryKey: ['plugin:PluginFeed:sources'],
  queryFn: async () => {
    const { data } = await axiosInstance.get<Source[]>(
      '/apis/api.feed.halo.run/v1alpha1/rss-sources',
    )
    return data
  },
})
</script>

<template>
  <div class=":uno: m-4">
    <VLoading v-if="isLoading" />
    <div v-else class=":uno: overflow-hidden border rounded">
      <VEntityContainer>
        <VEntity v-for="source in data" :key="source.pattern" class=":uno: group">
          <template #start>
            <VEntityField :title="source.displayName" :description="source.pattern">
              <template #extra>
                <IconInformation
                  v-tooltip="source.description"
                  class=":uno: hidden size-4 text-gray-600 group-hover:block"
                />
              </template>
            </VEntityField>
          </template>
        </VEntity>
      </VEntityContainer>
    </div>
  </div>
</template>
