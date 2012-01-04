package net.formicary.pricer;

import com.google.inject.Guice;
import com.google.inject.Injector;
import net.formicary.pricer.impl.FpmlSTAXTradeStore;
import net.formicary.pricer.model.Cashflow;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author hani
 *         Date: 10/11/11
 *         Time: 9:18 PM
 */
@Test
public class CashflowTests {

  private CashflowGenerator generator;
  private TradeStore store;
  private static final Logger log = LoggerFactory.getLogger(CashflowTests.class);

  @BeforeClass
  public void init() throws JAXBException {
    Injector injector = Guice.createInjector(new PricerModule(), new PersistenceModule("src/test/resources/fpml"));
    store = injector.getInstance(FpmlSTAXTradeStore.class);
    generator = injector.getInstance(CashflowGenerator.class);
  }

  @Test(dataProvider = "singletrade")
  public void generateFixedCashflows(String id) {
    long now = System.currentTimeMillis();

    List<Cashflow> flows = generator.generateCashflows(new LocalDate(2011, 11, 4), id);
    log.info("Flows for {}: {}", id, flows);
    log.info("Time to calculate flows for trade {}: {}ms", id, System.currentTimeMillis() - now);
    //assertEquals(flows.size(), 6, flows.toString());
  }

  @DataProvider(name = "trades")
  public Object[][] allTrades() {
    File file = new File("src/test/resources/fpml");
    List<String> files = new ArrayList<String>();
    Collections.addAll(files, file.list(new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        return name.startsWith("LCH") && name.endsWith(".xml");
      }
    }));
    Object[][] data = new Object[files.size()][];
    int i = 0;
    for (String s : files) {
      data[i++] = new Object[]{s.substring(0, s.lastIndexOf('.'))};
    }
    return data;
  }

  @DataProvider(name = "singletrade")
  public Object[][] singleTrade() {
    Object[][] data = new Object[1][];
    data[0] = new Object[]{"LCH00001074714"};
    return data;
  }
}
