(ns eponai.server.api.store
  (:require
    [eponai.common.database :as db]
    [eponai.server.datomic.format :as f]
    [eponai.server.external.aws-s3 :as s3]
    [eponai.server.external.stripe :as stripe]
    [taoensso.timbre :refer [debug info]]
    [eponai.common.format :as cf])
  (:import (com.stripe.exception CardException)))

(defn retracts [old-entities new-entities]
  (let [removed (filter #(not (contains? (into #{} (map :db/id new-entities)) (:db/id %))) old-entities)]
    (reduce (fn [l remove-photo]
              (conj l [:db.fn/retractEntity (:db/id remove-photo)]))
            []
            removed)))

(defn edit-many-txs [entity attribute old new]
  (let [db-txs-retracts (retracts old new)]
    (reduce (fn [l e]
              (if (db/tempid? (:db/id e))
                (conj l e [:db/add entity attribute (:db/id e)])
                (conj l e)))
            db-txs-retracts
            new)))

(defn photo-entities [aws-s3 ps]
  (map-indexed (fn [i new-photo]
                 (if (some? (:location new-photo))
                   (f/item-photo (s3/upload-photo aws-s3 new-photo) i)
                   (-> new-photo (assoc :store.item.photo/index i) cf/add-tempid)))
               ps))

(defn create-product [{:keys [state system]} store-id {:store.item/keys [photos skus] :as params}]
  (let [product (f/product params)

        ;; Craete product transactions
        product-txs [product
                     [:db/add store-id :store/items (:db/id product)]]

        ;; Create SKU transactions
        new-skus (map (fn [s] (f/sku s)) skus)
        product-sku-txs (into product-txs (edit-many-txs (:db/id product) :store.item/skus [] new-skus))

        ;; Create photo transactions, upload to S3 if necessary
        new-photos (photo-entities (:system/aws-s3 system) photos)
        product-sku-photo-txs (into product-sku-txs (edit-many-txs (:db/id product) :store.item/photos [] new-photos))]

    ;; Transact all updates to Datomic once
    (db/transact state product-sku-photo-txs)))

(defn update-product [{:keys [state system]} product-id {:store.item/keys [photos skus] :as params}]
  (let [old-item (db/pull (db/db state) [:db/id :store.item/photos :store.item/skus] product-id)
        old-skus (:store.item/skus old-item)
        old-photos (:store.item/photos old-item)

        ;; Update product with new info, name/description, etc. Collections are updated below.
        new-product (f/product (assoc params :db/id product-id))
        product-txs [new-product]

        ;; Update SKUs, remove SKUs not included in the skus collection from the client.
        new-skus (map (fn [s] (f/sku s)) skus)
        product-sku-txs (into product-txs (edit-many-txs product-id :store.item/skus old-skus new-skus))

        ;; Update photos, remove photos that are not included in the photos collections from the client.
        new-photos (photo-entities (:system/aws-s3 system) photos)
        product-sku-photo-txs (into product-sku-txs (edit-many-txs product-id :store.item/photos old-photos new-photos))]

    ;; Transact updates into datomic
    (db/transact state product-sku-photo-txs)))

(defn delete-product [{:keys [state]} product-id]
  (db/transact state [[:db.fn/retractEntity product-id]]))

(defn ->order [state o store-id user-id]
  (let [store (db/lookup-entity (db/db state) store-id)]
    (assoc o :order/store store :order/user user-id)))

(defn create-order [{:keys [state system auth]} store-id {:keys [items source shipping subtotal shipping-fee]}]
  (let [{:keys [stripe/id]} (stripe/pull-stripe (db/db state) store-id)
        {:keys [shipping/address]} shipping
        order (f/order {:order/items    items
                        :order/uuid     (db/squuid)
                        :order/shipping shipping
                        :order/user     (:user-id auth)
                        :order/store    store-id})]
    (when source
      (let [total-amount (* 100 (+ subtotal shipping-fee))  ;Convert to cents for Stripe
            application-fee (* 100 (* 0.2 subtotal))        ;Convert to cents for Stripe
            transaction-fee (* 0.029 total-amount)
            charge (try
                     (stripe/create-charge (:system/stripe system) {:amount      (int total-amount)
                                                                    ;:application_fee (int (+ application-fee transaction-fee))
                                                                    :currency    "cad"
                                                                    :source      source
                                                                    :metadata    {:order_uuid (:order/uuid order)}
                                                                    :shipping    {:name    (:shipping/name shipping)
                                                                                  :address {:line1       (:shipping.address/street address)
                                                                                            :line2       (:shipping.address/street2 address)
                                                                                            :postal_code (:shipping.address/postal address)
                                                                                            :city        (:shipping.address/locality address)
                                                                                            :state       (:shipping.address/region address)
                                                                                            :country     (:shipping.address/country address)}}
                                                                    :destination {:account id
                                                                                  :amount  (int (- total-amount (+ application-fee transaction-fee)))}})
                     (catch CardException e
                       (throw (ex-info (.getMessage e)
                                       {:message (.getMessage e)}))))
            charge-entity {:db/id     (db/tempid :db.part/user)
                           :charge/id (:charge/id charge)}
            is-paid? (:charge/paid? charge)
            order-status (if is-paid? :order.status/paid :order.status/created)
            charged-order (assoc order :order/status order-status :order/charge (:db/id charge-entity))
            result-db (:db-after (db/transact state [charge-entity
                                                     charged-order]))]
        ;; Return order entity to redirect in the client
        (db/pull result-db [:db/id] [:order/uuid (:order/uuid order)])))))

(defn update-order [{:keys [state system]} store-id order-id {:keys [order/status]}]
  (let [old-order (db/pull (db/db state) [:db/id :order/status {:order/charge [:charge/id]}] order-id)
        allowed-transitions {:order.status/created   #{:order.status/paid :order.status/canceled}
                             :order.status/paid      #{:order.status/fulfilled :order.status/canceled}
                             :order.status/fulfilled #{:order.status/returned}}
        old-status (:order/status old-order)
        is-status-transition-allowed? (contains? (get allowed-transitions old-status) status)]
    (if is-status-transition-allowed?
      (let [should-refund? (contains? #{:order.status/canceled :order.status/returned} status)]
        (when should-refund?
          (let [{:keys [stripe/secret]} (stripe/pull-stripe (db/db state) store-id)]
            (stripe/create-refund (:system/stripe system) secret {:charge (get-in old-order [:order/charge :charge/id])})))
        (db/transact state [[:db/add order-id :order/status status]]))
      (throw (ex-info (str "Order status transition not allowed, " status " can only transition to " (get allowed-transitions status))
                      {:order-status        status
                       :message             "Your order status could not be updated."
                       :allowed-transitions allowed-transitions})))))

(defn account [{:keys [state system]} store-id]
  (let [{:keys [stripe/id] :as s} (stripe/pull-stripe (db/db state) store-id)]
    (when (some? id)
      (merge s
             (stripe/get-account (:system/stripe system) id)))))