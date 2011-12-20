package net.formicary.pricer.impl.parsers;

import net.formicary.pricer.impl.FpmlContext;
import net.formicary.pricer.impl.NodeParser;
import net.formicary.pricer.model.BusinessDayConvention;
import net.formicary.pricer.model.CalculationPeriodDates;
import net.formicary.pricer.model.PeriodDate;
import org.joda.time.LocalDate;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.util.List;

import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;

/**
 * @author hsuleiman
 *         Date: 12/20/11
 *         Time: 11:44 AM
 */
public class CalculationPeriodDateParser implements NodeParser<CalculationPeriodDates> {

  enum Element {
    effectiveDate,
    terminationDate,
    calculationPeriodDatesAdjustments,
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
  public CalculationPeriodDates parse(XMLStreamReader reader, FpmlContext ctx) throws XMLStreamException {
    CalculationPeriodDates dates = new CalculationPeriodDates();
    PeriodDate currentDate = null;
    List<String> centers = null;
    while(reader.hasNext()) {
      int event = reader.next();
      if (event == START_ELEMENT) {
        Element element = Element.valueOf(reader.getLocalName());
        String myId = reader.getAttributeValue(null, "id");
        if(myId != null) {
          ctx.getCalculationPeriodDates().put(myId, dates);
        }
        switch(element) {
          case effectiveDate:
            currentDate = new PeriodDate();
            centers = currentDate.getBusinessCenters();
            dates.setEffectiveDate(currentDate);
            break;
          case terminationDate:
            currentDate = new PeriodDate();
            dates.setTerminationDate(currentDate);
            centers = currentDate.getBusinessCenters();
            break;
          case calculationPeriodDatesAdjustments:
            centers = dates.getPeriodBusinessCenters();
            currentDate = null;
            break;
          case unadjustedDate:
            //advance and read the text
            if(currentDate == null) {
              throw new RuntimeException("Parser error: found unadjustedDate outside of effective/terminationDate");
            }
            currentDate.setUnadjusted(new LocalDate(reader.getElementText()));
            break;
          case period:
            dates.setPeriod(reader.getElementText());
            break;
          case periodMultiplier:
            dates.setPeriodMultiplier(Integer.parseInt(reader.getElementText()));
            break;
          case rollConvention:
            dates.setRollConvention(Integer.parseInt(reader.getElementText()));
            break;
          case businessDayConvention:
            BusinessDayConvention convention = BusinessDayConvention.valueOf(reader.getElementText());
            if(currentDate != null) {
              currentDate.setConvention(convention);
            } else {
              dates.setPeriodConvention(convention);
            }
            break;
          case businessCenters:
            String id = reader.getAttributeValue(null, "id");
            if(id != null) {
              List<String> ref = currentDate == null ? dates.getPeriodBusinessCenters() : currentDate.getBusinessCenters();
              ctx.getBusinessCenters().put(id, ref);
            }
            break;
          case businessCentersReference:
            centers = ctx.getBusinessCenters().get(reader.getAttributeValue(null, "href"));
            break;
          case businessCenter:
            centers.add(reader.getElementText());
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
              currentDate.setBusinessCenters(centers);
            } else {
              dates.setPeriodBusinessCenters(centers);
            }
            break;
        }
      }
    }
    //we should never get here
    return null;
  }
}
