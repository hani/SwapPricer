package net.formicary.pricer.impl.parsers;

import java.util.HashMap;
import java.util.Map;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import net.formicary.pricer.HrefListener;
import net.formicary.pricer.impl.FpmlContext;
import net.formicary.pricer.impl.NodeParser;
import org.fpml.spec503wd3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;

/**
 * @author hsuleiman
 *         Date: 12/20/11
 *         Time: 1:23 PM
 */
public class SwapStreamParser implements NodeParser<InterestRateStream> {

  private static final Logger log = LoggerFactory.getLogger(SwapStreamParser.class);

  enum Element {
    swapStream,
    paymentDates,
    resetDates,
    calculationPeriodStartDates,
    payerPartyReference,
    receiverPartyReference,
    calculationPeriodDatesReference,
    calculationPeriodAmount
  }

  protected Map<String, NodeParser> parsers = new HashMap<String, NodeParser>();

  public SwapStreamParser() {
    parsers.put("calculationPeriodDates", new CalculationPeriodDatesParser());
    parsers.put("calculationPeriodAmount", new CalculationPeriodAmountParser());
    parsers.put("resetDates", new ResetDatesParser());
    parsers.put("paymentDates", new PaymentDatesParser());
    parsers.put("stubCalculationPeriodAmount", new StubParser());
  }

  @Override
  public InterestRateStream parse(XMLStreamReader reader, FpmlContext ctx) throws XMLStreamException {
    final InterestRateStream stream = new InterestRateStream();
    while(reader.hasNext()) {
      int event = reader.next();
      if(event == START_ELEMENT) {
        NodeParser parser = parsers.get(reader.getLocalName());
        if(parser != null) {
          Object entity = parser.parse(reader, ctx);
          if(entity instanceof CalculationPeriodAmount) {
            stream.setCalculationPeriodAmount((CalculationPeriodAmount)entity);
          } else if(entity instanceof CalculationPeriodDates) {
            stream.setCalculationPeriodDates((CalculationPeriodDates)entity);
          } else if(entity instanceof PaymentDates) {
            stream.setPaymentDates((PaymentDates)entity);
          } else if(entity instanceof ResetDates) {
            stream.setResetDates((ResetDates)entity);
          } else if(entity instanceof StubCalculationPeriodAmount) {
            stream.setStubCalculationPeriodAmount((StubCalculationPeriodAmount)entity);
          }
        } else {
          final Element element = Element.valueOf(reader.getLocalName());
          switch(element) {
            case receiverPartyReference:
            case payerPartyReference:
              ctx.addHrefListener(reader.getAttributeValue(null, "href"), new HrefListener() {
                @Override
                public void nodeAdded(String id, Object o) {
                  Party p = (Party)o;
                  PartyOrAccountReference ref = new PartyOrAccountReference();
                  ref.setHref(p);
                  if(element == Element.payerPartyReference) {
                    stream.setPayerPartyReference(ref);
                  } else {
                    stream.setReceiverPartyReference(ref);
                  }
                }
              });
              break;
            default:
              log.warn("No parser for {}" + reader.getLocalName());
          }
        }

      } else if(event == END_ELEMENT) {
        final Element element = Element.valueOf(reader.getLocalName());
        switch(element) {
          case swapStream:
            return stream;
        }
      }
    }
    return null;
  }
}
