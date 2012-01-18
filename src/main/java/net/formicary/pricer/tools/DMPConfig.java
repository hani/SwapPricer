package net.formicary.pricer.tools;

import com.beust.jcommander.Parameter;

public class DMPConfig {
  @Parameter(names = "-showTraces", description = "Show stacktraces for trades that are not priced")
  boolean showTraces = false;
  @Parameter(names = "-fastinfoset", description = "Whether the specified input directory is a fastinfoset (.fi files) or not, defaults to false")
  boolean isFastinfoset = false;
  @Parameter(names = "-in", description = "Input directory with FpML files")
  String inputDir;
  @Parameter(names = "-out", description = "Output file to write cashflow report to")
  String outputFile;

  public DMPConfig() {
  }

  public boolean isShowTraces() {
    return showTraces;
  }

  public void setShowTraces(boolean showTraces) {
    this.showTraces = showTraces;
  }

  public boolean isFastinfoset() {
    return isFastinfoset;
  }

  public void setFastinfoset(boolean fastinfoset) {
    isFastinfoset = fastinfoset;
  }

  public String getInputDir() {
    return inputDir;
  }

  public void setInputDir(String inputDir) {
    this.inputDir = inputDir;
  }

  public String getOutputFile() {
    return outputFile;
  }

  public void setOutputFile(String outputFile) {
    this.outputFile = outputFile;
  }
}