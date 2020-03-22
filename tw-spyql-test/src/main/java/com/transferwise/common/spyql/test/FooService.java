package com.transferwise.common.spyql.test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FooService {

  private final FooRepository fooRepository;

  @Autowired
  public FooService(FooRepository fooRepository) {
    this.fooRepository = fooRepository;
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public void addTwoFoosTransactionally() {
    fooRepository.save(new Foo("1"));
    fooRepository.save(new Foo("2"));
  }

  @Transactional
  public void addOneWithNestedTwo() {
    addTwoFoosTransactionally();
    fooRepository.save(new Foo("3"));
  }
}
