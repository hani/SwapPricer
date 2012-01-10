package net.formicary.pricer;

import com.google.inject.Guice;
import com.google.inject.Injector;
import net.formicary.pricer.util.FastDate;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

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
    Injector injector = Guice.createInjector(new PersistenceModule("src/test/resources/fpml"));
    manager = injector.getInstance(RateManager.class);
  }

  public void lookupRate() {
    double rate = manager.getZeroRate("LIBOR", "USD", "3M", new FastDate(2011, 5, 3));
    assertEquals(rate, 0.27225);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void lookupNonExistentRate() {
    manager.getZeroRate("LIBOR", "xxx", "1M", new FastDate(2010, 4, 27));
  }
}
