package com.oddlabs.tt.global;

/**
 * Whether this JVM runs the simulation without a window: no OpenGL context and no OpenAL device. Resource loaders
 * still read every number the simulation uses (sprite bounds and animation types, heights, supplies) but skip
 * textures, vertex buffers and sounds. Set once, before any world is built; the headless match runner does this.
 */
public final class Headless {
    private static volatile boolean enabled;

    private Headless() {
    }

    public static void enable() {
        enabled = true;
    }

    public static boolean isEnabled() {
        return enabled;
    }
}
