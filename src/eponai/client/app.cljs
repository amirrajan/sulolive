(ns eponai.client.app
  (:require [om.next :as om :refer-macros [defui]]
            [sablono.core :refer-macros [html]]
            [goog.dom :as gdom]
    ;; To initialize ReactDOM:
            [cljsjs.react.dom]
            [datascript.core :as d]
            [eponai.client.backend :as backend]
            [eponai.common.parser :as parser]
            [eponai.client.ui.add_transaction :refer [AddTransaction ->AddTransaction]]
            [eponai.client.ui.header :refer [Header ->Header]]
            [eponai.client.ui.all_transactions :refer [AllTransactions ->AllTransactions]]))

(defui App
  static om/IQuery
  (query [_]
    [:datascript/schema
     {:proxy/header (om/get-query Header)}
     {:proxy/transactions (om/get-query AllTransactions)}])
  Object
  (render
    [this]
    (let [{:keys [proxy/header proxy/transactions]} (om/props this)]
      (html [:div
             [:div (->Header header)]
             [:div {:class "content-section-b"}
              [:div {:class "content-section-b"}
               (->AllTransactions transactions)]]]))))

(defonce conn-atom (atom nil))

(defn init-conn
  "Sets up the datascript state. Caches the state so we can keep our app state between
  figwheel reloads."
  []
  (if @conn-atom
    @conn-atom
    (let [ui-schema {:ui/singleton {:db/unique :db.unique/identity}}
          ui-state [{:ui/singleton :budget/header}
                    {:ui/singleton :ui.singleton/app :app/inited? false}]
          conn (d/create-conn ui-schema)]
      (d/transact! conn ui-state)
      (reset! conn-atom conn))))

(defn initialize-app [conn]
  (let [parser (parser/parser)
        reconciler (om/reconciler {:state   conn
                                   :parser  parser
                                   :remotes [:remote]
                                   :send    (backend/send! "/user/")
                                   :merge   (backend/merge! conn)})]
    (om/add-root! reconciler App (gdom/getElement "my-app"))))

(defn run []
  (initialize-app (init-conn)))
