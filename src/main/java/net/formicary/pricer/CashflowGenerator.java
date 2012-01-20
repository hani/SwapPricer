package net.formicary.pricer;

import java.lang.Math;
import java.util.*;
import javax.inject.Inject;

import net.formicary.pricer.model.Cashflow;
import net.formicary.pricer.model.FlowType;
import net.formicary.pricer.util.FastDate;
import net.formicary.pricer.util.FpMLUtil;
import org.fpml.spec503wd3.*;

import static org.fpml.spec503wd3.BusinessDayConventionEnum.FOLLOWING;
import static org.fpml.spec503wd3.BusinessDayConventionEnum.MODFOLLOWING;
import static org.fpml.spec503wd3.CompoundingMethodEnum.FLAT;
import static org.fpml.spec503wd3.CompoundingMethodEnum.STRAIGHT;

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
    Product trade = tradeStore.getTrade(id);
    if(!(trade instanceof Swap)) {
      throw new UnsupportedOperationException("Trade of type " + trade.getClass().getSimpleName() + " not supported");
    }
    Swap swap = (Swap)trade;
    List<Cashflow> flows = new ArrayList<Cashflow>(80);
    for(InterestRateStream stream : swap.getSwapStream()) {
      if(FpMLUtil.isFixedStream(stream)) {
        List<Cashflow> fixed = generateFixedFlows(valuationDate, stream);
        flows.addAll(fixed);
      } else {
        List<Cashflow> floating = generateFloatingFlows(valuationDate, stream);
        flows.addAll(floating);
      }
    }

    Collections.sort(flows);
    flows.get(0).setTradeId(id);
    return flows;
  }

  private List<Cashflow> generateFloatingFlows(FastDate valuationDate, InterestRateStream leg) {
    StreamContext ctx = new StreamContext(calendarManager, valuationDate, leg);
    List<FastDate> calculationDates = ctx.calculationDates;
    List<FastDate> paymentDates = getPaymentDates(ctx);
    CalculationPeriodFrequency interval = ctx.interval;
    int nextCalculationIndex = getNextDateIndex(ctx, paymentDates);
    //we want the last calc thats before the cutoff date, that's when our calculations start

    List<FastDate> fixingDates;
    if(ctx.isOIS) {
      RelativeDateOffset oisFixingOffset = IndexProperties.getOISFixingOffset(ctx.currency);
      if(oisFixingOffset != null) {
        fixingDates = calendarManager.getFixingDates(calculationDates, oisFixingOffset);
      } else {
        fixingDates = calculationDates;
      }
    } else {
      fixingDates = calendarManager.getFixingDates(calculationDates, leg.getResetDates().getFixingDates());
    }
    List<Cashflow> flows = new ArrayList<Cashflow>(calculationDates.size() - nextCalculationIndex + 2);
    for(int i = nextCalculationIndex; i < calculationDates.size(); i++) {
      FastDate periodEndDate = calculationDates.get(i);
      FastDate fixingDate = fixingDates.get(i -1);
      FastDate periodStartDate = calculationDates.get(i - 1);
      //todo need to figure out how to handle this
 //     BigDecimal initialFloatingRate = FpMLUtil.getInitialFloatingRate(calculation);
//      if(i == nextCalculationIndex && initialFloatingRate != null) {
//          Cashflow flow = getCashflow(periodStartDate, periodEndDate, ctx, initialFloatingRate.doubleValue());
//          flows.add(flow);
//      }
      if(fixingDate.lteq(valuationDate) && (interval.getPeriod() != PeriodEnum.T || periodEndDate.lteq(valuationDate))) {
        String tenor = ctx.calculationTenor;
        if(ctx.isOIS) tenor = "1D";
        double rate = rateManager.getZeroRate(ctx.floatingIndexName, ctx.currency, tenor, fixingDate) / 100;
        Cashflow flow = getCashflow(periodStartDate, periodEndDate, ctx, rate);
        flows.add(flow);
      } else {
        FastDate tenorEndDate, tenorStartDate;
        tenorStartDate = calendarManager.adjustDate(periodStartDate, FOLLOWING, IndexProperties.getCenters(ctx.floatingIndexName, ctx.currency));
        if(interval.getPeriod() == PeriodEnum.T || ctx.isOIS) {
          tenorEndDate = periodEndDate;
        } else {
          tenorEndDate = calendarManager.applyInterval(tenorStartDate, interval, MODFOLLOWING, IndexProperties.getCenters(ctx.floatingIndexName, ctx.currency));
        }
        String curve = ctx.isOIS ? "OIS" : ctx.calculationTenor;
        double impliedForwardRate = curveManager.getImpliedForwardRate(tenorStartDate, tenorEndDate, valuationDate, ctx.currency, curve);
        Cashflow flow = getCashflow(periodStartDate, periodEndDate, ctx, impliedForwardRate);
        flows.add(flow);
      }
    }
    //we have all the calculated cashflows, we next need to check the payment dates
    //shortcut case, we have the same schedules
    applyStubs(ctx, flows);
    flows = convertToPaymentFlows(ctx, flows, paymentDates);
    adjustForPaymentOffset(leg, flows);
    for(Cashflow flow : flows) {
      discountFlow(ctx, flow);
      if(ctx.paying) {
        flow.reverse();
      }
    }
    return flows;
  }

  private List<FastDate> getPaymentDates(StreamContext ctx) {
    Interval paymentInterval = ctx.stream.getPaymentDates().getPaymentFrequency();
    FastDate paymentStartDate = ctx.effectiveDate;
    if(ctx.firstPaymentDate != null) {
      paymentStartDate = ctx.firstPaymentDate;
    } else if(ctx.firstRegularPeriodStartDate != null) {
      paymentStartDate = ctx.firstRegularPeriodStartDate;
    }

    List<FastDate> paymentDates = calendarManager.getAdjustedDates(paymentStartDate, ctx.endDate, ctx.conventions, paymentInterval, ctx.calculationCenters, ctx.interval.getRollConvention());
    if(ctx.lastRegularPeriodEndDate != null && ctx.lastRegularPeriodEndDate.lt(ctx.terminationDate)) {
      paymentDates.add(ctx.terminationDate);
    }
    return paymentDates;
  }

  private void applyStubs(StreamContext ctx, List<Cashflow> flows) {
    if(ctx.initialStub != null) {
      FastDate stubCalculationDate = ctx.firstRegularPeriodStartDate;
      if(stubCalculationDate == null) stubCalculationDate = ctx.firstPaymentDate;
      if(stubCalculationDate.gt(ctx.cutoffDate)) {
        Cashflow cashflow = calculateStubCashflow(ctx, ctx.effectiveDate, stubCalculationDate, ctx.initialStub.getFloatingRate());
        flows.set(0, cashflow);
      }
    }
    if(ctx.finalStub != null) {
      FastDate endDate = ctx.terminationDate;
      FastDate startDate = ctx.endDate;
      flows.add(calculateStubCashflow(ctx, startDate, endDate, ctx.finalStub.getFloatingRate()));
    }
  }

  private int getNextDateIndex(StreamContext ctx, List<FastDate> dates) {
    int index = Collections.binarySearch(dates, ctx.cutoffDate.plusDays(1));
    if(index == 0) {
      //we have a trade that starts 'now' effectively, so the first date we calculate is actually the start date
      //and we can move to the next one which will be the first calc
      index = 1;
    }
    else if(index < 0) {
      index = -(index + 1);
      //if index = 0, means we have a calc at the start (which doesn't count, since nothing has happened yet)
      if(index == 0) index = 1;
    }
    return index;
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
      flow.setType(FlowType.FIX);
      flows.add(flow);
    }
    for(Cashflow flow : flows) {
      if(flow.getAmount() == 0) {
        double undiscountedAmount = ctx.notional * flow.getRate() * flow.getDayCountFraction();
        flow.setAmount(undiscountedAmount);
      }
    }
    adjustForPaymentOffset(leg, flows);
    for (Cashflow flow : flows) {
      discountFlow(ctx, flow);
      if(ctx.paying) {
        //we're paying, so reverse values
        flow.reverse();
      }
    }
    return flows;
  }

  private Cashflow calculateStubCashflow(StreamContext ctx, FastDate startDate, FastDate endDate, List<FloatingRate> stubRates) {

    BusinessDayAdjustments adjustments = ctx.stream.getCalculationPeriodDates().getCalculationPeriodDatesAdjustments();
    BusinessCenters centers = adjustments.getBusinessCenters();
    if(adjustments.getBusinessCentersReference() != null) {
      centers = (BusinessCenters)adjustments.getBusinessCentersReference().getHref();
    }
    endDate = calendarManager.adjustDate(endDate, ctx.conventions[1], centers);
    double rate1Value = 0, rate2Value = 0;
    FastDate tenor1End = null, tenor2End = null;
    if(stubRates.size() > 0) {
      FastDate tenorStartDate = calendarManager.adjustDate(startDate, FOLLOWING, ctx.calculationCenters[1]);
      FastDate fixing = calendarManager.getFixingDate(tenorStartDate, ctx.stream.getResetDates().getFixingDates());
      FloatingRate rate1 = stubRates.get(0);
      Interval rate1IndexTenor = rate1.getIndexTenor();
      tenor1End = calendarManager.applyInterval(tenorStartDate, rate1IndexTenor, MODFOLLOWING, IndexProperties.getCenters(ctx.floatingIndexName, ctx.currency));
      if(tenorStartDate.lt(ctx.cutoffDate)) {
        rate1Value = rateManager.getZeroRate(ctx.floatingIndexName, ctx.currency, rate1IndexTenor.getPeriodMultiplier().toString() + rate1IndexTenor.getPeriod() , fixing) / 100;
      } else {
        rate1Value = curveManager.getImpliedForwardRate(tenorStartDate, tenor1End, ctx.valuationDate, ctx.currency, ctx.calculationTenor);
      }
      if(stubRates.size() == 2) {
        FloatingRate rate2 = stubRates.get(1);
        Interval rate2IndexTenor = rate2.getIndexTenor();
        tenor2End = calendarManager.applyInterval(tenorStartDate, rate2IndexTenor, MODFOLLOWING, IndexProperties.getCenters(ctx.floatingIndexName, ctx.currency));
        if(tenorStartDate.lt(ctx.cutoffDate)) {
          rate2Value = rateManager.getZeroRate(ctx.floatingIndexName, ctx.currency, rate2IndexTenor.getPeriodMultiplier().toString() + rate2IndexTenor.getPeriod() , fixing) / 100;
        } else {
          rate2Value = curveManager.getImpliedForwardRate(tenorStartDate, tenor2End, ctx.valuationDate, ctx.currency, ctx.calculationTenor);
        }
      }
    }
    double rateToUse = rate1Value;
    if(stubRates.size() == 2) {
      int periodLength = startDate.numDaysFrom(endDate);
      int rate1Period = startDate.numDaysFrom(tenor1End);
      int rate2Period = startDate.numDaysFrom(tenor2End);
      //note that LCH rounds to 7dp here and we don't, so our stubs will often be a little off (up to 300-400)
      rateToUse = rate1Value + (periodLength - rate1Period) * (rate2Value - rate1Value)/(rate2Period - rate1Period);
    }
    Cashflow flow = getCashflow(startDate, endDate, ctx, rateToUse);
    flow.setAmount(ctx.notional * flow.getRate() * flow.getDayCountFraction());
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
    int size = flows.size();
    for(int i = start; i < paymentDates.size(); i++) {
      Cashflow payment = new Cashflow();
      FastDate paymentDate = paymentDates.get(i);
      payment.setDate(paymentDate);
      double notional = ctx.notional;
      Iterator<Cashflow> iter = flows.iterator();
      List<Cashflow> previousFlows = new ArrayList<Cashflow>(size);
      while(iter.hasNext()) {
        Cashflow flow = iter.next();
        if(flow.getDate().lteq(paymentDate)) {
          if(flow.getAmount() == 0) {
            double rate = flow.getRate();
//            if(ctx.compoundingMethod == STRAIGHT || (ctx.compoundingMethod == FLAT && previousFlows.isEmpty()))
              rate = rate + spread;
            flow.setAmount(notional * rate * flow.getDayCountFraction());
            if(previousFlows.size() > 0 && ctx.compoundingMethod == FLAT) {
              double total = 0;
              for(Cashflow previousFlow : previousFlows) {
                total += previousFlow.getAmount();
              }
              flow.setAmount(flow.getAmount() + (total * flow.getRate() * flow.getDayCountFraction()));
            }
          }
          if(ctx.compounding && ctx.compoundingMethod == STRAIGHT) {
            notional += Math.abs(flow.getAmount());
          }
          payment.setAmount(payment.getAmount() + flow.getAmount());
          if(payment.getType() == null)
            payment.setType(flow.getType());
          previousFlows.add(flow);
          iter.remove();
          size--;
        } else {
          //we have a flow after our current payment, let it go to the next payment date
          break;
        }
      }
      if(payment.getAmount() != 0) {
        paymentFlows.add(payment);
      }
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

  private void adjustForPaymentOffset(InterestRateStream leg, List<Cashflow> flows) {
    Offset paymentOffset = leg.getPaymentDates().getPaymentDaysOffset();
    if(paymentOffset != null) {
      BusinessDayAdjustments paymentDatesAdjustments = leg.getPaymentDates().getPaymentDatesAdjustments();
      BusinessCenters centers = paymentDatesAdjustments.getBusinessCenters();
      if(paymentDatesAdjustments.getBusinessCentersReference() != null) {
        centers = (BusinessCenters)paymentDatesAdjustments.getBusinessCentersReference().getHref();
      }
      for (Cashflow flow : flows) {
        flow.setDate(calendarManager.applyDayInterval(flow.getDate(), paymentOffset, centers));
      }
    }
  }

}
