package com.omniblock.flappymobs.flight;

import org.bukkit.entity.EntityType;
import java.util.ArrayList;
import java.util.List;

public class Flight {
    private final String name;
    private EntityType creature;
    private double cost;
    private boolean invulnerable;
    private int parachuteTime;
    private boolean allowEnderpearlInFlight;
    private boolean allowEnderpearlInParachute;
    private boolean allowShiftDismount;
    private boolean allowCommandDismount;
    private final List<Waypoint> waypoints;

    public Flight(String name, EntityType creature, double cost) {
        this.name = name;
        this.creature = creature;
        this.cost = cost;
        this.invulnerable = true;
        this.parachuteTime = 5;
        this.allowEnderpearlInFlight = false;
        this.allowEnderpearlInParachute = false;
        this.allowShiftDismount = true;
        this.allowCommandDismount = true;
        this.waypoints = new ArrayList<>();
    }
    public String getName() { return name; }
    public EntityType getCreature() { return creature; }
    public void setCreature(EntityType creature) { this.creature = creature; }
    public double getCost() { return cost; }
    public void setCost(double cost) { this.cost = cost; }
    public boolean isInvulnerable() { return invulnerable; }
    public void setInvulnerable(boolean invulnerable) { this.invulnerable = invulnerable; }
    public int getParachuteTime() { return parachuteTime; }
    public void setParachuteTime(int parachuteTime) { this.parachuteTime = parachuteTime; }
    public boolean isAllowEnderpearlInFlight() { return allowEnderpearlInFlight; }
    public void setAllowEnderpearlInFlight(boolean allow) { this.allowEnderpearlInFlight = allow; }
    public boolean isAllowEnderpearlInParachute() { return allowEnderpearlInParachute; }
    public void setAllowEnderpearlInParachute(boolean allow) { this.allowEnderpearlInParachute = allow; }
    public boolean isAllowShiftDismount() { return allowShiftDismount; }
    public void setAllowShiftDismount(boolean allow) { this.allowShiftDismount = allow; }
    public boolean isAllowCommandDismount() { return allowCommandDismount; }
    public void setAllowCommandDismount(boolean allow) { this.allowCommandDismount = allow; }
    public List<Waypoint> getWaypoints() { return waypoints; }
    public void addWaypoint(Waypoint waypoint) { waypoints.add(waypoint); }
    public void removeLastWaypoint() { if (!waypoints.isEmpty()) { waypoints.remove(waypoints.size() - 1); } }
    public void clearWaypoints() { waypoints.clear(); }
}