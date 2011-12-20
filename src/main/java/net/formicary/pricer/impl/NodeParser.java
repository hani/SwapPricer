package net.formicary.pricer.impl;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * @author hsuleiman
 *         Date: 12/20/11
 *         Time: 11:39 AM
 */
public interface NodeParser {
  public Object parse(XMLStreamReader reader, FpmlContext ctx) throws XMLStreamException;
}
