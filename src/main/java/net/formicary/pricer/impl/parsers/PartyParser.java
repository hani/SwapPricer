package net.formicary.pricer.impl.parsers;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import net.formicary.pricer.impl.FpmlContext;
import net.formicary.pricer.impl.NodeParser;
import org.fpml.spec503wd3.Party;
import org.fpml.spec503wd3.PartyId;

import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;

/**
 * @author hsuleiman
 *         Date: 12/28/11
 *         Time: 5:29 PM
 */
public class PartyParser implements NodeParser<Party> {

  @Override
  public Party parse(XMLStreamReader reader, FpmlContext ctx) throws XMLStreamException {
    Party party = new Party();
    party.setId(reader.getAttributeValue(null, "id"));
    while(reader.hasNext()) {
      int event = reader.next();
      if(event == START_ELEMENT) {
        if("partyId".equals(reader.getLocalName())) {
          PartyId id = new PartyId();
          id.setValue(reader.getElementText());
          party.getPartyId().add(id);
        }
      } else if(event == END_ELEMENT) {
        if("party".equals(reader.getLocalName())) {
          if(party.getId() != null) {
            ctx.registerObject(party.getId(), party);
          }
          return party;

        }
      }
    }
    return null;
  }
}
