package net.formicary.pricer.model;

import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.List;

/**
 * @author hsuleiman
 *         Date: 12/20/11
 *         Time: 12:10 PM
 */
public class PeriodDate {
  private LocalDate unadjusted;
  private BusinessDayConvention convention;
  private List<String> businessCenters = new ArrayList<String>();

  public LocalDate getUnadjusted() {
    return unadjusted;
  }

  public void setUnadjusted(LocalDate unadjusted) {
    this.unadjusted = unadjusted;
  }

  public BusinessDayConvention getConvention() {
    return convention;
  }

  public void setConvention(BusinessDayConvention convention) {
    this.convention = convention;
  }

  public List<String> getBusinessCenters() {
    return businessCenters;
  }

  public void setBusinessCenters(List<String> businessCenters) {
    this.businessCenters = businessCenters;
  }

  @Override
  public String toString() {
    return "PeriodDate{" +
        "unadjusted=" + unadjusted +
        ", convention=" + convention +
        ", businesscenters=" + businessCenters +
        '}';
  }
}
