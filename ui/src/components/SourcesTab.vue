<script lang="ts" setup>
import { axiosInstance } from "@halo-dev/api-client";
import { IconInformation, VEntity, VEntityField, VLoading } from "@halo-dev/components";
import { useQuery } from "@tanstack/vue-query";

interface Source {
  displayName: string;
  description: string;
  pattern: string;
  example: string;
}

const { data, isLoading } = useQuery({
  queryKey: ["plugin:PluginFeed:sources"],
  queryFn: async () => {
    const { data } = await axiosInstance.get<Source[]>("/apis/api.feed.halo.run/v1alpha1/rss-sources");
    return data;
  },
});
</script>

<template>
  <div class="m-4">
    <VLoading v-if="isLoading" />
    <div v-else class="overflow-hidden border rounded">
      <ul class="box-border h-full w-full divide-y divide-gray-100" role="list">
        <li v-for="source in data" :key="source.pattern">
          <VEntity class="group">
            <template #start>
              <VEntityField :title="source.displayName" :description="source.pattern">
                <template #extra>
                  <IconInformation
                    v-tooltip="source.description"
                    class="hidden size-4 text-gray-600 group-hover:block"
                  />
                </template>
              </VEntityField>
            </template>
          </VEntity>
        </li>
      </ul>
    </div>
  </div>
</template>
