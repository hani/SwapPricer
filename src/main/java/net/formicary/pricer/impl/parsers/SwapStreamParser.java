package net.formicary.pricer.impl.parsers;

import net.formicary.pricer.impl.FpmlContext;
import net.formicary.pricer.impl.NodeParser;
import net.formicary.pricer.model.CalculationPeriodAmount;
import net.formicary.pricer.model.CalculationPeriodDates;
import net.formicary.pricer.model.SwapStream;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;

/**
 * @author hsuleiman
 *         Date: 12/20/11
 *         Time: 1:23 PM
 */
public class SwapStreamParser implements NodeParser {

  enum Element {
    swapStream,
    paymentDates,
    resetDates,
    calculationPeriodStartDates,
    payerPartyReference,
    receiverPartyReference,
    calculationPeriodAmount
  }

  @Override
  public Object parse(XMLStreamReader reader, FpmlContext ctx) throws XMLStreamException {
    SwapStream stream = new SwapStream();
    while(reader.hasNext()) {
      int event = reader.next();
      if(event == START_ELEMENT) {
        NodeParser parser = ctx.getParsers().get(reader.getLocalName());
        if(parser != null) {
          Object entity = parser.parse(reader, ctx);
          if(entity instanceof CalculationPeriodAmount) {
            stream.setCalculationPeriodAmount((CalculationPeriodAmount)entity);
          } else if(entity instanceof CalculationPeriodDates) {
            stream.setCalculationPeriodDates((CalculationPeriodDates)entity);
          }
        }

      } else if(event == END_ELEMENT) {
        Element element = Element.valueOf(reader.getLocalName());
        switch(element) {
          case swapStream:
            return stream;
        }
      }
    }
    return null;
  }
}
