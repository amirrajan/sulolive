(ns eponai.web.ui.store.home
  (:require
    [eponai.common.ui.dom :as dom]
    [eponai.common.ui.elements.callout :as callout]
    [eponai.common.ui.elements.css :as css]
    [eponai.common.ui.elements.grid :as grid]
    [eponai.common.ui.elements.menu :as menu]
    [eponai.client.routes :as routes]
    [eponai.web.ui.button :as button]
    [om.next :as om :refer [defui]]
    [eponai.common.format.date :as date]
    [eponai.common.mixpanel :as mixpanel]
    [eponai.common.ui.utils :refer [two-decimal-price]]
    [eponai.web.ui.photo :as photo]
    [taoensso.timbre :refer [debug]]))

(defn verification-status-element [component]
  (let [{:query/keys [stripe-account current-route]} (om/props component)
        {:stripe/keys [charges-enabled? payouts-enabled? verification]} stripe-account
        {:stripe.verification/keys [due-by fields-needed disabled-reason]} verification
        store-id (get-in current-route [:route-params :store-id])
        is-alert? (or (false? charges-enabled?) (false? payouts-enabled?) (when (some? due-by) (< due-by (date/current-secs))))
        is-warning? (and (not-empty fields-needed) (some? due-by))

        disabled-labels {:fields_needed (dom/p nil (dom/span nil "More information is needed to verify your account. Please ")
                                               (dom/a nil (dom/span nil "provide the required information")) (dom/span nil " to re-enable the account."))}]
    (when (or is-alert? is-warning? (not-empty fields-needed))
      (callout/callout
        (cond->> (css/add-class :account-status (css/add-class :notification))
                 (or is-alert? is-warning?)
                 (css/add-class :warning))
        (grid/row
          nil
          (grid/column
            (css/add-class :shrink)
            (dom/i {:classes ["fa fa-warning fa-fw"]}))
          (grid/column
            nil
            (dom/p
              nil
              (cond (false? charges-enabled?)
                    (dom/strong nil "Charges from this account are disabled")
                    (false? payouts-enabled?)
                    (dom/strong nil "Payouts to this account are disabled")
                    (some? due-by)
                    (dom/strong nil "More information is needed to verify your account")
                    (not-empty fields-needed)
                    (dom/strong nil "More information may be needed to verify your account")))
            (cond (false? charges-enabled?)
                  (get disabled-labels (keyword disabled-reason))
                  (false? payouts-enabled?)
                  (get disabled-labels (keyword disabled-reason))
                  (some? due-by)
                  (dom/p nil
                         (dom/span nil "More information needs to be collected to keep this account enabled. Please ")
                         (dom/a {:href (routes/url :store-dashboard/settings#activate {:store-id store-id})} "provide the required information")
                         (dom/span nil " to prevent disruption in service to this account."))
                  (some? fields-needed)
                  (dom/p nil
                         (dom/span nil "If this account continues to process more volume, more information may need to be collected. To prevent disruption in service to this account you can choose to ")
                         (dom/a {:href (routes/url :store-dashboard/settings#activate {:store-id store-id})} "provide the information")
                         (dom/span nil " proactively.")))
            ))))))

(defn check-list-item [done? href & [content]]
  (menu/item
    (cond->> (css/add-class :getting-started-item)
             done?
             (css/add-class :done))

    (dom/p nil
           (dom/i {:classes ["fa fa-check fa-fw"]})
           (dom/a
             {:href (when-not done? href)}
             content))))

(defui StoreHome
  static om/IQuery
  (query [_]
    [{:query/store [:db/id
                    :store/uuid
                    {:store/profile [:store.profile/name
                                     {:store.profile/photo [:photo/path :photo/id]}]}
                    {:store/owners [{:store.owner/user [:user/email]}]}
                    :store/stripe
                    {:order/_store [:order/items]}
                    {:stream/_store [:stream/state]}]}
     :query/store-item-count
     {:query/stripe-account [:stripe/details-submitted?]}
     :query/current-route])
  Object
  (render [this]
    (let [{:query/keys [store store-item-count current-route stripe-account]} (om/props this)
          {:keys [store-id]} (:route-params current-route)
          store-item-count (or store-item-count 0)]
      (dom/div
        {:id "sulo-main-dashboard"}

        (dom/div
          (css/add-class :section-title)
          (dom/h1 nil "Home"))

        (dom/div
          (css/add-class :section-title)
          (dom/h2 nil "Your store"))
        (callout/callout-small
          (css/add-class :section-info)
          (grid/row
            (->> (css/align :center)
                 (css/align :middle))

            (grid/column
              (->> (grid/column-size {:small 12 :medium 4})
                   (css/text-align :center))
              (dom/h3 nil (get-in store [:store/profile :store.profile/name]))
              (photo/store-photo store {:transformation :transformation/thumbnail})
              (button/default-hollow
                {:href    (routes/url :store-dashboard/profile {:store-id store-id})
                 :onClick #(mixpanel/track-key ::mixpanel/go-to-store-info {:source "store-dashboard"})}
                (dom/span nil "Store info")
                (dom/i {:classes ["fa fa-chevron-right"]})))

            (grid/column
              nil
              (grid/row
                (grid/columns-in-row {:small 2})
                (grid/column
                  (css/text-align :center)
                  (dom/h3 nil "Products")
                  (dom/p (css/add-class :stat) store-item-count)
                  (button/default-hollow
                    {:href    (routes/url :store-dashboard/product-list {:store-id store-id})
                     :onClick #(mixpanel/track-key ::mixpanel/go-to-products {:source "store-dashboard"})}
                    (dom/span nil "Products")
                    (dom/i {:classes ["fa fa-chevron-right"]})))
                (grid/column
                  (css/text-align :center)
                  (dom/h3 nil "Orders")
                  (dom/p (css/add-class :stat) (count (:order/_store store)))
                  (button/default-hollow
                    {:href    (routes/url :store-dashboard/order-list {:store-id store-id})
                     :onClick #(mixpanel/track-key ::mixpanel/go-to-orders {:source "store-dashboard"})}
                    (dom/span nil "Orders")
                    (dom/i {:classes ["fa fa-chevron-right"]})))))))

        (callout/callout
          nil
          (grid/row
            (grid/columns-in-row {:small 3})
            (grid/column
              (css/text-align :center)
              (dom/h3 nil "Balance")
              (dom/p (css/add-class :stat) (two-decimal-price 0)))
            (grid/column
              (css/text-align :center)
              (dom/h3 nil "Customers")
              (dom/p (css/add-class :stat) 0))
            (grid/column
              (css/text-align :center)
              (dom/h3 nil "Payments")
              (dom/p (css/add-class :stat) 0))))

        (dom/div
          (css/add-class :section-title)
          (dom/h2 nil "Getting started"))

        (callout/callout
          nil
          (menu/vertical
            nil

            (check-list-item
              (some? (:store.profile/description (:store/profile store)))
              (routes/url :store-dashboard/settings {:store-id store-id})
              (dom/span nil "Describe your store. People love to hear your story."))

            (check-list-item
              (boolean (pos? store-item-count))
              (routes/url :store-dashboard/create-product {:store-id store-id})
              (dom/span nil "Show off your amazing goods, add your first product."))

            (check-list-item
              false
              (routes/url :store-dashboard/stream {:store-id store-id})
              (dom/span nil "Setup your first stream and hangout with your customers when you feel like it."))

            (check-list-item
              (:stripe/details-submitted? stripe-account)
              (routes/url :store-dashboard/settings#business {:store-id store-id})
              (dom/span nil "Verify your account. You know, so we know you're real."))))

        (dom/div
          (css/add-class :section-title)
          (dom/h2 nil "Notifications"))
        (if (:stripe/details-submitted? stripe-account)
          (verification-status-element this)
          (callout/callout
            (->> (css/add-class :notification)
                 (css/add-class :action))
            (grid/row
              nil
              (grid/column
                (css/add-class :shrink)
                (dom/i {:classes ["fa fa-info fa-fw"]}))
              (grid/column
                nil
                (callout/header nil "Verify your account")))
            (dom/p nil
                   (dom/span nil "Before ")
                   (dom/a {:href (routes/url :store-dashboard/settings#business {:store-id store-id})} (dom/span nil "verifying your account"))
                   (dom/span nil ", you can only use SULO Live in test mode. You can manage your store, but it'll not be visible to the public."))
            (dom/p nil
                   "Once you've verified your account you'll immediately be able to use all features of SULO Live. Your account details are reviewed with Stripe to ensure they comply with our terms of service. If there is a problem, we'll get in touch right away to resolve it as quickly as possible.")))

        (dom/div
          (css/add-class :section-title)
          (dom/h2 nil "Questions?"))
        (callout/callout
          nil
          (dom/p nil
                 (dom/span nil "We love to hear from you! Give us feedback, report problems, or just say hi, at ")
                 (dom/a {:href "mailto:hello@sulo.live"} "hello@sulo.live")
                 (dom/span nil ". Miriam, Diana or Petter will help you out.")))






        ;(grid/row
        ;  (->> (css/align :center)
        ;       (css/add-class :expanded)
        ;       (css/add-class :collapse)
        ;       (grid/columns-in-row {:small 1 :medium 2}))
        ;  (grid/column
        ;    nil
        ;    (store-info-element this))
        ;  (grid/column
        ;    nil
        ;
        ;    (if (:stripe/details-submitted? stripe-account)
        ;      (verification-status-element this)
        ;      (callout/callout
        ;        (->> (css/add-class :notification)
        ;             (css/add-class :action))
        ;        (grid/row
        ;          nil
        ;          (grid/column
        ;            (css/add-class :shrink)
        ;            (dom/i {:classes ["fa fa-info fa-fw"]}))
        ;          (grid/column
        ;            nil
        ;            (callout/header nil "Activate your account")))
        ;        (dom/p nil
        ;               (dom/span nil "Before ")
        ;               (dom/a {:href (routes/url :store-dashboard/settings#activate {:store-id store-id})} (dom/span nil "activating your account"))
        ;               (dom/span nil ", you can only use SULO Live in test mode. You can manage your store, but it'll not be visible to the public."))
        ;        (dom/p nil
        ;               "Once you've activated you'll immediately be able to use all features of SULO Live. Your account details are reviewed with Stripe to ensure they comply with our terms of service. If there is a problem, we'll get in touch right away to resolve it as quickly as possible.")))))
        ))))