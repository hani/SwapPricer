package net.formicary.pricer;

import net.formicary.pricer.model.DayCountFraction;
import org.fpml.spec503wd3.BusinessCenters;
import org.fpml.spec503wd3.BusinessDayConventionEnum;
import org.fpml.spec503wd3.Interval;
import org.fpml.spec503wd3.RelativeDateOffset;
import org.joda.time.LocalDate;

import java.util.List;

/**
 * @author hani
 *         Date: 8/10/11
 *         Time: 9:54 AM
 */
public interface CalendarManager {
  LocalDate adjustDate(LocalDate date, BusinessDayConventionEnum convention, BusinessCenters businessCenters);

  LocalDate applyInterval(LocalDate date, Interval interval, BusinessCenters businessCenters);
  /**
   * @param conventions An array of 3 business day conventions. The first is the start date convention, followed
   *                    by the calculation period convention, and finally the termination date convention.
   * @param businessCenters An array of 3 business centers. First is start date centers, following by calculation
   *                        period centers, then the termination date conventions.
   */
  List<LocalDate> getAdjustedDates(LocalDate start, LocalDate end, BusinessDayConventionEnum[] conventions, Interval interval, BusinessCenters[] businessCenters);
  double getDayCountFraction(LocalDate start, LocalDate end, DayCountFraction dayCountFraction);

  List<LocalDate> adjustDates(List<LocalDate> dates, BusinessDayConventionEnum conventions[], BusinessCenters[] businessCenters);

  List<LocalDate> getDatesInRange(LocalDate start, LocalDate end, Interval interval);

  List<LocalDate> getFixingDates(List<LocalDate> dates, RelativeDateOffset fixingOffset);
}
