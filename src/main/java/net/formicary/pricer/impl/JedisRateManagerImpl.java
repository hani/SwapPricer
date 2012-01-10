package net.formicary.pricer.impl;

import javax.inject.Singleton;

import net.formicary.pricer.util.FastNumbers;
import org.fpml.spec503wd3.Interval;
import org.joda.time.LocalDate;
import redis.clients.jedis.Jedis;

/**
 * @author hsuleiman
 *         Date: 1/9/12
 *         Time: 9:57 AM
 */
@Singleton
public class JedisRateManagerImpl extends AbstractRateManager {
  private static final ThreadLocal<Jedis> threadLocal = new ThreadLocal<Jedis>() {
    @Override
    protected Jedis initialValue() {
      return new Jedis("localhost");
    }
  };

  protected double getRate(String key, String indexName, String currency, Interval interval, LocalDate date) {
    String value = threadLocal.get().get(key);
    if(value == null) {
      return 0;
    }
    return FastNumbers.parseDouble(value);
  }
}
