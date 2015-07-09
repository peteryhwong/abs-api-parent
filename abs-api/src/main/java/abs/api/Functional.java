package abs.api;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * ABS Functional layer as Java 8 API.
 */
public final class Functional {

  /**
   * A pattern matching abstraction in Java 8.
   * 
   * @param <X>
   * @param <Y>
   */
  public static interface Match<X, Y> extends Function<X, Optional<Y>> {

    static <X, Y> Function<X, Optional<Y>> F(Predicate<X> cond, Function<X, Y> what) {
      Function<X, Optional<Y>> f =
          (x) -> cond.test(x) ? Optional.ofNullable(what.apply(x)) : Optional.empty();
      return f;
    }

    static <X, Y> Match<X, Y> of(Predicate<X> cond, Function<X, Y> what) {
      Match<X, Y> m = input -> F(cond, what).apply(input);
      return m;
    }

    static <X, Y> Match<X, Y> of(Y origin) {
      Predicate<X> cond = ignored -> true;
      Function<X, Y> what = x -> origin;
      return of(cond, what);
    }

    static <X, Y> Match<X, Y> ofNull(Function<X, Y> what) {
      return of(x -> x == null, what);
    }

    default Match<X, Y> orDefault(Function<X, Y> what) {
      return or(ignored -> true, what);
    }

    default Match<X, Y> or(Predicate<X> cond, Function<X, Y> what) {
      Match<X, Y> orM = input -> {
        Optional<Y> thisMatch = apply(input);
        return thisMatch.isPresent() ? thisMatch : of(cond, what).apply(input);
      };
      return orM;
    }

  }

  // --- Logical

  public static boolean and(boolean a, boolean b) {
    return a && b;
  }

  public static boolean not(boolean a) {
    return !a;
  }

  public static boolean or(boolean a, boolean b) {
    return a || b;
  }

  // --- List

  public static <E> List<E> emptyList() {
    return new ArrayList<>();
  }

  public static <E> List<E> insert(E e, List<E> list) {
    return (List<E>) insertCollection(e, list);
  }

  public static <E> List<E> list(E e, Set<E> set) {
    return new ArrayList<>(set);
  }

  // --- Set

  public static <E> Set<E> emptySet() {
    return new HashSet<>();
  }

  public static <E> Set<E> insert(E e, Set<E> collection) {
    return (Set<E>) insertCollection(e, collection);
  }

  public static <E> Set<E> set(List<E> list) {
    return set_java(list);
  }

  // --- Map

  public static <K, V> Map<K, V> emptyMap() {
    return new ConcurrentHashMap<>();
  }

  public static <K, V> Map<K, V> insert(K key, V value, Map<K, V> map) {
    if (map == null) {
      map = emptyMap();
    }
    map.putIfAbsent(key, value);
    return map;
  }

  public static <K, V> Map.Entry<K, V> pair(K key, V value) {
    return new AbstractMap.SimpleEntry<K, V>(key, value);
  }

  public static <K, V> Map<K, V> map(List<K> keys, List<V> values) {
    if (keys == null || values == null || keys.size() != values.size()) {
      throw new IllegalArgumentException(
          "Keys and values do not match for map construction: " + keys + " -> " + values);
    }
    ConcurrentMap<K, V> map = IntStream.range(0, keys.size()).boxed()
        .collect(Collectors.toConcurrentMap(index -> keys.get(index), index -> values.get(index)));
    return map;
  }

  public static <K, V> Map<K, V> map(Collection<Entry<K, V>> entries) {
    return new ArrayList<>(entries).stream().collect(
        Collectors.toConcurrentMap((Entry<K, V> e) -> e.getKey(), (Entry<K, V> e) -> e.getValue()));
  }

  // --- Internal

  protected static <E> Collection<E> insertCollection(E e, Collection<E> col) {
    col.add(e);
    return col;
  }

  protected static <E> Set<E> set_java(List<E> list) {
    return new HashSet<>(list);
  }

  protected static <E> Set<E> set_func(List<E> list) {
    Match<List<E>, Set<E>> m = Match.ofNull((List<E> ignored) -> (Set<E>) emptySet())
        .or(l -> l.isEmpty(), ignored -> emptySet())
        .orDefault((List<E> l) -> insert(l.get(0), set_func(l.subList(1, l.size()))));
    return m.apply(list).get();
  }

}
