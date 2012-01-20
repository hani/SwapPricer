package net.formicary.pricer.util;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.bind.JAXBElement;
import javax.xml.datatype.XMLGregorianCalendar;

import net.formicary.pricer.model.DayCountFraction;
import org.fpml.spec503wd3.*;

import static net.formicary.pricer.model.DayCountFraction.*;

/**
 * @author hsuleiman
 *         Date: 12/27/11
 *         Time: 10:11 AM
 */
public class FpMLUtil {
  private static final Map<String, DayCountFraction> fractionMapping = new HashMap<String, DayCountFraction>();
  private static final String[] EMPTY = new String[2];

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
    fractionMapping.put("ACT/365.FIXED", ACT_365);
  }

  public static InterestRateStream getFixedStream(Swap s) {
    for (InterestRateStream stream : s.getSwapStream()) {
      if(isFixedStream(stream)) {
        return stream;
      }
    }
    throw new IllegalArgumentException("Trades with two floating streams not supported yet");
  }

  public static boolean isFixedStream(InterestRateStream stream) {
    Calculation calculation = stream.getCalculationPeriodAmount().getCalculation();
    return (calculation != null && calculation.getFixedRateSchedule() != null)
      || stream.getCalculationPeriodAmount().getKnownAmountSchedule() != null;
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

  public static FastDate getEndDate(InterestRateStream leg) {
    //if we have a stub, then our end is from this date
    XMLGregorianCalendar cal = leg.getCalculationPeriodDates().getLastRegularPeriodEndDate();
    //no stub, just use the termination date
    if(cal == null) cal = leg.getCalculationPeriodDates().getTerminationDate().getUnadjustedDate().getValue();
    return DateUtil.getDate(cal);
  }

  public static FastDate getStartDate(FastDate valuationDate, InterestRateStream leg) {
    XMLGregorianCalendar cal = leg.getCalculationPeriodDates().getFirstRegularPeriodStartDate();
    if(cal != null) {
      return DateUtil.getDate(cal);
    }

    FastDate effectiveDate = DateUtil.getDate(leg.getCalculationPeriodDates().getEffectiveDate().getUnadjustedDate().getValue());
    return effectiveDate;
  }

  public static BigDecimal getInitialFloatingRate(Calculation calculation) {
    JAXBElement<FloatingRateCalculation> fc = (JAXBElement<FloatingRateCalculation>) calculation.getRateCalculation();
    if(fc != null) {
      return fc.getValue().getInitialRate();
    }
    return null;
  }

  public static double getSpread(Calculation calculation) {
    JAXBElement<FloatingRateCalculation> fc = (JAXBElement<FloatingRateCalculation>) calculation.getRateCalculation();
    if(fc != null) {
      List<SpreadSchedule> spreadSchedule = fc.getValue().getSpreadSchedule();
      if(spreadSchedule.size() == 0) {
        return 0;
      }
      if(spreadSchedule.size() > 1) {
        throw new UnsupportedOperationException("Multiple spreadschedule not supported yet");
      }
      return spreadSchedule.get(0).getInitialValue().doubleValue();
    }
    return 0;
  }

  /**
   * Split up the floating index name into the index name + source, so we can spot OIS trades
   * @return never returns null, if it's a fixed side then we return an array of two null strings
   */
  public static String[] getFloatingIndexName(Calculation calculation) {
    if(calculation == null) return EMPTY;
    JAXBElement<? extends Rate> rateCalculation = calculation.getRateCalculation();
    if(rateCalculation == null) return EMPTY;
    FloatingRateCalculation floatingCalc = (FloatingRateCalculation) rateCalculation.getValue();
    if(floatingCalc == null || floatingCalc.getFloatingRateIndex() == null) return EMPTY;
    String index = floatingCalc.getFloatingRateIndex().getValue();
    return getFloatingIndexName(index);
  }

  public  static String[] getFloatingIndexName(String index) {
    String[] names = new String[2];
    int dash = index.indexOf('-') + 1;
    int dash2 = index.indexOf('-', dash);
    names[0] = index.substring(dash, dash2);
    names[1] = index.substring(dash2);
    return names;
  }
}
