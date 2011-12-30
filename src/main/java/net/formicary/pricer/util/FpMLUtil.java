package net.formicary.pricer.util;

import net.formicary.pricer.model.DayCountFraction;
import org.fpml.spec503wd3.*;
import org.joda.time.LocalDate;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.XMLGregorianCalendar;

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

  private static Stub getStub(InterestRateStream leg, String type) {
    StubCalculationPeriodAmount stubCalculationPeriodAmount = leg.getStubCalculationPeriodAmount();
    if(stubCalculationPeriodAmount == null) return null;
    for (JAXBElement<?> element : stubCalculationPeriodAmount.getContent()) {
      if(element.getName().getLocalPart().equals(type)) {
        return (Stub)element.getValue();
      }
    }
    return null;
  }

  public static Stub getInitialStub(InterestRateStream leg) {
    return getStub(leg, "initialStub");
  }

  public static Stub getFinalStub(InterestRateStream leg) {
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
    //if we have a stub, then our start is from this date
    XMLGregorianCalendar cal = leg.getCalculationPeriodDates().getFirstRegularPeriodStartDate();
    if(cal == null) {
      //no period start, life is easy
      return DateUtil.getDate(leg.getCalculationPeriodDates().getEffectiveDate().getUnadjustedDate().getValue());
    }

    //if we have a stub, we always use this date
    if(leg.getStubCalculationPeriodAmount() != null) {
      return DateUtil.getDate(cal);
    }

    LocalDate effectiveDate = DateUtil.getDate(leg.getCalculationPeriodDates().getEffectiveDate().getUnadjustedDate().getValue());
    //if our effective date is in the future, then we use that, ignoring the period date
    if(effectiveDate.isAfter(valuationDate)) {
      return effectiveDate;
    }

    //no stub, is it IMM? If it isn't. LCH generates this value for IMM trades since it's not sure if they have
    //a stub or not, so if it's IMM, we ignore this value
    boolean isIMM = leg.getCalculationPeriodDates().getCalculationPeriodFrequency().getRollConvention().startsWith("IMM");
    //we don't ignore this value though if our effectiveDate is in the past
    if(!isIMM) {
      return DateUtil.getDate(cal);
    }

    //no stub or no stub + isIMM, just use the effective date
    return effectiveDate;
  }
}
