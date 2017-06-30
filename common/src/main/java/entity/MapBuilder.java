package entity;

import java.util.HashMap;
import java.util.Map;

public class MapBuilder<K, V> {
  private final Map<K, V> map;

  public MapBuilder() {
    this(new HashMap<>());
  }

  public MapBuilder(Map<K, V> map) {
    this.map = map;
  }

  public MapBuilder<K, V> put(K key, V value) {
    map.put(key, value);
    return this;
  }

  public Map<K, V> build() {
    return map;
  }
}
