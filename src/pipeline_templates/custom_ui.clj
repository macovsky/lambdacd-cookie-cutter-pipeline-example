(ns pipeline-templates.custom-ui
  (:require [compojure.route :as route]
            [lambdacd.ui.api :as api]
            [lambdacd.ui.ui-page :as ui-page]
            [compojure.core :refer [routes GET context]]
            [hickory.core :as hickory]
            [hickory.render :as hickory-render]
            [hickory.convert :as hickory-convert]
            [hickory.select :as hickory-select]
            [clojure.zip :as zip]))

(defn- replace-title [hic pipeline-name]
  (let [locs (hickory-select/select-locs (hickory-select/tag :title) hic)]
    (if (empty? locs)
      (throw (Exception. "no locs found")))
    (zip/replace (first locs) {:type :element :tag :title :attrs nil :content [(str pipeline-name " - LambdaCD")]})))

(defn new-ui-page [pipeline]
  (let [pipeline-name  (get-in pipeline [:context :config :name])]
    (-> (ui-page/ui-page pipeline)
        (hickory/parse)
        (hickory/as-hickory)
        (replace-title pipeline-name)
        (zip/root)
        (hickory-render/hickory-to-html))))

(defn ui-for
  ([pipeline]
   (routes
     (context "/api" [] (api/rest-api pipeline))
     (GET "/" [] (new-ui-page pipeline))
     (route/resources "/" {:root "public"}))))

