# Clojars

This is the source code for the clojars.org community jar repository.

## Hacking

Here's how to get Clojars up and running locally so you can hack on
it.

1. Install CouchDB and Leiningen.

2. Set your configuration in src/clojars/config.clj.

3. Fetch dependencies

    $ lein deps

4. Run

    $ java -cp 'src:classes:lib/*' -i src/clojars/core.clj -e '(clojars.core/main)'

5. If you want to use the SSH integration, create a local "clojars"
   user, set the path to its ~/.ssh/authorized_keys file in config.clj
   and compile the Nailgun client ng and put it in $PATH.

You may also wish to disallow password authentication for the clojars
user by adding the following to /etc/sshd_config.

    Match User clojars
    PasswordAuthentication no

The Leiningen [mailing list](http://groups.google.com/group/leiningen)
and the #leiningen or #clojure channels on Freenode are the best places
to bring up questions or suggestions.

Contributions are preferred as either Github pull requests or using
"git format-patch" as described at http://clojure.org/patches. Please
use standard indentation with no tabs, trailing whitespace, or lines
longer than 80 columns. If you've got some time on your hands, reading
http://mumble.net/~campbell/scheme/style.txt wouldn't hurt either.

## License

Copyright (C) 2009 Alex Osborne and Phil Hagelberg

Distributed under the Eclipse Public License, the same as Clojure
uses. See the file COPYING.
