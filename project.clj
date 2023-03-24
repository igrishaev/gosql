(defproject com.github.igrishaev/gosql "0.1.0"

  :description
  "Good old SQL driven with templates"

  :url
  "https://github.com/igrishaev/gosql"

  :deploy-repositories
  {"releases" {:url "https://repo.clojars.org" :creds :gpg}}

  :license
  {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
   :url "https://www.eclipse.org/legal/epl-2.0/"}

  :release-tasks
  [["vcs" "assert-committed"]
   ["test"]
   ["change" "version" "leiningen.release/bump-version" "release"]
   ["vcs" "commit"]
   ["vcs" "tag" "--no-sign"]
   ["deploy"]
   ["change" "version" "leiningen.release/bump-version"]
   ["vcs" "commit"]
   ["vcs" "push"]]

  :dependencies
  [[selmer "1.12.56"]
   [com.github.seancorfield/next.jdbc "1.3.847"]]

  :profiles
  {:dev
   {:source-paths ["env/dev/src"]
    :resource-paths ["env/dev/resources"]

    :dependencies
    [[org.clojure/clojure "1.11.1"]

     ;; test & bench
     [mount "0.1.17"]
     [org.xerial/sqlite-jdbc "3.41.0.0"]
     [com.layerware/hugsql-core "0.5.3"]
     [com.layerware/hugsql-adapter-next-jdbc "0.5.3"]
     [com.github.seancorfield/honeysql "2.4.980"]]}})
