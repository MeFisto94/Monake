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
import com.jmonkeyengine.monake.es.ShapeInfos;
import com.jmonkeyengine.monake.es.components.ActiveWeaponComponent;
import com.jmonkeyengine.monake.es.components.AmmoNailgunComponent;
import com.jmonkeyengine.monake.es.components.AmmoShotgunComponent;
import com.jmonkeyengine.monake.es.components.ArmorComponent;
import com.jmonkeyengine.monake.es.components.HealthComponent;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import com.simsilica.es.Name;
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
        playerSet.applyChanges();

        for (Entity entity : playerSet) {
            HealthComponent health = entity.get(HealthComponent.class);

            if (health != null && health.isDead()) {
                // we need to do some respawning action here on this entity.
                log.info("Player has died, RESPAWNING!!!!");
                respawnPlayer(entity.getId(), ed);

            }

        }

    }

    public void respawnPlayer(EntityId player, EntityData ed) {
               
        // TODO randomize spawn locations
        Vector3f spawnLocation = GameEntities.spawnLocations.get(0); // 29.5f, 20f, -30.4f
        ed.setComponents(player, ObjectTypes.playerType(ed), new Mass(50f), new SpawnPosition(spawnLocation),
                ShapeInfos.playerInfo(ed));
        ed.setComponents(player, new HealthComponent(100), new ActiveWeaponComponent(WeaponTypes.SINGLESHOTGUN.ordinal()), new AmmoShotgunComponent(25), new AmmoNailgunComponent(0), new ArmorComponent(0, 0));
    }

}
