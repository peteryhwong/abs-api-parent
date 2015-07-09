package abs.api;

import static com.google.common.truth.Truth.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.Test;

import com.google.common.collect.Lists;

import abs.api.Functional.Match;

public class FunctionalTest {

  @Test
  public void matchInteger() throws Exception {
    Match<Integer, Boolean> match = Match.of((Integer i) -> i > 0, i -> true)
        .or(i -> i < 0, i -> false).or(i -> i == 0, i -> null);

    Optional<Boolean> o0 = match.apply(0);
    assertThat(o0).isNotNull();
    assertThat(o0.isPresent()).isFalse();

    Optional<Boolean> o1 = match.apply(1);
    assertThat(o1).isNotNull();
    assertThat(o1.isPresent()).isTrue();
    assertThat(o1.get()).isTrue();

    Optional<Boolean> o2 = match.apply(-1);
    assertThat(o2).isNotNull();
    assertThat(o2.isPresent()).isTrue();
    assertThat(o2.get()).isFalse();

  }

  @Test
  public void setFromList() throws Exception {
    List<Long> list = null;
    Set<Long> result = Functional.set(list);
    assertThat(result).isNotNull();
    assertThat(result).isEmpty();

    list = new ArrayList<>();
    result = Functional.set(list);
    assertThat(result).isNotNull();
    assertThat(result).isEmpty();

    list = Lists.newArrayList(1L, 2L, 3L);
    result = Functional.set(list);
    assertThat(result).isNotNull();
    assertThat(result).isNotEmpty();
    assertThat(result).hasSize(3);
    assertThat(result).containsAllIn(list);

    list = Lists.newArrayList(3L, 1L, 2L, 3L);
    result = Functional.set(list);
    assertThat(result).isNotNull();
    assertThat(result).isNotEmpty();
    assertThat(result).hasSize(3);
    assertThat(result).containsAnyIn(list);
  }

}
