# Garden

Garden is a library for rendering CSS in Clojure and ClojureScript.
Conceptually similar to [Hiccup](https://github.com/weavejester/hiccup), it uses
vectors to represent rules and maps to represent declarations. It is designed
for stylesheet authors who are interested in what's possible when you trade a
preprocessor for a programming language.

## Getting Started

Add the following dependency to your `project.clj` file:

[![Clojars Project](http://clojars.org/garden/latest-version.svg)](http://clojars.org/garden)

Garden 1.2.5 and below requires Clojure 1.6.0 and is known to work with
ClojureScript 0.0-2342. However, starting with Garden 1.3.0 Garden requires
Clojure 1.7 and ClojureScript 1.7.x to leverage a unified syntax with
[reader conditionals](http://dev.clojure.org/display/design/Reader+Conditionals),
and other major changes in the compiler and repl in Clojurescript.

Build Cljs

	lein build-cljs

Start a node repl

	lein node-repl

Run Clj tests, along with a test runner

	lein test-clj

Run Cljs tests (on Node)

	lein test-cljs

## Developer Guide

A Guide for Syntax, Rules, Declarations, Plugins, and community-contributed wiki is under [https://github.com/noprompt/garden/wiki].

Please contribute!

## Examples

Coming Soon.

## Contributors

Listed by first commit:

* [noprompt](https://github.com/noprompt)
* [malcolmsparks](https://github.com/malcolmsparks)
* [jeluard](https://github.com/jeluard)
* [ToBeReplaced](https://github.com/ToBeReplaced)
* [migroh](https://github.com/migroh)
* [priyatam](https://github.com/priyatam)

## Thanks

A big thank you goes out to [@weavejester](https://github.com/weavejester) for
creating Hiccup, [@briancarper](https://github.com/briancarper) for creating
[gaka](https://github.com/briancarper/gaka/), and
[@paraseba](https://github.com/paraseba) for creating
[cssgen](https://github.com/paraseba/cssgen). I learned a lot
from studying the source code of these libraries (and borrowed several ideas
from them). Writing this library would have been significantly more difficult
without the hard work of these individuals.

I'd also like to thank [@jhardy](https://github.com/jhardy) for putting up with
random questions and pushing me to keep working on this library.

Thanks to everyone in `#clojure` on IRC for answering my questions and being
patient with me. If you're looking for an example of a great community, look no
further.

## License

Copyright © 2013-2015 Joel Holdbrooks.

Distributed under the Eclipse Public License, the same as Clojure.
