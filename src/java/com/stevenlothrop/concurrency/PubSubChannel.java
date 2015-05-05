package com.stevenlothrop.concurrency;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class PubSubChannel<T> implements Publisher<T>{

    private final List<PubSubThread.SubscriptionBase<T>> subscriptions = new ArrayList<>();
    private final ReentrantReadWriteLock reentrantReadWriteLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock read = reentrantReadWriteLock.readLock();
    private final ReentrantReadWriteLock.WriteLock write = reentrantReadWriteLock.writeLock();

    @Override
    public void publish(T t) {
        try {
            read.lock();
            for (PubSubThread.SubscriptionBase<T> subscription : subscriptions) {
                subscription.transferWorkToSubscriberThread(t);
            }
        } finally{
            read.unlock();
        }
    }

    @Override
    public void publish(Iterable<T> t) {
        try {
            read.lock();
            for (PubSubThread.SubscriptionBase<T> subscription : subscriptions) {
                subscription.transferWorkToSubscriberThread(t);
            }
        } finally{
            read.unlock();
        }
    }

    void addSubscription(PubSubThread.SubscriptionBase<T> on) {
        try{
            write.lock();
            subscriptions.add(on);
        }finally{
            write.unlock();
        }
    }

}
