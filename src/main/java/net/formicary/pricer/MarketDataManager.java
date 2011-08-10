package net.formicary.pricer;

import org.joda.time.LocalDate;

/**
 * @author hani
 *         Date: 8/10/11
 *         Time: 9:54 AM
 */
public interface MarketDataManager {
  boolean isHoliday(String businessCenter, LocalDate date);
}
