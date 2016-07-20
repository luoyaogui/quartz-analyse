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

package oracle.sql;

import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Blob;
import java.sql.SQLException;

/**
--java.sql.Blob
The representation (mapping) in the JavaTM programming language of an SQL BLOB value. An SQL BLOB is a built-in type that stores a Binary Large Object as a column value in a row of a database table. By default drivers implement Blob using an SQL locator(BLOB), which means that a Blob object contains a logical pointer to the SQL BLOB data rather than the data itself. A Blob object is valid for the duration of the transaction in which is was created. 
Methods in the interfaces ResultSet, CallableStatement, and PreparedStatement, such as getBlob and setBlob allow a programmer to access an SQL BLOB value. The Blob interface provides methods for getting the length of an SQL BLOB (Binary Large Object) value, for materializing a BLOB value on the client, and for determining the position of a pattern of bytes within a BLOB value. In addition, this interface has methods for updating a BLOB value. 
All methods on the Blob interface must be fully implemented if the JDBC driver supports the data type.

《在javaTM编程语言中一个SQL二进制大对象的值的表示，一个SQL BLOB是数据库表中行的存储二进制大对象的列值的一种内建类型。默认驱动程序使用一个SQL实现Blob定位器(Blob)，这意味着一个Blod对象包含了逻辑指针指向了SQL BLOB对象而不是数据本身。一个Blob对象在已创建事务的整个时期都是可用的。
接口中的ResultSet, CallableStatement, and PreparedStatement的这些方法，例如getBlob和setBlob允许编程人员访问一个SQL BLOB值，Blob接口提供了获取SQL BLOB（二进制大对象）值长度、在客户端显示BLOB值、决定BLOB值的模式位置的方法，另外接口有更新BLOB值的方法。
对JDBC驱动支持的数据类型，Blob的所有方法必须完全被实现。》
 */
public class BLOB implements Blob {

  public long length() throws SQLException {
    return 0;
  }

  public byte[] getBytes(long pos, int length) throws SQLException {
    return null;
  }

  public InputStream getBinaryStream() throws SQLException {
    return null;
  }

  public long position(byte[] pattern, long start) throws SQLException {
    return 0;
  }

  public long position(Blob pattern, long start) throws SQLException {
    return 0;
  }

  public int setBytes(long pos, byte[] bytes) throws SQLException {
    return 0;
  }

  public int setBytes(long pos, byte[] bytes, int offset, int len) throws SQLException {
    return 0;
  }

  public OutputStream setBinaryStream(long pos) throws SQLException {
    return null;
  }

  public void truncate(long len) throws SQLException {
    //
  }

  public void free() throws SQLException {
    //
  }

  public InputStream getBinaryStream(long pos, long length) throws SQLException {
    return null;
  }

  public int putBytes(long pos, byte[] data) throws SQLException {
    return 0;
  }

  public void trim(long length) throws SQLException {
    //
  }

}
