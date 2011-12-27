package net.formicary.pricer.impl;

import net.formicary.pricer.TradeStore;
import net.formicary.pricer.impl.parsers.*;
import org.fpml.spec503wd3.Swap;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static javax.xml.stream.XMLStreamConstants.END_DOCUMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;

/**
 * @author hani
 *         Date: 10/11/11
 *         Time: 9:25 PM
 */
@Singleton
public class FpmlTradeStore implements TradeStore {

  private Map<String, NodeParser> parsers = new HashMap<String, NodeParser>();
  private final XMLInputFactory factory;
  @Inject
  private String fpmlDir;

  public FpmlTradeStore() {
    factory = XMLInputFactory.newFactory();
    parsers.put("calculationPeriodDates", new CalculationPeriodDateParser());
    parsers.put("calculationPeriodAmount", new CalculationPeriodAmountParser());
    parsers.put("resetDates", new ResetDatesParser());
    parsers.put("paymentDates", new PaymentDatesParser());
    parsers.put("swapStream", new SwapStreamParser());
    parsers.put("stubCalculationPeriodAmount", new StubParser());
  }

  public String getFpmlDir() {
    return fpmlDir;
  }

  public void setFpmlDir(String fpmlDir) {
    this.fpmlDir = fpmlDir;
  }

  public Swap getTrade(String id) {
    try {
      Swap swap = readFpml(new File(fpmlDir, id + ".xml"));
      swap.setId(id);
      return swap;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public Swap readFpml(File f) throws XMLStreamException, IOException {
    Swap swap = new Swap();
    FpmlContext ctx = new FpmlContext();
    ctx.setParsers(parsers);
    XMLStreamReader reader = factory.createXMLStreamReader(new FileInputStream(f));
    for (int event = reader.next(); event != END_DOCUMENT; event = reader.next()) {
      if (event == START_ELEMENT) {
        NodeParser parser = parsers.get(reader.getLocalName().toLowerCase());
        if(parser != null) {
          parser.parse(reader, ctx);
        }
      }
    }
    swap.getSwapStream().add(ctx.getStream1());
    swap.getSwapStream().add(ctx.getStream2());
    return swap;
  }

  public static void main(String[] args) {
    FpmlTradeStore store = new FpmlTradeStore();
    store.setFpmlDir("src/test/resources/fpml");
    long now = System.currentTimeMillis();
    for(int i = 0; i < 1000; i++) {
      store.getTrade("LCH00004300325");
    }
    long timeTaken = System.currentTimeMillis() - now;
    System.out.println("Time to read 1000 trades: " + timeTaken + "ms average:" + (timeTaken / 1000) + "ms");
  }
}
