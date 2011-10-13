package net.formicary.pricer.model;

/**
 * @author hani
 *         Date: 10/13/11
 *         Time: 12:16 PM
 */
public class FixedLeg extends SwapLeg {
  private double fixedRate;

  public double getFixedRate() {
    return fixedRate;
  }

  public void setFixedRate(double fixedRate) {
    this.fixedRate = fixedRate;
  }

}
