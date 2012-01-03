package net.formicary.pricer;

import net.formicary.pricer.model.Cashflow;
import net.formicary.pricer.model.FlowType;
import net.formicary.pricer.util.DateUtil;
import net.formicary.pricer.util.FpMLUtil;
import org.fpml.spec503wd3.*;
import org.joda.time.Days;
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

    List<Cashflow> flows = new ArrayList<Cashflow>();
    for(InterestRateStream stream : swap.getSwapStream()) {
      if(FpMLUtil.isFixedStream(stream)) {
        List<Cashflow> fixed = generateFixedFlows(valuationDate, stream);
        adjustForPaymentOffset(stream, fixed);
        flows.addAll(fixed);
      } else {
        flows.addAll(generateFloatingFlows(valuationDate, stream));
      }
    }

    Collections.sort(flows);
    flows.get(0).setTradeId(id);
    return flows;
  }

  private void adjustForPaymentOffset(InterestRateStream leg, List<Cashflow> flows) {
    Offset paymentOffset = leg.getPaymentDates().getPaymentDaysOffset();
    if(paymentOffset != null) {
      BusinessCenters centers = leg.getPaymentDates().getPaymentDatesAdjustments().getBusinessCenters();
      for (Cashflow flow : flows) {
        flow.setDate(calendarManager.applyDayInterval(flow.getDate(), paymentOffset, centers));
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
        if(fixingDate.isBefore(valuationDate) || fixingDate.equals(valuationDate)) {
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
      flows = convertToPaymentFlows(ctx, flows, paymentDates);
    } else {
      for(Cashflow flow : flows) {
        flow.setAmount(ctx.notional * flow.getRate() * flow.getDayCountFraction());
        if(ctx.paying) {
          //we're paying, so reverse values
          flow.setAmount(-flow.getAmount());
        }
      }
    }
    if(ctx.initialStub != null && ctx.startDate.isAfter(ctx.cutoffDate)) {
      flows.add(0, calculateInitialStubCashflow(ctx));
    }
    if(ctx.finalStub != null) {
      flows.add(calculateFinalStubCashflow(ctx));
    }
    for(Cashflow flow : flows) {
      discountFlow(ctx, flow, leg);
    }
    return flows;
  }

  private List<Cashflow> generateFixedFlows(LocalDate valuationDate, InterestRateStream leg) {
    StreamContext ctx = new StreamContext(valuationDate, leg);
    List<LocalDate> calculationDates = ctx.calculationDates;
    List<Cashflow> flows = new ArrayList<Cashflow>();
    BigDecimal rate = leg.getCalculationPeriodAmount().getCalculation().getFixedRateSchedule().getInitialValue();
    for(int i = 1; i < calculationDates.size(); i++) {
      LocalDate paymentDate = calculationDates.get(i);
      if(paymentDate.isAfter(ctx.cutoffDate)) {
        Cashflow flow = getCashflow(calculationDates.get(i - 1), paymentDate, ctx, rate.doubleValue());
        flows.add(flow);
      }
    }
    for(Cashflow flow : flows) {
      double undiscountedAmount = ctx.notional * flow.getRate() * flow.getDayCountFraction();
      flow.setAmount(undiscountedAmount);
      if(ctx.paying) {
        //we're paying, so reverse values
        flow.setAmount(-flow.getAmount());
      }
      discountFlow(ctx, flow, leg);
    }
    return flows;
  }

  private Cashflow calculateInitialStubCashflow(StreamContext ctx) {
    LocalDate endDate = ctx.startDate;
    BusinessCenters centers = ctx.stream.getCalculationPeriodDates().getCalculationPeriodDatesAdjustments().getBusinessCenters();
    endDate = calendarManager.adjustDate(endDate, ctx.conventions[1], centers);
    LocalDate startDate = DateUtil.getDate(ctx.stream.getCalculationPeriodDates().getEffectiveDate().getUnadjustedDate().getValue());
    List<FloatingRate> rates = ctx.initialStub.getFloatingRate();
    double rate1Value = 0, rate2Value = 0;
    if(rates.size() > 0) {
      FloatingRate rate1 = rates.get(0);
      String index = getFloatingIndexName(rate1.getFloatingRateIndex().getValue());
      String tenor1 = rate1.getIndexTenor().getPeriodMultiplier() + rate1.getIndexTenor().getPeriod().value();
      rate1Value = curveManager.getInterpolatedForwardRate(startDate, ctx.currency, tenor1);
      if(rates.size() == 2) {
        FloatingRate rate2 = rates.get(1);
        String tenor2 = rate2.getIndexTenor().getPeriodMultiplier() + rate1.getIndexTenor().getPeriod().value();
        rate2Value = curveManager.getInterpolatedForwardRate(startDate, ctx.currency, tenor2);
      }
    }
    double rateToUse = rate1Value;
    if(rates.size() == 2) {
      int periodLength = Days.daysBetween(startDate, endDate).getDays();
      LocalDate tenor1End = calendarManager.applyInterval(startDate, rates.get(0).getIndexTenor(), ctx.conventions[1], centers);
      LocalDate tenor2End = calendarManager.applyInterval(startDate, rates.get(1).getIndexTenor(), ctx.conventions[1], centers);
      int rate1Period = Days.daysBetween(startDate, tenor1End).getDays();
      int rate2Period = Days.daysBetween(startDate, tenor2End).getDays();
      rateToUse = rate1Value + (periodLength - rate1Period) * (rate2Value - rate1Value)/(rate2Period - rate1Period);
    }
    Cashflow flow = getCashflow(startDate, endDate, ctx, rateToUse);
    flow.setAmount(ctx.notional * flow.getRate() * flow.getDayCountFraction());
    if(ctx.paying) {
      //we're paying, so reverse values
      flow.setAmount(-flow.getAmount());
    }
    return flow;
  }

  private Cashflow calculateFinalStubCashflow(StreamContext ctx) {
    //todo verify which business centers and conventions we use for stubs
    BusinessCenters centers = ctx.stream.getCalculationPeriodDates().getCalculationPeriodDatesAdjustments().getBusinessCenters();
    LocalDate endDate = DateUtil.getDate(ctx.stream.getCalculationPeriodDates().getTerminationDate().getUnadjustedDate().getValue());
    endDate = calendarManager.adjustDate(endDate, ctx.conventions[1], centers);
    return getCashflow(ctx.endDate, endDate, ctx, 0);
  }

  private List<Cashflow> convertToPaymentFlows(StreamContext ctx, List<Cashflow> flows, List<LocalDate> paymentDates) {
    List<Cashflow> paymentFlows = new ArrayList<Cashflow>();
    int start = Collections.binarySearch(paymentDates, ctx.cutoffDate);
    //if start >= 0, then we have a payment on the cutoff day, so we go from start to end
    //if start < 0, then we reverse it to find the right index to start at, then go to the end
    if(start < 0) start = -(start + 1);
    for(int i = start; i < paymentDates.size(); i++) {
      Cashflow payment = new Cashflow();
      LocalDate paymentDate = paymentDates.get(i);
      payment.setDate(paymentDate);
      double notional = ctx.notional;
      Iterator<Cashflow> iter = flows.iterator();
      while(iter.hasNext()) {
        Cashflow flow = iter.next();
        if(flow.getDate().isBefore(paymentDate) || flow.getDate().equals(paymentDate)) {
          flow.setAmount(notional * flow.getRate() * flow.getDayCountFraction());
          if(ctx.compoundingMethod == CompoundingMethodEnum.FLAT) {
            notional += flow.getAmount();
          }
          if(ctx.paying) {
            //we're paying, so reverse values
            flow.setAmount(-flow.getAmount());
          }
          payment.setAmount(payment.getAmount() + flow.getAmount());
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
    double dayCountFraction = calendarManager.getDayCountFraction(periodStart, periodEnd, FpMLUtil.getDayCountFraction(ctx.fraction.getValue()));
    flow.setDayCountFraction(dayCountFraction);
    flow.setDate(periodEnd);
    flow.setRate(rate);
    flow.setType(ctx.isFixed ? FlowType.FIX : FlowType.FLT);
    return flow;
  }

  private void discountFlow(StreamContext ctx, Cashflow flow, InterestRateStream leg) {
    String currency = ctx.currency;
    double discountFactor = curveManager.getDiscountFactor(flow.getDate(), ctx.valuationDate, currency, leg.getPaymentDates().getPaymentFrequency(), ctx.isFixed);
    flow.setDiscountFactor(discountFactor);
    flow.setNpv(discountFactor * flow.getAmount());
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
    double notional;
    boolean paying;
    CompoundingMethodEnum compoundingMethod;
    DayCountFraction fraction;

    public StreamContext(LocalDate valuationDate, InterestRateStream leg) {
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
      calculationDates = calendarManager.getAdjustedDates(startDate, endDate, conventions, interval, centers);
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
