package com.stevenlothrop.concurrency;

import java.util.ArrayList;
import java.util.List;

public class PubSubThreads {
    private List<PubSubThread> threads = new ArrayList<>();
    public PubSubThread starterThread = new PubSubThread(t -> t.printStackTrace());

    public PubSubThread create(PubSubThread.ExceptionCallback exceptionCallback){
        PubSubThread e = new PubSubThread(exceptionCallback);
        threads.add(e);
        return e;
    }
    public PubSubThread createAndSwallowExceptions(){
        return create(t -> {});
    }
    public PubSubThread createAndSoutExceptions(){
        return create(t -> t.printStackTrace());
    }

    public void wrap(PubSubThread pubSubThread){
        threads.add(pubSubThread);
    }

    public void start(){
        threads.forEach(PubSubThread::start);
        starterThread.start();
    }

}
