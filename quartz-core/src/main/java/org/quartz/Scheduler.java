
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

package org.quartz;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.quartz.Trigger.TriggerState;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.spi.JobFactory;
import org.quartz.utils.Key;

/**
 * This is the main interface of a Quartz Scheduler.
 * <p>这是quartz调度器的主要接口</p>
 * 
 * <p>
 * A <code>Scheduler</code> maintains a registry of <code>{@link org.quartz.JobDetail}</code>s
 * and <code>{@link Trigger}</code>s. Once registered, the <code>Scheduler</code>
 * is responsible for executing <code>Job</code> s when their associated
 * <code>Trigger</code> s fire (when their scheduled time arrives).</p>
 *<p>一个Scheduler调度器维护了一个JobDetail列表和trigger列表的注册。一旦注册成功，Scheduler调度器将负责执行相关的Job-->
 * </p>
 * 
 * <p>
 * <code>Scheduler</code> instances are produced by a <code>{@link SchedulerFactory}</code>.
 * A scheduler that has already been created/initialized can be found and used
 * through the same factory that produced it. After a <code>Scheduler</code>
 * has been created, it is in "stand-by" mode, and must have its 
 * <code>start()</code> method called before it will fire any <code>Job</code>s.</p>
 * <p>Scheduler调度器通过SchedulerFactory创建，已经被创建/初始化的scheduler调度器通过同样的工厂生产它。
 * 一个Scheduler调度器被创建后处于等待模式，在触发任何Job之前必须调用start()方法
 * </p>
 * 
 * <p>
 * <code>Job</code> s are to be created by the 'client program', by defining
 * a class that implements the <code>{@link org.quartz.Job}</code>
 * interface. <code>{@link JobDetail}</code> objects are then created (also
 * by the client) to define a individual instances of the <code>Job</code>.
 * <code>JobDetail</code> instances can then be registered with the <code>Scheduler</code>
 * via the <code>scheduleJob(JobDetail, Trigger)</code> or <code>addJob(JobDetail, boolean)</code>
 * method.</p>
 * <p>job通过‘客户端程序’创建，通过定义一个Job接口实现类。定义一个独立的job实例时JobDetail被创建（通过客户端）。
 * jobDetail实例通过scheduleJob(JobDetail, Trigger)或addJob(JobDetail, boolean)方法就能
 * 能注册到Scheduler中
 * </p>
 * 
 * <p>
 * <code>Trigger</code> s can then be defined to fire individual <code>Job</code>
 * instances based on given schedules. <code>SimpleTrigger</code> s are most
 * useful for one-time firings, or firing at an exact moment in time, with N
 * repeats with a given delay between them. <code>CronTrigger</code> s allow
 * scheduling based on time of day, day of week, day of month, and month of
 * year.</p>
 * <p>基于给定的schedles调度器，trigger定义了一个job的触发动作。对于给定延迟的N次重复的单次触发或确切时间触发SimpleTrigger是
 * 最有用的。CronTrigger允许了基于天、周的天、月的天、年的月这些策略-->
 * </p>
 * 
 * <p>
 * <code>Job</code> s and <code>Trigger</code> s have a name and group
 * associated with them, which should uniquely identify them within a single
 * <code>{@link Scheduler}</code>. The 'group' feature may be useful for
 * creating logical groupings or categorizations of <code>Jobs</code> s and
 * <code>Triggers</code>s. If you don't have need for assigning a group to a
 * given <code>Jobs</code> of <code>Triggers</code>, then you can use the
 * <code>DEFAULT_GROUP</code> constant defined on this interface.
 * </p><p>job组和trigger组有一个名称和组进行关联，在每个Scheduler中作为唯一的标识。在创建一个逻辑组或分类job和trigger时
 * ‘组’的特征可能是很有用的。如果你没有设置Triggers的job的关联组，你可以使用定义在接口中的常量DEFAULT_GROUP
 * </p>
 * 
 * <p>
 * Stored <code>Job</code> s can also be 'manually' triggered through the use
 * of the <code>triggerJob(String jobName, String jobGroup)</code> function.</p>
 * <p>使用triggerJob(String jobName, String jobGroup)函数也能手动的触发存储的job
 * </p>
 * 
 * <p>
 * Client programs may also be interested in the 'listener' interfaces that are
 * available from Quartz. The <code>{@link JobListener}</code> interface
 * provides notifications of <code>Job</code> executions. The <code>{@link TriggerListener}</code>
 * interface provides notifications of <code>Trigger</code> firings. The
 * <code>{@link SchedulerListener}</code> interface provides notifications of
 * <code>Scheduler</code> events and errors.  Listeners can be associated with
 * local schedulers through the {@link ListenerManager} interface.  </p>
 * <p>客户端程序可能也对来自Quartz的‘listener’接口感兴趣。JobListener接口提供了job执行的通知。
 * TriggerListener接口提供了Trigger触发的通知。SchedulerListener接口提供了Scheduler事件和错误的通知。
 * 通过ListenerManager接口关联Listeners到本地schedulers中</p>
 * 
 * 
 * <p>
 * The setup/configuration of a <code>Scheduler</code> instance is very
 * customizable. Please consult the documentation distributed with Quartz.
 * <p>一个Scheduler实例的设置/配置是可定制的。请查看quartz的分发文档</p>
 * </p>
 * 
 * @see Job
 * @see JobDetail
 * @see JobBuilder
 * @see Trigger
 * @see TriggerBuilder
 * @see JobListener
 * @see TriggerListener
 * @see SchedulerListener
 * 
 * @author James House
 * @author Sharada Jambula
 */
public interface Scheduler {

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Constants.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    /**
     * A (possibly) useful constant that can be used for specifying the group
     * that <code>Job</code> and <code>Trigger</code> instances belong to.
     */
    String DEFAULT_GROUP = Key.DEFAULT_GROUP;

    /**
     * A constant <code>Trigger</code> group name used internally by the
     * scheduler - clients should not use the value of this constant
     * ("RECOVERING_JOBS") for the name of a <code>Trigger</code>'s group.
     *
     * @see org.quartz.JobDetail#requestsRecovery()
     */
    String DEFAULT_RECOVERY_GROUP = "RECOVERING_JOBS";

    /**
     * A constant <code>Trigger</code> group name used internally by the
     * scheduler - clients should not use the value of this constant
     * ("FAILED_OVER_JOBS") for the name of a <code>Trigger</code>'s group.
     *
     * @see org.quartz.JobDetail#requestsRecovery()
     */
    String DEFAULT_FAIL_OVER_GROUP = "FAILED_OVER_JOBS";


    /**
     * A constant <code>JobDataMap</code> key that can be used to retrieve the
     * name of the original <code>Trigger</code> from a recovery trigger's
     * data map in the case of a job recovering after a failed scheduler
     * instance.
     *
     * @see org.quartz.JobDetail#requestsRecovery()
     */
    String FAILED_JOB_ORIGINAL_TRIGGER_NAME =  "QRTZ_FAILED_JOB_ORIG_TRIGGER_NAME";

    /**
     * A constant <code>JobDataMap</code> key that can be used to retrieve the
     * group of the original <code>Trigger</code> from a recovery trigger's
     * data map in the case of a job recovering after a failed scheduler
     * instance.
     *
     * @see org.quartz.JobDetail#requestsRecovery()
     */
    String FAILED_JOB_ORIGINAL_TRIGGER_GROUP =  "QRTZ_FAILED_JOB_ORIG_TRIGGER_GROUP";

    /**
     * A constant <code>JobDataMap</code> key that can be used to retrieve the
     * fire time of the original <code>Trigger</code> from a recovery
     * trigger's data map in the case of a job recovering after a failed scheduler
     * instance.  
     * 
     * <p>Note that this is the time the original firing actually occurred,
     * which may be different from the scheduled fire time - as a trigger doesn't
     * always fire exactly on time.</p>
     *
     * @see org.quartz.JobDetail#requestsRecovery()
     */
    String FAILED_JOB_ORIGINAL_TRIGGER_FIRETIME_IN_MILLISECONDS =  "QRTZ_FAILED_JOB_ORIG_TRIGGER_FIRETIME_IN_MILLISECONDS_AS_STRING";

    /**
     * A constant <code>JobDataMap</code> key that can be used to retrieve the
     * scheduled fire time of the original <code>Trigger</code> from a recovery
     * trigger's data map in the case of a job recovering after a failed scheduler
     * instance.  
     * 
     * <p>Note that this is the time the original firing was scheduled for,
     * which may be different from the actual firing time - as a trigger doesn't
     * always fire exactly on time.</p>
     *
     * @see org.quartz.JobDetail#requestsRecovery()
     */
    String FAILED_JOB_ORIGINAL_TRIGGER_SCHEDULED_FIRETIME_IN_MILLISECONDS =  "QRTZ_FAILED_JOB_ORIG_TRIGGER_SCHEDULED_FIRETIME_IN_MILLISECONDS_AS_STRING";

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Interface.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    /**
     * Returns the name of the <code>Scheduler</code>.
     */
    String getSchedulerName() throws SchedulerException;

    /**
     * Returns the instance Id of the <code>Scheduler</code>.
     */
    String getSchedulerInstanceId() throws SchedulerException;

    /**
     * Returns the <code>SchedulerContext</code> of the <code>Scheduler</code>.
     */
    SchedulerContext getContext() throws SchedulerException;

    ///////////////////////////////////////////////////////////////////////////
    ///
    /// Scheduler State Management Methods
    ///
    ///////////////////////////////////////////////////////////////////////////
    
    /**
     * Starts the <code>Scheduler</code>'s threads that fire <code>{@link Trigger}s</code>.
     * When a scheduler is first created it is in "stand-by" mode, and will not
     * fire triggers.  The scheduler can also be put into stand-by mode by
     * calling the <code>standby()</code> method. 
     * 
     * <p>
     * The misfire/recovery process will be started, if it is the initial call
     * to this method on this scheduler instance.
     * </p>
     * 
     * @throws SchedulerException
     *           if <code>shutdown()</code> has been called, or there is an
     *           error within the <code>Scheduler</code>.
     *
     * @see #startDelayed(int)
     * @see #standby()
     * @see #shutdown()
     */
    void start() throws SchedulerException;

    /**
     * Calls {#start()} after the indicated number of seconds.
     * (This call does not block). This can be useful within applications that
     * have initializers that create the scheduler immediately, before the
     * resources needed by the executing jobs have been fully initialized.
     *
     * @throws SchedulerException
     *           if <code>shutdown()</code> has been called, or there is an
     *           error within the <code>Scheduler</code>.
     *
     * @see #start() 
     * @see #standby()
     * @see #shutdown()
     */
    void startDelayed(int seconds) throws SchedulerException;

    /**
     * Whether the scheduler has been started.  
     * 
     * <p>
     * Note: This only reflects whether <code>{@link #start()}</code> has ever
     * been called on this Scheduler, so it will return <code>true</code> even 
     * if the <code>Scheduler</code> is currently in standby mode or has been 
     * since shutdown.
     * </p>
     * 
     * @see #start()
     * @see #isShutdown()
     * @see #isInStandbyMode()
     */    
    boolean isStarted() throws SchedulerException;
    
    /**
     * Temporarily halts the <code>Scheduler</code>'s firing of <code>{@link Trigger}s</code>.
     * 
     * <p>
     * When <code>start()</code> is called (to bring the scheduler out of 
     * stand-by mode), trigger misfire instructions will NOT be applied
     * during the execution of the <code>start()</code> method - any misfires 
     * will be detected immediately afterward (by the <code>JobStore</code>'s 
     * normal process).
     * </p>
     * 
     * <p>
     * The scheduler is not destroyed, and can be re-started at any time.
     * </p>
     * 
     * @see #start()
     * @see #pauseAll()
     */
    void standby() throws SchedulerException;

    /**
     * Reports whether the <code>Scheduler</code> is in stand-by mode.
     * 
     * @see #standby()
     * @see #start()
     */
    boolean isInStandbyMode() throws SchedulerException;

    /**
     * Halts the <code>Scheduler</code>'s firing of <code>{@link Trigger}s</code>,
     * and cleans up all resources associated with the Scheduler. Equivalent to
     * <code>shutdown(false)</code>.
     * 
     * <p>
     * The scheduler cannot be re-started.
     * </p>
     * 
     * @see #shutdown(boolean)
     */
    void shutdown() throws SchedulerException;

    /**
     * Halts the <code>Scheduler</code>'s firing of <code>{@link Trigger}s</code>,
     * and cleans up all resources associated with the Scheduler.
     * 
     * <p>
     * The scheduler cannot be re-started.
     * </p>
     * 
     * @param waitForJobsToComplete
     *          if <code>true</code> the scheduler will not allow this method
     *          to return until all currently executing jobs have completed.
     * 
     * @see #shutdown
     */
    void shutdown(boolean waitForJobsToComplete)
        throws SchedulerException;

    /**
     * Reports whether the <code>Scheduler</code> has been shutdown.
     */
    boolean isShutdown() throws SchedulerException;

    /**
     * Get a <code>SchedulerMetaData</code> object describing the settings
     * and capabilities of the scheduler instance.
     * <p> 获取描述了调度器实例的设置和能力的一个SchedulerMetaData对象	</p>
     * 
     * <p>
     * Note that the data returned is an 'instantaneous' snap-shot, and that as
     * soon as it's returned, the meta data values may be different.
     * </p>
     * <p> 注意返回的数据快照是'瞬时的'，然后每次返回它数据的值可能不同	</p>
     */
    SchedulerMetaData getMetaData() throws SchedulerException;

    /**
     * Return a list of <code>JobExecutionContext</code> objects that
     * represent all currently executing Jobs in this Scheduler instance.
     * <p> 返回了JobExecutionContext对象列表，代表了当前调度器实例中所有正在执行的job	</p>
     * 
     * <p>
     * This method is not cluster aware.  That is, it will only return Jobs
     * currently executing in this Scheduler instance, not across the entire
     * cluster.
     * </p>
     * <p> 该方法没有集群意识。所以仅返回当前调度器实例中正在执行的job，不会贯穿整个集群	</p>
     * 
     * <p>
     * Note that the list returned is an 'instantaneous' snap-shot, and that as
     * soon as it's returned, the true list of executing jobs may be different.
     * Also please read the doc associated with <code>JobExecutionContext</code>-
     * especially if you're using RMI.
     * </p>
     * 
     * @see JobExecutionContext
     */
    List<JobExecutionContext> getCurrentlyExecutingJobs() throws SchedulerException;

    /**
     * Set the <code>JobFactory</code> that will be responsible for producing 
     * instances of <code>Job</code> classes.
     * <p> 设置将负责产生Job类实例的JobFactory	</p>
     * 
     * <p>
     * JobFactories may be of use to those wishing to have their application
     * produce <code>Job</code> instances via some special mechanism, such as to
     * give the opportunity for dependency injection.
     * </p>
     * <p> JobFactories可以使用那些希望通过特定的机制产生他们应用的job实例，	例如给一个依赖注入的机会</p>
     * 
     * @see org.quartz.spi.JobFactory
     */
    void setJobFactory(JobFactory factory) throws SchedulerException;
    
    
    /**
     * Get a reference to the scheduler's <code>ListenerManager</code>,
     * through which listeners may be registered.
     * <p> 获取一个调度器的监听器管理者，通过它可以注册监听器	</p>
     *  
     * @return the scheduler's <code>ListenerManager</code>
     * @throws SchedulerException if the scheduler is not local
     * @see ListenerManager
     * @see JobListener
     * @see TriggerListener
     * @see SchedulerListener
     */
    ListenerManager getListenerManager()  throws SchedulerException;
    
    ///////////////////////////////////////////////////////////////////////////
    ///
    /// Scheduling-related Methods
    ///
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Add the given <code>{@link org.quartz.JobDetail}</code> to the
     * Scheduler, and associate the given <code>{@link Trigger}</code> with
     * it.
     * <p> 添加指定的JobDetail到调度器中，并为它关联指定的触发器	</p>
     * 
     * <p>
     * If the given Trigger does not reference any <code>Job</code>, then it
     * will be set to reference the Job passed with it into this method.
     * </p>
     * <p> 如果指定的触发器没有被任何job引用，那么它将通过这个方法设置引用给这个Job	</p>
     * 
     * @throws SchedulerException
     *           if the Job or Trigger cannot be added to the Scheduler, or
     *           there is an internal Scheduler error.
     */
    Date scheduleJob(JobDetail jobDetail, Trigger trigger)
        throws SchedulerException;

    /**
     * Schedule the given <code>{@link org.quartz.Trigger}</code> with the
     * <code>Job</code> identified by the <code>Trigger</code>'s settings.
     * <p> 使用被触发器的设置来鉴别的job去调度给定的触发器	</p>
     * 
     * @throws SchedulerException
     *           if the indicated Job does not exist, or the Trigger cannot be
     *           added to the Scheduler, or there is an internal Scheduler
     *           error.
     */
    Date scheduleJob(Trigger trigger) throws SchedulerException;

    /**
     * Schedule all of the given jobs with the related set of triggers.
     * <p> 使用相关的触发器结婚调度指定的job列表	</p>
     * 
     * <p>If any of the given jobs or triggers already exist (or more
     * specifically, if the keys are not unique) and the replace 
     * parameter is not set to true then an exception will be thrown.</p>
     * <p> 如果任何指定的job或触发器已经存在（更具体的说，如果key不是唯一的）而且替换参数的标志为false，那么将抛出异常	</p>
     * 
     * @throws ObjectAlreadyExistsException if the job/trigger keys
     * are not unique and the replace flag is not set to true. 
     */
    void scheduleJobs(Map<JobDetail, Set<? extends Trigger>> triggersAndJobs, boolean replace) throws SchedulerException;
    
    /**
     * Schedule the given job with the related set of triggers.
     * <p> 使用相关的触发器集合调度指定的job	</p>
     * 
     * <p>If any of the given job or triggers already exist (or more
     * specifically, if the keys are not unique) and the replace 
     * parameter is not set to true then an exception will be thrown.</p>
     * <p> 如果任何指定的job或触发器已经存在（更具体的说，如果key不是唯一的）而且替换参数的标志为false，那么将抛出异常	</p>
     * 
     * @throws ObjectAlreadyExistsException if the job/trigger keys
     * are not unique and the replace flag is not set to true. 
     */
    void scheduleJob(JobDetail jobDetail, Set<? extends Trigger> triggersForJob, boolean replace) throws SchedulerException;
    
    /**
     * Remove the indicated <code>{@link Trigger}</code> from the scheduler.
     * <p> 从调度器中删除标志的触发器	</p>
     * 
     * <p>If the related job does not have any other triggers, and the job is
     * not durable, then the job will also be deleted.</p>
     */
    boolean unscheduleJob(TriggerKey triggerKey)
        throws SchedulerException;

    /**
     * Remove all of the indicated <code>{@link Trigger}</code>s from the scheduler.
     * <p> 从调度器中删除所有标志的触发器	</p>
     * 
     * <p>If the related job does not have any other triggers, and the job is
     * not durable, then the job will also be deleted.</p>
     * <p> 如果相关的job没有任何其他触发器，而且job不是持久的，那么job也会被删除	</p>
     * 
     * <p>Note that while this bulk operation is likely more efficient than
     * invoking <code>unscheduleJob(TriggerKey triggerKey)</code> several
     * times, it may have the adverse affect of holding data locks for a
     * single long duration of time (rather than lots of small durations
     * of time).</p> 
     */
    boolean unscheduleJobs(List<TriggerKey> triggerKeys)
        throws SchedulerException;
    
    /**
     * Remove (delete) the <code>{@link org.quartz.Trigger}</code> with the
     * given key, and store the new given one - which must be associated
     * with the same job (the new trigger must have the job name & group specified) 
     * - however, the new trigger need not have the same name as the old trigger.
     * <p> 删除指定key的触发器，然后存在新的触发器---它必须关联于同一个job（新的触发器必须有指定的job名&组）
     * -然而，新的触发器不必和旧的触发器有相同的名称	</p>
     * 
     * @param triggerKey identity of the trigger to replace
     * @param newTrigger
     *          The new <code>Trigger</code> to be stored.
     * 
     * @return <code>null</code> if a <code>Trigger</code> with the given
     *         name & group was not found and removed from the store (and the 
     *         new trigger is therefore not stored), otherwise
     *         the first fire time of the newly scheduled trigger is returned.
     */
    Date rescheduleJob(TriggerKey triggerKey, Trigger newTrigger) 
        throws SchedulerException;
    
    /**
     * Add the given <code>Job</code> to the Scheduler - with no associated
     * <code>Trigger</code>. The <code>Job</code> will be 'dormant' until
     * it is scheduled with a <code>Trigger</code>, or <code>Scheduler.triggerJob()</code>
     * is called for it.
     * <p> 添加job到调度器中---没有关联触发器。该job将'休眠'直到它被触发器调度，或者	Scheduler.triggerJob()调用它	</p>
     * 
     * <p>
     * The <code>Job</code> must by definition be 'durable', if it is not,
     * SchedulerException will be thrown.
     * </p>
     * <p> job必须被定义为'持久的'，否则将抛出SchedulerException异常	</p>
     *
     * @see #addJob(JobDetail, boolean, boolean)
     *
     * @throws SchedulerException
     *           if there is an internal Scheduler error, or if the Job is not
     *           durable, or a Job with the same name already exists, and
     *           <code>replace</code> is <code>false</code>.
     */
    void addJob(JobDetail jobDetail, boolean replace)
        throws SchedulerException;

    /**
     * Add the given <code>Job</code> to the Scheduler - with no associated
     * <code>Trigger</code>. The <code>Job</code> will be 'dormant' until
     * it is scheduled with a <code>Trigger</code>, or <code>Scheduler.triggerJob()</code>
     * is called for it.
     * <p> 添加给定的Job到调度器中---没有关联触发器。该job将'休眠'指定有一个触发器调度器它，或者使用Scheduler.triggerJob()调用它	</p>
     * <p>
     * With the <code>storeNonDurableWhileAwaitingScheduling</code> parameter
     * set to <code>true</code>, a non-durable job can be stored.  Once it is
     * scheduled, it will resume normal non-durable behavior (i.e. be deleted
     * once there are no remaining associated triggers).
     * </p>
     * <p> storeNonDurableWhileAwaitingScheduling参数被设置为true时，一个非持久的job才能存储。
     * 一旦被调度，它将恢复正常的非持久行为（一个没有关联的触发器时将被删除）	</p>
     *
     * @throws SchedulerException
     *           if there is an internal Scheduler error, or if the Job is not
     *           durable, or a Job with the same name already exists, and
     *           <code>replace</code> is <code>false</code>.
     */
    void addJob(JobDetail jobDetail, boolean replace, boolean storeNonDurableWhileAwaitingScheduling)
            throws SchedulerException;

    /**
     * Delete the identified <code>Job</code> from the Scheduler - and any
     * associated <code>Trigger</code>s.
     * <p> 从调度器中删除指定的job---和任何关联的触发器	</p>
     * 
     * @return true if the Job was found and deleted.
     * @throws SchedulerException
     *           if there is an internal Scheduler error.
     */
    boolean deleteJob(JobKey jobKey)
        throws SchedulerException;

    /**
     * Delete the identified <code>Job</code>s from the Scheduler - and any
     * associated <code>Trigger</code>s.
     * <p> 从调度器中删除指定的job列表---和任何关联的触发器	</p>
     * 
     * <p>Note that while this bulk operation is likely more efficient than
     * invoking <code>deleteJob(JobKey jobKey)</code> several
     * times, it may have the adverse affect of holding data locks for a
     * single long duration of time (rather than lots of small durations
     * of time).</p>
     * <p> 注意当批量操作可能比多次调用deleteJob(JobKey jobKey)高效时，它可能有每次长期持有数据锁（而不是多次短时间）的不利影响	</p>
     *  
     * @return true if all of the Jobs were found and deleted, false if 
     * one or more were not deleted.
     * @throws SchedulerException
     *           if there is an internal Scheduler error.
     */
    boolean deleteJobs(List<JobKey> jobKeys)
        throws SchedulerException;
    
    /**
     * Trigger the identified <code>{@link org.quartz.JobDetail}</code>
     * (execute it now).
     * <p> 触发指定的JobDetail（现在执行它）	</p>
     */
    void triggerJob(JobKey jobKey)
        throws SchedulerException;

    /**
     * Trigger the identified <code>{@link org.quartz.JobDetail}</code>
     * (execute it now).
     * <p> 触发指定的JobDetail（现在执行它）	</p>
     * 
     * @param data the (possibly <code>null</code>) JobDataMap to be 
     * associated with the trigger that fires the job immediately. 
     */
    void triggerJob(JobKey jobKey, JobDataMap data)
        throws SchedulerException;

    /**
     * Pause the <code>{@link org.quartz.JobDetail}</code> with the given
     * key - by pausing all of its current <code>Trigger</code>s.
     * <p> 暂停指定key的JobDetail-----暂停它当前的所有触发器	</p>
     * 
     * @see #resumeJob(JobKey)
     */
    void pauseJob(JobKey jobKey)
        throws SchedulerException;

    /**
     * Pause all of the <code>{@link org.quartz.JobDetail}s</code> in the
     * matching groups - by pausing all of their <code>Trigger</code>s.
     *<p> 暂停匹配组中的所有JobDetail-----暂停他们所有的触发器	</p>
     * <p>
     * The Scheduler will "remember" the groups paused, and impose the
     * pause on any new jobs that are added to any of those groups
     * until it is resumed.
     * </p>
     * <p>	调度器将记住所有暂停组，然后强制暂停任何新加入的job直到恢复	 </p>
     * 
     * <p>NOTE: There is a limitation that only exactly matched groups
     * can be remembered as paused.  For example, if there are pre-existing
     * job in groups "aaa" and "bbb" and a matcher is given to pause
     * groups that start with "a" then the group "aaa" will be remembered
     * as paused and any subsequently added jobs in group "aaa" will be paused,
     * however if a job is added to group "axx" it will not be paused,
     * as "axx" wasn't known at the time the "group starts with a" matcher 
     * was applied.  HOWEVER, if there are pre-existing groups "aaa" and
     * "bbb" and a matcher is given to pause the group "axx" (with a
     * group equals matcher) then no jobs will be paused, but it will be 
     * remembered that group "axx" is paused and later when a job is added 
     * in that group, it will become paused.</p>
     * <p>	注：有一个限制是仅仅完全匹配的组可以记住暂停。例如，如果有预先存在的触发器在'aaa'和'bbb'中，然而匹配器被暂停的组名以'a'为开头，
     * 	那么组'aaa'将被记住暂停并且随后加入该组的job将被暂停，然而如果触发器加入组'axx'那他将不被暂停，'axx'尚不知道‘以a开头的组’的
     * 匹配器的使用。然而，如果预先存在了组'aaa'和'bbb',并且匹配器要求暂停组'axx'（匹配一个组），那么将不会有触发器被暂停，但是它将
     * 记住组'axx'是被暂停的，随后一个触发器被增加到这个组时，它将被暂停 </p>
     *
     * @param matcher The matcher to evaluate against know groups
     * @throws SchedulerException On error
     * @see #resumeJobs(org.quartz.impl.matchers.GroupMatcher)
     */
    void pauseJobs(GroupMatcher<JobKey> matcher) throws SchedulerException;

    /**
     * Pause the <code>{@link Trigger}</code> with the given key.
     * 
     * @see #resumeTrigger(TriggerKey)
     */
    void pauseTrigger(TriggerKey triggerKey)
        throws SchedulerException;

    /**
     * Pause all of the <code>{@link Trigger}s</code> in the groups matching.
     * <p>	暂停在组中匹配的所有触发器	 </p>
     * <p>
     * The Scheduler will "remember" all the groups paused, and impose the
     * pause on any new triggers that are added to any of those groups
     * until it is resumed.
     * </p>
     * <p>	调度器将记住所有暂停组，然后强制暂停任何新加入触发器直到恢复	 </p>
     * 
     * <p>NOTE: There is a limitation that only exactly matched groups
     * can be remembered as paused.  For example, if there are pre-existing
     * triggers in groups "aaa" and "bbb" and a matcher is given to pause
     * groups that start with "a" then the group "aaa" will be remembered as
     * paused and any subsequently added triggers in that group be paused,
     * however if a trigger is added to group "axx" it will not be paused,
     * as "axx" wasn't known at the time the "group starts with a" matcher 
     * was applied.  HOWEVER, if there are pre-existing groups "aaa" and
     * "bbb" and a matcher is given to pause the group "axx" (with a
     * group equals matcher) then no triggers will be paused, but it will be 
     * remembered that group "axx" is paused and later when a trigger is added
     * in that group, it will become paused.</p>
     * <p>	注：有一个限制是仅仅完全匹配的组可以记住暂停。例如，如果有预先存在的触发器在'aaa'和'bbb'中，然而匹配器被暂停的组名以'a'为开头，
     * 	那么组'aaa'将被记住暂停并且随后加入该组的触发器将被暂停，然而如果触发器加入组'axx'那他将不被暂停，'axx'尚不知道‘以a开头的组’的
     * 匹配器的使用。然而，如果预先存在了组'aaa'和'bbb',并且匹配器要求暂停组'axx'（匹配一个组），那么将不会有触发器被暂停，但是它将
     * 记住组'axx'是被暂停的，随后一个触发器被增加到这个组时，它将被暂停 </p>
     * 
     * @param matcher The matcher to evaluate against know groups
     * @throws SchedulerException
     * @see #resumeTriggers(org.quartz.impl.matchers.GroupMatcher)
     */
    void pauseTriggers(GroupMatcher<TriggerKey> matcher) throws SchedulerException;

    /**
     * Resume (un-pause) the <code>{@link org.quartz.JobDetail}</code> with
     * the given key.
     * <p>	恢复（非暂停）指定key的jobDetail	 </p>
     * <p>
     * If any of the <code>Job</code>'s<code>Trigger</code> s missed one
     * or more fire-times, then the <code>Trigger</code>'s misfire
     * instruction will be applied.
     * </p>
     * 
     * @see #pauseJob(JobKey)
     */
    void resumeJob(JobKey jobKey)
        throws SchedulerException;

    /**
     * Resume (un-pause) all of the <code>{@link org.quartz.JobDetail}s</code>
     * in matching groups.
     * <p>	恢复（非暂停）匹配组中的所有jobDetail	 </p>
     * <p>
     * If any of the <code>Job</code> s had <code>Trigger</code> s that
     * missed one or more fire-times, then the <code>Trigger</code>'s
     * misfire instruction will be applied.
     * </p>
     * 
     * @param matcher The matcher to evaluate against known paused groups
     * @throws SchedulerException On error
     * @see #pauseJobs(GroupMatcher)
     */
    void resumeJobs(GroupMatcher<JobKey> matcher) throws SchedulerException;

    /**
     * Resume (un-pause) the <code>{@link Trigger}</code> with the given
     * key.<p>	恢复（非暂停）指定key的触发器	 </p>
     * 
     * <p>
     * If the <code>Trigger</code> missed one or more fire-times, then the
     * <code>Trigger</code>'s misfire instruction will be applied.
     * </p><p> 如果触发器失败触发一或多次，那么触发器的触发失败指令将被使用	 </p>
     * 
     * @see #pauseTrigger(TriggerKey)
     */
    void resumeTrigger(TriggerKey triggerKey)
        throws SchedulerException;

    /**
     * Resume (un-pause) all of the <code>{@link Trigger}s</code> in matching groups.
     * <p> 恢复（非暂停）所有匹配组的中的触发器	</p>
     * <p>
     * If any <code>Trigger</code> missed one or more fire-times, then the
     * <code>Trigger</code>'s misfire instruction will be applied.
     * </p>
     * <p> 如果任何触发器失败触发一或多次，那么触发器的触发失败指令将被使用	 </p>
     * 
     * @param matcher The matcher to evaluate against know paused groups
     * @throws SchedulerException On error
     * @see #pauseTriggers(org.quartz.impl.matchers.GroupMatcher)
     */
    void resumeTriggers(GroupMatcher<TriggerKey> matcher) throws SchedulerException;

    /**
     * Pause all triggers - similar to calling <code>pauseTriggerGroup(group)</code>
     * on every group, however, after using this method <code>resumeAll()</code> 
     * must be called to clear the scheduler's state of 'remembering' that all 
     * new triggers will be paused as they are added. 
     * <p>暂停所有触发器,跟对每个组调用pauseTriggerGroup(group)一样，使用这个方法后，resumeAll()必须
     * 被调用去清除所有新添加入暂停的调度器的记忆状态	</p>
     * <p>
     * When <code>resumeAll()</code> is called (to un-pause), trigger misfire
     * instructions WILL be applied.
     * </p>
     * <p>	resumeAll()被调用时（对非暂停），触发器触发失败指令将被使用	</p>
     * 
     * @see #resumeAll()
     * @see #pauseTriggers(org.quartz.impl.matchers.GroupMatcher)
     * @see #standby()
     */
    void pauseAll() throws SchedulerException;

    /**
     * Resume (un-pause) all triggers - similar to calling 
     * <code>resumeTriggerGroup(group)</code> on every group.
     * <p> 恢复所有非暂停的触发器-----跟对每个组调用resumeTriggerGroup(group)时相同的	</p>
     * <p>
     * If any <code>Trigger</code> missed one or more fire-times, then the
     * <code>Trigger</code>'s misfire instruction will be applied.
     * </p>
     * <p> 如果任何触发器触发失败一或多次，那么触发器的触发失败指令将被使用	</p>
     * 
     * @see #pauseAll()
     */
    void resumeAll() throws SchedulerException;

    /**
     * Get the names of all known <code>{@link org.quartz.JobDetail}</code>
     * groups.
     * <p> 获取所有JobDetail组的名称	</p>
     */
    List<String> getJobGroupNames() throws SchedulerException;

    /**
     * Get the keys of all the <code>{@link org.quartz.JobDetail}s</code>
     * in the matching groups.
     * <p> 获取匹配组的所有JobDetail的名称	</p>
     * @param matcher Matcher to evaluate against known groups
     * @return Set of all keys matching
     * @throws SchedulerException On error
     */
    Set<JobKey> getJobKeys(GroupMatcher<JobKey> matcher) throws SchedulerException;

    /**
     * Get all <code>{@link Trigger}</code> s that are associated with the
     * identified <code>{@link org.quartz.JobDetail}</code>.
     * <p> 获取指定JobDetail相关联的所有触发器	</p>
     * 
     * <p>The returned Trigger objects will be snap-shots of the actual stored
     * triggers.  If you wish to modify a trigger, you must re-store the
     * trigger afterward (e.g. see {@link #rescheduleJob(TriggerKey, Trigger)}).
     * </p>
     * <p> 返回真实存储的触发器快照对象。如果你希望修改触发器，你必须使用后面的rescheduleJob(TriggerKey, Trigger)重存储触发器	</p>
     */
    List<? extends Trigger> getTriggersOfJob(JobKey jobKey)
        throws SchedulerException;

    /**
     * Get the names of all known <code>{@link Trigger}</code> groups.
     * <p> 获取所有触发器组的名称	</p>
     */
    List<String> getTriggerGroupNames() throws SchedulerException;

    /**
     * Get the names of all the <code>{@link Trigger}s</code> in the given
     * group.
     * <p> 获取指定组的所有触发器的名称	</p>
     * @param matcher Matcher to evaluate against known groups
     * @return List of all keys matching
     * @throws SchedulerException On error
     */
    Set<TriggerKey> getTriggerKeys(GroupMatcher<TriggerKey> matcher) throws SchedulerException;

    /**
     * Get the names of all <code>{@link Trigger}</code> groups that are paused.
     * <p> 获取已经停止的触发器组名称	</p>
     */
    Set<String> getPausedTriggerGroups() throws SchedulerException;
    
    /**
     * Get the <code>{@link JobDetail}</code> for the <code>Job</code>
     * instance with the given key.
     * <p> 获取指定key的JobDetail	</p>
     * <p>The returned JobDetail object will be a snap-shot of the actual stored
     * JobDetail.  If you wish to modify the JobDetail, you must re-store the
     * JobDetail afterward (e.g. see {@link #addJob(JobDetail, boolean)}).
     * </p>
     * <p> 返回一个真实存储的JobDetail快照对象，如果你希望改变JobDetail，你必须使用后面的addJob(JobDetail, boolean)重存储该JobDetail	</p>
     */
    JobDetail getJobDetail(JobKey jobKey)
        throws SchedulerException;

    /**
     * Get the <code>{@link Trigger}</code> instance with the given key.
     * <p> 获取指定key的触发器	</p>
     * <p>The returned Trigger object will be a snap-shot of the actual stored
     * trigger.  If you wish to modify the trigger, you must re-store the
     * trigger afterward (e.g. see {@link #rescheduleJob(TriggerKey, Trigger)}).
     * </p>
     * <p> 返回一个真实存储的触发器快照对象，如果你希望改变触发器，你必须使用后面的rescheduleJob(TriggerKey, Trigger)重存储该触发器	</p>
     */
    Trigger getTrigger(TriggerKey triggerKey)
        throws SchedulerException;

    /**
     * Get the current state of the identified <code>{@link Trigger}</code>.
     * <p>获取指定触发器的当前状态	</p>
     * @see Trigger.TriggerState
     */
    TriggerState getTriggerState(TriggerKey triggerKey)
        throws SchedulerException;

    /**
     * Add (register) the given <code>Calendar</code> to the Scheduler.
     * <p> 添加一个Calendar到调度器中	</p>
     * @param updateTriggers whether or not to update existing triggers that
     * referenced the already existing calendar so that they are 'correct'
     * based on the new trigger. 
     * 
     *  
     * @throws SchedulerException
     *           if there is an internal Scheduler error, or a Calendar with
     *           the same name already exists, and <code>replace</code> is
     *           <code>false</code>.
     */
    void addCalendar(String calName, Calendar calendar, boolean replace, boolean updateTriggers)
        throws SchedulerException;

    /**
     * Delete the identified <code>Calendar</code> from the Scheduler.
     * <p> 从调度器中删除指定的Calendar	</p>
     * 
     * <p>
     * If removal of the <code>Calendar</code> would result in
     * <code>Trigger</code>s pointing to non-existent calendars, then a
     * <code>SchedulerException</code> will be thrown.
     * </p>
     * <p> 如果删除Calendar将导致触发器指向不存在的calendars，那么SchedulerException将被抛出	</p>
     * 
     * @return true if the Calendar was found and deleted.
     * @throws SchedulerException
     *           if there is an internal Scheduler error, or one or more 
     *           triggers reference the calendar
     */
    boolean deleteCalendar(String calName) throws SchedulerException;

    /**
     * Get the <code>{@link Calendar}</code> instance with the given name.
     */
    Calendar getCalendar(String calName) throws SchedulerException;

    /**
     * Get the names of all registered <code>{@link Calendar}s</code>.
     */
    List<String> getCalendarNames() throws SchedulerException;

    /**
     * Request the interruption, within this Scheduler instance, of all 
     * currently executing instances of the identified <code>Job</code>, which 
     * must be an implementor of the <code>InterruptableJob</code> interface.
     * <p>请求中断这个调度器中指定job的所有正在执行实例，该job必须实现了InterruptableJob接口</p>
     * <p>
     * If more than one instance of the identified job is currently executing,
     * the <code>InterruptableJob#interrupt()</code> method will be called on
     * each instance.  However, there is a limitation that in the case that  
     * <code>interrupt()</code> on one instances throws an exception, all 
     * remaining  instances (that have not yet been interrupted) will not have 
     * their <code>interrupt()</code> method called.
     * </p>
     * <p>如果指定job的正在执行实例有多个，那么实例的interrupt()将被调用。然而这有一个限制，实例调用interrupt()方法时抛出异常
     * 情况下，所有剩余的实例将不再调用interrupt()方法	</p>
     * 
     * <p>
     * This method is not cluster aware.  That is, it will only interrupt 
     * instances of the identified InterruptableJob currently executing in this 
     * Scheduler instance, not across the entire cluster.
     * </p>
     * <p> 这个方法不识别集群。因此，它仅仅在当前调度器实例中打断了正在执行的指定job，不会贯穿于整个集群	</p>
     * 
     * @return true if at least one instance of the identified job was found
     * and interrupted.
     * @throws UnableToInterruptJobException if the job does not implement
     * <code>InterruptableJob</code>, or there is an exception while 
     * interrupting the job.
     * @see InterruptableJob#interrupt()
     * @see #getCurrentlyExecutingJobs()
     * @see #interrupt(String)
     */
    boolean interrupt(JobKey jobKey) throws UnableToInterruptJobException;
    
    /**
     * Request the interruption, within this Scheduler instance, of the 
     * identified executing <code>Job</code> instance, which 
     * must be an implementor of the <code>InterruptableJob</code> interface.
     * <p> 请求中断这个调度器实例中正在执行的执行job实例，该job必须实现了InterruptableJob接口	</p>
     * <p>
     * This method is not cluster aware.  That is, it will only interrupt 
     * instances of the identified InterruptableJob currently executing in this 
     * Scheduler instance, not across the entire cluster.
     * </p>
     * <p> 这个方法不识别集群。因此，它仅仅在当前调度器实例中打断了正在执行的指定job，不会贯穿于整个集群	</p>
     * 
     * @param fireInstanceId the unique identifier of the job instance to
     * be interrupted (see {@link JobExecutionContext#getFireInstanceId()}
     * @return true if the identified job instance was found and interrupted.
     * @throws UnableToInterruptJobException if the job does not implement
     * <code>InterruptableJob</code>, or there is an exception while 
     * interrupting the job.
     * @see InterruptableJob#interrupt()
     * @see #getCurrentlyExecutingJobs()
     * @see JobExecutionContext#getFireInstanceId()
     * @see #interrupt(JobKey)
     */
    boolean interrupt(String fireInstanceId) throws UnableToInterruptJobException;
    
    /**
     * Determine whether a {@link Job} with the given identifier already 
     * exists within the scheduler.
     * <p> 确定一个指定的job是否已经存在于调度器中	</p>
     * @param jobKey the identifier to check for
     * @return true if a Job exists with the given identifier
     * @throws SchedulerException 
     */
    boolean checkExists(JobKey jobKey) throws SchedulerException; 
   
    /**
     * Determine whether a {@link Trigger} with the given identifier already 
     * exists within the scheduler.
     * <p> 确定一个指定的触发器是否已经存在于调度器中	</p>
     * 
     * @param triggerKey the identifier to check for
     * @return true if a Trigger exists with the given identifier
     * @throws SchedulerException 
     */
    boolean checkExists(TriggerKey triggerKey) throws SchedulerException;
    
    /**
     * Clears (deletes!) all scheduling data - all {@link Job}s, {@link Trigger}s
     * {@link Calendar}s.
     * <p> 删除所有的调度数据----所有的job、trigger、calendar		</p>
     * 
     * @throws SchedulerException
     */
    void clear() throws SchedulerException;


}
