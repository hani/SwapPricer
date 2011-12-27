package net.formicary.pricer;

import net.formicary.pricer.impl.FpmlTradeStore;
import org.cdmckay.coffeedom.input.SAXBuilder;
import org.cdmckay.coffeedom.xpath.XPath;
import org.fpml.spec503wd3.Swap;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author hsuleiman
 *         Date: 12/20/11
 *         Time: 4:07 PM
 */
@Test
public class FpmlParserTests {
  private FpmlTradeStore store;
  private SAXBuilder builder = new SAXBuilder();
  private XPath swapPath = XPath.newInstance("/*[name()='FpML']/*[name()='trade']/*[name()='swap']");
  private XPath unadjustedDate = XPath.newInstance("*[name()='swapStream'][1]/*[name()='calculationPeriodDates']/*[name()='effectiveDate']/*[name()='unadjustedDate']/text()");
  private long now;
  private int count;

  @BeforeClass
  public void init() {
    store = new FpmlTradeStore();
    store.setFpmlDir("src/test/resources/fpml");
    now = System.currentTimeMillis();
  }

  @AfterClass
  public void logTime() {
    long timeTaken = System.currentTimeMillis() - now;
    System.out.println("Time taken: " + timeTaken + "ms for " + count + " files. Average: " + (timeTaken / count) + "ms");
  }

  @Test(dataProvider = "fpml")
  public void checkFields(String id) throws IOException {
    File file = new File(store.getFpmlDir(), id + ".xml");
    Swap swap = store.getTrade(id);
//    Document doc = builder.build(file);
//    Element swapEl = (Element)swapPath.selectSingleNode(doc);
//    Text date = (Text)unadjustedDate.selectSingleNode(swapEl);
//    assertEquals(swap.getStream1().getCalculationPeriodDates().getEffectiveDate().getUnadjusted().toString(), date.getTextTrim());
  }

  @DataProvider(name = "fpml")
  public Object[][] allTrades() {
    File file = new File(store.getFpmlDir());
    List<String> files = new ArrayList<String>();
    Collections.addAll(files, file.list(new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        return name.startsWith("LCH") && name.endsWith(".xml");
      }
    }));
    count = files.size();
    Object[][] data = new Object[files.size()][];
    int i = 0;
    for (String s : files) {
      data[i++] = new Object[]{s.substring(0, s.lastIndexOf('.'))};
    }
    return data;
  }
}
