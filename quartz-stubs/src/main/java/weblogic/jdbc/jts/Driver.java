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

package weblogic.jdbc.jts;

import java.util.logging.Logger;
import java.sql.Connection;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.Properties;
/**
java.sql.Driver
The interface that every driver class must implement. 
The Java SQL framework allows for multiple database drivers. 
Each driver should supply a class that implements the Driver interface. 
The DriverManager will try to load as many drivers as it can find and then for any given connection request, it will ask each driver in turn to try to connect to the target URL. 
It is strongly recommended that each Driver class should be small and standalone so that the Driver class can be loaded and queried without bringing in vast quantities of supporting code. 
When a Driver class is loaded, it should create an instance of itself and register it with the DriverManager. This means that a user can load and register a driver by calling 
  Class.forName("foo.bah.Driver")
《每一个驱动类必须实现这个接口。
Java SQL框架允许多个数据库驱动。
每一个驱动应该提供一个类实现Driver接口。
驱动管理在发现可用驱动和任何给定链接请求事将会尽可能多的加载驱动，他将会轮流尝试连接目标url。
强烈建议每个驱动类应该小巧的和独立的以至于驱动类加载时无需携带大量辅助的代码。
当一个驱动类加载成功后，它应该创建一个自身的实例并注册到驱动管理中去，这意味着一个用户可以通过Class.forName("foo.bah.Driver")来加载和注册一个驱动类。》
*/
public class Driver implements java.sql.Driver {

  public Connection connect(String url, Properties info) throws SQLException {
    return null;
  }

  public boolean acceptsURL(String url) throws SQLException {
    return false;
  }

  public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
    return null;
  }

  public int getMajorVersion() {
    return 0;
  }

  public int getMinorVersion() {
    return 0;
  }

  public boolean jdbcCompliant() {
    return false;
  }

  public Logger getParentLogger() {
    return null;
  }
}
