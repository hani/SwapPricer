package net.formicary.pricer.impl;

import net.formicary.pricer.TradeStore;
import org.fpml.spec503wd3.DataDocument;
import org.fpml.spec503wd3.Swap;
import org.fpml.spec503wd3.Trade;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.IOException;

/**
 * @author hani
 *         Date: 10/11/11
 *         Time: 9:25 PM
 */
@Singleton
public class FpmlJAXBTradeStore implements TradeStore {

  @Inject
  private String fpmlDir;

  private Unmarshaller unmarshaller;

  @Inject
  public FpmlJAXBTradeStore(JAXBContext context) throws JAXBException {
    this.unmarshaller = context.createUnmarshaller();
  }

  public String getFpmlDir() {
    return fpmlDir;
  }

  public void setFpmlDir(String fpmlDir) {
    this.fpmlDir = fpmlDir;
  }

  public Swap getTrade(String id) {
    try {
      Trade trade = readFpml(new File(fpmlDir, id + ".xml"));
      Swap swap = (Swap)trade.getProduct().getValue();
      //swap.setId(id);
      return swap;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public Trade readFpml(File f) throws IOException, JAXBException {
    JAXBElement<DataDocument> dd = (JAXBElement<DataDocument>)unmarshaller.unmarshal(f);
    return dd.getValue().getTrade().get(0);
  }

  public static void main(String[] args) throws JAXBException {
    FpmlJAXBTradeStore store = new FpmlJAXBTradeStore(JAXBContext.newInstance(DataDocument.class));
    store.setFpmlDir("src/test/resources/fpml");
    long now = System.currentTimeMillis();
    for(int i = 0; i < 1000; i++) {
      store.getTrade("LCH00000894736");
    }
    long timeTaken = System.currentTimeMillis() - now;
    System.out.println("Time to read 1000 trades: " + timeTaken + "ms average:" + (timeTaken / 1000) + "ms");
  }
}
