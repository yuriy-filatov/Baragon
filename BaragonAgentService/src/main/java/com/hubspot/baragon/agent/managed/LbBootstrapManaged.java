package com.hubspot.baragon.agent.managed;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Stopwatch;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.baragon.agent.BaragonAgentServiceModule;
import com.hubspot.baragon.config.LoadBalancerConfiguration;
import com.hubspot.baragon.data.BaragonStateDatastore;
import com.hubspot.baragon.lbs.LbConfigHelper;
import com.hubspot.baragon.models.Service;
import com.hubspot.baragon.models.ServiceContext;
import io.dropwizard.lifecycle.Managed;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.curator.framework.recipes.leader.LeaderLatch;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

public class LbBootstrapManaged implements Managed {
  private static final Log LOG = LogFactory.getLog(LbBootstrapManaged.class);
  
  private final LoadBalancerConfiguration loadBalancerConfiguration;
  private final LbConfigHelper configHelper;
  private final BaragonStateDatastore stateDatastore;
  private final LeaderLatch leaderLatch;
  
  @Inject
  public LbBootstrapManaged(BaragonStateDatastore stateDatastore, LoadBalancerConfiguration loadBalancerConfiguration,
                            LbConfigHelper configHelper, @Named(BaragonAgentServiceModule.AGENT_LEADER_LATCH) LeaderLatch leaderLatch) {
    this.loadBalancerConfiguration = loadBalancerConfiguration;
    this.configHelper = configHelper;
    this.stateDatastore = stateDatastore;
    this.leaderLatch = leaderLatch;
  }

  private void applyCurrentConfigs() {
    final long now = System.currentTimeMillis();
    LOG.info("Loading current state of the world from zookeeper...");

    final Stopwatch stopwatch = new Stopwatch();

    stopwatch.start();
    final Collection<String> services = stateDatastore.getServices();

    for (String serviceId : services) {
      final Optional<Service> maybeServiceInfo = stateDatastore.getService(serviceId);
      if (!maybeServiceInfo.isPresent()) {
        continue;  // doubt this will ever happen
      }

      final Service service = maybeServiceInfo.get();
      final Collection<String> upstreams = stateDatastore.getUpstreams(serviceId);

      if (service.getLbs() == null || !service.getLbs().contains(loadBalancerConfiguration.getName())) {
        LOG.info(String.format("   Skipping %s, not applicable to this LB cluster", serviceId));
        continue;
      }

      LOG.info(String.format("    Applying %s: [%s]", serviceId, Joiner.on(", ").join(upstreams)));

      configHelper.apply(new ServiceContext(service, upstreams, now));
    }

    LOG.info(String.format("Applied %d services in %sms", services.size(), stopwatch.elapsed(TimeUnit.MILLISECONDS)));
  }

  @Override
  public void start() throws Exception {
    applyCurrentConfigs();

    LOG.info("Starting leader latch...");
    leaderLatch.start();
  }

  @Override
  public void stop() throws Exception {
    leaderLatch.close();
  }

}
