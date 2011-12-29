package net.formicary.pricer.impl.parsers;

import net.formicary.pricer.HrefListener;
import net.formicary.pricer.impl.FpmlContext;
import net.formicary.pricer.impl.NodeParser;
import org.fpml.spec503wd3.*;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import java.math.BigInteger;

import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;

/**
 * @author hsuleiman
 *         Date: 12/20/11
 *         Time: 5:33 PM
 */
public class StubParser implements NodeParser<StubCalculationPeriodAmount> {
  enum Element {
    stubCalculationPeriodAmount,
    calculationPeriodDatesReference,
    initialStub,
    finalStub,
    floatingRate,
    floatingRateIndex,
    indexTenor,
    periodMultiplier,
    period
  }

  @Override
  public StubCalculationPeriodAmount parse(XMLStreamReader reader, FpmlContext ctx) throws XMLStreamException {
    final StubCalculationPeriodAmount stubAmount = new StubCalculationPeriodAmount();
    final ObjectFactory objectFactory = new ObjectFactory();
    Stub stub = null;
    FloatingRate floatingRate = null;
    while (reader.hasNext()) {
      int event = reader.next();
      if (event == START_ELEMENT) {
        Element element = Element.valueOf(reader.getLocalName());
        switch(element) {
          case calculationPeriodDatesReference:
            ctx.addHrefListener(reader.getAttributeValue(null, "href"), new HrefListener() {
              @Override
              public void nodeAdded(String id, Object o) {
                CalculationPeriodDatesReference ref = new CalculationPeriodDatesReference();
                ref.setHref(o);
                stubAmount.getContent().add(objectFactory.createStubCalculationPeriodAmountCalculationPeriodDatesReference(ref));
              }
            });
            break;
          case initialStub:
            //todo this is floating stub, need to handle fixed stub (create a StubValue)
            stub = new Stub();
            stubAmount.getContent().add(objectFactory.createStubCalculationPeriodAmountInitialStub(stub));
            break;
          case finalStub:
            stub = new Stub();
            stubAmount.getContent().add(objectFactory.createStubCalculationPeriodAmountFinalStub(stub));
            break;
          case floatingRate:
            floatingRate = new FloatingRate();
            stub.getFloatingRate().add(floatingRate);
            break;
          case floatingRateIndex:
            FloatingRateIndex idx = new FloatingRateIndex();
            idx.setValue(reader.getElementText());
            floatingRate.setFloatingRateIndex(idx);
            break;
          case indexTenor:
            floatingRate.setIndexTenor(new Interval());
            break;
          case periodMultiplier:
            floatingRate.getIndexTenor().setPeriodMultiplier(new BigInteger(reader.getElementText()));
            break;
          case period :
            floatingRate.getIndexTenor().setPeriod(PeriodEnum.fromValue(reader.getElementText()));
        }
      } else if (event == END_ELEMENT) {
        Element element = Element.valueOf(reader.getLocalName());
        switch (element) {
          case stubCalculationPeriodAmount:
            return stubAmount;
        }
      }
    }
    return null;
  }
}
