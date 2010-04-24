(ns clojars.utils)

0(defn sha1 [& s]
  (when-let [s (seq s)]
    (let [md (java.security.MessageDigest/getInstance "SHA")]
      (.update md (.getBytes (apply str s)))
      (format "%040x" (java.math.BigInteger. 1 (.digest md))))))

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

(defmacro conj-when [coll test x]
  `(let [coll# ~coll]
     (if ~test
       (conj coll# ~x)
       coll#)))

(defmacro when->
  "Threaded version of when.  If test is true then returns the result of f
  applied to x and args.  Otherwise returns x."
  [x test f & [args]]
  `(let [x# ~x]
     (if ~test
       (f x# ~@args)
       x#)))

(defn unique-by
  "Returns a lazy sequence of the first values in coll for which f has a
   unique result.  If specified, seen is an initial set of values which
   should be treated as already seen.  The function f should have no
   side-effects."
  ([f coll] (unique-by f #{} coll))
  ([f seen coll]
     (lazy-seq
      (when-let [[x & xs] (seq (drop-while #(seen (f %)) coll))]
        (cons x (unique-by f (conj seen (f x)) xs))))))
