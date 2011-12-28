package net.formicary.pricer.impl;

import org.fpml.spec503wd3.BusinessCenters;
import org.fpml.spec503wd3.CalculationPeriodDates;
import org.fpml.spec503wd3.InterestRateStream;
import org.fpml.spec503wd3.Party;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
  private List<InterestRateStream> streams = new ArrayList<InterestRateStream>();
  private Map<String, Party> parties = new HashMap<String, Party>();

  public Map<String, Party> getParties() {
    return parties;
  }

  public void setParties(Map<String, Party> parties) {
    this.parties = parties;
  }

  public List<InterestRateStream> getStreams() {
    return streams;
  }

  public void setStreams(List<InterestRateStream> streams) {
    this.streams = streams;
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
