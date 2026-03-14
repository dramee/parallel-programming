package org.nsu.syspro.parprog;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.nsu.syspro.parprog.base.DefaultFork;
import org.nsu.syspro.parprog.base.DiningTable;
import org.nsu.syspro.parprog.examples.DefaultPhilosopher;
import org.nsu.syspro.parprog.helpers.TestLevels;
import org.nsu.syspro.parprog.interfaces.Fork;
import org.nsu.syspro.parprog.interfaces.Philosopher;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class CustomSchedulingTest extends TestLevels {

    static final class CustomizedPhilosopher extends DefaultPhilosopher {
        @Override
        public void onHungry(Fork left, Fork right) {
            sleepMillis(this.id * 20);
            System.out.println(Thread.currentThread() + " " + this + ": onHungry");
            super.onHungry(left, right);
        }
    }

    static final class CustomizedFork extends DefaultFork {
        @Override
        public void acquire() {
            System.out.println(Thread.currentThread() + " trying to acquire " + this);
            super.acquire();
            System.out.println(Thread.currentThread() + " acquired " + this);
            sleepMillis(100);
        }
    }

    static final class CustomizedTable extends DiningTable<CustomizedPhilosopher, CustomizedFork> {
        public CustomizedTable(int N) {
            super(N);
        }

        @Override
        public CustomizedFork createFork() {
            return new CustomizedFork();
        }

        @Override
        public CustomizedPhilosopher createPhilosopher() {
            return new CustomizedPhilosopher();
        }
    }

    static final class SlowPhilosopher extends DefaultPhilosopher {
        @Override
        public void onHungry(Fork left, Fork right) {
            left.acquire();
            try {
                right.acquire();
                try {
                    countMeal();
                    sleepSeconds(1);
                } finally {
                    right.release();
                }
            }finally {
                left.release();
            }
        }
    }

    static final class TableWithOneSlowPhilosopher extends DiningTable<DefaultPhilosopher, DefaultFork> {
        private int counter = 0;
        public TableWithOneSlowPhilosopher(int N) {
            super(N);
        }

        @Override
        public DefaultFork createFork() {
            return new DefaultFork();
        }

        @Override
        public DefaultPhilosopher createPhilosopher() {
            if (counter == 0) {
                counter++;
                return new SlowPhilosopher();
            } else {
                return new DefaultPhilosopher();
            }
        }
    }

    @EnabledIf("easyEnabled")
    @ParameterizedTest
    @ValueSource(ints = {2, 3, 4, 5})
    @Timeout(2)
    void testDeadlockFreedom(int N) {
        final CustomizedTable table = dine(new CustomizedTable(N), 1);
    }

    @EnabledIf("easyEnabled")
    @ParameterizedTest
    @ValueSource(ints = {4, 5, 6, 7, 8})
    @Timeout(10)
    void testSingleSlow(int N) {
        final TableWithOneSlowPhilosopher table = dine(new TableWithOneSlowPhilosopher(N), 5);
        assertTrue(table.maxMeals() >= 1000, "Should be more than 1000, but " + table.maxMeals());
    }
}
