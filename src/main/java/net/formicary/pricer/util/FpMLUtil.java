package net.formicary.pricer.util;

import net.formicary.pricer.model.DayCountFraction;
import org.fpml.spec503wd3.BusinessCenters;
import org.fpml.spec503wd3.BusinessDayConventionEnum;
import org.fpml.spec503wd3.InterestRateStream;
import org.fpml.spec503wd3.Swap;

/**
 * @author hsuleiman
 *         Date: 12/27/11
 *         Time: 10:11 AM
 */
public class FpMLUtil {
  public static InterestRateStream getFixedStream(Swap s) {
    for (InterestRateStream stream : s.getSwapStream()) {
      if(stream.getCalculationPeriodAmount().getCalculation().getFixedRateSchedule() != null) {
        return stream;
      }
    }
    return null;
  }

  public static boolean isFixedStream(InterestRateStream s) {
    return s.getCalculationPeriodAmount().getCalculation().getFixedRateSchedule() != null;
  }

  public static InterestRateStream getFloatingStream(Swap s) {
    for (InterestRateStream stream : s.getSwapStream()) {
      if(stream.getCalculationPeriodAmount().getCalculation().getFixedRateSchedule() == null) {
        return stream;
      }
    }
    return null;
  }

  public static DayCountFraction getDayCountFraction(String value) {
    String text = value.replace('/', '_');
    text = text.replace("30", "THIRTY");
    return DayCountFraction.valueOf(text);
  }

  public static BusinessDayConventionEnum[] getBusinessDayConventions(InterestRateStream leg) {
    BusinessDayConventionEnum[] conventions = new BusinessDayConventionEnum[3];
    conventions[0] = leg.getCalculationPeriodDates().getEffectiveDate().getDateAdjustments().getBusinessDayConvention();
    conventions[1] = leg.getCalculationPeriodDates().getCalculationPeriodDatesAdjustments().getBusinessDayConvention();
    conventions[2] = leg.getCalculationPeriodDates().getTerminationDate().getDateAdjustments().getBusinessDayConvention();
    return conventions;
  }

  public static BusinessCenters[] getBusinessCenters(InterestRateStream leg) {
    BusinessCenters[] centers = new BusinessCenters[3];
    centers[0] = leg.getCalculationPeriodDates().getEffectiveDate().getDateAdjustments().getBusinessCenters();
    centers[1] = leg.getCalculationPeriodDates().getCalculationPeriodDatesAdjustments().getBusinessCenters();
    centers[2] = leg.getCalculationPeriodDates().getTerminationDate().getDateAdjustments().getBusinessCenters();
    return centers;
  }
}
