package net.formicary.pricer.impl;

import net.formicary.pricer.RateManager;
import org.fpml.spec503wd3.Interval;
import org.joda.time.Days;
import org.joda.time.LocalDate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
  public double getZeroRate(String indexName, String currency, Interval interval, LocalDate date) {
    //avoid date.toString as it's relatively expensive
    String key = indexName + "-" + currency + "-" + date.getYear() + '-' + date.getDayOfYear() + '-' + date.getDayOfMonth() + "-" + interval.getPeriodMultiplier() + interval.getPeriod();
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

  protected abstract double getRate(String key, String indexName, String currency, Interval interval, LocalDate date);

  public boolean isCaching() {
    return caching;
  }

  public void setCaching(boolean caching) {
    this.caching = caching;
  }

  @Override
  public double getDiscountFactor(String indexName, String currency, Interval interval, LocalDate date, LocalDate valuationDate) {
    double zero = getZeroRate(indexName, currency, interval, date) / 100;
    double days = Days.daysBetween(date, valuationDate).getDays();
    return Math.exp(zero * -(days) / 365d);
  }
}
