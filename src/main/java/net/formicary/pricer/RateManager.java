package net.formicary.pricer;

import org.fpml.spec503wd3.Interval;
import org.joda.time.LocalDate;

/**
 * @author hani
 *         Date: 10/14/11
 *         Time: 5:28 PM
 */
public interface RateManager {
  double getZeroRate(String indexName, String currency, Interval interval, LocalDate date);
  double getDiscountFactor(String currency, Interval interval, LocalDate date, LocalDate valuationDate);
}
