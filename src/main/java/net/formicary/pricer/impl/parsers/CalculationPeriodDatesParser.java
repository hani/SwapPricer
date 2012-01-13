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

  enum Element {
    effectiveDate,
    terminationDate,
    calculationPeriodDatesAdjustments,
    firstRegularPeriodStartDate,
    lastRegularPeriodEndDate,
    businessDayConvention,
    businessCentersReference,
    unadjustedDate,
    dateAdjustments,
    businessCenters,
    calculationPeriodFrequency,
    periodMultiplier,
    period,
    rollConvention,
    calculationPeriodDates
  }

  private class Holder {
    BusinessCenters centers;
    AdjustableDate currentDate;
  }

  @Override
  public CalculationPeriodDates parse(XMLStreamReader reader, FpmlContext ctx) throws XMLStreamException {
    final CalculationPeriodDates dates = new CalculationPeriodDates();
    dates.setCalculationPeriodFrequency(new CalculationPeriodFrequency());
    String myId = reader.getAttributeValue(null, "id");
    if(myId != null) {
      ctx.registerObject(myId, dates);
    }
    final Holder holder = new Holder();
    while(reader.hasNext()) {
      int event = reader.next();
      if (event == START_ELEMENT) {
        Element element = Element.valueOf(reader.getLocalName());
        switch(element) {
          case effectiveDate:
            holder.currentDate = new AdjustableDate();
            holder.currentDate.setDateAdjustments(new BusinessDayAdjustments());
            dates.setEffectiveDate(holder.currentDate);
            break;
          case terminationDate:
            holder.currentDate = new AdjustableDate();
            holder.currentDate.setDateAdjustments(new BusinessDayAdjustments());
            dates.setTerminationDate(holder.currentDate);
            break;
          case firstRegularPeriodStartDate:
            dates.setFirstRegularPeriodStartDate(DateUtil.getCalendar(reader.getElementText()));
            break;
          case lastRegularPeriodEndDate:
            dates.setLastRegularPeriodEndDate(DateUtil.getCalendar(reader.getElementText()));
            break;
          case calculationPeriodDatesAdjustments:
            holder.currentDate = null;
            break;
          case unadjustedDate:
            //advance and read the text
            if(holder.currentDate == null) {
              throw new RuntimeException("Parser error: found unadjustedDate outside of effective/terminationDate");
            }
            IdentifiedDate date = new IdentifiedDate();
            date.setValue(DateUtil.getCalendar(reader.getElementText()));
            holder.currentDate.setUnadjustedDate(date);
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
            if(holder.currentDate != null) {
              holder.currentDate.getDateAdjustments().setBusinessDayConvention(convention);
            } else {
              if(dates.getCalculationPeriodDatesAdjustments() == null) {
                dates.setCalculationPeriodDatesAdjustments(new BusinessDayAdjustments());
              }
              dates.getCalculationPeriodDatesAdjustments().setBusinessDayConvention(convention);
            }
            break;
          case businessCenters:
            BusinessCenters bc = bcParser.parse(reader, ctx);
            if(holder.currentDate == null) {
              dates.getCalculationPeriodDatesAdjustments().setBusinessCenters(bc);
            } else {
              holder.currentDate.getDateAdjustments().setBusinessCenters(bc);
            }
            break;
          case businessCentersReference:
            ctx.addHrefListener(reader.getAttributeValue(null, "href"), new HrefListener() {
              @Override
              public void nodeAdded(String id, Object o) {
                if(holder.currentDate != null) {
                  holder.currentDate.getDateAdjustments().setBusinessCenters(holder.centers);
                } else {
                  dates.getCalculationPeriodDatesAdjustments().setBusinessCenters(holder.centers);
                }
              }
            });
            break;
        }
      } else if(event == END_ELEMENT) {
        Element element = Element.valueOf(reader.getLocalName());
        switch(element) {
          case calculationPeriodDates:
            //we're done
            return dates;
        }
      }
    }
    //we should never get here
    return null;
  }
}
