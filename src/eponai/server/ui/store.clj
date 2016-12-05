(ns eponai.server.ui.store
  (:require
    [om.dom :as dom]
    [om.next :as om :refer [defui]]
    [eponai.server.ui.common :as common]))
(def stores
  [{:name         "HeadnTail"
    :id           0
    :review-count 13
    :photo        "https://img1.etsystatic.com/122/0/10558959/isla_500x500.21872363_66njj7uo.jpg"
    :goods        [{:name    "Vintage 90's Fila Sport USA Sweatshirt Red Black Sport Trainer Sweater"
                    :price   "$34.00"
                    :img-src "https://img0.etsystatic.com/104/0/10558959/il_570xN.1015873526_1oed.jpg"}
                   {:name    "Jean Michel Basquiat Pink T Shirt"
                    :price   "$52.00"
                    :img-src "https://img1.etsystatic.com/108/0/10558959/il_570xN.1060411775_k1ob.jpg"}
                   {:name    "Vintage Tommy Hilfiger Short Pant Size 34"
                    :price   "$134.00"
                    :img-src "https://img1.etsystatic.com/146/0/10558959/il_570xN.1138598331_iu0t.jpg"}
                   {:name    "Majestic Utah Jazz Hardwood Classics White Throwback NBA Basketball Jersey"
                    :price   "$34.00"
                    :img-src "https://img1.etsystatic.com/101/0/10558959/il_570xN.1038331841_gk1f.jpg"}]}
   {:name         "MagicLinen"
    :id           1
    :cover "https://img0.etsystatic.com/151/0/11651126/isbl_3360x840.22956500_1bj341c6.jpg"
    :photo        "https://img0.etsystatic.com/125/0/11651126/isla_500x500.17338368_6u0a6c4s.jpg"
    :review-count 8
    :goods        [{:name    "Linen duvet cover. Woodrose colour. Linen bedding. Stonewashed linen duvet cover. Taupe bedding. Linen bedding queen, king, double, twin."
                    :price   "$34.00"
                    :img-src "https://img1.etsystatic.com/141/1/11651126/il_570xN.1142044641_1j6c.jpg"}
                   {:name    "Linen pillowcases with ribbons. Bow tie linen pillow cover. Natural linen pillow case. Pillow cases with ties. Romantic pillow cases"
                    :price   "$52.00"
                    :img-src "https://img0.etsystatic.com/137/0/11651126/il_570xN.1003284712_ip5e.jpg"}
                   {:name    "Stone washed linen duvet cover, pleated. White linen bedding. Linen quilt cover. White duvet cover. Luxury, original, modern, handmade"
                    :price   "$134.00"
                    :img-src "https://img0.etsystatic.com/133/0/11651126/il_570xN.915745904_opjr.jpg"}
                   {:name    "Linen fitted sheet. Aquamarine colour. Blue linen fitted sheet. Natural bed sheet. Softened. Green blue stone washed linen bed sheet"
                    :price   "$34.00"
                    :img-src "https://img1.etsystatic.com/126/0/11651126/il_570xN.1098073811_5ca0.jpg"}]}
   {:name         "thislovesthat"
    :id           2
    :cover "https://img1.etsystatic.com/126/0/6396625/iusb_760x100.17290451_heo8.jpg"
    :review-count 43
    :photo        "https://img1.etsystatic.com/121/0/6396625/isla_500x500.17289961_hkw1djlp.jpg"
    :goods        [{:name    "Glitter & Navy Blue Envelope Clutch"
                    :img-src "https://img1.etsystatic.com/030/0/6396625/il_570xN.635631611_4c3s.jpg"
                    :price   "$34.00"}
                   {:name    "Mint Green & Gold Scallop Canvas Clutch"
                    :img-src "https://img0.etsystatic.com/031/0/6396625/il_570xN.581066412_s3ff.jpg"
                    :price   "$52.00"}
                   {:name    "Modern Geometric Wood Bead Necklace"
                    :price   "$134.00"
                    :img-src "https://img0.etsystatic.com/045/1/6396625/il_570xN.723123424_ht5e.jpg"}
                   {:name    "Modern Wood Teardrop Stud Earrings"
                    :price   "$34.00"
                    :img-src "https://img1.etsystatic.com/033/0/6396625/il_570xN.523107737_juvf.jpg"}]}
   {:name         "Nafsika"
    :cover "https://img1.etsystatic.com/133/0/5243597/isbl_3360x840.20468865_f7kumdbt.jpg"
    :review-count 22
    :id           3
    :photo        "https://img0.etsystatic.com/139/0/5243597/isla_500x500.22177516_ath1ugrh.jpg"
    :goods        [{:name    "Silver Twig Ring Milky Aquamarine Cabochon Light Blue March Birthstone Gifts for her Botanical Jewelry"
                    :img-src "https://img0.etsystatic.com/036/1/5243597/il_570xN.654738182_2k08.jpg"
                    :price   "$34.00"}
                   {:name    "Bunny Charm Necklace, Bunny Necklace, Rabbit Necklace, Easter Gift, Child Necklace, Sterling silver, Rabbit Jewelry, Bunny Pendant"
                    :img-src "https://img0.etsystatic.com/024/0/5243597/il_570xN.519102094_4gu0.jpg"
                    :price   "$52.00"}
                   {:name    "Red Moss Planter Fall Cube Necklace Sterling Silver Pendant Botanical Jewelry Novelty Pendant"
                    :img-src "https://img0.etsystatic.com/140/1/5243597/il_570xN.964805038_b4eq.jpg"
                    :price   "$134.00"}
                   {:name    "Citrine Ring Elvish Twig Ring Branch Ring Thorn Jewelry November Birthstone Gifts for her Fine Jewelry"
                    :img-src "https://img0.etsystatic.com/139/3/5243597/il_570xN.931188156_qhqe.jpg"
                    :price   "$34.00"}]}])

;(def stores
;  {1 {:name  "HeadnTail"
;      :photo "https://img0.etsystatic.com/104/0/10558959/il_570xN.1015873526_1oed.jpg"
;      :goods [{:name    "Vintage 90's Fila Sport USA Sweatshirt Red Black Sport Trainer Sweater"
;               :price   "$34.00"
;               :img-src "https://img0.etsystatic.com/104/0/10558959/il_570xN.1015873526_1oed.jpg"}
;              {:name    "Jean Michel Basquiat Pink T Shirt"
;               :price   "$52.00"
;               :img-src "https://img1.etsystatic.com/108/0/10558959/il_570xN.1060411775_k1ob.jpg"}
;              {:name    "Vintage Tommy Hilfiger Short Pant Size 34"
;               :price   "$134.00"
;               :img-src "https://img1.etsystatic.com/146/0/10558959/il_570xN.1138598331_iu0t.jpg"}
;              {:name    "Majestic Utah Jazz Hardwood Classics White Throwback NBA Basketball Jersey"
;               :price   "$34.00"
;               :img-src "https://img1.etsystatic.com/101/0/10558959/il_570xN.1038331841_gk1f.jpg"}]}
;   2 {:name
;      :review-count "8"
;      [{:name    "Duvet Dream"
;        :price   "$34.00"
;        :img-src "https://img1.etsystatic.com/141/1/11651126/il_570xN.1142044641_1j6c.jpg"}
;       {:name    "Pillows"
;        :price   "$52.00"
;        :img-src "https://img0.etsystatic.com/137/0/11651126/il_570xN.1003284712_ip5e.jpg"}
;       {:name    "Organic Linen"
;        :price   "$134.00"
;        :img-src "https://img0.etsystatic.com/133/0/11651126/il_570xN.915745904_opjr.jpg"}
;       {:name    "Jewel Sheets"
;        :price   "$34.00"
;        :img-src "https://img1.etsystatic.com/126/0/11651126/il_570xN.1098073811_5ca0.jpg"}]}})

(defui Store
  Object
  (render [this]
    (let [{:keys [release? params]} (om/props this)
          {:keys [id]} params
          store (some #(when (= (str (:id %)) id) %) stores)]
      (prn "PROPS: " (om/props this))
      (dom/html
        {:lang "en"}

        (apply dom/head nil (common/head release?))

        (dom/body
          {:id "sulo-store"}
          (common/navbar nil)
          (dom/div {:className "cover-photo" :style {:background-image (str "url(" (:cover store) ")")}}
            (dom/div {:id "stream-container"
                      :className "row column"}))

          (dom/div {:className "store-nav"}
            (dom/div {:className "row column"}
              (dom/ul {:className "menu"}
                      (dom/li nil (dom/a nil "Sheets"))
                      (dom/li nil (dom/a nil "Pillows"))
                      (dom/li nil (dom/a nil "Duvets")))))

          (dom/div {:className "items"}
            (apply dom/div {:className "featured-items-container row small-up-2 medium-up-4"}
              (map (fn [p]
                     (common/product-element p))
                   (shuffle (apply concat (take 4 (repeat (:goods store))))))))

          ;(dom/script {:src "https://webrtc.github.io/adapter/adapter-latest.js"})
          (dom/script {:src "/lib/videojs/video.min.js"})
          (dom/script {:src "/lib/videojs/videojs-media-sources.min.js"})
          (dom/script {:src "/lib/videojs/videojs.hls.min.js"})
          (dom/script {:src "/lib/red5pro/red5pro-sdk.min.js"})
          (dom/script {:src  (common/budget-js-path release?)
                       :type common/text-javascript})

          (common/inline-javascript ["env.web.main.runstream()"]))))))