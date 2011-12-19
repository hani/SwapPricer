package net.formicary.pricer;

import com.google.inject.Guice;
import com.google.inject.Injector;
import net.formicary.pricer.impl.SimpleTradeStore;
import net.formicary.pricer.model.*;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;

/**
 * @author hani
 *         Date: 10/11/11
 *         Time: 9:18 PM
 */
@Test
public class CashflowTests {

  private CashflowGenerator generator;
  private SimpleTradeStore store;
  private Logger log = LoggerFactory.getLogger(CashflowTests.class);

  @BeforeClass
  public void init() {
    Injector injector = Guice.createInjector(new PricerModule(), new PersistenceModule());
    store = injector.getInstance(SimpleTradeStore.class);
    generator = injector.getInstance(CashflowGenerator.class);
  }

  public void generateFixedCashflows() {
    long now = System.currentTimeMillis();
    VanillaSwap swap = new VanillaSwap();
    swap.setId("LCH00000997564");
    swap.setValuationDate(new LocalDate(2011, 11, 4));
    store.addTrade(swap);

    FixedLeg fixed = new FixedLeg();
    swap.setFixedLeg(fixed);
    fixed.setNotional(44482393.66);
    fixed.setBusinessCentre("DEFR", "EUTA");
    fixed.setDayCount(DayCount.THIRTY_360);
    fixed.setCurrency("EUR");
    fixed.setFixedRate(0.053525);
    fixed.setStartBusinessDatConvention(BusinessDayConvention.NONE);
    fixed.setPeriodBusinessDatConvention(BusinessDayConvention.MODFOLLOWING);
    fixed.setEndBusinessDatConvention(BusinessDayConvention.MODFOLLOWING);
    fixed.setPeriodMultiplier("1Y");
    fixed.setStartDate(new LocalDate(2004, 6, 5));
    fixed.setEndDate(new LocalDate(2013, 6, 5));

    FloatingLeg floating = new FloatingLeg();
    swap.setFloatingLeg(floating);
    floating.setNotional(44482393.66);
    floating.setBusinessCentre("EUTA");
    floating.setDayCount(DayCount.ACT_360);
    floating.setCurrency("EUR");
    floating.setStartBusinessDatConvention(BusinessDayConvention.NONE);
    floating.setPeriodBusinessDatConvention(BusinessDayConvention.MODFOLLOWING);
    floating.setEndBusinessDatConvention(BusinessDayConvention.MODFOLLOWING);
    floating.setPeriodMultiplier("6M");
    floating.setFloatingRateIndex("USD-LIBOR-BBA");
    floating.setRollConvention(5);
    floating.setFixingRelativeToStart(true);
    floating.setFixingDateOffset(-2);
    floating.setStartDate(new LocalDate(2004, 6, 5));
    floating.setEndDate(new LocalDate(2013, 6, 5));

    List<Cashflow> flows = generator.generateCashflows("LCH00000997564");
    log.info("Time to calculate flows: " + (System.currentTimeMillis() - now) + "ms");
    assertEquals(flows.size(), 6, flows.toString());
  }
}
