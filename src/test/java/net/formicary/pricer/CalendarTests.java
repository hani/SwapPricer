package net.formicary.pricer;

import com.google.inject.Guice;
import com.google.inject.Injector;
import net.formicary.pricer.model.DayCountFraction;
import org.fpml.spec503wd3.*;
import org.joda.time.LocalDate;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static org.fpml.spec503wd3.BusinessDayConventionEnum.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * @author hani
 *         Date: 8/10/11
 *         Time: 9:57 AM
 */
@Test
public class CalendarTests {

  private CalendarManager manager;

  private BusinessCenters getCenters(String... vals) {
    BusinessCenters c = new BusinessCenters();
    for (String val : vals) {
      BusinessCenter center = new BusinessCenter();
      center.setId(val);
      center.setValue(val);
      c.getBusinessCenter().add(center);
    }
    return c;
  }

  @BeforeClass
  public void init() {
    Injector injector = Guice.createInjector(new PricerModule());
    manager = injector.getInstance(CalendarManager.class);
  }

  public void weekend() {
    assertEquals(manager.getAdjustedDate(new LocalDate(2011, 8, 13), FOLLOWING, getCenters("USNY")), new LocalDate(2011, 8, 15));
  }

  public void holiday() {
    assertEquals(manager.getAdjustedDate(new LocalDate(2010, 12, 27), FOLLOWING, getCenters("GBLO")), new LocalDate(2010, 12, 29));
  }

  public void notHoliday() {
    assertEquals(manager.getAdjustedDate(new LocalDate(2011, 8, 10), MODFOLLOWING, getCenters("GBLO")), new LocalDate(2011, 8, 10));
  }

  public void dayCountFractionAct360() {
    String f = Double.toString(manager.getDayCountFraction(new LocalDate(2011, 2, 7), new LocalDate(2011, 5, 5), DayCountFraction.ACT_360));
    assertTrue(f.startsWith("0.24166666"), f);
  }

  public void multipleCalendars() {
    assertEquals(manager.getAdjustedDate(new LocalDate(2011, 5, 30), PRECEDING, getCenters("GBLO", "USNY")), new LocalDate(2011, 5, 27));
  }

  public void paymentDates() {
    LocalDate start = new LocalDate(2011, 2, 5);
    LocalDate end = new LocalDate(2012, 2, 5);
    BusinessDayConventionEnum[] conventions = new BusinessDayConventionEnum[]{MODFOLLOWING, MODFOLLOWING, MODFOLLOWING};
    Interval interval = new Interval();
    interval.setPeriod(PeriodEnum.M);
    interval.setPeriodMultiplier(new BigInteger("3"));
    BusinessCenters[] centers = new BusinessCenters[]{getCenters("GBLO"), getCenters("GBLO"), getCenters("GBLO")};
    List<LocalDate> dates = manager.getAdjustedDates(start, end, conventions, interval, centers);
    Iterator<LocalDate> i = dates.iterator();
    assertEquals(i.next(), new LocalDate(2011, 2, 7));
    assertEquals(i.next(), new LocalDate(2011, 5, 5));
    assertEquals(i.next(), new LocalDate(2011, 8, 5));
    assertEquals(i.next(), new LocalDate(2011, 11, 7));
  }

  public void dayCountFractionThirty360() {
    String f = Double.toString(manager.getDayCountFraction(new LocalDate(2011, 2, 7), new LocalDate(2011, 8, 5), DayCountFraction.THIRTY_360));
    assertTrue(f.startsWith("0.49444444444444"), f);
    double d = manager.getDayCountFraction(new LocalDate(2013, 2, 5), new LocalDate(2013, 8, 5), DayCountFraction.THIRTY_360);
    assertEquals(d, 0.5d);
  }

  public void fixingDates() {
    List<LocalDate> dates = Arrays.asList(new LocalDate(2011, 2, 7));
    RelativeDateOffset offset = new RelativeDateOffset();
    offset.setBusinessCenters(getCenters("GBLO"));
    offset.setPeriod(PeriodEnum.D);
    offset.setPeriodMultiplier(new BigInteger("-2"));
    assertEquals(manager.getFixingDates(dates, offset).get(0), new LocalDate(2011, 2, 3));
  }
}
