package net.formicary.pricer.util;

import org.joda.time.LocalDate;

import javax.xml.datatype.XMLGregorianCalendar;

/**
 * @author hsuleiman
 *         Date: 12/27/11
 *         Time: 10:11 AM
 */
public class DateUtil {
  public static LocalDate getDate(XMLGregorianCalendar cal) {
    return new LocalDate(cal.getYear(), cal.getMonth(), cal.getDay());
  }
}
