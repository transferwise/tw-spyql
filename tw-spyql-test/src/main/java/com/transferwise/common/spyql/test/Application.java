package com.transferwise.common.spyql.test;

import com.transferwise.common.spyql.event.StatementExecuteEvent;
import com.transferwise.common.spyql.rx.ObservableListener;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application implements CommandLineRunner {

  private static final Logger log = LoggerFactory.getLogger(Application.class);
  private static final int TIME_WINDOW_SECONDS = 10;

  private final FooRepository fooRepository;

  private final FooService fooService;

  private final ObservableListener rxListener;

  @Autowired
  public Application(FooRepository fooRepository, FooService fooService, ObservableListener rxListener) {
    this.fooRepository = fooRepository;
    this.fooService = fooService;
    this.rxListener = rxListener;
  }

  public static void main(String[] args) {
    new SpringApplication(Application.class).run(args);
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  @Override
  @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED")
  public void run(String... args) throws Exception {
    // Example usage of ObservableListener as RxJava Observable.
    // Counts all SQL statements executed in a 10 second time window.
    rxListener
        .ofType(StatementExecuteEvent.class)
        .map(StatementExecuteEvent::getSql)
        .window(TIME_WINDOW_SECONDS, TimeUnit.SECONDS)
        .flatMap(w -> w
            .groupBy(sql -> sql)
            .flatMap(gr -> gr.count()
                .map(count -> Pair.of(gr.getKey(), count))
                .toObservable())
        )
        .map(pair -> "sql: " + pair.getKey() + "; count: " + pair.getValue())
        .subscribe(System.out::println);

    logFooSize();
    fooService.addTwoFoosTransactionally();
    logFooSize();
    fooService.addOneWithNestedTwo();
    logFooSize();

    // Will send the onComplete signal to all Observers
    rxListener.close();

    // Wait a bit for async Observers to finish
    Thread.sleep(1000);
  }

  private void logFooSize() {
    log.info("foo size: {}", fooRepository.count());
  }
}
