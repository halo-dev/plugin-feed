import { definePlugin } from "@halo-dev/console-shared";
import { markRaw } from "vue";
import SourcesTab from "./components/SourcesTab.vue";

export default definePlugin({
  extensionPoints: {
    "plugin:self:tabs:create": () => {
      return [
        {
          label: "订阅源列表",
          id: "sources",
          component: markRaw(SourcesTab),
          permissions: ["*"],
        },
      ];
    },
  },
});
