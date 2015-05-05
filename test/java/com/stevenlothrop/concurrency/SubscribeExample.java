package com.stevenlothrop.concurrency;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertTrue;

public class SubscribeExample {
    public static class PubSubChannels {
        public static final PubSubChannel<Boolean> BOOLEAN_PUB_SUB_CHANNEL = new PubSubChannel<>();
    }

    @Test
    public void simpleTest() throws InterruptedException {
        PubSubThreads pubSubThreads = new PubSubThreads();
        WrappedCountdownLatch wrappedCountdownLatch = new WrappedCountdownLatch(1);

        // Publish
        {
            PubSubThread pubSubThread = pubSubThreads.createAndSoutExceptions();
            pubSubThread.execute(() -> PubSubChannels.BOOLEAN_PUB_SUB_CHANNEL.publish(Boolean.TRUE));
        }

        // Subscribe
        {
            PubSubThread pubSubThread = pubSubThreads.createAndSoutExceptions();
            pubSubThread.subscribe(PubSubChannels.BOOLEAN_PUB_SUB_CHANNEL, wrappedCountdownLatch.on());
        }

        pubSubThreads.start();
        wrappedCountdownLatch.c.await();
        assertTrue(true);
    }

    public static class WrappedCountdownLatch {
        public CountDownLatch c;

        public WrappedCountdownLatch(int i) {
            this.c = new CountDownLatch(i);
        }

        public Subscriber<Boolean> on() {
            return aBoolean -> {
                c.countDown();
            };
        }
    }
}
