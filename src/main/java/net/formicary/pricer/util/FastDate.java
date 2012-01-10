package net.formicary.pricer.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * Building block class for a date-time, with no time zone.
 * This class is a hacked up/optimised version of date4j's DateTime, with the following changes:
 * <ul>
 *   <li>Remove convenient parsing/formatting methods, they are all quite expensive so the caller
 *   should worry about them</li>
 *   <li>Get rid of all time handling, we work with dates only</li>
 *   <li>Better caching of calculatations</li>
 * </ul>
 */
public final class FastDate implements Comparable<FastDate>, Serializable {

  /* The following 7 items represent the parsed form of a DateTime. */
  /**  @serial */
  private int year;
  /**  @serial */
  private int month;
  /**  @serial */
  private int day;
  /** @serial */
  private int hashCode;
  /** @serial */
  private int julianDayNumber;

  private static int EPOCH_MODIFIED_JD = 2400000;
  private static final long serialVersionUID =  -1300068157085493891L;

  /** The seven parts of a <tt>FastDate</tt> object. The <tt>DAY</tt> represents the day of the month (1..31), not the weekday. */
  public enum Unit {
    YEAR, MONTH, DAY
  }

  /**
   Policy for treating 'day-of-the-month overflow' conditions encountered during some date calculations.

   <P>Months are different from other units of time, since the length of a month is not fixed, but rather varies with
   both month and year. This leads to problems. Take the following simple calculation, for example :

   <PRE>May 31 + 1 month = ?</PRE>

   <P>What's the answer? Since there is no such thing as June 31, the result of this operation is inherently ambiguous.
   This  <tt>DayOverflow</tt> enumeration lists the various policies for treating such situations, as supported by
   <tt>FastDate</tt>.

   <P>This table illustrates how the policies behave :
   <P><table BORDER="1" CELLPADDING="3" CELLSPACING="0">
   <tr>
   <th>Date</th>
   <th>DayOverflow</th>
   <th>Result</th>
   </tr>
   <tr>
   <td>May 31 + 1 Month</td>
   <td>LastDay</td>
   <td>June 30</td>
   </tr>
   <tr>
   <td>May 31 + 1 Month</td>
   <td>FirstDay</td>
   <td>July 1</td>
   </tr>
   <tr>
   <td>December 31, 2001 + 2 Months</td>
   <td>Spillover</td>
   <td>March 3</td>
   </tr>
   <tr>
   <td>May 31 + 1 Month</td>
   <td>Abort</td>
   <td>RuntimeException</td>
   </tr>
   </table>
   */
  public enum DayOverflow {
    /** Coerce the day to the last day of the month. */
    LastDay,
    /** Coerce the day to the first day of the next month. */
    FirstDay,
    /** Spillover the day into the next month. */
    Spillover,
    /** Throw a RuntimeException. */
    Abort
  }

  /**
   Constructor taking each time unit explicitly.

   <P>Although all parameters are optional, many operations on this class require year-month-day to be
   present.

   @param aYear 1..9999, optional
   @param aMonth 1..12 , optional
   @param aDay 1..31, cannot exceed the number of days in the given month/year, optional
   */
  public FastDate(Integer aYear, Integer aMonth, Integer aDay) {
    year = aYear;
    month = aMonth;
    day = aDay;
    validateState();
  }

  /**
   Constructor taking a millisecond value and a {@link TimeZone}.
   This constructor may be use to convert a <tt>java.util.Date</tt> into a <tt>FastDate</tt>.

   <P>Unfortunately, only millisecond precision is possible for this method.

   @param aMilliseconds must be in the range corresponding to the range of dates supported by this class (year 1..9999); corresponds
   to a millisecond instant on the timeline, measured from the epoch used by {@link java.util.Date}.
   */
  public static FastDate forInstant(long aMilliseconds, TimeZone aTimeZone) {
    Calendar calendar = new GregorianCalendar(aTimeZone);
    calendar.setTimeInMillis(aMilliseconds);
    int year = calendar.get(Calendar.YEAR);
    int month = calendar.get(Calendar.MONTH) + 1; // 0-based
    int day = calendar.get(Calendar.DAY_OF_MONTH);
    return new FastDate(year, month, day);
  }

  /**
   For the given time zone,  return the corresponding time in milliseconds for this <tt>FastDate</tt>.

   <P>This method is meant to help you convert between a <tt>FastDate</tt> and the
   JDK's date-time classes, which are based on the combination of a time zone and a
   millisecond value from the Java epoch.
   <P>Since <tt>FastDate</tt> can go to nanosecond accuracy, the return value can
   lose precision. The nanosecond value is truncated to milliseconds, not rounded.
   <P>Requires year-month-day to be present; if not, a runtime exception is thrown.
   */
  public long getMilliseconds(TimeZone aTimeZone){
    Integer year = getYear();
    Integer month = getMonth();
    Integer day = getDay();

    Calendar calendar = new GregorianCalendar(aTimeZone);
    calendar.set(Calendar.YEAR, year);
    calendar.set(Calendar.MONTH, month-1); // 0-based
    calendar.set(Calendar.DAY_OF_MONTH, day);
    calendar.set(Calendar.HOUR_OF_DAY, 0); // 0..23
    calendar.set(Calendar.MINUTE, 0);
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);

    return calendar.getTimeInMillis();
  }

  /** Return the year, 1..9999. */
  public Integer getYear() {
    return year;
  }

  /** Return the Month, 1..12. */
  public Integer getMonth() {
    return month;
  }

  /** Return the Day of the Month, 1..31. */
  public Integer getDay() {
    return day;
  }

  /**
   Return the Modified Julian Day Number.
   <P>The Modified Julian Day Number is defined by astronomers for simplifying the calculation of the number of days between 2 dates.
   Returns a monotonically increasing sequence number.
   Day 0 is November 17, 1858 00:00:00 (whose Julian Date was 2400000.5).

   <P>Using the Modified Julian Day Number instead of the Julian Date has 2 advantages:
   <ul>
   <li>it's a smaller number
   <li>it starts at midnight, not noon (Julian Date starts at noon)
   </ul>

   <P>Does not reflect any time portion, if present.

   <P>(In spite of its name, this method, like all other methods in this class, uses the
   proleptic Gregorian calendar - not the Julian calendar.)

   <P>Requires year-month-day to be present; if not, a runtime exception is thrown.
   */
  public Integer getModifiedJulianDayNumber() {
    return calculateJulianDayNumberAtNoon() - 1 - EPOCH_MODIFIED_JD;
  }

  /**
   Return an index for the weekday for this <tt>FastDate</tt>.
   Returns 1..7 for Sunday..Saturday.
   <P>Requires year-month-day to be present; if not, a runtime exception is thrown.
   */
  public Integer getWeekDay() {
    int dayNumber = calculateJulianDayNumberAtNoon() + 1;
    int index = dayNumber % 7;
    return index + 1;
  }

  /**
   Return an integer in the range 1..366, representing a count of the number of days from the start of the year.
   January 1 is counted as day 1.
   <P>Requires year-month-day to be present; if not, a runtime exception is thrown.
   */
  public int getDayOfYear() {
    int k = isLeapYear() ? 1 : 2;
    return ((275 * month) / 9) - k * ((month + 9) / 12) + day - 30;
  }

  /**
   Returns true only if the year is a leap year.
   <P>Requires year to be present; if not, a runtime exception is thrown.
   */
  public boolean isLeapYear() {
    return isLeapYear(year);
  }

  /**
   Return the number of days in the month which holds this <tt>FastDate</tt>.
   <P>Requires year-month-day to be present; if not, a runtime exception is thrown.
   */
  public int getNumDaysInMonth() {
    return getNumDaysInMonth(year, month);
  }

  /**
   Return The week index of this <tt>FastDate</tt> with respect to a given starting <tt>FastDate</tt>.
   <P>The single parameter to this method defines first day of week number 1.
   <P>Requires year-month-day to be present; if not, a runtime exception is thrown.
   */
  public Integer getWeekIndex(FastDate aStartingFromDate) {
    int diff = getModifiedJulianDayNumber() - aStartingFromDate.getModifiedJulianDayNumber();
    return (diff / 7) + 1; // integer division
  }

  /**
   'Less than' comparison.
   Return <tt>true</tt> only if this <tt>FastDate</tt> comes before the given parameter, according to {@link #compareTo(FastDate)}.
   */
  public boolean lt(FastDate aThat) {
    return compareTo(aThat) < 0;
  }

  /**
   'Less than or equal to' comparison.
   Return <tt>true</tt> only if this <tt>FastDate</tt> comes before the given parameter, according to {@link #compareTo(FastDate)},
   or this <tt>FastDate</tt> equals the given parameter.
   */
  public boolean lteq(FastDate aThat) {
    return compareTo(aThat) < 0 || equals(aThat);
  }

  /**
   'Greater than' comparison.
   Return <tt>true</tt> only if this <tt>FastDate</tt> comes after the given parameter, according to {@link #compareTo(FastDate)}.
   */
  public boolean gt(FastDate aThat) {
    return compareTo(aThat) > 0;
  }

  /**
   'Greater than or equal to' comparison.
   Return <tt>true</tt> only if this <tt>FastDate</tt> comes after the given parameter, according to {@link #compareTo(FastDate)},
   or this <tt>FastDate</tt> equals the given parameter.
   */
  public boolean gteq(FastDate aThat) {
    return compareTo(aThat) > 0 || equals(aThat);
  }

  /**
   Return this <tt>FastDate</tt> with the time portion coerced to '00:00:00.000000000',
   and the day coerced to 1.
   <P>Requires year-month-day to be present; if not, a runtime exception is thrown.
   */
  public FastDate getStartOfMonth() {
    return getStartEndDateTime(1);
  }

  /**
   Return this <tt>FastDate</tt> with the time portion coerced to '23:59:59.999999999',
   and the day coerced to the end of the month.
   <P>Requires year-month-day to be present; if not, a runtime exception is thrown.
   */
  public FastDate getEndOfMonth() {
    return getStartEndDateTime(getNumDaysInMonth());
  }

  /**
   Create a new <tt>FastDate</tt> by adding an interval to this one.

   <P>See {@link #plusDays(Integer)} as well.

   <P>Changes are always applied by this class <i>in order of decreasing units of time</i>:
   years first, then months, and so on. After changing both the year and month, a check on the month-day combination is made before
   any change is made to the day. If the day exceeds the number of days in the given month/year, then
   (and only then) the given {@link DayOverflow} policy applied, and the day-of-the-month is adusted accordingly.

   <P>Afterwards, the day is then changed in the usual way, followed by the remaining items (hour, minute, and second).
   Changes to the fractional seconds are not included in this method, since there doesn't seem to be much practical use for it.

   <P>The returned value cannot come after <tt>9999-12-13 23:59:59</tt>.

   <P>This class works with <tt>FastDate</tt>'s having the following items present :
   <ul>
   <li>year-month-day and hour-minute-second (and optional nanoseconds)
   <li>year-month-day only. In this case, if a calculation with a time part is performed, that time part
   will be initialized by this class to 00:00:00.0, and the <tt>FastDate</tt> returned by this class will include a time part.
   <li>hour-minute-second (and optional nanoseconds) only. In this case, the calculation is done starting with the
   the arbitrary date <tt>0001-01-01</tt> (in order to remain within a valid state space of <tt>FastDate</tt>).
   </ul>

   @param aNumYears positive, required, in range 0...9999
   @param aNumMonths positive, required, in range 0...9999
   @param aNumDays positive, required, in range 0...9999
   */
  public FastDate plus(Integer aNumYears, Integer aNumMonths, Integer aNumDays, DayOverflow aDayOverflow) {
    DateTimeInterval interval = new DateTimeInterval(this, aDayOverflow);
    return interval.plus(aNumYears, aNumMonths, aNumDays);
  }

  /**
   Return a new <tt>DateTime</tt> by adding an integral number of days to this one.

   <P>Requires year-month-day to be present; if not, a runtime exception is thrown.
   @param aNumDays can be either sign; if negative, then the days are subtracted.
   */
  public FastDate plusDays(Integer aNumDays) {
    int thisJDAtNoon = getModifiedJulianDayNumber() + 1 + EPOCH_MODIFIED_JD;
    int resultJD = thisJDAtNoon + aNumDays;
    return fromJulianDayNumberAtNoon(resultJD);
  }

  /**
   The whole number of days between this <tt>FastDate</tt> and the given parameter.
   <P>Requires year-month-day to be present, both for this <tt>FastDate</tt> and for the <tt>aThat</tt>
   parameter; if not, a runtime exception is thrown.
   */
  public int numDaysFrom(FastDate aThat) {
    return aThat.getModifiedJulianDayNumber() - this.getModifiedJulianDayNumber();
  }

  /**
   Return the current date-time.
   <P>Combines the return value of {@link System#currentTimeMillis()} with the given {@link TimeZone}.

   <P>Only millisecond precision is possible for this method.
   */
  public static FastDate now(TimeZone aTimeZone) {
    return forInstant(System.currentTimeMillis(), aTimeZone);
  }

  /**
   Return the current date.
   <P>As in {@link #now(TimeZone)}, but truncates the time portion, leaving only year-month-day.
   */
  public static FastDate today(TimeZone aTimeZone) {
    return now(aTimeZone);
  }

  /**
   Compare this object to another, for ordering purposes.
   <P> Uses the 7 date-time elements (year..nanosecond). The Year is considered the most
   significant item, and the Nanosecond the least significant item. Null items are placed first in this comparison.
   */
  public int compareTo(FastDate aThat) {
    if (this == aThat) return 0;
    int compare = year < aThat.year ? -1 : year > aThat.year ? 1 : 0;
    if(compare != 0) return compare;
    compare = month < aThat.month ? -1 : month > aThat.month ? 1 : 0;
    if(compare != 0) return compare;
    compare = day < aThat.day ? -1 : day > aThat.day ? 1 : 0;
    return compare;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    FastDate fastDate = (FastDate) o;

    if (day != fastDate.day) return false;
    if (month != fastDate.month) return false;
    if (year != fastDate.year) return false;

    return true;
  }

  @Override
  public int hashCode() {
    if(hashCode > 0) {
      return hashCode;
    }
    hashCode = year;
    hashCode = 31 * hashCode + month;
    hashCode = 31 * hashCode + day;
    return hashCode;
  }

  /**
   <P><b>To format this <tt>DateTime</tt> for presentation to the user, see the various <tt>format</tt> methods.</b>

   <P>Otherwise, the return value is constructed from each date-time element, in a fixed format, depending
   on which time units are present. Example values :
   <ul>
   <li>2011-04-30 13:59:59.123456789
   <li>2011-04-30 13:59:59
   <li>2011-04-30
   <li>2011-04-30 13:59
   <li>13:59:59.123456789
   <li>13:59:59
   <li>and so on...
   </ul>

   <P>In the great majority of cases, this will give reasonable output for debugging and logging statements.

   <P>In cases where a bizarre combinations of time units is present, the return value is presented in a verbose form.
   For example, if all time units are present <i>except</i> for minutes, the return value has this form:
   <PRE>Y:2001 M:1 D:31 h:13 m:null s:59 f:123456789</PRE>
   */
  @Override public String toString() {
    return year + "-" + month + "-" + day;
  }

  static final class ItemOutOfRange extends RuntimeException {
    ItemOutOfRange(String aMessage) {
      super(aMessage);
    }
    private static final long serialVersionUID = 4760138291907517660L;
  }

  /**
   Return the number of days in the given month. The returned value depends on the year as
   well, because of leap years. Returns <tt>null</tt> if either year or month are
   absent. WRONG - should be public??
   Package-private, needed for interval calcs.
   */
  static Integer getNumDaysInMonth(Integer aYear, Integer aMonth) {
    Integer result = null;
    if (aYear != null && aMonth != null) {
      if (aMonth == 1) {
        result = 31;
      }
      else if (aMonth == 2) {
        result = isLeapYear(aYear) ? 29 : 28;
      }
      else if (aMonth == 3) {
        result = 31;
      }
      else if (aMonth == 4) {
        result = 30;
      }
      else if (aMonth == 5) {
        result = 31;
      }
      else if (aMonth == 6) {
        result = 30;
      }
      else if (aMonth == 7) {
        result = 31;
      }
      else if (aMonth == 8) {
        result = 31;
      }
      else if (aMonth == 9) {
        result = 30;
      }
      else if (aMonth == 10) {
        result = 31;
      }
      else if (aMonth == 11) {
        result = 30;
      }
      else if (aMonth == 12) {
        result = 31;
      }
      else {
        throw new AssertionError("Month is out of range 1..12:" + aMonth);
      }
    }
    return result;
  }

  static FastDate fromJulianDayNumberAtNoon(int aJDAtNoon) {
    //http://www.hermetic.ch/cal_stud/jdn.htm
    int l = aJDAtNoon + 68569;
    int n = (4 * l) / 146097;
    l = l - (146097 * n + 3) / 4;
    int i = (4000 * (l + 1)) / 1461001;
    l = l - (1461 * i) / 4 + 31;
    int j = (80 * l) / 2447;
    int d = l - (2447 * j) / 80;
    l = j / 11;
    int m = j + 2 - (12 * l);
    int y = 100 * (n - 49) + i + l;
    return new FastDate(y, m, d);
  }

  /**
   Return a the whole number, with no fraction.
   The JD at noon is 1 more than the JD at midnight.
   */
  private int calculateJulianDayNumberAtNoon() {
    //http://www.hermetic.ch/cal_stud/jdn.htm
    if(julianDayNumber != 0) {
      return julianDayNumber;
    }
    int y = year;
    int m = month;
    int d = day;
    julianDayNumber = (1461 * (y + 4800 + (m - 14) / 12)) / 4 + (367 * (m - 2 - 12 * ((m - 14) / 12))) / 12 - (3 * ((y + 4900 + (m - 14) / 12) / 100)) / 4 + d - 32075;
    return julianDayNumber;
  }

  private void validateState() {
    checkRange(year, 1, 9999, "Year");
    checkRange(month, 1, 12, "Month");
    checkRange(day, 1, 31, "Day");
    checkNumDaysInMonth(year, month, day);
  }

  private void checkRange(Integer aValue, int aMin, int aMax, String aName) {
    if(aValue != null){
      if (aValue < aMin || aValue > aMax){
        throw new ItemOutOfRange(aName + " is not in the range " + aMin + ".." + aMax + ". Value is:" + aValue);
      }
    }
  }

  private void checkNumDaysInMonth(Integer aYear, Integer aMonth, Integer aDay) {
    if (aDay > getNumDaysInMonth(aYear, aMonth)) {
      throw new ItemOutOfRange("The day-of-the-month value '" + aDay + "' exceeds the number of days in the month: " + getNumDaysInMonth(aYear, aMonth));
    }
  }

  public static boolean isLeapYear(int aYear) {
    boolean result = false;
    if (aYear % 100 == 0) {
      // this is a century year
      if (aYear % 400 == 0) {
        result = true;
      }
    }
    else if (aYear % 4 == 0) {
      result = true;
    }
    return result;
  }

  private FastDate getStartEndDateTime(Integer aDay) {
    return new FastDate(year, month, aDay);
  }

  /**
   Always treat de-serialization as a full-blown constructor, by
   validating the final state of the de-serialized object.
   */
  private void readObject(ObjectInputStream aInputStream) throws ClassNotFoundException, IOException {
    //always perform the default de-serialization first
    aInputStream.defaultReadObject();
    //no mutable fields in this case
    validateState();
  }

  /**
   This is the default implementation of writeObject.
   Customise if necessary.
   */
  private void writeObject(ObjectOutputStream aOutputStream) throws IOException {
    //perform the default serialization for all non-transient, non-static fields
    aOutputStream.defaultWriteObject();
  }

}