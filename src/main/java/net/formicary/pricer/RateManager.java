package net.formicary.pricer;

import net.formicary.pricer.util.FastDate;
import org.fpml.spec503wd3.Interval;

/**
 * @author hani
 *         Date: 10/14/11
 *         Time: 5:28 PM
 */
public interface RateManager {
  double getZeroRate(String indexName, String currency, Interval interval, FastDate date);
  double getDiscountFactor(String indexName, String currency, Interval interval, FastDate date, FastDate valuationDate);
}
