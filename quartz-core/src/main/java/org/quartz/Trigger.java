
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

import java.io.Serializable;
import java.util.Comparator;
import java.util.Date;



/**
 * The base interface with properties common to all <code>Trigger</code>s -
 * use {@link TriggerBuilder} to instantiate an actual Trigger.
 * 《	Trigger通用属性的基本接口，使用TriggerBuilder实例化真实的触发器	》
 * <p>
 * <code>Triggers</code>s have a {@link TriggerKey} associated with them, which
 * should uniquely identify them within a single <code>{@link Scheduler}</code>.
 * 《	触发器有一个触发key关联，在每个调度器中该key应该唯一标识触发器		》
 * </p>
 * 
 * <p>
 * <code>Trigger</code>s are the 'mechanism' by which <code>Job</code>s
 * are scheduled. Many <code>Trigger</code>s can point to the same <code>Job</code>,
 * but a single <code>Trigger</code> can only point to one <code>Job</code>.
 * 《	触发器是job调度的一种机制，多个触发器可以指向同一个job，但一个触发器只能指向一个job	》
 * </p>
 * 
 * <p>
 * Triggers can 'send' parameters/data to <code>Job</code>s by placing contents
 * into the <code>JobDataMap</code> on the <code>Trigger</code>.
 * 《	触发器通过把内容放入自己的JobDataMap中来发送参数/数据给job		》
 * </p>
 *
 * @see TriggerBuilder
 * @see JobDataMap
 * @see JobExecutionContext
 * @see TriggerUtils
 * @see SimpleTrigger
 * @see CronTrigger
 * @see CalendarIntervalTrigger
 * 
 * @author James House
 */
public interface Trigger extends Serializable, Cloneable, Comparable<Trigger> {

    public static final long serialVersionUID = -3904243490805975570L;
    
    public enum TriggerState { NONE, NORMAL, PAUSED, COMPLETE, ERROR, BLOCKED }
    
    /**
     * <p><code>NOOP</code> Instructs the <code>{@link Scheduler}</code> that the 
     * <code>{@link Trigger}</code> has no further instructions.</p>
     * 
     * <p><code>RE_EXECUTE_JOB</code> Instructs the <code>{@link Scheduler}</code> that the 
     * <code>{@link Trigger}</code> wants the <code>{@link org.quartz.JobDetail}</code> to 
     * re-execute immediately. If not in a 'RECOVERING' or 'FAILED_OVER' situation, the
     * execution context will be re-used (giving the <code>Job</code> the
     * ability to 'see' anything placed in the context by its last execution).</p>
     * 
     * <p><code>SET_TRIGGER_COMPLETE</code> Instructs the <code>{@link Scheduler}</code> that the 
     * <code>{@link Trigger}</code> should be put in the <code>COMPLETE</code> state.</p>
     * 
     * <p><code>DELETE_TRIGGER</code> Instructs the <code>{@link Scheduler}</code> that the 
     * <code>{@link Trigger}</code> wants itself deleted.</p>
     * 
     * <p><code>SET_ALL_JOB_TRIGGERS_COMPLETE</code> Instructs the <code>{@link Scheduler}</code> 
     * that all <code>Trigger</code>s referencing the same <code>{@link org.quartz.JobDetail}</code> 
     * as this one should be put in the <code>COMPLETE</code> state.</p>
     * 
     * <p><code>SET_TRIGGER_ERROR</code> Instructs the <code>{@link Scheduler}</code> that all 
     * <code>Trigger</code>s referencing the same <code>{@link org.quartz.JobDetail}</code> as
     * this one should be put in the <code>ERROR</code> state.</p>
     *
     * <p><code>SET_ALL_JOB_TRIGGERS_ERROR</code> Instructs the <code>{@link Scheduler}</code> that 
     * the <code>Trigger</code> should be put in the <code>ERROR</code> state.</p>
     */
    public enum CompletedExecutionInstruction { NOOP, RE_EXECUTE_JOB, SET_TRIGGER_COMPLETE, DELETE_TRIGGER, 
        SET_ALL_JOB_TRIGGERS_COMPLETE, SET_TRIGGER_ERROR, SET_ALL_JOB_TRIGGERS_ERROR }

    /**
     * Instructs the <code>{@link Scheduler}</code> that upon a mis-fire
     * situation, the <code>updateAfterMisfire()</code> method will be called
     * on the <code>Trigger</code> to determine the mis-fire instruction,
     * which logic will be trigger-implementation-dependent.
     * 《	指示调度器在触发失败情况下，在调度器确定了触发失败指令时将调用updateAfterMisfire()方法，
     * 		它的逻辑将是触发器实现依赖的		》
     * 
     * <p>
     * In order to see if this instruction fits your needs, you should look at
     * the documentation for the <code>getSmartMisfirePolicy()</code> method
     * on the particular <code>Trigger</code> implementation you are using.
     * 《	为了看这个指令是否满足你的需要，你应该查看文档了解你使用的触发器实现的getSmartMisfirePolicy()方法。		》
     * </p>
     */
    public static final int MISFIRE_INSTRUCTION_SMART_POLICY = 0;
    
    /**
     * Instructs the <code>{@link Scheduler}</code> that the 
     * <code>Trigger</code> will never be evaluated for a misfire situation, 
     * and that the scheduler will simply try to fire it as soon as it can, 
     * and then update the Trigger as if it had fired at the proper time. 
     * 《	指示调度器在触发失败情况下触发器永远不会被评估，而且调度器将尽可能快的简单的尝试触发，然后
     * 		更新触发器好像它已经在合适的时间触发了一样	》
     * 
     * <p>NOTE: if a trigger uses this instruction, and it has missed 
     * several of its scheduled firings, then several rapid firings may occur 
     * as the trigger attempt to catch back up to where it would have been. 
     * For example, a SimpleTrigger that fires every 15 seconds which has 
     * misfired for 5 minutes will fire 20 times once it gets the chance to 
     * fire.
     * 《	注：如果触发器使用这个指令，而且它已经好几次调度触发失败了，那么当触发器尝试捕获备份时可能会快速触发好几次。
     * 		例如，一个每15秒触发一次的SimpleTrigger已经触发失败了5分钟，那么它获得触发机会时将一次触发20次	》
     * </p>
     */
    public static final int MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY = -1;
    
    /**
     * The default value for priority.
     */
    public static final int DEFAULT_PRIORITY = 5;

    public TriggerKey getKey();

    public JobKey getJobKey();
    
    /**
     * Return the description given to the <code>Trigger</code> instance by
     * its creator (if any).
     * 
     * @return null if no description was set.
     */
    public String getDescription();

    /**
     * Get the name of the <code>{@link Calendar}</code> associated with this
     * Trigger.
     * 
     * @return <code>null</code> if there is no associated Calendar.
     */
    public String getCalendarName();

    /**
     * Get the <code>JobDataMap</code> that is associated with the 
     * <code>Trigger</code>.
     * 
     * <p>
     * Changes made to this map during job execution are not re-persisted, and
     * in fact typically result in an <code>IllegalStateException</code>.
     * 《	在job执行期间map的更改不会在进行持久化，事实上导致了典型的IllegalStateException	》
     * </p>
     */
    public JobDataMap getJobDataMap();

    /**
     * The priority of a <code>Trigger</code> acts as a tiebreaker such that if 
     * two <code>Trigger</code>s have the same scheduled fire time, then the
     * one with the higher priority will get first access to a worker
     * thread.
     * 《	触发器优先级扮演决胜者的角色，例如两个在同时调度触发的触发器，那么优先级高的将获得工作线程的优先访问	》
     * 
     * <p>
     * If not explicitly set, the default value is <code>5</code>.
     * </p>
     * 
     * @see #DEFAULT_PRIORITY
     */
    public int getPriority();

    /**
     * Used by the <code>{@link Scheduler}</code> to determine whether or not
     * it is possible for this <code>Trigger</code> to fire again.
     * 《	被调度器用于确定该触发器是否重复触发了		》
     * 
     * <p>
     * If the returned value is <code>false</code> then the <code>Scheduler</code>
     * may remove the <code>Trigger</code> from the <code>{@link org.quartz.spi.JobStore}</code>.
     * 《如果返回值为false，那么调度器可能从JobStore重移除触发器	》
     * </p>
     */
    public boolean mayFireAgain();

    /**
     * Get the time at which the <code>Trigger</code> should occur.
     * 《	获得触发器应该发生的时间		》
     */
    public Date getStartTime();

    /**
     * Get the time at which the <code>Trigger</code> should quit repeating -
     * regardless of any remaining repeats (based on the trigger's particular 
     * repeat settings). 
     * 《获取触发器退出循环的时间--不管剩余的重复次数（基于触发器特定的重复设置）	》
     * 
     * @see #getFinalFireTime()
     */
    public Date getEndTime();

    /**
     * Returns the next time at which the <code>Trigger</code> is scheduled to fire. If
     * the trigger will not fire again, <code>null</code> will be returned.  Note that
     * the time returned can possibly be in the past, if the time that was computed
     * for the trigger to next fire has already arrived, but the scheduler has not yet
     * been able to fire the trigger (which would likely be due to lack of resources
     * e.g. threads).
     * 《	返回触发器要被下次调度触发的时间。如果触发器不再触发，将返回null。注意，返回的时间可能是在过去，如果计算过的下次触发时间已经过时，
     * 		但是调度器还没触发该触发器（像由于资源缺乏）	》
     *
     * <p>The value returned is not guaranteed to be valid until after the <code>Trigger</code>
     * has been added to the scheduler.
     * 《	返回值不能保证一定有效直到该触发器已经被添加到调度器中去		》
     * </p>
     *
     * @see TriggerUtils#computeFireTimesBetween(org.quartz.spi.OperableTrigger, Calendar, java.util.Date, java.util.Date)
     */
    public Date getNextFireTime();

    /**
     * Returns the previous time at which the <code>Trigger</code> fired.
     * If the trigger has not yet fired, <code>null</code> will be returned.
     * 《	返回触发器上次触发时间，如果没有触发过，将返回null		》
     */
    public Date getPreviousFireTime();

    /**
     * Returns the next time at which the <code>Trigger</code> will fire,
     * after the given time. If the trigger will not fire after the given time,
     * <code>null</code> will be returned.
     * 《	返回给定时间之后的触发器下一次触发时间。如果给定时间后不再触发，将返回null		》
     */
    public Date getFireTimeAfter(Date afterTime);

    /**
     * Returns the last time at which the <code>Trigger</code> will fire, if
     * the Trigger will repeat indefinitely, null will be returned.
     * 《	返回触发器最后一次触发时间，如果触发器无限重复，将返回null	》
     * <p>
     * Note that the return time *may* be in the past.《注意返回值可能是在过去》
     * </p>
     */
    public Date getFinalFireTime();

    /**
     * Get the instruction the <code>Scheduler</code> should be given for
     * handling misfire situations for this <code>Trigger</code>- the
     * concrete <code>Trigger</code> type that you are using will have
     * defined a set of additional <code>MISFIRE_INSTRUCTION_XXX</code>
     * constants that may be set as this property's value.
     * 《	获取调度器应该给予当前触发器触发失败情景的处理指令---你应该把触发失败指令集常量设置为属性值，保存在触发器具体类型中		》
     * 
     * <p>
     * If not explicitly set, the default value is <code>MISFIRE_INSTRUCTION_SMART_POLICY</code>.
     * 《	如果不明确的设置，默认值是MISFIRE_INSTRUCTION_SMART_POLICY		》
     * </p>
     * 
     * @see #MISFIRE_INSTRUCTION_SMART_POLICY
     * @see SimpleTrigger
     * @see CronTrigger
     */
    public int getMisfireInstruction();

    /**
     * Get a {@link TriggerBuilder} that is configured to produce a 
     * <code>Trigger</code> identical to this one.
     * 《	通过配置的TriggerBuilder产生一个触发器标识	》
     * 
     * @see #getScheduleBuilder()
     */
    public TriggerBuilder<? extends Trigger> getTriggerBuilder();
    
    /**
     * Get a {@link ScheduleBuilder} that is configured to produce a 
     * schedule identical to this trigger's schedule.
     * 《	这个触发器调度可通过配置的ScheduleBuilder产生一个调度标志		》
     * 
     * @see #getTriggerBuilder()
     */
    public ScheduleBuilder<? extends Trigger> getScheduleBuilder();

    /**
     * Trigger equality is based upon the equality of the TriggerKey.
     * 《	触发器相等基于触发key相等	》
     * 
     * @return true if the key of this Trigger equals that of the given Trigger.
     */
    public boolean equals(Object other);
    
    /**
     * <p>
     * Compare the next fire time of this <code>Trigger</code> to that of
     * another by comparing their keys, or in other words, sorts them
     * according to the natural (i.e. alphabetical) order of their keys.
     * 《	跟其他触发器比较下次触发时间或触发key，换句话说，根据他们key的自然（字母顺序表）顺序排序		》
     * </p>
     */
    public int compareTo(Trigger other);

    /**
     * A Comparator that compares trigger's next fire times, or in other words,
     * sorts them according to earliest next fire time.  If the fire times are
     * the same, then the triggers are sorted according to priority (highest
     * value first), if the priorities are the same, then they are sorted
     * by key.
     * 《	触发器下次触发时间比较器，换句话说根据下次最早触发时间排序。如果触发时间相同，那么触发器根据优先级（高值优先），
     * 		如果优先级相同那么比较触发key	》
     */
    class TriggerTimeComparator implements Comparator<Trigger>, Serializable {
      
        private static final long serialVersionUID = -3904243490805975570L;
        
        // This static method exists for comparator in TC clustered quartz
        public static int compare(Date nextFireTime1, int priority1, TriggerKey key1, Date nextFireTime2, int priority2, TriggerKey key2) {
            if (nextFireTime1 != null || nextFireTime2 != null) {
                if (nextFireTime1 == null) {
                    return 1;
                }

                if (nextFireTime2 == null) {
                    return -1;
                }

                if(nextFireTime1.before(nextFireTime2)) {
                    return -1;
                }

                if(nextFireTime1.after(nextFireTime2)) {
                    return 1;
                }
            }

            int comp = priority2 - priority1;
            if (comp != 0) {
                return comp;
            }

            return key1.compareTo(key2);
        }


        public int compare(Trigger t1, Trigger t2) {
            return compare(t1.getNextFireTime(), t1.getPriority(), t1.getKey(), t2.getNextFireTime(), t2.getPriority(), t2.getKey());
        }
    }
}
