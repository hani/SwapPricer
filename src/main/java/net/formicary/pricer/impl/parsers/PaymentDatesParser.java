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
    businessCenter,
    paymentDaysOffset,
    dayType
  }

  @Override
  public PaymentDates parse(XMLStreamReader reader, FpmlContext ctx) throws XMLStreamException {
    PaymentDates dates = new PaymentDates();
    BusinessDayAdjustments adjustments = new BusinessDayAdjustments();
    dates.setPaymentDatesAdjustments(adjustments);
    adjustments.setBusinessCenters(new BusinessCenters());
    dates.setPaymentDaysOffset(new RelativeDateOffset());
    BigInteger periodMultiplier = null;
    String period = null;
    while(reader.hasNext()) {
      int event = reader.next();
      if(event == START_ELEMENT) {
        Element element = Element.valueOf(reader.getLocalName());
        switch(element) {
          case periodMultiplier:
            periodMultiplier = new BigInteger(reader.getElementText());
            break;
          case period :
            period = reader.getElementText();
            break;
          case businessDayConvention:
            adjustments.setBusinessDayConvention(BusinessDayConventionEnum.valueOf(reader.getElementText()));
            break;
          case calculationPeriodDatesReference:
            CalculationPeriodDates d = ctx.getCalculationPeriodDates().get(reader.getAttributeValue(null, "href"));
            CalculationPeriodDatesReference ref = new CalculationPeriodDatesReference();
            ref.setHref(d);
            dates.setCalculationPeriodDatesReference(ref);
            break;
          case businessCenter:
            BusinessCenter bc = new BusinessCenter();
            bc.setId(reader.getElementText());
            bc.setValue(bc.getId());
            adjustments.getBusinessCenters().getBusinessCenter().add(bc);
            break;
          case calculationPeriodDates:
            throw new RuntimeException("Not implemented: " + element.name());
          case businessCentersReference:
            adjustments.setBusinessCenters(ctx.getBusinessCenters().get(reader.getAttributeValue(null, "href")));
            break;
          case payRelativeTo:
            dates.setPayRelativeTo(PayRelativeToEnum.fromValue(reader.getElementText()));
            break;
          case dayType:
            dates.getPaymentDaysOffset().setDayType(DayTypeEnum.fromValue(reader.getElementText()));
            break;
        }
      } else if(event == END_ELEMENT) {
        Element element = Element.valueOf(reader.getLocalName());
        switch(element) {
          case paymentFrequency:
            Interval i1 = new Interval();
            i1.setPeriod(PeriodEnum.valueOf(period));
            i1.setPeriodMultiplier(periodMultiplier);
            dates.setPaymentFrequency(i1);
            break;
          case paymentDaysOffset:
            dates.getPaymentDaysOffset().setPeriod(PeriodEnum.valueOf(period));
            dates.getPaymentDaysOffset().setPeriodMultiplier(periodMultiplier);
            break;
          case paymentDates:
            return dates;
        }
      }
    }
    return null;
  }
}
