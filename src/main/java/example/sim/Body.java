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

package example.sim;

import java.util.concurrent.atomic.AtomicLong;

import com.simsilica.es.EntityId;
import com.simsilica.mathd.*;

import example.es.Position;

/**
 *  A physical body in space.  These are modeled as a "point mass"
 *  in the sense that their orientation does not integrate, nor does
 *  it affect integration.  ie: there is no rotational acceleration
 *  or velocity (at least not yet and there won't ever be in the true
 *  physics sense).  Rotation is kept because it's convenient.
 *
 *  @author    Paul Speed
 */
public class Body {

    public final EntityId bodyId;
    
    public Vec3d pos = new Vec3d();
    public Vec3d velocity = new Vec3d();
    public Vec3d acceleration = new Vec3d();
    public double radius = 1;
    public double invMass = 1;
    public AaBBox bounds = new AaBBox(radius);
    
    public Quatd orientation = new Quatd();
    public volatile ControlDriver driver; 
 
    public Body( EntityId bodyId ) {
        this.bodyId = bodyId;
    }
    
    public Body( EntityId bodyId, double x, double y, double z ) {
        this.bodyId = bodyId;
        pos.set(x, y, z);
    }
    
    public void setPosition( Position pos ) {
        this.pos.set(pos.getLocation());
        this.orientation.set(pos.getFacing());
    }
 
    public void integrate( double stepTime ) {
        // Integrate velocity
        velocity.addScaledVectorLocal(acceleration, stepTime);
        
        // Integrate position
        pos.addScaledVectorLocal(velocity, stepTime);
 
//System.out.println(bodyId + " pos:" + pos + "   dir:" + orientation.mult(new Vec3d(0, 0, 1)));        
        // That's it.  That's a physics engine.
        
        // Update the bounds since it's easy to do here and helps
        // other things know where the object is for real
        bounds.setCenter(pos);   
    }
}
