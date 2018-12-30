package com.jmonkeyengine.monake;

import com.simsilica.event.ErrorEvent;
import com.simsilica.event.EventBus;
import com.simsilica.event.EventListener;
import com.simsilica.sim.AbstractGameSystem;
import org.slf4j.LoggerFactory;

public class SimpleErrorHandlingSystem extends AbstractGameSystem {

    static org.slf4j.Logger log = LoggerFactory.getLogger(SimpleErrorHandlingSystem.class);

    ErrorObserver errorObserver = new ErrorObserver();

    @Override
    protected void initialize() {
        EventBus.addListener(errorObserver, ErrorEvent.fatalError, ErrorEvent.dispatchError);
    }

    @Override
    protected void terminate() {
        EventBus.removeListener(errorObserver, ErrorEvent.fatalError, ErrorEvent.dispatchError);
    }

    @Override
    public void start() {
    }

    private class ErrorObserver {
        public void fatalError(ErrorEvent event) {
            log.info("Fatal Error Occured: " + event);
            event.getError().printStackTrace();
        }

        public void dispatchError(ErrorEvent event) {
            log.info("Dispatch Error Occured: " + event);
            event.getError().printStackTrace();
        }
    }

}