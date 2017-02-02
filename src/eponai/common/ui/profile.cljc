(ns eponai.common.ui.profile
  (:require
    #?(:cljs
       [eponai.client.ui.photo-uploader :as pu])
    [eponai.common.ui.dom :as my-dom]
    [eponai.common.ui.common :as common]
    [eponai.common.ui.navbar :as nav]
    [om.dom :as dom]
    [om.next :as om :refer [defui]]
    [eponai.common.ui.elements.css :as css]
    [eponai.common.ui.elements.photo :as photo]
    [eponai.common.ui.elements.menu :as menu]
    [taoensso.timbre :refer [debug]]))

(defui Profile
  static om/IQuery
  (query [_]
    [{:proxy/navbar (om/get-query nav/Navbar)}
     {:query/user [:db/id :user/email {:user/photo [:photo/path]}]}
     {:query/auth [:db/id]}
     #?(:cljs
        {:proxy/photo-upload (om/get-query pu/PhotoUploader)})])
  Object
  (initLocalState [_]
    {:tab :following
     :file-upload? false
     :photo-url "https://s3.amazonaws.com/sulo-images/site/collection-women.jpg"})
  (render [this]
    (let [{:keys [query/user proxy/navbar proxy/photo-upload query/auth]} (om/props this)
          {:keys [tab file-upload? photo-url]} (om/get-state this)
          photo-url (or (get-in user [:user/photo :photo/path]) photo-url)
          is-current-user? (= (:db/id user) (:db/id auth))]
      (debug "Profile for user: " user)
      (dom/div
        #js {:id "sulo-profile" :className "sulo-page"}
        (common/page-container
          {:navbar navbar}
          (dom/div #js {:className "header"}
            #?(:cljs
               (when file-upload?
                 (common/modal {:on-close #(om/update-state! this assoc :file-upload? false)}
                               (dom/div nil
                                 (dom/h2 nil "Change Profile Photo")
                                 (menu/vertical nil
                                                (menu/item nil
                                                           (pu/->PhotoUploader (om/computed
                                                                                 photo-upload
                                                                                 {:on-change #(om/update-state! this assoc :file-upload? false)})))
                                                (menu/item nil
                                                           (dom/a #js {:className "button hollow expanded"} "Remove Photo")))))))
            (dom/div #js {:className "user-info"}
              (my-dom/div (->> (css/grid-row)
                               (css/align :center))
                          (my-dom/div (cond->> (->> (css/grid-column)
                                                    (css/add-class :profile-photo)
                                                    (css/grid-column-size {:small 4 :medium 3 :large 2}))
                                               is-current-user?
                                               (css/add-class :edit-enabled))
                                      (dom/a #js {:onClick #(when is-current-user? (om/update-state! this assoc :file-upload? true))}
                                             (photo/circle
                                               {:src photo-url}))))
              (my-dom/div (->> (css/grid-row)
                               (css/align :center))
                          (my-dom/div (->> (css/grid-column)
                                           (css/grid-column-size {:small 4 :medium 3 :large 2})
                                           (css/text-align :center))
                                      (dom/a #js {:className "button gray hollow"} (dom/span nil "+ Follow"))))
              (my-dom/div (->> (css/grid-row)

                               css/grid-column)
                          (menu/horizontal
                            (css/align :center)
                            (menu/item-tab {:active?  (= tab :following)
                                            :on-click #(om/update-state! this assoc :tab :following)} "Following")
                            (menu/item-tab {:active?  (= tab :followers)
                                            :on-click #(om/update-state! this assoc :tab :followers)} "Followers")
                            (menu/item-tab {:active?  (= tab :products)
                                            :on-click #(om/update-state! this assoc :tab :products)} "Products")
                            (menu/item-tab {:active?  (= tab :about)
                                            :on-click #(om/update-state! this assoc :tab :about)} "About"))))))))))

(def ->Profile (om/factory Profile))
