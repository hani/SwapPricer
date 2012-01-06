package net.formicary.pricer;

import java.io.*;
import java.util.*;
import javax.xml.bind.JAXBException;

import com.google.inject.Guice;
import com.google.inject.Injector;
import net.formicary.pricer.model.Cashflow;
import org.apache.commons.io.IOUtils;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * @author hani
 *         Date: 10/11/11
 *         Time: 9:18 PM
 */
@Test
public class CashflowTests {

  public static final String fpmlDir = "src/test/resources/fpml/";
  private CashflowGenerator generator;
  private static final Logger log = LoggerFactory.getLogger(CashflowTests.class);

  @BeforeClass
  public void init() throws JAXBException {
    Injector injector = Guice.createInjector(new PricerModule(), new PersistenceModule(fpmlDir));
    generator = injector.getInstance(CashflowGenerator.class);
  }

  @Test(dataProvider = "singletrade")
  public void generateCashflows(String id) throws Exception {
    List<Cashflow> actualFlows;
    try {
      actualFlows = generator.generateCashflows(new LocalDate(2011, 11, 4), id);
    } catch(Exception e) {
      log.error("Error generating flows for trade {}", id);
      throw e;
    }
    BufferedReader reader = new BufferedReader(new FileReader(fpmlDir + id + ".csv"));
    List<String> lines = IOUtils.readLines(reader);
    assertEquals(actualFlows.size(), lines.size());
    List<Cashflow> expectedFlows = transform(lines);
    Reconciler rec = new Reconciler(expectedFlows, actualFlows);
    Iterator<FlowComparison> i = rec.getFlowComparisons().iterator();
    StringBuilder errors = new StringBuilder();
    while(i.hasNext()) {
      FlowComparison next = i.next();
      Cashflow actual = next.getMemberFlow();
      Cashflow expected = next.getLchFlow();
      if(actual != null && expected != null) {
        int diff = (int)(actual.getAmount() - expected.getAmount());
        if(diff > 50) {
          errors.append("\nAmount diff: " + diff + " for flow on date " + actual.getDate() + " side: " + actual.getType());
        }
      }
      if(next.hasDateMismatch()) {
        errors.append("Date break: ").append(next);
      }
    }
    assertTrue(errors.length() == 0, "Rec failed for trade " + id + errors);
  }

  @DataProvider(name = "singletrade")
  public Object[][] singleTrade() {
    Object[][] data = new Object[1][];
    data[0] = new Object[]{"LCH00001073244"};
    return data;
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

  private List<Cashflow> transform(List<String> lines) {
    DateTimeFormatter formatter = DateTimeFormat.forPattern("dd/MM/yyyy");
    List<Cashflow> flows = new ArrayList<Cashflow>(lines.size());
    for(String line : lines) {
      String[] items = line.split("\t\t");
      Cashflow flow = new Cashflow();
      flow.setDate(formatter.parseLocalDate(items[0]));
      flow.setNpv(Double.parseDouble(items[1]));
      flow.setDiscountFactor(Double.parseDouble(items[3]));
      flow.setAmount(Double.parseDouble(items[4]));
      flows.add(flow);
    }
    return flows;
  }
}
