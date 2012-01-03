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
    StreamContext ctx = new StreamContext(calendarManager, valuationDate, leg);
    Calculation calculation = leg.getCalculationPeriodAmount().getCalculation();
    CalculationPeriodFrequency interval = ctx.interval;
    List<LocalDate> calculationDates = ctx.calculationDates;
    Interval paymentInterval = leg.getPaymentDates().getPaymentFrequency();
    if(interval.getPeriod() == paymentInterval.getPeriod() && interval.getPeriodMultiplier().equals(paymentInterval.getPeriodMultiplier())) {
      paymentInterval = interval;
    }
    List<LocalDate> paymentDates = calendarManager.getAdjustedDates(ctx.startDate, ctx.endDate, ctx.conventions, paymentInterval, FpMLUtil.getBusinessCenters(leg));
    int nextPaymentIndex = Collections.binarySearch(paymentDates, ctx.cutoffDate);
    if(nextPaymentIndex == 0) {
      //hrm this is weird, lets flag it for now
      throw new RuntimeException("Unexpected next payment index");
    }
    else if(nextPaymentIndex < 0) {
      nextPaymentIndex = -(nextPaymentIndex + 1);
      //if index = 0, means we have a payment at the start (which doesn't count, since nothing has happened yet)
      if(nextPaymentIndex == 0) nextPaymentIndex = 1;
    }
    //we want the last payment thats before the cutoff date, that's when our calculations start

    List<LocalDate> fixingDates = calendarManager.getFixingDates(calculationDates, leg.getResetDates().getFixingDates());
    List<Cashflow> flows = new ArrayList<Cashflow>();
    for(int i = nextPaymentIndex; i < calculationDates.size(); i++) {
      LocalDate periodEndDate = calculationDates.get(i);
      LocalDate fixingDate = fixingDates.get(i -1);
      LocalDate periodStartDate = calculationDates.get(i - 1);
      //todo need to figure out how to handle this
//        if(isFirst && getInitialFloatingRate(calculation) != null) {
//          //we have an explicit initial rate
//          Cashflow flow = getCashflow(periodStartDate, periodEndDate, ctx, getInitialFloatingRate(calculation).doubleValue());
//          flows.add(flow);
//        }
      if(fixingDate.isBefore(valuationDate) || fixingDate.equals(valuationDate)) {
        double rate = rateManager.getZeroRate(getFloatingIndexName(calculation), ctx.currency, interval, fixingDate) / 100;
        Cashflow flow = getCashflow(periodStartDate, periodEndDate, ctx, rate);
        flows.add(flow);
      } else {
        double impliedForwardRate = curveManager.getImpliedForwardRate(periodStartDate, periodEndDate, valuationDate, ctx.currency, interval);
        Cashflow flow = getCashflow(periodStartDate, periodEndDate, ctx, impliedForwardRate);
        flows.add(flow);
      }
    }
    //we have all the calculated cashflows, we next need to check the payment dates
    //shortcut case, we have the same schedules
    flows = convertToPaymentFlows(ctx, flows, paymentDates);
    if(ctx.initialStub != null && ctx.startDate.isAfter(ctx.cutoffDate)) {
      LocalDate endDate = DateUtil.getDate(ctx.stream.getCalculationPeriodDates().getEffectiveDate().getUnadjustedDate().getValue());
      flows.add(0, calculateStubCashflow(ctx, ctx.startDate, endDate, ctx.initialStub.getFloatingRate()));
    }
    if(ctx.finalStub != null) {
      LocalDate endDate = DateUtil.getDate(ctx.stream.getCalculationPeriodDates().getTerminationDate().getUnadjustedDate().getValue());
      LocalDate startDate = ctx.endDate;
      flows.add(calculateStubCashflow(ctx, endDate, startDate, ctx.finalStub.getFloatingRate()));
    }
    for(Cashflow flow : flows) {
      discountFlow(ctx, flow, leg);
    }
    return flows;
  }

  private List<Cashflow> generateFixedFlows(LocalDate valuationDate, InterestRateStream leg) {
    StreamContext ctx = new StreamContext(calendarManager, valuationDate, leg);
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

  private Cashflow calculateStubCashflow(StreamContext ctx, LocalDate endDate, LocalDate startDate, List<FloatingRate> stubRates) {
    BusinessCenters centers = ctx.stream.getCalculationPeriodDates().getCalculationPeriodDatesAdjustments().getBusinessCenters();
    endDate = calendarManager.adjustDate(endDate, ctx.conventions[1], centers);
    double rate1Value = 0, rate2Value = 0;
    if(stubRates.size() > 0) {
      FloatingRate rate1 = stubRates.get(0);
      //I guess for stuff in the past we need to check the historic index?
      //String index = getFloatingIndexName(rate1.getFloatingRateIndex().getValue());
      String tenor1 = rate1.getIndexTenor().getPeriodMultiplier() + rate1.getIndexTenor().getPeriod().value();
      rate1Value = curveManager.getInterpolatedForwardRate(startDate, ctx.currency, tenor1);
      if(stubRates.size() == 2) {
        FloatingRate rate2 = stubRates.get(1);
        String tenor2 = rate2.getIndexTenor().getPeriodMultiplier() + rate1.getIndexTenor().getPeriod().value();
        rate2Value = curveManager.getInterpolatedForwardRate(startDate, ctx.currency, tenor2);
      }
    }
    double rateToUse = rate1Value;
    if(stubRates.size() == 2) {
      int periodLength = Days.daysBetween(startDate, endDate).getDays();
      LocalDate tenor1End = calendarManager.applyInterval(startDate, stubRates.get(0).getIndexTenor(), ctx.conventions[1], centers);
      LocalDate tenor2End = calendarManager.applyInterval(startDate, stubRates.get(1).getIndexTenor(), ctx.conventions[1], centers);
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
    int start = Collections.binarySearch(paymentDates, ctx.cutoffDate.plusDays(1));
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
