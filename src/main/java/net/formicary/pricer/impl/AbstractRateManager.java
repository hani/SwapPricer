package net.formicary.pricer.impl;

import hirondelle.date4j.DateTime;
import net.formicary.pricer.RateManager;
import org.fpml.spec503wd3.Interval;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.commons.math.util.FastMath.exp;

/**
 * @author hani
 *         Date: 10/14/11
 *         Time: 5:30 PM
 */
public abstract class AbstractRateManager implements RateManager {
  private static final Object NOT_FOUND = new Object();
  private boolean caching = true;
  private Map<String, Object> cache = new ConcurrentHashMap<String, Object>();

  @Override
  public double getZeroRate(String indexName, String currency, Interval interval, DateTime date) {
    //avoid date.toString as it's relatively expensive
    String key = indexName + "-" + currency + "-" + date.getYear() + '-' + date.getDayOfYear() + '-' + date.getDay() + "-" + interval.getPeriodMultiplier() + interval.getPeriod();
    if(caching) {
      Object value = cache.get(key);
      if(value != null) {
        if(value == NOT_FOUND) {
          throw new IllegalArgumentException("No " + indexName + " rate found for " + currency + " " + interval.getPeriodMultiplier() + interval.getPeriod() + " on " + date);
        }
        return (Double)value;
      }
    }
    double rate = getRate(key, indexName, currency, interval, date);
    if(rate == 0) {
      if(caching)
        cache.put(key, NOT_FOUND);
      throw new IllegalArgumentException("No " + indexName + " rate found for " + currency + " " + interval.getPeriodMultiplier() + interval.getPeriod() + " on " + date);
    }
    if(caching)
      cache.put(key, rate);
    return rate;
  }

  protected abstract double getRate(String key, String indexName, String currency, Interval interval, DateTime date);

  public boolean isCaching() {
    return caching;
  }

  public void setCaching(boolean caching) {
    this.caching = caching;
  }

  @Override
  public double getDiscountFactor(String indexName, String currency, Interval interval, DateTime date, DateTime valuationDate) {
    double zero = getZeroRate(indexName, currency, interval, date) / 100;
    double days = date.numDaysFrom(valuationDate);
    return exp(zero * -(days) / 365d);
  }
}
