package net.formicary.pricer;

import net.formicary.pricer.model.Cashflow;
import net.formicary.pricer.model.FlowType;
import net.formicary.pricer.util.DateUtil;
import net.formicary.pricer.util.FpMLUtil;
import org.fpml.spec503wd3.*;
import org.joda.time.LocalDate;

import javax.inject.Inject;
import javax.xml.bind.JAXBElement;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * @author hani
 *         Date: 10/11/11
 *         Time: 9:21 PM
 */
public class CashflowGenerator {
  @Inject CurveManager curveManager;
  @Inject CalendarManager calendarManager;
  @Inject TradeStore tradeStore;
  @Inject RateManager rateManager;
  //we'll always pretend to be partyA from the LCH pov, to match the dmp tool
  private String ourName = "partyA";

  public List<Cashflow> generateCashflows(LocalDate valuationDate, String id) {
    Swap swap = tradeStore.getTrade(id);

    InterestRateStream fixed = FpMLUtil.getFixedStream(swap);
    List<Cashflow> flows = generateFixedFlows(valuationDate, fixed);
    adjustForPaymentOffset(fixed, flows);

    InterestRateStream floating = FpMLUtil.getFloatingStream(swap);
    List<Cashflow> floatingFlows = generateFloatingFlows(valuationDate, floating);
    adjustForPaymentOffset(fixed, floatingFlows);

    flows.addAll(floatingFlows);
    Collections.sort(flows);
    flows.get(0).setTradeId(id);
    return flows;
  }

  private void adjustForPaymentOffset(InterestRateStream leg, List<Cashflow> flows) {
    Offset paymentOffset = leg.getPaymentDates().getPaymentDaysOffset();
    if(paymentOffset != null) {
      BusinessCenters centers = leg.getPaymentDates().getPaymentDatesAdjustments().getBusinessCenters();
      for (Cashflow flow : flows) {
        flow.setDate(calendarManager.applyInterval(flow.getDate(), paymentOffset, centers));
      }
    }
  }

  private BigDecimal getInitialFloatingRate(Calculation calculation) {
    JAXBElement<FloatingRateCalculation> fc = (JAXBElement<FloatingRateCalculation>) calculation.getRateCalculation();
    if(fc != null) {
      return fc.getValue().getInitialRate();
    }
    return null;
  }

  private List<Cashflow> generateFloatingFlows(LocalDate valuationDate, InterestRateStream leg) {
    StreamContext ctx = new StreamContext(valuationDate, leg);
    Calculation calculation = leg.getCalculationPeriodAmount().getCalculation();
    CalculationPeriodFrequency interval = ctx.interval;
    List<LocalDate> calculationDates = ctx.calculationDates;
    List<LocalDate> fixingDates = calendarManager.getFixingDates(calculationDates, leg.getResetDates().getFixingDates());
    List<Cashflow> flows = new ArrayList<Cashflow>();
    for(int i = 1; i < calculationDates.size(); i++) {
      LocalDate periodEndDate = calculationDates.get(i);
      //todo optimise and use binarySearch to find the right index
      if(periodEndDate.isAfter(ctx.cutoffDate)) {
        LocalDate periodStartDate = calculationDates.get(i - 1);
        LocalDate fixingDate = fixingDates.get(i -1);
        //todo need to figure out how to handle this
//        if(isFirst && getInitialFloatingRate(calculation) != null) {
//          //we have an explicit initial rate
//          Cashflow flow = getCashflow(periodStartDate, periodEndDate, ctx, getInitialFloatingRate(calculation).doubleValue());
//          flows.add(flow);
//        }
        if(fixingDate.isBefore(valuationDate)) {
          String index = getFloatingIndexName(calculation);
          double rate = rateManager.getZeroRate(index, ctx.currency, interval, fixingDate) / 100;
          Cashflow flow = getCashflow(periodStartDate, periodEndDate, ctx, rate);
          flows.add(flow);
        } else {
          double impliedForwardRate = curveManager.getImpliedForwardRate(periodStartDate, periodEndDate, valuationDate, ctx.currency, interval);
          Cashflow flow = getCashflow(periodStartDate, periodEndDate, ctx, impliedForwardRate);
          flows.add(flow);
        }
      }
    }
    //we have all the calculated cashflows, we next need to check the payment dates
    //shortcut case, we have the same schedules
    Interval paymentInterval = leg.getPaymentDates().getPaymentFrequency();
    if(interval.getPeriod() != paymentInterval.getPeriod() || !interval.getPeriodMultiplier().equals(paymentInterval.getPeriodMultiplier())) {
      List<LocalDate> paymentDates = calendarManager.getAdjustedDates(ctx.startDate, ctx.endDate, ctx.conventions, paymentInterval, FpMLUtil.getBusinessCenters(leg));
      flows = convertToPaymentFlows(flows, ctx.cutoffDate, paymentDates);
    }
    if(ctx.initialStub != null && ctx.startDate.isAfter(ctx.cutoffDate)) {
      flows.add(calculateInitialStubCashflow(ctx));
    }
    if(ctx.finalStub != null) {
      flows.add(calculateFinalStubCashflow(ctx));
    }
    return flows;
  }

  private List<Cashflow> generateFixedFlows(LocalDate valuationDate, InterestRateStream leg) {
    StreamContext ctx = new StreamContext(valuationDate, leg);
    List<LocalDate> calculationDates = ctx.calculationDates;
    List<Cashflow> flows = new ArrayList<Cashflow>();
    for(int i = 1; i < calculationDates.size(); i++) {
      LocalDate paymentDate = calculationDates.get(i);
      if(paymentDate.isAfter(ctx.cutoffDate)) {
        BigDecimal rate = leg.getCalculationPeriodAmount().getCalculation().getFixedRateSchedule().getInitialValue();
        Cashflow flow = getCashflow(calculationDates.get(i - 1), paymentDate, ctx, rate.doubleValue());
        flows.add(flow);
      }
    }
    return flows;
  }

  private Cashflow calculateInitialStubCashflow(StreamContext ctx) {
    LocalDate endDate = ctx.startDate;
    BusinessCenters centers = ctx.stream.getCalculationPeriodDates().getCalculationPeriodDatesAdjustments().getBusinessCenters();
    endDate = calendarManager.adjustDate(endDate, ctx.conventions[1], centers);
    LocalDate startDate = DateUtil.getDate(ctx.stream.getCalculationPeriodDates().getEffectiveDate().getUnadjustedDate().getValue());
    List<FloatingRate> rates = ctx.initialStub.getFloatingRate();
    double rateToUse = 0;
    if(rates.size() > 0) {
      FloatingRate rate1 = rates.get(0);
      String index = getFloatingIndexName(rate1.getFloatingRateIndex().getValue());
      String tenor1 = rate1.getIndexTenor().getPeriodMultiplier() + rate1.getIndexTenor().getPeriod().value();
      double rate1Value = curveManager.getInterpolatedForwardRate(startDate, ctx.currency, tenor1);
      if(rates.size() == 1) {
        rateToUse = rate1Value;
      } else if(rates.size() == 2) {
        FloatingRate rate2 = rates.get(0);
        String tenor2 = rate2.getIndexTenor().getPeriodMultiplier() + rate1.getIndexTenor().getPeriod().value();
        rateToUse = (rate1Value + curveManager.getInterpolatedForwardRate(startDate, ctx.currency, tenor2)) / 2;
      }
    }
    return getCashflow(startDate, endDate, ctx, rateToUse);
  }

  private Cashflow calculateFinalStubCashflow(StreamContext ctx) {
    //todo verify which business centers and conventions we use for stubs
    BusinessCenters centers = ctx.stream.getCalculationPeriodDates().getCalculationPeriodDatesAdjustments().getBusinessCenters();
    LocalDate endDate = DateUtil.getDate(ctx.stream.getCalculationPeriodDates().getTerminationDate().getUnadjustedDate().getValue());
    endDate = calendarManager.adjustDate(endDate, ctx.conventions[1], centers);
    return getCashflow(ctx.endDate, endDate, ctx, 0);
  }

  private List<Cashflow> convertToPaymentFlows(List<Cashflow> flows, LocalDate cutoff, List<LocalDate> paymentDates) {
    List<Cashflow> paymentFlows = new ArrayList<Cashflow>();
    int start = Collections.binarySearch(paymentDates, cutoff);
    //if start >= 0, then we have a payment on the cutoff day, so we go from start to end
    //if start < 0, then we reverse it to find the right index to start at, then go to the end
    if(start < 0) start = -(start + 1);
    for(int i = start; i < paymentDates.size(); i++) {
      Cashflow payment = new Cashflow();
      LocalDate paymentDate = paymentDates.get(i);
      payment.setDate(paymentDate);
      Iterator<Cashflow> iter = flows.iterator();
      while(iter.hasNext()) {
        Cashflow flow = iter.next();
        if(flow.getDate().isBefore(paymentDate) || flow.getDate().equals(paymentDate)) {
          payment.setAmount(payment.getAmount() + flow.getAmount());
          payment.setNpv(payment.getNpv() + flow.getNpv());
          if(payment.getType() == null)
            payment.setType(flow.getType());
          iter.remove();
        } else {
          //we have a flow after our current payment, let it go to the next payment date
          break;
        }
      }
      if(payment.getAmount() != 0)
        paymentFlows.add(payment);
    }
    return paymentFlows;
  }

  private Cashflow getCashflow(LocalDate periodStart, LocalDate periodEnd, StreamContext ctx, double rate) {
    Cashflow flow = new Cashflow();
    InterestRateStream leg = ctx.stream;
    DayCountFraction fraction = leg.getCalculationPeriodAmount().getCalculation().getDayCountFraction();
    double dayCountFraction = calendarManager.getDayCountFraction(periodStart, periodEnd, FpMLUtil.getDayCountFraction(fraction.getValue()));
    flow.setDayCountFraction(dayCountFraction);
    String currency = ctx.currency;
    boolean paying = ((Party) leg.getPayerPartyReference().getHref()).getId().equals(ourName);
    double undiscountedAmount = ctx.principal * rate * dayCountFraction;
    //if it's a compounding trade, then we add the amount to the principal
    flow.setAmount(undiscountedAmount);
    double discountFactor = curveManager.getDiscountFactor(periodEnd, ctx.valuationDate, currency, leg.getPaymentDates().getPaymentFrequency(), ctx.isFixed);
    flow.setDiscountFactor(discountFactor);
    flow.setNpv(discountFactor * undiscountedAmount);
    flow.setDate(periodEnd);
    flow.setRate(rate);
    flow.setType(ctx.isFixed ? FlowType.FIX : FlowType.FLT);
    if(paying) {
      //we're paying, so reverse values
      flow.reverse();
    }
    if(leg.getCalculationPeriodAmount().getCalculation().getCompoundingMethod() == CompoundingMethodEnum.FLAT) {
      ctx.principal += undiscountedAmount;
    }
    return flow;
  }

  class StreamContext {
    boolean isFixed;
    StubValue initialStub;
    StubValue finalStub;
    CalculationPeriodFrequency interval;
    LocalDate valuationDate;
    LocalDate cutoffDate;
    LocalDate startDate;
    LocalDate endDate;
    BusinessDayConventionEnum[] conventions;
    List<LocalDate> calculationDates;
    InterestRateStream stream;
    String currency;
    double principal;

    public StreamContext(LocalDate valuationDate, InterestRateStream leg) {
      this.stream = leg;
      this.valuationDate = valuationDate;
      this.cutoffDate = valuationDate.plusDays(3);
      this.isFixed = FpMLUtil.isFixedStream(leg);
      startDate = FpMLUtil.getStartDate(valuationDate, leg);
      endDate = FpMLUtil.getEndDate(leg);
      conventions = FpMLUtil.getBusinessDayConventions(leg);
      LocalDate regularPeriodStartDate =  DateUtil.getDate(leg.getCalculationPeriodDates().getFirstRegularPeriodStartDate());
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
      calculationDates = calendarManager.getAdjustedDates(startDate, endDate, conventions, interval, centers);
      initialStub = FpMLUtil.getInitialStub(leg);
      finalStub = FpMLUtil.getFinalStub(leg);

      if(regularPeriodStartDate != null && regularPeriodStartDate.isAfter(cutoffDate) && initialStub == null) {
        //it's an imaginary stub! We have a hidden flow between effectivedate and calcperiodstart date
        LocalDate unadjustedEffectiveDate = DateUtil.getDate(leg.getCalculationPeriodDates().getEffectiveDate().getUnadjustedDate().getValue());
        if(!calculationDates.get(0).equals(unadjustedEffectiveDate))
          calculationDates.add(0, calendarManager.adjustDate(unadjustedEffectiveDate, conventions[1], centers[1]));
      }
      //stubs can only be on floating side right? Otherwise it'd be a fake stub handled above
      AmountSchedule notionalStepSchedule = leg.getCalculationPeriodAmount().getCalculation().getNotionalSchedule().getNotionalStepSchedule();
      currency = notionalStepSchedule.getCurrency().getValue();
      principal = notionalStepSchedule.getInitialValue().doubleValue();
    }
  }

  private String getFloatingIndexName(Calculation calculation) {
    FloatingRateCalculation floatingCalc = (FloatingRateCalculation) calculation.getRateCalculation().getValue();
    String index = floatingCalc.getFloatingRateIndex().getValue();
    return getFloatingIndexName(index);
  }

  private static String getFloatingIndexName(String index) {
    int dash = index.indexOf('-') + 1;
    index = index.substring(dash, index.indexOf('-', dash));
    return index;
  }
}
