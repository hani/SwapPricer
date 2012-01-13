package net.formicary.pricer.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.formicary.pricer.HrefListener;

/**
 * @author hsuleiman
 *         Date: 12/20/11
 *         Time: 12:54 PM
 */
public class FpmlContext {
  private Map<String, List<HrefListener>> listeners = new HashMap<String, List<HrefListener>>();
  private Map<String, Object> addedNodes = new HashMap<String, Object>();

  public void registerObject(String id, Object node) {
    List<HrefListener> list = listeners.get(id);
    if(list != null) {
      for (HrefListener hrefListener : list) {
        hrefListener.nodeAdded(id, node);
      }
    }
    addedNodes.put(id, node);
  }

  public void addHrefListener(String href, HrefListener listener) {
    List<HrefListener> list = listeners.get(href);
    if(list == null) {
      list = new ArrayList<HrefListener>();
      listeners.put(href, list);
    }
    list.add(listener);
    //fire past nodes
    Object old = addedNodes.get(href);
    if(old != null) {
      listener.nodeAdded(href, old);
    }
  }
}
