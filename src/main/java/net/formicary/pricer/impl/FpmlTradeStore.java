package net.formicary.pricer.impl;

import net.formicary.pricer.TradeStore;
import net.formicary.pricer.impl.parsers.*;
import net.formicary.pricer.model.VanillaSwap;

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
  }

  public String getFpmlDir() {
    return fpmlDir;
  }

  public void setFpmlDir(String fpmlDir) {
    this.fpmlDir = fpmlDir;
  }

  @Override
  public VanillaSwap getTrade(String id) {
    try {
      VanillaSwap swap = readFpml(new File(fpmlDir, id + ".xml"));
      swap.setId(id);
      return swap;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public VanillaSwap readFpml(File f) throws XMLStreamException, IOException {
    VanillaSwap swap = new VanillaSwap();
    FpmlContext ctx = new FpmlContext();
    ctx.setParsers(parsers);
    XMLStreamReader reader = factory.createXMLStreamReader(new FileInputStream(f));
    for (int event = reader.next(); event != END_DOCUMENT; event = reader.next()) {
      if (event == START_ELEMENT) {
        NodeParser parser = parsers.get(reader.getLocalName());
        if(parser != null) {
          parser.parse(reader, ctx);
        }
      }
    }
    return swap;
  }

  public static void main(String[] args) {
    FpmlTradeStore store = new FpmlTradeStore();
    store.setFpmlDir("src/test/resources/fpml");
    long now = System.currentTimeMillis();
    for(int i = 0; i < 1000; i++) {
      store.getTrade("LCH00004300325");
    }
    System.out.println("Time to read 1000 trades: " + (System.currentTimeMillis() - now) + "ms");
  }
}
