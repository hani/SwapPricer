package net.formicary.pricer.impl.parsers;

import java.math.BigInteger;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import net.formicary.pricer.HrefListener;
import net.formicary.pricer.impl.FpmlContext;
import net.formicary.pricer.impl.NodeParser;
import net.formicary.pricer.util.DateUtil;
import org.fpml.spec503wd3.*;

import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;
/**
 * @author hsuleiman
 *         Date: 12/20/11
 *         Time: 2:49 PM
 */
public class PaymentDatesParser implements NodeParser<PaymentDates> {
  private BusinessCentersParser bcParser = new BusinessCentersParser();

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
    paymentDaysOffset,
    dayType,
    firstPaymentDate
  }

  @Override
  public PaymentDates parse(XMLStreamReader reader, FpmlContext ctx) throws XMLStreamException {
    final PaymentDates dates = new PaymentDates();
    final BusinessDayAdjustments adjustments = new BusinessDayAdjustments();
    dates.setPaymentDatesAdjustments(adjustments);
    adjustments.setBusinessCenters(new BusinessCenters());
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
            ctx.addHrefListener(reader.getAttributeValue(null, "href"), new HrefListener() {
              @Override
              public void nodeAdded(String id, Object o) {
                CalculationPeriodDates d = (CalculationPeriodDates) o;
                CalculationPeriodDatesReference ref = new CalculationPeriodDatesReference();
                ref.setHref(d);
                dates.setCalculationPeriodDatesReference(ref);
              }
            });
            break;
          case businessCenters:
            adjustments.setBusinessCenters(bcParser.parse(reader, ctx));
            break;
          case calculationPeriodDates:
            throw new RuntimeException("Not implemented: " + element.name());
          case businessCentersReference:
            ctx.addHrefListener(reader.getAttributeValue(null, "href"), new HrefListener() {
              @Override
              public void nodeAdded(String id, Object o) {
                adjustments.setBusinessCenters((BusinessCenters) o);
              }
            });
            break;
          case paymentDaysOffset:
            dates.setPaymentDaysOffset(new RelativeDateOffset());
            break;
          case payRelativeTo:
            dates.setPayRelativeTo(PayRelativeToEnum.fromValue(reader.getElementText()));
            break;
          case dayType:
            dates.getPaymentDaysOffset().setDayType(DayTypeEnum.fromValue(reader.getElementText()));
            break;
          case firstPaymentDate:
            dates.setFirstPaymentDate(DateUtil.getCalendar(reader.getElementText()));
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
