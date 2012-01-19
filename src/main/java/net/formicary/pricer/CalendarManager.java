package net.formicary.pricer;

import java.util.List;

import net.formicary.pricer.model.DayCountFraction;
import net.formicary.pricer.util.FastDate;
import org.fpml.spec503wd3.*;

/**
 * @author hani
 *         Date: 8/10/11
 *         Time: 9:54 AM
 */
public interface CalendarManager {
  FastDate adjustDate(FastDate date, BusinessDayConventionEnum convention, BusinessCenters businessCenters);

  FastDate applyDayInterval(FastDate date, Interval interval, BusinessCenters businessCenters);

  double getDayCountFraction(FastDate start, FastDate end, DayCountFraction dayCountFraction);

  List<FastDate> getDatesInRange(FastDate start, FastDate end, Interval interval, String rollConvention);

  List<FastDate> getFixingDates(List<FastDate> dates, RelativeDateOffset fixingOffset);

  FastDate applyInterval(FastDate date, Interval interval, BusinessDayConventionEnum convention, BusinessCenters centers);

  /**
   * Adjust dates from a start to end date. The returned list will include an adjusted start date, and if the end
   * date falls on an interval date, it will be included too.
   *
   * @param conventions An array of 3 business day conventions. The first is the start date convention, followed
   * by the calculation period convention, and finally the termination date convention.
   * @param interval can be either a regular interval or a {@link org.fpml.spec503wd3.CalculationPeriodFrequency}.
   * @param businessCenters An array of 3 business centers. First is start date centers, following by calculation
   * period centers, then the termination date conventions.
   * @param rollConvention convention from the calculation period interval. If interval is a {@link org.fpml.spec503wd3.CalculationPeriodFrequency} then this value can be null.
   */
  List<FastDate> getAdjustedDates(FastDate startDate, FastDate endDate, BusinessDayConventionEnum[] conventions,
    Interval interval, BusinessCenters[] businessCenters, String rollConvention);

  List<FastDate> getValidDays(FastDate startDate, FastDate endDate, BusinessCenters calculationCenter);

  FastDate getFixingDate(FastDate date, RelativeDateOffset fixingOffset);
}
