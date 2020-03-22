package com.transferwise.common.spyql;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLType;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Map;

public class SpyqlCallableStatement extends SpyqlPreparedStatement implements CallableStatement {

  private CallableStatement callableStatement;

  public SpyqlCallableStatement(String sql, CallableStatement callableStatement, SpyqlConnection spyqlConnection) {
    super(sql, callableStatement, spyqlConnection);
    this.callableStatement = callableStatement;
  }

  //// Default behaviour ////

  @Override
  public Array getArray(int parameterIndex) throws java.sql.SQLException {
    return this.callableStatement.getArray(parameterIndex);
  }

  @Override
  public Array getArray(String parameterName) throws SQLException {
    return this.callableStatement.getArray(parameterName);
  }

  @Override
  public void setAsciiStream(String parameterName, InputStream x) throws java.sql.SQLException {
    this.callableStatement.setAsciiStream(parameterName, x);
  }

  @Override
  public void setAsciiStream(String parameterName, InputStream x, long length) throws SQLException {
    this.callableStatement.setAsciiStream(parameterName, x, length);
  }

  @Override
  public void setAsciiStream(String parameterName, InputStream x, int length) throws java.sql.SQLException {
    this.callableStatement.setAsciiStream(parameterName, x, length);
  }

  @Override
  public RowId getRowId(int parameterIndex) throws java.sql.SQLException {
    return this.callableStatement.getRowId(parameterIndex);
  }

  @Override
  public RowId getRowId(String parameterName) throws SQLException {
    return this.callableStatement.getRowId(parameterName);
  }

  @Override
  public String getString(int parameterIndex) throws java.sql.SQLException {
    return this.callableStatement.getString(parameterIndex);
  }

  @Override
  public String getString(String parameterName) throws java.sql.SQLException {
    return this.callableStatement.getString(parameterName);
  }

  @Override
  public int getInt(int parameterIndex) throws java.sql.SQLException {
    return this.callableStatement.getInt(parameterIndex);
  }

  @Override
  public int getInt(String parameterName) throws SQLException {
    return this.callableStatement.getInt(parameterName);
  }

  @Override
  public BigDecimal getBigDecimal(String parameterName) throws java.sql.SQLException {
    return this.callableStatement.getBigDecimal(parameterName);
  }

  @Override
  public BigDecimal getBigDecimal(int parameterIndex) throws SQLException {
    return this.callableStatement.getBigDecimal(parameterIndex);
  }

  @Deprecated
  @Override
  public BigDecimal getBigDecimal(int parameterIndex, int scale) throws SQLException {
    return this.callableStatement.getBigDecimal(parameterIndex, scale);
  }

  @Override
  public long getLong(String parameterName) throws java.sql.SQLException {
    return this.callableStatement.getLong(parameterName);
  }

  @Override
  public long getLong(int parameterIndex) throws SQLException {
    return this.callableStatement.getLong(parameterIndex);
  }

  @Override
  public void registerOutParameter(int parameterIndex, int sqlType, int scale) throws java.sql.SQLException {
    this.callableStatement.registerOutParameter(parameterIndex, sqlType, scale);
  }

  @Override
  public void registerOutParameter(int parameterIndex, SQLType sqlType) throws SQLException {
    this.callableStatement.registerOutParameter(parameterIndex, sqlType);
  }

  @Override
  public void registerOutParameter(int parameterIndex, int sqlType, String typeName) throws SQLException {
    this.callableStatement.registerOutParameter(parameterIndex, sqlType, typeName);
  }

  @Override
  public void registerOutParameter(int parameterIndex, SQLType sqlType, String typeName) throws SQLException {
    this.callableStatement.registerOutParameter(parameterIndex, sqlType, typeName);
  }

  @Override
  public void registerOutParameter(String parameterName, SQLType sqlType, int scale) throws SQLException {
    this.callableStatement.registerOutParameter(parameterName, sqlType, scale);
  }

  @Override
  public void registerOutParameter(int parameterIndex, int sqlType) throws SQLException {
    this.callableStatement.registerOutParameter(parameterIndex, sqlType);
  }

  @Override
  public void registerOutParameter(String parameterName, int sqlType) throws SQLException {
    this.callableStatement.registerOutParameter(parameterName, sqlType);
  }

  @Override
  public void registerOutParameter(String parameterName, int sqlType, String typeName) throws SQLException {
    this.callableStatement.registerOutParameter(parameterName, sqlType, typeName);
  }

  @Override
  public void registerOutParameter(String parameterName, SQLType sqlType) throws SQLException {
    this.callableStatement.registerOutParameter(parameterName, sqlType);
  }

  @Override
  public void registerOutParameter(String parameterName, int sqlType, int scale) throws SQLException {
    this.callableStatement.registerOutParameter(parameterName, sqlType, scale);
  }

  @Override
  public void registerOutParameter(String parameterName, SQLType sqlType, String typeName) throws SQLException {
    this.callableStatement.registerOutParameter(parameterName, sqlType, typeName);
  }

  @Override
  public void registerOutParameter(int parameterIndex, SQLType sqlType, int scale) throws SQLException {
    this.callableStatement.registerOutParameter(parameterIndex, sqlType, scale);
  }

  @Override
  public void setBinaryStream(String parameterName, InputStream x, long length) throws java.sql.SQLException {
    this.callableStatement.setBinaryStream(parameterName, x, length);
  }

  @Override
  public void setBinaryStream(String parameterName, InputStream x, int length) throws SQLException {
    this.callableStatement.setBinaryStream(parameterName, x, length);
  }

  @Override
  public void setBinaryStream(String parameterName, InputStream x) throws SQLException {
    this.callableStatement.setBinaryStream(parameterName, x);
  }

  @Override
  public void setObject(String parameterName, Object x, int targetSqlType, int scale) throws java.sql.SQLException {
    this.callableStatement.setObject(parameterName, x, targetSqlType, scale);
  }

  @Override
  public void setObject(String parameterName, Object x, SQLType targetSqlType) throws SQLException {
    this.callableStatement.setObject(parameterName, x, targetSqlType);
  }

  @Override
  public void setObject(String parameterName, Object x, SQLType targetSqlType, int scaleOrLength) throws SQLException {
    this.callableStatement.setObject(parameterName, x, targetSqlType, scaleOrLength);
  }

  @Override
  public void setObject(String parameterName, Object x, int targetSqlType) throws SQLException {
    this.callableStatement.setObject(parameterName, x, targetSqlType);
  }

  @Override
  public void setObject(String parameterName, Object x) throws SQLException {
    this.callableStatement.setObject(parameterName, x);
  }

  @Override
  public Ref getRef(int parameterIndex) throws SQLException {
    return this.callableStatement.getRef(parameterIndex);
  }

  @Override
  public Ref getRef(String parameterName) throws SQLException {
    return this.callableStatement.getRef(parameterName);
  }

  @Override
  public double getDouble(int parameterIndex) throws SQLException {
    return this.callableStatement.getDouble(parameterIndex);
  }

  @Override
  public double getDouble(String parameterName) throws SQLException {
    return this.callableStatement.getDouble(parameterName);
  }

  @Override
  public void setBytes(String parameterName, byte[] x) throws SQLException {
    this.callableStatement.setBytes(parameterName, x);
  }

  @Override
  public void setNClob(String parameterName, NClob value) throws SQLException {
    this.callableStatement.setNClob(parameterName, value);
  }

  @Override
  public void setNClob(String parameterName, Reader reader, long length) throws SQLException {
    this.callableStatement.setNClob(parameterName, reader, length);
  }

  @Override
  public void setNClob(String parameterName, Reader reader) throws SQLException {
    this.callableStatement.setNClob(parameterName, reader);
  }

  @Override
  public byte[] getBytes(int parameterIndex) throws SQLException {
    return this.callableStatement.getBytes(parameterIndex);
  }

  @Override
  public byte[] getBytes(String parameterName) throws SQLException {
    return this.callableStatement.getBytes(parameterName);
  }

  @Override
  public void setNCharacterStream(String parameterName, Reader value) throws SQLException {
    this.callableStatement.setNCharacterStream(parameterName, value);
  }

  @Override
  public void setNCharacterStream(String parameterName, Reader value, long length) throws SQLException {
    this.callableStatement.setNCharacterStream(parameterName, value, length);
  }

  @Override
  public Date getDate(int parameterIndex) throws SQLException {
    return this.callableStatement.getDate(parameterIndex);
  }

  @Override
  public Date getDate(int parameterIndex, Calendar cal) throws SQLException {
    return this.callableStatement.getDate(parameterIndex, cal);
  }

  @Override
  public Date getDate(String parameterName, Calendar cal) throws SQLException {
    return this.callableStatement.getDate(parameterName, cal);
  }

  @Override
  public Date getDate(String parameterName) throws SQLException {
    return this.callableStatement.getDate(parameterName);
  }

  @Override
  public void setInt(String parameterName, int x) throws SQLException {
    this.callableStatement.setInt(parameterName, x);
  }

  @Override
  public void setTimestamp(String parameterName, Timestamp x) throws SQLException {
    this.callableStatement.setTimestamp(parameterName, x);
  }

  @Override
  public void setTimestamp(String parameterName, Timestamp x, Calendar cal) throws SQLException {
    this.callableStatement.setTimestamp(parameterName, x, cal);
  }

  @Override
  public Timestamp getTimestamp(String parameterName, Calendar cal) throws SQLException {
    return this.callableStatement.getTimestamp(parameterName, cal);
  }

  @Override
  public Timestamp getTimestamp(int parameterIndex) throws SQLException {
    return this.callableStatement.getTimestamp(parameterIndex);
  }

  @Override
  public Timestamp getTimestamp(int parameterIndex, Calendar cal) throws SQLException {
    return this.callableStatement.getTimestamp(parameterIndex, cal);
  }

  @Override
  public Timestamp getTimestamp(String parameterName) throws SQLException {
    return this.callableStatement.getTimestamp(parameterName);
  }

  @Override
  public void setDate(String parameterName, Date x) throws SQLException {
    this.callableStatement.setDate(parameterName, x);
  }

  @Override
  public void setDate(String parameterName, Date x, Calendar cal) throws SQLException {
    this.callableStatement.setDate(parameterName, x, cal);
  }

  @Override
  public void setTime(String parameterName, Time x, Calendar cal) throws SQLException {
    this.callableStatement.setTime(parameterName, x, cal);
  }

  @Override
  public void setTime(String parameterName, Time x) throws SQLException {
    this.callableStatement.setTime(parameterName, x);
  }

  @Override
  public SQLXML getSQLXML(int parameterIndex) throws SQLException {
    return this.callableStatement.getSQLXML(parameterIndex);
  }

  @Override
  public SQLXML getSQLXML(String parameterName) throws SQLException {
    return this.callableStatement.getSQLXML(parameterName);
  }

  @Override
  public void setString(String parameterName, String x) throws SQLException {
    this.callableStatement.setString(parameterName, x);
  }

  @Override
  public void setBoolean(String parameterName, boolean x) throws SQLException {
    this.callableStatement.setBoolean(parameterName, x);
  }

  @Override
  public void setDouble(String parameterName, double x) throws SQLException {
    this.callableStatement.setDouble(parameterName, x);
  }

  @Override
  public <T> T getObject(int parameterIndex, Class<T> type) throws SQLException {
    return this.callableStatement.getObject(parameterIndex, type);
  }

  @Override
  public Object getObject(String parameterName) throws SQLException {
    return this.callableStatement.getObject(parameterName);
  }

  @Override
  public Object getObject(int parameterIndex) throws SQLException {
    return this.callableStatement.getObject(parameterIndex);
  }

  @Override
  public Object getObject(String parameterName, Map<String, Class<?>> map) throws SQLException {
    return this.callableStatement.getObject(parameterName, map);
  }

  @Override
  public Object getObject(int parameterIndex, Map<String, Class<?>> map) throws SQLException {
    return this.callableStatement.getObject(parameterIndex, map);
  }

  @Override
  public <T> T getObject(String parameterName, Class<T> type) throws SQLException {
    return this.callableStatement.getObject(parameterName, type);
  }

  @Override
  public void setLong(String parameterName, long x) throws SQLException {
    this.callableStatement.setLong(parameterName, x);
  }

  @Override
  public short getShort(String parameterName) throws SQLException {
    return this.callableStatement.getShort(parameterName);
  }

  @Override
  public short getShort(int parameterIndex) throws SQLException {
    return this.callableStatement.getShort(parameterIndex);
  }

  @Override
  public boolean getBoolean(int parameterIndex) throws SQLException {
    return this.callableStatement.getBoolean(parameterIndex);
  }

  @Override
  public boolean getBoolean(String parameterName) throws SQLException {
    return this.callableStatement.getBoolean(parameterName);
  }

  @Override
  public void setByte(String parameterName, byte x) throws SQLException {
    this.callableStatement.setByte(parameterName, x);
  }

  @Override
  public Blob getBlob(String parameterName) throws SQLException {
    return this.callableStatement.getBlob(parameterName);
  }

  @Override
  public Blob getBlob(int parameterIndex) throws SQLException {
    return this.callableStatement.getBlob(parameterIndex);
  }

  @Override
  public NClob getNClob(String parameterName) throws SQLException {
    return this.callableStatement.getNClob(parameterName);
  }

  @Override
  public NClob getNClob(int parameterIndex) throws SQLException {
    return this.callableStatement.getNClob(parameterIndex);
  }

  @Override
  public void setClob(String parameterName, Clob x) throws SQLException {
    this.callableStatement.setClob(parameterName, x);
  }

  @Override
  public void setClob(String parameterName, Reader reader) throws SQLException {
    this.callableStatement.setClob(parameterName, reader);
  }

  @Override
  public void setClob(String parameterName, Reader reader, long length) throws SQLException {
    this.callableStatement.setClob(parameterName, reader, length);
  }

  @Override
  public Time getTime(String parameterName, Calendar cal) throws SQLException {
    return this.callableStatement.getTime(parameterName, cal);
  }

  @Override
  public Time getTime(int parameterIndex, Calendar cal) throws SQLException {
    return this.callableStatement.getTime(parameterIndex, cal);
  }

  @Override
  public Time getTime(String parameterName) throws SQLException {
    return this.callableStatement.getTime(parameterName);
  }

  @Override
  public Time getTime(int parameterIndex) throws SQLException {
    return this.callableStatement.getTime(parameterIndex);
  }

  @Override
  public void setCharacterStream(String parameterName, Reader reader, int length) throws SQLException {
    this.callableStatement.setCharacterStream(parameterName, reader, length);
  }

  @Override
  public void setCharacterStream(String parameterName, Reader reader, long length) throws SQLException {
    this.callableStatement.setCharacterStream(parameterName, reader, length);
  }

  @Override
  public void setCharacterStream(String parameterName, Reader reader) throws SQLException {
    this.callableStatement.setCharacterStream(parameterName, reader);
  }

  @Override
  public String getNString(String parameterName) throws SQLException {
    return this.callableStatement.getNString(parameterName);
  }

  @Override
  public String getNString(int parameterIndex) throws SQLException {
    return this.callableStatement.getNString(parameterIndex);
  }

  @Override
  public void setBlob(String parameterName, InputStream inputStream, long length) throws SQLException {
    this.callableStatement.setBlob(parameterName, inputStream, length);
  }

  @Override
  public void setBlob(String parameterName, InputStream inputStream) throws SQLException {
    this.callableStatement.setBlob(parameterName, inputStream);
  }

  @Override
  public void setBlob(String parameterName, Blob x) throws SQLException {
    this.callableStatement.setBlob(parameterName, x);
  }

  @Override
  public byte getByte(String parameterName) throws SQLException {
    return this.callableStatement.getByte(parameterName);
  }

  @Override
  public byte getByte(int parameterIndex) throws SQLException {
    return this.callableStatement.getByte(parameterIndex);
  }

  @Override
  public void setNull(String parameterName, int sqlType, String typeName) throws SQLException {
    this.callableStatement.setNull(parameterName, sqlType, typeName);
  }

  @Override
  public void setNull(String parameterName, int sqlType) throws SQLException {
    this.callableStatement.setNull(parameterName, sqlType);
  }

  @Override
  public float getFloat(String parameterName) throws SQLException {
    return this.callableStatement.getFloat(parameterName);
  }

  @Override
  public float getFloat(int parameterIndex) throws SQLException {
    return this.callableStatement.getFloat(parameterIndex);
  }

  @Override
  public Clob getClob(String parameterName) throws SQLException {
    return this.callableStatement.getClob(parameterName);
  }

  @Override
  public Clob getClob(int parameterIndex) throws SQLException {
    return this.callableStatement.getClob(parameterIndex);
  }

  @Override
  public void setRowId(String parameterName, RowId x) throws SQLException {
    this.callableStatement.setRowId(parameterName, x);
  }

  @Override
  public Reader getCharacterStream(String parameterName) throws SQLException {
    return this.callableStatement.getCharacterStream(parameterName);
  }

  @Override
  public Reader getCharacterStream(int parameterIndex) throws SQLException {
    return this.callableStatement.getCharacterStream(parameterIndex);
  }

  @Override
  public Reader getNCharacterStream(String parameterName) throws SQLException {
    return this.callableStatement.getNCharacterStream(parameterName);
  }

  @Override
  public Reader getNCharacterStream(int parameterIndex) throws SQLException {
    return this.callableStatement.getNCharacterStream(parameterIndex);
  }

  @Override
  public boolean wasNull() throws SQLException {
    return this.callableStatement.wasNull();
  }

  @Override
  public void setURL(String parameterName, URL val) throws SQLException {
    this.callableStatement.setURL(parameterName, val);
  }

  @Override
  public URL getURL(String parameterName) throws SQLException {
    return this.callableStatement.getURL(parameterName);
  }

  @Override
  public URL getURL(int parameterIndex) throws SQLException {
    return this.callableStatement.getURL(parameterIndex);
  }

  @Override
  public void setFloat(String parameterName, float x) throws SQLException {
    this.callableStatement.setFloat(parameterName, x);
  }

  @Override
  public void setBigDecimal(String parameterName, BigDecimal x) throws SQLException {
    this.callableStatement.setBigDecimal(parameterName, x);
  }

  @Override
  public void setNString(String parameterName, String value) throws SQLException {
    this.callableStatement.setNString(parameterName, value);
  }

  @Override
  public void setSQLXML(String parameterName, SQLXML xmlObject) throws SQLException {
    this.callableStatement.setSQLXML(parameterName, xmlObject);
  }

  @Override
  public void setShort(String parameterName, short x) throws SQLException {
    this.callableStatement.setShort(parameterName, x);
  }
}
