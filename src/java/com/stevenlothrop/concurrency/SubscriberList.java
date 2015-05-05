package com.stevenlothrop.concurrency;

public interface SubscriberList<T> extends Listener<T>  {
    void on(Iterable<T> t);
}
