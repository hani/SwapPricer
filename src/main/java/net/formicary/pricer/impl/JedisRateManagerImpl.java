package net.formicary.pricer.impl;

import net.formicary.pricer.util.FastNumbers;
import org.fpml.spec503wd3.Interval;
import org.joda.time.LocalDate;
import redis.clients.jedis.Jedis;

import javax.inject.Inject;

/**
 * @author hsuleiman
 *         Date: 1/9/12
 *         Time: 9:57 AM
 */
public class JedisRateManagerImpl extends AbstractRateManager {
  @Inject private Jedis ds;

  protected double getRate(String key, String indexName, String currency, Interval interval, LocalDate date) {
    String value = ds.get(key);
    if(value == null) {
      return 0;
    }
    return FastNumbers.parseDouble(value);
  }
}
