package net.formicary.pricer;

import net.formicary.pricer.model.Cashflow;
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
    AmountSchedule notional = leg.getCalculationPeriodAmount().getCalculation().getNotionalSchedule().getNotionalStepSchedule();
    String currency = notional.getCurrency().getValue();
    Interval interval = leg.getPaymentDates().getPaymentFrequency();
    List<LocalDate> paymentDates = calendarManager.getAdjustedDates(startDate, endDate, conventions, interval, FpMLUtil.getBusinessCenters(leg));
    List<LocalDate> fixingDates = calendarManager.getFixingDates(paymentDates, leg.getResetDates().getFixingDates());
    List<Cashflow> flows = new ArrayList<Cashflow>();
    for(int i = 1; i < paymentDates.size(); i++) {
      LocalDate periodEndDate = paymentDates.get(i);
      LocalDate periodStartDate = paymentDates.get(i - 1);
      LocalDate fixingDate = fixingDates.get(i -1);
      if(fixingDate.isBefore(valuationDate) && periodEndDate.isAfter(valuationDate)) {
        double rate = rateManager.getZeroRate(currency, interval, fixingDate) / 100;
        double discountedAmount = calculateDiscountedAmount(valuationDate, periodStartDate, periodEndDate, leg, rate);
        flows.add(new Cashflow(discountedAmount, periodEndDate));
      } else if(fixingDate.isAfter(valuationDate)) {
        //future flows, doesn't work yet
        double impliedForwardRate = curveManager.getImpliedForwardRate(periodStartDate, periodEndDate, valuationDate, currency, interval);
        double discountedAmount = calculateDiscountedAmount(valuationDate, paymentDates.get(i - 1), periodEndDate, leg, impliedForwardRate);
        flows.add(new Cashflow(discountedAmount, periodEndDate));
      }
    }
    return flows;
  }

  private List<Cashflow> generateFixedFlows(LocalDate valuationDate, InterestRateStream leg) {
    LocalDate startDate = getStartDate(leg);
    LocalDate endDate = DateUtil.getDate(leg.getCalculationPeriodDates().getTerminationDate().getUnadjustedDate().getValue());
    BusinessDayConventionEnum[] conventions = FpMLUtil.getBusinessDayConventions(leg);
    List<LocalDate> dates = calendarManager.getAdjustedDates(startDate, endDate, conventions, leg.getPaymentDates().getPaymentFrequency(), FpMLUtil.getBusinessCenters(leg));
    List<Cashflow> flows = new ArrayList<Cashflow>();
    for(int i = 1; i < dates.size(); i++) {
      LocalDate start = dates.get(i);
      //TODO verify that LCH skips the payment thats due in between valuation date and next period start
      if(start.isAfter(valuationDate)) {
        BigDecimal rate = leg.getCalculationPeriodAmount().getCalculation().getFixedRateSchedule().getInitialValue();
        double discountedAmount = calculateDiscountedAmount(valuationDate, dates.get(i - 1), dates.get(i), leg, rate.doubleValue());
        flows.add(new Cashflow(discountedAmount, start));
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

  private double calculateDiscountedAmount(LocalDate valuationDate, LocalDate periodStart, LocalDate periodEnd, InterestRateStream leg, double rate) {
    DayCountFraction fraction = leg.getCalculationPeriodAmount().getCalculation().getDayCountFraction();
    double dayCountFraction = calendarManager.getDayCountFraction(periodStart, periodEnd, FpMLUtil.getDayCountFraction(fraction.getValue()));
    AmountSchedule notional = leg.getCalculationPeriodAmount().getCalculation().getNotionalSchedule().getNotionalStepSchedule();
    String currency = notional.getCurrency().getValue();
    double undiscountedAmount = notional.getInitialValue().doubleValue() * rate * dayCountFraction;
    double discountFactor = curveManager.getDiscountFactor(periodEnd, valuationDate, currency, leg.getPaymentDates().getPaymentFrequency(), FpMLUtil.isFixedStream(leg));
    return discountFactor * undiscountedAmount;
  }
}
