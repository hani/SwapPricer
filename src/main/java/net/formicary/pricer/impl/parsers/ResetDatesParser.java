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
    final ResetDates dates = new ResetDates();
    final BusinessDayAdjustments adjustments = new BusinessDayAdjustments();
    adjustments.setBusinessCenters(new BusinessCenters());
    dates.setResetDatesAdjustments(adjustments);
    dates.setFixingDates(new RelativeDateOffset());
    dates.setResetFrequency(new ResetFrequency());
    RelativeDateOffset fixingDates = dates.getFixingDates();
    String period = null;
    BigInteger multiplier = null;
    BusinessCenters centers = null;
    while(reader.hasNext()) {
      int event = reader.next();
      if(event == START_ELEMENT) {
        Element element = Element.valueOf(reader.getLocalName());
        switch(element) {
          case calculationPeriodDatesReference:
            ctx.addHrefListener(reader.getAttributeValue(null, "href"), new HrefListener() {
              @Override
              public void nodeAdded(String id, Object o) {
                CalculationPeriodDates item = (CalculationPeriodDates) o;
                CalculationPeriodDatesReference ref = new CalculationPeriodDatesReference();
                ref.setHref(item);
                dates.setCalculationPeriodDatesReference(ref);
              }
            });
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
          case businessCenters:
            centers = new BusinessCenters();
            break;
          case businessCenter:
            BusinessCenter bc = new BusinessCenter();
            bc.setId(reader.getElementText());
            bc.setValue(bc.getId());
            centers.getBusinessCenter().add(bc);
            break;
          case businessCentersReference:
            ctx.addHrefListener(reader.getAttributeValue(null, "href"), new HrefListener() {
              @Override
              public void nodeAdded(String id, Object o) {
                BusinessCenters b = (BusinessCenters)o;
                adjustments.setBusinessCenters(b);
              }
            });
            break;
          case dayType:
            fixingDates.setDayType(DayTypeEnum.fromValue(reader.getElementText()));
            break;
        }
      } else if(event == END_ELEMENT) {
        Element element = Element.valueOf(reader.getLocalName());
        switch(element) {
          case resetDates:
            return dates;
          case resetDatesAdjustments:
            if(centers != null) {
              adjustments.setBusinessCenters(centers);
              centers = null;
            }
            break;
          case fixingDates:
            if(centers != null) {
              fixingDates.setBusinessCenters(centers);
              centers = null;
            }
            fixingDates.setPeriod(PeriodEnum.valueOf(period));
            fixingDates.setPeriodMultiplier(multiplier);
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
