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
    effectivedate,
    terminationdate,
    calculationperioddatesadjustments,
    firstregularperiodstartdate,
    businessdayconvention,
    businesscentersreference,
    businesscenter,
    unadjusteddate,
    dateadjustments,
    businesscenters,
    calculationperiodfrequency,
    periodmultiplier,
    period,
    rollconvention,
    calculationperioddates
  }

  @Override
  public CalculationPeriodDates parse(XMLStreamReader reader, FpmlContext ctx) throws XMLStreamException {
    CalculationPeriodDates dates = new CalculationPeriodDates();
    PeriodDate currentDate = null;
    List<String> centers = null;
    while(reader.hasNext()) {
      int event = reader.next();
      if (event == START_ELEMENT) {
        Element element = Element.valueOf(reader.getLocalName().toLowerCase());
        String myId = reader.getAttributeValue(null, "id");
        if(myId != null) {
          ctx.getCalculationPeriodDates().put(myId, dates);
        }
        switch(element) {
          case effectivedate:
            currentDate = new PeriodDate();
            dates.setEffectiveDate(currentDate);
            break;
          case terminationdate:
            currentDate = new PeriodDate();
            dates.setTerminationDate(currentDate);
            break;
          case firstregularperiodstartdate:
            dates.setFirstRegularPeriodStartDate(new LocalDate(reader.getElementText()));
            break;
          case calculationperioddatesadjustments:
            currentDate = null;
            break;
          case unadjusteddate:
            //advance and read the text
            if(currentDate == null) {
              throw new RuntimeException("Parser error: found unadjusteddate outside of effective/terminationdate");
            }
            currentDate.setUnadjusted(new LocalDate(reader.getElementText()));
            break;
          case period:
            dates.setPeriod(reader.getElementText());
            break;
          case periodmultiplier:
            dates.setPeriodMultiplier(Integer.parseInt(reader.getElementText()));
            break;
          case rollconvention:
            dates.setRollConvention(reader.getElementText());
            break;
          case businessdayconvention:
            BusinessDayConvention convention = BusinessDayConvention.valueOf(reader.getElementText());
            if(currentDate != null) {
              currentDate.setConvention(convention);
            } else {
              dates.setPeriodConvention(convention);
            }
            break;
          case businesscenters:
            String id = reader.getAttributeValue(null, "id");
            centers = currentDate == null ? dates.getPeriodBusinessCenters() : currentDate.getBusinessCenters();
            if(id != null) {
              ctx.getBusinessCenters().put(id, centers);
            }
            break;
          case businesscentersreference:
            centers = ctx.getBusinessCenters().get(reader.getAttributeValue(null, "href"));
            break;
          case businesscenter:
            centers.add(reader.getElementText());
            break;
        }
      } else if(event == END_ELEMENT) {
        Element element = Element.valueOf(reader.getLocalName().toLowerCase());
        switch(element) {
          case calculationperioddates:
            //we're done
            return dates;
          case businesscentersreference:
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
