package net.formicary.pricer;

import java.util.List;

import net.formicary.pricer.model.DayCountFraction;
import net.formicary.pricer.util.FastDate;
import org.fpml.spec503wd3.BusinessCenters;
import org.fpml.spec503wd3.BusinessDayConventionEnum;
import org.fpml.spec503wd3.Interval;
import org.fpml.spec503wd3.RelativeDateOffset;

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

  FastDate applyInterval(FastDate date, Interval interval, BusinessDayConventionEnum convention,
    BusinessCenters centers);

  /**
   * Used to calculate payment dates where the interval doesn't specify a roll date, so we borrow the one from the calculation period
   * @param conventions An array of 3 business day conventions. The first is the start date convention, followed
   *                    by the calculation period convention, and finally the termination date convention.
   * @param interval can be either a regular interval or a {@link org.fpml.spec503wd3.CalculationPeriodFrequency}.
   * @param businessCenters An array of 3 business centers. First is start date centers, following by calculation
   *                        period centers, then the termination date conventions.
   * @param rollConvention convention from the calculation period interval. If interval is a {@link org.fpml.spec503wd3.CalculationPeriodFrequency} then this value can be null.
   */
  List<FastDate> getAdjustedDates(FastDate paymentStartDate, FastDate endDate, BusinessDayConventionEnum[] conventions,
    Interval interval, BusinessCenters[] businessCenters, String rollConvention);

  List<FastDate> getValidDays(FastDate startDate, FastDate endDate, BusinessCenters calculationCenter);

  /**
   * Apply an interval to an index date. This does not take the trade into account as the spot
   * lag for an index has nothing to do with the trade, and is instead composed of the union of the
   * index business center (eg, GBLO for LIBOR) and the currency (eg, USNY for USD). It is also
   * always MODFOLLOWING
   * @param date The start date
   * @param interval The interval to apply
   * @param index The index name (EURIBOR, LIBOR, etc)
   * @param ccy The currency (USD, GBP, EURO, etc)
   * @return An adjusted date for the end of the rate's period
   */
  FastDate applyIndexInterval(FastDate date, Interval interval, String index, String ccy);
}
