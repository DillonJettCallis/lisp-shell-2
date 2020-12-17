(do

(def $dev development)
(def $sprint feature/sprint)
(def $weDev feature/wholesaleExcellenceDev)
(def $weSit feature/wholesaleExcellenceSIT)
(def $weUat feature/wholesaleExcellenceUAT)

(def $branchBases {$weUat $dev, $weSit $weUat, $weDev $weSit, $sprint $dev, $dev release })


(defn $fetch [] (git fetch -p))
(defn $switch [$branch] (do ($fetch) (git checkout $branch) (git pull) ))
(defn $fork [$name] (git checkout -b (+ 'feature/' $name)))
(defn $currentBranch [] (String.trim (git branch --show-current)))
(defn $publish [] (git push -u origin ($currentBranch)))
(defn $commit [$short, $id, $long] (if (nil $id) (echo 'You need to pass a V1 Id') (do (git add .) (git commit -m (+ $short '\n\n' $id '\n\n' (if (nil $long) '' $long) )))))
(defn $delete [$branch] (do
  (if (== $branch ($currentBranch)) (switch (if (== $branch master) release master)))
  (echo `Deleting branch $branch `)
  (git branch -D $branch)
))

(let {
  $compose (fn [$first, $second] (fn [$arg] ($second ($first $arg))))
  $strip (eval `(char) => (branch) => { const index = branch.indexOf(char); if (index === -1) return branch; else return branch.substr(index + 1); }`)
  $stripStar (strip '*')
  $stripNameOff (strip '/')
  $trim (eval `str => str.trim()`)
  $action (eval `(local, remote, del) => local.filter(it => !remote.includes(it)).forEach(it => del(it))`)
} (
    defn $cleanBranches []
        (let {
            $local (Array.map (Parse.lines (git branch)) ($compose $stripStar $trim))
            $remote (Array.map (Parse.lines (git branch -r)) ($compose $stripNameOff $trim))
            }
        ($action $local $remote $delete)
        )
  )
)

(defn $branches [] ((get $Array map) ((get $Parse lines) (git branch)) (eval `line => line.replace('*', '').trim()`)))


(defn $updateBranch [$src, $dest] (do (def $message "Merged $src into $dest ") ($fetch) (git checkout $src) (git pull) (git checkout $dest) (git pull) (git merge $src)) )
(defn $updateDev [] (updateBranch release $dev) )
(defn $updateSprint [] (updateBranch $dev $sprint) )

(defn $from [$target] (let {$base ($currentBranch)} ($updateBranch $target $base)))
(defn $into [$target] (let {$base ($currentBranch)} (do (def $message "Merged $base into $target ") (switch $target) (git merge $base) )))

(defn $lift [$target] (do (git stash) (switch $target) (git stash apply)))

(defn $build [] (gradlew build))
(defn $kill [$name] (taskkill /F /IM $name))


(defn $update [$target]
  (let {
    $base, (get $branchBases $target)
  }, (if (nil $base) (echo "No base branch for '$target' ") (updateBranch $base $target) ))
)

)

