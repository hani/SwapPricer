package net.formicary.pricer.impl.parsers;

import java.util.HashMap;
import java.util.Map;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import net.formicary.pricer.impl.FpmlContext;
import net.formicary.pricer.impl.NodeParser;
import org.fpml.spec503wd3.ProductType;
import org.fpml.spec503wd3.Swap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;

/**
 * @author hsuleiman
 *         Date: 1/13/12
 *         Time: 4:00 PM
 */
public class SwapParser implements NodeParser<Swap> {
  private static final Logger log = LoggerFactory.getLogger(SwapParser.class);
  private SwapStreamParser streamParser = new SwapStreamParser();
  protected Map<String, NodeParser> parsers = new HashMap<String, NodeParser>();

  public SwapParser() {
    parsers.put("swapStream", new SwapStreamParser());
  }

  @Override
  public Swap parse(XMLStreamReader reader, FpmlContext ctx) throws XMLStreamException {
    Swap swap = new Swap();
    while(reader.hasNext()) {
      int event = reader.next();
      if(event == START_ELEMENT) {
        String name = reader.getLocalName();
        if(name.equals("swapStream")) {
          swap.getSwapStream().add(streamParser.parse(reader, ctx));
        } else if(name.equals("productType")) {
          ProductType type = new ProductType();
          type.setValue(reader.getElementText());
          swap.getProductType().add(type);
        } else {
          log.warn("No parser found for element {}", name);
        }
      } else if(event == END_ELEMENT) {
        String name = reader.getLocalName();
        if(name.equals("swap")) {
          return swap;
        }
      }
    }
    return null;
  }
}
