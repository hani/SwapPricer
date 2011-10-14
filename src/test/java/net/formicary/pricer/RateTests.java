package net.formicary.pricer;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.joda.time.LocalDate;
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
    Injector injector = Guice.createInjector(new PersistenceModule());
    manager = injector.getInstance(RateManager.class);
  }

  public void lookupRate() {
    double rate = manager.lookup("USD", "LIBOR", "1M", new LocalDate(2010, 4, 27));
    assertEquals(rate, 0.26672);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void lookupNonExistentRate() {
    manager.lookup("xxx", "LIBOR", "1M", new LocalDate(2010, 4, 27));
  }
}
