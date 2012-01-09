package net.formicary.pricer;

import hirondelle.date4j.DateTime;
import org.fpml.spec503wd3.Interval;

/**
 * @author hani
 *         Date: 10/14/11
 *         Time: 5:28 PM
 */
public interface RateManager {
  double getZeroRate(String indexName, String currency, Interval interval, DateTime date);
  double getDiscountFactor(String indexName, String currency, Interval interval, DateTime date, DateTime valuationDate);
}
