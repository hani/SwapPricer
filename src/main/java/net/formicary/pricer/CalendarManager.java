package net.formicary.pricer;

import java.util.List;

import net.formicary.pricer.model.BusinessDayConvention;
import net.formicary.pricer.model.DayCount;
import org.joda.time.LocalDate;

/**
 * @author hani
 *         Date: 8/10/11
 *         Time: 9:54 AM
 */
public interface CalendarManager {
  LocalDate getAdjustedDate(String businessCentre, LocalDate date, BusinessDayConvention convention);
  /**
   * @param conventions An array of 3 business day conventions. The first is the start date convention, followed
   * by the calculation period convention, and finally the termination date convention
   */
  List<LocalDate> getAdjustedDates(String businessCentre, LocalDate start, LocalDate end, BusinessDayConvention[]
    conventions, String multiplier);
  double getDayCountFraction(LocalDate start, LocalDate end, DayCount dayCount);
}
