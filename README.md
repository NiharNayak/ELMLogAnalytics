# ELMLogAnalytics

### How to Build
```
mvn clean install
```

### Run Spark job locally

```
java -cp target/elm-log-analytics-jar-with-dependencies.jar:. com.nihar.log.analytics.spark.SparkJob <input-file/dir> <out-put-dir> <sessionize-window-time(minute)*optional>
```

### Run on Yarn
###### Copy the target/elm-log-analytics.jar to cluster.

```
spark-submit --master yarn --deploy-mode client --class com.nihar.log.analytics.spark.SparkJob elm-log-analytics.jar <input-file/dir> <out-put-dir> <sessionize-window-time(minute)*optional>
```

All other details related to logic , Please refer to comment in source code.

Since the data is not so huge. we can test/cross-verify the count using command like

```
cat output/pageHitByIpPerSession/part-00000| grep '1.186.78.9' | head -10
gives out put
```

```
(Session_1,1.186.78.9,9)
(Session_21,1.186.78.9,1)
```

```
zcat data/2015_07_22_mktplace_shop_web_log_sample.log.gz |   grep '1.186.78.9' | wc -l
```
gives output
```
10
```
