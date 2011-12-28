package net.formicary.pricer.impl;

import net.formicary.pricer.CurveManager;
import net.formicary.pricer.model.CurvePillarPoint;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.fpml.spec503wd3.Interval;
import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author hani
 *         Date: 10/11/11
 *         Time: 10:36 AM
 */
public class CurveManagerImpl implements CurveManager {
  private static final Logger log = LoggerFactory.getLogger(CurveManagerImpl.class);
  //this is ok since we only use it when reading in the curve data, so no multithreaded access
  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormat.forPattern("dd/MM/yyyy");
  //a map of currency -> (tenor -> forward,discount) curves
  private Map<String, Map<String, String[]>> mapping = new HashMap<String, Map<String, String[]>>();
  //a map of curve -> list of pillar points
  private Map<String, List<CurvePillarPoint>> curveData = new HashMap<String, List<CurvePillarPoint>>();
  private int invocationCount;
  private int dateComparisons;

  public CurveManagerImpl() throws IOException {
    loadCurveMapping("/curvemapping.csv");
    loadCurveData("data/rep00100a.txt");
  }

  private void loadCurveData(String fileName) throws IOException {
    List<String> list = FileUtils.readLines(new File(fileName));
    for(int i = 1; i < list.size(); i++) {
      String[] data = list.get(i).split("\t");
      CurvePillarPoint p = getPillar(data);
      List<CurvePillarPoint> points = curveData.get(p.getCurveName());
      if(points == null) {
        points = new ArrayList<CurvePillarPoint>();
        curveData.put(p.getCurveName(), points);
      }
      points.add(p);
    }
  }

  private CurvePillarPoint getPillar(String[] data) {
    CurvePillarPoint p = new CurvePillarPoint();
    p.setCurveName(data[0]);
    p.setCloseDate(LocalDate.parse(data[1].substring(0, data[1].indexOf(' ')), DATE_FORMAT));
    p.setMaturityDate(LocalDate.parse(data[2].substring(0, data[2].indexOf(' ')), DATE_FORMAT));
    p.setAccrualFactor(Double.parseDouble(data[3]));
    p.setZeroRate(Double.parseDouble(data[4]));
    p.setDiscountFactor(Double.parseDouble(data[5]));
    return p;
  }

  private void loadCurveMapping(String resource) throws IOException {
    List<String> lines = IOUtils.readLines(getClass().getResourceAsStream(resource));
    for(String line : lines) {
      String[] data = line.split(",");
      String ccy = data[0];
      String tenor = data[1];
      String forwardCurve = data[2];
      String discountCurve = data[3];
      Map<String, String[]> tenorMap = mapping.get(ccy);
      if(tenorMap == null) {
        tenorMap = new HashMap<String, String[]>();
        mapping.put(ccy, tenorMap);
      }
      tenorMap.put(tenor, new String[]{forwardCurve, discountCurve});
    }
  }

  @Override
  public double getDiscountFactor(LocalDate flowDate, LocalDate valuationDate, String ccy, Interval tenor, boolean isFixed) {
    //LCH uses OIS rate for futured fixed flows
    double interpolatedZeroRate;
    if(isFixed) {
      interpolatedZeroRate = getInterpolatedForwardRate(flowDate, ccy, "OIS");
    } else {
      String val = tenor.getPeriodMultiplier() + tenor.getPeriod().value();
      interpolatedZeroRate = getInterpolatedDiscountRate(flowDate, ccy, val);
    }
    double days = Days.daysBetween(valuationDate, flowDate).getDays();
    return Math.exp(interpolatedZeroRate * -(days) / 365d);
  }

  @Override
  public double getInterpolatedForwardRate(LocalDate date, String ccy, String tenor) {
    String curve = getForwardCurve(ccy, tenor);
    return getInterpolatedRate(date, ccy, curve);
  }

  @Override
  public double getInterpolatedDiscountRate(LocalDate date, String ccy, String tenor) {
    String curve = getDiscountCurve(ccy, tenor);
    return getInterpolatedRate(date, ccy, curve);
  }

  public double getInterpolatedRate(LocalDate date, String ccy, String curve) {
    List<CurvePillarPoint> points = curveData.get(curve);
    if(points == null) {
      throw new IllegalArgumentException("No curve points found for curve " + curve + " currency " + ccy + " on date " + date);
    }
    invocationCount++;
    for(int i = 0; i < points.size(); i++) {
      CurvePillarPoint end = points.get(i);
      //todo future enhancement, since we know the list is sorted, we can probably find a better algorithm
      //to find the two pillar points rather than a linear search. This code is called a *lot* so that sort of optimisation
      //becomes worthwhile (called 385188 times for 240 trades currently).
      dateComparisons++;
      if(end.getMaturityDate().isAfter(date)) {
        //we have our two dates, since we know the pillar list is sorted by ascending dates
        if(i == 0) {
          //its the first rate we have, so we just use that since there's nothing before to interpolate with
          return end.getZeroRate();
        }
        CurvePillarPoint start = points.get(i - 1);
        double daysFromStartToNow = Days.daysBetween(start.getMaturityDate(), date).getDays();
        double totalDays = Days.daysBetween(start.getMaturityDate(), end.getMaturityDate()).getDays();
        //linear interpolation, nothing fancy
        return start.getZeroRate() + daysFromStartToNow * (end.getZeroRate() - start.getZeroRate()) / totalDays;
      }
    }
    throw new IllegalArgumentException("No rate data found for date " + date + " currency " + ccy);
  }

  @Override
  public double getImpliedForwardRate(LocalDate start, LocalDate end, LocalDate valuationDate, String ccy, Interval tenor) {
    String val = tenor.getPeriodMultiplier() + tenor.getPeriod().value();
    double startRate = getInterpolatedForwardRate(start, ccy, val);
    //all LCH curves are quoted /365
    double startDf = Math.exp(startRate * -(Days.daysBetween(valuationDate, start).getDays()) / 365d);
    double endRate = getInterpolatedForwardRate(end, ccy, val);
    double endDf = Math.exp(endRate * - (Days.daysBetween(valuationDate, end).getDays())/ 365d);
    double forwardRate = ((startDf/endDf) - 1) * (360d/Days.daysBetween(start, end).getDays());
    return forwardRate;
  }

  @Override
  public String getDiscountCurve(String ccy, String tenor) {
    String[] values = mapping.get(ccy).get(tenor);
    if(values == null) {
      values = mapping.get(ccy).get("other");
    }
    return values[1];
  }

  @Override
  public String getForwardCurve(String ccy, String tenor) {
    String[] values = mapping.get(ccy).get(tenor);
    if(values == null) {
      values = mapping.get(ccy).get("other");
    }
    return values[0];
  }
}
