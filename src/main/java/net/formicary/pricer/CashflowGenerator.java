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
    LocalDate endDate = DateUtil.getDate(leg.getCalculationPeriodDates().getTerminationDate().getUnadjustedDate().getValue());
    BusinessDayConventionEnum[] conventions = FpMLUtil.getBusinessDayConventions(leg);
    Calculation calculation = leg.getCalculationPeriodAmount().getCalculation();
    AmountSchedule notional = calculation.getNotionalSchedule().getNotionalStepSchedule();
    String currency = notional.getCurrency().getValue();
    CalculationPeriodFrequency interval = leg.getCalculationPeriodDates().getCalculationPeriodFrequency();
    List<LocalDate> paymentDates = calendarManager.getAdjustedDates(startDate, endDate, conventions, interval, FpMLUtil.getBusinessCenters(leg));
    List<LocalDate> fixingDates = calendarManager.getFixingDates(paymentDates, leg.getResetDates().getFixingDates());
    List<Cashflow> flows = new ArrayList<Cashflow>();
    for(int i = 1; i < paymentDates.size(); i++) {
      LocalDate periodEndDate = paymentDates.get(i);
      LocalDate periodStartDate = paymentDates.get(i - 1);
      LocalDate fixingDate = fixingDates.get(i -1);
      if(fixingDate.isBefore(valuationDate) && periodEndDate.isAfter(valuationDate)) {
        FloatingRateCalculation floatingCalc = (FloatingRateCalculation) calculation.getRateCalculation().getValue();
        String index = floatingCalc.getFloatingRateIndex().getValue();
        index = index.substring(index.indexOf('-') + 1, index.lastIndexOf('-'));
        double rate = rateManager.getZeroRate(index, currency, interval, fixingDate) / 100;
        Cashflow flow = getCashflow(valuationDate, periodStartDate, periodEndDate, leg, rate);
        flows.add(flow);
      } else if(fixingDate.isAfter(valuationDate)) {
        double impliedForwardRate = curveManager.getImpliedForwardRate(periodStartDate, periodEndDate, valuationDate, currency, interval);
        Cashflow flow = getCashflow(valuationDate, paymentDates.get(i - 1), periodEndDate, leg, impliedForwardRate);
        flows.add(flow);
      }
    }
    return flows;
  }

  private List<Cashflow> generateFixedFlows(LocalDate valuationDate, InterestRateStream leg) {
    LocalDate startDate = getStartDate(leg);
    LocalDate endDate = DateUtil.getDate(leg.getCalculationPeriodDates().getTerminationDate().getUnadjustedDate().getValue());
    BusinessDayConventionEnum[] conventions = FpMLUtil.getBusinessDayConventions(leg);
    //todo clarify: we're using calculation interval rather than payment interval, and hoping hoping hoping that they never mismatch or we're screwed
    CalculationPeriodFrequency interval = leg.getCalculationPeriodDates().getCalculationPeriodFrequency();
    List<LocalDate> dates = calendarManager.getAdjustedDates(startDate, endDate, conventions, interval, FpMLUtil.getBusinessCenters(leg));
    List<Cashflow> flows = new ArrayList<Cashflow>();
    for(int i = 1; i < dates.size(); i++) {
      LocalDate paymentDate = dates.get(i);
      //TODO verify that LCH skips the payment thats due in between valuation date and next period start
      if(paymentDate.isAfter(valuationDate)) {
        BigDecimal rate = leg.getCalculationPeriodAmount().getCalculation().getFixedRateSchedule().getInitialValue();
        Cashflow flow = getCashflow(valuationDate, dates.get(i - 1), paymentDate, leg, rate.doubleValue());
        flows.add(flow);
      }
    }
    return flows;
  }

  private LocalDate getStartDate(InterestRateStream leg) {
    XMLGregorianCalendar cal = leg.getCalculationPeriodDates().getFirstRegularPeriodStartDate();
    //if we don't have an explicit start date, use the effective date
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
}
