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

package org.quartz.spi;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;

/**
 * <p>
 * Provides an interface for a class to become a "plugin" to Quartz.
 * <--对Quartz提供了作为 “plugin” 的类接口-->
 * </p>
 * 
 * <p>
 * Plugins can do virtually anything you wish, though the most interesting ones
 * will obviously interact with the scheduler in some way - either actively: by
 * invoking actions on the scheduler, or passively: by being a <code>JobListener</code>,
 * <code>TriggerListener</code>, and/or <code>SchedulerListener</code>.
 * <--插件几乎可以做任何你希望做的事，虽然最感兴趣的以某种方式和scheduler交互--要不主动的：scheduler上的调度操作，
 * 或被动的：成为一个JobListener、TriggerListener、或/和SchedulerListener-->
 * </p>
 * 
 * <p>
 * If you use <code>{@link org.quartz.impl.StdSchedulerFactory}</code> to
 * initialize your Scheduler, it can also create and initialize your plugins -
 * look at the configuration docs for details.
 * <--如果你使用了StdSchedulerFactory来初始化你的Scheduler，它也能创建和初始化你的插件，细节请查看配置文档-->
 * </p>
 * 
 * <p>
 * If you need direct access your plugin, you can have it explicitly put a 
 * reference to itself in the <code>Scheduler</code>'s 
 * <code>SchedulerContext</code> as part of its
 * <code>{@link #initialize(String, Scheduler)}</code> method.
 * <--如果你需要直接访问你的插件，你可以明确的放置一个引用到Scheduler的SchedulerContext中，作为initialize(String, Scheduler)方法的一部分-->
 * </p>
 * 
 * @author James House
 */
public interface SchedulerPlugin {

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Interface.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    /**
     * <p>
     * Called during creation of the <code>Scheduler</code> in order to give
     * the <code>SchedulerPlugin</code> a chance to initialize.
     * </p>
     * 
     * <p>
     * At this point, the Scheduler's <code>JobStore</code> is not yet
     * initialized.
     * </p>
     * 
     * <p>
     * If you need direct access your plugin, for example during <code>Job</code>
     * execution, you can have this method explicitly put a 
     * reference to this plugin in the <code>Scheduler</code>'s 
     * <code>SchedulerContext</code>.
     * </p>
     * 
     * @param name
     *          The name by which the plugin is identified.
     * @param scheduler
     *          The scheduler to which the plugin is registered.
     * @param loadHelper
     *            The classLoadHelper the <code>SchedulerFactory</code> is
     *            actually using
     * 
     * @throws org.quartz.SchedulerConfigException
     *           if there is an error initializing.
     */
    void initialize(String name, Scheduler scheduler, ClassLoadHelper loadHelper)
        throws SchedulerException;

    /**
     * <p>
     * Called when the associated <code>Scheduler</code> is started, in order
     * to let the plug-in know it can now make calls into the scheduler if it
     * needs to.
     * </p>
     */
    void start();

    /**
     * <p>
     * Called in order to inform the <code>SchedulerPlugin</code> that it
     * should free up all of it's resources because the scheduler is shutting
     * down.
     * </p>
     */
    void shutdown();

}
