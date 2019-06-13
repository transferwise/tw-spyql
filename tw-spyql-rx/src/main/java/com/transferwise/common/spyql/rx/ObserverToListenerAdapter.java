package com.transferwise.common.spyql.rx;

import com.transferwise.common.spyql.event.*;
import com.transferwise.common.spyql.listener.SpyqlConnectionListener;
import com.transferwise.common.spyql.listener.SpyqlDataSourceListener;
import com.transferwise.common.spyql.utils.ConnectionListenersHelper;
import com.transferwise.common.spyql.utils.SimpleThrottler;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

class ObserverToListenerAdapter implements Observer<SpyqlEvent> {
    public static final int MAX_ERROR_COUNT = 1000;

    private static final Logger log = LoggerFactory.getLogger(ObserverToListenerAdapter.class);
    private static final int ERRORS_PER_MINUTE = 100;

    private int maxConcurrentConnections;

    private Map<Long, SpyqlConnectionListener> connectionListenerMap = new ConcurrentHashMap<>();
    private Disposable connectionDisposable;
    private AtomicInteger errorCount = new AtomicInteger(0);

    private SpyqlDataSourceListener listener;
    private ConnectionListenersHelper connectionListenersHelper;

    ObserverToListenerAdapter(SpyqlDataSourceListener listener, int maxConcurrentConnections) {
        if (listener == null) {
            throw new IllegalArgumentException("listener cannot be null");
        }
        this.listener = listener;
        this.maxConcurrentConnections = maxConcurrentConnections;
        this.connectionListenersHelper = new ConnectionListenersHelper(new SimpleThrottler(Duration.ofMinutes(1), ERRORS_PER_MINUTE));
    }

    @Override
    public void onSubscribe(Disposable d) {
        connectionDisposable = d;
    }

    @Override
    public void onNext(SpyqlEvent event) {
        if (event instanceof GetConnectionEvent) {
            GetConnectionEvent e = (GetConnectionEvent) event;
            SpyqlConnectionListener connectionListener = listener.onGetConnection(e);
            if (connectionListenerMap.size() < maxConcurrentConnections) {
                if (connectionListener == null) {
                    connectionListener = new NoopConnectionListener();
                }
                connectionListenerMap.put(e.getConnectionId(), connectionListener);
            } else {
                log.error("Trying to create more than {} connections", maxConcurrentConnections);
                registerErrorAndDetachIfNeeded();
            }
        } else if (event instanceof GetConnectionFailureEvent) {
            listener.onGetConnectionFailure((GetConnectionFailureEvent) event);
        } else if (event instanceof ConnectionCloseEvent) {
            ConnectionCloseEvent e = (ConnectionCloseEvent) event;
            SpyqlConnectionListener connectionListener = connectionListenerMap.get(e.getConnectionId());
            if (connectionListener != null) {
                connectionListenerMap.remove(e.getConnectionId());
                connectionListenersHelper.onEvent(connectionListener, e);
            } else {
                logConnectionErrorAndDetachIfNeeded(e);
            }
        } else if (event instanceof ConnectionCloseFailureEvent) {
            ConnectionCloseFailureEvent e = (ConnectionCloseFailureEvent) event;
            SpyqlConnectionListener connectionListener = connectionListenerMap.get(e.getConnectionId());
            if (connectionListener != null) {
                connectionListenerMap.remove(e.getConnectionId());
                connectionListenersHelper.onEvent(connectionListener, e);
            } else {
                logConnectionErrorAndDetachIfNeeded(e);
            }
        } else if (event instanceof ConnectionEvent) {
            ConnectionEvent e = (ConnectionEvent) event;
            SpyqlConnectionListener connectionListener = connectionListenerMap.get(e.getConnectionId());
            if (connectionListener != null) {
                connectionListenersHelper.onEvent(connectionListener, e);
            } else {
                logConnectionErrorAndDetachIfNeeded(e);
            }
        }
    }

    private void logConnectionErrorAndDetachIfNeeded(SpyqlEvent e) {
        log.error("{} was received but no connection with id {} found", e, e.getConnectionId());
        registerErrorAndDetachIfNeeded();
    }

    @Override
    public void onError(Throwable e) {
    }

    @Override
    public void onComplete() {
    }

    public int getConnectionListenerMapSize() {
        return connectionListenerMap.size();
    }

    private void registerErrorAndDetachIfNeeded() {
        if (errorCount.incrementAndGet() > MAX_ERROR_COUNT) {
            log.error("Number of errors surpassed {}", MAX_ERROR_COUNT);
            detach();
        }
    }

    private void detach() {
        log.info("Detaching the observer");
        connectionDisposable.dispose();
    }

}
