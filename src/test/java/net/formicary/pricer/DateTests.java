package net.formicary.pricer;

import java.util.Iterator;
import java.util.List;

import com.google.inject.Guice;
import com.google.inject.Injector;
import net.formicary.pricer.model.BusinessDayConvention;
import net.formicary.pricer.model.DayCount;
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

  private CalendarManager manager;

  @BeforeClass
  public void init() {
    Injector injector = Guice.createInjector(new PricerModule());
    manager = injector.getInstance(CalendarManager.class);
  }

  public void weekend() {
    assertEquals(manager.getAdjustedDate("USNY", new LocalDate(2011, 8, 13), BusinessDayConvention.FOLLOWING), new LocalDate(2011, 8, 15));
  }

  public void holiday() {
    assertEquals(manager.getAdjustedDate("GBLO", new LocalDate(2010, 12, 27), BusinessDayConvention.FOLLOWING), new LocalDate(2010, 12, 29));
  }

  public void notHoliday() {
    assertEquals(manager.getAdjustedDate("GBLO", new LocalDate(2011, 8, 10), BusinessDayConvention.MODFOLLOWING), new LocalDate(2011, 8, 10));
  }

  public void paymentDates() {
    LocalDate start = new LocalDate(2011, 2, 5);
    LocalDate end = new LocalDate(2012, 2, 5);
    List<LocalDate> dates = manager.getDates("GBLO", start, end, BusinessDayConvention.MODFOLLOWING, "3M");
    Iterator<LocalDate> i = dates.iterator();
    assertEquals(i.next(), new LocalDate(2011, 2, 7));
    assertEquals(i.next(), new LocalDate(2011, 5, 5));
    assertEquals(i.next(), new LocalDate(2011, 8, 5));
    assertEquals(i.next(), new LocalDate(2011, 11, 7));
  }

  public void dayCountFractionThirty360() {
    LocalDate start = new LocalDate(2011, 2, 7);
    LocalDate end = new LocalDate(2011, 8, 5);
    String f = Double.toString(manager.getDayCountFraction(start, end, DayCount.THIRTY_360));
    assertTrue(f.startsWith("0.49444444444444"), f);
  }
}
