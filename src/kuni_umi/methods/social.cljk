(ns kuni-umi.methods.social
  "Actor adapter over the shared social publication membrane."
  (:require [etzhayyim.social.publication :as publication]))

(def config {:actor-id "kuni-umi" :display-name "国産み — Kuni-umi"})
(def DISCLAIMER (publication/disclaimer config))

(defn draft-observation-post
  ([subject body sources]
   (publication/draft-observation-post config subject body sources))
  ([subject body sources author]
   (publication/draft-observation-post config subject body sources author)))

(defn build-live [& args]
  (apply publication/build-live config args))
