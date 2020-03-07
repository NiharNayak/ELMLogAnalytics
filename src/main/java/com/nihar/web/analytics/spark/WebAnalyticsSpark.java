package com.nihar.web.analytics.spark;

import com.nihar.web.analytics.bin.CompositeKeyPojo;
import com.nihar.web.analytics.bin.IpAndAvgTime;
import java.util.concurrent.TimeUnit;
import lombok.val;
import org.apache.spark.Partitioner;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFunction;
import org.apache.spark.storage.StorageLevel;
import scala.Serializable;
import scala.Tuple2;

public class WebAnalyticsSpark implements Serializable {
  private JavaPairRDD<CompositeKeyPojo, CompositeKeyPojo> rawInputToKeyRdd;
  private JavaPairRDD<String, CompositeKeyPojo> timeWindowSessionize;

  public WebAnalyticsSpark(JavaPairRDD<CompositeKeyPojo, CompositeKeyPojo> rawInput) {
    this.rawInputToKeyRdd = rawInput;
    rawInputToKeyRdd.persist(StorageLevel.MEMORY_AND_DISK());
  }

  /**
   * Since we want to group the time window to sessions we need to sort the time across the whole
   * data set if we take multiple partitions then the one order can't nbe maintained the logic for
   * windowing can't be achieved Creating a dummy Petitioner to push all the value to one reduce
   * node (container).
   *
   * @param sessionWindowSizeInMinutes
   */
  public void prepProcessAndMakeSessionFromLogTimestamp(int sessionWindowSizeInMinutes) {
    // Session , All-Other-Value
    this.timeWindowSessionize =
        rawInputToKeyRdd
            .repartitionAndSortWithinPartitions(
                /** Taking 1 partitions because we want to deal with the value sequentially. */
                new Partitioner() {
                  @Override
                  public int numPartitions() {
                    return 1;
                  } // With multiple partitions global order can't be maintained.

                  @Override
                  public int getPartition(Object key) {
                    return 0; // always returns 0 to make sure all the key and value goes to 1
                    // reducer.
                  }
                })
            .mapToPair(
                new PairFunction<
                    Tuple2<CompositeKeyPojo, CompositeKeyPojo>, String, CompositeKeyPojo>() {
                  // Initializing conditions.
                  int sessionCount = 1;
                  Long startTime = null;
                  String sessionKey = null;
                  Long currentTimeStamp;
                  CompositeKeyPojo compositeKey = null;
                  // Making timestamp to session based on window size.
                  @Override
                  public Tuple2<String, CompositeKeyPojo> call(
                      Tuple2<CompositeKeyPojo, CompositeKeyPojo> stringCompositeKeyPojoTuple2) {
                    compositeKey = stringCompositeKeyPojoTuple2._2();
                    currentTimeStamp = compositeKey.getTimeStamp();
                    // Initialization of session window.
                    if (startTime == null) {
                      startTime = compositeKey.getTimeStamp();
                    }
                    // Time Window boundary condition check.
                    if (TimeUnit.MILLISECONDS.toSeconds(currentTimeStamp - startTime)
                        > TimeUnit.MINUTES.toSeconds(sessionWindowSizeInMinutes)) {
                      sessionCount += 1;
                      startTime = currentTimeStamp;
                    }
                    sessionKey = "Session_" + sessionCount;
                    return new Tuple2<>(sessionKey, compositeKey);
                  }
                })
            // persists the sessionize data to reuse several times.
            .persist(StorageLevel.MEMORY_AND_DISK());
  }

  public JavaPairRDD<String, Integer> getPageHitByIpPerSession() {
    return timeWindowSessionize
        .mapToPair(
            line ->
                new Tuple2<>(
                    // Making Session and Ip as key to count the page hit perIp per Session
                    line._1() + "\t" + line._2().getIp(), 1))
        .reduceByKey((Function2<Integer, Integer, Integer>) Integer::sum);
  }

  /**
   * Used for calculating unique Url hit per session.
   *
   * @return
   */
  public JavaPairRDD<String, Integer> getUniqueURLHitPerSession() {
    return timeWindowSessionize
        .mapToPair(line -> new Tuple2<>(line._1(), line._2().getUrl()))
        .distinct() // Get distinct URl for counting unique URl per session.
        .mapToPair(line -> new Tuple2<>(line._1(), 1))
        .reduceByKey((Function2<Integer, Integer, Integer>) Integer::sum);
  }

  /**
   * Used for calculating unique Url hit per session.
   *
   * @return
   */
  public JavaPairRDD<String, Integer> getUniqueURLHitPerSessionPerIP() {
    return timeWindowSessionize
        .mapToPair(line -> new Tuple2<>(line._1() + "\t" + line._2().getIp(), line._2().getUrl()))
        .distinct() // Get distinct URl for counting unique URl per session.
        .mapToPair(line -> new Tuple2<>(line._1(), 1))
        .reduceByKey((Function2<Integer, Integer, Integer>) Integer::sum);
  }

  /**
   * Determine the Avg session time per Ip.
   *
   * @return
   */
  public JavaRDD<IpAndAvgTime> getAvgSessionTimePerIp() {
    // Not sure about hot to determine the session ,
    // Since don't have start time and end time of session by Ip/User.
    // Assuming total time spent per ip per unique URL for this solution.
    // The calculating average time spent per Ip/User.
    JavaPairRDD<String, Long> timeSpentPerUrlPerIp = rawInputToKeyRdd
    .mapToPair(
        k -> new Tuple2<>(k._1().getIp() + "\t" + k._1().getUrl(), k._2().getTimeStamp()))
    .groupByKey()
    // (Ip and Url) -> timeSpent per URL
    .mapToPair(
        stringIterableTuple2 -> {
          val itr = stringIterableTuple2._2().iterator();
          long max = Long.MIN_VALUE;
          long min = Long.MAX_VALUE;
          while (itr.hasNext()) {
            long current =  itr.next();
            if (current > max) {
              max = current;
            }
            if (current < min) {
              min = current;
            }
          }
          // Calculate the Total time-spent.
          return new Tuple2<>(stringIterableTuple2._1()+","+max+","+min, max - min);
        });
    return timeSpentPerUrlPerIp.mapToPair(l -> new Tuple2<>(l._1().split("\t")[0], l._2()))
        // Finding average session time (in seconds) per Ip.
        .groupByKey()
        .map(
            stringIterableTuple2 -> {
              val itr = stringIterableTuple2._2().iterator();
              long sum = 0;
              long count = 0;
              while (itr.hasNext()) {
                long current = itr.next();
                sum += current;
                count += 1;
              }
              // Calculate the Total time-spent.
              return new IpAndAvgTime(
                  stringIterableTuple2._1(), TimeUnit.MILLISECONDS.toSeconds((sum / count)));
            });
  }
}
