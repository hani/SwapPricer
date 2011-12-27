package net.formicary.pricer;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.fpml.spec503wd3.Interval;
import org.fpml.spec503wd3.PeriodEnum;
import org.joda.time.LocalDate;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.math.BigInteger;

import static org.testng.Assert.assertEquals;

/**
 * @author hani
 *         Date: 10/14/11
 *         Time: 5:39 PM
 */
@Test
public class RateTests {
  private RateManager manager;

  @BeforeClass
  public void init() {
    Injector injector = Guice.createInjector(new PersistenceModule());
    manager = injector.getInstance(RateManager.class);
  }

  public void lookupRate() {
    Interval interval = new Interval();
    interval.setPeriod(PeriodEnum.M);
    interval.setPeriodMultiplier(new BigInteger("3"));
    double rate = manager.getZeroRate("USD", interval, new LocalDate(2011, 5, 3));
    assertEquals(rate, 0.27225);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void lookupNonExistentRate() {
    Interval interval = new Interval();
    interval.setPeriod(PeriodEnum.M);
    interval.setPeriodMultiplier(new BigInteger("1"));
    manager.getZeroRate("xxx", interval, new LocalDate(2010, 4, 27));
  }
}
