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

package com.jmonkeyengine.monake.view;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Spatial;
import com.jmonkeyengine.monake.ConnectionState;
import com.jmonkeyengine.monake.net.GameSession;
import com.jmonkeyengine.monake.net.client.GameSessionClientService;
import com.jmonkeyengine.monake.sim.CharFlag;
import com.jmonkeyengine.monake.sim.CharFlags;
import com.jmonkeyengine.monake.sim.GameEntities;
import com.simsilica.es.EntityId;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.core.VersionedHolder;
import com.simsilica.lemur.input.*;
import com.simsilica.state.DebugHudState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 *
 *  @author    Paul Speed
 */
public class PlayerMovementState extends BaseAppState
                                 implements AnalogFunctionListener, StateFunctionListener {

    static Logger log = LoggerFactory.getLogger(PlayerMovementState.class);

    private InputMapper inputMapper;
    private Camera camera;
    private double turnSpeed = 2.5;  // one half complete revolution in 2.5 seconds
    private double yaw = FastMath.PI;
    private double pitch;
    private double maxPitch = FastMath.HALF_PI;
    private double minPitch = -FastMath.HALF_PI;
    private Quaternion cameraFacing = new Quaternion().fromAngles((float)pitch, (float)yaw, 0);
    private double forward;
    private double side;
    protected CharFlags flags;
 
    private Vector3f thrust = new Vector3f(); // not a direction, just 3 values
    
    private GameSession session;

    // For now we'll do this here but really we probably want a separate camera state
    private EntityId characterId;
    private ModelViewState models;  
    
    private Vector3f lastPosition = new Vector3f();
    private VersionedHolder<String> positionDisplay;
    private VersionedHolder<String> speedDisplay; 

    public PlayerMovementState() {
        flags = new CharFlags();
    }

    public void setCharacterId(EntityId characterId ) {
        this.characterId = characterId;
    }
    
    public EntityId getCharacterId() {
        return characterId;
    }
    
    public void setPitch( double pitch ) {
        this.pitch = pitch;
        updateFacing();
    }

    public double getPitch() {
        return pitch;
    }
    
    public void setYaw( double yaw ) {
        this.yaw = yaw;
        updateFacing();
    }
    
    public double getYaw() {
        return yaw;
    }

    public void setRotation( Quaternion rotation ) {
        // Do our best
        float[] angle = rotation.toAngles(null);
        this.pitch = angle[0];
        this.yaw = angle[1];
        updateFacing();
    }
    
    public Quaternion getRotation() {
        return camera.getRotation();
    }

    @Override
    protected void initialize( Application app ) {
        
        log.info("initialize()");
    
        this.camera = app.getCamera();
        
        if( inputMapper == null ) {
            inputMapper = GuiGlobals.getInstance().getInputMapper();
        }
        
        // Most of the movement functions are treated as analog.        
        inputMapper.addAnalogListener(this,
                                      PlayerMovementFunctions.F_Y_ROTATE,
                                      PlayerMovementFunctions.F_X_ROTATE,
                                      PlayerMovementFunctions.F_THRUST,
                                      PlayerMovementFunctions.F_STRAFE);

        // Only run mode is treated as a 'state' or a trinary value.
        // (Positive, Off, Negative) and in this case we only care about
        // Positive and Off.  See PlayerMovementFunctions for a description
        // of alternate ways this could have been done.
        inputMapper.addStateListener(this,
                                     PlayerMovementFunctions.F_BOOST,
                                     PlayerMovementFunctions.F_JUMP,
                                     PlayerAbilityFunctions.F_SHOOT);
                                     
        // Grab the game session
        session = getState(ConnectionState.class).getService(GameSessionClientService.class);
        if( session == null ) {
            throw new RuntimeException("PlayerMovementState requires an active game session.");
        }
 
        this.models = getState(ModelViewState.class);
 
        if( getState(DebugHudState.class) != null ) {
            DebugHudState debug = getState(DebugHudState.class);
            this.positionDisplay = debug.createDebugValue("Position", DebugHudState.Location.Top);
            this.speedDisplay = debug.createDebugValue("Speed", DebugHudState.Location.Top); 
        }
    }

    @Override
    protected void cleanup(Application app) {

        inputMapper.removeAnalogListener(this,
                                         PlayerMovementFunctions.F_Y_ROTATE,
                                         PlayerMovementFunctions.F_X_ROTATE,
                                         PlayerMovementFunctions.F_THRUST,
                                         PlayerMovementFunctions.F_STRAFE);
        inputMapper.removeStateListener(this,
                                        PlayerMovementFunctions.F_BOOST,
                                        PlayerMovementFunctions.F_JUMP,
                                        PlayerAbilityFunctions.F_SHOOT);
    }

    @Override
    protected void onEnable() {
        log.info("onEnable()");
    
        // Make sure our input group is enabled
        inputMapper.activateGroup(PlayerMovementFunctions.G_MOVEMENT);
        inputMapper.activateGroup(PlayerAbilityFunctions.G_ABILITY);
        
        // And kill the cursor
        GuiGlobals.getInstance().setCursorEventsEnabled(false);
        
        // A 'bug' in Lemur causes it to miss turning the cursor off if
        // we are enabled before the MouseAppState is initialized.
        getApplication().getInputManager().setCursorVisible(false);        
    }

    @Override
    protected void onDisable() {
        inputMapper.deactivateGroup(PlayerMovementFunctions.G_MOVEMENT);
        inputMapper.deactivateGroup(PlayerAbilityFunctions.G_ABILITY);
        GuiGlobals.getInstance().setCursorEventsEnabled(true);        
    }

    double speedAverage = 0;
    long lastSpeedTime = 0;
    protected void updateShipLocation( Vector3f loc ) {
        
        String s = String.format("%.2f, %.2f, %.2f", loc.x, loc.y, loc.z);
        positionDisplay.setObject(s);
 
        long time = System.nanoTime();
        if( lastSpeedTime != 0 ) {
            // Let's go ahead and calculate speed
            double speed = loc.subtract(lastPosition).length();
            
            // And de-integrate it based on the time delta
            speed = speed * 1000000000.0 / (time - lastSpeedTime);

            // A slight smoothing of the value
            speedAverage = (speedAverage * 2 + speed) / 3;
             
            s = String.format("%.2f", speedAverage);
            speedDisplay.setObject(s);       
        }
        lastPosition.set(loc);
        lastSpeedTime = time;
    }

    private long nextSendTime = 0;
    private long sendFrequency = 1000000000L / 20; // 20 times a second, every 50 ms
     
    @Override
    public void update( float tpf ) {
        // Update the camera position from the ship spatial
        Spatial spatial = models.getModel(characterId);
        if( spatial != null ) {
            camera.setLocation(spatial.getWorldTranslation().add(GameEntities.cameraOffset));
        }
            
        long time = System.nanoTime();
        if( time > nextSendTime ) {
            nextSendTime = time + sendFrequency;

            // @TODO: Think about using getYaw() etc or removing these methods.
            float yaw = camera.getRotation().toAngles(null)[1];

            // Z is forward
            thrust.x = (float)(side);
            thrust.y = 0f;
            thrust.z = (float)(forward);

            thrust.normalizeLocal();
            session.move(yaw, thrust, flags);
 
            // Only update the position/speed display 20 times a second
            //if( spatial != null ) {                
            //    updateShipLocation(spatial.getWorldTranslation());
            //}
        } 

            if( spatial != null ) {
                // This is only used for the DebugHudState and for nothing else.
                updateShipLocation(spatial.getWorldTranslation());
            }
 
        /*
        // 'integrate' camera position based on the current move, strafe,
        // and elevation speeds.
        if( forward != 0 || side != 0 || elevation != 0 ) {
            Vector3f loc = camera.getLocation();            
            Quaternion rot = camera.getRotation();

            Vector3f move = rot.mult(Vector3f.UNIT_Z).multLocal((float)(forward * speed * tpf)); 
            Vector3f strafe = rot.mult(Vector3f.UNIT_X).multLocal((float)(side * speed * tpf));
            
            // Note: this camera moves 'elevation' along the camera's current up
            // vector because I find it more intuitive in free flight.
            Vector3f elev = rot.mult(Vector3f.UNIT_Y).multLocal((float)(elevation * speed * tpf));
                        
            loc = loc.add(move).add(strafe).add(elev);
            camera.setLocation(loc); 
        }*/
    }
 
    /**
     *  Implementation of the StateFunctionListener interface.
     */
    @Override
    public void valueChanged( FunctionId func, InputState value, double tpf ) {
 
        // Change the speed based on the current run mode
        // Another option would have been to use the value
        // directly:
        //    speed = 3 + value.asNumber() * 5
        //...but I felt it was slightly less clear here.
        boolean b = value == InputState.Positive;
        if( func == PlayerMovementFunctions.F_BOOST ) {
            flags.setFlag(CharFlag.SPRINTING, b);
        } else if (func == PlayerMovementFunctions.F_JUMP) {
            flags.setFlag(CharFlag.JUMPING, b);
        } else if (func == PlayerAbilityFunctions.F_SHOOT) {
            flags.setFlag(CharFlag.SHOOTING, b);
        }
    }

    /**
     *  Implementation of the AnalogFunctionListener interface.
     */
    @Override
    public void valueActive( FunctionId func, double value, double tpf ) {
 
        // Setup rotations and movements speeds based on current
        // axes states.    
        if( func == PlayerMovementFunctions.F_Y_ROTATE ) {
            pitch += -value * tpf * turnSpeed;
            if( pitch < minPitch )
                pitch = minPitch;
            if( pitch > maxPitch )
                pitch = maxPitch;
        } else if( func == PlayerMovementFunctions.F_X_ROTATE ) {
            yaw += -value * tpf * turnSpeed;
            if( yaw < 0 )
                yaw += Math.PI * 2;
            if( yaw > Math.PI * 2 )
                yaw -= Math.PI * 2;
        } else if( func == PlayerMovementFunctions.F_THRUST ) {
            this.forward = value;
            return;
        } else if( func == PlayerMovementFunctions.F_STRAFE ) {
            this.side = -value;
            return;
        } else {
            return;
        }
        updateFacing();        
    }

    protected void updateFacing() {
        cameraFacing.fromAngles((float)pitch, (float)yaw, 0);
        camera.setRotation(cameraFacing);
    }
}


