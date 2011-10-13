package net.formicary.pricer;

import java.util.List;

import com.google.inject.Guice;
import com.google.inject.Injector;
import net.formicary.pricer.impl.SimpleTradeStore;
import net.formicary.pricer.model.*;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

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
    swap.setId("LCH00004300325");
    swap.setValuationDate(new LocalDate(2011, 5, 27));
    store.addTrade(swap);

    FixedLeg fixed = new FixedLeg();
    swap.setFixedLeg(fixed);
    fixed.setNotional(44025206);
    fixed.setBusinessCentre("GBLO");
    fixed.setDayCount(DayCount.THIRTY_360);
    fixed.setCurrency("USD");
    fixed.setFixedRate(0.02417610d);
    fixed.setStartBusinessDatConvention(BusinessDayConvention.NONE);
    fixed.setPeriodBusinessDatConvention(BusinessDayConvention.MODFOLLOWING);
    fixed.setEndBusinessDatConvention(BusinessDayConvention.MODFOLLOWING);
    fixed.setPeriodMultiplier("6M");
    fixed.setStartDate(new LocalDate(2009, 2, 5));
    fixed.setEndDate(new LocalDate(2014, 2, 5));

    FloatingLeg floating = new FloatingLeg();
    swap.setFloatingLeg(floating);
    floating.setNotional(44025206);
    floating.setBusinessCentre("GBLO");
    floating.setDayCount(DayCount.THIRTY_360);
    floating.setCurrency("USD");
    floating.setStartBusinessDatConvention(BusinessDayConvention.NONE);
    floating.setPeriodBusinessDatConvention(BusinessDayConvention.MODFOLLOWING);
    floating.setEndBusinessDatConvention(BusinessDayConvention.MODFOLLOWING);
    floating.setPeriodMultiplier("3M");
    floating.setFloatingRateIndex("USD-LIBOR-BBA");
    floating.setRollConvention(5);
    floating.setFixingRelativeToStart(true);
    floating.setFixingDateOffset(-2);
    floating.setStartDate(new LocalDate(2009, 2, 5));
    floating.setEndDate(new LocalDate(2014, 2, 5));

    List<Cashflow> flows = generator.generateCashflows("LCH00004300325");
    log.info("Time to calculate flows: " + (System.currentTimeMillis() - now) + "ms");
    assertEquals(flows.size(), 6, flows.toString());
  }
}
