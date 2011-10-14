package net.formicary.pricer.impl;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;

import net.formicary.pricer.model.BusinessDayConvention;
import net.formicary.pricer.CalendarManager;
import net.formicary.pricer.model.DayCount;
import net.objectlab.kit.datecalc.common.HolidayHandlerType;
import net.objectlab.kit.datecalc.joda.LocalDateCalculator;
import net.objectlab.kit.datecalc.joda.LocalDateKitCalculatorsFactory;
import org.joda.time.*;

/**
 * @author hani
 *         Date: 8/10/11
 *         Time: 10:27 AM
 */
public class CalendarManagerImpl implements CalendarManager {

  @Inject
  private LocalDateKitCalculatorsFactory factory;

  @Override
  public double getDayCountFraction(LocalDate start, LocalDate end, DayCount dayCount) {
    switch(dayCount) {
      case THIRTY_360:
        return ((360d * (end.getYear() - start.getYear())) + (30d * (end.getMonthOfYear() - start.getMonthOfYear())) + (end.getDayOfMonth() - start.getDayOfMonth())) / 360d;
      case ACT_360:
        return Days.daysBetween(start, end).getDays() / 360d;
      case ACT_365:
        return Days.daysBetween(start, end).getDays() / 365d;
      default:
        throw new UnsupportedOperationException("DayCount " + dayCount + " is not supported");
    }
  }

  @Override
  public List<LocalDate> getFixingDates(String businessCentre, List<LocalDate> dates, final int fixingOffset) {
    List<LocalDate> fixingDates = new ArrayList<LocalDate>();
    LocalDateCalculator calc = factory.getDateCalculator(businessCentre, null);
    for(LocalDate date : dates) {
      //move by fixing offset, we only count business days
      final int numberOfStepsLeft = Math.abs(fixingOffset);
      final int step = (fixingOffset < 0 ? -1 : 1);

      for (int i = 0; i < numberOfStepsLeft; i++) {
        do {
          date = date.plusDays(step);
        }
        while(calc.isNonWorkingDay(date));
      }
      fixingDates.add(date);
    }
    return fixingDates;
  }

  @Override
  public List<LocalDate> getAdjustedDates(String businessCentre, LocalDate start, LocalDate end, BusinessDayConvention conventions[], String multiplier) {
    if(end == null) {
      throw new NullPointerException("end date is null");
    }
    List<LocalDate> unadjustedDates = getDatesInRange(start, end, multiplier);
    unadjustedDates.set(0, getAdjustedDate(businessCentre, unadjustedDates.get(0), conventions[0]));
    for(int i = 0; i < unadjustedDates.size() - 1; i++) {
      unadjustedDates.set(i, getAdjustedDate(businessCentre, unadjustedDates.get(i), conventions[1]));
    }
    unadjustedDates.set(unadjustedDates.size() - 1, getAdjustedDate(businessCentre, unadjustedDates.get(unadjustedDates.size() - 1), conventions[2]));
    return unadjustedDates;
  }

  @Override
  public List<LocalDate> adjustDates(String businessCentre, List<LocalDate> dates, BusinessDayConvention conventions[]) {
    List<LocalDate> adjusted = new ArrayList<LocalDate>(dates);
    dates.set(0, getAdjustedDate(businessCentre, dates.get(0), conventions[0]));
    for(int i = 0; i < dates.size() - 1; i++) {
      adjusted.set(i, getAdjustedDate(businessCentre, dates.get(i), conventions[1]));
    }
    adjusted.set(dates.size() - 1, getAdjustedDate(businessCentre, dates.get(dates.size() - 1), conventions[2]));
    return adjusted;
  }

  @Override
  public List<LocalDate> getDatesInRange(LocalDate start, LocalDate end, String multiplier) {
    List<LocalDate> unadjustedDates = new ArrayList<LocalDate>();
    LocalDate current = new LocalDate(start);
    ReadablePeriod period = getPeriod(multiplier);
    while(current.isBefore(end) || current.equals(end)) {
      unadjustedDates.add(current);
      current = current.plus(period);
    }
    return unadjustedDates;
  }

  @Override
  public LocalDate getAdjustedDate(String businessCentre, LocalDate date, BusinessDayConvention convention) {
    if(convention == BusinessDayConvention.NONE) {
      return date;
    }
    LocalDateCalculator calc = factory.getDateCalculator(businessCentre, getHolidayHandlerType(convention));
    calc.setStartDate(date);
    return calc.getCurrentBusinessDate();
  }

  private String getHolidayHandlerType(BusinessDayConvention convention) {
    switch(convention) {
      case FOLLOWING:
        return HolidayHandlerType.FORWARD;
      case MODFOLLOWING:
        return HolidayHandlerType.MODIFIED_FOLLOWING;
      case PRECEDING:
        return HolidayHandlerType.BACKWARD;
      default:
        return null;
    }
  }

  private ReadablePeriod getPeriod(String multiplier) {
    if(multiplier == null) {
      throw new NullPointerException("null multiplier");
    }
    int m = multiplier.indexOf('M');
    if(m > -1) {
      int count = Integer.parseInt(multiplier.substring(0, m));
      return Months.months(count);
    }
    int y = multiplier.indexOf('Y');
    if(y > -1) {
      int count = Integer.parseInt(multiplier.substring(0, y));
      return Years.years(count);
    }
    throw new UnsupportedOperationException("Unparsable multiplier " + multiplier);
  }
}
