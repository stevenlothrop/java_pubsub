package com.stevenlothrop.concurrency;

public interface SubscriberSet<T> extends Listener<T>  {
    void on(Iterable<T> t);
}
