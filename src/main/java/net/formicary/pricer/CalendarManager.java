package net.formicary.pricer;

import java.util.List;

import net.formicary.pricer.model.BusinessDayConvention;
import net.formicary.pricer.model.DayCount;
import org.joda.time.LocalDate;

/**
 * @author hani
 *         Date: 8/10/11
 *         Time: 9:54 AM
 */
public interface CalendarManager {
  LocalDate getAdjustedDate(String businessCentre, LocalDate date, BusinessDayConvention convention);
  List<LocalDate> getDates(String businessCentre, LocalDate start, LocalDate end, BusinessDayConvention convention, String multiplier);
  double getDayCountFraction(LocalDate start, LocalDate end, DayCount dayCount);
}
