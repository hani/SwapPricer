package net.formicary.pricer;

import java.util.List;

import net.formicary.pricer.util.DateUtil;
import net.formicary.pricer.util.FpMLUtil;
import org.fpml.spec503wd3.*;
import org.joda.time.LocalDate;

/**
 * @author hani
 *         Date: 1/3/12
 *         Time: 5:43 PM
 */
public class StreamContext {
  final boolean isFixed;
  final StubValue initialStub;
  final StubValue finalStub;
  final CalculationPeriodFrequency interval;
  final LocalDate valuationDate;
  final LocalDate cutoffDate;
  final LocalDate startDate;
  final LocalDate endDate;
  final BusinessDayConventionEnum[] conventions;
  final List<LocalDate> calculationDates;
  final InterestRateStream stream;
  final String currency;
  final double notional;
  final boolean paying;
  CompoundingMethodEnum compoundingMethod;
  DayCountFraction fraction;
  //we'll always pretend to be partyA from the LCH pov, to match the dmp tool
  private String ourName = "partyA";

  public StreamContext(CalendarManager calendarManager, LocalDate valuationDate, InterestRateStream leg) {
    this.stream = leg;
    this.valuationDate = valuationDate;
    this.cutoffDate = valuationDate.plusDays(3);
    this.isFixed = FpMLUtil.isFixedStream(leg);
    startDate = FpMLUtil.getStartDate(valuationDate, leg);
    endDate = FpMLUtil.getEndDate(leg);
    conventions = FpMLUtil.getBusinessDayConventions(leg);
    paying = ((Party) leg.getPayerPartyReference().getHref()).getId().equals(ourName);
    fraction = leg.getCalculationPeriodAmount().getCalculation().getDayCountFraction();
    compoundingMethod = leg.getCalculationPeriodAmount().getCalculation().getCompoundingMethod();
    LocalDate regularPeriodStartDate =  DateUtil.getDate(leg.getCalculationPeriodDates().getFirstRegularPeriodStartDate());
    LocalDate lastRegularPeriodEndDate = DateUtil.getDate(leg.getCalculationPeriodDates().getLastRegularPeriodEndDate());
    BusinessCenters[] centers = FpMLUtil.getBusinessCenters(leg);
    //if we have a period start date, then we use the period conventions
    if(regularPeriodStartDate != null) {
      conventions[0] = conventions[1];
      centers[0] = centers[1];
    }
    interval = leg.getCalculationPeriodDates().getCalculationPeriodFrequency();
    LocalDate earliest = startDate;
    if(earliest.isBefore(valuationDate)) {
      earliest = valuationDate.minusYears(1);
    }
    calculationDates = calendarManager.getAdjustedDates(earliest, endDate, conventions, interval, centers);
    initialStub = FpMLUtil.getInitialStub(leg);
    finalStub = FpMLUtil.getFinalStub(leg);

    if(regularPeriodStartDate != null && regularPeriodStartDate.isAfter(cutoffDate) && initialStub == null) {
      //it's an imaginary stub! We have a hidden flow between effectivedate and calcperiodstart date
      LocalDate unadjustedEffectiveDate = DateUtil.getDate(leg.getCalculationPeriodDates().getEffectiveDate().getUnadjustedDate().getValue());
      if(!calculationDates.get(0).equals(unadjustedEffectiveDate))
        calculationDates.add(0, calendarManager.adjustDate(unadjustedEffectiveDate, conventions[1], centers[1]));
    }
    //now check if we have a back stub of some sort
    if(lastRegularPeriodEndDate != null && finalStub == null) {
      LocalDate terminationDate = DateUtil.getDate(leg.getCalculationPeriodDates().getTerminationDate().getUnadjustedDate().getValue());
      //add a final period
      if(terminationDate.isAfter(lastRegularPeriodEndDate))
        calculationDates.add(calendarManager.adjustDate(terminationDate, conventions[2], centers[2]));
    }
    //stubs can only be on floating side right? Otherwise it'd be a fake stub handled above
    if(stream.getCalculationPeriodAmount().getKnownAmountSchedule() != null) {
      throw new IllegalArgumentException("Trades with knownAmountSchedule not supported yet");
    }
    AmountSchedule notionalStepSchedule = leg.getCalculationPeriodAmount().getCalculation().getNotionalSchedule().getNotionalStepSchedule();
    currency = notionalStepSchedule.getCurrency().getValue();
    notional = notionalStepSchedule.getInitialValue().doubleValue();
  }
}
