package net.formicary.pricer.impl.parsers;

import net.formicary.pricer.impl.FpmlContext;
import net.formicary.pricer.impl.NodeParser;
import net.formicary.pricer.model.BusinessDayConvention;
import net.formicary.pricer.model.PayRelativeTo;
import net.formicary.pricer.model.PaymentDates;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;
/**
 * @author hsuleiman
 *         Date: 12/20/11
 *         Time: 2:49 PM
 */
public class PaymentDatesParser implements NodeParser<PaymentDates> {
  enum Element {
    paymentDates,
    calculationPeriodDates,
    calculationPeriodDatesReference,
    paymentFrequency,
    periodMultiplier,
    period,
    payRelativeTo,
    paymentDatesAdjustments,
    businessDayConvention,
    businessCentersReference,
    businessCenters,
    businessCenter
  }

  @Override
  public PaymentDates parse(XMLStreamReader reader, FpmlContext ctx) throws XMLStreamException {
    PaymentDates dates = new PaymentDates();
    while(reader.hasNext()) {
      int event = reader.next();
      if(event == START_ELEMENT) {
        Element element = Element.valueOf(reader.getLocalName());
        switch(element) {
          case periodMultiplier:
            dates.setPeriodMultiplier(Integer.parseInt(reader.getElementText()));
            break;
          case period :
            dates.setPeriod(reader.getElementText());
            break;
          case businessDayConvention:
            dates.setBusinessDayConvention(BusinessDayConvention.valueOf(reader.getElementText()));
            break;
          case calculationPeriodDatesReference:
            dates.setCalculationPeriodDates(ctx.getCalculationPeriodDates().get(reader.getAttributeValue(null, "href")));
            break;
          case calculationPeriodDates:
          case businessCenters:
          case businessCenter:
            throw new RuntimeException("Not implemented: " + element.name());
          case businessCentersReference:
            dates.setBusinessCenters(ctx.getBusinessCenters().get(reader.getAttributeValue(null, "href")));
            break;
          case payRelativeTo:
            dates.setPayRelativeTo(PayRelativeTo.valueOf(reader.getElementText()));
        }
      } else if(event == END_ELEMENT) {
        Element element = Element.valueOf(reader.getLocalName());
        switch(element) {
          case paymentDates:
            return dates;
        }
      }
    }
    return null;
  }
}
