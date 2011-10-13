package net.formicary.pricer.model;

/**
 * @author hani
 *         Date: 10/13/11
 *         Time: 12:18 PM
 */
public class FloatingLeg extends SwapLeg {
  private int fixingDateOffset;
  private boolean fixingRelativeToStart;
  private String fixingCalendar;
  private String floatingRateIndex;

  public String getFloatingRateIndex() {
    return floatingRateIndex;
  }

  public void setFloatingRateIndex(String floatingRateIndex) {
    this.floatingRateIndex = floatingRateIndex;
  }

  public int getFixingDateOffset() {
    return fixingDateOffset;
  }

  public void setFixingDateOffset(int fixingDateOffset) {
    this.fixingDateOffset = fixingDateOffset;
  }

  public boolean isFixingRelativeToStart() {
    return fixingRelativeToStart;
  }

  public void setFixingRelativeToStart(boolean fixingRelativeToStart) {
    this.fixingRelativeToStart = fixingRelativeToStart;
  }

  public String getFixingCalendar() {
    return fixingCalendar;
  }

  public void setFixingCalendar(String fixingCalendar) {
    this.fixingCalendar = fixingCalendar;
  }
}
