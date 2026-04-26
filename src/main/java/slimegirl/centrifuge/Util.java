package slimegirl.centrifuge;

import java.util.concurrent.Callable;

public class Util {
    public static class GraduallyTimer {
        private final int maxTicks;
        private final int perTicksOnFail;
        private int ticks = 0;
        private int currentMaxTicks = 0;

        public GraduallyTimer(int maxTicks, int perTicksOnFail) {
            this.maxTicks = maxTicks;
            this.perTicksOnFail = perTicksOnFail;
        }

        public GraduallyTimer(int maxTicks) {
            this.maxTicks = maxTicks;
            this.perTicksOnFail = 5;
        }

        public GraduallyTimer() {
            this.maxTicks = 20;
            this.perTicksOnFail = 5;
        }

        public boolean tick() {
            if (ticks < currentMaxTicks) {
                ticks++;
            } else {
                return true;
            }
            return false;
        }

        public void reset(boolean success) {
            ticks = 0;
            if (success) {
                currentMaxTicks = 0;
            } else {
                if (currentMaxTicks != maxTicks) {
                    currentMaxTicks += perTicksOnFail;
                    if (currentMaxTicks > maxTicks) {
                        currentMaxTicks = maxTicks;
                    }
                }
            }
        }

        public void reset(Callable<Boolean> task) {
            try {
                reset(task.call());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
