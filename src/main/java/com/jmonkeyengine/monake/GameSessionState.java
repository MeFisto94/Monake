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
package com.jmonkeyengine.monake;

import com.jme3.app.Application;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jmonkeyengine.monake.debug.TimeSequenceState;
import com.jmonkeyengine.monake.net.GameSessionListener;
import com.jmonkeyengine.monake.net.chat.ChatSessionListener;
import com.jmonkeyengine.monake.net.chat.client.ChatClientService;
import com.jmonkeyengine.monake.net.client.GameSessionClientService;
import com.jmonkeyengine.monake.view.*;
import com.jmonkeyengine.monake.view.effects.EffectsState;
import com.simsilica.es.EntityId;
import com.simsilica.ethereal.SynchedTimeSource;
import com.simsilica.event.EventBus;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.input.InputMapper;
import com.simsilica.state.CompositeAppState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The core state that manages the game session. This has several child app
 * states whose lifecycles are directly linked to this one.
 *
 * @author Paul Speed
 */
public class GameSessionState extends CompositeAppState {

    static Logger log = LoggerFactory.getLogger(GameSessionState.class);

    private GameSessionObserver gameSessionObserver = new GameSessionObserver();
    private ChatSessionObserver chatSessionObserver = new ChatSessionObserver();
    private ChatCommandEntry chatEntry = new ChatCommandEntry();

    // Temporary reference FIXME
    private PlayerMovementState us;
    private int clientId;

    private EntityId playerId;
    private EntityId characterId;

    public GameSessionState() {
        // add normal states on the super-constructor
        super(new MessageState(),
                new TimeState(), // Has to be before any visuals that might need it.
                new SkyState(),
                new ModelViewState(),
                new PlayerMovementState(),
                new HudLabelState(),
                new EffectsState(),
                new SpaceGridState(GameConstants.GRID_CELL_SIZE, 10, new ColorRGBA(0.8f, 1f, 1f, 0.5f))
        //new SpaceGridState(2, 10, ColorRGBA.White) 
        );

        // Add states that need to support enable/disable independent of
        // the outer state using addChild().
        addChild(new InGameMenuState(false), true);
        addChild(new CommandConsoleState(), true);

        // For popping up a time sync debug panel
        addChild(new TimeSequenceState(), true);

        addChild(new HelpState(), true);
        addChild(new PlayerListState(), true);

        // @TODO: Somewhen in the end of 2019, try again physics debug on the client (maybe synchronize physics then)
        /*addChild(new PhysicsDebugState(getState(ConnectionState.class).getEntityData(), shapes, new PositionAdapterImpl()), false);
        addChild(new ContactDebugState(getState(ConnectionState.class).getEntityData()), false);*/
    }

    public EntityId getCharacterId() {
        return characterId;
    }

    public void disconnect() {
        // Remove ourselves
        getStateManager().detach(this);
    }

    @Override
    protected void initialize(Application app) {
        super.initialize(app);
        log.info("initialize()");

        EventBus.publish(GameSessionEvent.sessionStarted, new GameSessionEvent());

        // Add a self-message because we're too late to have caught the
        // player joined message for ourselves.  (Please we'd want it to look like this, anyway.)
        getState(MessageState.class).addMessage("> You have joined the game.", ColorRGBA.Yellow);

        getState(ConnectionState.class).getService(GameSessionClientService.class).addGameSessionListener(gameSessionObserver);

        // Give the time state its time source
        SynchedTimeSource timeSource = getState(ConnectionState.class).getRemoteTimeSource();
        getState(TimeState.class).setTimeSource(timeSource);

        // Setup the chat related services
        getState(ConnectionState.class).getService(ChatClientService.class).addChatSessionListener(chatSessionObserver);
        getState(CommandConsoleState.class).setCommandEntry(chatEntry);

        InputMapper inputMapper = GuiGlobals.getInstance().getInputMapper();
        inputMapper.activateGroup(MainGameFunctions.IN_GAME);

        // Temporary FIXME
        clientId = getState(ConnectionState.class).getClientId();
        us = getState(PlayerMovementState.class);
        playerId = getState(ConnectionState.class).getService(GameSessionClientService.class).getPlayer();
        characterId = getState(ConnectionState.class).getService(GameSessionClientService.class).getShip();
        log.info("Player object:" + characterId);
        us.setCharacterId(characterId);

        // Maybe bad style, but we need to ensure characterId is here...
        // @TODO: Maybe add it to the above list and just place the access into the first update tick?
        addChild(new RealHudLabelState(), false);

        ((Main) getApplication()).getRootNode().addLight(new DirectionalLight(Vector3f.UNIT_Y.negate()));
        ((Main) getApplication()).getRootNode().addLight(new AmbientLight(ColorRGBA.White.mult(0.5f)));
    }

    @Override
    protected void cleanup(Application app) {

        InputMapper inputMapper = GuiGlobals.getInstance().getInputMapper();
        inputMapper.deactivateGroup(MainGameFunctions.IN_GAME);

        EventBus.publish(GameSessionEvent.sessionEnded, new GameSessionEvent());

        // The below will fail because there is no message state anymore... so
        // it wouldn't show the message anyway.       
        // getState(MessageState.class).addMessage("> You have left the game.", ColorRGBA.Yellow);
        getChild(CommandConsoleState.class).setCommandEntry(null);

        super.cleanup(app);
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        GuiGlobals.getInstance().setCursorEventsEnabled(false);
    }

    @Override
    protected void onDisable() {
        super.onEnable();
        GuiGlobals.getInstance().setCursorEventsEnabled(true);
    }

    /**
     * Notified by the server about game-session related events.
     */
    private class GameSessionObserver implements GameSessionListener {

    }

    /**
     * Hooks into the CommandConsoleState to forward messages to the chat
     * service.
     */
    private class ChatCommandEntry implements CommandEntry {

        @Override
        public void runCommand(String cmd) {
            getState(ConnectionState.class).getService(ChatClientService.class).sendMessage(cmd);
        }
    }

    /**
     * Notified by the server about chat-releated events.
     */
    private class ChatSessionObserver implements ChatSessionListener {

        @Override
        public void playerJoined(int clientId, String playerName) {
            getState(MessageState.class).addMessage("> " + playerName + " has joined.", ColorRGBA.Yellow);
        }

        @Override
        public void newMessage(int clientId, String playerName, String message) {
            message = message.trim();
            if (message.length() == 0) {
                return;
            }
            getState(MessageState.class).addMessage(playerName + " said:" + message, ColorRGBA.White);
        }

        @Override
        public void playerLeft(int clientId, String playerName) {
            getState(MessageState.class).addMessage("> " + playerName + " has left.", ColorRGBA.Yellow);
        }
    }
}
