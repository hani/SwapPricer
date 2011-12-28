package net.formicary.pricer.impl.parsers;

import net.formicary.pricer.impl.FpmlContext;
import net.formicary.pricer.impl.NodeParser;
import org.fpml.spec503wd3.Party;
import org.fpml.spec503wd3.PartyId;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;

/**
 * @author hsuleiman
 *         Date: 12/28/11
 *         Time: 5:29 PM
 */
public class PartyParser implements NodeParser<Party> {
  enum Element {
    partyId,
    party
  }

  @Override
  public Party parse(XMLStreamReader reader, FpmlContext ctx) throws XMLStreamException {
    Party party = new Party();
    party.setId(reader.getAttributeValue(null, "id"));
    while (reader.hasNext()) {
      int event = reader.next();
      if (event == START_ELEMENT) {
        Element element = Element.valueOf(reader.getLocalName());
        switch (element) {
          case partyId:
            PartyId id = new PartyId();
            id.setValue(reader.getElementText());
            party.getPartyId().add(id);
            break;
        }
      } else if (event == END_ELEMENT) {
        Element element = Element.valueOf(reader.getLocalName());
        switch (element) {
          case party:
            if(party.getId() != null)
              ctx.getParties().put(party.getId(), party);
            return party;
        }
      }
    }
    return null;
  }
}
