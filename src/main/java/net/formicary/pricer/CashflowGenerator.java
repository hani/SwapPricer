package net.formicary.pricer;

import net.formicary.pricer.model.Cashflow;
import net.formicary.pricer.model.FlowType;
import net.formicary.pricer.util.FpMLUtil;
import org.fpml.spec503wd3.*;
import org.joda.time.Days;
import org.joda.time.LocalDate;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.math.BigInteger;
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
  private static final BigInteger ONE = new BigInteger("1");
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
        List<Cashflow> floating = generateFloatingFlows(valuationDate, stream);
        adjustForPaymentOffset(stream, floating);
        flows.addAll(floating);
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

  private List<Cashflow> generateFloatingFlows(LocalDate valuationDate, InterestRateStream leg) {
    StreamContext ctx = new StreamContext(calendarManager, valuationDate, leg);
    Calculation calculation = leg.getCalculationPeriodAmount().getCalculation();
    List<LocalDate> calculationDates = ctx.calculationDates;
    CalculationPeriodFrequency interval = ctx.interval;
    Interval paymentInterval = leg.getPaymentDates().getPaymentFrequency();
    LocalDate paymentStartDate = ctx.effectiveDate;
    if(interval.getPeriod() == paymentInterval.getPeriod() && interval.getPeriodMultiplier().equals(paymentInterval.getPeriodMultiplier())) {
      paymentInterval = interval;
      if(ctx.firstRegularPeriodStartDate != null)
        paymentStartDate = ctx.firstRegularPeriodStartDate;
    }

    List<LocalDate> paymentDates = calendarManager.getAdjustedDates(paymentStartDate, ctx.endDate, ctx.conventions, paymentInterval, FpMLUtil.getBusinessCenters(leg), interval.getRollConvention());
    if(ctx.lastRegularPeriodEndDate != null && ctx.lastRegularPeriodEndDate.isBefore(ctx.terminationDate)) {
      paymentDates.add(ctx.terminationDate);
    }
    int nextPaymentIndex = Collections.binarySearch(paymentDates, ctx.cutoffDate.plusDays(1));
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
 //     BigDecimal initialFloatingRate = FpMLUtil.getInitialFloatingRate(calculation);
//      if(i == nextPaymentIndex && initialFloatingRate != null) {
//          Cashflow flow = getCashflow(periodStartDate, periodEndDate, ctx, initialFloatingRate.doubleValue());
//          flows.add(flow);
//      }
      if((fixingDate.isBefore(valuationDate) || fixingDate.equals(valuationDate)) && interval.getPeriod() != PeriodEnum.T) {
        double rate = rateManager.getZeroRate(ctx.floatingIndexName, ctx.currency, interval, fixingDate) / 100;
        Cashflow flow = getCashflow(periodStartDate, periodEndDate, ctx, rate);
        flows.add(flow);
      } else {
        LocalDate tenorEndDate = periodEndDate;
//        if(ctx.checkForEndToEndIndexRoll) {
//          Interval rollInterval = new Interval();
//          rollInterval.setPeriod(PeriodEnum.D);
//          rollInterval.setPeriodMultiplier(ONE);
//          LocalDate rolled = calendarManager.applyInterval(periodStartDate, rollInterval, ctx.conventions[1], ctx.calculationCenters[1]);
//          if(rolled.getMonthOfYear() != periodStartDate.getMonthOfYear()) {
//            //need to roll the end date to the last of the month, since the start is at the end of a month and rate is end to end
//            tenorEndDate = tenorEndDate.dayOfMonth().withMaximumValue();
//            tenorEndDate = calendarManager.adjustDate(tenorEndDate, ctx.conventions[1], ctx.calculationCenters[1]);
//          }
//        }
        double impliedForwardRate = curveManager.getImpliedForwardRate(periodStartDate, tenorEndDate, valuationDate, ctx.currency, interval);
        Cashflow flow = getCashflow(periodStartDate, periodEndDate, ctx, impliedForwardRate);
        flows.add(flow);
      }
    }
    //we have all the calculated cashflows, we next need to check the payment dates
    //shortcut case, we have the same schedules
    if(ctx.initialStub != null && ctx.firstRegularPeriodStartDate.isAfter(ctx.cutoffDate)) {
      LocalDate endDate = ctx.firstRegularPeriodStartDate;
      flows.add(0, calculateStubCashflow(ctx, ctx.effectiveDate, endDate, ctx.initialStub.getFloatingRate()));
    }
    if(ctx.finalStub != null) {
      LocalDate endDate = ctx.terminationDate;
      LocalDate startDate = ctx.endDate;
      flows.add(calculateStubCashflow(ctx, startDate, endDate, ctx.finalStub.getFloatingRate()));
    }
    flows = convertToPaymentFlows(ctx, flows, paymentDates);
    for(Cashflow flow : flows) {
      discountFlow(ctx, flow, leg);
    }
    return flows;
  }

  private List<Cashflow> generateFixedFlows(LocalDate valuationDate, InterestRateStream leg) {
    StreamContext ctx = new StreamContext(calendarManager, valuationDate, leg);
    List<LocalDate> calculationDates = ctx.calculationDates;
    List<Cashflow> flows = new ArrayList<Cashflow>();
    Calculation calculation = leg.getCalculationPeriodAmount().getCalculation();
    if(calculation != null) {
      BigDecimal rate = calculation.getFixedRateSchedule().getInitialValue();
      for(int i = 1; i < calculationDates.size(); i++) {
        LocalDate paymentDate = calculationDates.get(i);
        if(paymentDate.isAfter(ctx.cutoffDate)) {
          Cashflow flow = getCashflow(calculationDates.get(i - 1), paymentDate, ctx, rate.doubleValue());
          flows.add(flow);
        }
      }
    } else if (ctx.knownAmount != null) {
      Cashflow flow = new Cashflow();
      flow.setAmount(ctx.knownAmount.doubleValue());
      flow.setDate(ctx.endDate);
      flows.add(flow);
    }
    for(Cashflow flow : flows) {
      if(flow.getAmount() == 0) {
        double undiscountedAmount = ctx.notional * flow.getRate() * flow.getDayCountFraction();
        flow.setAmount(undiscountedAmount);
        if(ctx.paying) {
          //we're paying, so reverse values
          flow.setAmount(-flow.getAmount());
        }
      }
      discountFlow(ctx, flow, leg);
    }
    return flows;
  }

  private Cashflow calculateStubCashflow(StreamContext ctx, LocalDate startDate, LocalDate endDate, List<FloatingRate> stubRates) {
    BusinessCenters centers = ctx.stream.getCalculationPeriodDates().getCalculationPeriodDatesAdjustments().getBusinessCenters();
    endDate = calendarManager.adjustDate(endDate, ctx.conventions[1], centers);
    double rate1Value = 0, rate2Value = 0;
    if(stubRates.size() > 0) {
      FloatingRate rate1 = stubRates.get(0);
      //I guess for stuff in the past we need to check the historic index?
      //String index = getFloatingIndexName(rate1.getFloatingRateIndex().getValue());
      String tenor1 = rate1.getIndexTenor().getPeriodMultiplier() + rate1.getIndexTenor().getPeriod().value();
      //this is incorrect since we need to use the regular curve and NOT the stub tenor curve, whih is what LCH does
      //what we do here is technically correct, just not how LCH prices their stubs
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
    LocalDate endDate = ctx.terminationDate;
    endDate = calendarManager.adjustDate(endDate, ctx.conventions[1], centers);
    return getCashflow(ctx.endDate, endDate, ctx, 0);
  }

  private List<Cashflow> convertToPaymentFlows(StreamContext ctx, List<Cashflow> flows, List<LocalDate> paymentDates) {
    List<Cashflow> paymentFlows = new ArrayList<Cashflow>();
    int start = Collections.binarySearch(paymentDates, ctx.cutoffDate.plusDays(1));
    BigDecimal spread = FpMLUtil.getSpread(ctx.stream.getCalculationPeriodAmount().getCalculation());
    //if start >= 0, then we have a payment on the cutoff day, so we go from start to end
    //if start < 0, then we reverse it to find the right index to start at, then go to the end
    if(start < 0) start = -(start + 1);
    //rule out flows before the last payment
    if(start > 0) {
      LocalDate lastHistoricPayment = paymentDates.get(start - 1);
      Iterator<Cashflow> i = flows.iterator();
      while (i.hasNext()) {
        Cashflow flow = i.next();
        if(flow.getDate().isBefore(lastHistoricPayment) || flow.getDate().equals(lastHistoricPayment)) {
          i.remove();
        } else {
          //if we hit one that's later, we know we're done since the flows are date sorted
          break;
        }
      }
    }
    for(int i = start; i < paymentDates.size(); i++) {
      Cashflow payment = new Cashflow();
      LocalDate paymentDate = paymentDates.get(i);
      payment.setDate(paymentDate);
      double notional = ctx.notional;
      Iterator<Cashflow> iter = flows.iterator();
      while(iter.hasNext()) {
        Cashflow flow = iter.next();
        if(flow.getDate().isBefore(paymentDate) || flow.getDate().equals(paymentDate)) {
          if(flow.getAmount() == 0) {
            double rate = flow.getRate();
            if(spread != null) {
              rate = rate + spread.doubleValue();
            }
            flow.setAmount(notional * rate * flow.getDayCountFraction());
            if(ctx.paying) {
              //we're paying, so reverse values
              flow.setAmount(-flow.getAmount());
            }
          }
          if(ctx.compoundingMethod == CompoundingMethodEnum.FLAT) {
            notional += flow.getAmount();
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
}
