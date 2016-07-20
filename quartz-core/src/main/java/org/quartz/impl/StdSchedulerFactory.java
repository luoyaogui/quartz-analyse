/*
 * Copyright 2001-2009 Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 */

package org.quartz.impl;

import org.quartz.*;
import org.quartz.core.JobRunShellFactory;
import org.quartz.core.QuartzScheduler;
import org.quartz.core.QuartzSchedulerResources;
import org.quartz.ee.jta.JTAAnnotationAwareJobRunShellFactory;
import org.quartz.ee.jta.JTAJobRunShellFactory;
import org.quartz.ee.jta.UserTransactionHelper;
import org.quartz.impl.jdbcjobstore.JobStoreSupport;
import org.quartz.impl.jdbcjobstore.Semaphore;
import org.quartz.impl.jdbcjobstore.TablePrefixAware;
import org.quartz.impl.matchers.EverythingMatcher;
import org.quartz.management.ManagementRESTServiceConfiguration;
import org.quartz.simpl.RAMJobStore;
import org.quartz.simpl.SimpleThreadPool;
import org.quartz.spi.*;
import org.quartz.utils.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.*;
import java.lang.reflect.Method;
import java.security.AccessControlException;
import java.util.Collection;
import java.util.Locale;
import java.util.Properties;

/**
 * <p>
 * An implementation of <code>{@link org.quartz.SchedulerFactory}</code> that
 * does all of its work of creating a <code>QuartzScheduler</code> instance
 * based on the contents of a <code>Properties</code> file.
 * <--org.quartz.SchedulerFactory接口的一种实现，基于Properties属性文件
 * 的内容完成了创建一个QuartzScheduler实例的所有工作。-->
 * </p>
 *
 * <p>
 * By default a properties file named "quartz.properties" is loaded from the
 * 'current working directory'. If that fails, then the "quartz.properties"
 * file located (as a resource) in the org/quartz package is loaded. If you
 * wish to use a file other than these defaults, you must define the system
 * property 'org.quartz.properties' to point to the file you want.
 * <--当前工作目录下的"quartz.properties"文件作为默认属性文件进行加载导入。如果导入失败，位于org/quartz包中的"quartz.properties"文件(作为一个资源)
 * 将会被加载。如果你不希望使用那些默认属性，你必须定义系统属性'org.quartz.properties'来指定你想使用的配置文件。-->
 * </p>
 *
 * <p>
 * Alternatively, you can explicitly initialize the factory by calling one of
 * the <code>initialize(xx)</code> methods before calling <code>getScheduler()</code>.
 *<-- 可选择的，你可以在调用getScheduler()方法之前显示的调用initialize(xx)方法来初始化这个factory类。-->
 * </p>
 *
 * <p>
 * See the sample properties files that are distributed with Quartz for
 * information about the various settings available within the file.
 * Full configuration documentation can be found at
 * http://www.quartz-scheduler.org/docs/index.html
 * <--关于文件中各种变量设置的信息，可以在quartz分发版本中测试demo属性文件中看到。
 * 完整的配置文档可以在http://www.quartz-scheduler.org/docs/index.html进行查询了解。-->
 * </p>
 *
 * <p>
 * Instances of the specified <code>{@link org.quartz.spi.JobStore}</code>,
 * <code>{@link org.quartz.spi.ThreadPool}</code>, and other SPI classes will be created
 * by name, and then any additional properties specified for them in the config
 * file will be set on the instance by calling an equivalent 'set' method. For
 * example if the properties file contains the property
 * 'org.quartz.jobStore.myProp = 10' then after the JobStore class has been
 * instantiated, the method 'setMyProp()' will be called on it. Type conversion
 * to primitive Java types (int, long, float, double, boolean, and String) are
 * performed before calling the property's setter method.
 * <--实例化指定的org.quartz.spi.JobStore、org.quartz.spi.ThreadPool，和其他SPI类将通过名称来进行创建，而且对于这些实例的任何额外属性
 * 将通过实例的set方法进行设置。例如如果属性文件包含了属性'org.quartz.jobStore.myProp = 10'，当JobStore类初始化时将调用'setMyProp()'
 * 方法进行处理。在调用属性设置方法时，原生的java类型(int, long, float, double, boolean, and String)可执行相应的类型转换。-->
 * </p>
 * 
 * <p>
 * One property can reference another property's value by specifying a value
 * following the convention of "$@other.property.name", for example, to reference
 * the scheduler's instance name as the value for some other property, you
 * would use "$@org.quartz.scheduler.instanceName".
 * <--通过指定一个遵从"$@other.property.name"约定的值来保证一个属性可以参考另一个属性值，例如，参考scheduler的实例名称作为
 * 一些其他属性的值，你可以使用"$@org.quartz.scheduler.instanceName"。-->
 * </p> 
 *
 * @author James House
 * @author Anthony Eden
 * @author Mohammad Rezaei
 */
public class StdSchedulerFactory implements SchedulerFactory {

	/*
	 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	 *
	 * Constants.
	 *
	 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	 */

	public static final String PROPERTIES_FILE = "org.quartz.properties";

	public static final String PROP_SCHED_INSTANCE_NAME = "org.quartz.scheduler.instanceName";

	public static final String PROP_SCHED_INSTANCE_ID = "org.quartz.scheduler.instanceId";

	public static final String PROP_SCHED_INSTANCE_ID_GENERATOR_PREFIX = "org.quartz.scheduler.instanceIdGenerator";

	public static final String PROP_SCHED_INSTANCE_ID_GENERATOR_CLASS =
			PROP_SCHED_INSTANCE_ID_GENERATOR_PREFIX + ".class";

	public static final String PROP_SCHED_THREAD_NAME = "org.quartz.scheduler.threadName";

	public static final String PROP_SCHED_SKIP_UPDATE_CHECK = "org.quartz.scheduler.skipUpdateCheck";

	public static final String PROP_SCHED_BATCH_TIME_WINDOW = "org.quartz.scheduler.batchTriggerAcquisitionFireAheadTimeWindow";

	public static final String PROP_SCHED_MAX_BATCH_SIZE = "org.quartz.scheduler.batchTriggerAcquisitionMaxCount";

	public static final String PROP_SCHED_JMX_EXPORT = "org.quartz.scheduler.jmx.export";

	public static final String PROP_SCHED_JMX_OBJECT_NAME = "org.quartz.scheduler.jmx.objectName";

	public static final String PROP_SCHED_JMX_PROXY = "org.quartz.scheduler.jmx.proxy";

	public static final String PROP_SCHED_JMX_PROXY_CLASS = "org.quartz.scheduler.jmx.proxy.class";

	public static final String PROP_SCHED_RMI_EXPORT = "org.quartz.scheduler.rmi.export";

	public static final String PROP_SCHED_RMI_PROXY = "org.quartz.scheduler.rmi.proxy";

	public static final String PROP_SCHED_RMI_HOST = "org.quartz.scheduler.rmi.registryHost";

	public static final String PROP_SCHED_RMI_PORT = "org.quartz.scheduler.rmi.registryPort";

	public static final String PROP_SCHED_RMI_SERVER_PORT = "org.quartz.scheduler.rmi.serverPort";

	public static final String PROP_SCHED_RMI_CREATE_REGISTRY = "org.quartz.scheduler.rmi.createRegistry";

	public static final String PROP_SCHED_RMI_BIND_NAME = "org.quartz.scheduler.rmi.bindName";

	public static final String PROP_SCHED_WRAP_JOB_IN_USER_TX = "org.quartz.scheduler.wrapJobExecutionInUserTransaction";
	/** 它设置了 Quartz 能在哪里定位到应用服务器的 UserTransaction 管理器的 JNDI URL。默认值(未设定的话) 是 java:comp/UserTransaction ，这几乎能工作于所有的应用服务器中。Websphere 用户也许需要设置这个属性为 jta/usertransaction 。这个属性仅用于 Quartz 配置使用 JobStoreCMT 的情况，并且 org.quartz.scheduler.wrapJobExecutionInUserTransaction 被设定成了 true 。*/
	public static final String PROP_SCHED_USER_TX_URL = "org.quartz.scheduler.userTransactionURL";

	public static final String PROP_SCHED_IDLE_WAIT_TIME = "org.quartz.scheduler.idleWaitTime";

	public static final String PROP_SCHED_DB_FAILURE_RETRY_INTERVAL = "org.quartz.scheduler.dbFailureRetryInterval";

	public static final String PROP_SCHED_MAKE_SCHEDULER_THREAD_DAEMON = "org.quartz.scheduler.makeSchedulerThreadDaemon";

	public static final String PROP_SCHED_SCHEDULER_THREADS_INHERIT_CONTEXT_CLASS_LOADER_OF_INITIALIZING_THREAD = "org.quartz.scheduler.threadsInheritContextClassLoaderOfInitializer";

	public static final String PROP_SCHED_CLASS_LOAD_HELPER_CLASS = "org.quartz.scheduler.classLoadHelper.class";

	public static final String PROP_SCHED_JOB_FACTORY_CLASS = "org.quartz.scheduler.jobFactory.class";

	public static final String PROP_SCHED_JOB_FACTORY_PREFIX = "org.quartz.scheduler.jobFactory";

	public static final String PROP_SCHED_INTERRUPT_JOBS_ON_SHUTDOWN = "org.quartz.scheduler.interruptJobsOnShutdown";

	public static final String PROP_SCHED_INTERRUPT_JOBS_ON_SHUTDOWN_WITH_WAIT = "org.quartz.scheduler.interruptJobsOnShutdownWithWait";

	public static final String PROP_SCHED_CONTEXT_PREFIX = "org.quartz.context.key";

	public static final String PROP_THREAD_POOL_PREFIX = "org.quartz.threadPool";

	public static final String PROP_THREAD_POOL_CLASS = "org.quartz.threadPool.class";

	public static final String PROP_JOB_STORE_PREFIX = "org.quartz.jobStore";

	public static final String PROP_JOB_STORE_LOCK_HANDLER_PREFIX = PROP_JOB_STORE_PREFIX + ".lockHandler";

	public static final String PROP_JOB_STORE_LOCK_HANDLER_CLASS = PROP_JOB_STORE_LOCK_HANDLER_PREFIX + ".class";

	public static final String PROP_TABLE_PREFIX = "tablePrefix";

	public static final String PROP_SCHED_NAME = "schedName";

	public static final String PROP_JOB_STORE_CLASS = "org.quartz.jobStore.class";

	public static final String PROP_JOB_STORE_USE_PROP = "org.quartz.jobStore.useProperties";

	public static final String PROP_DATASOURCE_PREFIX = "org.quartz.dataSource";

	public static final String PROP_CONNECTION_PROVIDER_CLASS = "connectionProvider.class";

	/**
	 * @deprecated Replaced with {@link PoolingConnectionProvider#DB_DRIVER}
	 */
	@Deprecated
	public static final String PROP_DATASOURCE_DRIVER = "driver";

	/**
	 * @deprecated Replaced with {@link PoolingConnectionProvider#DB_URL}
	 */
	@Deprecated
	public static final String PROP_DATASOURCE_URL = "URL";

	/**
	 * @deprecated Replaced with {@link PoolingConnectionProvider#DB_USER}
	 */
	@Deprecated
	public static final String PROP_DATASOURCE_USER = "user";

	/**
	 * @deprecated Replaced with {@link PoolingConnectionProvider#DB_PASSWORD}
	 */
	@Deprecated
	public static final String PROP_DATASOURCE_PASSWORD = "password";

	/**
	 * @deprecated Replaced with {@link PoolingConnectionProvider#DB_MAX_CONNECTIONS}
	 */
	@Deprecated
	public static final String PROP_DATASOURCE_MAX_CONNECTIONS = "maxConnections";

	/**
	 * @deprecated Replaced with {@link PoolingConnectionProvider#DB_VALIDATION_QUERY}
	 */
	@Deprecated
	public static final String PROP_DATASOURCE_VALIDATION_QUERY = "validationQuery";

	public static final String PROP_DATASOURCE_JNDI_URL = "jndiURL";

	public static final String PROP_DATASOURCE_JNDI_ALWAYS_LOOKUP = "jndiAlwaysLookup";

	public static final String PROP_DATASOURCE_JNDI_INITIAL = "java.naming.factory.initial";

	public static final String PROP_DATASOURCE_JNDI_PROVDER = "java.naming.provider.url";

	public static final String PROP_DATASOURCE_JNDI_PRINCIPAL = "java.naming.security.principal";

	public static final String PROP_DATASOURCE_JNDI_CREDENTIALS = "java.naming.security.credentials";

	public static final String PROP_PLUGIN_PREFIX = "org.quartz.plugin";

	public static final String PROP_PLUGIN_CLASS = "class";

	public static final String PROP_JOB_LISTENER_PREFIX = "org.quartz.jobListener";

	public static final String PROP_TRIGGER_LISTENER_PREFIX = "org.quartz.triggerListener";

	public static final String PROP_LISTENER_CLASS = "class";

	public static final String DEFAULT_INSTANCE_ID = "NON_CLUSTERED";

	public static final String AUTO_GENERATE_INSTANCE_ID = "AUTO";

	public static final String PROP_THREAD_EXECUTOR = "org.quartz.threadExecutor";

	public static final String PROP_THREAD_EXECUTOR_CLASS = "org.quartz.threadExecutor.class";

	public static final String SYSTEM_PROPERTY_AS_INSTANCE_ID = "SYS_PROP";

	public static final String MANAGEMENT_REST_SERVICE_ENABLED = "org.quartz.managementRESTService.enabled";

	public static final String MANAGEMENT_REST_SERVICE_HOST_PORT = "org.quartz.managementRESTService.bind";

	/*
	 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	 *
	 * Data members.
	 *
	 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	 */

	private SchedulerException initException = null;

	private String propSrc = null;

	private PropertiesParser cfg;

	private final Logger log = LoggerFactory.getLogger(getClass());

	//  private Scheduler scheduler;

	/*
	 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	 *
	 * Constructors.
	 *
	 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	 */

	/**
	 * Create an uninitialized StdSchedulerFactory.
	 * 创建一个未初始化的StdSchedulerFactory
	 */
	public StdSchedulerFactory() {
		System.out.println("-----------------StdSchedulerFactory  源码修改测试-----------------");
	}

	/**
	 * Create a StdSchedulerFactory that has been initialized via
	 * <code>{@link #initialize(Properties)}</code>.
	 * 创建一个通过<code>{@link #initialize(Properties)}</code>方法初始化过的StdSchedulerFactory
	 *
	 * @see #initialize(Properties)
	 */
	public StdSchedulerFactory(Properties props) throws SchedulerException {
		initialize(props);
	}

	/**
	 * Create a StdSchedulerFactory that has been initialized via
	 * <code>{@link #initialize(String)}</code>.
	 * 创建一个通过<code>{@link #initialize(String)}</code>方法初始化过的StdSchedulerFactory
	 *
	 * @see #initialize(String)
	 */
	public StdSchedulerFactory(String fileName) throws SchedulerException {
		initialize(fileName);
	}

	/*
	 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	 *
	 * Interface.
	 *
	 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	 */

	public Logger getLog() {
		return log;
	}

	/**
	 * <p>
	 * Initialize the <code>{@link org.quartz.SchedulerFactory}</code> with
	 * the contents of a <code>Properties</code> file and overriding System
	 * properties.<--通过Properties文件内容和覆盖系统属性来初始化SchedulerFactory。-->
	 * </p>
	 *
	 * <p>
	 * By default a properties file named "quartz.properties" is loaded from
	 * the 'current working directory'. If that fails, then the
	 * "quartz.properties" file located (as a resource) in the org/quartz
	 * package is loaded. If you wish to use a file other than these defaults,
	 * you must define the system property 'org.quartz.properties' to point to
	 * the file you want.
	 * <--当前工作路径的"quartz.properties"文件作为默认配置文件进行加载。如果失败，则org/quartz路径下的
	 * "quartz.properties"（作为一个资源）将被加载。如果你不希望使用默认配置，你必须定义'org.quartz.properties'的
	 * 系统属性来指定你希望加载的配置文件。-->
	 * </p>
	 *
	 * <p>
	 * System properties (environment variables, and -D definitions on the
	 * command-line when running the JVM) override any properties in the
	 * loaded file.  For this reason, you may want to use a different initialize()
	 * method if your application security policy prohibits access to
	 * <code>{@link java.lang.System#getProperties()}</code>.
	 * <--系统属性（环境变量和运行JVM时指定的-D命令行）覆盖加载文件的任何属性。因为这个原因，如果你的应用安全策略禁止访问
	 * java.lang.System#getProperties()时，你可能想要使用不同的initialize()方法。-->
	 * </p>
	 */
	public void initialize() throws SchedulerException {
		// （已经初始化直接返回）short-circuit if already initialized
		if (cfg != null) {
			return;
		}
		if (initException != null) {
			throw initException;
		}
		//系统"org.quartz.properties"属性优先，如果存在则加载该文件，
		//否则按照当前目录./、根目录/、资源目录org/quartz/进行查找，没有则报告异常
		String requestedFile = System.getProperty(PROPERTIES_FILE);
		String propFileName = requestedFile != null ? requestedFile
				: "quartz.properties";
		File propFile = new File(propFileName);

		Properties props = new Properties();

		InputStream in = null;

		try {
			if (propFile.exists()) {
				try {
					if (requestedFile != null) {
						propSrc = "specified file: '" + requestedFile + "'";
					} else {
						propSrc = "default file in current working dir: 'quartz.properties'";
					}

					in = new BufferedInputStream(new FileInputStream(propFileName));
					props.load(in);

				} catch (IOException ioe) {
					initException = new SchedulerException("Properties file: '"
							+ propFileName + "' could not be read.", ioe);
					throw initException;
				}
			} else if (requestedFile != null) {
				in =
						Thread.currentThread().getContextClassLoader().getResourceAsStream(requestedFile);

				if(in == null) {
					initException = new SchedulerException("Properties file: '"
							+ requestedFile + "' could not be found.");
					throw initException;
				}

				propSrc = "specified file: '" + requestedFile + "' in the class resource path.";

				in = new BufferedInputStream(in);
				try {
					props.load(in);
				} catch (IOException ioe) {
					initException = new SchedulerException("Properties file: '"
							+ requestedFile + "' could not be read.", ioe);
					throw initException;
				}

			} else {
				propSrc = "default resource file in Quartz package: 'quartz.properties'";

				ClassLoader cl = getClass().getClassLoader();
				if(cl == null)
					cl = findClassloader();
				if(cl == null)
					throw new SchedulerConfigException("Unable to find a class loader on the current thread or class.");
				//加载属性配置文件
				in = cl.getResourceAsStream(
						"quartz.properties");

				if (in == null) {
					in = cl.getResourceAsStream(
							"/quartz.properties");
				}
				if (in == null) {
					in = cl.getResourceAsStream(
							"org/quartz/quartz.properties");
				}
				if (in == null) {
					initException = new SchedulerException(
							"Default quartz.properties not found in class path");
					throw initException;
				}
				try {
					props.load(in);
				} catch (IOException ioe) {
					initException = new SchedulerException(
							"Resource properties file: 'org/quartz/quartz.properties' "
									+ "could not be read from the classpath.", ioe);
					throw initException;
				}
			}
		} finally {
			if(in != null) {
				try { in.close(); } catch(IOException ignore) { /* ignore */ }
			}
		}

		initialize(overrideWithSysProps(props));////覆盖配置文件属性，进行配置关联
	}

	/**
	 * Add all System properties to the given <code>props</code>.  Will override
	 * any properties that already exist in the given <code>props</code>.
	 * 指定的系统属性将会覆盖已经存在于属性文件中的属性
	 */
	private Properties overrideWithSysProps(Properties props) {
		Properties sysProps = null;
		try {
			sysProps = System.getProperties();
		} catch (AccessControlException e) {
			getLog().warn(
					"Skipping overriding quartz properties with System properties " +
							"during initialization because of an AccessControlException.  " +
							"This is likely due to not having read/write access for " +
							"java.util.PropertyPermission as required by java.lang.System.getProperties().  " +
							"To resolve this warning, either add this permission to your policy file or " +
							"use a non-default version of initialize().",
							e);
		}

		if (sysProps != null) {
			props.putAll(sysProps);
		}

		return props;
	}

	/**
	 * <p>
	 * Initialize the <code>{@link org.quartz.SchedulerFactory}</code> with
	 * the contents of the <code>Properties</code> file with the given
	 * name.
	 * 通过指定属性文件名初始化SchedulerFactory。
	 * </p>
	 */
	public void initialize(String filename) throws SchedulerException {
		// short-circuit if already initialized
		if (cfg != null) {
			return;
		}

		if (initException != null) {
			throw initException;
		}

		InputStream is = null;
		Properties props = new Properties();

		is = Thread.currentThread().getContextClassLoader().getResourceAsStream(filename);

		try {
			if(is != null) {
				is = new BufferedInputStream(is);
				propSrc = "the specified file : '" + filename + "' from the class resource path.";
			} else {
				is = new BufferedInputStream(new FileInputStream(filename));
				propSrc = "the specified file : '" + filename + "'";
			}
			props.load(is);
		} catch (IOException ioe) {
			initException = new SchedulerException("Properties file: '"
					+ filename + "' could not be read.", ioe);
			throw initException;
		}
		finally {
			if(is != null)
				try { is.close(); } catch(IOException ignore) {}
		}

		initialize(props);
	}

	/**
	 * <p>
	 * Initialize the <code>{@link org.quartz.SchedulerFactory}</code> with
	 * the contents of the <code>Properties</code> file opened with the
	 * given <code>InputStream</code>.
	 * 通过属性文件的文件流初始化SchedulerFactory。
	 * </p>
	 */
	public void initialize(InputStream propertiesStream)
			throws SchedulerException {
		// short-circuit if already initialized
		if (cfg != null) {
			return;
		}

		if (initException != null) {
			throw initException;
		}

		Properties props = new Properties();

		if (propertiesStream != null) {
			try {
				props.load(propertiesStream);
				propSrc = "an externally opened InputStream.";
			} catch (IOException e) {
				initException = new SchedulerException(
						"Error loading property data from InputStream", e);
				throw initException;
			}
		} else {
			initException = new SchedulerException(
					"Error loading property data from InputStream - InputStream is null.");
			throw initException;
		}

		initialize(props);
	}

	/**
	 * <p>
	 * Initialize the <code>{@link org.quartz.SchedulerFactory}</code> with
	 * the contents of the given <code>Properties</code> object.
	 * 通过给定的属性文件对象初始化SchedulerFactory。
	 * </p>
	 */
	public void initialize(Properties props) throws SchedulerException {
		if (propSrc == null) {
			propSrc = "an externally provided properties instance.";
		}
		//设置配置文件解析
		this.cfg = new PropertiesParser(props);
	}

	private Scheduler instantiate() throws SchedulerException {
		if (cfg == null) {
			initialize();
		}

		if (initException != null) {
			throw initException;
		}
		//初始化
		JobStore js = null;
		ThreadPool tp = null;
		QuartzScheduler qs = null;//quartz的主体，也可进行远程调用（实现了Remote接口）
		DBConnectionManager dbMgr = null;//DB连接提供者的管理和透明访问
		String instanceIdGeneratorClass = null;//实例ID生成器实现类
		Properties tProps = null;
		String userTXLocation = null;//使用事务URL
		boolean wrapJobInTx = false;//用户事务包装Job执行
		boolean autoId = false;//自动ID生成标志
		long idleWaitTime = -1;		//空闲等待时间
		long dbFailureRetry = 15000L; // 15 secs   数据库失败重试时间间隔
		String classLoadHelperClass;
		String jobFactoryClass;
		ThreadExecutor threadExecutor;

		//获取全局scheduler实例注册中心
		SchedulerRepository schedRep = SchedulerRepository.getInstance();

		// Get Scheduler Properties
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		//读取配置文件，获取sched实例名（默认为QuartzScheduler）
		String schedName = cfg.getStringProperty(PROP_SCHED_INSTANCE_NAME,
				"QuartzScheduler");
		//读取配置文件，获取线程名（默认为sched实例名+_QuartzSchedulerThread）
		String threadName = cfg.getStringProperty(PROP_SCHED_THREAD_NAME,
				schedName + "_QuartzSchedulerThread");
		//读取配置文件，获取sched实例ID，默认为"NON_CLUSTERED"非集群
		String schedInstId = cfg.getStringProperty(PROP_SCHED_INSTANCE_ID,
				DEFAULT_INSTANCE_ID);

		if (schedInstId.equals(AUTO_GENERATE_INSTANCE_ID)) {//如果为自动生成ID，则设置自动ID标志为true，读取ID生成类为（默认为"org.quartz.simpl.SimpleInstanceIdGenerator"）
			autoId = true;
			instanceIdGeneratorClass = cfg.getStringProperty(
					PROP_SCHED_INSTANCE_ID_GENERATOR_CLASS,
					"org.quartz.simpl.SimpleInstanceIdGenerator");
		}
		else if (schedInstId.equals(SYSTEM_PROPERTY_AS_INSTANCE_ID)) {//如果为系统属性作为实例ID，则设置自动ID标志为true，ID生成类为"org.quartz.simpl.SystemPropertyInstanceIdGenerator"
			autoId = true;
			instanceIdGeneratorClass = 
					"org.quartz.simpl.SystemPropertyInstanceIdGenerator";
		}

		userTXLocation = cfg.getStringProperty(PROP_SCHED_USER_TX_URL,//用户事务，默认为null
				userTXLocation);
		if (userTXLocation != null && userTXLocation.trim().length() == 0) {
			userTXLocation = null;
		}

		classLoadHelperClass = cfg.getStringProperty(//内部的类、资源加载服务实现类，默认为"org.quartz.simpl.CascadingClassLoadHelper"
				PROP_SCHED_CLASS_LOAD_HELPER_CLASS,
				"org.quartz.simpl.CascadingClassLoadHelper");
		wrapJobInTx = cfg.getBooleanProperty(PROP_SCHED_WRAP_JOB_IN_USER_TX,//用事务包装job，默认为false
				wrapJobInTx);

		jobFactoryClass = cfg.getStringProperty(//job工厂类，默认为null
				PROP_SCHED_JOB_FACTORY_CLASS, null);

		idleWaitTime = cfg.getLongProperty(PROP_SCHED_IDLE_WAIT_TIME,//空闲等待实现，默认为-1
				idleWaitTime);
		if(idleWaitTime > -1 && idleWaitTime < 1000) {//如果小于1000ms，则异常
			throw new SchedulerException("org.quartz.scheduler.idleWaitTime of less than 1000ms is not legal.");
		}
		//数据库连接失败重试间隔时间，默认为15000L，即15second
		dbFailureRetry = cfg.getLongProperty(PROP_SCHED_DB_FAILURE_RETRY_INTERVAL, dbFailureRetry);
		if (dbFailureRetry < 0) {//小于0则异常
			throw new SchedulerException(PROP_SCHED_DB_FAILURE_RETRY_INTERVAL + " of less than 0 ms is not legal.");
		}

		boolean makeSchedulerThreadDaemon =
				cfg.getBooleanProperty(PROP_SCHED_MAKE_SCHEDULER_THREAD_DAEMON);//使得sched线程作为守护线程的标志，默认为false

		boolean threadsInheritInitalizersClassLoader =//线程继承上下文初始化线程的类加载器，默认为false;
				cfg.getBooleanProperty(PROP_SCHED_SCHEDULER_THREADS_INHERIT_CONTEXT_CLASS_LOADER_OF_INITIALIZING_THREAD);

		boolean skipUpdateCheck = cfg.getBooleanProperty(PROP_SCHED_SKIP_UPDATE_CHECK, true);//忽略更新检查，默认为true
		long batchTimeWindow = cfg.getLongProperty(PROP_SCHED_BATCH_TIME_WINDOW, 0L);//批处理时间，默认为0L（算法的主要思路是通过采用一个冲突任务替换一个已规划的任务，并将替换任务后移至下一时间窗口或在同一时间窗口内部后移）
		int maxBatchSize = cfg.getIntProperty(PROP_SCHED_MAX_BATCH_SIZE, 1);//最大批处理任务数，默认为1

		//sched关闭时中断任务执行标志，默认为false
		boolean interruptJobsOnShutdown = cfg.getBooleanProperty(PROP_SCHED_INTERRUPT_JOBS_ON_SHUTDOWN, false);
		//sched关闭时，等待任务中断标志，默认为false.
		boolean interruptJobsOnShutdownWithWait = cfg.getBooleanProperty(PROP_SCHED_INTERRUPT_JOBS_ON_SHUTDOWN_WITH_WAIT, false);

		boolean jmxExport = cfg.getBooleanProperty(PROP_SCHED_JMX_EXPORT);//JMX导出标志，默认为false
		String jmxObjectName = cfg.getStringProperty(PROP_SCHED_JMX_OBJECT_NAME);//JMX对象名，默认为Null

		boolean jmxProxy = cfg.getBooleanProperty(PROP_SCHED_JMX_PROXY);//JMX代理，默认为false
		String jmxProxyClass = cfg.getStringProperty(PROP_SCHED_JMX_PROXY_CLASS);//JMX代理类，默认为null

		boolean rmiExport = cfg.getBooleanProperty(PROP_SCHED_RMI_EXPORT, false);//RMI导出标志，默认为false
		boolean rmiProxy = cfg.getBooleanProperty(PROP_SCHED_RMI_PROXY, false);//RMI代理标志，默认为false
		String rmiHost = cfg.getStringProperty(PROP_SCHED_RMI_HOST, "localhost");//RMI的host，默认为localhost
		int rmiPort = cfg.getIntProperty(PROP_SCHED_RMI_PORT, 1099);//RMI监听端口，默认为1099
		int rmiServerPort = cfg.getIntProperty(PROP_SCHED_RMI_SERVER_PORT, -1);//RMI服务器端口，默认为-1
		/*注：绑定服务的默认端口为1099，如果使用了这个端口，则可以直接使用　start rmiregistry而不需要跟端口
		如果这种注册远程对象的方法不起作用．
		还有一种方法就是在绑定服务之前使用LocateRegistry.createRegistry(1099)　来注册远程对象．*/
		String rmiCreateRegistry = cfg.getStringProperty(//启用RMI注册远程对象辅助标志，默认为“never”
				PROP_SCHED_RMI_CREATE_REGISTRY,
				QuartzSchedulerResources.CREATE_REGISTRY_NEVER);
		String rmiBindName = cfg.getStringProperty(PROP_SCHED_RMI_BIND_NAME);//RMI绑定名称，默认为null

		if (jmxProxy && rmiProxy) {//如果JMX和RMI代理标志都为true,则异常------------只能使用其中一种方式
			throw new SchedulerConfigException("Cannot proxy both RMI and JMX.");
		}
		//管理REST服务使能标志，默认false
		boolean managementRESTServiceEnabled = cfg.getBooleanProperty(MANAGEMENT_REST_SERVICE_ENABLED, false);
		//管理REST服务的host和port,默认为"0.0.0.0:9889"
		String managementRESTServiceHostAndPort = cfg.getStringProperty(MANAGEMENT_REST_SERVICE_HOST_PORT, "0.0.0.0:9889");

		// If Proxying to remote scheduler, short-circuit here...
		// 如果是rmi代理
		if (rmiProxy) {

			if (autoId) {//如果自动生成ID标志为true则sched实例ID修改为“NON_CLUSTERED”
				schedInstId = DEFAULT_INSTANCE_ID;
			}
			//如果RMI绑定名称为null，则使用sched名称和sched实例名生成唯一的uid
			String uid = (rmiBindName == null) ? QuartzSchedulerResources.getUniqueIdentifier(schedName, schedInstId) : rmiBindName;
			//创建一个RemoteScheduler
			RemoteScheduler remoteScheduler = new RemoteScheduler(uid, rmiHost, rmiPort);
			//注册到sched资源管理中
			schedRep.bind(remoteScheduler);
			return remoteScheduler;
		}


		// （创建一个类加载帮助器并初始化）Create class load helper
		ClassLoadHelper loadHelper = null;
		try {
			loadHelper = (ClassLoadHelper) loadClass(classLoadHelperClass)
					.newInstance();
		} catch (Exception e) {
			throw new SchedulerConfigException(
					"Unable to instantiate class load helper class: "
							+ e.getMessage(), e);
		}
		loadHelper.initialize();

		// If Proxying to remote JMX scheduler, short-circuit here...
		if (jmxProxy) { //如果代理了一个远程JMX-scheduler，初始化后返回
			if (autoId) {
				schedInstId = DEFAULT_INSTANCE_ID;//如果自动生成ID标志为true则sched实例ID修改为“NON_CLUSTERED”
			}

			if (jmxProxyClass == null) {//代理类为null，则异常
				throw new SchedulerConfigException("No JMX Proxy Scheduler class provided");
			}

			RemoteMBeanScheduler jmxScheduler = null;//根据JMX代理类生成远程scheduler的MBean
			try {
				jmxScheduler = (RemoteMBeanScheduler)loadHelper.loadClass(jmxProxyClass)
						.newInstance();
			} catch (Exception e) {
				throw new SchedulerConfigException(
						"Unable to instantiate RemoteMBeanScheduler class.", e);
			}

			if (jmxObjectName == null) {//如果JMX对象名为空，则通过sched名称和sched实例ID生成
				jmxObjectName = QuartzSchedulerResources.generateJMXObjectName(schedName, schedInstId);
			}

			jmxScheduler.setSchedulerObjectName(jmxObjectName);//设置JMX对象名属性到MBean中

			tProps = cfg.getPropertyGroup(PROP_SCHED_JMX_PROXY, true);//读取配置文件，根据前缀初始化MBean的相关属性
			try {
				setBeanProps(jmxScheduler, tProps);
			} catch (Exception e) {
				initException = new SchedulerException("RemoteMBeanScheduler class '"
						+ jmxProxyClass + "' props could not be configured.", e);
				throw initException;
			}

			jmxScheduler.initialize();//初始化
			schedRep.bind(jmxScheduler);//注册到sched资源管理中
			return jmxScheduler;
		}


		JobFactory jobFactory = null;
		if(jobFactoryClass != null) {//实例化jobFactory类
			try {
				jobFactory = (JobFactory) loadHelper.loadClass(jobFactoryClass)
						.newInstance();
			} catch (Exception e) {
				throw new SchedulerConfigException(
						"Unable to instantiate JobFactory class: "
								+ e.getMessage(), e);
			}

			tProps = cfg.getPropertyGroup(PROP_SCHED_JOB_FACTORY_PREFIX, true);
			try {
				setBeanProps(jobFactory, tProps);//根据配置前缀设置相关的属性值
			} catch (Exception e) {
				initException = new SchedulerException("JobFactory class '"
						+ jobFactoryClass + "' props could not be configured.", e);
				throw initException;
			}
		}
		
		InstanceIdGenerator instanceIdGenerator = null;
		if(instanceIdGeneratorClass != null) {//实例化sched实例ID生成类
			try {
				instanceIdGenerator = (InstanceIdGenerator) loadHelper.loadClass(instanceIdGeneratorClass)
						.newInstance();
			} catch (Exception e) {
				throw new SchedulerConfigException(
						"Unable to instantiate InstanceIdGenerator class: "
								+ e.getMessage(), e);
			}

			tProps = cfg.getPropertyGroup(PROP_SCHED_INSTANCE_ID_GENERATOR_PREFIX, true);
			try {
				setBeanProps(instanceIdGenerator, tProps);//根据配置前缀设置相关的属性值
			} catch (Exception e) {
				initException = new SchedulerException("InstanceIdGenerator class '"
						+ instanceIdGeneratorClass + "' props could not be configured.", e);
				throw initException;
			}
		}

		// (获取线程池类，默认为SimpleThreadPool)  Get ThreadPool Properties
		String tpClass = cfg.getStringProperty(PROP_THREAD_POOL_CLASS, SimpleThreadPool.class.getName());

		if (tpClass == null) {//异常
			initException = new SchedulerException(
					"ThreadPool class not specified. ");
			throw initException;
		}

		try {
			tp = (ThreadPool) loadHelper.loadClass(tpClass).newInstance();//实例化线程池类
		} catch (Exception e) {
			initException = new SchedulerException("ThreadPool class '"
					+ tpClass + "' could not be instantiated.", e);
			throw initException;
		}
		tProps = cfg.getPropertyGroup(PROP_THREAD_POOL_PREFIX, true);
		try {
			setBeanProps(tp, tProps);//根据配置前缀设置相关的属性值
		} catch (Exception e) {
			initException = new SchedulerException("ThreadPool class '"
					+ tpClass + "' props could not be configured.", e);
			throw initException;
		}

		// （获取job数据持久类，默认为RAMJobStore） Get JobStore Properties
		String jsClass = cfg.getStringProperty(PROP_JOB_STORE_CLASS,RAMJobStore.class.getName());

		if (jsClass == null) {//异常
			initException = new SchedulerException(
					"JobStore class not specified. ");
			throw initException;
		}

		try {
			js = (JobStore) loadHelper.loadClass(jsClass).newInstance();//实例化JobStore
		} catch (Exception e) {
			initException = new SchedulerException("JobStore class '" + jsClass
					+ "' could not be instantiated.", e);
			throw initException;
		}
		//利用反射的方法调用更新js的schedName和schedInstId两个属性
		SchedulerDetailsSetter.setDetails(js, schedName, schedInstId);
		//获取排外的所有属性
		tProps = cfg.getPropertyGroup(PROP_JOB_STORE_PREFIX, true, new String[] {PROP_JOB_STORE_LOCK_HANDLER_PREFIX});
		try {
			setBeanProps(js, tProps);//更新JS的属性
		} catch (Exception e) {
			initException = new SchedulerException("JobStore class '" + jsClass
					+ "' props could not be configured.", e);
			throw initException;
		}

		if (js instanceof JobStoreSupport) {
			// (自定义锁处理器) Install custom lock handler (Semaphore)
			String lockHandlerClass = cfg.getStringProperty(PROP_JOB_STORE_LOCK_HANDLER_CLASS);
			if (lockHandlerClass != null) {
				try {
					Semaphore lockHandler = (Semaphore)loadHelper.loadClass(lockHandlerClass).newInstance();//实例

					tProps = cfg.getPropertyGroup(PROP_JOB_STORE_LOCK_HANDLER_PREFIX, true);//获取属性

					// If this lock handler requires the table prefix, add it to its properties.
					if (lockHandler instanceof TablePrefixAware) {//实现了JDBC表前缀定义接口，更新属性
						tProps.setProperty(
								PROP_TABLE_PREFIX, ((JobStoreSupport)js).getTablePrefix());
						tProps.setProperty(
								PROP_SCHED_NAME, schedName);
					}

					try {
						setBeanProps(lockHandler, tProps);//设置属性到自定义锁处理器对象中
					} catch (Exception e) {
						initException = new SchedulerException("JobStore LockHandler class '" + lockHandlerClass
								+ "' props could not be configured.", e);
						throw initException;
					}

					((JobStoreSupport)js).setLockHandler(lockHandler);//设置JS的锁处理器
					getLog().info("Using custom data access locking (synchronization): " + lockHandlerClass);
				} catch (Exception e) {
					initException = new SchedulerException("JobStore LockHandler class '" + lockHandlerClass
							+ "' could not be instantiated.", e);
					throw initException;
				}
			}
		}

		//（装配数据源）Set up any DataSources
		String[] dsNames = cfg.getPropertyGroups(PROP_DATASOURCE_PREFIX);//读取配置文件中数据源名称列表
		for (int i = 0; i < dsNames.length; i++) {//循环读取、配置、加载
			PropertiesParser pp = new PropertiesParser(cfg.getPropertyGroup(
					PROP_DATASOURCE_PREFIX + "." + dsNames[i], true));//获取指定数据源名称的属性值列表

			String cpClass = pp.getStringProperty(PROP_CONNECTION_PROVIDER_CLASS, null);//获取连接器提供类

			//（自定义连接器）custom connectionProvider...
			if(cpClass != null) {
				ConnectionProvider cp = null;
				try {
					cp = (ConnectionProvider) loadHelper.loadClass(cpClass).newInstance();//实例化
				} catch (Exception e) {
					initException = new SchedulerException("ConnectionProvider class '" + cpClass
							+ "' could not be instantiated.", e);
					throw initException;
				}

				try {
					//（移除类名，不是试图设置）remove the class name, so it isn't attempted to be set
					pp.getUnderlyingProperties().remove(PROP_CONNECTION_PROVIDER_CLASS);

					if (cp instanceof PoolingConnectionProvider) {//是连接池的实例，PoolingConnectionProvider提供了基于C3PO的默认实现
						//填充私有的扩展属性，移除默认属性
						populateProviderWithExtraProps((PoolingConnectionProvider)cp, pp.getUnderlyingProperties());
					} else {
						setBeanProps(cp, pp.getUnderlyingProperties());//根据配置文件属性值set属性
					}
					cp.initialize();//初始化
				} catch (Exception e) {
					initException = new SchedulerException("ConnectionProvider class '" + cpClass
							+ "' props could not be configured.", e);
					throw initException;
				}

				dbMgr = DBConnectionManager.getInstance();
				dbMgr.addConnectionProvider(dsNames[i], cp);//添加到连接器管理中
			} else {//未提供连接器
				String dsJndi = pp.getStringProperty(PROP_DATASOURCE_JNDI_URL, null);

				if (dsJndi != null) {//提供了JDNI地址
					boolean dsAlwaysLookup = pp.getBooleanProperty(
							PROP_DATASOURCE_JNDI_ALWAYS_LOOKUP);
					String dsJndiInitial = pp.getStringProperty(
							PROP_DATASOURCE_JNDI_INITIAL);
					String dsJndiProvider = pp.getStringProperty(
							PROP_DATASOURCE_JNDI_PROVDER);
					String dsJndiPrincipal = pp.getStringProperty(
							PROP_DATASOURCE_JNDI_PRINCIPAL);
					String dsJndiCredentials = pp.getStringProperty(
							PROP_DATASOURCE_JNDI_CREDENTIALS);
					Properties props = null;
					if (null != dsJndiInitial || null != dsJndiProvider
							|| null != dsJndiPrincipal || null != dsJndiCredentials) {
						props = new Properties();
						if (dsJndiInitial != null) {
							props.put(PROP_DATASOURCE_JNDI_INITIAL,
									dsJndiInitial);
						}
						if (dsJndiProvider != null) {
							props.put(PROP_DATASOURCE_JNDI_PROVDER,
									dsJndiProvider);
						}
						if (dsJndiPrincipal != null) {
							props.put(PROP_DATASOURCE_JNDI_PRINCIPAL,
									dsJndiPrincipal);
						}
						if (dsJndiCredentials != null) {
							props.put(PROP_DATASOURCE_JNDI_CREDENTIALS,
									dsJndiCredentials);
						}
					}
					JNDIConnectionProvider cp = new JNDIConnectionProvider(dsJndi,
							props, dsAlwaysLookup);//创建JDNI连接器
					dbMgr = DBConnectionManager.getInstance();
					dbMgr.addConnectionProvider(dsNames[i], cp);//添加到连接器管理中
				} else {//创建基于C3PO实现的PoolingConnectionProvider连接器
					String dsDriver = pp.getStringProperty(PoolingConnectionProvider.DB_DRIVER);
					String dsURL = pp.getStringProperty(PoolingConnectionProvider.DB_URL);

					if (dsDriver == null) {
						initException = new SchedulerException(
								"Driver not specified for DataSource: "
										+ dsNames[i]);
						throw initException;
					}
					if (dsURL == null) {
						initException = new SchedulerException(
								"DB URL not specified for DataSource: "
										+ dsNames[i]);
						throw initException;
					}
					try {//创建连接器
						PoolingConnectionProvider cp = new PoolingConnectionProvider(pp.getUnderlyingProperties());
						dbMgr = DBConnectionManager.getInstance();
						dbMgr.addConnectionProvider(dsNames[i], cp);//添加到连接器管理中

						// Populate the underlying C3P0 data source pool properties
						populateProviderWithExtraProps(cp, pp.getUnderlyingProperties());//填充私有的扩展属性，移除默认属性
					} catch (Exception sqle) {
						initException = new SchedulerException(
								"Could not initialize DataSource: " + dsNames[i],
								sqle);
						throw initException;
					}
				}

			}

		}

		// （装配任何Sched插件） Set up any SchedulerPlugins
		String[] pluginNames = cfg.getPropertyGroups(PROP_PLUGIN_PREFIX);//读取插件名称列表
		SchedulerPlugin[] plugins = new SchedulerPlugin[pluginNames.length];//创建插件数组
		for (int i = 0; i < pluginNames.length; i++) {//循环初始化
			Properties pp = cfg.getPropertyGroup(PROP_PLUGIN_PREFIX + "."
					+ pluginNames[i], true);//读取插件属性组

			String plugInClass = pp.getProperty(PROP_PLUGIN_CLASS, null);//读取插件实现类属性

			if (plugInClass == null) {
				initException = new SchedulerException(
						"SchedulerPlugin class not specified for plugin '"
								+ pluginNames[i] + "'");
				throw initException;
			}
			SchedulerPlugin plugin = null;
			try {
				plugin = (SchedulerPlugin)
						loadHelper.loadClass(plugInClass).newInstance();//实例化
			} catch (Exception e) {
				initException = new SchedulerException(
						"SchedulerPlugin class '" + plugInClass
						+ "' could not be instantiated.", e);
				throw initException;
			}
			try {
				setBeanProps(plugin, pp);//设置插件相关属性值
			} catch (Exception e) {
				initException = new SchedulerException(
						"JobStore SchedulerPlugin '" + plugInClass
						+ "' props could not be configured.", e);
				throw initException;
			}

			plugins[i] = plugin;//放入插件数组中
		}

		// （装配Job监听器）Set up any JobListeners
		Class<?>[] strArg = new Class[] { String.class };//参数列表
		String[] jobListenerNames = cfg.getPropertyGroups(PROP_JOB_LISTENER_PREFIX);//job监听器名称列表
		JobListener[] jobListeners = new JobListener[jobListenerNames.length];//job监听器数组
		for (int i = 0; i < jobListenerNames.length; i++) {
			Properties lp = cfg.getPropertyGroup(PROP_JOB_LISTENER_PREFIX + "."
					+ jobListenerNames[i], true);//读取相关监听器的属性列表

			String listenerClass = lp.getProperty(PROP_LISTENER_CLASS, null);//读取job监听器实现类

			if (listenerClass == null) {
				initException = new SchedulerException(
						"JobListener class not specified for listener '"
								+ jobListenerNames[i] + "'");
				throw initException;
			}
			JobListener listener = null;
			try {
				listener = (JobListener)
						loadHelper.loadClass(listenerClass).newInstance();//实例化
			} catch (Exception e) {
				initException = new SchedulerException(
						"JobListener class '" + listenerClass
						+ "' could not be instantiated.", e);
				throw initException;
			}
			try {
				Method nameSetter = null;
				try { 
					nameSetter = listener.getClass().getMethod("setName", strArg);//获取方法
				}
				catch(NoSuchMethodException ignore) { 
					/* do nothing */ 
				}
				if(nameSetter != null) {
					nameSetter.invoke(listener, new Object[] {jobListenerNames[i] } );//反射调用设置名称的方法
				}
				setBeanProps(listener, lp);//设置相关属性
			} catch (Exception e) {
				initException = new SchedulerException(
						"JobListener '" + listenerClass
						+ "' props could not be configured.", e);
				throw initException;
			}
			jobListeners[i] = listener;//放入job监听器数组中
		}

		// （装配触发器监听器）Set up any TriggerListeners
		String[] triggerListenerNames = cfg.getPropertyGroups(PROP_TRIGGER_LISTENER_PREFIX);//读取触发器监听器名称列表
		TriggerListener[] triggerListeners = new TriggerListener[triggerListenerNames.length];//创建相关数组
		for (int i = 0; i < triggerListenerNames.length; i++) {
			Properties lp = cfg.getPropertyGroup(PROP_TRIGGER_LISTENER_PREFIX + "."
					+ triggerListenerNames[i], true);//读取指定名称的触发器监听器属性列表

			String listenerClass = lp.getProperty(PROP_LISTENER_CLASS, null);//读取类名

			if (listenerClass == null) {
				initException = new SchedulerException(
						"TriggerListener class not specified for listener '"
								+ triggerListenerNames[i] + "'");
				throw initException;
			}
			TriggerListener listener = null;
			try {
				listener = (TriggerListener)
						loadHelper.loadClass(listenerClass).newInstance();//实例化
			} catch (Exception e) {
				initException = new SchedulerException(
						"TriggerListener class '" + listenerClass
						+ "' could not be instantiated.", e);
				throw initException;
			}
			try {
				Method nameSetter = null;
				try { 
					nameSetter = listener.getClass().getMethod("setName", strArg);//获取设置名称方法
				}
				catch(NoSuchMethodException ignore) { /* do nothing */ }
				if(nameSetter != null) {
					nameSetter.invoke(listener, new Object[] {triggerListenerNames[i] } );//调用设置名称的方法
				}
				setBeanProps(listener, lp);//设置相关的属性
			} catch (Exception e) {
				initException = new SchedulerException(
						"TriggerListener '" + listenerClass
						+ "' props could not be configured.", e);
				throw initException;
			}
			triggerListeners[i] = listener;//放入数组中
		}

		boolean tpInited = false;//线程池初始化完成标志，默认为false
		boolean qsInited = false;//quartz调度器初始化完成标志，默认为false


		// (获取业务线程池属性) Get ThreadExecutor Properties
		String threadExecutorClass = cfg.getStringProperty(PROP_THREAD_EXECUTOR_CLASS);//读取线程执行者的配置类
		if (threadExecutorClass != null) {
			tProps = cfg.getPropertyGroup(PROP_THREAD_EXECUTOR, true);//读取属性
			try {
				threadExecutor = (ThreadExecutor) loadHelper.loadClass(threadExecutorClass).newInstance();//实例化
				log.info("Using custom implementation for ThreadExecutor: " + threadExecutorClass);

				setBeanProps(threadExecutor, tProps);//设置相关属性
			} catch (Exception e) {
				initException = new SchedulerException(
						"ThreadExecutor class '" + threadExecutorClass + "' could not be instantiated.", e);
				throw initException;
			}
		} else {
			log.info("Using default implementation for ThreadExecutor");
			threadExecutor = new DefaultThreadExecutor();//作为默认实现
		}



		// （触发一切）Fire everything up
		try {

			JobRunShellFactory jrsf = null; // （创建shell运行的工厂类） Create correct run-shell factory...

			if (userTXLocation != null) {
				UserTransactionHelper.setUserTxLocation(userTXLocation);//设置用户事务
			}
			//创建工厂类
			if (wrapJobInTx) {//使用事务包装job
				jrsf = new JTAJobRunShellFactory();
			} else {
				jrsf = new JTAAnnotationAwareJobRunShellFactory();
			}
			
			if (autoId) {//ID自动生成
				try {
					schedInstId = DEFAULT_INSTANCE_ID;//初始化为NON_CLUSTERED
					if (js.isClustered()) {
						schedInstId = instanceIdGenerator.generateInstanceId();//集群则生成一个ID
					}
				} catch (Exception e) {
					getLog().error("Couldn't generate instance Id!", e);
					throw new IllegalStateException("Cannot run without an instance id.");
				}
			}

			if (js.getClass().getName().startsWith("org.terracotta.quartz")) {//持久化类名以terracotta开头
				try {
					String uuid = (String) js.getClass().getMethod("getUUID").invoke(js);//读取JobStore的ID
					if(schedInstId.equals(DEFAULT_INSTANCE_ID)) {//非集群
						schedInstId = "TERRACOTTA_CLUSTERED,node=" + uuid;//设置ID
						if (jmxObjectName == null) {//JMX对象名为null，则通过schedName和SchedInstID生成
							jmxObjectName = QuartzSchedulerResources.generateJMXObjectName(schedName, schedInstId);
						}
					} else if(jmxObjectName == null) {//集群下，JMX对象名称为null则通过schedName、SchedInstID+ID来生成
						jmxObjectName = QuartzSchedulerResources.generateJMXObjectName(schedName, schedInstId + ",node=" + uuid);
					}
				} catch(Exception e) {
					throw new RuntimeException("Problem obtaining node id from TerracottaJobStore.", e);
				}

				if(null == cfg.getStringProperty(PROP_SCHED_JMX_EXPORT)) {//JMX导出属性不存在，设置JMX导出标志为true
					jmxExport = true;
				}
			}

			if (js instanceof JobStoreSupport) {//作为JobStoreSupport的实例
				JobStoreSupport jjs = (JobStoreSupport)js;
				jjs.setDbRetryInterval(dbFailureRetry);//设置DB失败重试时间间隔
				if(threadsInheritInitalizersClassLoader)//设置线程继承初始化类加载器的上下文
					jjs.setThreadsInheritInitializersClassLoadContext(threadsInheritInitalizersClassLoader);

				jjs.setThreadExecutor(threadExecutor);//设置业务线程池
			}

			QuartzSchedulerResources rsrcs = new QuartzSchedulerResources();//quartz调度器资源实例
			rsrcs.setName(schedName);
			rsrcs.setThreadName(threadName);
			rsrcs.setInstanceId(schedInstId);
			rsrcs.setJobRunShellFactory(jrsf);
			rsrcs.setMakeSchedulerThreadDaemon(makeSchedulerThreadDaemon);
			rsrcs.setThreadsInheritInitializersClassLoadContext(threadsInheritInitalizersClassLoader);
			rsrcs.setRunUpdateCheck(!skipUpdateCheck);
			rsrcs.setBatchTimeWindow(batchTimeWindow);
			rsrcs.setMaxBatchSize(maxBatchSize);
			rsrcs.setInterruptJobsOnShutdown(interruptJobsOnShutdown);
			rsrcs.setInterruptJobsOnShutdownWithWait(interruptJobsOnShutdownWithWait);
			rsrcs.setJMXExport(jmxExport);
			rsrcs.setJMXObjectName(jmxObjectName);

			if (managementRESTServiceEnabled) {//需要管理REST服务
				ManagementRESTServiceConfiguration managementRESTServiceConfiguration = new ManagementRESTServiceConfiguration();
				managementRESTServiceConfiguration.setBind(managementRESTServiceHostAndPort);
				managementRESTServiceConfiguration.setEnabled(managementRESTServiceEnabled);
				rsrcs.setManagementRESTServiceConfiguration(managementRESTServiceConfiguration);
			}

			if (rmiExport) {//rmi设置
				rsrcs.setRMIRegistryHost(rmiHost);
				rsrcs.setRMIRegistryPort(rmiPort);
				rsrcs.setRMIServerPort(rmiServerPort);
				rsrcs.setRMICreateRegistryStrategy(rmiCreateRegistry);
				rsrcs.setRMIBindName(rmiBindName);
			}

			SchedulerDetailsSetter.setDetails(tp, schedName, schedInstId);//设置线程池中的schedName和schedInstId

			rsrcs.setThreadExecutor(threadExecutor);//设置业务线程池
			threadExecutor.initialize();//初始化业务线程池---------------------------------------------*******重点*******

			rsrcs.setThreadPool(tp);//设置线程池
			if(tp instanceof SimpleThreadPool) {
				if(threadsInheritInitalizersClassLoader)//设置线程继承初始化类加载器的上下文
					((SimpleThreadPool)tp).setThreadsInheritContextClassLoaderOfInitializingThread(threadsInheritInitalizersClassLoader);
			}
			tp.initialize();//初始化线程池---------------------------------------------*******重点*******
			tpInited = true;

			rsrcs.setJobStore(js);//设置jobStore

			// add plugins
			for (int i = 0; i < plugins.length; i++) {//添加插件到资源管理中
				rsrcs.addSchedulerPlugin(plugins[i]);
			}
			
			qs = new QuartzScheduler(rsrcs, idleWaitTime, dbFailureRetry);//创建quart调度器实例----------------------*******重点*******
			qsInited = true;

			// Create Scheduler ref...
			Scheduler scheduler = instantiate(rsrcs, qs);//创建一个qs引用代理----------------------------------

			// set job factory if specified
			if(jobFactory != null) {
				qs.setJobFactory(jobFactory);//设置job工厂
			}

			// Initialize plugins now that we have a Scheduler instance.
			for (int i = 0; i < plugins.length; i++) {
				plugins[i].initialize(pluginNames[i], scheduler, loadHelper);//就当前scheduler实例化插件
			}

			// add listeners
			for (int i = 0; i < jobListeners.length; i++) {//增加job监听器
				qs.getListenerManager().addJobListener(jobListeners[i], EverythingMatcher.allJobs());
			}
			for (int i = 0; i < triggerListeners.length; i++) {//增加触发器
				qs.getListenerManager().addTriggerListener(triggerListeners[i], EverythingMatcher.allTriggers());
			}

			// （设置scheduler的上下文属性数据）set scheduler context data...
			Properties schedCtxtProps = cfg.getPropertyGroup(PROP_SCHED_CONTEXT_PREFIX, true);//sched上下文前缀属性组，去除前缀
			for(Object key: schedCtxtProps.keySet()) {
				String val = schedCtxtProps.getProperty((String) key);    
				scheduler.getContext().put((String)key, val);
			}

			// （触发job持久化和runshell工厂类）fire up job store, and runshell factory
			js.setInstanceId(schedInstId);
			js.setInstanceName(schedName);
			js.setThreadPoolSize(tp.getPoolSize());
			js.initialize(loadHelper, qs.getSchedulerSignaler());//类加载帮助器、信号实例

			jrsf.initialize(scheduler);//初始化runshell工厂

			qs.initialize();//初始化QuartzScheduler---------------------------------------------*******重点*******

			getLog().info("Quartz scheduler '" + scheduler.getSchedulerName() + "' initialized from " + propSrc);
			getLog().info("Quartz scheduler version: " + qs.getVersion());

			// prevents the repository from being garbage collected
			qs.addNoGCObject(schedRep);//阻止垃圾回收
			// prevents the db manager from being garbage collected
			if (dbMgr != null) {
				qs.addNoGCObject(dbMgr);
			}

			schedRep.bind(scheduler);//绑定
			return scheduler;
		}
		catch(SchedulerException e) {
			shutdownFromInstantiateException(tp, qs, tpInited, qsInited);
			throw e;
		}
		catch(RuntimeException re) {
			shutdownFromInstantiateException(tp, qs, tpInited, qsInited);
			throw re;
		}
		catch(Error re) {
			shutdownFromInstantiateException(tp, qs, tpInited, qsInited);
			throw re;
		}
	}

	private void populateProviderWithExtraProps(PoolingConnectionProvider cp, Properties props) throws Exception {
		Properties copyProps = new Properties();
		copyProps.putAll(props);
		// Remove all the default properties first (they don't always match to setter name, and they are already
		// been set!)
		copyProps.remove(PoolingConnectionProvider.DB_DRIVER);
		copyProps.remove(PoolingConnectionProvider.DB_URL);
		copyProps.remove(PoolingConnectionProvider.DB_USER);
		copyProps.remove(PoolingConnectionProvider.DB_PASSWORD);
		copyProps.remove(PoolingConnectionProvider.DB_IDLE_VALIDATION_SECONDS);
		copyProps.remove(PoolingConnectionProvider.DB_MAX_CONNECTIONS);
		copyProps.remove(PoolingConnectionProvider.DB_MAX_CACHED_STATEMENTS_PER_CONNECTION);
		copyProps.remove(PoolingConnectionProvider.DB_VALIDATE_ON_CHECKOUT);
		copyProps.remove(PoolingConnectionProvider.DB_VALIDATION_QUERY);
		setBeanProps(cp.getDataSource(), copyProps);
	}

	private void shutdownFromInstantiateException(ThreadPool tp, QuartzScheduler qs, boolean tpInited, boolean qsInited) {
		try {
			if(qsInited)
				qs.shutdown(false);
			else if(tpInited)
				tp.shutdown(false);
		} catch (Exception e) {
			getLog().error("Got another exception while shutting down after instantiation exception", e);
		}
	}
	//实例化
	protected Scheduler instantiate(QuartzSchedulerResources rsrcs, QuartzScheduler qs) {
		Scheduler scheduler = new StdScheduler(qs);
		return scheduler;
	}

	//根据属性调用对象的相应set方法更新该对象的属性
	private void setBeanProps(Object obj, Properties props)
			throws NoSuchMethodException, IllegalAccessException,
			java.lang.reflect.InvocationTargetException,
			IntrospectionException, SchedulerConfigException {
		props.remove("class");

		BeanInfo bi = Introspector.getBeanInfo(obj.getClass());
		PropertyDescriptor[] propDescs = bi.getPropertyDescriptors();
		PropertiesParser pp = new PropertiesParser(props);

		java.util.Enumeration<Object> keys = props.keys();
		while (keys.hasMoreElements()) {
			String name = (String) keys.nextElement();
			String c = name.substring(0, 1).toUpperCase(Locale.US);
			String methName = "set" + c + name.substring(1);

			java.lang.reflect.Method setMeth = getSetMethod(methName, propDescs);

			try {
				if (setMeth == null) {
					throw new NoSuchMethodException(
							"No setter for property '" + name + "'");
				}

				Class<?>[] params = setMeth.getParameterTypes();
				if (params.length != 1) {
					throw new NoSuchMethodException(
							"No 1-argument setter for property '" + name + "'");
				}

				// does the property value reference another property's value? If so, swap to look at its value
				PropertiesParser refProps = pp;
				String refName = pp.getStringProperty(name);
				if(refName != null && refName.startsWith("$@")) {
					refName =  refName.substring(2);
					refProps = cfg;
				}
				else
					refName = name;

				if (params[0].equals(int.class)) {
					setMeth.invoke(obj, new Object[]{Integer.valueOf(refProps.getIntProperty(refName))});
				} else if (params[0].equals(long.class)) {
					setMeth.invoke(obj, new Object[]{Long.valueOf(refProps.getLongProperty(refName))});
				} else if (params[0].equals(float.class)) {
					setMeth.invoke(obj, new Object[]{Float.valueOf(refProps.getFloatProperty(refName))});
				} else if (params[0].equals(double.class)) {
					setMeth.invoke(obj, new Object[]{Double.valueOf(refProps.getDoubleProperty(refName))});
				} else if (params[0].equals(boolean.class)) {
					setMeth.invoke(obj, new Object[]{Boolean.valueOf(refProps.getBooleanProperty(refName))});
				} else if (params[0].equals(String.class)) {
					setMeth.invoke(obj, new Object[]{refProps.getStringProperty(refName)});
				} else {
					throw new NoSuchMethodException(
							"No primitive-type setter for property '" + name
							+ "'");
				}
			} catch (NumberFormatException nfe) {
				throw new SchedulerConfigException("Could not parse property '"
						+ name + "' into correct data type: " + nfe.toString());
			}
		}
	}

	private java.lang.reflect.Method getSetMethod(String name,
			PropertyDescriptor[] props) {
		for (int i = 0; i < props.length; i++) {
			java.lang.reflect.Method wMeth = props[i].getWriteMethod();

			if (wMeth != null && wMeth.getName().equals(name)) {
				return wMeth;
			}
		}

		return null;
	}

	private Class<?> loadClass(String className) throws ClassNotFoundException, SchedulerConfigException {

		try {
			ClassLoader cl = findClassloader();
			if(cl != null)
				return cl.loadClass(className);
			throw new SchedulerConfigException("Unable to find a class loader on the current thread or class.");
		} catch (ClassNotFoundException e) {
			if(getClass().getClassLoader() != null)
				return getClass().getClassLoader().loadClass(className);
			throw e;
		}
	}

	private ClassLoader findClassloader() {
		// work-around set context loader for windows-service started jvms (QUARTZ-748)
		if(Thread.currentThread().getContextClassLoader() == null && getClass().getClassLoader() != null) {
			Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
		}
		return Thread.currentThread().getContextClassLoader();
	}

	private String getSchedulerName() {
		return cfg.getStringProperty(PROP_SCHED_INSTANCE_NAME,
				"QuartzScheduler");
	}

	/**
	 * <p>
	 * Returns a handle to the Scheduler produced by this factory.
	 * </p>
	 *
	 * <p>
	 * If one of the <code>initialize</code> methods has not be previously
	 * called, then the default (no-arg) <code>initialize()</code> method
	 * will be called by this method.
	 * </p>
	 */
	public Scheduler getScheduler() throws SchedulerException {
		if (cfg == null) {
            initialize();//加载配置文件---》覆盖属性值（系统属性覆盖配置文件属性）---》放入配置解析类中，管理的配置解析中
        }

        SchedulerRepository schedRep = SchedulerRepository.getInstance();//管理scheduler（包括名称关联、删除、查询）和保证该管理类的单例

		Scheduler sched = schedRep.lookup(getSchedulerName());

		if (sched != null) {
			if (sched.isShutdown()) {
				schedRep.remove(getSchedulerName());
			} else {
				return sched;
			}
		}

		sched = instantiate();

		return sched;
	}

	/**
	 * <p>
	 * Returns a handle to the default Scheduler, creating it if it does not
	 * yet exist.
	 * </p>
	 *
	 * @see #initialize()
	 */
	public static Scheduler getDefaultScheduler() throws SchedulerException {
		StdSchedulerFactory fact = new StdSchedulerFactory();

		return fact.getScheduler();
	}

	/**
	 * <p>
	 * Returns a handle to the Scheduler with the given name, if it exists (if
	 * it has already been instantiated).
	 * </p>
	 */
	public Scheduler getScheduler(String schedName) throws SchedulerException {
		return SchedulerRepository.getInstance().lookup(schedName);
	}

	/**
	 * <p>
	 * Returns a handle to all known Schedulers (made by any
	 * StdSchedulerFactory instance.).
	 * </p>
	 */
	public Collection<Scheduler> getAllSchedulers() throws SchedulerException {
		return SchedulerRepository.getInstance().lookupAll();
	}
}
