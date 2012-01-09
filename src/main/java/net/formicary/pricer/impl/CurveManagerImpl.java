package net.formicary.pricer.impl;

import hirondelle.date4j.DateTime;
import javolution.text.TypeFormat;
import net.formicary.pricer.CurveManager;
import net.formicary.pricer.model.CurvePillarPoint;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.fpml.spec503wd3.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.*;

import static org.apache.commons.math.util.FastMath.exp;

/**
 * @author hani
 *         Date: 10/11/11
 *         Time: 10:36 AM
 */
public class CurveManagerImpl implements CurveManager {
  private static final Logger log = LoggerFactory.getLogger(CurveManagerImpl.class);
  //this is ok since we only use it when reading in the curve data, so no multithreaded access
  //a map of currency -> (tenor -> forward,discount) curves
  private Map<String, Map<String, String[]>> mapping = new HashMap<String, Map<String, String[]>>();
  //a map of curve -> list of pillar points
  private String curveDir = "curvedata";
  private Map<String, List<CurvePillarPoint>> curveData = new HashMap<String, List<CurvePillarPoint>>();
//  private static final Object NOT_FOUND = new Object();
//  private Map<String, Object> cache = new ConcurrentHashMap<String, Object>();

  public CurveManagerImpl() throws IOException {
    File dir = new File(curveDir);
    if(!dir.exists() || !dir.isDirectory()) {
      throw new IllegalArgumentException("Invalid curve dir " + dir.getAbsolutePath());
    }
    loadCurveMapping("/curvemapping.csv");
    String[] files = dir.list(new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        return name.toLowerCase().endsWith(".txt");
      }
    });
    for(String file : files) {
      loadCurveData(new File(dir, file));
    }
  }

  private void loadCurveData(File file) throws IOException {
    List<String> list = FileUtils.readLines(file);
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
    //just to be safe, in case LCH changes stuff in the report
    for (List<CurvePillarPoint> points : curveData.values()) {
      Collections.sort(points);
    }
  }

  private CurvePillarPoint getPillar(String[] data) {
    CurvePillarPoint p = new CurvePillarPoint();
    p.setCurveName(data[0]);
    String close = data[1].substring(0, data[1].indexOf(' '));
    p.setCloseDate(DateTime.forDateOnly(Integer.parseInt(close.substring(6, 10)), Integer.parseInt(close.substring(3, 5)), Integer.parseInt(close.substring(0, 2))));
    String maturity = data[2].substring(0, data[2].indexOf(' '));
    p.setMaturityDate(DateTime.forDateOnly(Integer.parseInt(maturity.substring(6, 10)), Integer.parseInt(maturity.substring(3, 5)), Integer.parseInt(maturity.substring(0, 2))));
    p.setAccrualFactor(TypeFormat.parseDouble(data[3]));
    p.setZeroRate(TypeFormat.parseDouble(data[4]));
    p.setDiscountFactor(TypeFormat.parseDouble(data[5]));
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
  public double getDiscountFactor(DateTime flowDate, DateTime valuationDate, String ccy, Interval tenor, boolean isFixed) {
    //LCH uses OIS rate for futured fixed flows
    double interpolatedZeroRate;
    if(isFixed) {
      interpolatedZeroRate = getInterpolatedForwardRate(flowDate, ccy, "OIS");
    } else {
      String val = tenor.getPeriodMultiplier() + tenor.getPeriod().value();
      interpolatedZeroRate = getInterpolatedDiscountRate(flowDate, ccy, val);
    }
    double days = valuationDate.numDaysFrom(flowDate);
    return exp(interpolatedZeroRate * -(days) / 365d);
  }

  @Override
  public double getInterpolatedForwardRate(DateTime date, String ccy, String tenor) {
    String curve = getForwardCurve(ccy, tenor);
    return getInterpolatedRate(date, ccy, curve);
  }

  @Override
  public double getInterpolatedDiscountRate(DateTime date, String ccy, String tenor) {
    String curve = getDiscountCurve(ccy, tenor);
    return getInterpolatedRate(date, ccy, curve);
  }

  public double getInterpolatedRate(DateTime date, String ccy, String curve) {
//    String key = ccy + curve + date.getYear() + '-' + date.getDayOfYear() + '-' + date.getDayOfMonth();
//    Object value = cache.get(key);
//    if(value != null) {
//      if(value == NOT_FOUND) {
//        throw new IllegalArgumentException("No curve points found for curve " + curve + " currency " + ccy + " on date " + date);
//      }
//      return (Double)value;
//    }

    List<CurvePillarPoint> points = curveData.get(curve);
    if(points == null) {
//      cache.put(key, NOT_FOUND);
      throw new IllegalArgumentException("No curve points found for curve " + curve + " currency " + ccy + " on date " + date);
    }
    //if we're here, then we definitely have to interpolate, and so the search will always return a negative
    int index = Collections.binarySearch(points, new CurvePillarPoint(curve, date));
    if(index > -1) {
      //we have an exact match, no need to interpolate, right?
      double zeroRate = points.get(index).getZeroRate();
//      cache.put(key, zeroRate);
      return zeroRate;
    } else if(index == -1) {
      //it's right before the first element, so we just use that rate
      double zeroRate = points.get(0).getZeroRate();
//      cache.put(key, zeroRate);
      return zeroRate;
    } else {
      //we definitely need to interpolate, it's a negative index thats somewhere between two pillar points
      int endIndex = -(index + 1);
      CurvePillarPoint end = points.get(endIndex);
      CurvePillarPoint start = points.get(endIndex - 1);
      double daysFromStartToNow =  start.getMaturityDate().numDaysFrom(date);
      double totalDays = start.getMaturityDate().numDaysFrom(end.getMaturityDate());
      //linear interpolation, nothing fancy
      double interpRate = start.getZeroRate() + daysFromStartToNow * (end.getZeroRate() - start.getZeroRate()) / totalDays;
//      cache.put(key, interpRate);
      return interpRate;
    }
  }

  @Override
  public double getImpliedForwardRate(DateTime start, DateTime end, DateTime valuationDate, String ccy, Interval tenor) {
    String val = tenor.getPeriodMultiplier() + tenor.getPeriod().value();
    double startRate = getInterpolatedForwardRate(start, ccy, val);
    //all LCH curves are quoted /365
    double startDf = Math.exp(startRate * -(valuationDate.numDaysFrom(start)) / 365d);
    double endRate = getInterpolatedForwardRate(end, ccy, val);
    double endDf = Math.exp(endRate * - (valuationDate.numDaysFrom(end))/ 365d);
    double forwardRate = ((startDf/endDf) - 1) * (360d/start.numDaysFrom(end));
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
