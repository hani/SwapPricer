package net.formicary.pricer.impl.parsers;

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
    //TODO need to impl this one
    dateRelativeTo
  }

  @Override
  public ResetDates parse(XMLStreamReader reader, FpmlContext ctx) throws XMLStreamException {
    ResetDates dates = new ResetDates();
    BusinessDayAdjustments adjustments = new BusinessDayAdjustments();
    adjustments.setBusinessCenters(new BusinessCenters());
    dates.setResetDatesAdjustments(adjustments);
    dates.setFixingDates(new RelativeDateOffset());
    dates.setResetFrequency(new ResetFrequency());
    String period = null;
    BigInteger multiplier = null;
    while(reader.hasNext()) {
      int event = reader.next();
      if(event == START_ELEMENT) {
        Element element = Element.valueOf(reader.getLocalName());
        switch(element) {
          case calculationPeriodDatesReference:
            CalculationPeriodDates item = ctx.getCalculationPeriodDates().get(reader.getAttributeValue(null, "href"));
            CalculationPeriodDatesReference ref = new CalculationPeriodDatesReference();
            ref.setHref(item);
            dates.setCalculationPeriodDatesReference(ref);
            break;
          case resetRelativeTo:
            dates.setResetRelativeTo(ResetRelativeToEnum.fromValue(reader.getElementText()));
            break;
          case calculationPeriodDates:
            throw new RuntimeException("Not implemented: " + element.name());
          case businessDayConvention:
            dates.getResetDatesAdjustments().setBusinessDayConvention(BusinessDayConventionEnum.valueOf(reader.getElementText()));
            break;
          case period :
            period = reader.getElementText();
            break;
          case periodMultiplier:
            multiplier = new BigInteger(reader.getElementText());
            break;
          case businessCenter:
            BusinessCenter bc = new BusinessCenter();
            bc.setId(reader.getElementText());
            bc.setValue(bc.getId());
            adjustments.getBusinessCenters().getBusinessCenter().add(bc);
            break;
          case businessCentersReference:
            BusinessCenters bcs = ctx.getBusinessCenters().get(reader.getAttributeValue(null, "href"));
            adjustments.setBusinessCenters(bcs);
            break;
          case dayType:
            dates.getFixingDates().setDayType(DayTypeEnum.fromValue(reader.getElementText()));
            break;
        }
      } else if(event == END_ELEMENT) {
        Element element = Element.valueOf(reader.getLocalName());
        switch(element) {
          case resetDates:
            return dates;
          case fixingDates:
            dates.getFixingDates().setPeriod(PeriodEnum.valueOf(period));
            dates.getFixingDates().setPeriodMultiplier(multiplier);
            break;
          case resetFrequency:
            dates.getResetFrequency().setPeriod(PeriodEnum.valueOf(period));
            dates.getResetFrequency().setPeriodMultiplier(multiplier);
            break;
        }
      }
    }
    return null;
  }
}
