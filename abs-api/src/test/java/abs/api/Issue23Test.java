package abs.api;

import static com.google.common.truth.Truth.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import org.junit.Test;

import com.google.common.collect.Lists;

public class Issue23Test {

  static class TestActor implements abs.api.Actor {
    private static final long serialVersionUID = 1L;

    TestActor() throws Exception {
      Context context = Configuration.newConfiguration()
          .withExecutorService(Executors.newFixedThreadPool(4096)).buildContext();
      context.newActor(toString(), this);
    }


    private void busyWait() {
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        // Ignore
      }
    }

    private int x = 1;
    private final List<String> mObs = new ArrayList<>();
    private final List<String> rObs = new ArrayList<>();

    public void m() {
      mObs.add("m1");
      busyWait();
      mObs.add("m2");
      x = 0;
      mObs.add("m3");
      Supplier<Boolean> condition = () -> x == 1;
      await(this, condition);
      mObs.add("m4");
      busyWait();
    }

    public void run() {
      Runnable mRunnable = () -> m();
      send(this, mRunnable);
      rObs.add("r1");
      Supplier<Boolean> condition = () -> x == 0;
      await(this, condition).getValue();
      x = 1;
      rObs.add("r2");
      busyWait();
    }
  }

  @Test
  public void eachMessageExactlyOnceExecution() throws Exception {
    TestActor ta = new TestActor();
    ta.run();
    assertThat(ta.x).isEqualTo(1);
    assertThat(Lists.newArrayList("m1", "m2", "m3", "m4")).isEqualTo(ta.mObs);
    assertThat(Lists.newArrayList("r1", "r2")).isEqualTo(ta.rObs);
  }

}
