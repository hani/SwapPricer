package net.formicary.pricer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.formicary.pricer.model.Cashflow;

import static java.lang.Math.abs;

/**
 * Shamelessly stolen and adapted from dmp tools ReconcileResult, which in turn is shamelessly stolen from
 * Chris' LCH reconcile code.
 * @author hani
 *         Date: 1/5/12
 *         Time: 9:04 PM
 */
public class Reconciler {
  private List<FlowComparison> flowComparisons = new ArrayList<FlowComparison>();

  /**
   * Reconcile two lists of flows, and create an object holding various reconciliation results.
   *
   * @param lchFlows          list of flows from LCH
   * @param memberFlows          list of flows from our calc
   */
  public Reconciler(List<Cashflow> lchFlows, List<Cashflow> memberFlows) {
    for (Cashflow lchFLow : lchFlows) {
      Cashflow matchedMemberFlow = removeMatchingFlow(lchFLow, memberFlows);
      if (matchedMemberFlow != null) {
        flowComparisons.add(new FlowComparison(lchFLow, matchedMemberFlow));
      } else {
        flowComparisons.add(new FlowComparison(lchFLow, null));
      }
    }
    for (Cashflow extraFlow : memberFlows) {
      flowComparisons.add(new FlowComparison(null, extraFlow));
    }
  }

  /**
   * Look for a flow in a list with the same date as the given parameter.
   *
   * @param flow  date and party to search for
   * @param flows list of flows
   * @return the flow that was removed from the list, if any
   */
  private Cashflow removeMatchingFlow(Cashflow flow, List<Cashflow> flows) {
    for (Iterator<Cashflow> i = flows.iterator(); i.hasNext();) {
      Cashflow checkFlow = i.next();
      if (checkFlow.getDate().equals(flow.getDate()) && checkFlow.isPay() == flow.isPay()) {
        i.remove();
        return checkFlow;
      }
    }
    return null;
  }

  public double getAbsoluteDiff() {
    double diff = 0;
    for (FlowComparison flowComparison : flowComparisons) {
      if (flowComparison.getLchFlow() == null) {
        diff = diff + abs(flowComparison.getMemberFlow().getNpv());
      } else if (flowComparison.getMemberFlow() == null) {
        diff = diff + abs(flowComparison.getLchFlow().getNpv());
      } else {
        diff = diff + (flowComparison.getLchFlow().getNpv() - abs(flowComparison.getMemberFlow().getNpv()));
      }
    }
    return diff;
  }

  public List<FlowComparison> getFlowComparisons() {
    return flowComparisons;
  }
}
