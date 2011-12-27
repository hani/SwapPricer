package net.formicary.pricer.impl;

import org.fpml.spec503wd3.BusinessCenters;
import org.fpml.spec503wd3.CalculationPeriodDates;
import org.fpml.spec503wd3.InterestRateStream;

import java.util.HashMap;
import java.util.Map;

/**
 * @author hsuleiman
 *         Date: 12/20/11
 *         Time: 12:54 PM
 */
public class FpmlContext {
  private Map<String, BusinessCenters> businessCenters = new HashMap<String, BusinessCenters>();
  private Map<String, CalculationPeriodDates> calculationPeriodDates = new HashMap<String, CalculationPeriodDates>();
  private Map<String, NodeParser> parsers;
  private InterestRateStream stream1;
  private InterestRateStream stream2;

  public InterestRateStream getStream1() {
    return stream1;
  }

  public void setStream1(InterestRateStream stream1) {
    this.stream1 = stream1;
  }

  public InterestRateStream getStream2() {
    return stream2;
  }

  public void setStream2(InterestRateStream stream2) {
    this.stream2 = stream2;
  }

  public Map<String, BusinessCenters> getBusinessCenters() {
    return businessCenters;
  }

  public void setBusinessCenters(Map<String, BusinessCenters> businessCenters) {
    this.businessCenters = businessCenters;
  }

  public Map<String, CalculationPeriodDates> getCalculationPeriodDates() {
    return calculationPeriodDates;
  }

  public void setCalculationPeriodDates(Map<String, CalculationPeriodDates> calculationPeriodDates) {
    this.calculationPeriodDates = calculationPeriodDates;
  }

  public void setParsers(Map<String, NodeParser> parsers) {
    this.parsers = parsers;
  }

  public Map<String, NodeParser> getParsers() {
    return parsers;
  }
}
