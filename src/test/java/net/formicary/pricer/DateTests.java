package net.formicary.pricer;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.joda.time.LocalDate;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * @author hani
 *         Date: 8/10/11
 *         Time: 9:57 AM
 */
@Test
public class DateTests {

  private MarketDataManager manager;

  @BeforeClass
  public void init() {
    Injector injector = Guice.createInjector(new PricerModule());
    manager = injector.getInstance(MarketDataManager.class);
  }

  public void weekend() {

  }

  public void holiday() {
    assertEquals(manager.getAdjustedDate("GBLO", new LocalDate(2010, 12, 27), BusinessDayConvention.FOLLOWING), new LocalDate(2010, 12, 29));
  }

  public void notHoliday() {
    assertEquals(manager.getAdjustedDate("GBLO", new LocalDate(2011, 8, 10), BusinessDayConvention.MODFOLLOWING), new LocalDate(2011, 8, 10));
  }
}
