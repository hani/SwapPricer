package net.formicary.pricer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.datatype.XMLGregorianCalendar;

import net.formicary.pricer.util.DateUtil;
import net.formicary.pricer.util.FastDate;
import net.formicary.pricer.util.FpMLUtil;
import org.fpml.spec503wd3.*;

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
  final String calculationTenor;
  final String paymentTenor;
  final FastDate valuationDate;
  final FastDate cutoffDate;
  final FastDate endDate;
  final BusinessDayConventionEnum[] conventions;
  final List<FastDate> calculationDates;
  final InterestRateStream stream;
  final String currency;
  final double notional;
  final boolean paying;
  final CompoundingMethodEnum compoundingMethod;
  final boolean compounding;
  final DayCountFraction fraction;
  final FastDate firstRegularPeriodStartDate;
  //we'll always pretend to be partyA from the LCH pov, to match the dmp tool
  private String ourName = "partyA";
  final FastDate lastRegularPeriodEndDate;
  final FastDate effectiveDate;
  final FastDate terminationDate;
  final double knownAmount;
  final String floatingIndexName;
  final BusinessCenters[] calculationCenters;
  final boolean isOIS;
  final Map<FastDate, Double> notionalSteps;
  final FastDate firstPaymentDate;

  public StreamContext(CalendarManager calendarManager, FastDate valuationDate, InterestRateStream leg) {
    this.stream = leg;
    this.valuationDate = valuationDate;
    this.cutoffDate = valuationDate.plusDays(3);
    this.isFixed = FpMLUtil.isFixedStream(leg);
    endDate = FpMLUtil.getEndDate(leg);
    effectiveDate = DateUtil.getDate(leg.getCalculationPeriodDates().getEffectiveDate().getUnadjustedDate().getValue());
    conventions = FpMLUtil.getBusinessDayConventions(leg);
    paying = ((Party) leg.getPayerPartyReference().getHref()).getId().equals(ourName);

    Calculation calculation = leg.getCalculationPeriodAmount().getCalculation();
    String[] floatingIndexName = FpMLUtil.getFloatingIndexName(calculation);
    if("Federal Funds".equals(floatingIndexName[0])) {
      floatingIndexName[0] = "FEDFUND";
    }
    this.floatingIndexName = floatingIndexName[0];
    isOIS = floatingIndexName[1] != null && floatingIndexName[1].contains("OIS-COMPOUND");
    if(calculation != null) {
      fraction = calculation.getDayCountFraction();
      compoundingMethod = calculation.getCompoundingMethod();
    } else {
      fraction = null;
      compoundingMethod = null;
    }

    compounding = isOIS || compoundingMethod == CompoundingMethodEnum.STRAIGHT || compoundingMethod == CompoundingMethodEnum.FLAT;
    AmountSchedule knownAmountSchedule = leg.getCalculationPeriodAmount().getKnownAmountSchedule();
    if(knownAmountSchedule != null) {
      knownAmount = knownAmountSchedule.getInitialValue().doubleValue();
    } else {
      knownAmount = 0;
    }
    firstRegularPeriodStartDate =  DateUtil.getDate(leg.getCalculationPeriodDates().getFirstRegularPeriodStartDate());
    lastRegularPeriodEndDate = DateUtil.getDate(leg.getCalculationPeriodDates().getLastRegularPeriodEndDate());
    calculationCenters = FpMLUtil.getBusinessCenters(leg);
    //if we have a period start date, then we use the period conventions
    if(firstRegularPeriodStartDate != null) {
      conventions[0] = conventions[1];
      calculationCenters[0] = calculationCenters[1];
    }
    interval = leg.getCalculationPeriodDates().getCalculationPeriodFrequency();
    calculationTenor = interval.getPeriodMultiplier() + interval.getPeriod().value();
    PaymentDates paymentDates = leg.getPaymentDates();
    firstPaymentDate = DateUtil.getDate(paymentDates.getFirstPaymentDate());
    paymentTenor = paymentDates.getPaymentFrequency().getPeriodMultiplier() + paymentDates.getPaymentFrequency().getPeriod().value();

    initialStub = FpMLUtil.getInitialStub(leg);
    finalStub = FpMLUtil.getFinalStub(leg);

    if(isOIS) {
      calculationDates = calendarManager.getValidDays(effectiveDate, calendarManager.adjustDate(endDate, conventions[2], calculationCenters[2]), calculationCenters[1]);
    } else {
      //our first date will always be the effective date
      FastDate earliest = firstRegularPeriodStartDate == null ? effectiveDate : firstRegularPeriodStartDate;
      calculationDates = calendarManager.getAdjustedDates(earliest, endDate, conventions, interval, calculationCenters, null);
      if(firstRegularPeriodStartDate != null) {
        //if our first date isn't our effective date, then we adjust it according to calc conventions, not start conventions
        calculationDates.set(0, calendarManager.adjustDate(calculationDates.get(0), conventions[1], calculationCenters[1]));
        //if we have a stub, we add the calculation start date
        calculationDates.add(0, calendarManager.adjustDate(effectiveDate, conventions[0], calculationCenters[0]));
      }
    }
    //now check if we have a back stub of some sort
    terminationDate = DateUtil.getDate(leg.getCalculationPeriodDates().getTerminationDate().getUnadjustedDate().getValue());
    if(lastRegularPeriodEndDate != null && finalStub == null) {
      //add a final period
      if(terminationDate.gt(lastRegularPeriodEndDate))
        calculationDates.add(calendarManager.adjustDate(terminationDate, conventions[2], calculationCenters[2]));
    }
    //stubs can only be on floating side right? Otherwise it'd be a fake stub handled above
    if(calculation != null) {
      AmountSchedule notionalStepSchedule = calculation.getNotionalSchedule().getNotionalStepSchedule();
      List<Step> steps = notionalStepSchedule.getStep();
      if(steps.size() > 0) {
        notionalSteps = new HashMap<FastDate, Double>();
        for (Step step : steps) {
          XMLGregorianCalendar cal = step.getStepDate();
          notionalSteps.put(new FastDate(cal.getYear(), cal.getMonth(), cal.getDay()), step.getStepValue().doubleValue());
        }
      } else {
        notionalSteps = null;
      }
      currency = notionalStepSchedule.getCurrency().getValue();
      notional = notionalStepSchedule.getInitialValue().doubleValue();
    } else {
      notionalSteps = null;
      notional = 0;
      currency = knownAmountSchedule.getCurrency().getValue();
    }
  }
}
