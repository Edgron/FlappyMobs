package com.omniblock.flappymobs.flight;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public class FlightSession {

    private final Player player;
    private final Flight flight;
    private final Entity creature;
    private int currentWaypointIndex;
    private double xPerTick;
    private double yPerTick;
    private double zPerTick;
    private BukkitTask movementTask;

    public FlightSession(Player player, Flight flight, Entity creature) {
        this.player = player;
        this.flight = flight;
        this.creature = creature;
        this.currentWaypointIndex = 0;
    }

    public Player getPlayer() {
        return player;
    }

    public Flight getFlight() {
        return flight;
    }

    public Entity getCreature() {
        return creature;
    }

    public int getCurrentWaypointIndex() {
        return currentWaypointIndex;
    }

    public void setCurrentWaypointIndex(int index) {
        this.currentWaypointIndex = index;
    }

    public double getXPerTick() {
        return xPerTick;
    }

    public void setXPerTick(double xPerTick) {
        this.xPerTick = xPerTick;
    }

    public double getYPerTick() {
        return yPerTick;
    }

    public void setYPerTick(double yPerTick) {
        this.yPerTick = yPerTick;
    }

    public double getZPerTick() {
        return zPerTick;
    }

    public void setZPerTick(double zPerTick) {
        this.zPerTick = zPerTick;
    }

    public BukkitTask getMovementTask() {
        return movementTask;
    }

    public void setMovementTask(BukkitTask task) {
        this.movementTask = task;
    }

    public void cancel() {
        if (movementTask != null && !movementTask.isCancelled()) {
            movementTask.cancel();
        }
    }
}