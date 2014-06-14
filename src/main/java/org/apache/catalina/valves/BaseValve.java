package org.apache.catalina.valves;

import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.util.LifecycleSupport;

public abstract class BaseValve extends ValveBase implements Lifecycle {

    /**
     * The lifecycle event support for this component.
     */
    protected LifecycleSupport lifecycle = new LifecycleSupport(this);

    /**
     * enabled this component
     */
    protected boolean enabled = true;

    /**
     * The pattern used to format our access log lines.
     */
    protected String pattern = null;

    /**
     * Has this component been started yet?
     */
    protected boolean started = false;

    /**
     * Add a lifecycle event listener to this component.
     *
     * @param listener
     *        The listener to add
     */
    public void addLifecycleListener(final LifecycleListener listener) {
        lifecycle.addLifecycleListener(listener);
    }

    /**
     * Get the lifecycle listeners associated with this lifecycle. If this
     * Lifecycle has no listeners registered, a zero-length array is returned.
     */
    public LifecycleListener[] findLifecycleListeners() {
        return lifecycle.findLifecycleListeners();
    }

    /**
     * Remove a lifecycle event listener from this component.
     *
     * @param listener
     *        The listener to add
     */
    public void removeLifecycleListener(final LifecycleListener listener) {
        lifecycle.removeLifecycleListener(listener);
    }

    /**
     * Prepare for the beginning of active use of the public methods of this
     * component. This method should be called after <code>configure()</code>,
     * and before any of the public methods of the component are utilized.
     *
     * @exception LifecycleException
     *            if this component detects a fatal error
     *            that prevents this component from being used
     */
    public void startInternal() throws LifecycleException {

        // Validate and update our current component state
        if (started) {
            throw new LifecycleException("valve already started");
        }

        setState(LifecycleState.STARTING);
        started = true;

        afterStart();
    }

    /**
     * Gracefully terminate the active use of the public methods of this
     * component. This method should be the last one called on a given
     * instance of this component.
     *
     * @exception LifecycleException
     *            if this component detects a fatal error
     *            that needs to be reported
     */
    public void stopInternal() throws LifecycleException {

        // Validate and update our current component state
        if (!started) {
            throw new LifecycleException("valve not started");
        }

        setState(LifecycleState.STOPPING);

        started = false;

        afterStop();
    }

    @SuppressWarnings("unused")
    protected void afterStart() throws LifecycleException {
    }

    @SuppressWarnings("unused")
    protected void afterStop() throws LifecycleException {
    }
}