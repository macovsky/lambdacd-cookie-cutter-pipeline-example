(ns pipeline-templates.pipeline
  (:use [lambdacd.steps.control-flow]
        [lambdacd.steps.manualtrigger]
        [pipeline-templates.steps])
  (:require
    [org.httpkit.server :as http-kit]
    [pipeline-templates.custom-ui :as custom-ui]
    [pipeline-templates.auth :as auth]
    [lambdacd-git.core :as git]
    [lambdacd.runners :as runners]
    [lambdacd.core :as lambdacd]
    [lambdaui.core :as lambdaui]
    [compojure.core :as compojure]
    [hiccup.core :as h]
    [ring.middleware.oauth2 :refer [wrap-oauth2]]
    [ring.middleware.defaults :refer [wrap-defaults site-defaults secure-site-defaults]])
  (:gen-class)
  (:import (java.nio.file.attribute FileAttribute)
           (java.nio.file Files)))

(def projects [{:name         "LambdaCD"
                :pipeline-url "/lambdacd"
                :repo-uri     "git@github.com:flosell/lambdacd.git"
                :test-command "./go test"
                :logins       #{"macovsky"}}
               {:name         "LambdaCD Artifacts"
                :pipeline-url "/artifacts"
                :repo-uri     "git@github.com:flosell/lambdacd-artifacts.git"
                :test-command "./go test"}
               {:name         "LambdaCD Leiningen Template"
                :pipeline-url "/template"
                :repo-uri     "git@github.com:flosell/lambdacd-template.git"
                :test-command "lein test"}])

(defn accessible? [project login]
  ((get project :logins #{}) login))

(defn mk-pipeline-def [{repo-uri :repo-uri test-command :test-command}]
  `(
     (either
       wait-for-manual-trigger
       (wait-for-commit-on-master ~repo-uri))
     (with-workspace
       (clone ~repo-uri)
       (run-tests ~test-command)
       publish)))

(defn- mk-lambda-ui-links [projects]
  (for [{pipeline-url :pipeline-url name :name} projects]
    {:url (str pipeline-url "/lambda-ui/lambdaui") :text name}))

(defn pipeline-for [project]
  (let [home-dir     (str (Files/createTempDirectory "lambdacd" (into-array FileAttribute [])))
        config       {:home-dir home-dir :name (:name project)
                      :ui-config {:navbar {:links (mk-lambda-ui-links projects)}}}
        pipeline-def (mk-pipeline-def project)
        pipeline     (lambdacd/assemble-pipeline pipeline-def config)
        app          (custom-ui/ui-for pipeline)]
    (runners/start-one-run-after-another pipeline)
    app))

(defn- wrap-if
  [f pred middleware]
  (fn [request]
    (if (pred request)
      ((middleware f) request)
      (f request))))

(defn mk-context [project]
  (let [app (pipeline-for project)] ; don't inline this, otherwise compojure will always re-initialize a pipeline on each HTTP request
    (compojure/context (:pipeline-url project)
                       []
                       (wrap-if app
                                (fn [req]
                                  (not (accessible? project (auth/login req))))
                                auth/redirect-to-root))))

;; Nice overview page:
(defn mk-link [{url :pipeline-url name :name}]
  [:li [:span [:a {:href (str url "/")} name]]])

(defn mk-index [req projects]
  (let [accessible-projects (filter #(accessible? % (auth/login req)) projects)
        login (auth/login req)
        sign-in-uri (get-in auth/oauth-config [:github :launch-uri])]
    (h/html
      [:html
       [:head
        [:title "Pipelines"]]
       [:body
        (when (seq accessible-projects)
          [:ul (map mk-link accessible-projects)])
        [:p
         (if login
           [:small login]
           [:a {:href sign-in-uri} "Sign in with Github"])]]])))

(def defaults
  (let [settings (if (System/getenv "SSL")
                   (assoc secure-site-defaults :proxy true)
                   site-defaults)]
    (assoc-in settings [:security :anti-forgery] false)))

(defn -main [& args]
  (let [
        contexts (map mk-context projects)
        routes (apply compojure/routes
                      (conj contexts
                            (compojure/GET "/" [] #(mk-index % projects))
                            (compojure/GET "/set-user" [] auth/set-user)))
         app (wrap-defaults (wrap-oauth2 routes auth/oauth-config) defaults)
         port (Integer/parseInt (or (System/getenv "PORT") "8080"))]
       (http-kit/run-server app {:port port})))
