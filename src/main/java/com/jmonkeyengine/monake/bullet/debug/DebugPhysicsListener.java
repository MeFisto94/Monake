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

package com.jmonkeyengine.monake.bullet.debug;

import com.jmonkeyengine.monake.bullet.EntityGhostObject;
import com.jmonkeyengine.monake.bullet.EntityPhysicsObject;
import com.jmonkeyengine.monake.bullet.EntityRigidBody;
import com.jmonkeyengine.monake.bullet.PhysicsObjectListener;

import com.simsilica.es.EntityData;
import com.simsilica.sim.SimTime;


/**
 *  Publishes body status components for physics entities.  This
 *  must be added to the BulletSystem for the PhysicsDebugState
 *  to work.
 *
 *  @author    Paul Speed
 */
public class DebugPhysicsListener implements PhysicsObjectListener {

    private EntityData ed;

    public DebugPhysicsListener( EntityData ed ) {
        this.ed = ed;
    }
    
    public void startFrame( SimTime time ) {
    }
    
    public void endFrame() {
    }
    
    public void added( EntityPhysicsObject object ) {        
        if( object instanceof EntityGhostObject ) {
            ed.setComponent(object.getId(), new BodyDebugStatus(BodyDebugStatus.GHOST));
        } else if( object instanceof EntityRigidBody ) {
            EntityRigidBody body = (EntityRigidBody)object;
            if( body.getMass() == 0 ) {
                ed.setComponent(object.getId(), new BodyDebugStatus(BodyDebugStatus.STATIC));    
            }
        }  
    }
    
    public void updated( EntityPhysicsObject object ) {
        if( object instanceof EntityRigidBody ) {
            EntityRigidBody body = (EntityRigidBody)object;
            int status = body.getMass() == 0 ? BodyDebugStatus.STATIC 
                            : body.isActive() ? BodyDebugStatus.ACTIVE : BodyDebugStatus.INACTIVE;
            BodyDebugStatus existing = ed.getComponent(object.getId(), BodyDebugStatus.class);
            if( existing == null || existing.getStatus() != status ) {
                ed.setComponent(object.getId(), new BodyDebugStatus(status));
            } 
        }
    }
    
    public void removed( EntityPhysicsObject object ) {
    }
}

