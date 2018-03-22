package com.transferwise.spyql.test;

import com.transferwise.spyql.rx.ObservableListener;
import com.transferwise.spyql.rx.events.StatementExecuteEvent;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.observables.GroupedObservable;
import javafx.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.concurrent.TimeUnit;

@SpringBootApplication
public class Application implements CommandLineRunner {
	private static final Logger log = LoggerFactory.getLogger(Application.class);

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

	@Override
	public void run(String... args) throws Exception {
		// Example usage of ObservableListener as RxJava Observable.
		// Counts all SQL statements executed in a 10 second time window.
		rxListener
				.ofType(StatementExecuteEvent.class)
				.map(StatementExecuteEvent::getSql)
				.window(10, TimeUnit.SECONDS)
				.flatMap(w -> w
					.groupBy(sql -> sql)
					.flatMap(gr -> gr.count()
									.map(count -> new Pair<>(gr.getKey(), count))
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
