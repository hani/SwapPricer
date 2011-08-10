package net.formicary.pricer;

import org.joda.time.LocalDate;

/**
 * @author hani
 *         Date: 8/10/11
 *         Time: 9:54 AM
 */
public interface MarketDataManager {
  LocalDate getAdjustedDate(String businessCentre, LocalDate date, BusinessDayConvention convention);
}
