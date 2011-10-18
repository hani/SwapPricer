package net.formicary.pricer;

import org.joda.time.LocalDate;

/**
 * @author hani
 *         Date: 10/14/11
 *         Time: 5:28 PM
 */
public interface RateManager {
  double getZeroRate(String currency, String tenorPeriod, LocalDate date);
  double getDiscountFactor(String currency, String tenorPeriod, LocalDate date, LocalDate valuationDate);
}
