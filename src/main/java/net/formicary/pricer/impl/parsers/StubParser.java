package net.formicary.pricer.impl.parsers;

import net.formicary.pricer.impl.FpmlContext;
import net.formicary.pricer.impl.NodeParser;
import org.fpml.spec503wd3.Stub;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;

/**
 * @author hsuleiman
 *         Date: 12/20/11
 *         Time: 5:33 PM
 */
public class StubParser implements NodeParser<Stub> {
  enum Element {
    stubCalculationPeriodAmount,
    calculationPeriodDatesReference,
    initialStub,
    floatingRate,
    floatingRateIndex,
    indexTenor,
    periodMultiplier,
    period
  }

  @Override
  public Stub parse(XMLStreamReader reader, FpmlContext ctx) throws XMLStreamException {
    Stub stub = new Stub();
    while (reader.hasNext()) {
      int event = reader.next();
      if (event == START_ELEMENT) {
        Element element = Element.valueOf(reader.getLocalName());
      } else if (event == END_ELEMENT) {
        Element element = Element.valueOf(reader.getLocalName());
        switch (element) {
          case stubCalculationPeriodAmount:
            return stub;
        }
      }
    }
    return null;
  }
}
