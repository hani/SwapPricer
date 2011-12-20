package net.formicary.pricer.impl.parsers;

import net.formicary.pricer.impl.FpmlContext;
import net.formicary.pricer.impl.NodeParser;
import net.formicary.pricer.model.BusinessDayConvention;
import net.formicary.pricer.model.DayType;
import net.formicary.pricer.model.ResetDates;
import net.formicary.pricer.model.ResetRelativeTo;

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
    resetdates,
    resetfrequency,
    calculationperioddatesreference,
    calculationperioddates,
    resetrelativeto,
    fixingdates,
    periodmultiplier,
    period,
    resetdatesadjustments,
    daytype,
    businessdayconvention,
    businesscentersreference,
    businesscenters,
    businesscenter,
    daterelativeto
  }

  @Override
  public ResetDates parse(XMLStreamReader reader, FpmlContext ctx) throws XMLStreamException {
    ResetDates dates = new ResetDates();
    String period = null;
    int multiplier = 0;
    while(reader.hasNext()) {
      int event = reader.next();
      if(event == START_ELEMENT) {
        Element element = Element.valueOf(reader.getLocalName().toLowerCase());
        switch(element) {
          case calculationperioddatesreference:
            dates.setCalculationPeriodDates(ctx.getCalculationPeriodDates().get(reader.getAttributeValue(null, "href")));
            break;
          case resetrelativeto:
            dates.setResetRelativeTo(ResetRelativeTo.valueOf(reader.getElementText()));
            break;
          case calculationperioddates:
            throw new RuntimeException("Not implemented: " + element.name());
          case businessdayconvention:
            dates.setBusinessDayConvention(BusinessDayConvention.valueOf(reader.getElementText()));
            break;
          case period :
            period = reader.getElementText();
            break;
          case periodmultiplier:
            multiplier = Integer.parseInt(reader.getElementText());
            break;
          case businesscenter:
            dates.getBusinessCenters().add(reader.getElementText());
            break;
          case businesscentersreference:
            dates.setBusinessCenters(ctx.getBusinessCenters().get(reader.getAttributeValue(null, "href")));
            break;
          case daytype:
            dates.setDayType(DayType.valueOf(reader.getElementText()));
            break;
        }
      } else if(event == END_ELEMENT) {
        Element element = Element.valueOf(reader.getLocalName().toLowerCase());
        switch(element) {
          case resetdates:
            return dates;
          case fixingdates:
            dates.setFixingMultiplier(multiplier);
            dates.setFixingPeriod(period);
            break;
          case resetfrequency:
            dates.setResetMultiplier(multiplier);
            dates.setResetPeriod(period);
            break;
        }
      }
    }
    return null;
  }
}
