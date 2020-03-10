package com.nihar.log.analytics.bin;

import java.io.Serializable;

public class IpAndAvgTime implements Serializable, Comparable<IpAndAvgTime> {
  String ip;
  Long avgTimeSpent;

  public IpAndAvgTime(String ip, Long avgTimeSpent) {
    this.ip = ip;
    this.avgTimeSpent = avgTimeSpent;
  }

  public IpAndAvgTime() {
  }

  public String getIp() {
    return ip;
  }

  public Long getAvgTimeSpent() {
    return avgTimeSpent;
  }

  public void setIp(String ip) {
    this.ip = ip;
  }

  public void setAvgTimeSpent(Long avgTimeSpent) {
    this.avgTimeSpent = avgTimeSpent;
  }

  @Override
  public int compareTo(IpAndAvgTime o) {
    // Descending order by time spent to get top values
    return o.avgTimeSpent.compareTo(this.avgTimeSpent);
  }

  @Override
  public String toString() {
    return "IpAndAvgTime{" + "ip='" + ip + '\'' + ", avgTimeSpent='" + avgTimeSpent + '\'' + '}';
  }
}
