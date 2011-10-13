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
    List<LocalDate> dates = calendarManager.getAdjustedDates(leg.getBusinessCentre(), leg.getStartDate(),
      leg.getEndDate(), leg.getBusinessDayConventions(), leg.getPeriodMultiplier());
    List<Cashflow> flows = new ArrayList<Cashflow>();
    for(int i = 0; i < dates.size(); i++) {
      LocalDate start = dates.get(i);
      if(start.isAfter(valuationDate)) {
        double dayCountFraction = calendarManager.getDayCountFraction(dates.get(i - 1), dates.get(i), leg.getDayCount());
        double discountFactor = curveManager.getDiscountFactor(start, valuationDate, leg.getCurrency());
        Cashflow flow = new Cashflow();
        flow.setDate(start);
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
        double dayCountFraction = calendarManager.getDayCountFraction(dates.get(i-1), dates.get(i), fixed.getDayCount());
        double undiscountedAmount = fixed.getNotional() * fixed.getFixedRate() * dayCountFraction;
        double discountFactor = curveManager.getDiscountFactor(start, valuationDate, fixed.getCurrency());
        double discountedAmount = discountFactor * undiscountedAmount;
        Cashflow flow = new Cashflow();
        flow.setDate(start);
        flow.setNpv(discountedAmount);
        flows.add(flow);
      }
    }
    return flows;
  }
}
