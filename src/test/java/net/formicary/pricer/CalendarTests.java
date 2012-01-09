package net.formicary.pricer;

import com.google.inject.Guice;
import com.google.inject.Injector;
import hirondelle.date4j.DateTime;
import net.formicary.pricer.model.DayCountFraction;
import org.fpml.spec503wd3.*;
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
    assertEquals(manager.adjustDate(DateTime.forDateOnly(2011, 8, 13), FOLLOWING, getCenters("USNY")), DateTime.forDateOnly(2011, 8, 15));
  }

  public void verifyEOMConvention() {
    CalculationPeriodFrequency f = new CalculationPeriodFrequency();
    f.setRollConvention("EOM");
    f.setPeriod(PeriodEnum.M);
    f.setPeriodMultiplier(new BigInteger("1"));
    List<DateTime> dates = manager.getDatesInRange(DateTime.forDateOnly(2011, 1, 31), DateTime.forDateOnly(2012, 1, 1), f, null);
    assertEquals(dates.size(), 12);
    assertEquals(dates.get(1), DateTime.forDateOnly(2011, 2, 28));
    assertEquals(dates.get(2), DateTime.forDateOnly(2011, 3, 31));
    assertEquals(dates.get(3), DateTime.forDateOnly(2011, 4, 30));
  }

  public void verifyIMMConvention() {
    CalculationPeriodFrequency f = new CalculationPeriodFrequency();
    f.setRollConvention("IMM");
    f.setPeriod(PeriodEnum.Y);
    f.setPeriodMultiplier(new BigInteger("1"));
    List<DateTime> dates = manager.getDatesInRange(DateTime.forDateOnly(2010, 12, 19), DateTime.forDateOnly(2022, 12, 21), f, null);
    assertEquals(dates.get(1), DateTime.forDateOnly(2011, 12, 21));
    assertEquals(dates.get(2), DateTime.forDateOnly(2012, 12, 19));
    assertEquals(dates.get(3), DateTime.forDateOnly(2013, 12, 18));
  }

  public void holiday() {
    assertEquals(manager.adjustDate(DateTime.forDateOnly(2010, 12, 27), FOLLOWING, getCenters("GBLO")), DateTime.forDateOnly(2010, 12, 29));
  }

  public void notHoliday() {
    assertEquals(manager.adjustDate(DateTime.forDateOnly(2011, 8, 10), MODFOLLOWING, getCenters("GBLO")), DateTime.forDateOnly(2011, 8, 10));
  }

  public void dayCountFractionAct360() {
    String f = Double.toString(manager.getDayCountFraction(DateTime.forDateOnly(2011, 2, 7), DateTime.forDateOnly(2011, 5, 5), DayCountFraction.ACT_360));
    assertTrue(f.startsWith("0.24166666"), f);
  }

  public void applyIntervalWithModFollowing() {
    DateTime date = DateTime.forDateOnly(2015, 5, 29);
    CalculationPeriodFrequency interval = new CalculationPeriodFrequency();
    interval.setPeriod(PeriodEnum.M);
    interval.setPeriodMultiplier(new BigInteger("3"));
    interval.setRollConvention("29");
    DateTime actual = manager.applyInterval(date, interval, MODFOLLOWING, getCenters("USNY", "GBLO"));
    assertEquals(actual.truncate(DateTime.Unit.DAY), DateTime.forDateOnly(2015, 8, 28));
  }

  public void multipleCalendars() {
    assertEquals(manager.adjustDate(DateTime.forDateOnly(2011, 5, 30), PRECEDING, getCenters("GBLO", "USNY")), DateTime.forDateOnly(2011, 5, 27));
  }

  public void paymentDates() {
    DateTime start = DateTime.forDateOnly(2011, 2, 5);
    DateTime end = DateTime.forDateOnly(2012, 2, 5);
    BusinessDayConventionEnum[] conventions = new BusinessDayConventionEnum[]{MODFOLLOWING, MODFOLLOWING, MODFOLLOWING};
    Interval interval = new Interval();
    interval.setPeriod(PeriodEnum.M);
    interval.setPeriodMultiplier(new BigInteger("3"));
    BusinessCenters[] centers = new BusinessCenters[]{getCenters("GBLO"), getCenters("GBLO"), getCenters("GBLO")};
    List<DateTime> dates = manager.getAdjustedDates(start, end, conventions, interval, centers, "5");
    Iterator<DateTime> i = dates.iterator();
    assertEquals(i.next(), DateTime.forDateOnly(2011, 2, 7));
    assertEquals(i.next(), DateTime.forDateOnly(2011, 5, 5));
    assertEquals(i.next(), DateTime.forDateOnly(2011, 8, 5));
    assertEquals(i.next(), DateTime.forDateOnly(2011, 11, 7));
  }

  public void dayCountFractionThirty360() {
    String f = Double.toString(manager.getDayCountFraction(DateTime.forDateOnly(2011, 2, 7), DateTime.forDateOnly(2011, 8, 5), DayCountFraction.THIRTY_360));
    assertTrue(f.startsWith("0.49444444444444"), f);
    double d = manager.getDayCountFraction(DateTime.forDateOnly(2013, 2, 5), DateTime.forDateOnly(2013, 8, 5), DayCountFraction.THIRTY_360);
    assertEquals(d, 0.5d);
  }

  public void fixingDates() {
    List<DateTime> dates = Arrays.asList(DateTime.forDateOnly(2011, 2, 7));
    RelativeDateOffset offset = new RelativeDateOffset();
    offset.setBusinessCenters(getCenters("GBLO"));
    offset.setPeriod(PeriodEnum.D);
    offset.setPeriodMultiplier(new BigInteger("-2"));
    assertEquals(manager.getFixingDates(dates, offset).get(0), DateTime.forDateOnly(2011, 2, 3));
  }

  public void zeroFixingDateWithPrecedingConvention() {
    //LCH00000931776
    Interval interval = new Interval();
    interval.setPeriod(PeriodEnum.T);
    interval.setPeriodMultiplier(new BigInteger("1"));
    BusinessDayConventionEnum[] conventions = new BusinessDayConventionEnum[]{MODFOLLOWING, MODFOLLOWING, MODFOLLOWING};
    BusinessCenters[] centers = new BusinessCenters[]{getCenters("EUTA"), getCenters("EUTA"), getCenters("EUTA")};
    List<DateTime> dates = manager.getAdjustedDates(DateTime.forDateOnly(2011, 6, 1), DateTime.forDateOnly(2012, 2, 1), conventions, interval, centers, null);
    assertEquals(dates.size(), 2);
    assertEquals(dates.get(0), DateTime.forDateOnly(2011, 6, 1));
    //payment offset for this trade actually means payment is on 2/2, but we don't need to worry about that here
    assertEquals(dates.get(1), DateTime.forDateOnly(2012, 2, 1));
    RelativeDateOffset offset = new RelativeDateOffset();
    offset.setBusinessDayConvention(PRECEDING);
    offset.setPeriod(PeriodEnum.D);
    offset.setPeriodMultiplier(new BigInteger("0"));
    List<DateTime> fixingDates = manager.getFixingDates(dates, offset);
    assertEquals(fixingDates.get(0), DateTime.forDateOnly(2011, 6, 1));
    assertEquals(fixingDates.get(1), DateTime.forDateOnly(2012, 2, 1));
  }
}
