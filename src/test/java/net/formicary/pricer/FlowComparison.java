package net.formicary.pricer;

import net.formicary.pricer.model.Cashflow;

/**
 * @author hani
 *         Date: 1/5/12
 *         Time: 9:02 PM
 */
public class FlowComparison implements Comparable<FlowComparison>{
  private final Cashflow lchFlow;
  private final Cashflow memberFlow;

  public FlowComparison(Cashflow lchFlow, Cashflow memberFlow) {
    this.lchFlow = lchFlow;
    this.memberFlow = memberFlow;
  }

  public Cashflow getLchFlow() {
    return lchFlow;
  }

  public Cashflow getMemberFlow() {
    return memberFlow;
  }

  @Override
  public String toString() {
    return "PvFlowComparison{" +
      "lchFlow=" + lchFlow +
      ", memberFlow=" + memberFlow +
      '}';
  }

  public int compareTo(FlowComparison o) {
    if(lchFlow != null) {
      if(o.getLchFlow() != null) {
        return compareFlows(lchFlow, o.getLchFlow());
      }
      return compareFlows(lchFlow, o.getMemberFlow());
    }
    if(o.getLchFlow() != null) {
      return compareFlows(memberFlow, o.getLchFlow());
    }
    return compareFlows(memberFlow, o.getMemberFlow());
  }

  private int compareFlows(Cashflow flow1, Cashflow flow2) {
    int compare = flow1.getDate().compareTo(flow2.getDate());
    if(compare == 0) {
      compare = flow1.isPay() == flow2.isPay() ? 0 : -1;
    }
    return compare;
  }

  public boolean hasDateMismatch() {
    return getLchFlow() == null || getMemberFlow() == null;
  }
}
