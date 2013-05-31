# Suggester Plugin for Elasticsearch

This plugin uses the FSTSuggester, the AnalyzingSuggester or the FuzzySuggester from Lucene to create suggestions from a certain field for a specified term instead of returning the whole document data.

Feel free to comment, improve and help - I am thankful for any insights, no matter whether you want to help with elasticsearch, lucene or my other flaws I will have done for sure.

Oh and in case you have not read it above:

In case you want to contact me, drop me a mail at alexander@reelsen.net

## Breaking changes for elasticsearch 0.90 

Because elasticsearch now comes with its own suggest API (not based on in-memory automatons per shard), big parts of this plugin needs to be changed.

### REST endpoints have been moved

Both REST endpoints have been moved. The `/_suggest` endpoint now resides at `__suggest`. Refreshing has changed from `_suggestRefresh` to `__suggestRefresh`.
I do not like this renaming either, but I have not yet got the ieda of a better name.
I am totally open for better names. This is a WIP until elasticsearch 1.0 is released.

### Package names have been moved

Everything is now in the `de.spinscale` package name space in order to avoid clashes. This means, if you are using the request builder classes, you will have to change your application.

## Installation

If you do not want to work on the repository, just use the standard elasticsearch plugin command (inside your elasticsearch/bin directory)

```
bin/plugin -install de.spinscale/elasticsearch-plugin-suggest/0.90.1-0.7
```

### Compatibility


**Note**: Please make sure the plugin version matches with your elasticsearch version. Follow this compatibility matrix

    ---------------------------------------
    | suggest Plugin   | ElasticSearch    |
    ---------------------------------------
    | master           | 0.90.1 -> master |
    ---------------------------------------
    | 0.90.1-0.7       | 0.90.1           |
    ---------------------------------------
    | 0.90.0-0.6.*     | 0.90.0           |
    ---------------------------------------
    | 0.20.5-0.5       | 0.20.5 -> 0.20.6 |
    ---------------------------------------
    | 0.20.2-0.4       | 0.20.2 -> 0.20.4 |
    ---------------------------------------
    | 0.19.12-0.2      | 0.19.12          |
    ---------------------------------------
    | 0.19.11-0.1      | 0.19.11          |
    ---------------------------------------


### Development

If you want to work on the repository

 * Clone this repo with `git clone git://github.com/spinscale/elasticsearch-suggest-plugin.git`
 * Checkout the tag (find out via `git tag`) you want to build with (possibly master is not for your elasticsearch version)
 * Run: `mvn clean package -DskipTests=true` - this does not run any unit tests, as they take some time. If you want to run them, better run `mvn clean package`
 * Install the plugin: `/path/to/elasticsearch/bin/plugin -install elasticsearch-suggest -url file:///$PWD/target/releases/elasticsearch-suggest-$version.zip`

Alternatively you can now use this plugin via maven and include it via the sonatype repo likes this in your pom.xml (or any other dependency manager)

```
<repositories>
  <repository>
    <id>Sonatype</id>
    <name>Sonatype</name>
    <url>http://oss.sonatype.org/content/repositories/releases/</url>
  </repository>
</repositories>

<dependencies>
  <dependency>
    <groupId>de.spinscale</groupId>
    <artifactId>elasticsearch-suggest-plugin</artifactId>
    <version>0.90.1-0.7</version>
  </dependency>
  ...
<dependencies>
```

The maven repo can be visited at https://oss.sonatype.org/content/repositories/releases/de/spinscale/elasticsearch-plugin-suggest/

## Usage

### FST based suggestions

Fire up curl like this, in case you have a products index and the right fields - if not, read below how to setup a clean elasticsearch in order to support suggestions.

```
# curl -X POST 'localhost:9200/products1/product/__suggest?pretty=1' -d '{ "field": "ProductName.suggest", "term": "tischwäsche", "size": "10"  }'
{
  "suggest" : [ "tischwäsche", "tischwäsche 100", 
    "tischwäsche aberdeen", "tischwäsche acryl", "tischwäsche ambiente", 
    "tischwäsche aquarius", "tischwäsche atlanta", "tischwäsche atlas", 
    "tischwäsche augsburg", "tischwäsche aus", "tischwäsche austria" ]
}
```

As you can see, this queries the products index for the field `ProductName.suggest` with the specified term and size.

You can also use HTTP GET for getting suggestions - even with the `callback` and the `source` parameters like in any normal elasticsearch search.

You might want to check out the included unit test as well. I use a shingle filter in my examples, take a look at the files in `src/test/resources` directory.

### Full suggestions

With Lucene 4 (and the upgrade to elasticsearch 0.90.0) two new suggesters were added, one of them the [AnalyzingSuggester](http://lucene.apache.org/core/4_3_0/suggest/org/apache/lucene/search/suggest/analyzing/AnalyzingSuggester.html) and the [FuzzySuggester](http://lucene.apache.org/core/4_3_0/suggest/org/apache/lucene/search/suggest/analyzing/FuzzySuggester.html) based on the first one. Both have the great capability of returning the original form, but search on an analyzed one. Take this example (notice the search for a lowercase `b`, but getting back the original field name):

```
» curl -X POST localhost:9200/cars/car/__suggest -d '{ "field" : "name", "type": "full", "term" : "b", "analyzer" : "standard" }'

{"suggestions":["BMW 320","BMW 525d"],"_shards":{"total":5,"successful":5,"failed":0}}
```

*Note*: If you use type `full` or type `fuzzy`, the `similarity` parameter will not have any effect. In addition, these parameters are supported only for `full` and `fuzzy`:

* `analyzer`:
* `index_analyzer`:
* `search_analyzer`:

This suggester can even ignore stopwords if configured appropriately - but only if you disable position increments for stopwords. Use this mapping and index settings when creating an index:

```
curl -X DELETE localhost:9200/cars
curl -X PUT localhost:9200/cars -d '{
  "mappings" : {
    "car" : {
      "properties" : {
        "name" : {
          "type" : "multi_field",
          "fields" : {
            "name":    { "type": "string", "index": "not_analyzed" }
          }
        }
      }
    }
  },
  "settings" : {
    "analysis" : {
      "analyzer" : {
        "suggest_analyzer_stopwords" : {
          "type" : "custom",
          "tokenizer" : "standard",
          "filter" : [ "standard", "lowercase", "stopword_no_position_increment" ]
        },
        "suggest_analyzer_synonyms" : {
          "type" : "custom",
          "tokenizer" : "standard",
          "filter" : [ "standard", "lowercase", "my_synonyms" ]
        }
      },
      "filter" : {
        "stopword_no_position_increment" : {
          "type" : "stop",
          "enable_position_increments" : false
        },
        "my_synonyms" : {
          "type" : "synonym",
          "synonyms" : [ "jetta, bora" ]
        }
      }
    }
  }
}'


curl -X POST localhost:9200/cars/car -d '{ "name" : "The BMW ever" }'
curl -X POST localhost:9200/cars/car -d '{ "name" : "BMW 320" }'
curl -X POST localhost:9200/cars/car -d '{ "name" : "BMW 525d" }'
curl -X POST localhost:9200/cars/car -d '{ "name" : "VW Jetta" }'
curl -X POST localhost:9200/cars/car -d '{ "name" : "VW Bora" }'
```

Now when querying with a stopwords analyzer, you can even get back `The BMW ever`

```
» curl -X POST localhost:9200/cars/car/__suggest -d '{ "field" : "name", "type": "full", "term" : "b", "analyzer" : "suggest_analyzer_stopwords" }'
{"suggestions":["BMW 320","BMW 525d","The BMW ever"],"_shards":{"total":5,"successful":5,"failed":0}}
```

Or you could use synonyms (FYI: jetta and bora were the same cars, but named different in USA and Europe, so a search should return both)

```
» curl -X POST localhost:9200/cars/car/__suggest -d '{ "field" : "name", "type": "full", "term" : "vw je", "analyzer" : "suggest_analyzer_synonyms" }'

{"suggestions":["VW Bora","VW Jetta"],"_shards":{"total":5,"successful":5,"failed":0}}
```

### Full fuzzy suggestions

The FuzzySuggester uses LevenShtein distance to cater for typos.

```
» curl -X POST localhost:9200/cars/car/__suggest -d '{ "field" : "name", "type": "fuzzy", "term" : "bwm", "analyzer" : "standard" }'

{"suggestions":["BMW 320","BMW 525d"],"_shards":{"total":5,"successful":5,"failed":0}}
```

### Statistics

The `FuzzySuggester` and the `AnalyzingSuggester` suggesters contain a method to find out their size, which is also exposed as an own endpoint, in case you want to monitor memory consumption of the in-memory structures.

```
» curl localhost:9200/__suggestStatistics
{"_shards":{"total":2,"successful":2,"failed":0},"fstStats":{"cars-0":[{"analyzingsuggester-name-queryAnalyzer:suggest_analyzer_synonyms-indexAnalyzer:suggest_analyzer_synonyms":147},{"analyzingsuggester-name-queryAnalyzer:suggest_analyzer_stopwords-indexAnalyzer:suggest_analyzer_stopwords":126}]}}
```

### Configuration

Furthermore the suggest data is not updated, whenever you index a new product but every few minutes. The default is to update the index every 10 minutes, but you can change that in your elasticsearch.yml configuration:

```
suggest:
  refresh_interval: 600s
```

In this case the suggest indexes are refreshed every 10 minutes. This is also the default. You can use values like "10s", "10ms" or "10m" as with most other time based configuration settings in elasticsearch.

If you want to deactivate automatic refresh completely, put this in your elasticsearch configuration

```
suggest:
  refresh_disabled: true
```

If you want to refresh your FST suggesters manually instead of waiting for 10 minutes just issue a POST request to the `/__suggestRefresh` URL.

```
# curl -X POST 'localhost:9200/__suggestRefresh' 
# curl -X POST 'localhost:9200/products/product/__suggestRefresh' 
# curl -X POST 'localhost:9200/products/product/__suggestRefresh' -d '{ "field" : "ProductName.suggest" }'
```

## Usage from Java

```
SuggestRequest request = new SuggestRequest(index);
request.term(term);
request.field(field);
request.size(size);
request.similarity(similarity);

SuggestResponse response = node.client().execute(SuggestAction.INSTANCE, request).actionGet();
```

Refresh works like this - you can add an index and a field in the suggest refresh request as well, if you want to trigger it externally:

```
SuggestRefreshRequest refreshRequest = new SuggestRefreshRequest();
SuggestRefreshResponse response = node.client().execute(SuggestRefreshAction.INSTANCE, refreshRequest).actionGet();
```

You can also use the included builders

```
List<String> suggestions = new SuggestRequestBuilder(client)
            .field(field)
            .term(term)
            .size(size)
            .similarity(similarity)
            .execute().actionGet().suggestions();
```

```
SuggestRefreshRequestBuilder builder = new SuggestRefreshRequestBuilder(client);
builder.execute().actionGet();
```

## Thanks

* Shay ([@kimchy](http://twitter.com/kimchy)) for giving feedback
* David ([@dadoonet](http://twitter.com/dadoonet)) for pushing me to get it into the maven repo
* Adrien ([@jpountz](http://twitter.com/jpountz)) for helping me to understand the the AnalyzingSuggester details and having the idea for only creating the FST on the primary shard

## TODO

* Find and verify the absence of the current resource leak (open deleted files after lots of merging) with the new architecture
* Create the FST structure only on the primary shard and send it to the replica over the wire as byte array
* Allow deletion of of fields in cache instead of refresh
* Reenable the field refresh tests by checking statistics
* Also expose the guava cache statistics in the endpoint
* Stop working if the elasticsearch version does not match on startup
* Create a testing rule that does start a node/cluster only once per test run, not per test. This costs so much time.

## Changelog

* 2013-05-31: Removing usage of jdk7 only methods, version bumb to 0.90.1
* 2013-05-25: Changing suggest statistics format, fixing cache loading bug for analyzing/fuzzysuggester
* 2013-05-12: Fix for trying to access a closed index reader in AnalyzingSuggesster (i.e. after refresh) 
* 2013-05-12: Documentation update
* 2013-05-01: Added support for the fuzzy suggester
* 2013-04-28: Added support for the analyzing suggester and stopwords
* 2013-03-20: Moved to own package namespaces, changed REST endpoints in order to be compatible with elasticsearch 0.90
* 2013-01-18: Support for HTTP GET, together with JSONP and the source parameter.
* 2012-10-21: The REST urls can now be used without specifiying a type (which is unused at the moment anyway). You can use now the `$index/_suggest` and `$index/_suggestRefresh` urls
* 2012-10-21: Allowing to set `suggest.refresh_disabled = true` in order to deactivate automatic refreshing of the suggest index
* 2012-10-06: Shutting down the shard suggest service clean in case the instance is stopped or a shard is moved
* 2012-10-03: Starting cluster nodes in parallel in tests where several nodes are created (big speedup)
* 2012-10-03: Added tests for refreshing suggest in memory structures for one index or one field in an index only
* 2012-10-03: Replaced gradle with maven
* 2012-10-03: Updated to elasticsearch 0.19.10
* 2012-10-03: You can use the plugin now with a TransportClient for the first time. Yay!
* 2012-10-03: Using the FSTCompletionLookup now instead of the deprecated FSTLookup
* 2012-10-03: Pretty much a core rewrite today (having tests is great, even if they run 10 minutes). The suggest service is now implemented as service on shard level - no more central Suggester structures. The whole implementation is much cleaner and adheres way better to the whole elasticsearch architecture instead of being cowboy coded together - at least that is what I think.
* 2012-09-30: Updated to elasticsearch 0.19.9. Making TransportClients work again not spitting an exception on startup, when the module is in classpath. Updated this docs.
* 2012-06-25: Trying to fix another resource leak, which did not eat up diskspace but still did not close all files
* 2012-06-11: Fixing bad resoure leak due to not closing index reader properly - this lead to lots of deleted files, which still had open handles, thus taking up space
* 2012-05-13: Updated to work with elasticsearch 0.19.3
* 2012-03-07: Updated to work with elasticsearch 0.19.0
* 2012-02-10: Created `SuggestRequestBuilder` and `SuggestRefreshRequestBuilder` classes - results in easy to use request classes (check the examples and tests)
* 2011-12-29: The refresh interval can now be chosen as time based value like any other elasticsearch configuration
* 2011-12-29: Instead of having all nodes sleeping the same time and updating the suggester asynchronously, the master node now triggers the update for all slaves
* 2011-12-20: Added transport action (and REST action) to trigger reloading of all FST suggesters
* 2011-12-11: Fixed the biggest issue: Searchers are released now and do not leak
* 2011-12-11: Indexing is now done periodically
* 2011-12-11: Found a way to get the injector from the node, so I can build my tests without using HTTP requests

# HOWTO - the long version

This HOWTO will help you to setup a clean elasticsearch installation with the correct index settings and mappings, so you can use the plugin as easy as possible.
We will setup elasticsearch, index some products and query those for suggestions.

Get elasticsearch, install it, get this plugin, install it.

Add a suggest and a lowercase analyzer to your `elasticsearch/config/elasticsearch.yml` config file (or do it on index creation whatever you like)

```
index:
  analysis:
    analyzer:
      lowercase_analyzer:
        type: custom
        tokenizer: standard
        filter: [standard, lowercase] 
      suggest_analyzer:
        type: custom
        tokenizer: standard
        filter: [standard, lowercase, shingle]
```


Start elasticsearch and create a mapping. You can either create it via configuration in a file or during index creation. We will create an index with a mapping now

```
curl -X PUT localhost:9200/products -d '{
    "mappings" : {
        "product" : {
            "properties" : {
	        "ProductId":	{ "type": "string", "index": "not_analyzed" },
	        "ProductName" : {
	            "type" : "multi_field",
	            "fields" : {
	                "ProductName":  { "type": "string", "index": "not_analyzed" },
	                "lowercase":    { "type": "string", "analyzer": "lowercase_analyzer" },
	                "suggest" :     { "type": "string", "analyzer": "suggest_analyzer" }
	            }
	        }
            }
        }
    }
}'
```

Lets add some products

```
for i in 1 2 3 4 5 6 7 8 9 10 100 101 1000; do
    json=$(printf '{"ProductId": "%s", "ProductName": "%s" }', $i, "My Product $i")
    curl -X PUT localhost:9200/products/product/$i -d "$json"
done
```

## Queries

Time to query and understand the different analyzers

Queries the not analyzed field, returns 10 matches (default), always the full product name:

```
curl -X POST localhost:9200/products/product/_suggest -d '{ "field": "ProductName", "term": "My" }'
```

Queries the not analyzed field, returns nothing (because lowercase):

```
curl -X POST localhost:9200/products/product/_suggest -d '{ "field": "ProductName", "term": "my" }'
```

Queries the lowercase field, returns only the occuring word (which is pretty bad for suggests):

```
curl -X POST localhost:9200/products/product/_suggest -d '{ "field": 
"ProductName.lowercase", "term": "m" }'
```

Queries the suggest field, returns two words (this is the default length of the shingle filter), in this case "my" and "my product"

```
curl -X POST localhost:9200/products/product/_suggest -d '{ "field": "ProductName.suggest", "term": "my" }'
```

Queries the suggest field, returns ten product names as we started with the second word + another one due to the shingle

```
curl -X POST localhost:9200/products/product/_suggest -d '{ "field": "ProductName.suggest", "term": "product" }'
```

Queries the suggest field, returns all products with "product 1" in the shingle

```
curl -X POST localhost:9200/products/product/_suggest -d '{ "field": "ProductName.suggest", "term": "product 1" }'
```

The same query as above, but limits the result set to two 

```
curl -X POST localhost:9200/products/product/_suggest -d '{ "field": "ProductName.suggest", "term": "product 1", "size": 2 }'
```

And last but not least, typo finding, the query without similarity parameter set returns nothing:

```
curl -X POST localhost:9200/products/product/_suggest -d '{ "field": "ProductName.suggest", "term": "proudct", similarity: 0.7 }'
```

The similarity is a float between 0.0 and 1.0 - if it is not specified 1.0 is used, which means it must match exactly. I've found 0.7 ok for cases, when two letters were exchanged, but mileage may very as I tested merely on german product names.

With the tests I did, a shingle filter held the best results. Please check http://www.elasticsearch.org/guide/reference/index-modules/analysis/shingle-tokenfilter.html for more information about setup, like the default tokenization of two terms.

Now test with your data, come up and improve this configuration. I am happy to hear about your specific configuration for successful suggestion queries.
