
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

package org.quartz.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.spi.SchedulerSignaler;

/**
 * An interface to be used by <code>JobStore</code> instances in order to
 * communicate signals back to the <code>QuartzScheduler</code>.
 * 《 JobStore实例用于跟QuartzScheduler交换通信信号 》
 * 
 * @author jhouse
 */
public class SchedulerSignalerImpl implements SchedulerSignaler {

    Logger log = LoggerFactory.getLogger(SchedulerSignalerImpl.class);
    
    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Data members.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    protected QuartzScheduler sched;
    protected QuartzSchedulerThread schedThread;

    /**
     * 《 JobStore实例用于跟QuartzScheduler交换通信信号 》
     * 创建一个信号器，关联于调度器（QuartzScheduler）和调度线程（QuartzSchedulerThread）
     */
    public SchedulerSignalerImpl(QuartzScheduler sched, QuartzSchedulerThread schedThread) {
        this.sched = sched;
        this.schedThread = schedThread;
        
        log.info("Initialized Scheduler Signaller of type: " + getClass());
    }

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Interface.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    public void notifyTriggerListenersMisfired(Trigger trigger) {
        try {
            sched.notifyTriggerListenersMisfired(trigger);
        } catch (SchedulerException se) {
            sched.getLog().error(
                    "Error notifying listeners of trigger misfire.", se);
            sched.notifySchedulerListenersError(
                    "Error notifying listeners of trigger misfire.", se);
        }
    }

    public void notifySchedulerListenersFinalized(Trigger trigger) {
        sched.notifySchedulerListenersFinalized(trigger);
    }
    /**
     * 调用调度器执行线程的相关方法
     */
    public void signalSchedulingChange(long candidateNewNextFireTime) {
        schedThread.signalSchedulingChange(candidateNewNextFireTime);
    }

    public void notifySchedulerListenersJobDeleted(JobKey jobKey) {
        sched.notifySchedulerListenersJobDeleted(jobKey);
    }

    public void notifySchedulerListenersError(String string, SchedulerException jpe) {
        sched.notifySchedulerListenersError(string, jpe);
    }
}
