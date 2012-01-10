package net.formicary.pricer.util;

import static net.formicary.pricer.util.FastDate.DayOverflow.FirstDay;
import static net.formicary.pricer.util.FastDate.DayOverflow.LastDay;
import static net.formicary.pricer.util.FastDate.DayOverflow.Spillover;

/**
 Helper class for adding intervals of time.
 The mental model of this class is similar to that of a car's odometer, except
 in reverse.
 */
final class DateTimeInterval {

  public static final String YEAR = "Year";
  public static final String MONTH = "Month";
  public static final String DAY = "Day";

  DateTimeInterval(FastDate aFrom, FastDate.DayOverflow aMonthOverflow){
    fFrom = aFrom;
    fYear =  fFrom.getYear();
    fMonth =  fFrom.getMonth();
    fDay = fFrom.getDay();
    fDayOverflow = aMonthOverflow;
  }

  FastDate plus(int aYear, int aMonth, int aDay){
    return plusOrMinus(PLUS, aYear, aMonth, aDay);
  }

  // PRIVATE
  private final FastDate fFrom;
  private boolean fIsPlus;
  private FastDate.DayOverflow fDayOverflow;

  private int fYearIncr;
  private int fMonthIncr;
  private int  fDayIncr;

  private int fYear;
  private int fMonth;
  private int fDay;

  private static final int MIN = 0;
  private static final int MAX = 9999;
  private static final boolean PLUS = true;

  private FastDate plusOrMinus(boolean aIsPlus, int aYear, int aMonth, int aDay){
    fIsPlus = aIsPlus;
    fYearIncr = aYear;
    fMonthIncr = aMonth;
    fDayIncr = aDay;

    checkRange(fYearIncr, YEAR);
    checkRange(fMonthIncr, MONTH);
    checkRange(fDayIncr, DAY);

    changeYear();
    changeMonth();
    handleMonthOverflow();
    changeDay();
    return new FastDate(fYear, fMonth, fDay);
  }

  private void checkRange(int aValue, String aName) {
    if ( aValue <  MIN || aValue > MAX ) {
      throw new IllegalArgumentException(aName + " is not in the range " + MIN + ".." + MAX);
    }
  }

  private void changeYear(){
    if(fIsPlus){
      fYear = fYear + fYearIncr;
    }
    else {
      fYear = fFrom.getYear() - fYearIncr;
    }
    //the DateTime ctor will check the range of the year
  }

  private void changeMonth(){
    int count = 0;
    while (count < fMonthIncr){
      stepMonth();
      count++;
    }
  }

  private void  changeDay(){
    int count = 0;
    while (count < fDayIncr){
      stepDay();
      count++;
    }
  }

  private void stepYear() {
    if(fIsPlus) {
      fYear = fYear + 1;
    }
    else {
      fYear = fYear - 1;
    }
  }

  private void stepMonth() {
    if(fIsPlus){
      fMonth = fMonth + 1;
    }
    else {
      fMonth = fMonth - 1;
    }
    if(fMonth > 12) {
      fMonth = 1;
      stepYear();
    }
    else if(fMonth < 1){
      fMonth = 12;
      stepYear();
    }
  }

  private void stepDay() {
    if(fIsPlus){
      fDay = fDay + 1;
    }
    else {
      fDay = fDay - 1;
    }
    if(fDay > numDaysInMonth()){
      fDay = 1;
      stepMonth();
    }
    else if (fDay < 1){
      fDay = numDaysInPreviousMonth();
      stepMonth();
    }
  }

  private int numDaysInMonth(){
    return FastDate.getNumDaysInMonth(fYear, fMonth);
  }

  private int numDaysInPreviousMonth(){
    int result;
    if(fMonth > 1) {
      result = FastDate.getNumDaysInMonth(fYear, fMonth - 1);
    }
    else {
      result = FastDate.getNumDaysInMonth(fYear - 1 , 12);
    }
    return result;
  }


  private void handleMonthOverflow(){
    int daysInMonth = numDaysInMonth();
    if( fDay > daysInMonth ){
      if(FastDate.DayOverflow.Abort == fDayOverflow) {
        throw new RuntimeException(
            "Day Overflow: Year:" + fYear + " Month:" + fMonth + " has " + daysInMonth + " days, but day has value:" + fDay +
                " To avoid these exceptions, please specify a different DayOverflow policy."
        );
      }
      else if (FirstDay == fDayOverflow) {
        fDay = 1;
        stepMonth();
      }
      else if (LastDay == fDayOverflow) {
        fDay = daysInMonth;
      }
      else if (Spillover == fDayOverflow) {
        fDay = fDay - daysInMonth;
        stepMonth();
      }
    }
  }

}
