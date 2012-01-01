package net.formicary.pricer.util;

import java.util.HashMap;
import java.util.Map;

import net.formicary.pricer.model.DayCountFraction;
import org.fpml.spec503wd3.*;
import org.joda.time.LocalDate;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.XMLGregorianCalendar;

import static net.formicary.pricer.model.DayCountFraction.*;

/**
 * @author hsuleiman
 *         Date: 12/27/11
 *         Time: 10:11 AM
 */
public class FpMLUtil {
  private static final Map<String, DayCountFraction> fractionMapping = new HashMap<String, DayCountFraction>();
  static {
    fractionMapping.put("1/1", ONE);
    fractionMapping.put("ACT/ACT.ISDA", ACT);
    fractionMapping.put("ACT/360", ACT_360);
    fractionMapping.put("ACT/ACT", ACT);
    fractionMapping.put("ACT", ACT);
    fractionMapping.put("ACT/365", ACT_365);
    fractionMapping.put("30/360", THIRTY_360);
    fractionMapping.put("30E/360", THIRTYE_360);
    fractionMapping.put("30E/360.ISDA", THIRTYE_360_ISDA);
  }

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
    DayCountFraction f = fractionMapping.get(value);
    if(value == null) {
      throw new IllegalArgumentException("No mapping found for day count fraction " + value);
    }
    return f;
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

  private static StubValue getStub(InterestRateStream leg, String type) {
    StubCalculationPeriodAmount stubCalculationPeriodAmount = leg.getStubCalculationPeriodAmount();
    if(stubCalculationPeriodAmount == null) return null;
    for (JAXBElement<?> element : stubCalculationPeriodAmount.getContent()) {
      if(element.getName().getLocalPart().equals(type)) {
        return (StubValue)element.getValue();
      }
    }
    return null;
  }

  public static StubValue getInitialStub(InterestRateStream leg) {
    return getStub(leg, "initialStub");
  }

  public static StubValue getFinalStub(InterestRateStream leg) {
    return getStub(leg, "finalStub");
  }

  public static LocalDate getEndDate(InterestRateStream leg) {
    //if we have a stub, then our end is from this date
    XMLGregorianCalendar cal = leg.getCalculationPeriodDates().getLastRegularPeriodEndDate();
    //no stub, just use the termination date
    if(cal == null) cal = leg.getCalculationPeriodDates().getTerminationDate().getUnadjustedDate().getValue();
    return DateUtil.getDate(cal);
  }

  public static LocalDate getStartDate(LocalDate valuationDate, InterestRateStream leg) {
    XMLGregorianCalendar cal = leg.getCalculationPeriodDates().getFirstRegularPeriodStartDate();
    if(cal != null) {
      return DateUtil.getDate(cal);
    }

    LocalDate effectiveDate = DateUtil.getDate(leg.getCalculationPeriodDates().getEffectiveDate().getUnadjustedDate().getValue());
    return effectiveDate;
  }
}
