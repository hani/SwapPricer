package net.formicary.pricer;

import net.formicary.pricer.impl.FpmlTradeStore;
import org.cdmckay.coffeedom.Document;
import org.cdmckay.coffeedom.Element;
import org.cdmckay.coffeedom.Text;
import org.cdmckay.coffeedom.input.SAXBuilder;
import org.cdmckay.coffeedom.xpath.XPath;
import org.fpml.spec503wd3.IdentifiedDate;
import org.fpml.spec503wd3.Swap;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.xml.datatype.XMLGregorianCalendar;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.testng.Assert.assertEquals;

/**
 * @author hsuleiman
 *         Date: 12/20/11
 *         Time: 4:07 PM
 */
@Test
public class FpmlParserTests {
  private FpmlTradeStore store;
  private SAXBuilder builder = new SAXBuilder();
  private XPath stream1Path = XPath.newInstance("/*[name()='FpML']/*[name()='trade']/*[name()='swap']/*[name()='swapStream'][1]");
  private XPath unadjustedDate = XPath.newInstance("*[name()='calculationPeriodDates']/*[name()='effectiveDate']/*[name()='unadjustedDate']/text()");
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
    Document doc = builder.build(file);
    Element streamEl = (Element) stream1Path.selectSingleNode(doc);
    Text date = (Text)unadjustedDate.selectSingleNode(streamEl);
    assertEquals(toString(swap.getSwapStream().get(0).getCalculationPeriodDates().getEffectiveDate().getUnadjustedDate()), date.getTextTrim());
  }

  private static String toString(IdentifiedDate date) {
    XMLGregorianCalendar cal = date.getValue();
    return cal.toString();
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
