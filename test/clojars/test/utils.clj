(ns clojars.test.utils
  (:use [clojure.test]
        [clojars.utils]))

(deftest test-sha1
  (is (= (sha1 "") "da39a3ee5e6b4b0d3255bfef95601890afd80709"))
  (is (= (sha1 "foobar") (sha1 "foo" "bar")))
  (is (= (sha1 "foo bar") "3773dea65156909838fa6c22825cafe090ff8030")))

(deftest test-gen-salt
  (is (= (count (gen-salt)) 16))
  (is (not= (gen-salt) (gen-salt))))
