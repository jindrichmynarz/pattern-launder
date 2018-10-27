# pattern-launder

Get instances of RDF triple patterns from [LOD Laundromat](http://lodlaundromat.org). For a given RDF triple pattern, the tools looks up datasets that might have its instances via [LOD Laundromat Index](http://index.lodlaundromat.org), then retrieves the instances from these datasets exposed via [Triple Pattern Fragments](http://www.hydra-cg.com/spec/latest/triple-pattern-fragments) endpoints.

Does basically some of what [Frank](https://github.com/LOD-Laundromat/Frank) does.

## How to use it?

You can use the tool via its command-line interface. Either download a [released executable](https://github.com/jindrichmynarz/pattern-launder/releases/latest) of the tool or compile it yourself via [Leiningen](https://leiningen.org) by running `lein bin`. In order to run the tool, you need to have Java (version 1.6 or newer) installed.

The `pattern_launder` executable reads RDF triple patterns from the standard input stream and writes its results to the standard output stream. This makes it possible to use it in data processing pipelines. The input triple patterns are provided in CSV format with the columns `subject`, `predicate`, and `object`. The values of these columns are either RDF terms, serialized according to the [Triple Pattern Fragments](http://www.hydra-cg.com/spec/latest/triple-pattern-fragments/#controls) specification, or empty, indicating a variable. For example, the following are some triple patterns for [SKOS](https://www.w3.org/TR/skos-reference):

```csv
subject,predicate,object
,http://www.w3.org/2004/02/skos/core#relatedMatch,
,http://www.w3.org/2004/02/skos/core#closeMatch,
,http://www.w3.org/2004/02/skos/core#narrowMatch,
```

By default, the tool retrieves data matching each triple pattern and outputs it in the [N-Triples](https://www.w3.org/TR/n-triples) RDF syntax. Alternatively, you can ask for estimated counts of triples matching given triple patterns by using the `--counts` command-line switch. In that case, a `count` column with the estimated counts will be added to the input CSV with triple patterns, like this:

```
subject,predicate,object,count
,http://www.w3.org/2004/02/skos/core#relatedMatch,,118806
,http://www.w3.org/2004/02/skos/core#closeMatch,,298452
,http://www.w3.org/2004/02/skos/core#narrowMatch,,2738
```

You can see all the command-line options of the tool by running `./pattern_launder --help`.

For a more detailed example, you can see [how to estimate popularity of vocabularies](examples/vocabulary-usage/README.md).

Alternatively, you can use the tool as a dependency in your Clojure programs. The `pattern-launder.core` namespace exposes two public functions matching the command-line functionality. The function `triples` obtains triples matching a given triple pattern (a map with `:subject`, `:predicate`, and `:object` keys) and returns a lazy sequence of triples in [JSON-LD](https://w3c.github.io/json-ld-syntax). The function `triple-count` returns an estimated count of triples matching the given triple pattern, provided in the same format as above.

## License

Copyright © 2018 Jindřich Mynarz

Distributed under the Eclipse Public License version 1.0.
