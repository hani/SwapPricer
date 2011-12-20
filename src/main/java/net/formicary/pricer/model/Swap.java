package net.formicary.pricer.model;

/**
 * @author hsuleiman
 *         Date: 12/20/11
 *         Time: 4:17 PM
 */
public class Swap {
  private String id;
  private SwapStream stream1;
  private SwapStream stream2;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public SwapStream getStream1() {
    return stream1;
  }

  public void setStream1(SwapStream stream1) {
    this.stream1 = stream1;
  }

  public SwapStream getStream2() {
    return stream2;
  }

  public void setStream2(SwapStream stream2) {
    this.stream2 = stream2;
  }
}
