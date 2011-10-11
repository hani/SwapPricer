package net.formicary.pricer;

import java.io.IOException;

import net.formicary.pricer.impl.CurveManagerImpl;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

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
    assertEquals(manager.getDiscountCurve("GBP", "OIS"), "GBP_SONIA_EOD");
  }
}
