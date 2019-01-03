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

import com.jme3.network.HostedConnection;
import com.jme3.network.Network;
import com.jme3.network.Server;
import com.jme3.network.serializing.Serializer;
import com.jme3.network.serializing.serializers.FieldSerializer;
import com.jme3.network.service.AbstractHostedService;
import com.jme3.network.service.HostedServiceManager;
import com.jme3.network.service.rmi.RmiHostedService;
import com.jme3.network.service.rpc.RpcHostedService;
import com.jmonkeyengine.monake.GameConstants;
import com.jmonkeyengine.monake.SimpleErrorHandlingSystem;
import com.jmonkeyengine.monake.bullet.*;
import com.jmonkeyengine.monake.es.BodyPosition;
import com.jmonkeyengine.monake.es.ObjectType;
import com.jmonkeyengine.monake.es.Position;
import com.jmonkeyengine.monake.es.SphereShape;
import com.jmonkeyengine.monake.es.components.AmmoComponent;
import com.jmonkeyengine.monake.es.components.ArmorComponent;
import com.jmonkeyengine.monake.es.components.EffectComponent;
import com.jmonkeyengine.monake.es.components.HealthComponent;
import com.jmonkeyengine.monake.es.components.IsPickupComponent;
import com.jmonkeyengine.monake.net.chat.server.ChatHostedService;
import com.jmonkeyengine.monake.sim.*;
import com.jmonkeyengine.monake.util.server.ServerApplication;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.Name;
import com.simsilica.es.base.DefaultEntityData;
import com.simsilica.es.common.Decay;
import com.simsilica.es.server.EntityDataHostedService;
import com.simsilica.es.server.EntityUpdater;
import com.simsilica.ethereal.EtherealHost;
import com.simsilica.ethereal.NetworkStateListener;
import com.simsilica.sim.GameLoop;
import com.simsilica.sim.GameSystemManager;
import com.simsilica.sim.common.DecaySystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

// To test something
/**
 * The main GameServer that manages the back end game services, hosts
 * connections, etc..
 *
 * @author Paul Speed
 */
public class GameServer {

    static Logger log = LoggerFactory.getLogger(GameServer.class);

    private Server server;
    private GameSystemManager systems;
    private GameLoop loop;

    private String description;

    public GameServer(int port, String description) throws IOException {
        this.description = description;

        // Make sure we are running with a fresh serializer registry
        Serializer.initialize();

        this.systems = new GameSystemManager();
        // Update at Tick 30, not Tick 60, allows CPU consumption throttling
        //this.loop = new GameLoop(systems, 2L * 16666667L);
        this.loop = new GameLoop(systems);
        //loop.setIdleSleepTime(1L);

        // Create the SpiderMonkey server and setup our standard
        // initial hosted services 
        this.server = Network.createServer(GameConstants.GAME_NAME,
                GameConstants.PROTOCOL_VERSION,
                port, port);

        // Create a separate channel to do chat stuff so it doesn't interfere
        // with any real game stuff.
        server.addChannel(port + 1);

        // And a separate channel for ES stuff
        server.addChannel(port + 2);

        // Do some rearranging of the service ordering because we want
        // to add a delay to see if we can recreate an issue a user is
        // seeing.        
        //ServerSerializerRegistrationsService regSvc = server.getServices().getService(ServerSerializerRegistrationsService.class);        
        //server.getServices().removeService(regSvc);
        //server.getServices().addService(new DelayService());
        //server.getServices().addService(regSvc);
        // Adding a delay for the connectionAdded right after the serializer registration
        // service gets to run let's the client get a small break in the buffer that should
        // generally prevent the RpcCall messages from coming too quickly and getting processed
        // before the SerializerRegistrationMessage has had a chance to process.
        server.getServices().addService(new DelayService());

        server.getServices().addServices(new RpcHostedService(),
                new RmiHostedService(),
                new AccountHostedService(description),
                new GameSessionHostedService(systems),
                new ChatHostedService(GameConstants.CHAT_CHANNEL)
        );

        // Add the SimEtheral host that will serve object sync updates to
        // the clients. 
        EtherealHost ethereal = new EtherealHost(GameConstants.OBJECT_PROTOCOL,
                GameConstants.ZONE_GRID,
                GameConstants.ZONE_RADIUS);

        // See https://hub.jmonkeyengine.org/t/sim-eth-es-troubleshootings/41249/10
        ethereal.setTimeSource(() -> systems.getStepTime().getUnlockedTime(System.nanoTime()));

        server.getServices().addService(ethereal);

        // Add the various game services to the GameSystemManager
        // Add any hosted services that require those systems to already
        // exist
        // We'll add the decay system first so that things that need to will
        // get cleaned up at the beginning of update.
        systems.addSystem(new DecaySystem() {
            protected void destroyEntity(Entity e) {
                super.destroyEntity(e);
                //log.info("Destroyed:" + e.getId());
            }
        });

        // Add a system that will forward physics changes to the Ethereal 
        // zone manager       
        systems.addSystem(new com.jmonkeyengine.monake.bulletimpl.ZoneNetworkSystem(ethereal.getZones()));

        // Setup our entity data and the hosting service
        DefaultEntityData ed = new DefaultEntityData();
        server.getServices().addService(new EntityDataHostedService(GameConstants.ES_CHANNEL, ed));

        // Add it to the game systems so that we send updates properly
        systems.addSystem(new EntityUpdater(server.getServices().getService(EntityDataHostedService.class)));

        // Add another publisher to post object updates to our server-side
        // BodyPosition components.  This will also make BodyPosition available
        // on the clients.
        systems.addSystem(new BodyPositionPublisher());

        CollisionShapes shapes = systems.register(CollisionShapes.class,
                new DefaultCollisionShapes(ed, CollisionShapeProvider::lookup));

        CollisionShapeProvider.registerDefaults(ed, shapes);

        BulletSystem bullet = new BulletSystem();
        // Only Needed with
        // bullet.addPhysicsObjectListener(new DebugPhysicsListener(ed));
        
        // this chunk of code basically makes everything horrible, and broken
        // we should only publish physics contacts we care about into the entity system
        
//        bullet.addEntityCollisionListener(new DefaultContactPublisher(ed) {
//            /**
//             * Overridden to give some extra contact decay time so the debug
//             * visualization always has a chance to see them.
//             */
//            @Override
//            protected EntityId createEntity(Contact c) {
//                //System.out.println("Some Collision it seems");
//                EntityId result = ed.createEntity();
//                ed.setComponents(result, c,
//                        Decay.duration(systems.getStepTime().getTime(),
//                                systems.getStepTime().toSimTime(0.1))
//                );
//                return result;
//            }
//        });

        systems.register(BulletSystem.class, bullet);

        // Register some custom serializers
        registerSerializers();

        // Make the EntityData available to other systems
        systems.register(EntityData.class, ed);

        // Add a system for creating the basic "world" entities
        systems.addSystem(new BasicEnvironment());

        // Some Errors aren't printed out so we listen for these events
        systems.addSystem(new SimpleErrorHandlingSystem());

        systems.register(ShootingSystem.class, new ShootingSystem(ethereal.getTimeSource()));

        systems.addSystem(new PickupSystem());

        log.info("Initializing game systems...");
        // Initialize the game system manager to prepare to start later
        systems.initialize();
    }

    protected void registerSerializers() {
        Serializer.registerClass(Name.class, new FieldSerializer());

        Serializer.registerClass(BodyPosition.class, new FieldSerializer());
        Serializer.registerClass(ObjectType.class, new FieldSerializer());
        Serializer.registerClass(Position.class, new FieldSerializer());
        Serializer.registerClass(SphereShape.class, new FieldSerializer());
        Serializer.registerClass(AmmoComponent.class, new FieldSerializer());
        Serializer.registerClass(ArmorComponent.class, new FieldSerializer());
        Serializer.registerClass(HealthComponent.class, new FieldSerializer());
        Serializer.registerClass(EffectComponent.class, new FieldSerializer());
        Serializer.registerClass(IsPickupComponent.class, new FieldSerializer());
        Serializer.registerClass(CharFlags.class, new FieldSerializer());
    }

    public Server getServer() {
        return server;
    }

    /**
     * Starts the systems and begins accepting remote connections.
     */
    public void start() {
        log.info("Starting game server...");
        systems.start();
        server.start();
        loop.start();
        log.info("Game server started.");
    }

    /**
     * Kicks all current connection, closes the network host, stops all systems,
     * and finally terminates them. The GameServer is not restartable at this
     * point.
     */
    public void close(String kickMessage) {
        log.info("Stopping game server..." + kickMessage);
        loop.stop();

        if (kickMessage != null) {
            for (HostedConnection conn : server.getConnections()) {
                conn.close(kickMessage);
            }
        }
        server.close();

        // The GameLoop dying should have already stopped the game systems
        if (systems.isInitialized()) {
            systems.stop();
            systems.terminate();
        }
        log.info("Game server stopped.");
    }

    /**
     * Closes the network host, stops all systems, and finally terminates them.
     * The GameServer is not restartable at this point.
     */
    public void close() {
        close(null);
    }

    /**
     * Logs the current connection statistics for each connection.
     */
    public void logStats() {

        EtherealHost host = server.getServices().getService(EtherealHost.class);

        for (HostedConnection conn : server.getConnections()) {
            log.info("Client[" + conn.getId() + "] address:" + conn.getAddress());
            NetworkStateListener listener = host.getStateListener(conn);
            if (listener == null) {
                log.info("[" + conn.getId() + "] No stats");
                continue;
            }
            log.info("[" + conn.getId() + "] Ping time: " + (listener.getConnectionStats().getAveragePingTime() / 1000000.0) + " ms");
            String miss = String.format("%.02f", listener.getConnectionStats().getAckMissPercent());
            log.info("[" + conn.getId() + "] Ack miss: " + miss + "%");
            log.info("[" + conn.getId() + "] Average msg size: " + listener.getConnectionStats().getAverageMessageSize() + " bytes");
        }
    }

    /**
     * Allow running a basic dedicated server from the command line using the
     * default port. If we want something more advanced then we should break it
     * into a separate class with a proper shell and so on.
     */
    public static void main(String... args) throws Exception {

        StringWriter sOut = new StringWriter();
        PrintWriter out = new PrintWriter(sOut);
        boolean hasDescription = false;
        for (int i = 0; i < args.length; i++) {
            if ("-m".equals(args[i])) {
                out.println(args[++i]);
                hasDescription = true;
            }
        }
        if (!hasDescription) {
            // Put a default description in
            out.println("Dedicated Server");
            out.println();
            out.println("In game:");
            out.println("WASD + mouse to fly");
            out.println("Enter to open chat bar");
            out.println("F5 to toggle stats");
            out.println("Esc to open in-game help");
            out.println("PrtScrn to save a screen shot");
        }

        out.close();
        String desc = sOut.toString();

        ServerApplication.createApplication();

        GameServer gs = new GameServer(GameConstants.DEFAULT_PORT, desc);
        gs.start();

        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        String line;
        while ((line = in.readLine()) != null) {
            if (line.length() == 0) {
                continue;
            }
            if ("exit".equals(line)) {
                break;
            } else if ("stats".equals(line)) {
                gs.logStats();
            } else {
                System.err.println("Unknown command:" + line);
            }
        }

        gs.close();
    }

    // Just for debugging something
    private class DelayService extends AbstractHostedService {

        private void safeSleep(long ms) {
            try {
                Thread.sleep(ms);
            } catch (InterruptedException e) {
                throw new RuntimeException("Checked exceptions are lame", e);
            }
        }

        @Override
        protected void onInitialize(HostedServiceManager serviceManager) {
            System.out.println("DelayService.onInitialize()");
            //safeSleep(2000);
            //System.out.println("DelayService.delay done");
        }

        @Override
        public void start() {
            System.out.println("DelayService.start()");
            //safeSleep(2000);
            //System.out.println("DelayService.delay done");
        }

        @Override
        public void connectionAdded(Server server, HostedConnection hc) {
            // Just in case
            super.connectionAdded(server, hc);
            System.out.println("DelayService.connectionAdded(" + hc + ")");
            safeSleep(500);
            System.out.println("DelayService.delay done");
        }
    }
}
