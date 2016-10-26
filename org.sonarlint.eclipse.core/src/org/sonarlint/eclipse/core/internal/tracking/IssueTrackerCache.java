package org.sonarlint.eclipse.core.internal.tracking;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// TODO make it persistent
public class IssueTrackerCache {

  private final Map<String, Collection<MutableTrackable>> cache;

  public IssueTrackerCache(String moduleKey) {
    this.cache = new ConcurrentHashMap<>();
  }

  public boolean isFirstAnalysis(String file) {
    return !cache.containsKey(file);
  }

  public Collection<MutableTrackable> getCurrentTrackables(String file) {
    return cache.get(file);
  }

  public void put(String file, Collection<MutableTrackable> trackables) {
    cache.put(file, trackables);
  }

}
