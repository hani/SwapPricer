package net.formicary.pricer.impl;

import javax.inject.Inject;

import net.formicary.pricer.BusinessDayConvention;
import net.formicary.pricer.MarketDataManager;
import net.objectlab.kit.datecalc.common.HolidayHandlerType;
import net.objectlab.kit.datecalc.joda.LocalDateCalculator;
import net.objectlab.kit.datecalc.joda.LocalDateKitCalculatorsFactory;
import org.joda.time.LocalDate;

/**
 * @author hani
 *         Date: 8/10/11
 *         Time: 10:27 AM
 */
public class MarketDataManagerImpl implements MarketDataManager {

  @Inject
  private LocalDateKitCalculatorsFactory factory;

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
        return "";
    }
  }
}
