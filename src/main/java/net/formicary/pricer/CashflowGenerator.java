package net.formicary.pricer;

import java.util.*;
import javax.inject.Inject;

import net.formicary.pricer.model.*;
import org.joda.time.LocalDate;

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

  public List<Cashflow> generateCashflows(String id) {
    VanillaSwap swap = tradeStore.getTrade(id);
    FixedLeg fixed = swap.getFixedLeg();
    LocalDate valuationDate = swap.getValuationDate();
    List<Cashflow> flows = generateFixedFlows(valuationDate, fixed);
    flows.addAll(generateFloatingFlows(valuationDate, swap.getFloatingLeg()));
    Collections.sort(flows);
    return flows;
  }

  private List<Cashflow> generateFloatingFlows(LocalDate valuationDate, FloatingLeg leg) {
    List<LocalDate> paymentDates = calendarManager.getAdjustedDates(leg.getBusinessCentre(), leg.getStartDate(),
      leg.getEndDate(), leg.getBusinessDayConventions(), leg.getPeriodMultiplier());
    List<LocalDate> fixingDates = calendarManager.getFixingDates(leg.getBusinessCentre(), paymentDates, leg.getFixingDateOffset());
    List<Cashflow> flows = new ArrayList<Cashflow>();
    for(int i = 0; i < paymentDates.size(); i++) {
      LocalDate paymentDate = paymentDates.get(i);
      LocalDate fixingDate = fixingDates.get(i);
      if(fixingDate.isBefore(valuationDate) && paymentDate.isAfter(valuationDate)) {
        //known floatig flow
        double historicRate = rateManager.lookup(leg.getCurrency(), leg.getFloatingRateIndex(), leg.getPeriodMultiplier(), fixingDate);
        double discountedAmount = calculateDiscountedAmount(valuationDate, paymentDates.get(i-1), paymentDates.get(i), leg, historicRate);
        flows.add(new Cashflow(discountedAmount, paymentDate));
      } else if(fixingDate.isAfter(valuationDate)) {
        Cashflow flow = new Cashflow();
        flow.setDate(paymentDate);
        flows.add(flow);
      }
    }
    return flows;
  }

  private List<Cashflow> generateFixedFlows(LocalDate valuationDate, FixedLeg fixed) {
    List<LocalDate> dates = calendarManager.getAdjustedDates(fixed.getBusinessCentre(), fixed.getStartDate(),
      fixed.getEndDate(), fixed.getBusinessDayConventions(), fixed.getPeriodMultiplier());
    List<Cashflow> flows = new ArrayList<Cashflow>();
    for(int i = 0; i < dates.size(); i++) {
      LocalDate start = dates.get(i);
      if(start.isAfter(valuationDate)) {
        double discountedAmount = calculateDiscountedAmount(valuationDate, dates.get(i-1), dates.get(i), fixed, fixed.getFixedRate());
        flows.add(new Cashflow(discountedAmount, start));
      }
    }
    return flows;
  }

  private double calculateDiscountedAmount(LocalDate valuationDate, LocalDate periodStart, LocalDate periodEnd, SwapLeg leg, double rate) {
    double dayCountFraction = calendarManager.getDayCountFraction(periodStart, periodEnd, leg.getDayCount());
    double undiscountedAmount = leg.getNotional() * rate * dayCountFraction;
    double discountFactor = curveManager.getDiscountFactor(periodEnd, valuationDate, leg.getCurrency());
    return discountFactor * undiscountedAmount;
  }
}
