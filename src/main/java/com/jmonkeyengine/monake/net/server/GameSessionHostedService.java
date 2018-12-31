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

package com.jmonkeyengine.monake.net.server;

import com.jmonkeyengine.monake.bullet.BulletSystem;
import com.jmonkeyengine.monake.es.Position;
import com.jmonkeyengine.monake.net.GameSession;
import com.jmonkeyengine.monake.net.GameSessionListener;
import com.jmonkeyengine.monake.net.chat.server.ChatHostedService;
import com.jmonkeyengine.monake.sim.*;
import org.slf4j.*;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.network.HostedConnection;
import com.jme3.network.serializing.Serializer;
import com.jme3.network.serializing.serializers.FieldSerializer;
import com.jme3.network.service.AbstractHostedConnectionService;
import com.jme3.network.service.HostedServiceManager;
import com.jme3.network.service.rmi.RmiHostedService;
import com.jme3.network.service.rmi.RmiRegistry;

import com.simsilica.event.EventBus;
import com.simsilica.mathd.Vec3d;
import com.simsilica.sim.GameSystemManager;

import com.simsilica.ethereal.EtherealHost;

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.server.EntityDataHostedService;

/**
 *  Provides game session management for connected players.  This is where
 *  all of the game-session-specific state is organized and managed on behalf
 *  of a client.  The game systems are concerned with the state of 'everyone'
 *  but a game session is specific to a player.
 *
 *  @author    Paul Speed
 */
public class GameSessionHostedService extends AbstractHostedConnectionService {

    static Logger log = LoggerFactory.getLogger(GameSessionHostedService.class);

    private static final String ATTRIBUTE_SESSION = "game.session";

    private GameSystemManager gameSystems;
    private EntityData ed;

    private RmiHostedService rmiService;
    private AccountObserver accountObserver = new AccountObserver();

    private List<GameSessionImpl> players = new CopyOnWriteArrayList<>();
 
    public GameSessionHostedService( GameSystemManager gameSystems ) {
    
        this.gameSystems = gameSystems;
    
        // We do not autohost because we want to host only when the
        // player is actually logged on.
        setAutoHost(false);
        
        // Make sure that quaternions are registered with the serializer
        Serializer.registerClass(Quaternion.class, new FieldSerializer());
    }

    
    @Override
    protected void onInitialize( HostedServiceManager s ) {
        
        // Grab the RMI service so we can easily use it later        
        this.rmiService = getService(RmiHostedService.class);
        if( rmiService == null ) {
            throw new RuntimeException("GameSessionHostedService requires an RMI service.");
        }
 
        // Register ourselves to listen for global account events
        EventBus.addListener(accountObserver, AccountEvent.playerLoggedOn, AccountEvent.playerLoggedOff);
    }

    @Override
    public void start() {
        super.start();
            
        EntityDataHostedService eds = getService(EntityDataHostedService.class);
        if( eds == null ) {
            throw new RuntimeException("AccountHostedService requires an EntityDataHostedService");
        }
        this.ed = eds.getEntityData();
    }
 
    @Override
    public void startHostingOnConnection( HostedConnection conn ) {
        throw new UnsupportedOperationException("Call the startHostingOnConnection(conn, player) version instead.");
    }
    
    public void startHostingOnConnection( HostedConnection conn, EntityId player ) { 
        
        log.debug("startHostingOnConnection(" + conn + ")");
    
        GameSessionImpl session = new GameSessionImpl(player, conn);
        conn.setAttribute(ATTRIBUTE_SESSION, session);
        
        // Expose the session as an RMI resource to the client
        RmiRegistry rmi = rmiService.getRmiRegistry(conn);
        rmi.share(session, GameSession.class);
 
        players.add(session);
        
        session.initialize();
 
        // Setup to start using SimEthereal synching
        getService(EtherealHost.class).startHostingOnConnection(conn);
        getService(EtherealHost.class).setConnectionObject(conn, session.characterEntity.getId(), new Vec3d());
 
        // Start hosting on the chat server also
        String name = AccountHostedService.getPlayerName(conn);
        getService(ChatHostedService.class).startHostingOnConnection(conn, name);
    }
 
    protected GameSessionImpl getGameSession( HostedConnection conn ) {
        return conn.getAttribute(ATTRIBUTE_SESSION);
    }
    
    @Override   
    public void stopHostingOnConnection( HostedConnection conn ) {
        log.debug("stopHostingOnConnection(" + conn + ")");
        
        GameSessionImpl session = getGameSession(conn);
        if( session != null ) {
            
            session.close();

            // Remove this connection from the chat service also.
            // Note: the chat service will have to take care that it
            // might get two such calls if the connection is being closed. 
            getService(ChatHostedService.class).stopHostingOnConnection(conn);
        
            players.remove(session);
 
            // Clear the session so we know we are logged off
            conn.setAttribute(ATTRIBUTE_SESSION, null);
            
            // If we don't do that then we'll be notified twice when the
            // player logs off.  Once because we detect the connection shutting
            // down and again because the account service has notified us the
            // player has logged off.  This is ok because sometime there might
            // be a reason the player logs out of the game session but stays
            // connected.  We just need to cover the double-event case by
            // checkint for an existing account session and then clearing it
            // when we've stopped hosting our service on it.
        }   
    }
    
    private class AccountObserver {
        
        public void onPlayerLoggedOn( AccountEvent event ) {
            log.debug("onPlayerLoggedOn()");
            startHostingOnConnection(event.getConnection(), event.getPlayerEntity());            
        }
        
        public void onPlayerLoggedOff( AccountEvent event ) {
            log.debug("onPlayerLoggedOff()");
            stopHostingOnConnection(event.getConnection());   
        }
    }

    /**
     *  The connection-specific 'host' for the GameSession.
     */ 
    private class GameSessionImpl implements GameSession {
 
        private HostedConnection conn;
        private GameSessionListener callback;
        private EntityId playerEntity;
        private EntityId characterEntity;
        private CharInputDriver characterDriver;
        
        public GameSessionImpl( EntityId playerEntity, HostedConnection conn ) {
            this.playerEntity = playerEntity;
            this.conn = conn;
            
            // Create a character for the player
            this.characterEntity = GameEntities.createCharacter(playerEntity, ed);
            this.characterDriver = new CharInputDriver(new CharPhysics());
            
            // Set the ship driver directly on the Body.  This could
            // also have been managed with a component-based system but 
            // that will wait.
            gameSystems.get(BulletSystem.class, true).setControlDriver(characterEntity, characterDriver);
            
            // Set the position when we want the ship to actually appear
            // in space 'for real'.
            ed.setComponent(characterEntity, new Position(1, 1, 1));
            System.out.println("Set position on:" + characterEntity);
        }
 
        public void initialize() {
        }
 
        public void close() {
            log.debug("Closing game session for:" + conn);        
            // Remove our physics body
            //physics.removeBody(characterEntity);
            // Physics body is now removed as a side-effect of the entity
            // going away.
            
            // Remove the character we created
            ed.removeEntity(characterEntity);
        }
 
        @Override
        public EntityId getPlayer() {
            return playerEntity;
        }

        @Override
        public EntityId getShip() {
            return characterEntity;
        }
        
        @Override   
        public void move( Quaternion rotation, Vector3f thrust, CharFlags flags) {
            if( log.isTraceEnabled() ) {
                log.trace("move(" + rotation + ", " + thrust + ", " + flags.toString() + " )");
            }

            // Need to forward this to the game world
            //characterDriver.applyMovementState(rotation, thrust);
            characterDriver.setInput(rotation, thrust, flags);
        }
        
        protected GameSessionListener getCallback() {
            if( callback == null ) {
                RmiRegistry rmi = rmiService.getRmiRegistry(conn);
                callback = rmi.getRemoteObject(GameSessionListener.class);
                if( callback == null ) {
                    throw new RuntimeException("Unable to locate client callback for GameSessionListener");
                }
            }
            return callback;
        } 
 
    }    
}

