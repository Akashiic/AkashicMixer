package com.akashiic.mixer.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Which target (a mixer channel) a sound played through Minecraft's own sound handler belongs to, by its sound event id
 * ({@code domain:path}, the key in the mod's {@code sounds.json}). The longest matching path prefix wins; the first
 * registration of a given (domain, prefix) wins. Sounds of the {@code minecraft} domain can never be routed: vanilla
 * categories stay under the vanilla sliders.
 * <p>
 * Read on the sound thread and the client thread on every volume computation, written rarely (pre-init): the table is
 * an immutable array replaced on write. Pure; tested in SoundRoutesTest.
 *
 * @param <T> the routing target
 */
public final class SoundRoutes<T> {

    private static final class Route<T> {

        final String domain;
        final String prefix;
        final T target;

        Route(String domain, String prefix, T target) {
            this.domain = domain;
            this.prefix = prefix;
            this.target = target;
        }
    }

    private volatile List<Route<T>> routes = new ArrayList<>();

    /**
     * Routes the sounds {@code domain:<pathPrefix>...} to {@code target}. An empty prefix routes the whole domain.
     *
     * @return false when (domain, prefix) was already routed (the first registration is kept)
     * @throws IllegalArgumentException for a null or empty domain, the {@code minecraft} domain, a null prefix or a
     *                                  null target
     */
    public synchronized boolean add(String domain, String pathPrefix, T target) {
        if (domain == null || domain.isEmpty()) throw new IllegalArgumentException("domain");
        if ("minecraft".equals(domain)) throw new IllegalArgumentException("vanilla sounds cannot be routed");
        if (pathPrefix == null) throw new IllegalArgumentException("pathPrefix");
        if (target == null) throw new IllegalArgumentException("target");
        for (Route<T> r : routes) if (r.domain.equals(domain) && r.prefix.equals(pathPrefix)) return false;
        List<Route<T>> next = new ArrayList<>(routes);
        next.add(new Route<>(domain, pathPrefix, target));
        routes = next;
        return true;
    }

    /** The target for the sound {@code domain:path}, or null when it is not routed. */
    public T find(String domain, String path) {
        if (domain == null || path == null) return null;
        Route<T> best = null;
        for (Route<T> r : routes) {
            if (!r.domain.equals(domain) || !path.startsWith(r.prefix)) continue;
            if (best == null || r.prefix.length() > best.prefix.length()) best = r;
        }
        return best == null ? null : best.target;
    }

    public boolean isEmpty() {
        return routes.isEmpty();
    }

    /**
     * The volume handed to the sound engine for a routed sound: what vanilla computes with the category slider
     * replaced by the channel level, clamped to 0..1 like vanilla.
     */
    public static float volume(float soundVolume, double entryVolume, float channelLevel) {
        double v = soundVolume * entryVolume * channelLevel;
        if (!(v > 0)) return 0f;
        return v >= 1 ? 1f : (float) v;
    }
}
