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

import com.jme3.bullet.collision.PhysicsCollisionEvent;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jmonkeyengine.monake.bullet.ControlDriver;
import com.jmonkeyengine.monake.bullet.EntityPhysicsObject;
import com.jmonkeyengine.monake.bullet.EntityRigidBody;
import com.simsilica.sim.SimTime;

/**
 *  A control driver that will apply CharInput to a physics objects
 *  using the current character physics settings.
 *
 *  @author    Paul Speed
 */
public class CharInputDriver implements ControlDriver {
    private EntityRigidBody body;
    
    private Vector3f vTemp = new Vector3f();
    private Quaternion qTemp = new Quaternion();
    private float[] angles = new float[3];
    
    private float upThreshold = 0.7f; // ~45 degrees
    protected static final float walkSpeed = 5;
    protected static final float runSpeed = 10;

    // Input
    Quaternion rotation;
    Vector3f movement;
    CharFlags flags;
    
    private Vector3f groundVelocity = new Vector3f();
    private int groundContactCount = 0;
    private boolean canJump = false;
    private boolean isJumping = false;
    
    private Vector3f force = new Vector3f();
 
    private CharPhysics charPhysics;

    public CharInputDriver( CharPhysics charPhysics ) {
        setCharPhysics(charPhysics);
        rotation = Quaternion.IDENTITY;
        movement = Vector3f.ZERO;
        flags = new CharFlags();
    }

    public void setInput(Quaternion rotation, Vector3f movement, CharFlags flags) {
        this.rotation = rotation;
        this.movement = movement;
        this.flags = flags;
    }
    
    public void setCharPhysics( CharPhysics charPhysics ) {
        this.charPhysics = charPhysics;
        if( body != null ) {
            // Make sure gravity is current
            body.setGravity(charPhysics.gravity);
        }
    }
    
    public CharPhysics getCharPhysics() {
        return charPhysics;
    }

    @Override
    public void initialize( EntityRigidBody body ) {
        this.body = body;
        body.setGravity(charPhysics.gravity);
    }
  
    @Override
    public void addCollision(EntityPhysicsObject otherBody, PhysicsCollisionEvent event ) {
        // For this we don't care about ghost objects
        if( !(otherBody instanceof PhysicsRigidBody) ) {
            return;
        }

        PhysicsCollisionObject us = event.getObjectA();
        
        // Check the normal
        Vector3f normal = event.getNormalWorldOnB();
        if( event.getObjectA() == otherBody ) {
            // The normal goes the other way
            normal = normal.negate();
            us = event.getObjectB();           
        }

        float isUp = Vector3f.UNIT_Y.dot(normal);
        if( isUp < upThreshold ) {
            // This is not something we are standing on
            return;
        }

        // Can jump even if it's just our ghost that is intersecting the ground
        //canJump = true;

        // We care about our ghost object intersection but not for ground
        // velocity tracking.  So for relative ground velocity tracking, 
        // check to see if our colliding object is really us.        
        if( us == body ) {
            //System.out.println("++++ contact:" + otherBody);
            // Just until we can tweak our ghost size more appropriately
            canJump = true;
            PhysicsRigidBody rb = (PhysicsRigidBody)otherBody;
            rb.getLinearVelocity(vTemp);
 
            groundVelocity.addLocal(vTemp);
            groundContactCount++;
        }        
    }
 
    protected void calculateCollisionData() {
        if( groundContactCount > 0 ) {
            // Average the various ground velocities
            groundVelocity.multLocal(1f/groundContactCount);
        }
    }
    
    protected void invalidateCollisionData() {
        groundContactCount = 0;
        groundVelocity.set(0, 0, 0);
        canJump = false;
    }      
    
    @Override
    public void update( SimTime time, EntityRigidBody body ) {
    
        calculateCollisionData();
    
        //body.getPhysicsRotation(qTemp);
        body.getAngularVelocity(vTemp);
        
        // Kill any non-yaw orientation
        /*qTemp.toAngles(angles);
        if( angles[0] != 0 || angles[2] != 0 ) {
            angles[0] = 0;
            angles[2] = 0;
            body.setPhysicsRotation(qTemp.fromAngles(angles));
        }*/
        // The above is unnecessary because we are going to force
        // orientation anyway.
        // Note: for non-capsule shapes something else would have to
        // be done so that the environment affects orientation.
        // Player input orientation then becomes more of a suggestion
        // that needs to be reconciled with environment influences.
        //
        // Note: apparently killing angular velocity is actually needed.
        // Otherwise we tip and intercollide with ramps.
        
        // Kill any non-yaw rotation
        if( vTemp.x != 0 && vTemp.z != 0 ) {
            vTemp.x = 0;
            vTemp.y *= 0.95f; // Let's see if we can dampen the spinning
            vTemp.z = 0;
            body.setAngularVelocity(vTemp);
        }

        final float speed = flags.getFlag(CharFlag.SPRINTING) ? runSpeed : walkSpeed;
        Vector3f desiredVelocity =
            rotation.mult(
                movement.clone().setY(0f).normalizeLocal()
            ) // transfer movement from local to world space.
            .setY(0f).normalizeLocal().multLocal(speed);
            // Then remove the y-component and re-normalize, so we have full speed on xz and then multiply with speed.

        //System.out.println("groundVelocity:" + groundVelocity + "  desiredVelocity:" + desiredVelocity);

        // Our real desired velocity is relative to the movement of what we
        // are standing on
        desiredVelocity.addLocal(groundVelocity);

        // See how much our velocity has to change to reach the
        // desired velocity
        body.getLinearVelocity(vTemp);
        float verticalVelocity = vTemp.y - groundVelocity.y;

        //@TODO: FIXME: HERE IS A BUG, FOR SOME REASON current != real movement speed! adjusting runSpeed influences this value
        // even if you DONT SPRINT. Might be related to updating the physics position though? NOTE: In the meantime I think
        // that the system/controller just can't settle with one speed perfectly, so fine tune or asking the lord at that point
        // System.out.println("current:" + vTemp + "  desired:" + desiredVelocity + "  delta:" + desiredVelocity.subtract(vTemp));

        // Calculate a force that will either brake or accelerate in the
        // appropriate direction to achieve the desired velocity
        force.set(desiredVelocity).subtractLocal(vTemp);
        force.y = 0;
        if( groundContactCount > 0 ) {
            force.multLocal(getCharPhysics().groundImpulse);
            //System.out.println("   groundForce:" + force + "  vTemp:" + vTemp);
        } else {
            force.multLocal(getCharPhysics().airImpulse);
            //System.out.println("   airForce:" + force + "  vTemp:" + vTemp);
        }
        body.applyCentralForce(force);        
        body.setPhysicsRotation(rotation);

        if (flags.getFlag(CharFlag.JUMPING)) {
            if( canJump && !isJumping ) {
                System.out.println("-------------------JUMP!   vTemp.y:" + vTemp.y);
                // Clear any downward momentum we might still have
                vTemp.y = groundVelocity.y;                        
                vTemp.addLocal(0, getCharPhysics().jumpForce, 0);
                body.setLinearVelocity(vTemp);
                isJumping = true;   
            } 
        } else {
            // If the player releases early and we allow short jumps
            // then kill some of the momentum
            if(getCharPhysics().shortJumps && isJumping && verticalVelocity > 0 ) {
                System.out.println("---------------KILL JUMP!");
                // Then for 'short jumps' support we should 
                // set a maximum upward velocity
                vTemp.y = Math.min(vTemp.y, groundVelocity.y + 2);
                body.setLinearVelocity(vTemp);
            }
            isJumping = false;            
        }
 
        //body.getLinearVelocity(vTemp);
        //System.out.println("current velocity:" + vTemp + "   groundVelocity:" + groundVelocity);
        //System.out.println("isJumping:" + isJumping + "  verticalVelocity:" + verticalVelocity + "    vTemp:" + vTemp + "  groundVel:" + groundVelocity.y);
        // See if we are actually falling and no longer jumping
        // because if we autoBounce then we might be ready to jump again
        // soon.
        if(getCharPhysics().autoBounce && isJumping && (vTemp.y - groundVelocity.y < 0) ) {
            isJumping = false;
            System.out.println("---------------Jump done, falling.");
        }
           
        // Get ready for the next set of collision events       
        invalidateCollisionData();        
    }
    
    @Override
    public void terminate( EntityRigidBody body ) {
    }
}


