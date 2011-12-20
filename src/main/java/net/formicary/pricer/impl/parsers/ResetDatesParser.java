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
    resetDates,
    resetFrequency,
    calculationPeriodDatesReference,
    calculationPeriodDates,
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
    String period = null;
    int multiplier = 0;
    while(reader.hasNext()) {
      int event = reader.next();
      if(event == START_ELEMENT) {
        Element element = Element.valueOf(reader.getLocalName());
        switch(element) {
          case calculationPeriodDatesReference:
            dates.setCalculationPeriodDates(ctx.getCalculationPeriodDates().get(reader.getAttributeValue(null, "href")));
            break;
          case resetRelativeTo:
            dates.setResetRelativeTo(ResetRelativeTo.valueOf(reader.getElementText()));
            break;
          case calculationPeriodDates:
            throw new RuntimeException("Not implemented: " + element.name());
          case businessDayConvention:
            dates.setBusinessDayConvention(BusinessDayConvention.valueOf(reader.getElementText()));
            break;
          case period :
            period = reader.getElementText();
            break;
          case periodMultiplier:
            multiplier = Integer.parseInt(reader.getElementText());
            break;
          case businessCenter:
            dates.getBusinessCenters().add(reader.getElementText());
            break;
          case businessCentersReference:
            dates.setBusinessCenters(ctx.getBusinessCenters().get(reader.getAttributeValue(null, "href")));
            break;
          case dayType:
            dates.setDayType(DayType.valueOf(reader.getElementText()));
            break;
        }
      } else if(event == END_ELEMENT) {
        Element element = Element.valueOf(reader.getLocalName());
        switch(element) {
          case resetDates:
            return dates;
          case fixingDates:
            dates.setFixingMultiplier(multiplier);
            dates.setFixingPeriod(period);
            break;
          case resetFrequency:
            dates.setResetMultiplier(multiplier);
            dates.setResetPeriod(period);
            break;
        }
      }
    }
    return null;
  }
}
