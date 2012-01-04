package net.formicary.pricer.impl;

import com.sun.xml.fastinfoset.stax.StAXDocumentParser;
import org.fpml.spec503wd3.Swap;

import javax.inject.Singleton;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import static javax.xml.stream.XMLStreamConstants.END_DOCUMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;

/**
 * @author hani
 *         Date: 10/11/11
 *         Time: 9:25 PM
 */
@Singleton
public class FpmlFastInfosetTradeStore extends  FpmlSTAXTradeStore {

  public Swap readFpml(File f) throws XMLStreamException, IOException {
    Swap swap = new Swap();
    FpmlContext ctx = new FpmlContext();
    ctx.setParsers(parsers);
    //most fpml files are around 8.5k
    BufferedInputStream is = new BufferedInputStream(new FileInputStream(f), 6000);
    XMLStreamReader reader = new StAXDocumentParser(is);
    for (int event = reader.next(); event != END_DOCUMENT; event = reader.next()) {
      if (event == START_ELEMENT) {
        NodeParser parser = parsers.get(reader.getLocalName());
        if(parser != null) {
          parser.parse(reader, ctx);
        }
      }
    }
    is.close();
    swap.getSwapStream().addAll(ctx.getStreams());
    return swap;
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
