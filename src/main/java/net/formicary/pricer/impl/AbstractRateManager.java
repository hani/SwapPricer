package net.formicary.pricer.impl;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import net.formicary.pricer.RateManager;
import net.formicary.pricer.util.FastDate;

import static org.apache.commons.math.util.FastMath.exp;

/**
 * @author hani
 *         Date: 10/14/11
 *         Time: 5:30 PM
 */
public abstract class AbstractRateManager implements RateManager {
  private boolean caching = true;
  //this is naughty, we're creating a large enough cache to make sure we don't run into a 'resize' operation,
  //if we do run into it, then we're screwed because all sorts of concurrency problems happen
  private Object2DoubleMap<String> cache = new Object2DoubleOpenHashMap<String>(20000);

  @Override
  public double getZeroRate(String indexName, String currency, String tenor, FastDate date) {
    //avoid date.toString as it's relatively expensive
    String key = indexName + "-" + currency + "-" + date.hashCode() + "-" + tenor;
    if(caching) {
      double value = cache.getDouble(key);
      if(value != 0) {
        return value;
      }
    }
    double rate = getRate(key, indexName, currency, tenor, date);
    if(rate == 0) {
      throw new IllegalArgumentException("No " + indexName + " rate found for " + currency + " " + tenor + " on " + date);
    }
    if(caching)
      cache.put(key, rate);
    return rate;
  }

  protected abstract double getRate(String key, String indexName, String currency, String tenor, FastDate date);

  public boolean isCaching() {
    return caching;
  }

  public void setCaching(boolean caching) {
    this.caching = caching;
  }

  @Override
  public double getDiscountFactor(String indexName, String currency, String tenor, FastDate date, FastDate valuationDate) {
    double zero = getZeroRate(indexName, currency, tenor, date) / 100;
    double days = date.numDaysFrom(valuationDate);
    return exp(zero * -(days) / 365d);
  }
}
