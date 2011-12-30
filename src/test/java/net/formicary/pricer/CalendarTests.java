package net.formicary.pricer;

import com.google.inject.Guice;
import com.google.inject.Injector;
import net.formicary.pricer.model.DayCountFraction;
import net.formicary.pricer.util.FpMLUtil;
import org.fpml.spec503wd3.*;
import org.joda.time.LocalDate;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigDecimal;
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
  private static final LocalDate valuationDate = new LocalDate(2011, 11, 4);

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
    assertEquals(manager.adjustDate(new LocalDate(2011, 8, 13), FOLLOWING, getCenters("USNY")), new LocalDate(2011, 8, 15));
  }

  public void verifyEOMConvention() {
    CalculationPeriodFrequency f = new CalculationPeriodFrequency();
    f.setRollConvention("EOM");
    f.setPeriod(PeriodEnum.M);
    f.setPeriodMultiplier(new BigInteger("1"));
    List<LocalDate> dates = manager.getDatesInRange(new LocalDate(2011, 1, 31), new LocalDate(2011, 12, 31), f);
    assertEquals(dates.size(), 12);
    assertEquals(dates.get(1), new LocalDate(2011, 2, 28));
    assertEquals(dates.get(2), new LocalDate(2011, 3, 31));
    assertEquals(dates.get(3), new LocalDate(2011, 4, 30));
  }

  public void verifyIMMConvention() {
    CalculationPeriodFrequency f = new CalculationPeriodFrequency();
    f.setRollConvention("IMM");
    f.setPeriod(PeriodEnum.Y);
    f.setPeriodMultiplier(new BigInteger("1"));
    List<LocalDate> dates = manager.getDatesInRange(new LocalDate(2010, 12, 19), new LocalDate(2022, 12, 21), f);
    assertEquals(dates.get(1), new LocalDate(2011, 12, 21));
    assertEquals(dates.get(2), new LocalDate(2012, 12, 19));
    assertEquals(dates.get(3), new LocalDate(2013, 12, 18));
  }

  public void holiday() {
    assertEquals(manager.adjustDate(new LocalDate(2010, 12, 27), FOLLOWING, getCenters("GBLO")), new LocalDate(2010, 12, 29));
  }

  public void notHoliday() {
    assertEquals(manager.adjustDate(new LocalDate(2011, 8, 10), MODFOLLOWING, getCenters("GBLO")), new LocalDate(2011, 8, 10));
  }

  public void dayCountFractionAct360() {
    String f = Double.toString(manager.getDayCountFraction(new LocalDate(2011, 2, 7), new LocalDate(2011, 5, 5), DayCountFraction.ACT_360));
    assertTrue(f.startsWith("0.24166666"), f);
  }

  public void multipleCalendars() {
    assertEquals(manager.adjustDate(new LocalDate(2011, 5, 30), PRECEDING, getCenters("GBLO", "USNY")), new LocalDate(2011, 5, 27));
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

  public void zeroFixingDateWithPrecedingConvention() {
    //LCH00000931776
    Interval interval = new Interval();
    interval.setPeriod(PeriodEnum.T);
    interval.setPeriodMultiplier(new BigInteger("1"));
    BusinessDayConventionEnum[] conventions = new BusinessDayConventionEnum[]{MODFOLLOWING, MODFOLLOWING, MODFOLLOWING};
    BusinessCenters[] centers = new BusinessCenters[]{getCenters("EUTA"), getCenters("EUTA"), getCenters("EUTA")};
    List<LocalDate> dates = manager.getAdjustedDates(new LocalDate(2011, 6, 1), new LocalDate(2012, 2, 1), conventions, interval, centers);
    assertEquals(dates.size(), 2);
    assertEquals(dates.get(0), new LocalDate(2011, 6, 1));
    //payment offset for this trade actually means payment is on 2/2, but we don't need to worry about that here
    assertEquals(dates.get(1), new LocalDate(2012, 2, 1));
    RelativeDateOffset offset = new RelativeDateOffset();
    offset.setBusinessDayConvention(PRECEDING);
    offset.setPeriod(PeriodEnum.D);
    offset.setPeriodMultiplier(new BigInteger("0"));
    List<LocalDate> fixingDates = manager.getFixingDates(dates, offset);
    assertEquals(fixingDates.get(0), new LocalDate(2011, 6, 1));
    assertEquals(fixingDates.get(1), new LocalDate(2012, 2, 1));
  }

  public void startDateWithIMMAndNoStub() {
    //LCH00001018118
    LocalDate effectiveDate = new LocalDate(2011, 6, 15);
    InterestRateStream fixed = createStream(effectiveDate, new LocalDate(2011, 12, 1), "IMM", 0.066, false);
    LocalDate actual = FpMLUtil.getStartDate(valuationDate, fixed);
    assertEquals(actual, effectiveDate);
  }

  public void noPeriodStartDate() {
    LocalDate effectiveDate = new LocalDate(2011, 6, 1);
    InterestRateStream fixed = createStream(effectiveDate, null, "1", 0.01, false);
    LocalDate actual = FpMLUtil.getStartDate(valuationDate, fixed);
    assertEquals(actual, effectiveDate);
  }

  public void startDateWithoutIMMAndNoStub() {
    //LCH00000923966
    LocalDate effectiveDate = new LocalDate(2010, 10, 8);
    LocalDate periodStart = new LocalDate(2011, 1, 8);
    InterestRateStream fixed = createStream(effectiveDate, periodStart, "8", 0.0085, false);
    LocalDate actual = FpMLUtil.getStartDate(valuationDate, fixed);
    assertEquals(actual, periodStart);
  }

  public void futureStartDateWithoutIMMAndNoStub() {
    //LCH00000927940
    LocalDate effectiveDate = new LocalDate(2019, 11, 11);
    LocalDate periodStart = new LocalDate(2020, 2, 9);
    InterestRateStream fixed = createStream(effectiveDate, periodStart, "9", 0.04446, false);
    LocalDate actual = FpMLUtil.getStartDate(valuationDate, fixed);
    assertEquals(actual, effectiveDate);
  }

  public void startDateWithoutIMMAndStubOnOtherLeg() {
    //LCH00000927688
    LocalDate effectiveDate = new LocalDate(2009, 10, 12);
    LocalDate periodStart = new LocalDate(2010, 1, 15);
    InterestRateStream fixed = createStream(effectiveDate, periodStart, "15", 0.0387, false);
    LocalDate actual = FpMLUtil.getStartDate(valuationDate, fixed);
    assertEquals(actual, periodStart);
  }

  public void startDateWithStub() {
    LocalDate effectiveDate = new LocalDate(2009, 10, 12);
    LocalDate periodStart = new LocalDate(2010, 1, 15);
    InterestRateStream floating = createStream(effectiveDate, periodStart, "15", null, true);
    LocalDate actual = FpMLUtil.getStartDate(valuationDate, floating);
    assertEquals(actual, periodStart);
  }

  private InterestRateStream createStream(LocalDate effectiveDate, LocalDate periodStartDate, String rollConvention, Double fixedRate, boolean withStub) {
    DatatypeFactory factory;
    InterestRateStream s = new InterestRateStream();
    try {
      factory = DatatypeFactory.newInstance();
    } catch (DatatypeConfigurationException e) {
      throw new RuntimeException(e);
    }

    s.setCalculationPeriodDates(new CalculationPeriodDates());
    if(periodStartDate != null) {
      XMLGregorianCalendar cal = factory.newXMLGregorianCalendar(periodStartDate.toString());
      s.getCalculationPeriodDates().setFirstRegularPeriodStartDate(cal);
    }

    if(fixedRate != null) {
      s.setCalculationPeriodAmount(new CalculationPeriodAmount());
      s.getCalculationPeriodAmount().setCalculation(new Calculation());
      s.getCalculationPeriodAmount().getCalculation().setFixedRateSchedule(new Schedule());
      s.getCalculationPeriodAmount().getCalculation().getFixedRateSchedule().setInitialValue(new BigDecimal(fixedRate));
    }
    s.getCalculationPeriodDates().setEffectiveDate(new AdjustableDate());
    s.getCalculationPeriodDates().getEffectiveDate().setUnadjustedDate(new IdentifiedDate());
    s.getCalculationPeriodDates().getEffectiveDate().getUnadjustedDate().setValue(factory.newXMLGregorianCalendar(effectiveDate.toString()));
    s.getCalculationPeriodDates().setCalculationPeriodFrequency(new CalculationPeriodFrequency());
    s.getCalculationPeriodDates().getCalculationPeriodFrequency().setRollConvention(rollConvention);
    if(withStub) {
      s.setStubCalculationPeriodAmount(new StubCalculationPeriodAmount());
    }
    return s;
  }
}
