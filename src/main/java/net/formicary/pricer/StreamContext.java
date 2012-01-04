package net.formicary.pricer;

import net.formicary.pricer.util.DateUtil;
import net.formicary.pricer.util.FpMLUtil;
import org.fpml.spec503wd3.*;
import org.joda.time.LocalDate;

import java.math.BigDecimal;
import java.util.List;

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
  final CompoundingMethodEnum compoundingMethod;
  final DayCountFraction fraction;
  LocalDate firstRegularPeriodStartDate;
  //we'll always pretend to be partyA from the LCH pov, to match the dmp tool
  private String ourName = "partyA";
  final LocalDate lastRegularPeriodEndDate;
  final LocalDate effectiveDate;
  final LocalDate terminationDate;
  final BigDecimal knownAmount;

  public StreamContext(CalendarManager calendarManager, LocalDate valuationDate, InterestRateStream leg) {
    this.stream = leg;
    this.valuationDate = valuationDate;
    this.cutoffDate = valuationDate.plusDays(3);
    this.isFixed = FpMLUtil.isFixedStream(leg);
    startDate = FpMLUtil.getStartDate(valuationDate, leg);
    endDate = FpMLUtil.getEndDate(leg);
    conventions = FpMLUtil.getBusinessDayConventions(leg);
    paying = ((Party) leg.getPayerPartyReference().getHref()).getId().equals(ourName);

    Calculation calculation = leg.getCalculationPeriodAmount().getCalculation();
    if(calculation != null) {
      fraction = calculation.getDayCountFraction();
      compoundingMethod = calculation.getCompoundingMethod();
    } else {
      fraction = null;
      compoundingMethod = null;
    }
    AmountSchedule knownAmountSchedule = leg.getCalculationPeriodAmount().getKnownAmountSchedule();
    if(knownAmountSchedule != null) {
      knownAmount = knownAmountSchedule.getInitialValue();
    } else {
      knownAmount = null;
    }
    firstRegularPeriodStartDate =  DateUtil.getDate(leg.getCalculationPeriodDates().getFirstRegularPeriodStartDate());
    lastRegularPeriodEndDate = DateUtil.getDate(leg.getCalculationPeriodDates().getLastRegularPeriodEndDate());
    BusinessCenters[] centers = FpMLUtil.getBusinessCenters(leg);
    //if we have a period start date, then we use the period conventions
    if(firstRegularPeriodStartDate != null) {
      conventions[0] = conventions[1];
      centers[0] = centers[1];
    }
    interval = leg.getCalculationPeriodDates().getCalculationPeriodFrequency();
    LocalDate earliest = startDate;
    calculationDates = calendarManager.getAdjustedDates(earliest, endDate, conventions, interval, centers);
    initialStub = FpMLUtil.getInitialStub(leg);
    finalStub = FpMLUtil.getFinalStub(leg);

    effectiveDate = DateUtil.getDate(leg.getCalculationPeriodDates().getEffectiveDate().getUnadjustedDate().getValue());
    if(firstRegularPeriodStartDate != null && firstRegularPeriodStartDate.isAfter(cutoffDate) && initialStub == null) {
      //it's an imaginary stub! We have a hidden flow between effectivedate and calcperiodstart date
      if(!calculationDates.get(0).equals(effectiveDate))
        calculationDates.add(0, calendarManager.adjustDate(effectiveDate, conventions[1], centers[1]));
    }
    //now check if we have a back stub of some sort
    terminationDate = DateUtil.getDate(leg.getCalculationPeriodDates().getTerminationDate().getUnadjustedDate().getValue());
    if(lastRegularPeriodEndDate != null && finalStub == null) {
      //add a final period
      if(terminationDate.isAfter(lastRegularPeriodEndDate))
        calculationDates.add(calendarManager.adjustDate(terminationDate, conventions[2], centers[2]));
    }
    //stubs can only be on floating side right? Otherwise it'd be a fake stub handled above
    if(calculation != null) {
      AmountSchedule notionalStepSchedule = calculation.getNotionalSchedule().getNotionalStepSchedule();
      currency = notionalStepSchedule.getCurrency().getValue();
      notional = notionalStepSchedule.getInitialValue().doubleValue();
    } else {
      notional = 0;
      currency = knownAmountSchedule.getCurrency().getValue();
    }
  }
}
