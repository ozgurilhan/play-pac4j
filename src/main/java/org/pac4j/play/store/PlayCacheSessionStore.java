package org.pac4j.play.store;

import com.google.inject.Inject;
import org.pac4j.core.context.Pac4jConstants;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.util.CommonHelper;
import org.pac4j.play.PlayWebContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.cache.CacheApi;
import play.mvc.Http;

/**
 * The cache storage uses the Play Cache, only an identifier is saved into the Play session.
 *
 * @author Jerome Leleu
 * @since 2.0.0
 */
public class PlayCacheSessionStore implements PlaySessionStore {

    private static final Logger logger = LoggerFactory.getLogger(PlayCacheSessionStore.class);

    private final static String SEPARATOR = "$";

    // prefix for the cache
    private String prefix = "";

    // 1 hour = 3600 seconds
    private int timeout = 3600;

    private final CacheApi cache;

    @Inject
    public PlayCacheSessionStore(final CacheApi cache) {
        this.cache = cache;
    }

    String getKey(final String sessionId, final String key) {
        return prefix + SEPARATOR + sessionId + SEPARATOR + key;
    }

    @Override
    public String getOrCreateSessionId(final PlayWebContext context) {
        final Http.Session session = context.getJavaSession();
        // get current sessionId
        String sessionId = session.get(Pac4jConstants.SESSION_ID);
        logger.trace("retrieved sessionId: {}", sessionId);
        // if null, generate a new one
        if (sessionId == null) {
            // generate id for session
            sessionId = java.util.UUID.randomUUID().toString();
            logger.debug("generated sessionId: {}", sessionId);
            // and save it to session
            session.put(Pac4jConstants.SESSION_ID, sessionId);
        }
        return sessionId;
    }

    @Override
    public Object get(final PlayWebContext context, final String key) {
        final String sessionId = getOrCreateSessionId(context);
        return cache.get(getKey(sessionId, key));
    }

    @Override
    public void set(final PlayWebContext context, final String key, final Object value) {
        final String sessionId = getOrCreateSessionId(context);
        cache.set(getKey(sessionId, key), value, timeout);
    }

    @Override
    public boolean invalidateSession(final PlayWebContext context) {
        final Http.Session session = context.getJavaSession();
        final String sessionId = session.get(Pac4jConstants.SESSION_ID);
        if (sessionId != null) {
            session.remove(Pac4jConstants.SESSION_ID);
            return true;
        }
        return false;
    }

    @Override
    public Object getTrackableSession(final PlayWebContext context) {
        return context.getJavaSession().get(Pac4jConstants.SESSION_ID);
    }

    @Override
    public SessionStore<PlayWebContext> buildFromTrackableSession(final PlayWebContext context, final Object trackableSession) {
        context.getJavaSession().put(Pac4jConstants.SESSION_ID, (String) trackableSession);
        return this;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(final String prefix) {
        this.prefix = prefix;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(final int timeout) {
        this.timeout = timeout;
    }

    @Override
    public String toString() {
        return CommonHelper.toString(this.getClass(), "cache", cache, "prefix", prefix, "timeout", timeout);
    }
}
