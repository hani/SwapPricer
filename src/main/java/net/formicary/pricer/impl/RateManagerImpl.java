package net.formicary.pricer.impl;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.query.Query;
import net.formicary.pricer.RateManager;
import net.formicary.pricer.model.Index;
import org.fpml.spec503wd3.Interval;
import org.fpml.spec503wd3.PeriodEnum;
import org.joda.time.Days;
import org.joda.time.LocalDate;

import javax.inject.Inject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author hani
 *         Date: 10/14/11
 *         Time: 5:30 PM
 */
public class RateManagerImpl implements RateManager {
  @Inject private Datastore ds;
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
    Query<Index> query = ds.createQuery(Index.class);
    query.field("currency").equal(currency);
    //hack, LCH rates are given to us in 12M vs 1Y
    if(interval.getPeriod() == PeriodEnum.Y && interval.getPeriodMultiplier().intValue() == 1) {
      query.field("tenorPeriod").equal("M");
      query.field("tenorUnit").equal("12");
    } else {
      query.field("tenorPeriod").equal(interval.getPeriod().value());
      query.field("tenorUnit").equal(interval.getPeriodMultiplier().toString());
    }
    query.field("fixingDate").equal(date);
    query.field("name").equal(indexName);
    Index index = query.get();
    if(index == null) {
      if(caching)
        cache.put(key, NOT_FOUND);
      throw new IllegalArgumentException("No " + indexName + " rate found for " + currency + " " + interval.getPeriodMultiplier() + interval.getPeriod() + " on " + date);
    }
    if(caching)
      cache.put(key, index.getRate());
    return index.getRate();
  }

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
