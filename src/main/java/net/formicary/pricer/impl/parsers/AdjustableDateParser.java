package net.formicary.pricer.impl.parsers;

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
 *         Time: 12:59 PM
 */
public class AdjustableDateParser implements NodeParser<AdjustableDate> {
  private BusinessCentersParser bcParser = new BusinessCentersParser();

  enum Element {
    unadjustedDate,
    dateAdjustments,
    businessDayConvention,
    businessCenters,
    businessCentersReference
  }

  @Override
  public AdjustableDate parse(XMLStreamReader reader, FpmlContext ctx) throws XMLStreamException {
    final AdjustableDate date = new AdjustableDate();
    String id = reader.getAttributeValue(null, "id");
    if(id != null) {
      ctx.registerObject(id, date);
    }
    while(reader.hasNext()) {
      int event = reader.next();
      if (event == START_ELEMENT) {
        Element element = Element.valueOf(reader.getLocalName());
        switch(element) {
          case unadjustedDate:
            IdentifiedDate d = new IdentifiedDate();
            d.setValue(DateUtil.getCalendar(reader.getElementText()));
            date.setUnadjustedDate(d);
            break;
          case dateAdjustments:
            date.setDateAdjustments(new BusinessDayAdjustments());
            break;
          case businessDayConvention:
            date.getDateAdjustments().setBusinessDayConvention(BusinessDayConventionEnum.valueOf(reader.getElementText()));
            break;
          case businessCenters:
            date.getDateAdjustments().setBusinessCenters(bcParser.parse(reader, ctx));
            break;
          case businessCentersReference:
            ctx.addHrefListener(reader.getAttributeValue(null, "href"), new HrefListener() {
              @Override
              public void nodeAdded(String id, Object o) {
                BusinessCentersReference ref = new BusinessCentersReference();
                ref.setHref(o);
                date.getDateAdjustments().setBusinessCentersReference(ref);
              }
            });
            break;

        }
      } else if(event == END_ELEMENT) {
        if("dateAdjustments".equals(reader.getLocalName())) {
          return date;
        }
      }
    }
    return null;
  }
}
