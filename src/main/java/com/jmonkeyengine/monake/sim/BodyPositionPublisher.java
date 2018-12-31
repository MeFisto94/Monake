/*
 * $Id$
 *
 * Copyright (c) 2018, Simsilica, LLC
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

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jmonkeyengine.monake.bullet.BulletSystem;
import com.jmonkeyengine.monake.bullet.EntityPhysicsObject;
import com.jmonkeyengine.monake.bullet.EntityRigidBody;
import com.jmonkeyengine.monake.bullet.PhysicsObjectListener;
import com.jmonkeyengine.monake.es.BodyPosition;
import com.jmonkeyengine.monake.es.Position;
import com.simsilica.es.EntityData;
import com.simsilica.sim.AbstractGameSystem;
import com.simsilica.sim.SimTime;


/**
 *  Publishes to a BodyPosition component so that server-side systems
 *  have easy access to the mobile entity positions.  Since we wrote
 *  our own simple physics engine for this example, we could have just
 *  added BodyPosition as a field to the Body class but I wanted to show
 *  how one might integrate this component using a physics system that
 *  wouldn't let you do that.
 *
 *  Note: also adding the BodyPosition to the entity on the server
 *  is what makes it available on the client so that it can have a place
 *  to add its object update events from the network.  The BodyPosition
 *  component itself is actually transferred empty.
 *
 *  @author    Paul Speed
 */
public class BodyPositionPublisher extends AbstractGameSystem
        implements PhysicsObjectListener {

    private EntityData ed;
    private SimTime time;

    public BodyPositionPublisher() {
    }

    @Override
    protected void initialize() {
        this.ed = getSystem(EntityData.class);
        getSystem(BulletSystem.class).addPhysicsObjectListener(this);
    }

    @Override
    protected void terminate() {
        getSystem(BulletSystem.class).removePhysicsObjectListener(this);
    }

    @Override
    public void startFrame( SimTime time ) {
        this.time = time;
    }

    @Override
    public void added( EntityPhysicsObject object ) {
        /*if( !(object instanceof EntityRigidBody) ) {
            return;
        }
        EntityRigidBody body = (EntityRigidBody)object;
        if( body.getMass() == 0 ) {
            // We don't really care about static objects here
            return;
        }*/
        // We don't really care about static objects here since they
        // don't require special BodyPosition setup.
        if( object instanceof EntityRigidBody && ((EntityRigidBody)object).getMass() == 0 ) {
            return;
        }

        // The server side needs hardly any backlog.  We'll use 3 just in case
        // but 2 (even possibly 1) should be fine.  If we ever need to rewind
        // for shot resolution then we can increase the backlog as necessary
        BodyPosition bPos = new BodyPosition(3);

        // We should probably give the position its initial value
        // but it's about to get one in updated() right after this.
        // If we come up with cases that slip between these two states
        // then we can readdress

        ed.setComponent(object.getId(), bPos);
    }

    @Override
    public void updated( EntityPhysicsObject object ) {
        //if( !(object instanceof EntityRigidBody) ) {
        //    return;
        //}

        //EntityRigidBody body = (EntityRigidBody)object;
        Vector3f loc = object.getPhysicsLocation(null);
        Quaternion orient = object.getPhysicsRotation(null);

        //if( body.getMass() == 0 ) {
        if( object instanceof EntityRigidBody && ((EntityRigidBody)object).getMass() == 0 ) {
            // It's a static object so use standard Position publishing
            //System.out.println("update(" + object + ")");
            Position pos = new Position(loc, orient);
            //System.out.println("  pos:" + pos);
            ed.setComponents(object.getId(), pos);
        } else {
            // It's a mob or a ghost
            //System.out.println("updateMob(" + body + ") loc:" + loc);
            // Grab it's buffer which we are guaranteed to have because we set it in
            // added()
            BodyPosition pos = ed.getComponent(object.getId(), BodyPosition.class);
            pos.addFrame(time.getTime(), loc, orient, true);
        }
    }

    @Override
    public void removed( EntityPhysicsObject object ) {
        // We don't really care about static objects here since they
        // don't require special BodyPosition management.
        if( object instanceof EntityRigidBody && ((EntityRigidBody)object).getMass() == 0 ) {
            return;
        }
        //if( !(object instanceof EntityRigidBody) ) {
        //    return;
        //}
        BodyPosition pos = ed.getComponent(object.getId(), BodyPosition.class);
        if( pos == null ) {
            // It was a static object or somehow initialized poorly
            return;
        }

        // Give one last position update with the visibility shut off
        Vector3f loc = object.getPhysicsLocation(null);
        Quaternion orient = object.getPhysicsRotation(null);
        pos.addFrame(time.getTime(), loc, orient, false);
    }

    @Override
    public void endFrame() {
    }
}