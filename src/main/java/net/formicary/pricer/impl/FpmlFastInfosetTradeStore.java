package net.formicary.pricer.impl;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import javax.inject.Singleton;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import com.sun.xml.fastinfoset.stax.StAXDocumentParser;
import org.fpml.spec503wd3.Product;

/**
 * @author hani
 *         Date: 10/11/11
 *         Time: 9:25 PM
 */
@Singleton
public class FpmlFastInfosetTradeStore extends  FpmlSTAXTradeStore {

  public Product readFpml(File f) throws XMLStreamException, IOException {
    FpmlContext ctx = new FpmlContext();
    //most fast infoset files are around 4k
    BufferedInputStream is = new BufferedInputStream(new FileInputStream(f), 6000);
    XMLStreamReader reader = new StAXDocumentParser(is);
    Product p = getProduct(ctx, reader);
    is.close();
    return p;
  }

  public static void main(String[] args) {
    FpmlFastInfosetTradeStore store = new FpmlFastInfosetTradeStore();
    store.setFpmlDir("/hani/eurfpml-fastinfoset");
    long now = System.currentTimeMillis();
    String[] list = new File(store.getFpmlDir()).list();
    for (String s : list) {
      store.getTrade(s.substring(0, s.lastIndexOf('.')));
    }
    long timeTaken = System.currentTimeMillis() - now;
    System.out.println("Time to read " + list.length + " trades: " + timeTaken + "ms average:" + (timeTaken / 1000) + "ms");
  }
}
