(import #array array)

(defn $fib [[$end Integer 'number of Fibonacci number to return. Min is 2']] (let
  {
    $recurseFib (fn [[$first Integer], [$second Integer], [$prev Array]] (
      if
      (<= $end (array/size $prev) )
      $prev
      ( let { $next (+ $first $second) } ( $recurse $second $next (array/add $prev $next) ) )
    ))
  }

  ($recurseFib 1, 1 [1, 1])
))


(array/fold
    (array/flatMap
        ($fib 10)
        (\ do [$1 (* $1 2)])
    )
    0
    $+
)
