package me.diffusehyperion.inertiaanticheat.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Scheduler {
    private final HashMap<Runnable, Integer> tasksToRun = new HashMap<>();

    public void addTask(int ticksDelay, Runnable runnable) {
        tasksToRun.put(runnable, ticksDelay);
    }

    public void cancelTask(Runnable runnable) {
        tasksToRun.remove(runnable);
    }

    public void tick() {
        Iterator<Map.Entry<Runnable, Integer>> it = tasksToRun.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Runnable, Integer> entry = it.next();

            int newValue = entry.getValue() - 1;
            tasksToRun.put(entry.getKey(), newValue);

            if (newValue <= 0) {
                entry.getKey().run();
                it.remove();
            }
        }
    }
}
