package net.formicary.pricer;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.inject.Singleton;

import org.fpml.spec503wd3.BusinessCenter;
import org.fpml.spec503wd3.BusinessCenters;
import org.fpml.spec503wd3.BusinessDayConventionEnum;
import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;

/**
 * @author hani
 *         Date: 12/30/11
 *         Time: 11:42 PM
 */
@Singleton
public class HolidayManager {

  private Map<String, Set<LocalDate>> holidays = new HashMap<String, Set<LocalDate>>();

  public void registerHolidays(String key, Set<LocalDate> value) {
    holidays.put(key, value);
  }

  public boolean isNonWorkingDay(String value, LocalDate date) {
    int dayOfWeek = date.getDayOfWeek();
    //weekend check, we only support western weekends for now, so no middle east stuff to worry about yet
    if(dayOfWeek == DateTimeConstants.SATURDAY || dayOfWeek == DateTimeConstants.SUNDAY) return true;
    return holidays.get(value).contains(date);
  }

  public LocalDate adjustDate(LocalDate date, BusinessDayConventionEnum convention, BusinessCenters businessCenters) {
    if(convention == BusinessDayConventionEnum.NONE) return date;
    LocalDate current = date;
    if(businessCenters != null) {
      for(BusinessCenter center : businessCenters.getBusinessCenter()) {
        while(isNonWorkingDay(center.getValue(), current)) {
          current = adjustDate(current, convention, center.getValue());
        }
      }
    }
    return current;
  }

  private LocalDate adjustDate(LocalDate date, BusinessDayConventionEnum convention, String value) {
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
        return date.minusDays(1);
      case MODPRECEDING:
        stepToUse = 1;
        return move(date, value, stepToUse);
      case NEAREST:
        throw new UnsupportedOperationException("NEAREST convention not supported");
      default:
        return date;
    }
  }

  private LocalDate move(LocalDate date, String value, int stepToUse) {
    final int month = date.getMonthOfYear();
    while (isNonWorkingDay(value, date)) {
        date = date.plusDays(stepToUse);
        if (date.getMonthOfYear() != month) {
            // flick to backward
            stepToUse *= -1;
            date = date.plusDays(stepToUse);
        }
    }
    return date;
  }
}
