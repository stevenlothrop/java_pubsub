package com.stevenlothrop.concurrency;

public interface Subscriber<T> extends Listener<T>{
    void on(T t);
}
