(ns miasma.handler
  (:require [com.stuartsierra.component :as component]
            reitit.coercion.spec
            [reitit.dev.pretty :as pretty]
            [reitit.http :as http]
            [reitit.http.coercion :as coercion]
            [reitit.interceptor.sieppari :as sieppari]
            [reitit.swagger :as swagger]
            [reitit.swagger-ui :as swagger-ui]
            [reitit.http.interceptors.parameters :as parameters]
            [reitit.http.interceptors.muuntaja :as muuntaja]
            [reitit.http.interceptors.exception :as exception]
            [reitit.http.interceptors.multipart :as multipart]
            [sieppari.async.manifold]
            [clojure.spec.alpha :as s]
            [reitit.ring :as ring]
            [manifold.deferred :as d]
            [miasma.db :as db]
            [miasma.util :refer [parse-webfinger-resource]]
            [muuntaja.core :as m]
            [clojure.tools.logging :as log]))

(defprotocol Handler
  (handler [this]
    "Return a Ring compatible handler."))

(declare handler-impl)

(defrecord HandlerImpl [db handler-ref domain]
  component/Lifecycle
  (start [this] (assoc this :handler-ref (or handler-ref (atom nil))))
  (stop [this] this)

  Handler
  (handler [this]
    (swap! handler-ref (fn [impl] (or impl (handler-impl this))))))

(s/def :webfinger/resource string?)

(defn- handler-impl
  [this]
  (http/ring-handler
    (http/router
      [["/.well-known/webfinger"
        {:get {:summary    "Webfinger endpoint"
               :parameters {:query (s/keys :req-un [:webfinger/resource])}
               :responses  {200 {:body any?}}
               :handler    (miasma.handler.webfinger/webfinger-handler this)}}]]
      {:exception pretty/exception
       :data      {:coercion reitit.coercion.spec/coercion
                   :muuntaja m/instance
                   :interceptors [swagger/swagger-feature
                                  (parameters/parameters-interceptor)
                                  (muuntaja/format-negotiate-interceptor)
                                  (muuntaja/format-response-interceptor)
                                  (exception/exception-interceptor)
                                  (muuntaja/format-request-interceptor)
                                  (coercion/coerce-response-interceptor)
                                  (coercion/coerce-request-interceptor)
                                  (multipart/multipart-interceptor)]}})
    (ring/routes
      (swagger-ui/create-swagger-ui-handler
        {:path "/swagger"
         :config {:validatorUrl nil
                  :operationsSorter "alpha"}})
      (ring/create-default-handler))
    {:executor sieppari/executor}))

