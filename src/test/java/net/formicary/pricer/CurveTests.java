package net.formicary.pricer;

import net.formicary.pricer.impl.CurveManagerImpl;
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
    assertEquals(manager.getDiscountCurve("USD", "1M"), "USD_FEDFUND_EOD");
  }

  public void verifyForwardMapping() {
    assertEquals(manager.getForwardCurve("GBP", "OIS"), "GBP_SONIA_EOD");
  }

  public void calculateDiscountRate() {
    double rate = manager.getInterpolatedRate(new LocalDate(2011, 8, 5), "USD", "OIS");
    assertTrue(Double.toString(rate).startsWith("0.00101388"));
  }

  public void calculateDiscountFactor() {
    double df = manager.getDiscountFactor(new LocalDate(2011, 8, 5), new LocalDate(2011, 5, 25), "USD", "OIS");
    assertTrue(Double.toString(df).startsWith("0.9998000"), Double.toString(df));
  }
}
