package net.formicary.pricer.impl.parsers;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import net.formicary.pricer.impl.FpmlContext;
import net.formicary.pricer.impl.NodeParser;
import org.fpml.spec503wd3.BusinessCenter;
import org.fpml.spec503wd3.BusinessCenters;

import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;

/**
 * @author hsuleiman
 *         Date: 1/13/12
 *         Time: 12:27 PM
 */
public class BusinessCentersParser implements NodeParser<BusinessCenters> {

  @Override
  public BusinessCenters parse(XMLStreamReader reader, FpmlContext ctx) throws XMLStreamException {
    BusinessCenters bc = new BusinessCenters();
    String id = reader.getAttributeValue(null, "id");
    if(id != null) {
      ctx.registerObject(id, bc);
    }
    while(reader.hasNext()) {
      int event = reader.next();
      if(event == START_ELEMENT) {
        if("businessCenter".equals(reader.getLocalName())) {
          BusinessCenter c = new BusinessCenter();
          c.setId(reader.getElementText());
          c.setValue(c.getId());
          bc.getBusinessCenter().add(c);
        }
      } else if(event == END_ELEMENT) {
        if("businessCenters".equals(reader.getLocalName())) {
          return bc;
        }
      }
    }
    return bc;
  }
}
