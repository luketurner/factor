# ClojureScript Quickstart

This is a guide for starting, configuring, and deploying a new ClojureScript project, reconstructed from my recent experience.

> One of the complexities in ClojureScript development is the number of options and tools out there. This document will proceed with opinionated recommendations, to keep decisions to a minimum.
> 
> If you do want to explore other options, check these out: 
>  - `clj` - https://clojurescript.org/guides/quick-start
>  - `boot` - https://github.com/magomimmo/modern-cljs/blob/master/doc/second-edition/tutorial-01.md
>  - `leiningen` - I'd recommend https://github.com/bhauman/lein-figwheel

The goal of this guide is to achieve:

- Easy interfacing with `npm` packages
- Cross-platform (Linux+Windows)
- Doesn't require tons of configuration

We'll be using [shadow-cljs](https://github.com/thheller/shadow-cljs) for the build toolchain, [builds.sr.ht](https://builds.sr.ht) for CD, and [Netlify](https://www.netlify.com/) for hosting. (DNS setup isn't covered, but one nice feature of Netlify is the automatic HTTPS support for custom domains.)

## Requirements

- Java SDK 8
- Node 6+ (w/NPM)

## Setup

### Create your project

```
npx create-cljs-project my-app
```

### Add scripts to package.json

```json
  "scripts": {
    "start": "shadow-cljs watch app",
    "build": "shadow-cljs release app"
  }
```

### Create a public/index.html

This should load the JS file, and also have an element with id `app` that will contain the dynamic stuff:

``` html
<!doctype html>
<html>
  <head>
    <meta charset="utf-8" />
    <title>MyApp</title>
  </head>
  <body>
    <div id="app"></div>
    <script src="/js/main.js"></script>
  </body>
</html>
```

### Create core.cljs

Create `src/main/myapp/core.cljs` "Hello World" `init` function:

``` clojure
(ns myapp.core
  (:require [reagent.dom :refer [render]]))

(defn app []
  [:h1 "Hello World!"])

(defn init []
  (render [app] (js/document.getElementById "app")))
```

### Configure shadow-cljs.edn

Update the `shadow-cljs.edn` to look kinda like this.

``` clojure
{:source-paths
 ["src/dev"
  "src/main"
  "src/test"]

 :dependencies
 [[re-frame "1.1.2"]
  [re-frisk "1.3.6"]
  [garden "1.3.10"]]

 :dev-http {8080 "public"}
 :builds
 {:app {:target :browser
        :devtools {:preloads [re-frisk.preload]}
        :modules {:main {:init-fn myapp.core/init}}}}}
```

- Note: The `:app` keyword is the "name" of the build, and could really be whatever you want.

### Testing your setup

Now you should be able to run `npm run start`, and once the build is finished, you can browse to `localhost:8080` and see your "Hello World" text.

Also, you should see some little drawers on the right side of the page -- those are `re-frisk`, and they'll only appear in development builds. Try clicking them.

## Publishing and CI/CD

Before we get too far writing application code, I like to set up CI/CD for my project. There's something about seeing it published on the Internet that hits different, you know?

There's a lot of options here. Github Actions would totally work, or Circle, &c. But this is an opinionated guide, so I'm just gonna pick what I like the most.

### Double Pushing

We'll be using https://sr.ht for some things, so we want our repository to be published to https://git.sr.ht as well as https://github.com. Apparently, this is possible:

```bash
git remote set-url --push --add origin $SOURCEHUT_REPO_URL
git remote set-url --push --add origin $GITHUB_REPO_URL
```

Now, a `git push` will push to both origins at once. Just don't let them get out-of-sync!

### Set up netlify


1. Create a new site. Note the site ID. Configure for manual deployments.
2. Create a new personal access token called `sourcehut`.

### Create secret

Create a secret in Sourcehut with your site ID and access token from Netlify. The secret should be a file called `~/netlify_config` with `444` permissions, containing Bash environment variables:

``` bash
NETLIFY_SITE_ID="my-site-id"
export NETLIFY_AUTH_TOKEN="my-auth-token"
```

Then, note the resulting secret's UUID for the next step.

### Add a .build.yml

Note: replace the variables (e.g. `$SOURCEHUT_REPO_URL`) with the actual value for your project.

``` yaml
image: alpine/latest
packages:
  - openjdk8
  - nodejs
  - npm
sources:
  - $SOURCEHUT_REPO_URL
secrets:
  # ~/netlify_config -- expected to have:
  #   NETLIFY_SITE_ID
  #   export NETLIFY_AUTH_TOKEN
  - $NETLIFY_SECRET_UUID
tasks:
  - install-node-modules: |
      cd $MY_APP_NAME
      npm i --no-progress
      npm i netlify-cli --quiet --no-progress
  - build-js: |
      cd $MY_APP_NAME
      npm run build
      rm public/js/manifest.edn
  - deploy: |
      set +x
      source ~/netlify_config
      set -x
      cd $MY_APP_NAME
      node_modules/.bin/netlify deploy --site="$NETLIFY_SITE_ID" --dir=public --prod >/dev/null 2>&1
```

> **Warning:** It's important to do `set +x` before sourcing the secret file `~/netlify_config`. If you don't, your Netlify secrets will end up in the build log, which is publicly visible. The same applies to any other usage of secrets in your `.build.yml`.

### Testing

Now, when you push your changes to Sourcehut, it should deploy your "Hello World" to your Netlify site.

## Automated tests

(Note -- I'm still figuring this out myself.)

Automated tests are written with `cljs.test` and should live in the `src/test` directory. Example test file:

``` clojure
; src/test/myapp/foo-test.cljs
(ns myapp.foo-test
  (:require [cljs.test :refer [deftest is]]))

(deftest example-test
  (is (= 1 1) "one should equal itself"))
```

To run tests in Node (i.e. only for code that doesn't depend on browser APIs) add the `test` and `citest` builds to your `shadow-cljs.edn` in the `:builds` map:

``` clojure
:builds {
  ...
  :test {:target    :node-test
         :autorun true
         :output-to  "test-out/node-test.js"}
  :citest {:target    :node-test
           :output-to  "test-out/node-test.js"}}
```

The only difference is the CI test build doesn't include `:autorun`, because the :autorun parameter doesn't propagate the exit code (so even if tests fail, your build will succeed.)

Instead, you can compile and run tests in your CI/CD pipeline with:

``` bash
shadow-cljs compile citest && node test-out/node-test.js
```

But, for local development, `:autorun` is much faster and more convenient. Add the `test` build to your `start` script in package.json:

```
"start": "shadow-cljs watch app test"
```

Now, tests are automatically recompiled and re-run whenever a test file (or any file referenced by a test file) is updated.

(Don't forget to add `test-out` to your .gitignore)
## Further notes

At this point, we can architect our app however we want.

A couple thoughts/tips for the journey:

### Adding dependencies

Clojure dependencies should be added to `shadow-cljs.edn`. NPM dependencies should be added to `package.json`. 

> **Note:** `shadow-cljs` will pull in the Clojure dependencies automatically, but the NPM dependencies must be installed manually with `npm install`.

For example, `re-frame` is referenced in `shadow-cljs.edn` as:

``` clojure
[re-frame "1.1.2"]
```

This can be required with:

``` clojure
(ns myapp.core
 (:require [re-frame.core]))
```

To reference NPM packages, use strings in the `:require` list:

``` clojure
(ns myapp.core
  (:require ["react-dom" :as react-dom]))
```

### Recommended libraries

- Writing views: `reagent`
- Managing state: `re-frame`
- Writing styles: `garden`

### Ergonomic Development

The `npm run start` command already includes file watching and nREPL support. Couple tips:

- The Calva extension for VSCode works seamlessly with `shadow-cljs` nREPL, and has surprisingly great Intellisense.
  - I recommend running `npm run start` manually in the terminal, then use Calva's `Connect to an existing REPL server in your project`

- The development-only `re-frisk` dashboards are great for observing state propagation through your app. It can answer the question "what happened?" and obviates (most) debug logging.