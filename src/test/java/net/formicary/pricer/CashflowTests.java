package net.formicary.pricer;

import com.google.inject.Guice;
import com.google.inject.Injector;
import net.formicary.pricer.impl.FpmlJAXBTradeStore;
import net.formicary.pricer.model.Cashflow;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.xml.bind.JAXBException;
import java.util.List;

import static org.testng.Assert.assertEquals;

/**
 * @author hani
 *         Date: 10/11/11
 *         Time: 9:18 PM
 */
@Test
public class CashflowTests {

  private CashflowGenerator generator;
  private FpmlJAXBTradeStore store;
  private Logger log = LoggerFactory.getLogger(CashflowTests.class);

  @BeforeClass
  public void init() throws JAXBException {
    Injector injector = Guice.createInjector(new PricerModule(), new PersistenceModule());
    store = injector.getInstance(FpmlJAXBTradeStore.class);
    store.setFpmlDir("src/test/resources/fpml");
    generator = injector.getInstance(CashflowGenerator.class);
  }

  public void generateFixedCashflows() {
    long now = System.currentTimeMillis();

    List<Cashflow> flows = generator.generateCashflows(new LocalDate(2011, 11, 4), "LCH00000997564");
    log.info("Time to calculate flows: " + (System.currentTimeMillis() - now) + "ms");
    assertEquals(flows.size(), 6, flows.toString());
  }
}
