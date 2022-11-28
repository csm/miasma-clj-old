(ns miasma.handler.webfinger
  (:require [miasma.util :refer [parse-webfinger-resource]]))

(defn webfinger-handler
  [{:keys [db domain]}]
  (fn [{{{:keys [resource]} :query} :parameters}]
    (log/info "resource:" resource)
    (d/chain
      (when-let [u (parse-webfinger-resource resource)]
        (log/info "read user query" u)
        (db/get db :users (:username u)))
      (fn [user]
        (if (not (nil? user))
          {:body
           {:subject (str "acct:" (:id user) \@ domain)
            :aliases [(str "https://" domain \/ \@ (:id user))
                      (str "https://" domain "/users/" (:id user))]
            :links   [{:rel  "http://webfinger.net/rel/profile-page"
                       :type "text/html"
                       :href (str "https://" domain "/@" (:id user))}
                      {:rel  "self"
                       :type "application/activity+json"
                       :href (str "https://" domain "/users/" (:id user))}
                      {:rel "http://ostatus.org/schema/1.0/subscribe"
                       :template "https://" domain "/authorize_interaction?uri={uri}"}]}}
          {:status 404})))))