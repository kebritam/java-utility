package io.github.kebritam.ratelimiter;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

public class BlockingLeakyBucketRateLimiter implements RateLimiter {

    long perRequest;
    AtomicLong nextAccessTime;

    public BlockingLeakyBucketRateLimiter(int rate) {
        this.perRequest = 1_000_000_000 / rate;
        this.nextAccessTime = new AtomicLong(0);
    }

    @Override
    public void Take() {
        long newNextAccessTime;
        long currentTime;
        long tempNextAccessTime;

        while (true) {
            currentTime = System.nanoTime();
            long currentNextAccessTime = this.nextAccessTime.get();

            if (currentTime - currentNextAccessTime > perRequest) {
                newNextAccessTime = currentTime;
            } else {
                newNextAccessTime = currentNextAccessTime + perRequest;
            }

            tempNextAccessTime = this.nextAccessTime.get();
            if (this.nextAccessTime.compareAndSet(currentNextAccessTime, newNextAccessTime)) {
                break;
            }
        }

        long sleepDuration = tempNextAccessTime - currentTime;
        if (sleepDuration > 0) {
            try {
                Thread.sleep(Duration.ofNanos(sleepDuration));
            } catch (InterruptedException ignore) {
            }
        }
    }
}