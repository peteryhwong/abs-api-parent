package abs.api;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * ABS Functional layer as Java 8 API.
 */
public final class Functional {

  private static final String ANSI_RESET = "\u001B[0m";
  private static final String ANSI_GREEN = "\u001B[32m";
  private static final String ANSI_RED = "\u001B[31m";
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

    static <X, Y> Match<X, Y> equals(X x, Y y) {
      Predicate<X> cond = input -> Objects.equals(x, input);
      Function<X, Y> what = ignored -> y;
      return of(cond, what);
    }

    default Match<X, Y> orDefault(Function<X, Y> what) {
      return or(ignored -> true, what);
    }

    default Match<X, Y> orEquals(X x, Y y) {
      Predicate<X> cond = input -> Objects.equals(x, input);
      Function<X, Y> what = ignored -> y;
      return or(cond, what);
    }

    default Match<X, Y> or(Predicate<X> cond, Function<X, Y> what) {
      Match<X, Y> orM = input -> {
        Optional<Y> thisMatch = apply(input);
        return thisMatch.isPresent() ? thisMatch : of(cond, what).apply(input);
      };
      return orM;
    }

  }

  /**
   * An extension over {@link Map.Entry}
   * 
   * @param <X>
   * @param <Y>
   */
  public static interface Pair<X, Y> extends Map.Entry<X, Y> {

    /**
     * An implementation of {@link Pair} using
     * {@link SimpleEntry}.
     * 
     * @param <X>
     * @param <Y>
     */
    static class SimplePair<X, Y> extends AbstractMap.SimpleEntry<X, Y>implements Pair<X, Y> {
      private static final long serialVersionUID = 1L;

      /**
       * Ctor.
       * 
       * @param key the first element
       * @param value the second element
       */
      public SimplePair(X key, Y value) {
        super(key, value);
      }
    }

    public static <A, B> Pair<A, B> newPair(A a, B b) {
      return new SimplePair<A, B>(a, b);
    }

    default X getFirst() {
      return getKey();
    }

    default Y getSecond() {
      return getValue();
    }

    default void setSecond(Y second) {
      setValue(second);
    }

  }
  
  // -- Pair Constructor
  
  

  // --- Arithmetic

  public static <X> X max(X x, X y) {
    if (x instanceof Comparable == false) {
      return Objects.equals(x, y) ? x : null;
    }
    Comparable<X> cx = (Comparable<X>) x;
    final int c = cx.compareTo(y);
    return c >= 0 ? x : y;
  }

  public static <X> X min(X x, X y) {
    X max = max(x, y);
    return max == null ? null : max == x ? y : x;
  }

  public static int random(int bound) {
    return RANDOM.nextInt(bound);
  }

  // --- Logical

  public static boolean and(final boolean a, final boolean b) {
    return a && b;
  }

  public static boolean not(final boolean a) {
    return !a;
  }

  public static boolean or(final boolean a, final boolean b) {
    return a || b;
  }

  // --- List

  /**
   * Same as {@link #emptyList()}.
   * 
   * @return a new empty {@link List}.
   * @deprecated Use {@link #emptyList()}.
   */
  public static <E> List<E> EmptyList() {
    return emptyList();
  }

  public static <E> List<E> Nil() {
    return emptyList();
  }

  public static <E> List<E> emptyList() {
    return new LinkedList<>();
  }

  public static <E> List<E> insert(E e, List<E> list) {
    if (list == null) {
      list = emptyList();
    }
    return (List<E>) insertCollection(e, list);
  }

  /**
   * Same as {@link #insert(Object, List)}
   * 
   * @param list the list
   * @param e the element
   * @return the updated list
   * @deprecated Use {@link #insert(Object, List)}
   */
  public static <E> List<E> insertElement(List<E> list, E e) {
    return (List<E>) insertCollection(e, list);
  }

  public static <E> List<E> remove(List<E> list, E e) {
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

  public static <E> List<E> list(E... args) {
    if (args == null || args.length == 0) {
      return emptyList();
    }
    List<E> asList = Arrays.asList(args);
    List<E> result = emptyList();
    result.addAll(asList);
    return result;
  }

  public static <E> boolean contains(List<E> list, E e) {
    return containsCollection(e, list);
  }

  public static <E> int size(List<E> list) {
    return sizeCollection(list);
  }

  public static <E> int length(List<E> list) {
    return size(list);
  }

  public static <E> boolean isEmpty(List<E> list) {
    return isEmptyCollection(list);
  }

  /**
   * Same as {@link #isEmpty(List)}
   * 
   * @param list the list
   * @return <code>true</code> if the list is empty; otherwise
   *         <code>false</code>
   * @deprecated Use {@link #isEmpty(List)}
   */
  public static <E> boolean emptyList(List<E> list) {
    return isEmptyCollection(list);
  }

  public static <E> E get(List<E> list, int index) {
    if (index >= size(list)) {
      throw new IllegalArgumentException("Index out of bound: " + index);
    }
    return list.get(index);
  }

  public static <E> List<E> without(List<E> list, E e) {
    return remove(list, e);
  }

  public static <E> List<E> concatenate(List<E> list1, List<E> list2) {
    final List<E> result = emptyList();
    result.addAll(list1);
    result.addAll(list2);
    return result;
  }

  public static <E> List<E> appendRight(List<E> list, E e) {
    return (List<E>) insertCollection(e, list);
  }

  public static <E> List<E> reverse(List<E> list) {
    Collections.reverse(list);
    return list;
  }

  public static <E> List<E> copy(final E value, final int n) {
    return IntStream.range(0, n).mapToObj(i -> value).collect(Collectors.toList());
  }

  public static <E> List<E> tail(List<E> list) {
    if (list == null || list.isEmpty()) {
      return emptyList();
    }
    return list.subList(1, list.size());
  }

  public static <E> E head(List<E> list) {
    if (list == null || list.isEmpty()) {
      return null;
    }
    return list.get(0);
  }

  /**
   * Same as {@link #insert(Object, List)}.
   * 
   * @param e the element
   * @param list the list
   * @return the updated list
   * @deprecated Please {@link #insert(Object, List)}.
   */
  public static <E> List<E> Cons(E e, List<E> list) {
    return insert(e, list);
  }

  // --- Set

  /**
   * Same as {@link #emptySet()}.
   * 
   * @return a new empty {@link Set}
   * @deprecated Use {@link #EmptySet()}.
   */
  public static <E> Set<E> EmptySet() {
    return emptySet();
  }

  public static <E> Set<E> emptySet() {
    return new HashSet<>();
  }

  public static <E> Set<E> insert(E e, Set<E> set) {
    if (set == null) {
      set = emptySet();
    }
    return (Set<E>) insertCollection(e, set);
  }

  /**
   * Same as {@link #insert(Object, Set)}
   * 
   * @param set the set
   * @param e the element
   * @return the updated set
   * @deprecated Use {@link #insert(Object, Set)}
   */
  public static <E> Set<E> insertElement(Set<E> set, E e) {
    return (Set<E>) insertCollection(e, set);
  }

  public static <E> Set<E> remove(Set<E> set, E e) {
    return (Set<E>) removeCollection(e, set);
  }

  public static <E> Set<E> set(List<E> list) {
    return set_java(list);
  }

  public static <E> boolean contains(E e, Set<E> set) {
    return containsCollection(e, set);
  }

  public static <E> int size(Set<E> set) {
    return sizeCollection(set);
  }

  public static <E> int length(Set<E> set) {
    return size(set);
  }

  public static <E> boolean isEmpty(Set<E> set) {
    return isEmptyCollection(set);
  }

  /**
   * Same as {@link #isEmpty(Set)}
   * 
   * @param set the set
   * @return <code>true</code> if the set is empty; otherwise
   *         <code>false</code>
   * @deprecated Use {@link #isEmpty(Set)}
   */
  public static <E> boolean emptySet(Set<E> set) {
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

  /**
   * Same as {@link #emptyMap()}
   * 
   * @return a new empty {@link Map}
   * @deprecated Use {@link #emptyMap()}.
   */
  public static <K, V> Map<K, V> EmptyMap() {
    return emptyMap();
  }
  

  public static <K, V> Map<K, V> emptyMap() {
    return new ConcurrentHashMap<>();
  }

  public static <K, V> Map<K, V> insert(Map<K, V> map, K key, V value) {
    if (map == null) {
      map = emptyMap();
    }
    map.put(key, value);
    return map;
  }

  public static <K, V> Map<K, V> insert(Map<K, V> map, Pair<K,V> pair ) {
    return insert(pair, map);
  }
  
  public static <K, V> Map<K, V> insert(Entry<K, V> pair, Map<K, V> map) {
    return insert(map, pair.getKey(), pair.getValue());
  }

  public static <K, V> Map<K, V> put(Map<K, V> map, K key, V value) {
    return insert(map, key, value);
  }

  /**
   * Creates a new {@link Pair}.
   * 
   * @param key the first element
   * @param value the second element
   * @return an instance of {@link Pair}
   * @deprecated Try to use {@link #pair(Object, Object)} which
   *             uses standard Java's {@link Entry}.
   */
  public static <K, V> Pair<K, V> Pair(K key, V value) {
    return Pair.newPair(key, value);
  }

  /**
   * Returns the first element in a {@link Pair}.
   * 
   * @param pair the pair instance
   * @return the first element
   * @deprecated Usage is not recommended.
   */
  public static <K, V> K fst(Entry<K, V> pair) {
    return pair.getKey();
  }

  /**
   * Retrieves the second element in a {@link Pair}.
   * 
   * @param pair the pair instance
   * @return the second element
   * @deprecated Usage is not recommended.
   */
  public static <K, V> V snd(Entry<K, V> pair) {
    return pair.getValue();
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

  public static <K, V> Map<K, V> removeKey(Map<K, V> map, K key) {
    map.remove(key);
    return map;
  }

  public static <K, V, C extends Collection<K>> C keys(Map<K, V> map) {
    return (C) map.keySet();
  }

  public static <K, V> Collection<V> values(Map<K, V> map) {
    return map.values();
  }

  public static <K, V> Optional<V> lookup(Map<K, V> map, K key) {
    return Optional.ofNullable(map.get(key));
  }

  public static <K, V> V lookupDefault(Map<K, V> map, K key, V defaultValue) {
    return lookup(map, key).orElse(defaultValue);
  }

  public static <K, V> V lookupUnsafe(Map<K, V> map, K key) {
    return lookup(map, key).orElse(null);
  }

  // --- Strings

  public static String substr(String s, int beginIndex, int length) {
    return s.substring(beginIndex, beginIndex + length);
  }

  public static int strlen(String s) {
    return s.length();
  }

  public static String toString(Object o) {
    return Objects.toString(o, "null");
  }

  public static String concatenate(String s1, String s2) {
    return new StringBuilder().append(s1).append(s2).toString();
  }

  // --- I/O

  public static void println(String format, Object... args) {
    System.out.println(String.format(format, args));
  }

  public static void println(Object o) {
    System.out.println(o);
  }

  public static void print(Object o) {
    System.out.print(o);
  }

  /**
   * Print a boolean with ANSI color support. This is a specific
   * method with <code>GREEN</code> and <code>RED</code> color
   * support on ANSI terminal. Use with caution.
   * 
   * @param bool the value
   */
  public static void println(final boolean bool) {
    if (bool) {
      println("%sTRUE%s", ANSI_GREEN, ANSI_RESET);
    } else {
      println("%sFALSE%s", ANSI_RED, ANSI_RESET);
    }
  }

  public static String readln() {
    return System.console().readLine();
  }

  // --- Time

  public static Duration duration(long duration, TimeUnit unit) {
    return Duration.ofMillis(unit.toMillis(duration));
  }

  public static Duration duration(long millis) {
    return Duration.ofMillis(millis);
  }

  public static boolean durationLessThan(Duration d1, Duration d2) {
    return d1.compareTo(d2) < 0;
  }

  public static Duration subtractFromDuration(Duration d, long v) {
    if (isDurationInfinite(d)) {
      return d;
    }
    Duration d2 = d.minusMillis(v);
    return d2.isNegative() || d2.isZero() ? d : d2;
  }

  public static Duration infinity() {
    return ChronoUnit.FOREVER.getDuration();
  }

  public static boolean isDurationInfinite(Duration d) {
    return d == null || infinity().equals(d);
  }

  public static long currentms() {
    return ContextClock.CLOCK.millis();
  }

  public static Instant now() {
    return ContextClock.CLOCK.instant();
  }

  public static long timeDifference(Instant t1, Instant t2) {
    return Duration.between(t1, t2).abs().toMillis();
  }

  public static Instant addDuration(Instant t, Duration d) {
    return t.plus(d);
  }

  public static Instant subtractDuration(Instant t, Duration d) {
    return t.minus(d);
  }

  public static Duration lowlevelDeadline(Long millis) {
    return millis == null || millis <= 0 ? infinity() : duration(millis);
  }

  public static Duration deadline(Duration d) {
    return d;
  }

  public static Duration deadline(Long millis) {
    return lowlevelDeadline(millis);
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
    return col == null ? 0 : col.size();
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
