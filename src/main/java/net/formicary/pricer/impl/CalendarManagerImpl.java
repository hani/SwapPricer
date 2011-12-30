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
    int d2 = end.getDayOfMonth();
    int d1 = start.getDayOfMonth();
    switch(dayCountFraction) {
      case THIRTY_360:
        //ISDA defs section 4.1.6f
        if(d1 == 31) d1 = 30;
        if(d2 == 31 && d1 > 29) d2 = 30;
        return ((360d * (end.getYear() - start.getYear())) + (30d * (end.getMonthOfYear() - start.getMonthOfYear())) + (d2 - d1)) / 360d;
      case ACT_360:
        return Days.daysBetween(start, end).getDays() / 360d;
      case ACT_365:
        return Days.daysBetween(start, end).getDays() / 365d;
      case THIRTYE_360:
        //ISDA defs section 4.1.6g
        if(d1 == 31) d1 = 30;
        if(d2 == 31) d2 = 30;
        return ((360d * (end.getYear() - start.getYear())) + (30d * (end.getMonthOfYear() - start.getMonthOfYear())) + (d2 - d1)) / 360d;
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
//      if(offset == 0) {
//        //we need to roll anyway since we can't fix on a holiday
//        //fixing date is 0 in cases where payment day has an offset (unhandled right now). See LCH00000923966.xml
//        fixingDates.add(adjustDate(date, fixingOffset.getBusinessDayConvention(), fixingOffset.getBusinessCenters()));
//      } else
      {
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
    }
    return fixingDates;
  }

  private boolean isNonWorkingDay(LocalDate date, BusinessCenters businessCenters) {
    for(BusinessCenter s : businessCenters.getBusinessCenter()) {
      LocalDateCalculator calc = factory.getDateCalculator(s.getValue(), null);
        if(calc.isNonWorkingDay(date)) {
          return true;
        }
    }
    return false;
  }

  @Override
  public List<LocalDate> getAdjustedDates(LocalDate start, LocalDate end, BusinessDayConventionEnum conventions[], Interval interval, BusinessCenters[] businessCenters) {
    if(end == null) {
      throw new NullPointerException("end date is null");
    }
    List<LocalDate> unadjustedDates = getDatesInRange(start, end, interval);
    unadjustedDates.set(0, adjustDate(unadjustedDates.get(0), conventions[0], businessCenters[0]));
    for(int i = 1; i < unadjustedDates.size() - 1; i++) {
      unadjustedDates.set(i, adjustDate(unadjustedDates.get(i), conventions[1], businessCenters[1]));
    }
    unadjustedDates.set(unadjustedDates.size() - 1, adjustDate(unadjustedDates.get(unadjustedDates.size() - 1), conventions[2], businessCenters[2]));
    return unadjustedDates;
  }

  @Override
  public List<LocalDate> adjustDates(List<LocalDate> dates, BusinessDayConventionEnum conventions[], BusinessCenters[] businessCenters) {
    List<LocalDate> adjusted = new ArrayList<LocalDate>(dates);
    dates.set(0, adjustDate(dates.get(0), conventions[0], businessCenters[0]));
    for(int i = 0; i < dates.size() - 1; i++) {
      adjusted.set(i, adjustDate(dates.get(i), conventions[1], businessCenters[1]));
    }
    adjusted.set(dates.size() - 1, adjustDate(dates.get(dates.size() - 1), conventions[2], businessCenters[2]));
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
    String rollConvention = null;
    boolean isIMM = false;
    boolean isEOM = false;
    int rollDay = 0;
    if(interval instanceof CalculationPeriodFrequency) {
      CalculationPeriodFrequency f = (CalculationPeriodFrequency)interval;
      rollConvention = f.getRollConvention();
      if(rollConvention.startsWith("IMM")) {
        isIMM = true;
      } else if(rollConvention.equals("EOM")) {
        isEOM = true;
      }
      if(!isIMM && !isEOM) {
        rollDay = Integer.parseInt(rollConvention);
      }
    }
    ReadablePeriod period = getPeriod(interval);
    while(current.isBefore(end) || current.equals(end)) {
      unadjustedDates.add(current);
      current = current.plus(period);
      if(rollDay > 0 && current.getDayOfMonth() != rollDay) {
        try {
          current = current.withDayOfMonth(rollDay);
        } catch(IllegalFieldValueException ex) {
          //can't do stuff like feb 29 on a non-leap year etc, it's ok.
        }
      }
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
        if("IMMCAD".equals(rollConvention)) {
          current = current.minusDays(2);
        } else if("IMMNZD".equals(rollConvention)) {
          throw new UnsupportedOperationException("IMMNZD is not supported yet");
        } else if("IMMAUD".equals(rollConvention)) {
          throw new UnsupportedOperationException("IMMAUD is not supported yet");
        }
      }
    }
    return unadjustedDates;
  }

  @Override
  public LocalDate applyInterval(LocalDate date, Interval interval, BusinessCenters businessCenters) {
    //move by fixing offset, we only count business days
    if(interval instanceof Offset) {
      if(((Offset)interval).getDayType() != DayTypeEnum.BUSINESS) {
        throw new UnsupportedOperationException("Only BUSINESS dayType is currently supported");
      }
    }
    int offset = interval.getPeriodMultiplier().intValue();
    {
      final int numberOfStepsLeft = Math.abs(offset);
      final int step = (offset < 0 ? -1 : 1);

      for (int i = 0; i < numberOfStepsLeft; i++) {
        do {
          date = date.plusDays(step);
        }
        while(isNonWorkingDay(date, businessCenters));
      }
    }
    return date;
  }

  @Override
  public LocalDate adjustDate(LocalDate date, BusinessDayConventionEnum convention, BusinessCenters businessCenters) {
    if(convention == BusinessDayConventionEnum.NONE) {
      return date;
    }
    LocalDate adjusted = date;
    for(BusinessCenter s : businessCenters.getBusinessCenter()) {
      LocalDateCalculator calc = factory.getDateCalculator(s.getValue(), getHolidayHandlerType(convention));
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
