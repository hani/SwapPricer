package net.formicary.pricer;

import net.formicary.pricer.model.Cashflow;
import net.formicary.pricer.model.FlowType;
import net.formicary.pricer.util.DateUtil;
import net.formicary.pricer.util.FpMLUtil;
import org.fpml.spec503wd3.*;
import org.joda.time.LocalDate;

import javax.inject.Inject;
import javax.xml.datatype.XMLGregorianCalendar;
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
    flows.addAll(generateFloatingFlows(valuationDate, FpMLUtil.getFloatingStream(swap)));
    Collections.sort(flows);
    return flows;
  }

  private List<Cashflow> generateFloatingFlows(LocalDate valuationDate, InterestRateStream leg) {
    LocalDate startDate = getStartDate(leg);
    LocalDate endDate = getEndDate(leg);
    BusinessDayConventionEnum[] conventions = FpMLUtil.getBusinessDayConventions(leg);
    Calculation calculation = leg.getCalculationPeriodAmount().getCalculation();
    AmountSchedule notional = calculation.getNotionalSchedule().getNotionalStepSchedule();
    String currency = notional.getCurrency().getValue();
    CalculationPeriodFrequency interval = leg.getCalculationPeriodDates().getCalculationPeriodFrequency();
    List<LocalDate> calculationDates = calendarManager.getAdjustedDates(startDate, endDate, conventions, interval, FpMLUtil.getBusinessCenters(leg));
    List<LocalDate> fixingDates = calendarManager.getFixingDates(calculationDates, leg.getResetDates().getFixingDates());
    List<Cashflow> flows = new ArrayList<Cashflow>();
    //todo verify LCH skips the payment thats 3 days after valuation date
    LocalDate cutoff = valuationDate.plusDays(3);
    for(int i = 1; i < calculationDates.size(); i++) {
      LocalDate periodEndDate = calculationDates.get(i);
      //todo optimise and use binarySearch to find the right index
      if(periodEndDate.isAfter(cutoff)) {
        LocalDate periodStartDate = calculationDates.get(i - 1);
        LocalDate fixingDate = fixingDates.get(i -1);
        if(fixingDate.isBefore(valuationDate)) {
          String index = getFloatingIndexName(calculation);
          double rate = rateManager.getZeroRate(index, currency, interval, fixingDate) / 100;
          Cashflow flow = getCashflow(valuationDate, periodStartDate, periodEndDate, leg, rate);
          flows.add(flow);
        } else {
          double impliedForwardRate = curveManager.getImpliedForwardRate(periodStartDate, periodEndDate, valuationDate, currency, interval);
          Cashflow flow = getCashflow(valuationDate, calculationDates.get(i - 1), periodEndDate, leg, impliedForwardRate);
          flows.add(flow);
        }
      }
    }
    //we have all the calculated cashflows, we next need to check the payment dates
    //shortcut case, we have the same schedules
    Interval paymentInterval = leg.getPaymentDates().getPaymentFrequency();
    if(interval.getPeriod() == paymentInterval.getPeriod() && interval.getPeriodMultiplier().equals(paymentInterval.getPeriodMultiplier())) {
      return flows;
    } else {
      List<LocalDate> paymentDates = calendarManager.getAdjustedDates(startDate, endDate, conventions, paymentInterval, FpMLUtil.getBusinessCenters(leg));
      return convertToPaymentFlows(flows, cutoff, paymentDates);
    }
  }

  private List<Cashflow> generateFixedFlows(LocalDate valuationDate, InterestRateStream leg) {
    LocalDate startDate = getStartDate(leg);
    LocalDate endDate = getEndDate(leg);
    BusinessDayConventionEnum[] conventions = FpMLUtil.getBusinessDayConventions(leg);
    //todo clarify: we're using calculation interval rather than payment interval, we could convert but do we ever have mismatched dates for fixed flows?
    CalculationPeriodFrequency interval = leg.getCalculationPeriodDates().getCalculationPeriodFrequency();
    List<LocalDate> calculationDates = calendarManager.getAdjustedDates(startDate, endDate, conventions, interval, FpMLUtil.getBusinessCenters(leg));
    List<Cashflow> flows = new ArrayList<Cashflow>();
    //TODO verify that LCH skips the payment thats 3 days after valuation date
    LocalDate cutoff = valuationDate.plusDays(3);
    for(int i = 1; i < calculationDates.size(); i++) {
      LocalDate paymentDate = calculationDates.get(i);
      if(paymentDate.isAfter(cutoff)) {
        BigDecimal rate = leg.getCalculationPeriodAmount().getCalculation().getFixedRateSchedule().getInitialValue();
        Cashflow flow = getCashflow(valuationDate, calculationDates.get(i - 1), paymentDate, leg, rate.doubleValue());
        flows.add(flow);
      }
    }
    return flows;
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

  private LocalDate getEndDate(InterestRateStream leg) {
    //if we have a stub, then our end is from this date
    XMLGregorianCalendar cal = leg.getCalculationPeriodDates().getLastRegularPeriodEndDate();
    //no stub, just use the termination date
    if(cal == null) cal = leg.getCalculationPeriodDates().getTerminationDate().getUnadjustedDate().getValue();
    return DateUtil.getDate(cal);
  }

  private LocalDate getStartDate(InterestRateStream leg) {
    //if we have a stub, then our start is from this date
    XMLGregorianCalendar cal = leg.getCalculationPeriodDates().getFirstRegularPeriodStartDate();
    //no stub, just use the effective date
    if(cal == null) cal = leg.getCalculationPeriodDates().getEffectiveDate().getUnadjustedDate().getValue();
    return DateUtil.getDate(cal);
  }

  private Cashflow getCashflow(LocalDate valuationDate, LocalDate periodStart, LocalDate periodEnd, InterestRateStream leg, double rate) {
    Cashflow flow = new Cashflow();
    DayCountFraction fraction = leg.getCalculationPeriodAmount().getCalculation().getDayCountFraction();
    double dayCountFraction = calendarManager.getDayCountFraction(periodStart, periodEnd, FpMLUtil.getDayCountFraction(fraction.getValue()));
    flow.setDayCountFraction(dayCountFraction);
    AmountSchedule notional = leg.getCalculationPeriodAmount().getCalculation().getNotionalSchedule().getNotionalStepSchedule();
    String currency = notional.getCurrency().getValue();
    double undiscountedAmount = notional.getInitialValue().doubleValue() * rate * dayCountFraction;
    flow.setAmount(undiscountedAmount);
    boolean isFixed = FpMLUtil.isFixedStream(leg);
    double discountFactor = curveManager.getDiscountFactor(periodEnd, valuationDate, currency, leg.getPaymentDates().getPaymentFrequency(), isFixed);
    flow.setDiscountFactor(discountFactor);
    flow.setNpv(discountFactor * undiscountedAmount);
    flow.setDate(periodEnd);
    flow.setRate(rate);
    flow.setType(isFixed ? FlowType.FIX : FlowType.FLT);
    if(((Party)leg.getPayerPartyReference().getHref()).getId().equals(ourName)) {
      //we're paying, so reverse values
      flow.reverse();
    }
    return flow;
  }

  private String getFloatingIndexName(Calculation calculation) {
    FloatingRateCalculation floatingCalc = (FloatingRateCalculation) calculation.getRateCalculation().getValue();
    String index = floatingCalc.getFloatingRateIndex().getValue();
    int dash = index.indexOf('-') + 1;
    index = index.substring(dash, index.indexOf('-', dash));
    return index;
  }
}
