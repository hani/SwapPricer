package net.formicary.pricer.impl.parsers;

import java.math.BigDecimal;
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
 *         Date: 1/13/12
 *         Time: 12:22 PM
 */
public class FRAParser implements NodeParser<Fra> {
  private BusinessCentersParser bcParser = new BusinessCentersParser();
  private AdjustableDateParser dateParser = new AdjustableDateParser();

  enum Element {
    buyerPartyReference,
    sellerPartyReference,
    adjustedEffectiveDate,
    adjustedTerminationDate,
    paymentDate,
    businessDayConvention,
    fixingDateOffset,
    periodMultiplier,
    period,
    dayType,
    fra,
    businessDayConventions,
    businessCenters,
    dayCountFraction,
    calculationPeriodNumberOfDays,
    fraDiscounting,
    notional,
    currency,
    amount,
    fixedRate,
    floatingRateIndex,
    indexTenor
  }

  @Override
  public Fra parse(XMLStreamReader reader, FpmlContext ctx) throws XMLStreamException {
    final Fra fra = new Fra();
    Interval interval = null;
    while (reader.hasNext()) {
      int event = reader.next();
      if (event == START_ELEMENT) {
        final Element element = Element.valueOf(reader.getLocalName());
        switch (element) {
          case fixingDateOffset:
            RelativeDateOffset offset = new RelativeDateOffset();
            fra.setFixingDateOffset(offset);
            interval = offset;
            break;
          case businessCenters:
            fra.getFixingDateOffset().setBusinessCenters(bcParser.parse(reader, ctx));
            break;
          case buyerPartyReference:
          case sellerPartyReference:
            ctx.addHrefListener(reader.getAttributeValue(null, "href"), new HrefListener() {
              @Override
              public void nodeAdded(String id, Object o) {
                Party p = (Party)o;
                PartyOrTradeSideReference ref = new PartyOrTradeSideReference();
                ref.setHref(p);
                if(element == Element.buyerPartyReference) {
                  fra.setBuyerPartyReference(ref);
                } else {
                  fra.setSellerPartyReference(ref);
                }
              }
            });
            break;
          case adjustedEffectiveDate:
            RequiredIdentifierDate d = new RequiredIdentifierDate();
            d.setValue(DateUtil.getCalendar(reader.getElementText()));
            fra.setAdjustedEffectiveDate(d);
            break;
          case adjustedTerminationDate:
            fra.setAdjustedTerminationDate(DateUtil.getCalendar(reader.getElementText()));
            break;
          case paymentDate:
            fra.setPaymentDate(dateParser.parse(reader, ctx));
            break;
          case businessDayConvention:
            BusinessDayConventionEnum convention = BusinessDayConventionEnum.valueOf(reader.getElementText());
            fra.getFixingDateOffset().setBusinessDayConvention(convention);
            break;
          case periodMultiplier:
            interval.setPeriodMultiplier(new BigInteger(reader.getElementText()));
            break;
          case period:
            interval.setPeriod(PeriodEnum.fromValue(reader.getElementText()));
            break;
          case dayType:
            fra.getFixingDateOffset().setDayType(DayTypeEnum.fromValue(reader.getElementText()));
            break;
          case fraDiscounting:
            fra.setFraDiscounting(FraDiscountingEnum.fromValue(reader.getElementText()));
            break;
          case calculationPeriodNumberOfDays:
            fra.setCalculationPeriodNumberOfDays(new BigInteger(reader.getElementText()));
            break;
          case dayCountFraction:
            DayCountFraction f = new DayCountFraction();
            f.setValue(reader.getElementText());
            fra.setDayCountFraction(f);
            break;
          case fixedRate:
            fra.setFixedRate(new BigDecimal(reader.getElementText()));
            break;
          case floatingRateIndex:
            FloatingRateIndex idx = new FloatingRateIndex();
            idx.setValue(reader.getElementText());
            fra.setFloatingRateIndex(idx);
            break;
          case notional:
            fra.setNotional(new Money());
            break;
          case currency:
            Currency c = new Currency();
            c.setValue(reader.getElementText());
            fra.getNotional().setCurrency(c);
            break;
          case amount:
            fra.getNotional().setAmount(new BigDecimal(reader.getElementText()));
            break;
          case indexTenor:
            interval = new Interval();
            fra.getIndexTenor().add(interval);
            break;
        }
      } else if (event == END_ELEMENT) {
        Element element = Element.valueOf(reader.getLocalName());
        switch (element) {
          case fra:
            return fra;
        }
      }
    }
    return null;
  }
}
