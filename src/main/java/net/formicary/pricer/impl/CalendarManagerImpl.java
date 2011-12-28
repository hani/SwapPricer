package net.formicary.pricer.impl;

import net.formicary.pricer.CalendarManager;
import net.formicary.pricer.model.DayCountFraction;
import net.objectlab.kit.datecalc.common.HolidayHandlerType;
import net.objectlab.kit.datecalc.joda.LocalDateCalculator;
import net.objectlab.kit.datecalc.joda.LocalDateKitCalculatorsFactory;
import org.fpml.spec503wd3.*;
import org.fpml.spec503wd3.Interval;
import org.joda.time.*;

import javax.inject.Inject;
import java.lang.Math;
import java.util.ArrayList;
import java.util.List;

/**
 * @author hani
 *         Date: 8/10/11
 *         Time: 10:27 AM
 */
public class CalendarManagerImpl implements CalendarManager {

  @Inject
  private LocalDateKitCalculatorsFactory factory;

  @Override
  public double getDayCountFraction(LocalDate start, LocalDate end, DayCountFraction dayCountFraction) {
    switch(dayCountFraction) {
      case THIRTY_360:
        return ((360d * (end.getYear() - start.getYear())) + (30d * (end.getMonthOfYear() - start.getMonthOfYear())) + (end.getDayOfMonth() - start.getDayOfMonth())) / 360d;
      case ACT_360:
        return Days.daysBetween(start, end).getDays() / 360d;
      case ACT_365:
        return Days.daysBetween(start, end).getDays() / 365d;
      default:
        throw new UnsupportedOperationException("DayCountFraction " + dayCountFraction + " is not supported");
    }
  }

  @Override
  public List<LocalDate> getFixingDates(List<LocalDate> dates, final RelativeDateOffset fixingOffset) {
    List<LocalDate> fixingDates = new ArrayList<LocalDate>();
    if(fixingOffset.getPeriod() != PeriodEnum.D) {
      throw new UnsupportedOperationException("Fixing dates only supports day periods");
    }
    for(LocalDate date : dates) {
      //move by fixing offset, we only count business days
      int offset = fixingOffset.getPeriodMultiplier().intValue();
      final int numberOfStepsLeft = Math.abs(offset);
      final int step = (offset < 0 ? -1 : 1);

      for (int i = 0; i < numberOfStepsLeft; i++) {
        do {
          date = date.plusDays(step);
        }
        while(isNonWorkingDay(date, fixingOffset.getBusinessCenters()));
      }
      fixingDates.add(date);
    }
    return fixingDates;
  }

  private boolean isNonWorkingDay(LocalDate date, BusinessCenters businessCenters) {
    for(BusinessCenter s : businessCenters.getBusinessCenter()) {
      LocalDateCalculator calc = factory.getDateCalculator(s.getId(), null);
        if(calc.isNonWorkingDay(date)) {
          return true;
        }
    }
    return false;
  }

  @Override
  public List<LocalDate> getAdjustedDates(LocalDate start, LocalDate end, BusinessDayConventionEnum conventions[], Interval interval, BusinessCenters[] businessCentres) {
    if(end == null) {
      throw new NullPointerException("end date is null");
    }
    List<LocalDate> unadjustedDates = getDatesInRange(start, end, interval);
    unadjustedDates.set(0, getAdjustedDate(unadjustedDates.get(0), conventions[0], businessCentres[0]));
    for(int i = 0; i < unadjustedDates.size() - 1; i++) {
      unadjustedDates.set(i, getAdjustedDate(unadjustedDates.get(i), conventions[1], businessCentres[1]));
    }
    unadjustedDates.set(unadjustedDates.size() - 1, getAdjustedDate(unadjustedDates.get(unadjustedDates.size() - 1), conventions[2], businessCentres[2]));
    return unadjustedDates;
  }

  @Override
  public List<LocalDate> adjustDates(List<LocalDate> dates, BusinessDayConventionEnum conventions[], BusinessCenters[] businessCentres) {
    List<LocalDate> adjusted = new ArrayList<LocalDate>(dates);
    dates.set(0, getAdjustedDate(dates.get(0), conventions[0], businessCentres[0]));
    for(int i = 0; i < dates.size() - 1; i++) {
      adjusted.set(i, getAdjustedDate(dates.get(i), conventions[1], businessCentres[1]));
    }
    adjusted.set(dates.size() - 1, getAdjustedDate(dates.get(dates.size() - 1), conventions[2], businessCentres[2]));
    return adjusted;
  }

  @Override
  public List<LocalDate> getDatesInRange(LocalDate start, LocalDate end, Interval interval) {
    List<LocalDate> unadjustedDates = new ArrayList<LocalDate>();
    if(interval.getPeriod() == PeriodEnum.T) {
      unadjustedDates.add(start);
      unadjustedDates.add(end);
      return unadjustedDates;
    }
    LocalDate current = new LocalDate(start);
    boolean isIMM = false;
    boolean isEOM = false;
    if(interval instanceof CalculationPeriodFrequency) {
      CalculationPeriodFrequency f = (CalculationPeriodFrequency)interval;
      if(f.getRollConvention().equals("IMM")) {
        isIMM = true;
      } else if(f.getRollConvention().equals("EOM")) {
        isEOM = true;
      }
    }
    ReadablePeriod period = getPeriod(interval);
    while(current.isBefore(end) || current.equals(end)) {
      unadjustedDates.add(current);
      current = current.plus(period);
      if(isEOM) {
        current = current.property(DateTimeFieldType.dayOfMonth()).withMaximumValue();
      } else if(isIMM) {
        //go to the first day
        current = current.property(DateTimeFieldType.dayOfMonth()).withMinimumValue();
        //go to the first wednesday
        //if we're after a weds on the first, then our first one is the next week's weds
        if(current.getDayOfWeek() > DateTimeConstants.WEDNESDAY) {
          current = current.plusWeeks(1);
        }
        current = current.withDayOfWeek(DateTimeConstants.WEDNESDAY);
        //we have the first weds, we want the third, so move forward twice
        current = current.plusWeeks(2);
      }
    }
    return unadjustedDates;
  }

  @Override
  public LocalDate getAdjustedDate(LocalDate date, BusinessDayConventionEnum convention, BusinessCenters businessCenters) {
    if(convention == BusinessDayConventionEnum.NONE) {
      return date;
    }
    LocalDate adjusted = date;
    for(BusinessCenter s : businessCenters.getBusinessCenter()) {
      LocalDateCalculator calc = factory.getDateCalculator(s.getId(), getHolidayHandlerType(convention));
      calc.setStartDate(adjusted);
      adjusted = calc.getCurrentBusinessDate();
    }
    return adjusted;
  }

  private String getHolidayHandlerType(BusinessDayConventionEnum convention) {
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

  private ReadablePeriod getPeriod(Interval interval) {
    int multiplier = interval.getPeriodMultiplier().intValue();
    //T is handled in getDatesInRange
    switch (interval.getPeriod()) {
      case D:
        return Days.days(multiplier);
      case W:
        return Weeks.weeks(multiplier);
      case M:
        return Months.months(multiplier);
      case Y:
        return Years.years(multiplier);
    }
    return null;
  }
}
