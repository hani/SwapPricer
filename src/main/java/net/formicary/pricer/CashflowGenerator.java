package net.formicary.pricer;

import java.util.ArrayList;
import java.util.List;
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
    SwapLeg fixed = swap.getFixedLeg();
    List<LocalDate> dates = calendarManager.getFixedFlowDates(fixed.getBusinessCentre(), fixed.getStartDate(),
      fixed.getEndDate(), fixed.getBusinessDayConventions(), fixed.getPeriodMultiplier());
    List<Cashflow> flows = new ArrayList<Cashflow>();
    for(int i = 0; i < dates.size(); i++) {
      LocalDate start = dates.get(i);
      if(start.isAfter(swap.getValuationDate())) {
        double dayCountFraction = calendarManager.getDayCountFraction(dates.get(i-1), dates.get(i), fixed.getDayCount());
        double undiscountedAmount = fixed.getNotional() * fixed.getFixedRate() * dayCountFraction;
        double discountFactor = curveManager.getDiscountFactor(start, swap.getValuationDate(), fixed.getCurrency());
        double discountedAmount = discountFactor * undiscountedAmount;
        Cashflow flow = new Cashflow();
        flow.setId(id);
        flow.setDate(start);
        flow.setNpv(discountedAmount);
        flows.add(flow);
      }
    }
    return flows;
  }
}
