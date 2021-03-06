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

package com.jmonkeyengine.monake.es;

import com.simsilica.es.EntityComponent;
import com.simsilica.mathd.Vec3d;


/**
 *
 *
 *  @author    Paul Speed
 */
public class SphereShape implements EntityComponent {
    
    private double radius;
    private Vec3d offset;
 
    /**
     *  For SpiderMonkey serialization purposes.
     */   
    protected SphereShape() {
    }
    
    /**
     *  Creates a new sphere collision shape with the specified
     *  radius and 'center' relative to the natural origin of
     *  the object to which it applies.  -CoG in most cases.
     */
    public SphereShape( double radius, Vec3d centerOffset ) {
        this.radius = radius;
        this.offset = centerOffset;
    }
    
    public double getRadius() {
        return radius;
    }
    
    public Vec3d getCenterOffset() {
        return offset;
    }
    
    @Override
    public String toString() {
        return "SphereShape[radius=" + radius + ", centerOffset=" + offset + "]";
    }
}
