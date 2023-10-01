package io.github.ithaml.itio.context;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * @author: ken.lin
 * @since: 2023-09-30 18:37
 */
public class MsgLatch {

    private final ConcurrentHashMap<Class<?>, Thread> waitThreadMap = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<Thread, Object> signalResultMap = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public <T> T await(Class<T> msgClass, int timeout, TimeUnit unit) {
        Thread thread = Thread.currentThread();
        waitThreadMap.put(msgClass, thread);
        if (timeout > 0) {
            LockSupport.parkNanos(unit.toNanos(timeout));
        } else {
            LockSupport.park();
        }
        Object result = signalResultMap.remove(thread);
        waitThreadMap.remove(msgClass);
        if (result instanceof Throwable) {
            if (result instanceof RuntimeException) {
                throw (RuntimeException) result;
            } else {
                throw new RuntimeException((Throwable) result);
            }
        }
        return (T) result;
    }

    public boolean signal(Object msg) {
        for (Map.Entry<Class<?>, Thread> entry : waitThreadMap.entrySet()) {
            Class<?> clazz = entry.getKey();
            Thread thread = entry.getValue();
            if (clazz.isAssignableFrom(msg.getClass())) {
                signalResultMap.put(thread, msg);
                LockSupport.unpark(thread);
                return true;
            }
        }
        return false;
    }

    public boolean signal(Throwable exception) {
        for (Map.Entry<Class<?>, Thread> entry : waitThreadMap.entrySet()) {
            Thread thread = entry.getValue();
            signalResultMap.put(thread, exception);
            LockSupport.unpark(thread);
        }
        return !waitThreadMap.isEmpty();
    }
}
