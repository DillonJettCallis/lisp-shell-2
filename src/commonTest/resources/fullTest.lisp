(def $git @"C:\\Programs\\cmder\\vendor\\git-for-windows\\cmd\\git.exe")

(def $dev development)
(def $sprint feature/sprint)

(defn $currentBranch [] (git branch --show-current))
(defn $fork [$name] (let {$branchName (+ 'feature/' $name)} (git checkout -b $branchName)))

(defn $features []
  (let {
    $raw (git branch )
    $all (parse/lines $raw)
    } (array/filter (fn [$line] (string/contains 'feature/' $line)) $all)
  )
)

(defn $feat [] (let {$cwd (path "C:\\Users\\Dillon\\Projects\\Scriptly")} (features)))

(defn $feat [[$arg String 'first art'],
                 [$thing Integer 'second arg'],
                 [$action Function 'A function from x -> y']]
  ($action $arg $thing)
)

(defn $feat [$arg, $thing, $action]
  (do
    (assertType $arg String 'first art'),
    (assertType $thing Integer 'second arg'),
    (assertType $action Function 'A function from x -> y')

    ($action $arg $thing)
  )
)

