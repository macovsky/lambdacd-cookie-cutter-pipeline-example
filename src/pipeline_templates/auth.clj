(ns pipeline-templates.auth
  (:require [clj-http.client :as http]
            [cheshire.core :as json]
            [ring.util.response :as resp]))

(def oauth-config
  {:github
   {:authorize-uri    "https://github.com/login/oauth/authorize"
    :access-token-uri "https://github.com/login/oauth/access_token"
    :client-id        (or (System/getenv "CLIENT_ID") "d98b9b146b7ce3f5d432")
    :client-secret    (or (System/getenv "CLIENT_SECRET") "0e0dbfcc612d3c41c5c083e3eb80546fe7df39a6")
    :scopes           ["read:user"]
    :launch-uri       "/oauth2/github"
    :redirect-uri     "/oauth2/github/callback"
    :landing-uri      "/set-user"}})

(defn fetch-login [token]
  (let [resp (http/post "https://api.github.com/graphql"
                        {:body "{ \"query\": \"query { viewer { login } }\" }"
                         :headers {"Authorization" (str "bearer " token)}})
        body (json/parse-string (:body resp) true)]
    (get-in body [:data :viewer :login] nil)))

(defn get-token [req]
  (get-in req [:oauth2/access-tokens :github :token]))

(defn login [req]
  (get-in req [:session :login]))

(defn set-user [req]
  (let [session (:session req)
        token (get-token req)
        login (when token
                (fetch-login token))]
    (-> (resp/redirect "/")
        (assoc :session (assoc session :login login)))))

(defn not-authenticated? [req]
  (complement (get-token req)))

(defn redirect-to-root [f]
  (fn [req]
    (resp/redirect "/")))
