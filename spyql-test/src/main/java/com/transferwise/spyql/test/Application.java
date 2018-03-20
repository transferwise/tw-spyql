package com.transferwise.spyql.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application implements CommandLineRunner {
	private static final Logger log = LoggerFactory.getLogger(Application.class);

	private final FooRepository fooRepository;

	private final FooService fooService;

	@Autowired
	public Application(FooRepository fooRepository, FooService fooService) {
		this.fooRepository = fooRepository;
		this.fooService = fooService;
	}

	public static void main(String[] args) {
		new SpringApplication(Application.class).run(args);
	}

	@Override
	public void run(String... args) throws Exception {
		logFooSize();
		fooService.addTwoFoosTransactionally();
		logFooSize();
		fooService.addOneWithNestedTwo();
		logFooSize();

		Thread.sleep(1000);
	}

	private void logFooSize() {
		log.info("foo size: {}", fooRepository.count());
	}
}
