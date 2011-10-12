package net.formicary.pricer.model;

/**
 * @author hani
 *         Date: 10/11/11
 *         Time: 9:07 AM
 */
public class VanillaSwap {
  private String id;
  private SwapLeg fixedLeg;
  private SwapLeg floatingLeg;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public SwapLeg getFixedLeg() {
    return fixedLeg;
  }

  public void setFixedLeg(SwapLeg fixedLeg) {
    this.fixedLeg = fixedLeg;
  }

  public SwapLeg getFloatingLeg() {
    return floatingLeg;
  }

  public void setFloatingLeg(SwapLeg floatingLeg) {
    this.floatingLeg = floatingLeg;
  }
}
