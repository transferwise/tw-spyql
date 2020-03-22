package com.transferwise.common.spyql.rx;

import com.transferwise.common.spyql.event.GetConnectionEvent;
import com.transferwise.common.spyql.event.GetConnectionFailureEvent;
import com.transferwise.common.spyql.event.SpyqlEvent;
import com.transferwise.common.spyql.listener.SpyqlConnectionListener;
import com.transferwise.common.spyql.listener.SpyqlDataSourceListener;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

public class ObservableListener extends Observable<SpyqlEvent> implements SpyqlDataSourceListener {

  private static final int MAX_CONCURRENT_CONNECTIONS_DEFAULT = 5000;

  private Subject<SpyqlEvent> subject;
  private int maxConcurrentConnections;

  public ObservableListener() {
    this(MAX_CONCURRENT_CONNECTIONS_DEFAULT);
  }

  public ObservableListener(int maxConcurrentTransactions) {
    this(maxConcurrentTransactions, PublishSubject.create());
  }

  public ObservableListener(int maxConcurrentConnections, Subject<SpyqlEvent> subject) {
    this.subject = subject;
    this.maxConcurrentConnections = maxConcurrentConnections;
  }

  @Override
  protected void subscribeActual(Observer<? super SpyqlEvent> observer) {
    subject.subscribe(observer);
  }

  public void attachListener(SpyqlDataSourceListener listener) {
    subscribe(new ObserverToListenerAdapter(listener, maxConcurrentConnections));
  }

  public void attachAsyncListener(SpyqlDataSourceListener listener) {
    this.observeOn(Schedulers.newThread())
        .subscribe(new ObserverToListenerAdapter(listener, maxConcurrentConnections));
  }

  public void close() {
    subject.onComplete();
  }

  @Override
  public SpyqlConnectionListener onGetConnection(GetConnectionEvent event) {
    subject.onNext(event);
    return new ConnectionListener();
  }

  @Override
  public void onGetConnectionFailure(GetConnectionFailureEvent event) {
    subject.onNext(event);
  }

  class ConnectionListener implements SpyqlConnectionListener {

    @Override
    public void onEvent(SpyqlEvent event) {
      subject.onNext(event);
    }
  }
}
