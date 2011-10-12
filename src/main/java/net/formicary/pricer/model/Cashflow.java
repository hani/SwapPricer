package net.formicary.pricer.model;

import org.joda.time.LocalDate;

/**
 * @author hani
 *         Date: 10/11/11
 *         Time: 9:22 PM
 */
public class Cashflow {
  private double npv;
  private String id;
  private LocalDate date;

  public double getNpv() {
    return npv;
  }

  public void setNpv(double npv) {
    this.npv = npv;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public LocalDate getDate() {
    return date;
  }

  public void setDate(LocalDate date) {
    this.date = date;
  }
}
