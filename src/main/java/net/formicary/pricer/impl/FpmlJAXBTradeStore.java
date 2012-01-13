package net.formicary.pricer.impl;

import java.io.File;
import java.io.IOException;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import net.formicary.pricer.TradeStore;
import org.fpml.spec503wd3.DataDocument;
import org.fpml.spec503wd3.Product;
import org.fpml.spec503wd3.Trade;

/**
 * @author hani
 *         Date: 10/11/11
 *         Time: 9:25 PM
 */
@Singleton
public class FpmlJAXBTradeStore implements TradeStore {

  @Inject
  private String fpmlDir;

  private JAXBContext context;

  @Inject
  public FpmlJAXBTradeStore(JAXBContext context) throws JAXBException {
    this.context = context;
  }

  public String getFpmlDir() {
    return fpmlDir;
  }

  @Inject
  public void setFpmlDir(@Named("fpmlDir")String fpmlDir) {
    this.fpmlDir = fpmlDir;
  }

  public Product getTrade(String id) {
    try {
      Trade trade = readFpml(new File(fpmlDir, id + ".xml"));
      return trade.getProduct().getValue();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public Trade readFpml(File f) throws IOException, JAXBException {
    Unmarshaller unmarshaller = context.createUnmarshaller();
    JAXBElement<DataDocument> dd = (JAXBElement<DataDocument>)unmarshaller.unmarshal(f);
    return dd.getValue().getTrade().get(0);
  }
}
