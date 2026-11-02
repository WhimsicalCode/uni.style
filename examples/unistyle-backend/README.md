# uni.style backend

A small, server-rendered todo list built with **Ring + Jetty, Reitit, Hiccup 2,
and [`uni.style`](../../README.md)**. Todos live in a process-local atom.
There is no client-side JavaScript, React, Shadow-CLJS, or database.

## Run

Requires Java and the Clojure CLI. From this directory:

```bash
clojure -M:dev
```

Open **http://localhost:8093**. The server binds to loopback only.

Development mode reloads changed `src/` namespaces and regenerates CSS on the
next request. Just save and refresh the browser—no restart or client-side script.
CSS is built on the first request, then reused until sources change or the output
is missing. Compilation errors fail the request; fix the source and refresh to
retry. Todos and existing sessions survive reloads through `defonce` stores.
Restart after changing dependencies, `dev/`, or the library itself.

To run without development reloading:

```bash
clojure -M:build
clojure -M:serve
```

`:serve` requires the generated stylesheet. In this mode, rebuild CSS and restart
the server after changing templates or styles.

## Try it

- Add a task, mark it complete or active again, and delete it.
- Filter by All, Active, or Completed.
- Switch between Auto, Light, and Dark. Auto follows the OS through a CSS media
  query; explicit modes render a theme class on `<html>`. Theme/filter choices
  live in the URL and are preserved through form submissions.
- Try a narrow viewport or disable JavaScript—everything still works.

POST forms carry session-bound CSRF tokens. Successful changes return **303**
redirects so refreshing the result page doesn't repeat the submission. Empty,
whitespace-only, and over-120-character titles are rejected on the server; invalid
input returns a rendered **422** response. Hiccup escapes todo titles, including
when they appear in attributes.

**This is a shared demo, not a production task manager.** Every visitor sees the
same list; there are no accounts or per-user lists. Restarting the process resets
todos and sessions. There is no persistence or storage limit.

## Source

| File | Responsibility |
| --- | --- |
| `src/example/todos.clj` | Pure state transitions and filtering, with three seed tasks |
| `src/example/server.clj` | In-memory atom, Reitit routes, Ring middleware, and Jetty startup |
| `src/example/views.clj` | Hiccup templates, native forms, and `def` + `s/c` view styles |
| `src/example/styles.clj` | Palette-based semantic tokens, themes, preflight, and shared styles |
| `src/example/build.clj` | Shared style-compilation callback and JVM-only CSS generation |
| `dev/example/dev.clj` | Development-only Ring source reload and `uni.style` CSS reload |

Routes:

| Method | Path | Action |
| --- | --- | --- |
| GET | `/` | Render the list; accepts `theme` and `filter` query parameters |
| POST | `/todos` | Add a task |
| POST | `/todos/:id/toggle` | Toggle completion |
| POST | `/todos/:id/delete` | Delete a task |
| GET | `/css/bundle.css` | Serve the generated stylesheet |

## How CSS reaches the browser

1. A dedicated build JVM calls `uni.style.build/write-css!`.
2. Its callback loads **all styled namespaces** in dependency order: styles,
   then views. Macro expansion collects their CSS.
3. The build writes `resources/public/css/bundle.css` (gitignored).
4. A separate server JVM renders templates and serves that static stylesheet.

This is the production build path; CSS is not generated during production
requests. In `:dev`, Ring's `wrap-reload` runs before
`uni.style.build/wrap-css-reload`, which runs the same compilation callback in the
server JVM when sources change. The dev handler serializes reloads, compilation,
and rendering. JVM class names are deterministic, so template classes match the
build artifact. Package the stylesheet with the application.
The library depends on ClojureScript for shared compiler internals, but this
example compiles no ClojureScript and emits no JavaScript.

Style rules are one per line, and palette colors use the `sc` alias. Hiccup 2
serializes `UniStyle` attributes via `toString()`, so templates pass `s/c` values
directly as `:class`. Avoid mixing Hiccup `.class` tag shorthand with these values;
use `s/c` to compose all classes instead.

## Verify

```bash
clojure -M:test
clojure -M:build
```

For linting and formatting, install clj-kondo and cljfmt, then run:

```bash
mkdir -p .clj-kondo  # clj-kondo copies the libraries' configs into it
clj-kondo --lint "$(clojure -Spath -M:dev:test)" --dependencies --parallel --copy-configs
clj-kondo --lint src dev test
cljfmt check src dev test
```

Tests cover pure todo operations, input validation, filtering, escaped rendering,
Ring request handling, and CSRF rejection. No browser test framework is included.
