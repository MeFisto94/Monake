/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jmonkeyengine.monake.sim;

import com.jme3.math.Vector3f;
import com.jmonkeyengine.monake.bullet.Mass;
import com.jmonkeyengine.monake.bullet.SpawnPosition;
import com.jmonkeyengine.monake.es.ObjectType;
import com.jmonkeyengine.monake.es.ObjectTypes;
import com.jmonkeyengine.monake.es.components.*;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import com.simsilica.es.filter.FieldFilter;
import com.simsilica.sim.AbstractGameSystem;
import com.simsilica.sim.SimTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author wobblytrout
 */
public class DeathSystem extends AbstractGameSystem {
    static Logger log = LoggerFactory.getLogger(DeathSystem.class);
    EntityData ed;
    EntitySet playerSet;

    @Override
    protected void initialize() {
        ed = getSystem(EntityData.class);
        if (ed == null) {
            throw new RuntimeException("EntityData object is required for a: " + this.getClass());
        }

        // get a entity set that filters for players and gets health component
        playerSet = ed.getEntities(new FieldFilter(ObjectType.class, "type", ObjectTypes.playerType(ed).getType()),
                ObjectType.class, HealthComponent.class);

    }

    @Override
    protected void terminate() {
        playerSet.release();
    }

    @Override
    public void update(SimTime time) {
        super.update(time);

        // if a player gets to zero health, we MURDER them and respawn them at a random spawn location after 2 seconds?
        if (playerSet.applyChanges()) {
            for (Entity entity: playerSet.getChangedEntities()) {
                HealthComponent health = entity.get(HealthComponent.class);

                if (health != null && health.isDead()) {
                    // we need to do some respawning action here on this entity.
                    log.info("Player has died, RESPAWNING!!!!");
                    respawnPlayer(entity.getId(), ed);
                }
            }
        }
    }

    public void respawnPlayer(EntityId player, EntityData ed) {
        ed.setComponents(player, new Mass(50f), new SpawnPosition(GameEntities.getRandomSpawnSpot()),
                new HealthComponent(100), new ActiveWeaponComponent(WeaponTypes.SINGLESHOTGUN.ordinal()),
                new AmmoShotgunComponent(25), new AmmoNailgunComponent(0),
                new ArmorComponent(0, 0));
    }

}
