package com.nihar.web.analytics.bin;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class IpAndAvgTime implements Serializable, Comparable<IpAndAvgTime> {
  String ip;
  Long avgTimeSpent;

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
