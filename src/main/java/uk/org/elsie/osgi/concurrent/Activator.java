package uk.org.elsie.osgi.concurrent;

import java.util.Hashtable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

public class Activator implements BundleActivator, ServiceListener {

	private static Log log = LogFactory.getLog(Activator.class);

	public static final String CORE_POOL_SIZE = "corePoolSize";
	public static final String SHUTDOWN_TIMEOUT = "shutdownTimeout";
	public static final String SHUTDOWN_TIMEOUT_UNIT = "shutdownTimeoutUnit";
	public static final String MAX_THREADS = "maximumPoolSize";

	private ScheduledThreadPoolExecutor executorService;
	private ServiceTracker executorServiceTracker;
	private long shutdownTimeout = 10;
	private TimeUnit shutdownTimeoutUnits = TimeUnit.SECONDS;

	public void start(BundleContext context) throws Exception {	
		int initialCorePoolSize = 1;
		executorService = new ScheduledThreadPoolExecutor(1);

		Hashtable<String, String> props = new Hashtable<String, String>();
		props.put(CORE_POOL_SIZE, Integer.toString(initialCorePoolSize));
		props.put(MAX_THREADS, Integer.toString(executorService.getMaximumPoolSize()));
		props.put(SHUTDOWN_TIMEOUT, Long.toString(shutdownTimeout));
		props.put(SHUTDOWN_TIMEOUT_UNIT, shutdownTimeoutUnits.toString());

		context.registerService(new String[] { ExecutorService.class.getName(), ScheduledExecutorService.class.getName() }, executorService, props);

		// create a tracker and track the service
		executorServiceTracker = new ServiceTracker(context, ExecutorService.class.getName(), null);
		executorServiceTracker.open();
		
	    context.addServiceListener(this, "(objectclass=" + ExecutorService.class.getName() + ")");
	}

	public void stop(BundleContext context) throws Exception {
		try {
			executorServiceTracker.close();
			executorService.shutdown();
			executorService.awaitTermination(shutdownTimeout, shutdownTimeoutUnits);
		} finally {
			executorService = null;
			executorServiceTracker = null;
		}
	}

	public void serviceChanged(ServiceEvent ev) {
		ServiceReference sr = ev.getServiceReference();
		switch(ev.getType()) {
			case ServiceEvent.MODIFIED:
				try {
					Integer i;

					i = Integer.parseInt(sr.getProperty(CORE_POOL_SIZE).toString());
					if(i.intValue() != executorService.getPoolSize()) {
						executorService.setCorePoolSize(i);
					}

					i = Integer.parseInt(sr.getProperty(MAX_THREADS).toString());
					if(i.intValue() != executorService.getMaximumPoolSize()) {
						executorService.setMaximumPoolSize(i);
					}
					
					this.shutdownTimeout = Long.parseLong(sr.getProperty(SHUTDOWN_TIMEOUT).toString());
					this.shutdownTimeoutUnits = TimeUnit.valueOf((String) sr.getProperty(SHUTDOWN_TIMEOUT_UNIT));
				} catch (Exception e) {
					log.error("Error trying to use new properties", e);
				}
			break;
		}
	}


}
