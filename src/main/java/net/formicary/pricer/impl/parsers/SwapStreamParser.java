package net.formicary.pricer.impl.parsers;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import net.formicary.pricer.HrefListener;
import net.formicary.pricer.impl.FpmlContext;
import net.formicary.pricer.impl.NodeParser;
import org.fpml.spec503wd3.InterestRateStream;
import org.fpml.spec503wd3.Party;
import org.fpml.spec503wd3.PartyOrAccountReference;
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
    calculationPeriodAmount,
    calculationPeriodDates,
    calculationPeriodDatesReference,
    stubCalculationPeriodAmount
  }

  private final CalculationPeriodDatesParser calculationPeriodDatesParser = new CalculationPeriodDatesParser();
  private final CalculationPeriodAmountParser calculationPeriodAmountParser = new CalculationPeriodAmountParser();
  private final ResetDatesParser resetDatesParser = new ResetDatesParser();
  private final PaymentDatesParser paymentDatesParser = new PaymentDatesParser();
  private final StubParser stubParser = new StubParser();

  @Override
  public InterestRateStream parse(XMLStreamReader reader, FpmlContext ctx) throws XMLStreamException {
    final InterestRateStream stream = new InterestRateStream();
    while(reader.hasNext()) {
      int event = reader.next();
      if(event == START_ELEMENT) {
        final Element element = Element.valueOf(reader.getLocalName());
        switch(element) {
          case calculationPeriodAmount:
            stream.setCalculationPeriodAmount(calculationPeriodAmountParser.parse(reader, ctx));
            break;
          case calculationPeriodDates:
            stream.setCalculationPeriodDates(calculationPeriodDatesParser.parse(reader, ctx));
            break;
          case paymentDates:
            stream.setPaymentDates(paymentDatesParser.parse(reader, ctx));
            break;
          case resetDates:
            stream.setResetDates(resetDatesParser.parse(reader, ctx));
            break;
          case stubCalculationPeriodAmount:
            stream.setStubCalculationPeriodAmount(stubParser.parse(reader, ctx));
            break;
          case payerPartyReference:
          case receiverPartyReference:
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
        }
      } else if(event == END_ELEMENT) {
        if("swapStream".equals(reader.getLocalName())) return stream;
      }
    }
    return null;
  }
}
