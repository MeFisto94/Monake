/*
 * Monake
 *
 * Copyright (c) 2019 Monake Dev Team
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jmonkeyengine.monake.sim;

import com.jme3.bullet.collision.PhysicsCollisionEvent;
import com.jmonkeyengine.monake.bullet.BulletSystem;
import com.jmonkeyengine.monake.bullet.EntityCollisionListener;
import com.jmonkeyengine.monake.bullet.EntityPhysicsObject;
import com.jmonkeyengine.monake.es.ObjectType;
import com.jmonkeyengine.monake.es.ObjectTypes;
import com.jmonkeyengine.monake.es.components.AmmoShotgunComponent;
import com.jmonkeyengine.monake.es.components.HealthComponent;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntitySet;
import com.simsilica.sim.AbstractGameSystem;
import com.simsilica.sim.SimTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles collisions with shells and deal damage to the player
 *
 * @author MeFisto94
 */
public class ShellDamageSystem extends AbstractGameSystem implements EntityCollisionListener {
    static Logger log = LoggerFactory.getLogger(ShellDamageSystem.class);
    EntityData ed;

    public ShellDamageSystem() {
    }

    @Override
    protected void initialize() {
        ed = getSystem(EntityData.class);
        if (ed == null) {
            throw new RuntimeException("ShellDamageSystem requires an EntityData object.");
        }

        getSystem(BulletSystem.class).addEntityCollisionListener(this);
    }

    @Override
    protected void terminate() {
        getSystem(BulletSystem.class).removeEntityCollisionListener(this);
    }

    @Override
    public void update(SimTime time) {
        super.update(time);
    }

    @Override
    public void collision(EntityPhysicsObject object1, EntityPhysicsObject object2, PhysicsCollisionEvent event) {

        // which objects do we have, is it a player on player, is it player on bullet? what happened
        // can directly access the entity system for this, because we're on the server and it will be fast
        // can't guarantee which components are going to be in the colliding objects so its hard to filter
        Entity objectEntity1 = ed.getEntity(object1.getId(), ObjectType.class);
        Entity objectEntity2 = ed.getEntity(object2.getId(), ObjectType.class);

        ObjectType objectTypeClass1 = objectEntity1.get(ObjectType.class);
        ObjectType objectTypeClass2 = objectEntity2.get(ObjectType.class);

        String objectType1 = objectTypeClass1.getTypeName(ed);
        String objectType2 = objectTypeClass2.getTypeName(ed);

        // @TODO: Change to ObjectTypes.WeaponShell, which contains a Health Component, a Ghost Component and a Decay Component (when not hitting something)
        if (objectType1.equals(ObjectTypes.PLAYER) && objectType2.equals(ObjectTypes.WEAPON_SHELL)) {
            processHealthPlayerCollision(objectEntity1, objectEntity2);
        } else if (objectType2.equals(ObjectTypes.PLAYER) && objectType1.equals(ObjectTypes.WEAPON_SHELL)) {
            processHealthPlayerCollision(objectEntity2, objectEntity1);
        }
    }

    private void processHealthPlayerCollision(Entity player, Entity shell) {
        HealthComponent shellDamage = ed.getComponent(shell.getId(), HealthComponent.class);
        HealthComponent playerHealth = ed.getComponent(player.getId(), HealthComponent.class);

        if (playerHealth.isDead()) {
            return;
        }

        int newHealth = Math.max(0, playerHealth.getHealth() - shellDamage.getHealth());
        System.out.println("Setting Health to " + newHealth);
        ed.setComponent(player.getId(), new HealthComponent(newHealth));
        ed.removeEntity(shell.getId());

        //@TODO: FX and a System listening for HealthComponent.isDead
    }
}
