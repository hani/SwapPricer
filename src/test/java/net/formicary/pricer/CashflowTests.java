package net.formicary.pricer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.google.inject.Guice;
import com.google.inject.Injector;
import net.formicary.pricer.model.Cashflow;
import net.formicary.pricer.util.FastDate;
import org.apache.commons.io.IOUtils;
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
  public void init() {
    Injector injector = Guice.createInjector(new PricerModule(), new PersistenceModule("src/test/resources/fpml"));
    generator = injector.getInstance(CashflowGenerator.class);
  }

  @Test(dataProvider = "trades")
  public void generateCashflows(String id) throws Exception {
    List<Cashflow> actualFlows;
    try {
      actualFlows = generator.generateCashflows(new FastDate(2012, 1, 17), id);
    } catch(Exception e) {
      log.error("Error generating flows for trade {}", id);
      throw e;
    }
    BufferedReader reader = new BufferedReader(new FileReader(fpmlDir + id + ".csv"));
    List<String> lines = IOUtils.readLines(reader);
    List<Cashflow> expectedFlows = transform(lines);
    assertEquals(actualFlows.size(), lines.size());
    Reconciler rec = new Reconciler(expectedFlows, actualFlows);
    Iterator<FlowComparison> i = rec.getFlowComparisons().iterator();
    StringBuilder errors = new StringBuilder();
    while(i.hasNext()) {
      FlowComparison next = i.next();
      Cashflow actual = next.getMemberFlow();
      Cashflow expected = next.getLchFlow();
      if(actual != null && expected != null) {
        int diff = (int)(actual.getAmount() - expected.getAmount());
        if(Math.abs(diff) > 50) {
          errors.append("\nAmount diff: " + diff + " for flow on date " + actual.getDate() + " side: " + actual.getType());
        } else {
          diff = (int)(actual.getNpv() - expected.getNpv());
          if(Math.abs(diff) > 50) {
            errors.append("\nNPV diff: " + diff + " for flow on date " + actual.getDate() + " side: " + actual.getType());
          }
        }
      }
      if(next.hasDateMismatch()) {
        errors.append("\nDate break: ").append(next);
      }
    }
    assertTrue(errors.length() == 0, "Rec failed for trade " + id + errors);
  }

  @DataProvider(name = "singletrade")
  public Object[][] singleTrade() {
    Object[][] data = new Object[1][];
    data[0] = new Object[]{"LCH00000787894"};
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
    List<Cashflow> flows = new ArrayList<Cashflow>(lines.size());
    for(String line : lines) {
      String[] items = line.split("\t\t");
      Cashflow flow = new Cashflow();
      FastDate dt = new FastDate(Integer.parseInt(items[0].substring(6, 10)), Integer.parseInt(items[0].substring(3, 5)), Integer.parseInt(items[0].substring(0, 2)));
      flow.setDate(dt);
      flow.setNpv(Double.parseDouble(items[1]));
      flow.setDiscountFactor(Double.parseDouble(items[3]));
      flow.setAmount(Double.parseDouble(items[4]));
      flows.add(flow);
    }
    return flows;
  }
}
