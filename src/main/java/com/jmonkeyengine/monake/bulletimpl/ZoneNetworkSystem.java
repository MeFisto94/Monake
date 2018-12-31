/*
 * $Id$
 *
 * Copyright (c) 2016, Simsilica, LLC
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

package com.jmonkeyengine.monake.bulletimpl;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jmonkeyengine.monake.bullet.BulletSystem;
import com.jmonkeyengine.monake.bullet.EntityPhysicsObject;
import com.jmonkeyengine.monake.bullet.EntityRigidBody;
import com.jmonkeyengine.monake.bullet.PhysicsObjectListener;
import com.simsilica.ethereal.zone.ZoneManager;
import com.simsilica.mathd.AaBBox;
import com.simsilica.mathd.Quatd;
import com.simsilica.mathd.Vec3d;
import com.simsilica.sim.AbstractGameSystem;
import com.simsilica.sim.SimTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *  A game system that registers a listener with the Bullet Physics
 *  system and then forwards those events to the SimEtheral zone manager,
 *  which in turn will package them up for the clients in an efficient way.
 *
 *  @author    Paul Speed
 */
public class ZoneNetworkSystem extends AbstractGameSystem {

    static Logger log = LoggerFactory.getLogger(ZoneNetworkSystem.class);

    private ZoneManager zones;
    private PhysicsObserver physicsObserver = new PhysicsObserver();

    public ZoneNetworkSystem( ZoneManager zones ) {
        this.zones = zones;
    }

    @Override
    protected void initialize() {
        getSystem(BulletSystem.class, true).addPhysicsObjectListener(physicsObserver);
    }

    @Override
    protected void terminate() {
        getSystem(BulletSystem.class, true).removePhysicsObjectListener(physicsObserver);
    }

    /**
     *  Listens for changes in the physics objects and sends them
     *  to the zone manager.
     */
    private class PhysicsObserver implements PhysicsObjectListener {

        private Vector3f posf = new Vector3f();
        private Quaternion orientf = new Quaternion();

        private Vec3d pos = new Vec3d();
        private Quatd orient = new Quatd();

        // We probably won't have many zones, if we even have more than one.
        // The physics objects do not provide any sort of accurate bounds so
        // we'll guess at a size that is "big enough" for any particular mobile
        // object.  2x2x2 meters should be good enough... until it isn't.
        private AaBBox box = new AaBBox(1);

        @Override
        public void startFrame( SimTime time ) {
            zones.beginUpdate(time.getTime());
        }

        @Override
        public void endFrame() {
            zones.endUpdate();
        }

        @Override
        public void added( EntityPhysicsObject object ) {
            // Don't really care about this
        }

        @Override
        public void updated( EntityPhysicsObject object ) {
            if( object instanceof EntityRigidBody && ((EntityRigidBody)object).getMass() != 0 ) {
                EntityRigidBody body = (EntityRigidBody)object;

                // Grab the latest reference frame in our temp variables
                body.getPhysicsLocation(posf);
                body.getPhysicsRotation(orientf);

                // Convert them to mathd values
                pos.set(posf);
                orient.set(orientf);

                if( log.isTraceEnabled() ) {
                    log.trace("body:" + object.getId() + "  pos:" + pos);
                }

                // Move the box as appropriate
                // Note: we don't so much care about bounds as there won't be many zones.
                // So we set it rather arbitrarily to 'big enough'.
                box.setCenter(pos);

                zones.updateEntity(body.getId().getId(), true, pos, orient, box);
            }
        }

        @Override
        public void removed( EntityPhysicsObject object ) {
            zones.remove(object.getId().getId());
        }
    }
}



