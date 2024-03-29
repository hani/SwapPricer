package net.formicary.pricer;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.inject.Singleton;

import net.formicary.pricer.util.FastDate;
import org.fpml.spec503wd3.BusinessCenter;
import org.fpml.spec503wd3.BusinessCenters;
import org.fpml.spec503wd3.BusinessDayConventionEnum;

/**
 * @author hani
 *         Date: 12/30/11
 *         Time: 11:42 PM
 */
@Singleton
public class HolidayManager {

  private Map<String, Set<FastDate>> holidays = new HashMap<String, Set<FastDate>>(50);

  public void registerHolidays(String key, Set<FastDate> value) {
    holidays.put(key, value);
  }

  public boolean isNonWorkingDay(String value, FastDate date) {
    int dayOfWeek = date.getWeekDay();
    //weekend check, we only support western weekends for now, so no middle east stuff to worry about yet
    if(dayOfWeek == 1 || dayOfWeek == 7) return true;
    return holidays.get(value).contains(date);
  }

  public FastDate adjustDate(FastDate date, BusinessDayConventionEnum convention, BusinessCenters businessCenters) {
    if(convention == BusinessDayConventionEnum.NONE) return date;
    FastDate current = date;
    if(businessCenters != null) {
      for(BusinessCenter center : businessCenters.getBusinessCenter()) {
        while(isNonWorkingDay(center.getValue(), current)) {
          current = adjustDate(current, convention, center.getValue());
        }
      }
    }
    return current;
  }

  private FastDate adjustDate(FastDate date, BusinessDayConventionEnum convention, String value) {
    switch(convention) {
      case NONE:
      case NOT_APPLICABLE:
        return date;
      case FOLLOWING:
        return date.plusDays(1);
      case FRN:
        throw new UnsupportedOperationException("FRN convention not supported");
      case MODFOLLOWING:
        int stepToUse = 1;
        return move(date, value, stepToUse);
      case PRECEDING:
        return date.plusDays(-1);
      case MODPRECEDING:
        stepToUse = 1;
        return move(date, value, stepToUse);
      case NEAREST:
        throw new UnsupportedOperationException("NEAREST convention not supported");
      default:
        return date;
    }
  }

  private FastDate move(FastDate date, String value, int stepToUse) {
    final int month = date.getMonth();
    while (isNonWorkingDay(value, date)) {
        date = date.plusDays(stepToUse);
        if (date.getMonth() != month) {
            // flick to backward
            stepToUse *= -1;
            date = date.plusDays(stepToUse);
        }
    }
    return date;
  }
}
