package net.formicary.pricer.tools;

import com.beust.jcommander.Parameter;

public class DMPConfig {
  @Parameter(names = "-showTraces", description = "Show stacktraces for trades that are not priced")
  public  boolean showTraces = false;
  @Parameter(names = "-fastinfoset", description = "Whether the specified input directory is a fastinfoset (.fi files) or not, defaults to false")
  public  boolean isFastinfoset = false;
  @Parameter(names = "-in", description = "Input directory with FpML files", required = true)
  public  String inputDir;
  @Parameter(names = "-out", description = "Output file to write cashflow report to", required = true)
  public String outputFile;
  @Parameter(names = "-asof", description = "Trade valuation date, in YYYY-MM-DD format", required = true)
  public String valuationDate;
}