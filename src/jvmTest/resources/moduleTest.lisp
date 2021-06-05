
(import #array array)
(defn $prod [$x $y] (* $x $y))

(import (module
  (defn $factorial [[$init, Integer, 'Number to calculate the factorial of']]
    (if (== 1 $init)
        (return 1)
        ($prod $init ($recurse (- $init 1)))
    )
  )

  (export $factorial)
))


(factorial 4)
