package net.formicary.pricer;

import com.google.inject.ImplementedBy;
import net.formicary.pricer.impl.FpmlTradeStore;
import org.fpml.spec503wd3.Swap;

/**
 * @author hani
 *         Date: 10/11/11
 *         Time: 9:24 PM
 */
@ImplementedBy(FpmlTradeStore.class)
public interface TradeStore {
  Swap getTrade(String id);
}
