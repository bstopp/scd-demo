package com.github.bstopp.demo.scd.core.listeners;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.distribution.DistributionRequestType;
import org.apache.sling.distribution.Distributor;
import org.apache.sling.distribution.SimpleDistributionRequest;
import org.apache.sling.distribution.util.DistributionJcrUtils;
import org.apache.sling.jcr.api.SlingRepository;

import org.osgi.service.component.ComponentException;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        immediate = true
)
@Designate(ocd = PageModificationEventListener.Config.class)
public class PageModificationEventListener implements EventListener {

    @ObjectClassDefinition(
            name = "SCD Demo - Page Modification Event Listener",
            description = "Event listener which listens for and submits requests to distribute pages on updates."

    )
    @interface Config {
        @AttributeDefinition(
                name = "Service User Name",
                description = "The name of the Service User that this Service will use for Session access. "
        )
        String service_username();

        @AttributeDefinition(
                name = "Sling Distribution Agent",
                description = "The Sling Distribution Agent that will be used by this Service for Distribution events. "
        )
        String sling_distribution_agent();

        @AttributeDefinition(
                name = "Event Listener Path",
                description = "The JCR path on which this Listener will act."
        )
        String event_listener_path();
    }

    private Config config;

    @Reference
    private SlingRepository repository;

    @Reference
    private Distributor distributor;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    private static final Logger log = LoggerFactory.getLogger(PageModificationEventListener.class);

    private final int events = Event.NODE_ADDED | Event.PROPERTY_ADDED | Event.PROPERTY_CHANGED | Event.PROPERTY_REMOVED;
    private final String[] nodeTypes = new String[]{ "cq:PageContent" };
    private final DistributionRequestType distributionType = DistributionRequestType.ADD;

    private final boolean noLocal = true;
    private final boolean isDeep = true;
    private final String[] uuids = null;

    private Session observationSession = null;

    @Override
    public void onEvent(final EventIterator events) {
        ResourceResolver resourceResolver = null;
        Set<String> distributePaths = new HashSet<>();

        try {
            Map<String, Object> param = new HashMap<>();
            param.put(ResourceResolverFactory.SUBSERVICE, config.service_username());
            resourceResolver = resourceResolverFactory.getServiceResourceResolver(param);

            Session serviceSession = resourceResolver.adaptTo(Session.class);
            while (events.hasNext()) {
                Event event = events.nextEvent();
                if (!DistributionJcrUtils.isSafe(event)) { continue; }

                String pagePath = getPath(event, serviceSession);
                if (pagePath != null) {
                    log.debug("Adding for Distribution: {}", pagePath);
                    distributePaths.add(pagePath);
                }
            }

            if (!distributePaths.isEmpty()) {
                String[] distributePathsStrings = distributePaths.toArray(new String[distributePaths.size()]);
                SimpleDistributionRequest distributionRequest = new SimpleDistributionRequest(distributionType, isDeep, distributePathsStrings);
                distributor.distribute(config.sling_distribution_agent(), resourceResolver, distributionRequest);
            }

        } catch (LoginException ex) {
            log.error("LoginException", ex);
        } catch (RepositoryException ex) {
            log.error("RepositoryException", ex);
        } finally {
            if ((resourceResolver != null) && resourceResolver.isLive()) {
                resourceResolver.close();
            }
        }
    }

    // Finds the jcr:content node for the event.
    private String getPath(Event event, Session serviceSession) {
        String path = null;
        Node rootNode;
        Node eventNode;

        try {
            path = event.getPath();
            log.debug("Event Path: {}", path);


            if (serviceSession.itemExists(path)) {
                Item item = serviceSession.getItem(path);

                eventNode = item.isNode() ? serviceSession.getNode(path) : item.getParent();
                log.debug("Event Node: {}", eventNode.getPath());
            } else {
                return null;
            }

            rootNode = serviceSession.getRootNode();
            boolean found = eventNode.isNodeType("cq:PageContent");
            while (!eventNode.isSame(rootNode) && !found) {
                eventNode = eventNode.getParent();
                log.debug("Event Node: {}", eventNode.getPath());
            }

            path = (found ? eventNode.getPath() : null);
            log.debug("getPath: {}", path);
        } catch (RepositoryException ex) {
            log.error("RepositoryException", ex);
        }

        return path;
    }


    @Activate
    @Modified
    public void activate(Config config) throws ComponentException {

        log.trace("Activation start..");
        log.debug("Service User Name {}", config.service_username());
        log.debug("Sling Distribution Agent {}", config.sling_distribution_agent());
        log.debug("Event Listener Path {}", config.event_listener_path());
        log.debug("Events: {}", events);
        log.debug("Node Type: {}", nodeTypes);
        log.debug("Distribution Type: {}", distributionType);

        this.config = config;
        try {
            registerListener();
        } catch (RepositoryException ex) {
            log.error("Unable to register the listener for events.", ex);
            throw new ComponentException("Unable to register listener", ex);
        }

        log.trace("Activated.");
    }

    @Deactivate
    public void deactivate(Config config) throws RepositoryException {
        log.trace("Deactivation start.");
        unregisterListener();
        log.trace("Deactivation complete.");

    }

    private void registerListener() throws RepositoryException {
        observationSession = repository.loginService(config.service_username(), null);
        final ObservationManager observationManager = observationSession.getWorkspace().getObservationManager();
        observationManager.addEventListener(this, events, config.event_listener_path(), isDeep, uuids, nodeTypes, noLocal);
    }

    private void unregisterListener() {
        try {
            final ObservationManager observationManager = observationSession.getWorkspace().getObservationManager();

            if (observationManager != null) {
                observationManager.removeEventListener(this);
            }

            log.info("Collection Event Listener Deactivated..");
        } catch(RepositoryException ex) {
            log.error("An error occurred while trying to deactivate this EventListener", ex);
        } finally {
            if (observationSession != null) {
                observationSession.logout();
                observationSession = null;
            }
        }
    }

}