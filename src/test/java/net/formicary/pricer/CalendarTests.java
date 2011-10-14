package net.formicary.pricer;

import java.util.Arrays;
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
public class CalendarTests {

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

  public void dayCountFractionAct360() {
    String f = Double.toString(manager.getDayCountFraction(new LocalDate(2011, 2, 7), new LocalDate(2011, 5, 5), DayCount.ACT_360));
    assertTrue(f.startsWith("0.24166666"), f);
  }

  public void paymentDates() {
    LocalDate start = new LocalDate(2011, 2, 5);
    LocalDate end = new LocalDate(2012, 2, 5);
    BusinessDayConvention[] conventions = new BusinessDayConvention[]{BusinessDayConvention.MODFOLLOWING, BusinessDayConvention.MODFOLLOWING, BusinessDayConvention.MODFOLLOWING};
    List<LocalDate> dates = manager.getAdjustedDates("GBLO", start, end, conventions, "3M");
    Iterator<LocalDate> i = dates.iterator();
    assertEquals(i.next(), new LocalDate(2011, 2, 7));
    assertEquals(i.next(), new LocalDate(2011, 5, 5));
    assertEquals(i.next(), new LocalDate(2011, 8, 5));
    assertEquals(i.next(), new LocalDate(2011, 11, 7));
  }

  public void dayCountFractionThirty360() {
    String f = Double.toString(manager.getDayCountFraction(new LocalDate(2011, 2, 7), new LocalDate(2011, 8, 5), DayCount.THIRTY_360));
    assertTrue(f.startsWith("0.49444444444444"), f);
    double d = manager.getDayCountFraction(new LocalDate(2013, 2, 5), new LocalDate(2013, 8, 5), DayCount.THIRTY_360);
    assertEquals(d, 0.5d);
  }

  public void fixingDates() {
    List<LocalDate> dates = Arrays.asList(new LocalDate(2011, 2, 7));
    assertEquals(manager.getFixingDates("GBLO", dates, -2).get(0), new LocalDate(2011, 2, 3));
  }
}
