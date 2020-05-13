To publish to the Central Repository, first make sure that `~/.m2/settings.xml`
has your SonaType credentials, then do:

```
$ mvn -pl runtime clean deploy -P release
```
