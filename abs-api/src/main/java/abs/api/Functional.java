package abs.api;

import java.time.Clock;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Random;
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

  private static final Random RANDOM = new Random(Clock.systemUTC().millis());

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

  // --- Arithmetic

  public static <X> X max(X x, X y) {
    if (x instanceof Comparable == false) {
      return null;
    }
    Comparable<X> cx = (Comparable<X>) x;
    final int c = cx.compareTo(y);
    return c == 0 || c > 1 ? x : y;
  }

  public static <X> X min(X x, X y) {
    X max = max(x, y);
    return max == null ? null : max == x ? y : x;
  }

  public static int random(int bound) {
    return RANDOM.nextInt(bound);
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

  public static <E> List<E> remove(E e, List<E> list) {
    return (List<E>) removeCollection(e, list);
  }

  public static <E> List<E> list(Set<E> set) {
    final List<E> list = emptyList();
    if (set == null) {
      return list;
    }
    list.addAll(set);
    return list;
  }

  public static <E> boolean contains(List<E> list, E e) {
    return containsCollection(e, list);
  }

  public static <E> int size(List<E> list) {
    return sizeCollection(list);
  }

  public static <E> boolean isEmpty(List<E> list) {
    return isEmptyCollection(list);
  }

  public static <E> E get(List<E> list, int index) {
    if (index >= size(list)) {
      throw new IllegalArgumentException("Index is beyond list size: " + index);
    }
    return list.get(index);
  }

  public static <E> List<E> without(E e, List<E> list) {
    return remove(e, list);
  }

  public static <E> List<E> concatenate(List<E> list1, List<E> list2) {
    final List<E> result = emptyList();
    result.addAll(list1);
    result.addAll(list2);
    return result;
  }

  public static <E> List<E> appendRight(E e, List<E> list) {
    list.add(e);
    return list;
  }

  public static <E> List<E> reverse(List<E> list) {
    Collections.reverse(list);
    return list;
  }

  public static <E> List<E> copy(final E value, final int n) {
    return IntStream.range(0, n).mapToObj(i -> value).collect(Collectors.toList());
  }

  // --- Set

  public static <E> Set<E> emptySet() {
    return new HashSet<>();
  }

  public static <E> Set<E> insert(E e, Set<E> set) {
    return (Set<E>) insertCollection(e, set);
  }

  public static <E> Set<E> remove(E e, Set<E> set) {
    return (Set<E>) removeCollection(e, set);
  }

  public static <E> Set<E> set(List<E> list) {
    return set_java(list);
  }

  public static <E> boolean contains(Set<E> set, E e) {
    return containsCollection(e, set);
  }

  public static <E> int size(Set<E> set) {
    return sizeCollection(set);
  }

  public static <E> boolean isEmpty(Set<E> set) {
    return isEmptyCollection(set);
  }

  public static <E> E take(Set<E> set) {
    return next(set);
  }

  public static <E> boolean hasNext(Set<E> set) {
    return hastNext(set);
  }

  public static <E> Set<E> union(Set<E> set1, Set<E> set2) {
    final Set<E> union = emptySet();
    union.addAll(set1);
    union.addAll(set2);
    return union;
  }

  public static <E> Set<E> intersection(Set<E> set1, Set<E> set2) {
    final Set<E> inter = emptySet();
    inter.addAll(set1);
    inter.retainAll(set2);
    return inter;
  }

  public static <E> Set<E> difference(Set<E> set1, Set<E> set2) {
    final Set<E> diff = emptySet();
    diff.addAll(set1);
    diff.removeAll(set2);
    return diff;
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

  protected static <E> Collection<E> removeCollection(E e, Collection<E> col) {
    col.remove(e);
    return col;
  }

  protected static <E> boolean containsCollection(E e, Collection<E> col) {
    return col.contains(e);
  }

  protected static <E> int sizeCollection(Collection<E> col) {
    return col.size();
  }

  protected static <E> boolean isEmptyCollection(Collection<E> col) {
    return col.isEmpty();
  }

  protected static <E> E next(Iterable<E> it) {
    return it == null || it.iterator().hasNext() ? null : it.iterator().next();
  }

  protected static <E> boolean hastNext(Iterable<E> it) {
    return it == null ? false : it.iterator().hasNext();
  }

  protected static <E> Set<E> set_java(List<E> list) {
    Set<E> set = emptySet();
    if (list == null) {
      return set;
    }
    set.addAll(list);
    return set;
  }

  protected static <E> Set<E> set_func(List<E> list) {
    Match<List<E>, Set<E>> m = Match.ofNull((List<E> ignored) -> (Set<E>) emptySet())
        .or(l -> l.isEmpty(), ignored -> emptySet())
        .orDefault((List<E> l) -> insert(l.get(0), set_func(l.subList(1, l.size()))));
    return m.apply(list).get();
  }

}
