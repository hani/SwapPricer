package net.formicary.pricer;

import java.lang.Math;
import java.util.*;
import javax.inject.Inject;

import net.formicary.pricer.model.Cashflow;
import net.formicary.pricer.model.FlowType;
import net.formicary.pricer.util.FastDate;
import net.formicary.pricer.util.FpMLUtil;
import org.fpml.spec503wd3.*;

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

  public List<Cashflow> generateCashflows(FastDate valuationDate, String id) {
    Swap swap = tradeStore.getTrade(id);

    List<Cashflow> flows = new ArrayList<Cashflow>(80);
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

  private List<Cashflow> generateFloatingFlows(FastDate valuationDate, InterestRateStream leg) {
    StreamContext ctx = new StreamContext(calendarManager, valuationDate, leg);
    List<FastDate> calculationDates = ctx.calculationDates;
    CalculationPeriodFrequency interval = ctx.interval;
    Interval paymentInterval = leg.getPaymentDates().getPaymentFrequency();
    FastDate paymentStartDate = ctx.effectiveDate;
    if(interval.getPeriod() == paymentInterval.getPeriod() && interval.getPeriodMultiplier().equals(paymentInterval.getPeriodMultiplier())) {
      paymentInterval = interval;
      if(ctx.firstRegularPeriodStartDate != null)
        paymentStartDate = ctx.firstRegularPeriodStartDate;
    }

    List<FastDate> paymentDates = calendarManager.getAdjustedDates(paymentStartDate, ctx.endDate, ctx.conventions, paymentInterval, FpMLUtil.getBusinessCenters(leg), interval.getRollConvention());
    if(ctx.lastRegularPeriodEndDate != null && ctx.lastRegularPeriodEndDate.lt(ctx.terminationDate)) {
      paymentDates.add(ctx.terminationDate);
    }
    int nextPaymentIndex = Collections.binarySearch(paymentDates, ctx.cutoffDate.plusDays(1));
    if(nextPaymentIndex == 0) {
      //we have a trade that starts 'now' effectively, so the first date we calculate is actually the start date
      //and we can move to the next one which will be the first pauyment
      nextPaymentIndex = 1;
    }
    else if(nextPaymentIndex < 0) {
      nextPaymentIndex = -(nextPaymentIndex + 1);
      //if index = 0, means we have a payment at the start (which doesn't count, since nothing has happened yet)
      if(nextPaymentIndex == 0) nextPaymentIndex = 1;
    }
    //we want the last payment thats before the cutoff date, that's when our calculations start

    List<FastDate> fixingDates = calendarManager.getFixingDates(calculationDates, leg.getResetDates().getFixingDates());
    List<Cashflow> flows = new ArrayList<Cashflow>(calculationDates.size() - nextPaymentIndex + 2);
    for(int i = nextPaymentIndex; i < calculationDates.size(); i++) {
      FastDate periodEndDate = calculationDates.get(i);
      FastDate fixingDate = fixingDates.get(i -1);
      FastDate periodStartDate = calculationDates.get(i - 1);
      //todo need to figure out how to handle this
 //     BigDecimal initialFloatingRate = FpMLUtil.getInitialFloatingRate(calculation);
//      if(i == nextPaymentIndex && initialFloatingRate != null) {
//          Cashflow flow = getCashflow(periodStartDate, periodEndDate, ctx, initialFloatingRate.doubleValue());
//          flows.add(flow);
//      }
      if(fixingDate.lteq(valuationDate) && interval.getPeriod() != PeriodEnum.T) {
        String tenor = ctx.calculationTenor;
        if(ctx.isOIS) tenor = "1D";
        double rate = rateManager.getZeroRate(ctx.floatingIndexName, ctx.currency, tenor, fixingDate) / 100;
        Cashflow flow = getCashflow(periodStartDate, periodEndDate, ctx, rate);
        flows.add(flow);
      } else {
        //todo centers should be based on the index centers, and period start should be fixingdate + 2D (to handle case where periodstart is a bad date)
        FastDate tenorEndDate;
        if(interval.getPeriod() == PeriodEnum.T || ctx.isOIS) {
          tenorEndDate = periodEndDate;
        } else {
          tenorEndDate = calendarManager.applyInterval(periodStartDate, interval, BusinessDayConventionEnum.MODFOLLOWING, ctx.calculationCenters[1]);
        }
        String curve = ctx.isOIS ? "OIS" : ctx.calculationTenor;
        double impliedForwardRate = curveManager.getImpliedForwardRate(periodStartDate, tenorEndDate, valuationDate, ctx.currency, curve);
        Cashflow flow = getCashflow(periodStartDate, periodEndDate, ctx, impliedForwardRate);
        flows.add(flow);
      }
    }
    //we have all the calculated cashflows, we next need to check the payment dates
    //shortcut case, we have the same schedules
    if(ctx.initialStub != null && ctx.firstRegularPeriodStartDate.gt(ctx.cutoffDate)) {
      FastDate endDate = ctx.firstRegularPeriodStartDate;
      flows.add(0, calculateStubCashflow(ctx, ctx.effectiveDate, endDate, ctx.initialStub.getFloatingRate()));
    }
    if(ctx.finalStub != null) {
      FastDate endDate = ctx.terminationDate;
      FastDate startDate = ctx.endDate;
      flows.add(calculateStubCashflow(ctx, startDate, endDate, ctx.finalStub.getFloatingRate()));
    }
    flows = convertToPaymentFlows(ctx, flows, paymentDates);
    for(Cashflow flow : flows) {
      discountFlow(ctx, flow);
    }
    return flows;
  }

  private List<Cashflow> generateFixedFlows(FastDate valuationDate, InterestRateStream leg) {
    StreamContext ctx = new StreamContext(calendarManager, valuationDate, leg);
    List<FastDate> calculationDates = ctx.calculationDates;
    List<Cashflow> flows = new ArrayList<Cashflow>(calculationDates.size());
    Calculation calculation = leg.getCalculationPeriodAmount().getCalculation();
    if(calculation != null) {
      double rate = calculation.getFixedRateSchedule().getInitialValue().doubleValue();
      for(int i = 1; i < calculationDates.size(); i++) {
        FastDate paymentDate = calculationDates.get(i);
        if(paymentDate.gt(ctx.cutoffDate)) {
          Cashflow flow = getCashflow(calculationDates.get(i - 1), paymentDate, ctx, rate);
          flows.add(flow);
        }
      }
    } else if (ctx.knownAmount != 0) {
      Cashflow flow = new Cashflow();
      flow.setAmount(ctx.knownAmount);
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
      discountFlow(ctx, flow);
    }
    return flows;
  }

  private Cashflow calculateStubCashflow(StreamContext ctx, FastDate startDate, FastDate endDate, List<FloatingRate> stubRates) {
    BusinessCenters centers = ctx.stream.getCalculationPeriodDates().getCalculationPeriodDatesAdjustments().getBusinessCenters();
    endDate = calendarManager.adjustDate(endDate, ctx.conventions[1], centers);
    double rate1Value = 0, rate2Value = 0;
    if(stubRates.size() > 0) {
      FloatingRate rate1 = stubRates.get(0);
      FastDate rate1EndDate = calendarManager.applyInterval(startDate, rate1.getIndexTenor(), BusinessDayConventionEnum.MODFOLLOWING, ctx.calculationCenters[1]);
      rate1Value = curveManager.getImpliedForwardRate(startDate, rate1EndDate, ctx.valuationDate, ctx.currency, ctx.calculationTenor);
      if(stubRates.size() == 2) {
        FloatingRate rate2 = stubRates.get(1);
        FastDate rate2EndDate = calendarManager.applyInterval(startDate, rate2.getIndexTenor(), BusinessDayConventionEnum.MODFOLLOWING, ctx.calculationCenters[1]);
        rate2Value = curveManager.getImpliedForwardRate(startDate, rate2EndDate, ctx.valuationDate, ctx.currency, ctx.calculationTenor);
      }
    }
    double rateToUse = rate1Value;
    if(stubRates.size() == 2) {
      int periodLength = startDate.numDaysFrom(endDate);
      FastDate tenor1End = calendarManager.applyInterval(startDate, stubRates.get(0).getIndexTenor(), ctx.conventions[1], centers);
      FastDate tenor2End = calendarManager.applyInterval(startDate, stubRates.get(1).getIndexTenor(), ctx.conventions[1], centers);
      int rate1Period = startDate.numDaysFrom(tenor1End);
      int rate2Period = startDate.numDaysFrom(tenor2End);
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

  private List<Cashflow> convertToPaymentFlows(StreamContext ctx, List<Cashflow> flows, List<FastDate> paymentDates) {
    List<Cashflow> paymentFlows = new ArrayList<Cashflow>(paymentDates.size());
    int start = Collections.binarySearch(paymentDates, ctx.cutoffDate.plusDays(1));
    double spread = FpMLUtil.getSpread(ctx.stream.getCalculationPeriodAmount().getCalculation());
    //if start >= 0, then we have a payment on the cutoff day, so we go from start to end
    //if start < 0, then we reverse it to find the right index to start at, then go to the end
    if(start < 0) start = -(start + 1);
    //rule out flows before the last payment
    if(start > 0) {
      FastDate lastHistoricPayment = paymentDates.get(start - 1);
      Iterator<Cashflow> i = flows.iterator();
      while (i.hasNext()) {
        Cashflow flow = i.next();
        if(flow.getDate().lteq(lastHistoricPayment)) {
          i.remove();
        } else {
          //if we hit one that's later, we know we're done since the flows are date sorted
          break;
        }
      }
    }
    for(int i = start; i < paymentDates.size(); i++) {
      Cashflow payment = new Cashflow();
      FastDate paymentDate = paymentDates.get(i);
      payment.setDate(paymentDate);
      double notional = ctx.notional;
      Iterator<Cashflow> iter = flows.iterator();
      while(iter.hasNext()) {
        Cashflow flow = iter.next();
        if(flow.getDate().lteq(paymentDate)) {
          if(flow.getAmount() == 0) {
            double rate = flow.getRate();
            rate = rate + spread;
            flow.setAmount(notional * rate * flow.getDayCountFraction());
            if(ctx.paying) {
              //we're paying, so reverse values
              flow.setAmount(-flow.getAmount());
            }
          }
          if(ctx.isOIS || ctx.compoundingMethod == CompoundingMethodEnum.FLAT || ctx.compoundingMethod == CompoundingMethodEnum.STRAIGHT) {
            //we use abs here since compounding is always positive
            notional += Math.abs(flow.getAmount());
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

  private Cashflow getCashflow(FastDate periodStart, FastDate periodEnd, StreamContext ctx, double rate) {
    Cashflow flow = new Cashflow();
    double dayCountFraction = calendarManager.getDayCountFraction(periodStart, periodEnd, FpMLUtil.getDayCountFraction(ctx.fraction.getValue()));
    flow.setDayCountFraction(dayCountFraction);
    flow.setDate(periodEnd);
    flow.setRate(rate);
    flow.setType(ctx.isFixed ? FlowType.FIX : FlowType.FLT);
    return flow;
  }

  private void discountFlow(StreamContext ctx, Cashflow flow) {
    String currency = ctx.currency;
    double discountFactor = curveManager.getDiscountFactor(flow.getDate(), ctx.valuationDate, currency, ctx.paymentTenor, ctx.isFixed);
    flow.setDiscountFactor(discountFactor);
    flow.setNpv(discountFactor * flow.getAmount());
  }
}
