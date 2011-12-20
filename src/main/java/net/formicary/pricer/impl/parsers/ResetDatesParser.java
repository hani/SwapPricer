package net.formicary.pricer.impl.parsers;

import net.formicary.pricer.impl.FpmlContext;
import net.formicary.pricer.impl.NodeParser;
import net.formicary.pricer.model.ResetDates;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;

/**
 * @author hsuleiman
 *         Date: 12/20/11
 *         Time: 2:49 PM
 */
public class ResetDatesParser implements NodeParser<ResetDates> {
  enum Element {
    resetDates,
    resetFrequency,
    calculationPeriodDatesReference,
    resetRelativeTo,
    fixingDates,
    periodMultiplier,
    period,
    resetDatesAdjustments,
    dayType,
    businessDayConvention,
    businessCentersReference,
    businessCenters,
    businessCenter,
    dateRelativeTo
  }

  @Override
  public ResetDates parse(XMLStreamReader reader, FpmlContext ctx) throws XMLStreamException {
    ResetDates dates = new ResetDates();
    while(reader.hasNext()) {
      int event = reader.next();
      if(event == START_ELEMENT) {
        Element element = Element.valueOf(reader.getLocalName());
        switch(element) {

        }
      } else if(event == END_ELEMENT) {
        Element element = Element.valueOf(reader.getLocalName());
        switch(element) {
          case resetDates:
            return dates;
        }
      }
    }
    return null;
  }
}
