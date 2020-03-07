package com.nihar.log.analytics.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommonUtil {
  static final SimpleDateFormat sourceFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
  static final TimeZone utc = TimeZone.getTimeZone("UTC");

  static {
    sourceFormat.setTimeZone(utc);
  }

  public static List<String> getColumnsFrom(String str) {
    List<String> list = new ArrayList<>();
    Matcher m = Pattern.compile("([^\"]\\S*|\".+?\")\\s*").matcher(str);
    while (m.find()) list.add(m.group(1));
    return list;
  }

  public static String getUrl(String str) {
    String url = str;
    Matcher murl = Pattern.compile("\"(?i:GET|PUT)(.*)?(?i:HTTP\\/1.1\")").matcher(url);
    while (murl.find()) url = murl.group(1).trim();
    return url;
  }

  public static Long getTimeStampFromStr(String str) throws ParseException {
    return sourceFormat.parse(str).getTime();
  }

  public static String getStrTimeFromMillis(long timeStamp) {
    return sourceFormat.format(timeStamp);
  }
}
