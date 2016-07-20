
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

import java.util.Collection;

/**
 * Provides a mechanism for obtaining client-usable handles to <code>Scheduler</code>
 * instances.
 * 提供一种包含客户端可用的<code>Scheduler</code>实例句柄的机制
 * 
 * @see Scheduler
 * @see org.quartz.impl.StdSchedulerFactory
 * 
 * @author James House
 */
public interface SchedulerFactory {

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Interface.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    /**
     * <p>
     * Returns a client-usable handle to a <code>Scheduler</code>.返回一个客户端可用的Scheduler句柄
     * </p>
     * 
     * @throws SchedulerException
     *           if there is a problem with the underlying <code>Scheduler</code>.
     * 如果scheduler出现潜在的错误将抛出SchedulerException异常
     */
    Scheduler getScheduler() throws SchedulerException;

    /**
     * <p>
     * Returns a handle to the Scheduler with the given name, if it exists.返回一个指定名称的Scheduler句柄
     * </p>
     */
    Scheduler getScheduler(String schedName) throws SchedulerException;

    /**
     * <p>
     * Returns handles to all known Schedulers (made by any SchedulerFactory
     * within this jvm.).返回所有已知schedulers句柄（在这个JVM中被任何SchedulerFactory创建的）
     * </p>
     */
    Collection<Scheduler> getAllSchedulers() throws SchedulerException;

}
