package net.formicary.pricer.impl.parsers;

import net.formicary.pricer.HrefListener;
import net.formicary.pricer.impl.FpmlContext;
import net.formicary.pricer.impl.NodeParser;
import net.formicary.pricer.util.DateUtil;
import org.fpml.spec503wd3.*;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.math.BigInteger;

import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;

/**
 * @author hsuleiman
 *         Date: 12/20/11
 *         Time: 11:44 AM
 */
public class CalculationPeriodDatesParser implements NodeParser<CalculationPeriodDates>, HrefListener {

  private AdjustableDate currentDate;
  private BusinessCenters centers;

  enum Element {
    effectiveDate,
    terminationDate,
    calculationPeriodDatesAdjustments,
    firstRegularPeriodStartDate,
    businessDayConvention,
    businessCentersReference,
    businessCenter,
    unadjustedDate,
    dateAdjustments,
    businessCenters,
    calculationPeriodFrequency,
    periodMultiplier,
    period,
    rollConvention,
    calculationPeriodDates
  }

  @Override
  public void nodeAdded(String id, Object o) {
    //it's only business centers for now
    centers = (BusinessCenters)o;
  }

  @Override
  public CalculationPeriodDates parse(XMLStreamReader reader, FpmlContext ctx) throws XMLStreamException {
    CalculationPeriodDates dates = new CalculationPeriodDates();
    dates.setCalculationPeriodFrequency(new CalculationPeriodFrequency());
    String myId = reader.getAttributeValue(null, "id");
    if(myId != null) {
      ctx.registerObject(myId, dates);
    }
    currentDate = null;
    centers = null;
    while(reader.hasNext()) {
      int event = reader.next();
      if (event == START_ELEMENT) {
        Element element = Element.valueOf(reader.getLocalName());
        switch(element) {
          case effectiveDate:
            currentDate = new AdjustableDate();
            currentDate.setDateAdjustments(new BusinessDayAdjustments());
            dates.setEffectiveDate(currentDate);
            break;
          case terminationDate:
            currentDate = new AdjustableDate();
            currentDate.setDateAdjustments(new BusinessDayAdjustments());
            dates.setTerminationDate(currentDate);
            break;
          case firstRegularPeriodStartDate:
            dates.setFirstRegularPeriodStartDate(DateUtil.getCalendar(reader.getElementText()));
            break;
          case calculationPeriodDatesAdjustments:
            currentDate = null;
            break;
          case unadjustedDate:
            //advance and read the text
            if(currentDate == null) {
              throw new RuntimeException("Parser error: found unadjustedDate outside of effective/terminationDate");
            }
            IdentifiedDate date = new IdentifiedDate();
            date.setValue(DateUtil.getCalendar(reader.getElementText()));
            currentDate.setUnadjustedDate(date);
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
            if(currentDate != null) {
              currentDate.getDateAdjustments().setBusinessDayConvention(convention);
            } else {
              if(dates.getCalculationPeriodDatesAdjustments() == null) {
                dates.setCalculationPeriodDatesAdjustments(new BusinessDayAdjustments());
              }
              dates.getCalculationPeriodDatesAdjustments().setBusinessDayConvention(convention);
            }
            break;
          case businessCenters:
            centers = new BusinessCenters();
            if(currentDate == null) {
              dates.getCalculationPeriodDatesAdjustments().setBusinessCenters(centers);
            } else {
              currentDate.getDateAdjustments().setBusinessCenters(centers);
            }
            String id = reader.getAttributeValue(null, "id");
            if(id != null) {
              ctx.registerObject(id, centers);
            }
            break;
          case businessCentersReference:
            ctx.addHrefListener(reader.getAttributeValue(null, "href"), this);
            break;
          case businessCenter:
            BusinessCenter bc = new BusinessCenter();
            bc.setId(reader.getElementText());
            bc.setValue(bc.getId());
            centers.getBusinessCenter().add(bc);
            break;
        }
      } else if(event == END_ELEMENT) {
        Element element = Element.valueOf(reader.getLocalName());
        switch(element) {
          case calculationPeriodDates:
            //we're done
            return dates;
          case businessCentersReference:
            if(currentDate != null) {
              currentDate.getDateAdjustments().setBusinessCenters(centers);
            } else {
              dates.getCalculationPeriodDatesAdjustments().setBusinessCenters(centers);
            }
            break;
        }
      }
    }
    //we should never get here
    return null;
  }
}
