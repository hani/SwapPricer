package net.formicary.pricer.util;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

/**
 * @author hsuleiman
 *         Date: 12/27/11
 *         Time: 10:11 AM
 */
public class DateUtil {
  private static DatatypeFactory dataTypeFactory;
  static {
    try {
      dataTypeFactory = DatatypeFactory.newInstance();
    } catch (DatatypeConfigurationException e) {
      throw new RuntimeException(e);
    }
  }

  public static FastDate getDate(XMLGregorianCalendar cal) {
    if(cal == null) return null;
    return new FastDate(cal.getYear(), cal.getMonth(), cal.getDay());
  }

  public static XMLGregorianCalendar getCalendar(String s) {
    return dataTypeFactory.newXMLGregorianCalendar(s);
  }
}
