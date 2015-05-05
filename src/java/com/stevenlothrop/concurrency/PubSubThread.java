package com.stevenlothrop.concurrency;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class PubSubThread {
    private final Thread thread;
    private final Lock lock = new ReentrantLock();
    private final Condition waiter = lock.newCondition();

    private boolean isRunning = true;
    private List<Runnable> runnables = new ArrayList<>();

    private boolean finalCallbackEnabled = false;
    private Runnable finalRunnable;

    public PubSubThread(ExceptionCallback exceptionCallback) {
        this.thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (isRunning) {
                    List<Runnable> toExecute;
                    List<Runnable> buffer = new ArrayList<>();
                    try {
                        lock.lock();
                        while (runnables.isEmpty() && isRunning) {
                            try {
                                waiter.await();
                            } catch (InterruptedException e) {
                                exceptionCallback.on(e);
                            }
                        }
                        toExecute = runnables;
                        runnables = buffer;
                    } finally {
                        lock.unlock();
                    }

                    for (Runnable aToExecute : toExecute) {
                        try {
                            aToExecute.run();
                        } catch (Throwable t) {
                            exceptionCallback.on(t);
                        }
                    }
                    if (finalCallbackEnabled) {
                        try {
                            finalRunnable.run();
                        } catch (Throwable t) {
                            exceptionCallback.on(t);
                        }
                    }
                }
            }
        });
    }

    public PubSubThread withFinalRunnable(Runnable finalRunnable) {
        this.finalRunnable = finalRunnable;
        this.finalCallbackEnabled = true;
        return this;
    }

    public void execute(Runnable r) {
        try {
            lock.lock();
            runnables.add(r);
            waiter.signal();
        } finally {
            lock.unlock();
        }
    }

    public void execute(Iterable<Runnable> rs) {
        try {
            lock.lock();
            for (Runnable r : rs) {
                runnables.add(r);
            }
            waiter.signal();
        } finally {
            lock.unlock();
        }
    }

    public void start() {
        thread.start();
    }

    public void dispose() {
        try {
            lock.lock();
            isRunning = false;
            waiter.signal();
        } finally {
            lock.unlock();
        }
    }

    public <T> void subscribe(PubSubChannel<T> pubSubChannel, Subscriber<T> on) {
        pubSubChannel.addSubscription(new Subscription<T>(this, on));
    }

    public <T> void subscribeLast(PubSubChannel<T> pubSubChannel, Subscriber<T> on) {
        pubSubChannel.addSubscription(new SubscriptionLast<T>(this, on));
    }


    public <T> void subscribeList(PubSubChannel<T> pubSubChannel, SubscriberList<T> on) {
        pubSubChannel.addSubscription(new SubscriptionList<T>(this, on));
    }

    public <T> void subscribeSet(PubSubChannel<T> pubSubChannel, SubscriberSet<T> on, HashKeyFunction<T> hashCodeFunction) {
        pubSubChannel.addSubscription(new SubscriptionHashSet<T>(this, on, hashCodeFunction));
    }

    public interface ExceptionCallback {
        void on(Throwable t);
    }

    public static abstract class SubscriptionBase<T> {
        abstract void transferWorkToSubscriberThread(T t);

        abstract void transferWorkToSubscriberThread(Iterable<T> t);
    }

    public static class Subscription<T> extends SubscriptionBase<T> {
        private PubSubThread pubSubThread;
        private Subscriber run;

        public <T> Subscription(PubSubThread pubSubThread, Subscriber<T> run) {
            this.pubSubThread = pubSubThread;
            this.run = run;
        }

        @Override
        void transferWorkToSubscriberThread(T t) {
            pubSubThread.execute(() -> run.on(t));
        }

        @Override
        void transferWorkToSubscriberThread(Iterable<T> ts) {
            List<Runnable> rs = new ArrayList<>();
            for (T t : ts) {
                rs.add(() -> run.on(t));
            }
            pubSubThread.execute(rs);
        }
    }

    public static class SubscriptionLast<T> extends SubscriptionBase<T> {
        private final Subscriber run;

        private Lock lock = new ReentrantLock();

        private boolean enqueuedOnSubscriberThread = false;
        private T t = null;

        private final PubSubThread pubSubThread;

        public <T> SubscriptionLast(PubSubThread pubSubThread, Subscriber<T> run) {
            this.pubSubThread = pubSubThread;
            this.run = run;
        }

        @Override
        void transferWorkToSubscriberThread(T t) {
            try {
                lock.lock();
                this.t = t;
                if (!enqueuedOnSubscriberThread) {
                    pubSubThread.execute(doWorkOnSubscribingThread());
                    enqueuedOnSubscriberThread = true;
                }
            } finally {
                lock.unlock();
            }
        }

        @Override
        void transferWorkToSubscriberThread(Iterable<T> t) {
            try {
                lock.lock();
                for (T aT : t) {
                    this.t = aT;
                }
                if (!enqueuedOnSubscriberThread) {
                    pubSubThread.execute(doWorkOnSubscribingThread());
                    enqueuedOnSubscriberThread = true;
                }
            } finally {
                lock.unlock();
            }
        }

        public Runnable doWorkOnSubscribingThread() {
            return () -> {
                T currentT;
                try {
                    lock.lock();
                    currentT = t;
                    t = null;
                    enqueuedOnSubscriberThread = false;
                } finally {
                    lock.unlock();
                }
                if (currentT != null) {
                    run.on(currentT);
                }
            };
        }
    }

    public static class SubscriptionList<T> extends SubscriptionBase<T> {
        private final PubSubThread pubSubThread;

        private final SubscriberList run;
        private Lock lock = new ReentrantLock();

        private boolean enqueuedOnSubscriberThread = false;
        private List<T> ts = new ArrayList<>();

        public <T> SubscriptionList(PubSubThread pubSubThread, SubscriberList<T> run) {
            this.pubSubThread = pubSubThread;
            this.run = run;
        }

        @Override
        void transferWorkToSubscriberThread(T t) {
            try {
                lock.lock();
                ts.add(t);
                if (!enqueuedOnSubscriberThread) {
                    pubSubThread.execute(doWorkOnSubscribingThread());
                    enqueuedOnSubscriberThread = true;
                }
            } finally {
                lock.unlock();
            }
        }

        @Override
        void transferWorkToSubscriberThread(Iterable<T> t) {
            try {
                lock.lock();
                for (T aT : t) {
                    ts.add(aT);
                }
                if (!enqueuedOnSubscriberThread) {
                    pubSubThread.execute(doWorkOnSubscribingThread());
                    enqueuedOnSubscriberThread = true;
                }
            } finally {
                lock.unlock();
            }
        }

        public Runnable doWorkOnSubscribingThread() {
            return () -> {
                List<T> currentTs;
                List<T> newTs = new ArrayList<>();
                try {
                    lock.lock();
                    currentTs = ts;
                    ts = newTs;
                    enqueuedOnSubscriberThread = false;
                } finally {
                    lock.unlock();
                }
                if (currentTs != null) {
                    run.on(currentTs);
                }
            };
        }
    }

    public static class SubscriptionHashSet<T> extends SubscriptionBase<T> {
        private final SubscriberSet run;

        private Lock lock = new ReentrantLock();

        private boolean enqueuedOnSubscriberThread = false;
        private Map<String, T> ts = new HashMap<>();

        private final PubSubThread pubSubThread;
        private final HashKeyFunction hashKeyFunction;

        public <T> SubscriptionHashSet(PubSubThread pubSubThread, SubscriberSet<T> run, HashKeyFunction<T> hashKeyFunction) {
            this.pubSubThread = pubSubThread;
            this.run = run;
            this.hashKeyFunction = hashKeyFunction;
        }

        @Override
        void transferWorkToSubscriberThread(T t) {
            try {
                lock.lock();
                String key = hashKeyFunction.hashKey(t);
                if (key != null) {
                    ts.put(key, t);
                    if (!enqueuedOnSubscriberThread) {
                        pubSubThread.execute(doWorkOnSubscribingThread());
                        enqueuedOnSubscriberThread = true;
                    }
                }
            } finally {
                lock.unlock();
            }
        }

        @Override
        void transferWorkToSubscriberThread(Iterable<T> t) {
            try {
                lock.lock();
                boolean setAValue = false;
                for (T aT : t) {
                    String key = hashKeyFunction.hashKey(aT);
                    if(key != null){
                        ts.put(key, aT);
                        setAValue = true;
                    }
                }
                if (setAValue && !enqueuedOnSubscriberThread) {
                    pubSubThread.execute(doWorkOnSubscribingThread());
                    enqueuedOnSubscriberThread = true;
                }
            } finally {
                lock.unlock();
            }
        }

        public Runnable doWorkOnSubscribingThread() {
            return () -> {
                Map<String, T> currentTs;
                Map<String, T> newTs = new HashMap<>();
                try {
                    lock.lock();
                    currentTs = ts;
                    ts = newTs;
                    enqueuedOnSubscriberThread = false;
                } finally {
                    lock.unlock();
                }
                if (currentTs != null) {
                    run.on(currentTs.values());
                }
            };
        }
    }

    public interface HashKeyFunction<T> {
        String hashKey(T t);
    }
}
