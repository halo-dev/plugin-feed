import { defineConfig, presetUno, transformerCompileClass, transformerDirectives } from "unocss";
export default defineConfig({
  presets: [presetUno()],
  transformers: [transformerDirectives(), transformerCompileClass()],
});
