/* 
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved. 
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
 
package org.quartz.examples.example10;

import static org.quartz.DateBuilder.evenMinuteDate;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

import java.util.Date;

import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.SchedulerMetaData;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This example will spawn a large number of jobs to run
 * 
 * @author James House, Bill Kratzer
 */
public class PlugInExample {

  public void run() throws Exception {
    Logger log = LoggerFactory.getLogger(PlugInExample.class);

    // First we must get a reference to a scheduler
    SchedulerFactory sf = new StdSchedulerFactory();
    Scheduler sched = null;
    try {
      sched = sf.getScheduler();
    } catch (NoClassDefFoundError e) {
      log.error(" Unable to load a class - most likely you do not have jta.jar on the classpath. If not present in the examples/lib folder, please " +
                "add it there for this sample to run.", e);
      return;
    }

    System.out.println("------- Initialization Complete -----------");

    System.out.println("------- (Not Scheduling any Jobs - relying on XML definitions --");

    System.out.println("------- Starting Scheduler ----------------");

    // start the schedule
    sched.start();
    
    JobDetail job = newJob(SimpleJob.class).withIdentity("job1", "group1").build();
    Trigger trigger = newTrigger().withIdentity("trigger1", "group1").withSchedule(simpleSchedule().withIntervalInSeconds(10).withRepeatCount(10)).build();
    sched.scheduleJob(job, trigger);

    System.out.println("------- Started Scheduler -----------------");

    System.out.println("------- Waiting five minutes... -----------");

    // wait five minutes to give our jobs a chance to run
    try {
      Thread.sleep(300L * 1000L);
    } catch (Exception e) {
      //
    }

    // shut down the scheduler
    System.out.println("------- Shutting Down ---------------------");
    sched.shutdown(true);
    System.out.println("------- Shutdown Complete -----------------");

    SchedulerMetaData metaData = sched.getMetaData();
    System.out.println("Executed " + metaData.getNumberOfJobsExecuted() + " jobs.");
  }

  public static void main(String[] args) throws Exception {

    PlugInExample example = new PlugInExample();
    example.run();
    
  }

}
