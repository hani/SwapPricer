package net.formicary.pricer.impl;

import java.io.*;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import net.formicary.pricer.TradeStore;
import net.formicary.pricer.impl.parsers.FRAParser;
import net.formicary.pricer.impl.parsers.PartyParser;
import net.formicary.pricer.impl.parsers.SwapParser;
import org.fpml.spec503wd3.Product;

import static javax.xml.stream.XMLStreamConstants.END_DOCUMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;

/**
 * @author hani
 *         Date: 10/11/11
 *         Time: 9:25 PM
 */
@Singleton
public class FpmlSTAXTradeStore implements TradeStore {

  private final XMLInputFactory factory;
  private String fpmlDir;
  private final SwapParser swapParser = new SwapParser();
  private final FRAParser fraParser = new FRAParser();
  private final PartyParser partyParser = new PartyParser();

  public FpmlSTAXTradeStore() {
    factory = XMLInputFactory.newFactory();
    factory.setProperty(XMLInputFactory.IS_COALESCING, false);
    factory.setProperty(XMLInputFactory.IS_VALIDATING, false);
  }


  public String getFpmlDir() {
    return fpmlDir;
  }

  @Inject
  public void setFpmlDir(@Named("fpmlDir") String fpmlDir) {
    this.fpmlDir = fpmlDir;
  }

  protected String getExt() {
    return "xml";
  }

  public Product getTrade(String id) {
    try {
      Product trade = readFpml(new File(fpmlDir, id + '.' + getExt()));
      trade.setId(id);
      return trade;
    } catch (Exception e) {
      if(e instanceof RuntimeException) {
        throw (RuntimeException)e;
      } else {
        throw new RuntimeException(e);
      }
    }
  }

  public Product readFpml(File f) throws XMLStreamException, IOException {
    FpmlContext ctx = new FpmlContext();
    //most fpml files are around 8.5k
    BufferedInputStream is = new BufferedInputStream(new FileInputStream(f), 12000);
    XMLStreamReader reader = factory.createXMLStreamReader(is);
    Product p = getProduct(ctx, reader);
    is.close();
    return p;
  }

  protected Product getProduct(FpmlContext ctx, XMLStreamReader reader) throws XMLStreamException {
    Product p = null;
    for (int event = reader.next(); event != END_DOCUMENT; event = reader.next()) {
      if (event == START_ELEMENT) {
        String name = reader.getLocalName();
        if("swap".equals(name)) {
          p = swapParser.parse(reader, ctx);
        } else if("party".equals(name)) {
          partyParser.parse(reader, ctx);
        } else if("fra".equals(name)) {
          p = fraParser.parse(reader, ctx);
        }
      }
    }
    reader.close();
    return p;
  }

  public static void main(String[] args) {
    FpmlSTAXTradeStore store = new FpmlSTAXTradeStore();
    store.setFpmlDir("/hani/eurfpml");
    long now = System.currentTimeMillis();
    String[] list = new File(store.getFpmlDir()).list();
    for (String s : list) {
      store.getTrade(s.substring(0, s.lastIndexOf('.')));
    }
    long timeTaken = System.currentTimeMillis() - now;
    System.out.println("Time to read " + list.length + " trades: " + timeTaken + "ms average:" + (timeTaken / 1000) + "ms");
  }
}
