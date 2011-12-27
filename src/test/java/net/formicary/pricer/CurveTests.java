package net.formicary.pricer;

import net.formicary.pricer.impl.CurveManagerImpl;
import org.fpml.spec503wd3.Interval;
import org.joda.time.LocalDate;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * @author hani
 *         Date: 10/11/11
 *         Time: 11:07 AM
 */
@Test
public class CurveTests {

  private CurveManager manager;

  @BeforeClass
  public void init() throws IOException {
    manager = new CurveManagerImpl();
  }

  public void verifyDiscountMapping() {
    assertEquals(manager.getDiscountCurve("EUR", "1M"), "EUR_EONIA_EOD");
  }

  public void verifyForwardMapping() {
    assertEquals(manager.getForwardCurve("EUR", "OIS"), "EUR_EONIA_EOD");
  }

  public void calculateDiscountRate() {
    double rate = manager.getInterpolatedForwardRate(new LocalDate(2011, 8, 5), "EUR", "OIS");
    assertTrue(Double.toString(rate).startsWith("0.009043552774336"), Double.toString(rate));
  }

  public void calculateDiscountFactor() {
    //fixed rates don't actually care about the interval since the curve mapping is always OIS
    double df = manager.getDiscountFactor(new LocalDate(2011, 8, 5), new LocalDate(2011, 5, 25), "EUR", new Interval(), true);
    assertTrue(Double.toString(df).startsWith("0.9982176565659857"), Double.toString(df));
  }
}
