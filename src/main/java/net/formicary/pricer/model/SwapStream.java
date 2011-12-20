package net.formicary.pricer.model;

/**
 * @author hsuleiman
 *         Date: 12/20/11
 *         Time: 1:20 PM
 */
public class SwapStream {
  private CalculationPeriodDates calculationPeriodDates;
  private CalculationPeriodAmount calculationPeriodAmount;
  private ResetDates resetDates;
  private PaymentDates paymentDates;

  public CalculationPeriodDates getCalculationPeriodDates() {
    return calculationPeriodDates;
  }

  public void setCalculationPeriodDates(CalculationPeriodDates calculationPeriodDates) {
    this.calculationPeriodDates = calculationPeriodDates;
  }

  public CalculationPeriodAmount getCalculationPeriodAmount() {
    return calculationPeriodAmount;
  }

  public void setCalculationPeriodAmount(CalculationPeriodAmount calculationPeriodAmount) {
    this.calculationPeriodAmount = calculationPeriodAmount;
  }

  public ResetDates getResetDates() {
    return resetDates;
  }

  public void setResetDates(ResetDates resetDates) {
    this.resetDates = resetDates;
  }

  public PaymentDates getPaymentDates() {
    return paymentDates;
  }

  public void setPaymentDates(PaymentDates paymentDates) {
    this.paymentDates = paymentDates;
  }
}
