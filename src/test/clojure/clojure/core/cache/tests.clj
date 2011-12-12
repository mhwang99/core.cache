;   Copyright (c) Rich Hickey. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns ^{:doc "A caching library for Clojure."
      :author "Fogus"}
  clojure.core.cache.tests
  (:use [clojure.core.cache] :reload-all)
  (:use [clojure.test])
  (:import [clojure.core.cache BasicCache FIFOCache LRUCache TTLCache LUCache LIRSCache]))

(deftest test-basic-cache-lookup
  (testing "that the BasicCache can lookup as expected"
    (is (= :robot (lookup (miss (BasicCache. {}) '(servo) :robot) '(servo))))))

(defn do-ilookup-tests [c]
  (are [expect actual] (= expect actual)
       1   (:a c)
       2   (:b c)
       42  (:X c 42)
       nil (:X c)))

(defn do-assoc [c]
  (are [expect actual] (= expect actual)
       1   (:a (assoc c :a 1))
       nil (:a (assoc c :b 1))))

(defn do-dissoc [c]
  (are [expect actual] (= expect actual)
       2   (:b (dissoc c :a))
       nil (:a (dissoc c :a))
       nil (:b (-> c (dissoc :a) (dissoc :b)))
       0   (count (-> c (dissoc :a) (dissoc :b)))))

(defn do-getting [c]
  (are [actual expect] (= expect actual)
       (get c :a) 1
       (get c :e) nil
       (get c :e 0) 0
       (get c :b 0) 2
       (get c :f 0) nil
       
       (get-in c [:c :e]) 4
       (get-in c '(:c :e)) 4
       (get-in c [:c :x]) nil
       (get-in c [:f]) nil
       (get-in c [:g]) false
       (get-in c [:h]) nil
       (get-in c []) c
       (get-in c nil) c
       
       (get-in c [:c :e] 0) 4
       (get-in c '(:c :e) 0) 4
       (get-in c [:c :x] 0) 0
       (get-in c [:b] 0) 2
       (get-in c [:f] 0) nil
       (get-in c [:g] 0) false
       (get-in c [:h] 0) 0
       (get-in c [:x :y] {:y 1}) {:y 1}
       (get-in c [] 0) c
       (get-in c nil 0) c))

(defn do-finding [c]
  (are [expect actual] (= expect actual)
       (find c :a) [:a 1]
       (find c :b) [:b 2]
       (find c :c) nil
       (find c nil) nil))

(defn do-contains [c]
  (are [expect actual] (= expect actual)
       (contains? c :a) true
       (contains? c :b) true
       (contains? c :c) false
       (contains? c nil) false))


(def big-map {:a 1, :b 2, :c {:d 3, :e 4}, :f nil, :g false, nil {:h 5}})
(def small-map {:a 1 :b 2})

(deftest test-basic-cache-ilookup
  (testing "counts"
    (is (= 0 (count (BasicCache. {}))))
    (is (= 1 (count (BasicCache. {:a 1})))))
  (testing "that the BasicCache can lookup via keywords"
    (do-ilookup-tests (BasicCache. small-map)))
  (testing "assoc and dissoc for BasicCache"
    (do-assoc (BasicCache. {}))
    (do-dissoc (BasicCache. {:a 1 :b 2})))
  (testing "that get and cascading gets work for BasicCache"
    (do-getting (BasicCache. big-map)))
  (testing "that finding works for BasicCache"
    (do-finding (BasicCache. small-map)))
  (testing "that contains? works for BasicCache"
    (do-contains (BasicCache. small-map))))

(deftest test-fifo-cache-ilookup
  (testing "that the FifoCache can lookup via keywords"
    (do-ilookup-tests (FIFOCache. small-map clojure.lang.PersistentQueue/EMPTY 2)))
  (testing "assoc and dissoc for FifoCache"
    (do-assoc (FIFOCache. {} clojure.lang.PersistentQueue/EMPTY 2))
    (do-dissoc (FIFOCache. {:a 1 :b 2} clojure.lang.PersistentQueue/EMPTY 2)))
  (testing "that get and cascading gets work for FifoCache"
    (do-getting (FIFOCache. big-map clojure.lang.PersistentQueue/EMPTY 2)))
  (testing "that finding works for FifoCache"
    (do-finding (FIFOCache. small-map clojure.lang.PersistentQueue/EMPTY 2)))
  (testing "that contains? works for FifoCache"
    (do-contains (FIFOCache. small-map clojure.lang.PersistentQueue/EMPTY 2))))

(deftest test-lru-cache-ilookup
  (testing "that the LRUCache can lookup via keywords"
    (do-ilookup-tests (LRUCache. small-map {} 0 2)))
  (testing "assoc and dissoc for LRUCache"
    (do-assoc (LRUCache. {} {} 0 2))
    (do-dissoc (LRUCache. {:a 1 :b 2} {}  0 2))))

(deftest test-ttl-cache-ilookup
  (testing "that the TTLCache can lookup via keywords"
    (do-ilookup-tests (TTLCache. small-map {} 2)))
  (testing "assoc and dissoc for LRUCache"
    (do-assoc (TTLCache. {} {} 2))
    (do-dissoc (TTLCache. {:a 1 :b 2} {} 2))))

(deftest test-lu-cache-ilookup
  (testing "that the LUCache can lookup via keywords"
    (do-ilookup-tests (LUCache. small-map {} 2)))
  (testing "assoc and dissoc for LRUCache"
    (do-assoc (LUCache. {} {}  2))
    #_(do-dissoc (LUCache. {:a 1 :b 2} {} 2))))

;; # LIRS

(defn- lirs-map [lirs]
  {:cache (.cache lirs)
   :lruS (.lruS lirs)
   :lruQ (.lruQ lirs)
   :tick (.tick lirs)
   :limitS (.limitS lirs)
   :limitQ (.limitQ lirs)})

(deftest test-LIRSCache
  (testing "that the LIRSCache can lookup as expected"
    (is (= :robot (lookup (miss (seed (LIRSCache. {} {} {} 0 1 1) {}) '(servo) :robot) '(servo)))))

  (testing "a hit of a LIR block:

L LIR block
H HIR block
N non-resident HIR block


          +-----------------------------+   +----------------+
          |           HIT 4             |   |     HIT 8      |
          |                             v   |                |
          |                                 |                |
    H 5   |                           L 4   |                v
    H 3   |                           H 5   |
    N 2   |                           H 3   |              L 8
    L 1   |                           N 2   |              L 4
    N 6   |                           L 1   |              H 5
    N 9   |                           N 6   |              H 3
    L 4---+ 5                         N 9   | 5            N 2     5
    L 8     3                         L 8---+ 3            L 1     3

      S     Q                           S     Q              S     Q

"
    (let [lirs (LIRSCache. {:1 1 :3 3 :4 4 :5 5 :8 8}
                           {:5 7 :3 6 :2 5 :1 4 :6 3 :9 2 :4 1 :8 0}
                           {:5 1 :3 0} 7 3 2)]
      (testing "hit 4"
        (is (= (lirs-map (hit lirs :4))
               (lirs-map (LIRSCache. {:1 1 :3 3 :4 4 :5 5 :8 8}
                                     {:5 7 :3 6 :2 5 :1 4 :6 3 :9 2 :4 8 :8 0}
                                     {:5 1 :3 0} 8 3 2)))))
      (testing "hit 8 prunes the stack"
        (is (= (lirs-map (-> lirs (hit :4) (hit :8)))
               (lirs-map (LIRSCache. {:1 1 :3 3 :4 4 :5 5 :8 8}
                                     {:5 7 :3 6 :2 5 :1 4 :4 8 :8 9}
                                     {:5 1 :3 0} 9 3 2)))))))
  (testing "a hit of a HIR block:

L LIR block
H HIR block
N non-resident HIR block


                     HIT 3                                  HIT 5
         +-----------------------------+         +------------------+-----+
         |                             |         |                  |     |
    L 4  |+----------------------------+-----+   |                  v     |
    L 8  ||                            v     |   |                        |
    H 5  ||                                  v   |                H 5     v
    H 3-- |                          L 3         |                L 3
    N 2   | 5                        L 4     1   |                L 4     5
    L 1---+ 3                        L 8     5---+                L 8     1

      S     Q                          S     Q                      S     Q

"
    (let [lirs (LIRSCache. {:1 1 :3 3 :4 4 :5 5 :8 8}
                           {:4 9 :8 8 :5 7 :3 6 :2 5 :1 4}
                           {:5 1 :3 0} 9 3 2)]
      (testing "hit 3 prunes the stack and moves oldest block of lruS to lruQ"
        (is (= (lirs-map (hit lirs :3))
               {:cache {:1 1 :3 3 :4 4 :5 5 :8 8}
                :lruS {:3 10 :4 9 :8 8}
                :lruQ {:1 10 :5 1}
                :tick 10 :limitS 3 :limitQ 2})))
      (testing "hit 5 adds the block to lruS"
        (is (= (lirs-map (-> lirs (hit :3) (hit :5)))
               {:cache {:1 1 :3 3 :4 4 :5 5 :8 8}
                :lruS {:5 11 :3 10 :4 9 :8 8}
                :lruQ {:5 11 :1 10}
                :tick 11 :limitS 3 :limitQ 2})))))
  (testing "a miss:

L LIR block
H HIR block
N non-resident HIR block


                     MISS 7                          MISS 9                    MISS 5
             ---------------------+-----+    -----------------+-----+     +-------------------+
                                  |     |                     v     |     |                   |
                                  v     |                           |     |                   v
                                        |                   H 9  + -| - - +
                                H 7     |                   H 7  |  |                       L 5  +--+
    H 5                         H 5     v                   N 5- +  v                       H 9  |  v
    L 3                         L 3                         L 3                             N 7  |
    L 4     5                   L 4     7                   L 4     9                       L 3  |  8
    L 8     1                   L 8     5                   L 8--+  7                       L 4  |  9
                                                                 +-------------------------------+
      S     Q                     S     Q                     S     Q                         S     Q


"
    (let [lirs (LIRSCache. {:1 1 :3 3 :4 4 :5 5 :8 8}
                           {:5 11 :3 10 :4 9 :8 8}
                           {:5 11 :1 10} 11 3 2)]
      (testing "miss 7 adds the block to lruS and lruQ and removes the oldest block in lruQ"
        (is (= (lirs-map (miss lirs :7 7))
               {:cache {:3 3 :4 4 :5 5 :8 8 :7 7}
                :lruS {:7 12 :5 11 :3 10 :4 9 :8 8}
                :lruQ {:7 12 :5 11}
                :tick 12 :limitS 3 :limitQ 2})))
      (testing "miss 9 makes 5 a non-resident HIR block"
        (is (= (lirs-map (-> lirs (miss :7 7) (miss :9 9)))
               {:cache {:3 3 :4 4 :8 8 :7 7 :9 9}
                :lruS {:9 13 :7 12 :5 11 :3 10 :4 9 :8 8}
                :lruQ {:9 13 :7 12}
                :tick 13 :limitS 3 :limitQ 2})))
      (testing "miss 5, a non-resident HIR block becomes a new LIR block"
        (is (= (lirs-map (-> lirs (miss :7 7) (miss :9 9) (miss :5 5)))
               {:cache {:3 3 :4 4 :8 8 :9 9 :5 5}
                :lruS {:5 14 :9 13 :7 12 :3 10 :4 9}
                :lruQ {:8 14 :9 13}
                :tick 14 :limitS 3 :limitQ 2}))))))
