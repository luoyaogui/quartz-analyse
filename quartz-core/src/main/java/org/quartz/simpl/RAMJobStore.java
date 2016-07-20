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

package org.quartz.simpl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;

import org.quartz.Calendar;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.JobPersistenceException;
import org.quartz.ObjectAlreadyExistsException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.Trigger.CompletedExecutionInstruction;
import org.quartz.Trigger.TriggerState;
import org.quartz.Trigger.TriggerTimeComparator;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.impl.matchers.StringMatcher;
import org.quartz.spi.ClassLoadHelper;
import org.quartz.spi.JobStore;
import org.quartz.spi.OperableTrigger;
import org.quartz.spi.SchedulerSignaler;
import org.quartz.spi.TriggerFiredBundle;
import org.quartz.spi.TriggerFiredResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * <p>
 * This class implements a <code>{@link org.quartz.spi.JobStore}</code> that
 * utilizes RAM as its storage device.
 * <--JobStore的类实现，利用了存储设备的RAM-->
 * </p>
 * 
 * <p>
 * As you should know, the ramification of this is that access is extrememly
 * fast, but the data is completely volatile - therefore this <code>JobStore</code>
 * should not be used if true persistence between program shutdowns is
 * required.
 * <--正如你知道的，这种访问是非常快的，但是数据非常不稳定----所以程序关闭期间真正持久化时不应该使用-->
 * </p>
 * 
 * @author James House
 * @author Sharada Jambula
 * @author Eric Mueller
 */
public class RAMJobStore implements JobStore {

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Data members.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    protected HashMap<JobKey, JobWrapper> jobsByKey = new HashMap<JobKey, JobWrapper>(1000);//jobkey--job包装器的映射map

    protected HashMap<TriggerKey, TriggerWrapper> triggersByKey = new HashMap<TriggerKey, TriggerWrapper>(1000);//触发器key--触发器包装器的映射map

    protected HashMap<String, HashMap<JobKey, JobWrapper>> jobsByGroup = new HashMap<String, HashMap<JobKey, JobWrapper>>(25);//job组---job包装映射列表的map

    protected HashMap<String, HashMap<TriggerKey, TriggerWrapper>> triggersByGroup = new HashMap<String, HashMap<TriggerKey, TriggerWrapper>>(25);//触发器组---触发器映射列表的map

    protected TreeSet<TriggerWrapper> timeTriggers = new TreeSet<TriggerWrapper>(new TriggerWrapperComparator());//根据触发器触发时间进行树排序，用于调度处理的

    protected HashMap<String, Calendar> calendarsByName = new HashMap<String, Calendar>(25);//日期名-日期对象的map

    protected ArrayList<TriggerWrapper> triggers = new ArrayList<TriggerWrapper>(1000);//保存实际的触发器

    protected final Object lock = new Object();

    protected HashSet<String> pausedTriggerGroups = new HashSet<String>();//暂停触发器组

    protected HashSet<String> pausedJobGroups = new HashSet<String>();//暂停任务组

    protected HashSet<JobKey> blockedJobs = new HashSet<JobKey>();//阻塞任务列表
    
    protected long misfireThreshold = 5000l;//触发失败阀值

    protected SchedulerSignaler signaler;

    private final Logger log = LoggerFactory.getLogger(getClass());

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Constructors.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    /**
     * <p>
     * Create a new <code>RAMJobStore</code>.
     * </p>
     */
    public RAMJobStore() {
    }

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Interface.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    protected Logger getLog() {
        return log;
    }

    /**
     * <p>
     * Called by the QuartzScheduler before the <code>JobStore</code> is
     * used, in order to give the it a chance to initialize.
     * </p>
     */
    public void initialize(ClassLoadHelper loadHelper, SchedulerSignaler schedSignaler) {

        this.signaler = schedSignaler;

        getLog().info("RAMJobStore initialized.");
    }

    public void schedulerStarted() {
        // nothing to do
    }

    public void schedulerPaused() {
        // nothing to do
    }
    
    public void schedulerResumed() {
        // nothing to do
    }
    
    public long getMisfireThreshold() {//获取触发失败阀值
        return misfireThreshold;
    }

    /**
     * The number of milliseconds by which a trigger must have missed its
     * next-fire-time, in order for it to be considered "misfired" and thus
     * have its misfire instruction applied.
     * 
     * @param misfireThreshold the new misfire threshold
     */
    @SuppressWarnings("UnusedDeclaration")
    public void setMisfireThreshold(long misfireThreshold) {
        if (misfireThreshold < 1) {
            throw new IllegalArgumentException("Misfire threshold must be larger than 0");
        }
        this.misfireThreshold = misfireThreshold;
    }

    /**
     * <p>
     * Called by the QuartzScheduler to inform the <code>JobStore</code> that
     * it should free up all of it's resources because the scheduler is
     * shutting down.
     * </p>
     */
    public void shutdown() {
    }

    public boolean supportsPersistence() {
        return false;
    }

    /**
     * Clear (delete!) all scheduling data - all {@link Job}s, {@link Trigger}s
     * {@link Calendar}s.
     * 
     * @throws JobPersistenceException
     */
    public void clearAllSchedulingData() throws JobPersistenceException {

        synchronized (lock) {
            // unschedule jobs (delete triggers)
            List<String> lst = getTriggerGroupNames();
            for (String group: lst) {
                Set<TriggerKey> keys = getTriggerKeys(GroupMatcher.triggerGroupEquals(group));
                for (TriggerKey key: keys) {
                    removeTrigger(key);
                }
            }
            // delete jobs
            lst = getJobGroupNames();
            for (String group: lst) {
                Set<JobKey> keys = getJobKeys(GroupMatcher.jobGroupEquals(group));
                for (JobKey key: keys) {
                    removeJob(key);
                }
            }
            // delete calendars
            lst = getCalendarNames();
            for(String name: lst) {
                removeCalendar(name);
            }
        }
    }
    
    /**
     * <p>
     * Store the given <code>{@link org.quartz.JobDetail}</code> and <code>{@link org.quartz.Trigger}</code>.
     * </p>
     * 
     * @param newJob
     *          The <code>JobDetail</code> to be stored.
     * @param newTrigger
     *          The <code>Trigger</code> to be stored.
     * @throws ObjectAlreadyExistsException
     *           if a <code>Job</code> with the same name/group already
     *           exists.
     */
    public void storeJobAndTrigger(JobDetail newJob,
            OperableTrigger newTrigger) throws JobPersistenceException {
        storeJob(newJob, false);
        storeTrigger(newTrigger, false);
    }

    /**
     * <p>
     * Store the given <code>{@link org.quartz.Job}</code>.
     * </p>
     * 
     * @param newJob
     *          The <code>Job</code> to be stored.
     * @param replaceExisting
     *          If <code>true</code>, any <code>Job</code> existing in the
     *          <code>JobStore</code> with the same name & group should be
     *          over-written.
     * @throws ObjectAlreadyExistsException
     *           if a <code>Job</code> with the same name/group already
     *           exists, and replaceExisting is set to false.
     */
    public void storeJob(JobDetail newJob,
            boolean replaceExisting) throws ObjectAlreadyExistsException {
        JobWrapper jw = new JobWrapper((JobDetail)newJob.clone());

        boolean repl = false;

        synchronized (lock) {
            if (jobsByKey.get(jw.key) != null) {
                if (!replaceExisting) {
                    throw new ObjectAlreadyExistsException(newJob);
                }
                repl = true;
            }

            if (!repl) {
                // get job group
                HashMap<JobKey, JobWrapper> grpMap = jobsByGroup.get(newJob.getKey().getGroup());
                if (grpMap == null) {
                    grpMap = new HashMap<JobKey, JobWrapper>(100);
                    jobsByGroup.put(newJob.getKey().getGroup(), grpMap);
                }
                // add to jobs by group
                grpMap.put(newJob.getKey(), jw);
                // add to jobs by FQN map
                jobsByKey.put(jw.key, jw);
            } else {
                // update job detail
                JobWrapper orig = jobsByKey.get(jw.key);
                orig.jobDetail = jw.jobDetail; // already cloned
            }
        }
    }

    /**
     * <p>
     * Remove (delete) the <code>{@link org.quartz.Job}</code> with the given
     * name, and any <code>{@link org.quartz.Trigger}</code> s that reference
     * it.
     * </p>
     *
     * @return <code>true</code> if a <code>Job</code> with the given name &
     *         group was found and removed from the store.
     */
    public boolean removeJob(JobKey jobKey) {

        boolean found = false;

        synchronized (lock) {
            List<OperableTrigger> triggersOfJob = getTriggersForJob(jobKey);
            for (OperableTrigger trig: triggersOfJob) {
                this.removeTrigger(trig.getKey());
                found = true;
            }
            
            found = (jobsByKey.remove(jobKey) != null) | found;
            if (found) {

                HashMap<JobKey, JobWrapper> grpMap = jobsByGroup.get(jobKey.getGroup());
                if (grpMap != null) {
                    grpMap.remove(jobKey);
                    if (grpMap.size() == 0) {
                        jobsByGroup.remove(jobKey.getGroup());
                    }
                }
            }
        }

        return found;
    }

    public boolean removeJobs(List<JobKey> jobKeys)
            throws JobPersistenceException {
        boolean allFound = true;

        synchronized (lock) {
            for(JobKey key: jobKeys)
                allFound = removeJob(key) && allFound;
        }

        return allFound;
    }

    public boolean removeTriggers(List<TriggerKey> triggerKeys)
            throws JobPersistenceException {
        boolean allFound = true;

        synchronized (lock) {
            for(TriggerKey key: triggerKeys)
                allFound = removeTrigger(key) && allFound;
        }

        return allFound;
    }

    public void storeJobsAndTriggers(
            Map<JobDetail, Set<? extends Trigger>> triggersAndJobs, boolean replace)
            throws JobPersistenceException {

        synchronized (lock) {
            // make sure there are no collisions...
            if(!replace) {
                for(Entry<JobDetail, Set<? extends Trigger>> e: triggersAndJobs.entrySet()) {
                    if(checkExists(e.getKey().getKey()))
                        throw new ObjectAlreadyExistsException(e.getKey());
                    for(Trigger trigger: e.getValue()) {
                        if(checkExists(trigger.getKey()))
                            throw new ObjectAlreadyExistsException(trigger);
                    }
                }
            }
            // do bulk add...
            for(Entry<JobDetail, Set<? extends Trigger>> e: triggersAndJobs.entrySet()) {
                storeJob(e.getKey(), true);
                for(Trigger trigger: e.getValue()) {
                    storeTrigger((OperableTrigger) trigger, true);
                }
            }
        }
        
    }

    /**
     * <p>
     * Store the given <code>{@link org.quartz.Trigger}</code>.
     * </p>
     *
     * @param newTrigger
     *          The <code>Trigger</code> to be stored.
     * @param replaceExisting
     *          If <code>true</code>, any <code>Trigger</code> existing in
     *          the <code>JobStore</code> with the same name & group should
     *          be over-written.
     * @throws ObjectAlreadyExistsException
     *           if a <code>Trigger</code> with the same name/group already
     *           exists, and replaceExisting is set to false.
     *
     * @see #pauseTriggers(org.quartz.impl.matchers.GroupMatcher)
     */
    public void storeTrigger(OperableTrigger newTrigger,
            boolean replaceExisting) throws JobPersistenceException {
        TriggerWrapper tw = new TriggerWrapper((OperableTrigger)newTrigger.clone());//复制

        synchronized (lock) {
            if (triggersByKey.get(tw.key) != null) {//已经存在
                if (!replaceExisting) {
                    throw new ObjectAlreadyExistsException(newTrigger);//存在不替换，则抛出已经存在异常
                }
    
                removeTrigger(newTrigger.getKey(), false);
            }
    
            if (retrieveJob(newTrigger.getJobKey()) == null) {//读取jobDetail，确定该触发器被引用
                throw new JobPersistenceException("The job ("
                        + newTrigger.getJobKey()
                        + ") referenced by the trigger does not exist.");
            }

            // add to triggers array
            triggers.add(tw);//添加
            // add to triggers by group
            HashMap<TriggerKey, TriggerWrapper> grpMap = triggersByGroup.get(newTrigger.getKey().getGroup());//获取组
            if (grpMap == null) {
                grpMap = new HashMap<TriggerKey, TriggerWrapper>(100);
                triggersByGroup.put(newTrigger.getKey().getGroup(), grpMap);//创建新组
            }
            grpMap.put(newTrigger.getKey(), tw);//添加
            // add to triggers by FQN map
            triggersByKey.put(tw.key, tw);//放入触发映射中

            if (pausedTriggerGroups.contains(newTrigger.getKey().getGroup())//属于暂停触发组	或  暂停任务组，则更改为暂停/暂停阻塞状态
                    || pausedJobGroups.contains(newTrigger.getJobKey().getGroup())) {
                tw.state = TriggerWrapper.STATE_PAUSED;
                if (blockedJobs.contains(tw.jobKey)) {
                    tw.state = TriggerWrapper.STATE_PAUSED_BLOCKED;
                }
            } else if (blockedJobs.contains(tw.jobKey)) {//属于阻塞任务，则更新为阻塞状态
                tw.state = TriggerWrapper.STATE_BLOCKED;
            } else {
                timeTriggers.add(tw);//否则添加到触发树中
            }
        }
    }

    /**
     * <p>
     * Remove (delete) the <code>{@link org.quartz.Trigger}</code> with the
     * given name.
     * </p>
     *
     * @return <code>true</code> if a <code>Trigger</code> with the given
     *         name & group was found and removed from the store.
     */
    public boolean removeTrigger(TriggerKey triggerKey) {
        return removeTrigger(triggerKey, true);
    }
    
    private boolean removeTrigger(TriggerKey key, boolean removeOrphanedJob) {

        boolean found;

        synchronized (lock) {
            // remove from triggers by FQN map
            found = (triggersByKey.remove(key) != null);//存在
            if (found) {
                TriggerWrapper tw = null;
                // remove from triggers by group
                HashMap<TriggerKey, TriggerWrapper> grpMap = triggersByGroup.get(key.getGroup());//读取组信息
                if (grpMap != null) {
                    grpMap.remove(key);//从组中删除
                    if (grpMap.size() == 0) {
                        triggersByGroup.remove(key.getGroup());//如果该组没有其他数据，则删除组信息
                    }
                }
                // remove from triggers array
                Iterator<TriggerWrapper> tgs = triggers.iterator();//检索触发器列表，执行删除操作
                while (tgs.hasNext()) {
                    tw = tgs.next();
                    if (key.equals(tw.key)) {
                        tgs.remove();
                        break;
                    }
                }
                timeTriggers.remove(tw);//从触发器触发排序树中删除

                if (removeOrphanedJob) {//删除孤立的触发器
                    JobWrapper jw = jobsByKey.get(tw.jobKey);
                    List<OperableTrigger> trigs = getTriggersForJob(tw.jobKey);
                    if ((trigs == null || trigs.size() == 0) && !jw.jobDetail.isDurable()) {
                        if (removeJob(jw.key)) {
                            signaler.notifySchedulerListenersJobDeleted(jw.key);
                        }
                    }
                }
            }
        }

        return found;
    }


    /**
     * @see org.quartz.spi.JobStore#replaceTrigger(TriggerKey triggerKey, OperableTrigger newTrigger)
     */
    public boolean replaceTrigger(TriggerKey triggerKey, OperableTrigger newTrigger) throws JobPersistenceException {

        boolean found;

        synchronized (lock) {
            // remove from triggers by FQN map
            TriggerWrapper tw = triggersByKey.remove(triggerKey);//从触发映射中删除
            found = (tw != null);

            if (found) {//存在

                if (!tw.getTrigger().getJobKey().equals(newTrigger.getJobKey())) {//新的触发器并没有和老的触发器关联同一个Job
                    throw new JobPersistenceException("New trigger is not related to the same job as the old trigger.");
                }

                tw = null;
                // remove from triggers by group
                HashMap<TriggerKey, TriggerWrapper> grpMap = triggersByGroup.get(triggerKey.getGroup());//读取组信息
                if (grpMap != null) {
                    grpMap.remove(triggerKey);//删除
                    if (grpMap.size() == 0) {
                        triggersByGroup.remove(triggerKey.getGroup());//如果组中无其他的数据，则删除
                    }
                }
                // remove from triggers array
                Iterator<TriggerWrapper> tgs = triggers.iterator();//检查触发器列表，删除该触发器
                while (tgs.hasNext()) {
                    tw = tgs.next();
                    if (triggerKey.equals(tw.key)) {
                        tgs.remove();
                        break;
                    }
                }
                timeTriggers.remove(tw);//从触发器触发排序树中删除

                try {
                    storeTrigger(newTrigger, false);//存储
                } catch(JobPersistenceException jpe) {
                    storeTrigger(tw.getTrigger(), false); // put previous trigger back...失败则把原先的触发器放回
                    throw jpe;
                }
            }
        }

        return found;
    }

    /**
     * <p>
     * Retrieve the <code>{@link org.quartz.JobDetail}</code> for the given
     * <code>{@link org.quartz.Job}</code>.
     * </p>
     *
     * @return The desired <code>Job</code>, or null if there is no match.
     */
    public JobDetail retrieveJob(JobKey jobKey) {
        synchronized(lock) {
            JobWrapper jw = jobsByKey.get(jobKey);
            return (jw != null) ? (JobDetail)jw.jobDetail.clone() : null;
        }
    }

    /**
     * <p>
     * Retrieve the given <code>{@link org.quartz.Trigger}</code>.
     * </p>
     *
     * @return The desired <code>Trigger</code>, or null if there is no
     *         match.
     */
    public OperableTrigger retrieveTrigger(TriggerKey triggerKey) {
        synchronized(lock) {
            TriggerWrapper tw = triggersByKey.get(triggerKey);
    
            return (tw != null) ? (OperableTrigger)tw.getTrigger().clone() : null;
        }
    }
    
    /**
     * Determine whether a {@link Job} with the given identifier already 
     * exists within the scheduler.
     * 
     * @param jobKey the identifier to check for
     * @return true if a Job exists with the given identifier
     * @throws JobPersistenceException
     */
    public boolean checkExists(JobKey jobKey) throws JobPersistenceException {
        synchronized(lock) {
            JobWrapper jw = jobsByKey.get(jobKey);
            return (jw != null);
        }
    }
    
    /**
     * Determine whether a {@link Trigger} with the given identifier already 
     * exists within the scheduler.
     * 
     * @param triggerKey the identifier to check for
     * @return true if a Trigger exists with the given identifier
     * @throws JobPersistenceException
     */
    public boolean checkExists(TriggerKey triggerKey) throws JobPersistenceException {
        synchronized(lock) {
            TriggerWrapper tw = triggersByKey.get(triggerKey);
    
            return (tw != null);
        }
    }
 
    /**
     * <p>
     * Get the current state of the identified <code>{@link Trigger}</code>.
     * </p>
     *
     * @see TriggerState#NORMAL
     * @see TriggerState#PAUSED
     * @see TriggerState#COMPLETE
     * @see TriggerState#ERROR
     * @see TriggerState#BLOCKED
     * @see TriggerState#NONE
     */
    public TriggerState getTriggerState(TriggerKey triggerKey) throws JobPersistenceException {
        synchronized(lock) {
            TriggerWrapper tw = triggersByKey.get(triggerKey);
            
            if (tw == null) {
                return TriggerState.NONE;
            }
    
            if (tw.state == TriggerWrapper.STATE_COMPLETE) {
                return TriggerState.COMPLETE;
            }
    
            if (tw.state == TriggerWrapper.STATE_PAUSED) {
                return TriggerState.PAUSED;
            }
    
            if (tw.state == TriggerWrapper.STATE_PAUSED_BLOCKED) {
                return TriggerState.PAUSED;
            }
    
            if (tw.state == TriggerWrapper.STATE_BLOCKED) {
                return TriggerState.BLOCKED;
            }
    
            if (tw.state == TriggerWrapper.STATE_ERROR) {
                return TriggerState.ERROR;
            }
    
            return TriggerState.NORMAL;
        }
    }

    /**
     * <p>
     * Store the given <code>{@link org.quartz.Calendar}</code>.
     * </p>
     * <p> 存储Calendar </p>
     *
     * @param calendar
     *          The <code>Calendar</code> to be stored.
     * @param replaceExisting
     *          If <code>true</code>, any <code>Calendar</code> existing
     *          in the <code>JobStore</code> with the same name & group
     *          should be over-written.
     * @param updateTriggers
     *          If <code>true</code>, any <code>Trigger</code>s existing
     *          in the <code>JobStore</code> that reference an existing
     *          Calendar with the same name with have their next fire time
     *          re-computed with the new <code>Calendar</code>.
     * @throws ObjectAlreadyExistsException
     *           if a <code>Calendar</code> with the same name already
     *           exists, and replaceExisting is set to false.
     */
    public void storeCalendar(String name,
            Calendar calendar, boolean replaceExisting, boolean updateTriggers)
        throws ObjectAlreadyExistsException {

        calendar = (Calendar) calendar.clone();
        
        synchronized (lock) {
    
            Object obj = calendarsByName.get(name);
    
            if (obj != null && !replaceExisting) {//如果已经存在，且不替换，则抛出已存在异常
                throw new ObjectAlreadyExistsException(
                    "Calendar with name '" + name + "' already exists.");
            } else if (obj != null) {
                calendarsByName.remove(name);//否则删除
            }
    
            calendarsByName.put(name, calendar);//添加到映射列表中
    
            if(obj != null && updateTriggers) {//更新，重置触发器排序树
                for (TriggerWrapper tw : getTriggerWrappersForCalendar(name)) {
                    OperableTrigger trig = tw.getTrigger();
                    boolean removed = timeTriggers.remove(tw);

                    trig.updateWithNewCalendar(calendar, getMisfireThreshold());

                    if (removed) {
                        timeTriggers.add(tw);
                    }
                }
            }
        }
    }

    /**
     * <p>
     * Remove (delete) the <code>{@link org.quartz.Calendar}</code> with the
     * given name.
     * </p>
     * <p>删除指定名称的Calendar</p>
     *
     * <p>
     * If removal of the <code>Calendar</code> would result in
     * <code>Trigger</code>s pointing to non-existent calendars, then a
     * <code>JobPersistenceException</code> will be thrown.</p>
     * <p>如果移除Calendar可能导致触发器指向不存在的Calendar对象，那么将抛出JobPersistenceException异常</p>
     * 
     * @param calName The name of the <code>Calendar</code> to be removed.
     * @return <code>true</code> if a <code>Calendar</code> with the given name
     * was found and removed from the store.
     */
    public boolean removeCalendar(String calName)
        throws JobPersistenceException {
        int numRefs = 0;

        synchronized (lock) {
            for (TriggerWrapper trigger : triggers) {//检索该日期名对应的触发器引用数量
                OperableTrigger trigg = trigger.trigger;
                if (trigg.getCalendarName() != null
                        && trigg.getCalendarName().equals(calName)) {
                    numRefs++;
                }
            }
        }

        if (numRefs > 0) {//有引用，抛出‘如果被触发器引用则该日期不能被删除’异常
            throw new JobPersistenceException(
                    "Calender cannot be removed if it referenced by a Trigger!");
        }

        return (calendarsByName.remove(calName) != null);//删除日期名对应的映射数据
    }

    /**
     * <p>
     * Retrieve the given <code>{@link org.quartz.Trigger}</code>.
     * </p>
     * <p> 读取指定名称的Calendar </p>
     *
     * @param calName
     *          The name of the <code>Calendar</code> to be retrieved.
     * @return The desired <code>Calendar</code>, or null if there is no
     *         match.
     */
    public Calendar retrieveCalendar(String calName) {
        synchronized (lock) {
            Calendar cal = calendarsByName.get(calName);
            if(cal != null)
                return (Calendar) cal.clone();
            return null;
        }
    }

    /**
     * <p>
     * Get the number of <code>{@link org.quartz.JobDetail}</code> s that are
     * stored in the <code>JobsStore</code>.
     * </p>
     */
    public int getNumberOfJobs() {
        synchronized (lock) {
            return jobsByKey.size();
        }
    }

    /**
     * <p>
     * Get the number of <code>{@link org.quartz.Trigger}</code> s that are
     * stored in the <code>JobsStore</code>.
     * </p>
     */
    public int getNumberOfTriggers() {
        synchronized (lock) {
            return triggers.size();
        }
    }

    /**
     * <p>
     * Get the number of <code>{@link org.quartz.Calendar}</code> s that are
     * stored in the <code>JobsStore</code>.
     * </p>
     */
    public int getNumberOfCalendars() {
        synchronized (lock) {
            return calendarsByName.size();
        }
    }

    /**
     * <p>
     * Get the names of all of the <code>{@link org.quartz.Job}</code> s that
     * match the given groupMatcher.
     * </p>
     */
    public Set<JobKey> getJobKeys(GroupMatcher<JobKey> matcher) {
        Set<JobKey> outList = null;
        synchronized (lock) {

            StringMatcher.StringOperatorName operator = matcher.getCompareWithOperator();
            String compareToValue = matcher.getCompareToValue();

            switch(operator) {
                case EQUALS:
                    HashMap<JobKey, JobWrapper> grpMap = jobsByGroup.get(compareToValue);
                    if (grpMap != null) {
                        outList = new HashSet<JobKey>();

                        for (JobWrapper jw : grpMap.values()) {

                            if (jw != null) {
                                outList.add(jw.jobDetail.getKey());
                            }
                        }
                    }
                    break;

                default:
                    for (Map.Entry<String, HashMap<JobKey, JobWrapper>> entry : jobsByGroup.entrySet()) {
                        if(operator.evaluate(entry.getKey(), compareToValue) && entry.getValue() != null) {
                            if(outList == null) {
                                outList = new HashSet<JobKey>();
                            }
                            for (JobWrapper jobWrapper : entry.getValue().values()) {
                                if(jobWrapper != null) {
                                    outList.add(jobWrapper.jobDetail.getKey());
                                }
                            }
                        }
                    }
            }
        }

        return outList == null ? java.util.Collections.<JobKey>emptySet() : outList;
    }

    /**
     * <p>
     * Get the names of all of the <code>{@link org.quartz.Calendar}</code> s
     * in the <code>JobStore</code>.
     * </p>
     *
     * <p>
     * If there are no Calendars in the given group name, the result should be
     * a zero-length array (not <code>null</code>).
     * </p>
     */
    public List<String> getCalendarNames() {
        synchronized(lock) {
            return new LinkedList<String>(calendarsByName.keySet());
        }
    }

    /**
     * <p>
     * Get the names of all of the <code>{@link org.quartz.Trigger}</code> s
     * that match the given groupMatcher.
     * </p>
     */
    public Set<TriggerKey> getTriggerKeys(GroupMatcher<TriggerKey> matcher) {
        Set<TriggerKey> outList = null;
        synchronized (lock) {

            StringMatcher.StringOperatorName operator = matcher.getCompareWithOperator();
            String compareToValue = matcher.getCompareToValue();

            switch(operator) {
                case EQUALS:
                    HashMap<TriggerKey, TriggerWrapper> grpMap = triggersByGroup.get(compareToValue);
                    if (grpMap != null) {
                        outList = new HashSet<TriggerKey>();

                        for (TriggerWrapper tw : grpMap.values()) {

                            if (tw != null) {
                                outList.add(tw.trigger.getKey());
                            }
                        }
                    }
                    break;

                default:
                    for (Map.Entry<String, HashMap<TriggerKey, TriggerWrapper>> entry : triggersByGroup.entrySet()) {
                        if(operator.evaluate(entry.getKey(), compareToValue) && entry.getValue() != null) {
                            if(outList == null) {
                                outList = new HashSet<TriggerKey>();
                            }
                            for (TriggerWrapper triggerWrapper : entry.getValue().values()) {
                                if(triggerWrapper != null) {
                                    outList.add(triggerWrapper.trigger.getKey());
                                }
                            }
                        }
                    }
            }
        }

        return outList == null ? Collections.<TriggerKey>emptySet() : outList;
    }

    /**
     * <p>
     * Get the names of all of the <code>{@link org.quartz.Job}</code>
     * groups.
     * </p>
     */
    public List<String> getJobGroupNames() {
        List<String> outList;

        synchronized (lock) {
            outList = new LinkedList<String>(jobsByGroup.keySet());
        }

        return outList;
    }

    /**
     * <p>
     * Get the names of all of the <code>{@link org.quartz.Trigger}</code>
     * groups.
     * </p>
     */
    public List<String> getTriggerGroupNames() {
        LinkedList<String> outList;

        synchronized (lock) {
            outList = new LinkedList<String>(triggersByGroup.keySet());
        }

        return outList;
    }

    /**
     * <p>
     * Get all of the Triggers that are associated to the given Job.
     * </p>
     * <p> 获取关联了指定job的所有triggers </p>
     *
     * <p>
     * If there are no matches, a zero-length array should be returned.
     * </p>
     * <p> 如果没有匹配的，那么返回0元素的数组 </p>
     */
    public List<OperableTrigger> getTriggersForJob(JobKey jobKey) {
        ArrayList<OperableTrigger> trigList = new ArrayList<OperableTrigger>();

        synchronized (lock) {
            for (TriggerWrapper tw : triggers) {
                if (tw.jobKey.equals(jobKey)) {
                    trigList.add((OperableTrigger) tw.trigger.clone());
                }
            }
        }

        return trigList;
    }
    //获取指定job的所有触发器封装器
    protected ArrayList<TriggerWrapper> getTriggerWrappersForJob(JobKey jobKey) {
        ArrayList<TriggerWrapper> trigList = new ArrayList<TriggerWrapper>();
        synchronized (lock) {
            for (TriggerWrapper trigger : triggers) {
                if (trigger.jobKey.equals(jobKey)) {
                    trigList.add(trigger);
                }
            }
        }

        return trigList;
    }
    //获取指定日期名的所有触发器封装器
    protected ArrayList<TriggerWrapper> getTriggerWrappersForCalendar(String calName) {
        ArrayList<TriggerWrapper> trigList = new ArrayList<TriggerWrapper>();

        synchronized (lock) {
            for (TriggerWrapper tw : triggers) {
                String tcalName = tw.getTrigger().getCalendarName();
                if (tcalName != null && tcalName.equals(calName)) {
                    trigList.add(tw);
                }
            }
        }

        return trigList;
    }

    /**
     * <p>
     * Pause the <code>{@link Trigger}</code> with the given name.
     * </p>
     * <p>  暂停给定名称的触发器	</p>
     *
     */
    public void pauseTrigger(TriggerKey triggerKey) {

        synchronized (lock) {
            TriggerWrapper tw = triggersByKey.get(triggerKey);
    
            // does the trigger exist?
            if (tw == null || tw.trigger == null) {//不存在
                return;
            }
    
            // if the trigger is "complete" pausing it does not make sense...
            if (tw.state == TriggerWrapper.STATE_COMPLETE) {//已经执行完成
                return;
            }

            if(tw.state == TriggerWrapper.STATE_BLOCKED) {
                tw.state = TriggerWrapper.STATE_PAUSED_BLOCKED;
            } else {
                tw.state = TriggerWrapper.STATE_PAUSED;
            }

            timeTriggers.remove(tw);//从触发排序树中删除
        }
    }

    /**
     * <p>
     * Pause all of the known <code>{@link Trigger}s</code> matching.
     * </p>
     * <p> 暂停所有匹配的触发器，并返回组列表	</p>
     *
     * <p>
     * The JobStore should "remember" the groups paused, and impose the
     * pause on any new triggers that are added to one of these groups while the group is
     * paused.
     * </p>
     * <p> JobStore应该记住暂停的组，强制暂停任何加入到这些暂停组的触发器	</p>
     *
     */
    public List<String> pauseTriggers(GroupMatcher<TriggerKey> matcher) {

        List<String> pausedGroups;
        synchronized (lock) {
            pausedGroups = new LinkedList<String>();

            StringMatcher.StringOperatorName operator = matcher.getCompareWithOperator();
            switch (operator) {
                case EQUALS://等于
                    if(pausedTriggerGroups.add(matcher.getCompareToValue())) {
                        pausedGroups.add(matcher.getCompareToValue());
                    }
                    break;
                default :
                    for (String group : triggersByGroup.keySet()) {
                        if(operator.evaluate(group, matcher.getCompareToValue())) {
                            if(pausedTriggerGroups.add(matcher.getCompareToValue())) {
                                pausedGroups.add(group);
                            }
                        }
                    }
            }

            for (String pausedGroup : pausedGroups) {//轮训，暂停指定组的所有触发器
                Set<TriggerKey> keys = getTriggerKeys(GroupMatcher.triggerGroupEquals(pausedGroup));

                for (TriggerKey key: keys) {
                    pauseTrigger(key);
                }
            }
        }

        return pausedGroups;
    }

    /**
     * <p>
     * Pause the <code>{@link org.quartz.JobDetail}</code> with the given
     * name - by pausing all of its current <code>Trigger</code>s.
     * </p>
     *
     */
    public void pauseJob(JobKey jobKey) {
        synchronized (lock) {
            List<OperableTrigger> triggersOfJob = getTriggersForJob(jobKey);
            for (OperableTrigger trigger: triggersOfJob) {
                pauseTrigger(trigger.getKey());
            }
        }
    }

    /**
     * <p>
     * Pause all of the <code>{@link org.quartz.JobDetail}s</code> in the
     * given group - by pausing all of their <code>Trigger</code>s.
     * </p>
     *
     *
     * <p>
     * The JobStore should "remember" that the group is paused, and impose the
     * pause on any new jobs that are added to the group while the group is
     * paused.
     * </p>
     */
    public List<String> pauseJobs(GroupMatcher<JobKey> matcher) {
        List<String> pausedGroups = new LinkedList<String>();
        synchronized (lock) {

            StringMatcher.StringOperatorName operator = matcher.getCompareWithOperator();
            switch (operator) {
                case EQUALS:
                    if (pausedJobGroups.add(matcher.getCompareToValue())) {
                        pausedGroups.add(matcher.getCompareToValue());
                    }
                    break;
                default :
                    for (String group : jobsByGroup.keySet()) {
                        if(operator.evaluate(group, matcher.getCompareToValue())) {
                            if (pausedJobGroups.add(group)) {
                                pausedGroups.add(group);
                            }
                        }
                    }
            }

            for (String groupName : pausedGroups) {
                for (JobKey jobKey: getJobKeys(GroupMatcher.jobGroupEquals(groupName))) {
                    List<OperableTrigger> triggersOfJob = getTriggersForJob(jobKey);
                    for (OperableTrigger trigger: triggersOfJob) {
                        pauseTrigger(trigger.getKey());
                    }
                }
            }
        }

        return pausedGroups;
    }

    /**
     * <p>
     * Resume (un-pause) the <code>{@link Trigger}</code> with the given
     * key.
     * </p>
     * <p> 恢复非暂停的给定key的触发器	</p>
     *
     * <p>
     * If the <code>Trigger</code> missed one or more fire-times, then the
     * <code>Trigger</code>'s misfire instruction will be applied.
     * </p>
     * <p> 如果触发器失败一或多次，那么触发器的触发失败指令将马上使用	</p>
     */
    public void resumeTrigger(TriggerKey triggerKey) {

        synchronized (lock) {
            TriggerWrapper tw = triggersByKey.get(triggerKey);
    
            // does the trigger exist?
            if (tw == null || tw.trigger == null) {
                return;
            }
    
            OperableTrigger trig = tw.getTrigger();
    
            // （如果触发器是非暂停重启，那么它毫无意义）if the trigger is not paused resuming it does not make sense...
            if (tw.state != TriggerWrapper.STATE_PAUSED &&
                    tw.state != TriggerWrapper.STATE_PAUSED_BLOCKED) {
                return;
            }

            if(blockedJobs.contains( trig.getJobKey() )) {
                tw.state = TriggerWrapper.STATE_BLOCKED;
            } else {
                tw.state = TriggerWrapper.STATE_WAITING;
            }

            applyMisfire(tw);//使用触发失败

            if (tw.state == TriggerWrapper.STATE_WAITING) {//如果状态是等待
                timeTriggers.add(tw);//增加到触发树中
            }
        }
    }

    /**
     * <p>
     * Resume (un-pause) all of the <code>{@link Trigger}s</code> in the
     * given group.
     * </p>
     * <p> 恢复指定组的所有非暂停触发器		</p>
     *
     * <p>
     * If any <code>Trigger</code> missed one or more fire-times, then the
     * <code>Trigger</code>'s misfire instruction will be applied.
     * </p>
     *	<p> 如果任何触发器失败了一或多次，那么触发器的触发失败指令将被使用	</p>
     */
    public List<String> resumeTriggers(GroupMatcher<TriggerKey> matcher) {
        Set<String> groups = new HashSet<String>();

        synchronized (lock) {
            Set<TriggerKey> keys = getTriggerKeys(matcher);

            for (TriggerKey triggerKey: keys) {
                groups.add(triggerKey.getGroup());
                if(triggersByKey.get(triggerKey) != null) {
                    String jobGroup = triggersByKey.get(triggerKey).jobKey.getGroup();
                    if(pausedJobGroups.contains(jobGroup)) {
                        continue;
                    }
                }
                resumeTrigger(triggerKey);
            }
            for (String group : groups) {
                pausedTriggerGroups.remove(group);
            }
        }

        return new ArrayList<String>(groups);
    }

    /**
     * <p>
     * Resume (un-pause) the <code>{@link org.quartz.JobDetail}</code> with
     * the given name.
     * </p>
     * <p> 恢复指定名称的非暂停jobDetail	</p>
     *
     * <p>
     * If any of the <code>Job</code>'s<code>Trigger</code> s missed one
     * or more fire-times, then the <code>Trigger</code>'s misfire
     * instruction will be applied.
     * </p>
     *	<p> 如果job关联的任何触发器失败了一或多次，那么触发器的触发失败指令将被使用	</p>
     */
    public void resumeJob(JobKey jobKey) {

        synchronized (lock) {
            List<OperableTrigger> triggersOfJob = getTriggersForJob(jobKey);
            for (OperableTrigger trigger: triggersOfJob) {
                resumeTrigger(trigger.getKey());
            }
        }
    }

    /**
     * <p>
     * Resume (un-pause) all of the <code>{@link org.quartz.JobDetail}s</code>
     * in the given group.
     * </p>
     *	<p> 恢复指定组的所有非暂停jobDetail	</p>
     * <p>
     * If any of the <code>Job</code> s had <code>Trigger</code> s that
     * missed one or more fire-times, then the <code>Trigger</code>'s
     * misfire instruction will be applied.
     * </p>
     *	<p> 如果job关联的任何触发器失败了一或多次，那么触发器的触发失败指令将被使用	</p>
     */
    public Collection<String> resumeJobs(GroupMatcher<JobKey> matcher) {
        Set<String> resumedGroups = new HashSet<String>();
        synchronized (lock) {
            Set<JobKey> keys = getJobKeys(matcher);

            for (String pausedJobGroup : pausedJobGroups) {
                if(matcher.getCompareWithOperator().evaluate(pausedJobGroup, matcher.getCompareToValue())) {
                    resumedGroups.add(pausedJobGroup);
                }
            }

            for (String resumedGroup : resumedGroups) {
                pausedJobGroups.remove(resumedGroup);
            }

            for (JobKey key: keys) {
                List<OperableTrigger> triggersOfJob = getTriggersForJob(key);
                for (OperableTrigger trigger: triggersOfJob) {
                    resumeTrigger(trigger.getKey());
                }
            }
        }
        return resumedGroups;
    }

    /**
     * <p>
     * Pause all triggers - equivalent of calling <code>pauseTriggerGroup(group)</code>
     * on every group.
     * </p>
     * <p> 暂停所有触发器	</p>
     *
     * <p>
     * When <code>resumeAll()</code> is called (to un-pause), trigger misfire
     * instructions WILL be applied.
     * </p>
     *
     * @see #resumeAll()
     * @see #pauseTrigger(org.quartz.TriggerKey)
     * @see #pauseTriggers(org.quartz.impl.matchers.GroupMatcher)
     */
    public void pauseAll() {

        synchronized (lock) {
            List<String> names = getTriggerGroupNames();

            for (String name: names) {
                pauseTriggers(GroupMatcher.triggerGroupEquals(name));
            }
        }
    }

    /**
     * <p>
     * Resume (un-pause) all triggers - equivalent of calling <code>resumeTriggerGroup(group)</code>
     * on every group.
     * </p>
     * <p> 	恢复所有触发器	</p>
     *
     * <p>
     * If any <code>Trigger</code> missed one or more fire-times, then the
     * <code>Trigger</code>'s misfire instruction will be applied.
     * </p>
     *
     * @see #pauseAll()
     */
    public void resumeAll() {

        synchronized (lock) {
            pausedJobGroups.clear();
            resumeTriggers(GroupMatcher.anyTriggerGroup());
        }
    }
    /**
     * 应用触发失败判断
     */
    protected boolean applyMisfire(TriggerWrapper tw) {

        long misfireTime = System.currentTimeMillis();
        if (getMisfireThreshold() > 0) {//获取触发失败阀值
            misfireTime -= getMisfireThreshold();
        }

        Date tnft = tw.trigger.getNextFireTime();//获取下一次触发时间
        //为null 或  大于当前时间减去触发失败阀值的时长    或  属于触发失败忽略策略
        if (tnft == null || tnft.getTime() > misfireTime 
                || tw.trigger.getMisfireInstruction() == Trigger.MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY) { 
            return false; 
        }

        Calendar cal = null;
        if (tw.trigger.getCalendarName() != null) {//根据日期名读取日期
            cal = retrieveCalendar(tw.trigger.getCalendarName());
        }
        //通知触发器监听器触发失败
        signaler.notifyTriggerListenersMisfired((OperableTrigger)tw.trigger.clone());

        tw.trigger.updateAfterMisfire(cal);//更新触发器状态

        if (tw.trigger.getNextFireTime() == null) {//无下次触发时间则更新状态
            tw.state = TriggerWrapper.STATE_COMPLETE;
            signaler.notifySchedulerListenersFinalized(tw.trigger);//通知调度触发器回收
            synchronized (lock) {
                timeTriggers.remove(tw);//删除
            }
        } else if (tnft.equals(tw.trigger.getNextFireTime())) {//下次触发时间跟这次一样
            return false;
        }

        return true;
    }

    private static final AtomicLong ftrCtr = new AtomicLong(System.currentTimeMillis());

    protected String getFiredTriggerRecordId() {
        return String.valueOf(ftrCtr.incrementAndGet());
    }

    /**
     * <p>
     * Get a handle to the next trigger to be fired, and mark it as 'reserved'
     * by the calling scheduler.
     * </p>
     * <p>获取下一个要被启动的触发器，然后有调度器标志它为'保留'</p>
     *
     * @see #releaseAcquiredTrigger(OperableTrigger)
     */
    public List<OperableTrigger> acquireNextTriggers(long noLaterThan, int maxCount, long timeWindow) {
        synchronized (lock) {
            List<OperableTrigger> result = new ArrayList<OperableTrigger>();
            Set<JobKey> acquiredJobKeysForNoConcurrentExec = new HashSet<JobKey>();
            Set<TriggerWrapper> excludedTriggers = new HashSet<TriggerWrapper>();
            long firstAcquiredTriggerFireTime = 0;//好像没什么用
            
            // return empty list if store has no triggers.
            if (timeTriggers.size() == 0)//时间触发器列表为空
                return result;
            
            while (true) {
                TriggerWrapper tw;

                try {
                    tw = timeTriggers.first();//读取第一个
                    if (tw == null)
                        break;
                    timeTriggers.remove(tw);//删除
                } catch (java.util.NoSuchElementException nsee) {
                    break;
                }

                if (tw.trigger.getNextFireTime() == null) {//下次触发时间为null，则继续
                    continue;
                }

                if (applyMisfire(tw)) {//使用触发失败？
                    if (tw.trigger.getNextFireTime() != null) {
                        timeTriggers.add(tw);//加入触发执行
                    }
                    continue;
                }
                //时间窗口：算法的主要思路是通过采用一个冲突任务替换一个已规划的任务，并将替换任务后移至下一时间窗口或在同一时间窗口内部后移。
                if (tw.getTrigger().getNextFireTime().getTime() > noLaterThan + timeWindow) {//不迟于+时间窗口
                    timeTriggers.add(tw);//加入触发执行
                    break;
                }
                
                // If trigger's job is set as @DisallowConcurrentExecution, and it has already been added to result, then
                // put it back into the timeTriggers set and continue to search for next trigger.
                JobKey jobKey = tw.trigger.getJobKey();
                JobDetail job = jobsByKey.get(tw.trigger.getJobKey()).jobDetail;
                if (job.isConcurrentExectionDisallowed()) {//不允许并发执行
                    if (acquiredJobKeysForNoConcurrentExec.contains(jobKey)) {//jobkey非并发执行
                        excludedTriggers.add(tw);//放入踢出队列
                        continue; // go to next trigger in store.
                    } else {
                        acquiredJobKeysForNoConcurrentExec.add(jobKey);
                    }
                }

                tw.state = TriggerWrapper.STATE_ACQUIRED;//设置状态
                tw.trigger.setFireInstanceId(getFiredTriggerRecordId());//设置触发执行ID
                OperableTrigger trig = (OperableTrigger) tw.trigger.clone();
                result.add(trig);//触发器加入结果中
                if(firstAcquiredTriggerFireTime == 0)
                    firstAcquiredTriggerFireTime = tw.trigger.getNextFireTime().getTime();

                if (result.size() == maxCount)
                    break;
            }
            
            // （如果我们排除触发器去阻止ACQUIRE状态由于不允许并发执行，我们需要把他们装回存储）If we did excluded triggers to prevent ACQUIRE state due to DisallowConcurrentExecution, we need to add them back to store.
            if (excludedTriggers.size() > 0)
                timeTriggers.addAll(excludedTriggers);
            return result;
        }
    }

    /**
     * <p>
     * Inform the <code>JobStore</code> that the scheduler no longer plans to
     * fire the given <code>Trigger</code>, that it had previously acquired
     * (reserved).
     * </p>
     * <p>	通知JobStore，调度器不再计划触发给定的触发器，那些已经保存的	</p>
     */
    public void releaseAcquiredTrigger(OperableTrigger trigger) {
        synchronized (lock) {
            TriggerWrapper tw = triggersByKey.get(trigger.getKey());
            if (tw != null && tw.state == TriggerWrapper.STATE_ACQUIRED) {//如果处于捕获处理中的状态，则更新为等待状态，放入触发树中
                tw.state = TriggerWrapper.STATE_WAITING;
                timeTriggers.add(tw);
            }
        }
    }

    /**
     * <p>
     * Inform the <code>JobStore</code> that the scheduler is now firing the
     * given <code>Trigger</code> (executing its associated <code>Job</code>),
     * that it had previously acquired (reserved).
     * </p>
     * <p>通知jobstore，调度器现在要触发之前保存的指定触发器（执行与它相关联的job）</p>
     */
    public List<TriggerFiredResult> triggersFired(List<OperableTrigger> firedTriggers) {

        synchronized (lock) {
            List<TriggerFiredResult> results = new ArrayList<TriggerFiredResult>();//保存执行时数据和异常数据的list

            for (OperableTrigger trigger : firedTriggers) {//循环处理
                TriggerWrapper tw = triggersByKey.get(trigger.getKey());//获取该触发器的包装类
                // （被获得的已经删除）was the trigger deleted since being acquired?
                if (tw == null || tw.trigger == null) {
                    continue;
                }
                // （被获得的已经触发完成、暂停、阻塞等等）was the trigger completed, paused, blocked, etc. since being acquired?
                if (tw.state != TriggerWrapper.STATE_ACQUIRED) {
                    continue;
                }

                Calendar cal = null;
                if (tw.trigger.getCalendarName() != null) {//触发器的日历不为空，则检查
                    cal = retrieveCalendar(tw.trigger.getCalendarName());
                    if(cal == null)
                        continue;
                }
                Date prevFireTime = trigger.getPreviousFireTime();//获取上一次触发时间
                // （以防触发器在获取或触发中被替换）in case trigger was replaced between acquiring and firing
                timeTriggers.remove(tw);
                // （调用触发复制）call triggered on our copy, and the scheduler's copy
                tw.trigger.triggered(cal);//更新下一次触发的时间
                trigger.triggered(cal);//更新下一次触发的时间，实现类都这样实现
                //tw.state = TriggerWrapper.STATE_EXECUTING;
                tw.state = TriggerWrapper.STATE_WAITING;
                
                //封装job相关的数据--------《	用于QuartzSchedulerThread的从jobstore中获取的返回执行时数据的一个简单类		》
                TriggerFiredBundle bndle = new TriggerFiredBundle(retrieveJob(
                        tw.jobKey), trigger, cal,
                        false, new Date(), trigger.getPreviousFireTime(), prevFireTime,
                        trigger.getNextFireTime());

                JobDetail job = bndle.getJobDetail();//读取job细节数据

                if (job.isConcurrentExectionDisallowed()) {//是否携带了不允许并发执行的注解
                    ArrayList<TriggerWrapper> trigs = getTriggerWrappersForJob(job.getKey());//获取job相关的触发器封装类
                    for (TriggerWrapper ttw : trigs) {
                        if (ttw.state == TriggerWrapper.STATE_WAITING) {//如果是等待，修改为阻塞
                            ttw.state = TriggerWrapper.STATE_BLOCKED;
                        }
                        if (ttw.state == TriggerWrapper.STATE_PAUSED) {//如果是暂停，修改为暂停阻塞
                            ttw.state = TriggerWrapper.STATE_PAUSED_BLOCKED;
                        }
                        timeTriggers.remove(ttw);//移除
                    }
                    blockedJobs.add(job.getKey());//加入阻塞
                } else if (tw.trigger.getNextFireTime() != null) {
                    synchronized (lock) {
                        timeTriggers.add(tw);
                    }
                }

                results.add(new TriggerFiredResult(bndle));
            }
            return results;
        }
    }

    /**
     * <p>
     * Inform the <code>JobStore</code> that the scheduler has completed the
     * firing of the given <code>Trigger</code> (and the execution its
     * associated <code>Job</code>), and that the <code>{@link org.quartz.JobDataMap}</code>
     * in the given <code>JobDetail</code> should be updated if the <code>Job</code>
     * is stateful.
     * </p>
     * <p>	通知JobStore，调度器已经完成了给定触发器的触发（执行它相关的job，而如果job是有状态的那么给定jobDetail的JobDataMap应该进行了更新）	</p>
     */
    public void triggeredJobComplete(OperableTrigger trigger,
            JobDetail jobDetail, CompletedExecutionInstruction triggerInstCode) {
        synchronized (lock) {

            JobWrapper jw = jobsByKey.get(jobDetail.getKey());//读取job
            TriggerWrapper tw = triggersByKey.get(trigger.getKey());//读取触发器

            // It's possible that the job is null if:
            //   1- it was deleted during execution
            //   2- RAMJobStore is being used only for volatile jobs / triggers
            //      from the JDBC job store
            if (jw != null) {
                JobDetail jd = jw.jobDetail;

                if (jd.isPersistJobDataAfterExecution()) {//是否执行执行后持久化
                    JobDataMap newData = jobDetail.getJobDataMap();
                    if (newData != null) {
                        newData = (JobDataMap)newData.clone();
                        newData.clearDirtyFlag();
                    }
                    jd = jd.getJobBuilder().setJobData(newData).build();
                    jw.jobDetail = jd;
                }
                if (jd.isConcurrentExectionDisallowed()) {//是否不允许并发执行
                    blockedJobs.remove(jd.getKey());
                    ArrayList<TriggerWrapper> trigs = getTriggerWrappersForJob(jd.getKey());
                    for(TriggerWrapper ttw : trigs) {
                        if (ttw.state == TriggerWrapper.STATE_BLOCKED) {
                            ttw.state = TriggerWrapper.STATE_WAITING;
                            timeTriggers.add(ttw);
                        }
                        if (ttw.state == TriggerWrapper.STATE_PAUSED_BLOCKED) {
                            ttw.state = TriggerWrapper.STATE_PAUSED;
                        }
                    }
                    signaler.signalSchedulingChange(0L);
                }
            } else { // （即使已经删除，但可能需要做一些清理工作）even if it was deleted, there may be cleanup to do
                blockedJobs.remove(jobDetail.getKey());
            }
    
            // （检查执行期触发器删除）check for trigger deleted during execution...
            if (tw != null) {
                if (triggerInstCode == CompletedExecutionInstruction.DELETE_TRIGGER) {//执行删除
                    
                    if(trigger.getNextFireTime() == null) {//下次触发时间为null
                        // double check for possible reschedule within job 
                        // execution, which would cancel the need to delete...
                        if(tw.getTrigger().getNextFireTime() == null) {//无下次触发时间
                            removeTrigger(trigger.getKey());//删除触发器
                        }
                    } else {
                        removeTrigger(trigger.getKey());
                        signaler.signalSchedulingChange(0L);
                    }
                } else if (triggerInstCode == CompletedExecutionInstruction.SET_TRIGGER_COMPLETE) {
                    tw.state = TriggerWrapper.STATE_COMPLETE;
                    timeTriggers.remove(tw);//从触发树中删除
                    signaler.signalSchedulingChange(0L);
                } else if(triggerInstCode == CompletedExecutionInstruction.SET_TRIGGER_ERROR) {
                    getLog().info("Trigger " + trigger.getKey() + " set to ERROR state.");
                    tw.state = TriggerWrapper.STATE_ERROR;
                    signaler.signalSchedulingChange(0L);
                } else if (triggerInstCode == CompletedExecutionInstruction.SET_ALL_JOB_TRIGGERS_ERROR) {
                    getLog().info("All triggers of Job " 
                            + trigger.getJobKey() + " set to ERROR state.");
                    setAllTriggersOfJobToState(trigger.getJobKey(), TriggerWrapper.STATE_ERROR);
                    signaler.signalSchedulingChange(0L);
                } else if (triggerInstCode == CompletedExecutionInstruction.SET_ALL_JOB_TRIGGERS_COMPLETE) {
                    setAllTriggersOfJobToState(trigger.getJobKey(), TriggerWrapper.STATE_COMPLETE);
                    signaler.signalSchedulingChange(0L);
                }
            }
        }
    }

    protected void setAllTriggersOfJobToState(JobKey jobKey, int state) {
        ArrayList<TriggerWrapper> tws = getTriggerWrappersForJob(jobKey);
        for (TriggerWrapper tw : tws) {
            tw.state = state;
            if (state != TriggerWrapper.STATE_WAITING) {
                timeTriggers.remove(tw);
            }
        }
    }
    /**
     * 返回触发器的key列表字符串
     */
    @SuppressWarnings("UnusedDeclaration")
    protected String peekTriggers() {

        StringBuilder str = new StringBuilder();
        synchronized (lock) {
            for (TriggerWrapper triggerWrapper : triggersByKey.values()) {
                str.append(triggerWrapper.trigger.getKey().getName());
                str.append("/");
            }
        }
        str.append(" | ");

        synchronized (lock) {
            for (TriggerWrapper timeTrigger : timeTriggers) {
                str.append(timeTrigger.trigger.getKey().getName());
                str.append("->");
            }
        }

        return str.toString();
    }

    /** 
     * @see org.quartz.spi.JobStore#getPausedTriggerGroups()
     */
    public Set<String> getPausedTriggerGroups() throws JobPersistenceException {
        HashSet<String> set = new HashSet<String>();
        
        set.addAll(pausedTriggerGroups);
        
        return set;
    }

    public void setInstanceId(String schedInstId) {
        //
    }

    public void setInstanceName(String schedName) {
        //
    }

    public void setThreadPoolSize(final int poolSize) {
        //
    }

    public long getEstimatedTimeToReleaseAndAcquireTrigger() {
        return 5;
    }

    public boolean isClustered() {
        return false;
    }

}

/**
 * Helper Classes.
 * 触发器封装类比较器
 */
class TriggerWrapperComparator implements Comparator<TriggerWrapper>, java.io.Serializable {
  
    private static final long serialVersionUID = 8809557142191514261L;

    TriggerTimeComparator ttc = new TriggerTimeComparator();
    
    public int compare(TriggerWrapper trig1, TriggerWrapper trig2) {
        return ttc.compare(trig1.trigger, trig2.trigger);
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof TriggerWrapperComparator);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
/**
 * job封装类，包含了job-key、jobDetail
 */
class JobWrapper {

    public JobKey key;

    public JobDetail jobDetail;

    JobWrapper(JobDetail jobDetail) {
        this.jobDetail = jobDetail;
        key = jobDetail.getKey();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof JobWrapper) {
            JobWrapper jw = (JobWrapper) obj;
            if (jw.key.equals(this.key)) {
                return true;
            }
        }

        return false;
    }
    
    @Override
    public int hashCode() {
        return key.hashCode(); 
    }
}
/**
 * 触发器封装类，包含了触发器key、job-key、触发器、状态
 */
class TriggerWrapper {

    public final TriggerKey key;

    public final JobKey jobKey;

    public final OperableTrigger trigger;

    public int state = STATE_WAITING;

    public static final int STATE_WAITING = 0;

    public static final int STATE_ACQUIRED = 1;

    @SuppressWarnings("UnusedDeclaration")
    public static final int STATE_EXECUTING = 2;

    public static final int STATE_COMPLETE = 3;

    public static final int STATE_PAUSED = 4;

    public static final int STATE_BLOCKED = 5;

    public static final int STATE_PAUSED_BLOCKED = 6;

    public static final int STATE_ERROR = 7;
    
    TriggerWrapper(OperableTrigger trigger) {
        if(trigger == null)
            throw new IllegalArgumentException("Trigger cannot be null!");
        this.trigger = trigger;
        key = trigger.getKey();
        this.jobKey = trigger.getJobKey();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TriggerWrapper) {
            TriggerWrapper tw = (TriggerWrapper) obj;
            if (tw.key.equals(this.key)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public int hashCode() {
        return key.hashCode(); 
    }

    
    public OperableTrigger getTrigger() {
        return this.trigger;
    }
}
