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
 *         Time: 11:44 AM
 */
public class CalculationPeriodDatesParser implements NodeParser<CalculationPeriodDates> {

  private BusinessCentersParser bcParser = new BusinessCentersParser();
  private AdjustableDateParser dateParser = new AdjustableDateParser();

  enum Element {
    effectiveDate,
    terminationDate,
    calculationPeriodDatesAdjustments,
    businessDayConvention,
    businessCentersReference,
    dateAdjustments,
    businessCenters,
    calculationPeriodFrequency,
    periodMultiplier,
    period,
    rollConvention,
    calculationPeriodDates,
    firstRegularPeriodStartDate,
    lastRegularPeriodEndDate,
  }

  @Override
  public CalculationPeriodDates parse(XMLStreamReader reader, FpmlContext ctx) throws XMLStreamException {
    final CalculationPeriodDates dates = new CalculationPeriodDates();
    dates.setCalculationPeriodFrequency(new CalculationPeriodFrequency());
    String myId = reader.getAttributeValue(null, "id");
    if(myId != null) {
      ctx.registerObject(myId, dates);
    }
    while(reader.hasNext()) {
      int event = reader.next();
      if (event == START_ELEMENT) {
        Element element = Element.valueOf(reader.getLocalName());
        switch(element) {
          case effectiveDate:
            dates.setEffectiveDate(dateParser.parse(reader, ctx));
            break;
          case terminationDate:
            dates.setTerminationDate(dateParser.parse(reader, ctx));
            break;
          case firstRegularPeriodStartDate:
            dates.setFirstRegularPeriodStartDate(DateUtil.getCalendar(reader.getElementText()));
            break;
          case lastRegularPeriodEndDate:
            dates.setLastRegularPeriodEndDate(DateUtil.getCalendar(reader.getElementText()));
            break;
          case calculationPeriodDatesAdjustments:
            dates.setCalculationPeriodDatesAdjustments(new BusinessDayAdjustments());
            break;
          case period:
            dates.getCalculationPeriodFrequency().setPeriod(PeriodEnum.valueOf(reader.getElementText()));
            break;
          case periodMultiplier:
            dates.getCalculationPeriodFrequency().setPeriodMultiplier(new BigInteger(reader.getElementText()));
            break;
          case rollConvention:
            dates.getCalculationPeriodFrequency().setRollConvention(reader.getElementText());
            break;
          case businessDayConvention:
            BusinessDayConventionEnum convention = BusinessDayConventionEnum.valueOf(reader.getElementText());
            dates.getCalculationPeriodDatesAdjustments().setBusinessDayConvention(convention);
            break;
          case businessCenters:
            BusinessCenters bc = bcParser.parse(reader, ctx);
            dates.getCalculationPeriodDatesAdjustments().setBusinessCenters(bc);
            break;
          case businessCentersReference:
            ctx.addHrefListener(reader.getAttributeValue(null, "href"), new HrefListener() {
              @Override
              public void nodeAdded(String id, Object o) {
                BusinessCentersReference ref = new BusinessCentersReference();
                ref.setHref(o);
                dates.getCalculationPeriodDatesAdjustments().setBusinessCentersReference(ref);
              }
            });
            break;
        }
      } else if(event == END_ELEMENT) {
        if("calculationPeriodDates".equals(reader.getLocalName())) {
          return dates;
        }
      }
    }
    //we should never get here
    return null;
  }
}
