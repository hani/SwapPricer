package net.formicary.pricer.impl;

import java.lang.Math;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;

import net.formicary.pricer.CalendarManager;
import net.formicary.pricer.HolidayManager;
import net.formicary.pricer.model.DayCountFraction;
import net.formicary.pricer.util.FastDate;
import org.fpml.spec503wd3.*;

import static net.formicary.pricer.util.FastDate.DayOverflow.LastDay;
import static net.formicary.pricer.util.FastDate.DayOverflow.Spillover;

/**
 * @author hani
 *         Date: 8/10/11
 *         Time: 10:27 AM
 */
public class CalendarManagerImpl implements CalendarManager {

  @Inject
  private HolidayManager holidayManager;
  private static Map<String, String> currencyCenters = new HashMap<String, String>();
  static {
    currencyCenters.put("GBP", "GBLO");
    currencyCenters.put("USD", "USNY");
    currencyCenters.put("EURO", "EUTA");
  }

  @Override
  public double getDayCountFraction(FastDate start, FastDate end, DayCountFraction dayCountFraction) {
    switch(dayCountFraction) {
      case ONE:
        return 1;
      case THIRTY_360:
        //ISDA defs section 4.1.6f
        int d1 = start.getDay();
        int d2 = end.getDay();
        if(d1 == 31) d1 = 30;
        if(d2 == 31 && d1 > 29) d2 = 30;
        return ((360d * (end.getYear() - start.getYear())) + (30d * (end.getMonth() - start.getMonth())) + (d2 - d1)) / 360d;
      case ACT_360:
        return start.numDaysFrom(end) / 360d;
      case ACT_365:
        return start.numDaysFrom(end) / 365d;
      case THIRTYE_360:
        //ISDA defs section 4.1.6g
        d1 = start.getDay();
        d2 = end.getDay();
        if(d1 == 31) d1 = 30;
        if(d2 == 31) d2 = 30;
        return ((360d * (end.getYear() - start.getYear())) + (30d * (end.getMonth() - start.getMonth()))
          + (d2 - d1)) / 360d;
      case ACT:
        //note that we don't handle periods that span >2 years, should flag it as an error really so it's easier to spot
        int startYear = start.getYear();
        boolean isStartInLeapYear = startYear % 4 == 0;
        int endYear = end.getYear();
        boolean isEndInLeapYear = endYear % 4 == 0;
        if(!isStartInLeapYear && !isEndInLeapYear) {
          return start.numDaysFrom(end) / 365d;
        }
        //one of the start or end is in a leap year, so we work out the number of days separately
        FastDate yearSwitchOver = new FastDate(endYear, 1, 1);
        double startFraction = start.numDaysFrom(yearSwitchOver) / (isStartInLeapYear ? 366d : 365d);
        double endFraction = yearSwitchOver.numDaysFrom(end) / (isEndInLeapYear ? 366d : 365d);
        return startFraction + endFraction;
      case THIRTYE_360_ISDA:
      default:
        throw new UnsupportedOperationException("DayCountFraction " + dayCountFraction + " is not supported");
    }
  }

  @Override
  public FastDate getFixingDate(FastDate date, final RelativeDateOffset fixingOffset) {
    if(fixingOffset.getPeriod() != PeriodEnum.D) {
      throw new UnsupportedOperationException("Fixing dates only supports day periods");
    }
    int offset = fixingOffset.getPeriodMultiplier().intValue();
    final int numberOfStepsLeft = Math.abs(offset);
    final int step = (offset < 0 ? -1 : 1);

    for(int i = 0; i < numberOfStepsLeft; i++) {
      do {
        date = date.plusDays(step);
      }
      while(isNonWorkingDay(date, fixingOffset.getBusinessCenters()));
    }
    return date;
  }

  @Override
  public List<FastDate> getFixingDates(List<FastDate> dates, final RelativeDateOffset fixingOffset) {
    List<FastDate> fixingDates = new ArrayList<FastDate>(dates.size());
    if(fixingOffset.getPeriod() != PeriodEnum.D) {
      throw new UnsupportedOperationException("Fixing dates only supports day periods");
    }
    int offset = fixingOffset.getPeriodMultiplier().intValue();
    final int numberOfStepsLeft = Math.abs(offset);
    for(FastDate date : dates) {
      //move by fixing offset, we only count business days
//      if(offset == 0) {
//        //we need to roll anyway since we can't fix on a holiday
//        //fixing date is 0 in cases where payment day has an offset (unhandled right now). See LCH00000923966.xml
//        fixingDates.add(adjustDate(date, fixingOffset.getBusinessDayConvention(), fixingOffset.getBusinessCenters()));
//      } else
      {
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

  private boolean isNonWorkingDay(FastDate date, BusinessCenters businessCenters) {
    for(BusinessCenter s : businessCenters.getBusinessCenter()) {
        if(holidayManager.isNonWorkingDay(s.getValue(), date)) {
          return true;
        }
    }
    return false;
  }

  @Override
  public List<FastDate> getAdjustedDates(FastDate start, FastDate end, BusinessDayConventionEnum conventions[], Interval interval, BusinessCenters[] businessCenters, String rollConvention) {
    if(end == null) {
      throw new NullPointerException("end date is null");
    }
    List<FastDate> unadjustedDates = getDatesInRange(start, end, interval, rollConvention);
    if(unadjustedDates.get(unadjustedDates.size() - 1).lt(end)) {
      unadjustedDates.add(end);
    }
    //this is a hack that assumes that start dates are always valid, and so we can adjust them with impunity
    //the reason we do that is because we might have a stub or some other thing that messes with the start date but is still our 'perceived' start date, so we
    //adjust it for the middle adjustments
    unadjustedDates.set(0, adjustDate(unadjustedDates.get(0), conventions[0], businessCenters[0]));
    for(int i = 1; i < unadjustedDates.size() - 1; i++) {
      unadjustedDates.set(i, adjustDate(unadjustedDates.get(i), conventions[1], businessCenters[1]));
    }
    unadjustedDates.set(unadjustedDates.size() - 1, adjustDate(unadjustedDates.get(unadjustedDates.size() - 1), conventions[2], businessCenters[2]));
    return unadjustedDates;
  }

  @Override
  public List<FastDate> getDatesInRange(FastDate start, FastDate end, Interval interval, String rollConvention) {
    List<FastDate> unadjustedDates = new ArrayList<FastDate>(40);
    if(interval.getPeriod() == PeriodEnum.T) {
      unadjustedDates.add(start);
      unadjustedDates.add(end);
      return unadjustedDates;
    }
    FastDate current = new FastDate(start.getYear(),
        start.getMonth(), start.getDay());
    boolean isIMM = false;
    boolean isEOM = false;
    int rollDay = 0;
    if(interval instanceof CalculationPeriodFrequency) {
      CalculationPeriodFrequency f = (CalculationPeriodFrequency)interval;
      rollConvention = f.getRollConvention();
    }
    if(rollConvention == null && interval.getPeriod() != PeriodEnum.D) {
      throw new IllegalArgumentException("No rollconvention specified");
    }
    if(rollConvention != null) {
      if(rollConvention.startsWith("IMM")) {
        isIMM = true;
      } else if(rollConvention.equals("EOM")) {
        isEOM = true;
      }
      if(!isIMM && !isEOM) {
        rollDay = Integer.parseInt(rollConvention);
      }
    }

    while(current.lteq(end)) {
      unadjustedDates.add(current);
      current = plus(current, interval.getPeriod(), interval.getPeriodMultiplier());
      if(rollDay > 0 && current.getDay() != rollDay) {
        try {
          current = new FastDate(current.getYear(), current.getMonth(), rollDay);
        } catch (Exception e) {
          //can't do stuff like feb 29 on a non-leap year etc, it's ok.
        }
      }
      if(isEOM) {
        current = current.getEndOfMonth();
      } else if(isIMM) {
        //go to the first day
        current = current.getStartOfMonth();
        //go to the first wednesday
        //if we're after a weds on the first, then our first one is the next week's weds
        if(current.getWeekDay() > 4) {
          current = current.plusDays(7);
        }
        int distanceToWeds = current.getWeekDay() - 4;
        current = current.plusDays(-distanceToWeds);
        //we have the first weds, we want the third, so move forward twice
        current = current.plusDays(14);
        if("IMMCAD".equals(rollConvention)) {
          current = current.plusDays(-2);
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
  public FastDate applyDayInterval(FastDate date, Interval interval, BusinessCenters businessCenters) {
    //move by fixing offset, we only count business days
    if(interval instanceof Offset) {
      if(((Offset)interval).getDayType() != DayTypeEnum.BUSINESS) {
        throw new UnsupportedOperationException("Only BUSINESS dayType is currently supported. not " + ((Offset)interval).getDayType());
      }
    }
    if(interval.getPeriod() != PeriodEnum.D) {
      throw new IllegalArgumentException("Use applyInterval to caluclate non-day period dates");
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
  public List<FastDate> getValidDays(FastDate startDate, FastDate endDate, BusinessCenters calculationCenter) {
    int size = startDate.numDaysFrom(endDate);
    List<FastDate> dates = new ArrayList<FastDate>(size);
    FastDate current = startDate;
    while(current.lteq(endDate)) {
      if(!isNonWorkingDay(current, calculationCenter)) {
        dates.add(current);
      }
      current = current.plusDays(1);
    }
    return dates;
  }

  @Override
  public FastDate applyInterval(FastDate date, Interval interval, BusinessDayConventionEnum convention, BusinessCenters centers) {
    PeriodEnum period = interval.getPeriod();
    date = plus(date, period, interval.getPeriodMultiplier());
    return adjustDate(date, convention, centers);
  }

  @Override
  public FastDate applyIndexInterval(FastDate date, Interval interval, String index, String ccy) {
    PeriodEnum period = interval.getPeriod();
    date = plus(date, period, interval.getPeriodMultiplier());
    String[] centers = new String[2];
    if("LIBOR".equals(index) && !"EUR".equals(ccy)) {
      centers[0] = "GBLO";
    } else if ("EURIBOR".equals(index)) {
      centers[0] = "EUTA";
    }
    if("EUR".equals(ccy)) {
      centers[1] = "EUTA";
    }
    if("USD".equals(ccy)) {
      centers[1] = "USNY";
    }
    return adjustDate(date, BusinessDayConventionEnum.MODFOLLOWING, centers);
  }

  public FastDate adjustDate(FastDate date, BusinessDayConventionEnum convention, String... businessCenters) {
    if(convention == BusinessDayConventionEnum.NONE) {
      return date;
    }
    return holidayManager.adjustDate(date, convention, businessCenters);
  }

  private FastDate plus(FastDate date, PeriodEnum period, BigInteger periodMultiplier) {
    switch (period) {
      case D:
        date = date.plus(0, 0, periodMultiplier.intValue(), Spillover);
        break;
      case W:
        date = date.plus(0, 0, 7 * periodMultiplier.intValue(), Spillover);
        break;
      case M:
        date = date.plus(0, periodMultiplier.intValue(), 0, LastDay);
        break;
      case Y:
        date = date.plus(periodMultiplier.intValue(), 0, 0, LastDay);
        break;
    }
    return date;
  }

  @Override
  public FastDate adjustDate(FastDate date, BusinessDayConventionEnum convention, BusinessCenters businessCenters) {
    if(convention == BusinessDayConventionEnum.NONE) {
      return date;
    }
    return holidayManager.adjustDate(date, convention, businessCenters);
  }
}
