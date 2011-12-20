package net.formicary.pricer.impl.parsers;

import net.formicary.pricer.impl.FpmlContext;
import net.formicary.pricer.impl.NodeParser;
import net.formicary.pricer.model.BusinessDayConvention;
import net.formicary.pricer.model.DayType;
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
    paymentdates,
    calculationperioddates,
    calculationperioddatesreference,
    paymentfrequency,
    periodmultiplier,
    period,
    payrelativeto,
    paymentdatesadjustments,
    businessdayconvention,
    businesscentersreference,
    businesscenters,
    businesscenter,
    paymentdaysoffset,
    daytype
  }

  @Override
  public PaymentDates parse(XMLStreamReader reader, FpmlContext ctx) throws XMLStreamException {
    PaymentDates dates = new PaymentDates();
    int periodMultiplier = 0;
    String period = null;
    while(reader.hasNext()) {
      int event = reader.next();
      if(event == START_ELEMENT) {
        Element element = Element.valueOf(reader.getLocalName().toLowerCase());
        switch(element) {
          case periodmultiplier:
            periodMultiplier = Integer.parseInt(reader.getElementText());
            break;
          case period :
            period = reader.getElementText();
            break;
          case businessdayconvention:
            dates.setBusinessDayConvention(BusinessDayConvention.valueOf(reader.getElementText()));
            break;
          case calculationperioddatesreference:
            dates.setCalculationPeriodDates(ctx.getCalculationPeriodDates().get(reader.getAttributeValue(null, "href")));
            break;
          case businesscenter:
            dates.getBusinessCenters().add(reader.getElementText());
            break;
          case calculationperioddates:
            throw new RuntimeException("Not implemented: " + element.name());
          case businesscentersreference:
            dates.setBusinessCenters(ctx.getBusinessCenters().get(reader.getAttributeValue(null, "href")));
            break;
          case payrelativeto:
            dates.setPayRelativeTo(PayRelativeTo.valueOf(reader.getElementText()));
            break;
          case daytype:
            dates.setDayType(DayType.valueOf(reader.getElementText()));
            break;
        }
      } else if(event == END_ELEMENT) {
        Element element = Element.valueOf(reader.getLocalName().toLowerCase());
        switch(element) {
          case paymentfrequency:
            dates.setPaymentFrequencyPeriodMultiplier(periodMultiplier);
            dates.setPaymentFrequencyPeriod(period);
            break;
          case paymentdaysoffset:
            dates.setPaymentDaysOffsetPeriodMultiplier(periodMultiplier);
            dates.setPaymentDaysOffsetPeriod(period);
            break;
          case paymentdates:
            return dates;
        }
      }
    }
    return null;
  }
}
