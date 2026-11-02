import { defineConfig } from 'vite';
import cljf from 'clojurific/vite';

export default defineConfig(({ command }) => ({
  server: { port: 8094, strictPort: true },
  preview: { port: 8094, strictPort: true },
  plugins: [
    cljf({
      outputDir: 'target/cljs',
      compilerOptions: {
        // Writes the CSS of uni.style's macros after every compile, which the
        // bundle-css link in index.html loads (and reloads in development).
        buildHooks: [["'uni.style.build/esm-hook", { outputTo: 'public/css/bundle.css' }]],
        // Start an nREPL server for npm run repl and editors. Port 0 picks a
        // free port, written to .nrepl-port, where they find it.
        ...(command === 'serve' && { repl: { nreplPort: 0 } }),
      },
    }),
  ],
}));
