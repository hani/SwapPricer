package net.formicary.pricer.model;

import org.joda.time.LocalDate;

/**
 * @author hani
 *         Date: 10/11/11
 *         Time: 9:07 AM
 */
public class VanillaSwap {
  private String id;
  private FixedLeg fixedLeg;
  private FloatingLeg floatingLeg;
  private LocalDate valuationDate;

  public LocalDate getValuationDate() {
    return valuationDate;
  }

  public void setValuationDate(LocalDate valuationDate) {
    this.valuationDate = valuationDate;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public FixedLeg getFixedLeg() {
    return fixedLeg;
  }

  public void setFixedLeg(FixedLeg fixedLeg) {
    this.fixedLeg = fixedLeg;
  }

  public FloatingLeg getFloatingLeg() {
    return floatingLeg;
  }

  public void setFloatingLeg(FloatingLeg floatingLeg) {
    this.floatingLeg = floatingLeg;
  }
}
