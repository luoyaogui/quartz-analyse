package org.quartz.spi;

/**
 * 保存jobstore中的执行时数据和异常信息
 * @author lorban
 */
public class TriggerFiredResult {

  private TriggerFiredBundle triggerFiredBundle;//用于QuartzSchedulerThread的从jobstore中获取的返回执行时数据的一个简单类

  private Exception exception;

  public TriggerFiredResult(TriggerFiredBundle triggerFiredBundle) {
    this.triggerFiredBundle = triggerFiredBundle;
  }

  public TriggerFiredResult(Exception exception) {
    this.exception = exception;
  }

  public TriggerFiredBundle getTriggerFiredBundle() {
    return triggerFiredBundle;
  }

  public Exception getException() {
    return exception;
  }
}
