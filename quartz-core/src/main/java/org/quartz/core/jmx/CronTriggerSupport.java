package org.quartz.core.jmx;

import static javax.management.openmbean.SimpleType.STRING;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;

import org.quartz.CronTrigger;
import org.quartz.impl.triggers.CronTriggerImpl;
import org.quartz.spi.OperableTrigger;


/**
 * @author luoyaogui
具体查JMX规范------Java Management Extensions
 软件包 javax.management 的描述 提供 Java Management Extensions 的核心类。 Java Management Extensions (JMXTM) API 是一个用于管理和监视的标准 API。
典型用途包括： 查询并更改应用程序配置 累积有关应用程序行为的统计并使其可用 通知状态更改及错误状况。 JMX API 还可以作为解决方案的一部分来管理系统、网络等。 API 包括远程访问，因此，
远程管理程序可以基于这些目的与正在运行的应用程序交互。
 */
public class CronTriggerSupport {
    private static final String COMPOSITE_TYPE_NAME = "CronTrigger";
    private static final String COMPOSITE_TYPE_DESCRIPTION = "CronTrigger Details";
    private static final String[] ITEM_NAMES = new String[] { "expression", "timeZone" };
    private static final String[] ITEM_DESCRIPTIONS = new String[] { "expression", "timeZone" };
    private static final OpenType[] ITEM_TYPES = new OpenType[] { STRING, STRING };
    private static final CompositeType COMPOSITE_TYPE;
    private static final String TABULAR_TYPE_NAME = "CronTrigger collection";
    private static final String TABULAR_TYPE_DESCRIPTION = "CronTrigger collection";
    private static final TabularType TABULAR_TYPE;

    static {
        try {
            COMPOSITE_TYPE = new CompositeType(COMPOSITE_TYPE_NAME,
                    COMPOSITE_TYPE_DESCRIPTION, getItemNames(), getItemDescriptions(),
                    getItemTypes());
            TABULAR_TYPE = new TabularType(TABULAR_TYPE_NAME,
                    TABULAR_TYPE_DESCRIPTION, COMPOSITE_TYPE, getItemNames());
        } catch (OpenDataException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static String[] getItemNames() {
        List<String> l = new ArrayList<String>(Arrays.asList(ITEM_NAMES));
        l.addAll(Arrays.asList(TriggerSupport.getItemNames()));
        return l.toArray(new String[l.size()]);
    }

    public static String[] getItemDescriptions() {
        List<String> l = new ArrayList<String>(Arrays.asList(ITEM_DESCRIPTIONS));
        l.addAll(Arrays.asList(TriggerSupport.getItemDescriptions()));
        return l.toArray(new String[l.size()]);
    }
    
    public static OpenType[] getItemTypes() {
        List<OpenType> l = new ArrayList<OpenType>(Arrays.asList(ITEM_TYPES));
        l.addAll(Arrays.asList(TriggerSupport.getItemTypes()));
        return l.toArray(new OpenType[l.size()]);
    }
    
    public static CompositeData toCompositeData(CronTrigger trigger) {
        try {
            return new CompositeDataSupport(COMPOSITE_TYPE, ITEM_NAMES,
                    new Object[] {
                            trigger.getCronExpression(),
                            trigger.getTimeZone(),
                            trigger.getKey().getName(),
                            trigger.getKey().getGroup(),
                            trigger.getJobKey().getName(),
                            trigger.getJobKey().getGroup(),
                            trigger.getDescription(),
                            JobDataMapSupport.toTabularData(trigger
                                    .getJobDataMap()),
                            trigger.getCalendarName(),
                            ((OperableTrigger)trigger).getFireInstanceId(),
                            trigger.getMisfireInstruction(),
                            trigger.getPriority(), trigger.getStartTime(),
                            trigger.getEndTime(), trigger.getNextFireTime(),
                            trigger.getPreviousFireTime(),
                            trigger.getFinalFireTime() });
        } catch (OpenDataException e) {
            throw new RuntimeException(e);
        }
    }

    public static TabularData toTabularData(List<? extends CronTrigger> triggers) {
        TabularData tData = new TabularDataSupport(TABULAR_TYPE);
        if (triggers != null) {
            ArrayList<CompositeData> list = new ArrayList<CompositeData>();
            for (CronTrigger trigger : triggers) {
                list.add(toCompositeData(trigger));
            }
            tData.putAll(list.toArray(new CompositeData[list.size()]));
        }
        return tData;
    }
    
    public static OperableTrigger newTrigger(CompositeData cData) throws ParseException {
        CronTriggerImpl result = new CronTriggerImpl();
        result.setCronExpression((String) cData.get("cronExpression"));
        if(cData.containsKey("timeZone")) {
            result.setTimeZone(TimeZone.getTimeZone((String)cData.get("timeZone")));
        }
        TriggerSupport.initializeTrigger(result, cData);
        return result;
    }

    public static OperableTrigger newTrigger(Map<String, Object> attrMap) throws ParseException {
        CronTriggerImpl result = new CronTriggerImpl();
        result.setCronExpression((String) attrMap.get("cronExpression"));
        if(attrMap.containsKey("timeZone")) {
            result.setTimeZone(TimeZone.getTimeZone((String)attrMap.get("timeZone")));
        }
        TriggerSupport.initializeTrigger(result, attrMap);
        return result;
    }
}
