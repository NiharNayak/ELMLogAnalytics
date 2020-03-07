package com.nihar.log.analytics.spark;

import com.google.common.base.Strings;
import com.nihar.log.analytics.bin.CompositeKeyPojo;
import com.nihar.log.analytics.bin.IpAndAvgTime;
import com.nihar.log.analytics.util.CommonUtil;
import lombok.val;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.storage.StorageLevel;
import scala.Tuple2;

/**
 * @author nihar.nayak
 *     <p>Main class for reading parsing input data and tranform them to required format. This class
 *     uses Spark-Core JavaRDD to trnasform the data.
 */
public class SparkJob {
  private static void usage() {
    System.out.println(
        "Usage: \n  java -cp elm-log-analytics-jar-with-dependencies.jar:. "
            + "com.nihar.log.analytics.spark.SparkJob <input-file/dir> "
            + "<out-put-dir> <sessionize-window-time(minute)*optional>)");
  }

  public static void main(String[] args) {
    if (args.length < 2) {
      usage();
      System.exit(1);
    }
    JavaSparkContext jc = new JavaSparkContext("local[*]", "WebAnalyticsSpark");
    final int sessionWindowSizeInMinutes = (args.length == 3) ? Integer.parseInt(args[2]) : 5;
    jc.getConf().set("spark.files.overwrite", "true");
    // The reason for taking key and value both equal is , we want to sort the CompositeKeyPojo
    // based on timeStamp. in value side ,
    // since shuffle and sort phase only sort the key, so we need to keep the key same as associated
    // value.
    JavaPairRDD<CompositeKeyPojo, CompositeKeyPojo> rawInputToKeyRdd =
        jc.textFile(args[0])
            .mapToPair(
                line -> {
                  val list = CommonUtil.getColumnsFrom(line);
                  val timeStamp = list.get(0);
                  val clientIp = list.get(2).split(":")[0].trim();
                  String url = list.get(11);
                  if (!Strings.isNullOrEmpty(timeStamp) && !Strings.isNullOrEmpty(timeStamp)) {
                    url = CommonUtil.getUrl(url);
                    long longTimeStamp = CommonUtil.getTimeStampFromStr(timeStamp);

                    val compositeKey = new CompositeKeyPojo();
                    compositeKey.setTimeStamp(longTimeStamp);
                    compositeKey.setUrl(url);
                    compositeKey.setIp(clientIp);
                    return new Tuple2<>(compositeKey, compositeKey);
                  }
                  return null;
                });

    WebAnalyticsSpark webAnalyticsSpark = new WebAnalyticsSpark(rawInputToKeyRdd);
    webAnalyticsSpark.prepProcessAndMakeSessionFromLogTimestamp(sessionWindowSizeInMinutes);
    webAnalyticsSpark
        .getPageHitByIpPerSession()
        .saveAsTextFile(args[1] + "/pageHitByIpPerSession/");
    webAnalyticsSpark
        .getUniqueURLHitPerSession()
        .saveAsTextFile(args[1] + "/uniqueURLHitPerSession");
    webAnalyticsSpark
        .getUniqueURLHitPerSessionPerIP()
        .saveAsTextFile(args[1] + "/uniqueURLHitPerSessionPerIP");

    JavaRDD<IpAndAvgTime> avgSessionTimePerIp =
        webAnalyticsSpark.getAvgSessionTimePerIp().persist(StorageLevel.MEMORY_AND_DISK());
    avgSessionTimePerIp
        .mapToPair(l -> new Tuple2<>(l.getIp(), l.getAvgTimeSpent()))
        .saveAsTextFile(args[1] + "/avgSessionTimePerIp");

    // Descending order to find top 10 values.
    val top10mostEngagingUsers = avgSessionTimePerIp.takeOrdered(10);
    System.out.println(top10mostEngagingUsers);
  }
}
