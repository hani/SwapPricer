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
    List<LocalDate> dates = calendarManager.getDates(fixed.getBusinessCentre(), fixed.getStartDate(), fixed.getEndDate(), fixed.getBusinessDayConventions(),
      fixed.getPeriodMultiplier());
    List<Cashflow> flows = new ArrayList<Cashflow>();
    return flows;
  }
}
