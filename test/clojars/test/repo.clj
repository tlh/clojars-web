(ns clojars.test.repo
  (:use clojars.repo
        clojure.test))

(deftest test-parse-version
  (is (= (parse-version "12345678901234567890")
         {:qualifier "12345678901234567890"})))

(deftest test-compare-versions
  (is (< (compare-versions "0.5.6" "1.0.0") 0))
  (is (= (compare-versions "1.0.0" "1.0.0") 0))
  (is (> (compare-versions "4.0.0" "1") 0))
  (is (= (compare-versions "4.2-0" "4.2.0") 0))
  (is (< (compare-versions "1.0.0-alpha" "1.0.0-beta") 0))
  (is (< (compare-versions "1.0.0-beta" "1.0.0") 0))
  (is (< (compare-versions "1.0.0-rc1" "1.0.0-rc2") 0))
  (is (< (compare-versions "bar" "foo"))))

(deftest test-uncamel
  (is (uncamel "foo") "foo")
  (is (uncamel "fooBar") "foo-bar")
  (is (uncamel "fooBarBaz") "foo-bar-baz")
  (is (uncamel "fooBar12Baz") "foo-bar-baz")
  (is (uncamel "camelCaseIsReallyUgly") "camel-case-is-really-ugly"))

(deftest test-plural-of?
  (is (plural-of? "dependency" "dependencies"))
  (is (plural-of? "cow" "cows"))
  (is (not (plural-of? "dog" "cat"))))

;; (run-tests)