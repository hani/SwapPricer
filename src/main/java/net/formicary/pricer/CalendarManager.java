package net.formicary.pricer;

import java.util.List;

import net.formicary.pricer.model.BusinessDayConvention;
import net.formicary.pricer.model.DayCountFraction;
import org.joda.time.LocalDate;

/**
 * @author hani
 *         Date: 8/10/11
 *         Time: 9:54 AM
 */
public interface CalendarManager {
  LocalDate getAdjustedDate(LocalDate date, BusinessDayConvention convention, String... businessCentre);
  /**
   * @param conventions An array of 3 business day conventions. The first is the start date convention, followed
   * by the calculation period convention, and finally the termination date convention
   */
  List<LocalDate> getAdjustedDates(LocalDate start, LocalDate end, BusinessDayConvention[] conventions, String multiplier, String... businessCentre);
  double getDayCountFraction(LocalDate start, LocalDate end, DayCountFraction dayCountFraction);

  List<LocalDate> adjustDates(List<LocalDate> dates, BusinessDayConvention conventions[], String... businessCentre);

  List<LocalDate> getDatesInRange(LocalDate start, LocalDate end, String multiplier);

  List<LocalDate> getFixingDates(List<LocalDate> dates, int fixingOffset, String... businessCentre);
}
