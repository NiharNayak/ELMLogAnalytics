package com.nihar.log.analytics.bin;

import java.io.Serializable;

/**
 * @author nihar.nayak
 * <p>This composite class deal with handeling multiple attributes of log data aslo responsible
 * for secondary sorting on key value.
 */

public class CompositeKeyPojo implements Comparable<CompositeKeyPojo>, Serializable {
  private String ip;
  private Long timeStamp;
  private String url;

  public String getIp() {
    return ip;
  }

  public Long getTimeStamp() {
    return timeStamp;
  }

  public String getUrl() {
    return url;
  }

  @Override
  public int compareTo(CompositeKeyPojo o) {
    // we are only concern about sorting the time because ,
    // we want to deal with sorted time for windowing (sessionizing)
    return this.timeStamp.compareTo(o.timeStamp);
  }
}
