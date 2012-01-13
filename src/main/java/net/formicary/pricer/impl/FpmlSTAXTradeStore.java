package net.formicary.pricer.impl;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import net.formicary.pricer.TradeStore;
import net.formicary.pricer.impl.parsers.*;
import org.fpml.spec503wd3.Product;
import org.fpml.spec503wd3.Swap;

import static javax.xml.stream.XMLStreamConstants.END_DOCUMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;

/**
 * @author hani
 *         Date: 10/11/11
 *         Time: 9:25 PM
 */
@Singleton
public class FpmlSTAXTradeStore implements TradeStore {

  protected Map<String, NodeParser> parsers = new HashMap<String, NodeParser>();
  private final XMLInputFactory factory;
  private String fpmlDir;

  public FpmlSTAXTradeStore() {
    factory = XMLInputFactory.newFactory();
    factory.setProperty(XMLInputFactory.IS_COALESCING, false);
    factory.setProperty(XMLInputFactory.IS_VALIDATING, false);
    parsers.put("calculationPeriodDates", new CalculationPeriodDatesParser());
    parsers.put("calculationPeriodAmount", new CalculationPeriodAmountParser());
    parsers.put("resetDates", new ResetDatesParser());
    parsers.put("paymentDates", new PaymentDatesParser());
    parsers.put("swapStream", new SwapStreamParser());
    parsers.put("fra", new FRAParser());
    parsers.put("stubCalculationPeriodAmount", new StubParser());
    parsers.put("party", new PartyParser());
  }


  public String getFpmlDir() {
    return fpmlDir;
  }

  @Inject
  public void setFpmlDir(@Named("fpmlDir") String fpmlDir) {
    this.fpmlDir = fpmlDir;
  }

  public Product getTrade(String id) {
    try {
      Product trade = readFpml(new File(fpmlDir, id + ".xml"));
      trade.setId(id);
      return trade;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public Product readFpml(File f) throws XMLStreamException, IOException {
    Swap swap = new Swap();
    FpmlContext ctx = new FpmlContext();
    ctx.setParsers(parsers);
    //most fpml files are around 8.5k
    BufferedInputStream is = new BufferedInputStream(new FileInputStream(f), 12000);
    XMLStreamReader reader = factory.createXMLStreamReader(is);
    for (int event = reader.next(); event != END_DOCUMENT; event = reader.next()) {
      if (event == START_ELEMENT) {
        NodeParser parser = parsers.get(reader.getLocalName());
        if(parser != null) {
          parser.parse(reader, ctx);
        }
      }
    }
    reader.close();
    is.close();
    swap.getSwapStream().addAll(ctx.getStreams());
    return swap;
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
