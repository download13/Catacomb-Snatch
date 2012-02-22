package com.mojang.mojam.entity.building;

import java.util.ArrayList;

import com.mojang.mojam.entity.mob.*;
import com.mojang.mojam.network.TurnSynchronizer;
import com.mojang.mojam.screen.*;


public class SpawnerEntity extends Building {
    public static final int SPAWN_INTERVAL = 60 * 4;
    public static final int SPAWN_LIMIT = 30;
    
    public int spawnTime = 0;

    public int type;
    
    private ArrayList<Mob> mobs = new ArrayList<Mob>();
    private ArrayList<Mob> mobsToRemove = new ArrayList<Mob>();

    public SpawnerEntity(double x, double y, int team, int type) {
        super(x, y, team);

        this.type = type;
        setStartHealth(20);
        freezeTime = 10;
        spawnTime = TurnSynchronizer.synchedRandom.nextInt(SPAWN_INTERVAL);
        minimapIcon = 4;

        deathPoints = type * 5 + 5;
    }

    public void tick() {
        super.tick();
        if (freezeTime > 0) return;

        if (--spawnTime <= 0 && mobs.size() <= SPAWN_LIMIT) {
            spawn();
            spawnTime = SPAWN_INTERVAL;
        }
        
        for (Mob m : mobs) {
        	if (m.removed) {
        		mobsToRemove.add(m);
        	}
        }
        mobs.removeAll(mobsToRemove);
        mobsToRemove.clear();
    }

    private void spawn() {
        double x = pos.x + (TurnSynchronizer.synchedRandom.nextFloat() - 0.5) * 5;
        double y = pos.y + (TurnSynchronizer.synchedRandom.nextFloat() - 0.5) * 5;

        Mob te = null;
        if (type == 0) te = new Bat(x, y);
        if (type == 1) te = new Snake(x, y);
        if (type == 2) te = new Mummy(x, y);

        if (level.getEntities(te.getBB().grow(8), te.getClass()).size() == 0) {
            level.addEntity(te);
            mobs.add(te);
        }
    }

    public void die() {
        super.die();
    }

    private int lastIndex = 0;

    public Bitmap getSprite() {
        int newIndex = 3 - (3 * health) / maxHealth;
        if (newIndex != lastIndex) {
            lastIndex = newIndex;
        }
        return Art.mobSpawner[newIndex][0];
    }
    
    public boolean isHighlightable() { // Not sure about this, was that a bug or a feature?
        return false;
    }
}
