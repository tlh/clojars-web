(ns clojars.utils
  (:import java.security.MessageDigest
           java.math.BigInteger))

(defn sha1 [& s]
  (when-let [s (seq s)]
    (let [md (MessageDigest/getInstance "SHA")]
      (.update md (.getBytes (apply str s)))
      (format "%040x" (BigInteger. 1 (.digest md))))))

(let [chars (map char
                 (mapcat (fn [[x y]] (range (int x) (inc (int y))))
                         [[\a \z] [\A \Z] [\0 \9]]))]
  (defn rand-string
    "Generates a random string of [A-z0-9] of length n."
    [n]
    (apply str (take n (map #(nth chars %)
                            (repeatedly #(rand (count chars))))))))

(defn gen-salt []
  (rand-string 16))

(defn conj-when [coll test x]
  (if test
    (conj coll x)
    coll))