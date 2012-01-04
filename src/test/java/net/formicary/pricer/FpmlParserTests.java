package net.formicary.pricer;

import net.formicary.pricer.impl.FpmlSTAXTradeStore;
import org.apache.commons.jexl2.Expression;
import org.apache.commons.jexl2.JexlContext;
import org.apache.commons.jexl2.JexlEngine;
import org.apache.commons.jexl2.ObjectContext;
import org.cdmckay.coffeedom.Document;
import org.cdmckay.coffeedom.Namespace;
import org.cdmckay.coffeedom.Text;
import org.cdmckay.coffeedom.input.SAXBuilder;
import org.cdmckay.coffeedom.xpath.XPath;
import org.fpml.spec503wd3.IdentifiedDate;
import org.fpml.spec503wd3.Swap;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.xml.datatype.XMLGregorianCalendar;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.*;

import static org.testng.Assert.assertEquals;

/**
 * @author hsuleiman
 *         Date: 12/20/11
 *         Time: 4:07 PM
 */
@Test
public class FpmlParserTests {
  private FpmlSTAXTradeStore store;
  private SAXBuilder builder = new SAXBuilder();
  private JexlEngine engine = new JexlEngine();

  private Map<String, Pair> expressions = new HashMap<String, Pair>();

  class Pair {
    String objPath;
    XPath xpath;
  }

  @BeforeClass
  public void init() throws IOException {
    Namespace ns = Namespace.getNamespace("c", "http://www.fpml.org/FpML-5/confirmation");
    Properties props = new Properties();
    props.load(getClass().getResourceAsStream("parser.properties"));
    for (Map.Entry<Object, Object> item : props.entrySet()) {
      String key = (String)item.getKey();
      String value = (String)item.getValue();
      int dot = key.indexOf('.');
      String name = key.substring(0, dot);
      Pair p = expressions.get(name);
      if(p == null) {
        p = new Pair();
        expressions.put(name, p);
      }
      String type = key.substring(dot + 1, key.length());
      if(type.equals("xpath")) {
        p.xpath = XPath.newInstance(value);
        p.xpath.addNamespace(ns);
      } else if(type.equals("objPath")) {
        p.objPath = value;
      }
    }

    store = new FpmlSTAXTradeStore();
    store.setFpmlDir("src/test/resources/fpml");
  }

  @Test(dataProvider = "fpml")
  public void checkFields(String id) throws IOException {
    File file = new File(store.getFpmlDir(), id + ".xml");
    Swap swap = store.getTrade(id);
    JexlContext jc = new ObjectContext<Swap>(engine, swap);
    Document doc = builder.build(file);
    for (Map.Entry<String, Pair> entry : expressions.entrySet()) {
      Text el = (Text)entry.getValue().xpath.selectSingleNode(doc);
      Expression e = engine.createExpression(entry.getValue().objPath);
      Object obj = e.evaluate(jc);
      assertEquals(toString(obj), el == null ? null : el.getText(), "Mismatch in evaluating " + entry.getKey() + " in trade " + id);
    }
  }

  private static String toString(Object o) {
    if(o == null) return null;
    if(o instanceof IdentifiedDate) {
      XMLGregorianCalendar cal = ((IdentifiedDate)o).getValue();
      return cal.toString();
    }
    return o.toString();
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
    Object[][] data = new Object[files.size()][];
    int i = 0;
    for (String s : files) {
      data[i++] = new Object[]{s.substring(0, s.lastIndexOf('.'))};
    }
    return data;
  }

  @DataProvider(name = "onefpml")
  public Object[][] singleTrade() {
    Object[][] data = new Object[1][];
    data[0] = new Object[]{"LCH00000894736"};
    return data;
  }
}
