package com.transferwise.common.spyql;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.util.Calendar;
import java.util.Map;

public class SpyqlCallableStatement extends SpyqlPreparedStatement implements CallableStatement {
    private CallableStatement callableStatement;

    public SpyqlCallableStatement(String sql, CallableStatement callableStatement, SpyqlConnection spyqlConnection) {
        super(sql, callableStatement, spyqlConnection);
        this.callableStatement = callableStatement;
    }

    //// Default behaviour ////

    public Array getArray(int parameterIndex) throws java.sql.SQLException {
        return this.callableStatement.getArray(parameterIndex);
    }

    public void setAsciiStream(String parameterName, InputStream x) throws java.sql.SQLException {
        this.callableStatement.setAsciiStream(parameterName, x);
    }

    public RowId getRowId(int parameterIndex) throws java.sql.SQLException {
        return this.callableStatement.getRowId(parameterIndex);
    }

    public String getString(int parameterIndex) throws java.sql.SQLException {
        return this.callableStatement.getString(parameterIndex);
    }

    public int getInt(int parameterIndex) throws java.sql.SQLException {
        return this.callableStatement.getInt(parameterIndex);
    }

    public BigDecimal getBigDecimal(String parameterName) throws java.sql.SQLException {
        return this.callableStatement.getBigDecimal(parameterName);
    }

    public String getString(String parameterName) throws java.sql.SQLException {
        return this.callableStatement.getString(parameterName);
    }

    public void setAsciiStream(String parameterName, InputStream x, int length) throws java.sql.SQLException {
        this.callableStatement.setAsciiStream(parameterName, x, length);
    }

    public long getLong(String parameterName) throws java.sql.SQLException {
        return this.callableStatement.getLong(parameterName);
    }

    public void registerOutParameter(int parameterIndex, int sqlType, int scale) throws java.sql.SQLException {
        this.callableStatement.registerOutParameter(parameterIndex, sqlType, scale);
    }

    public void setBinaryStream(String parameterName, InputStream x, long length) throws java.sql.SQLException {
        this.callableStatement.setBinaryStream(parameterName, x, length);
    }

    public void setObject(String parameterName, Object x, int targetSqlType, int scale) throws java.sql.SQLException {
        this.callableStatement.setObject(parameterName, x, targetSqlType, scale);
    }

    public void registerOutParameter(String parameterName, SQLType sqlType, String typeName) throws SQLException {
        this.callableStatement.registerOutParameter(parameterName, sqlType, typeName);
    }

    public Ref getRef(int parameterIndex) throws SQLException {
        return this.callableStatement.getRef(parameterIndex);
    }

    public Array getArray(String parameterName) throws SQLException {
        return this.callableStatement.getArray(parameterName);
    }

    public double getDouble(int parameterIndex) throws SQLException {
        return this.callableStatement.getDouble(parameterIndex);
    }

    public void setBytes(String parameterName, byte[] x) throws SQLException {
        this.callableStatement.setBytes(parameterName, x);
    }

    public void setNClob(String parameterName, NClob value) throws SQLException {
        this.callableStatement.setNClob(parameterName, value);
    }

    public byte[] getBytes(int parameterIndex) throws SQLException {
        return this.callableStatement.getBytes(parameterIndex);
    }

    public void setNCharacterStream(String parameterName, Reader value) throws SQLException {
        this.callableStatement.setNCharacterStream(parameterName, value);
    }

    public Date getDate(int parameterIndex) throws SQLException {
        return this.callableStatement.getDate(parameterIndex);
    }

    public void setInt(String parameterName, int x) throws SQLException {
        this.callableStatement.setInt(parameterName, x);
    }

    public void setTimestamp(String parameterName, Timestamp x) throws SQLException {
        this.callableStatement.setTimestamp(parameterName, x);
    }

    public Timestamp getTimestamp(String parameterName, Calendar cal) throws SQLException {
        return this.callableStatement.getTimestamp(parameterName, cal);
    }

    public void setDate(String parameterName, Date x) throws SQLException {
        this.callableStatement.setDate(parameterName, x);
    }

    public void setTime(String parameterName, Time x, Calendar cal) throws SQLException {
        this.callableStatement.setTime(parameterName, x, cal);
    }

    public Date getDate(String parameterName) throws SQLException {
        return this.callableStatement.getDate(parameterName);
    }

    public void registerOutParameter(String parameterName, int sqlType, int scale) throws SQLException {
        this.callableStatement.registerOutParameter(parameterName, sqlType, scale);
    }

    public void setObject(String parameterName, Object x) throws SQLException {
        this.callableStatement.setObject(parameterName, x);
    }

    public SQLXML getSQLXML(int parameterIndex) throws SQLException {
        return this.callableStatement.getSQLXML(parameterIndex);
    }

    public void setTime(String parameterName, Time x) throws SQLException {
        this.callableStatement.setTime(parameterName, x);
    }

    public void setDate(String parameterName, Date x, Calendar cal) throws SQLException {
        this.callableStatement.setDate(parameterName, x, cal);
    }

    public void setString(String parameterName, String x) throws SQLException {
        this.callableStatement.setString(parameterName, x);
    }

    public void setBoolean(String parameterName, boolean x) throws SQLException {
        this.callableStatement.setBoolean(parameterName, x);
    }

    public void setDouble(String parameterName, double x) throws SQLException {
        this.callableStatement.setDouble(parameterName, x);
    }

    public <T> T getObject(int parameterIndex, Class<T> type) throws SQLException {
        return this.callableStatement.getObject(parameterIndex, type);
    }

    public void setLong(String parameterName, long x) throws SQLException {
        this.callableStatement.setLong(parameterName, x);
    }

    public SQLXML getSQLXML(String parameterName) throws SQLException {
        return this.callableStatement.getSQLXML(parameterName);
    }

    public void setNCharacterStream(String parameterName, Reader value, long length) throws SQLException {
        this.callableStatement.setNCharacterStream(parameterName, value, length);
    }

    public short getShort(String parameterName) throws SQLException {
        return this.callableStatement.getShort(parameterName);
    }

    public boolean getBoolean(int parameterIndex) throws SQLException {
        return this.callableStatement.getBoolean(parameterIndex);
    }

    public void setByte(String parameterName, byte x) throws SQLException {
        this.callableStatement.setByte(parameterName, x);
    }

    public Blob getBlob(String parameterName) throws SQLException {
        return this.callableStatement.getBlob(parameterName);
    }

    public NClob getNClob(String parameterName) throws SQLException {
        return this.callableStatement.getNClob(parameterName);
    }

    public void setClob(String parameterName, Clob x) throws SQLException {
        this.callableStatement.setClob(parameterName, x);
    }

    public void setClob(String parameterName, Reader reader) throws SQLException {
        this.callableStatement.setClob(parameterName, reader);
    }

    public Time getTime(String parameterName, Calendar cal) throws SQLException {
        return this.callableStatement.getTime(parameterName, cal);
    }

    public void setCharacterStream(String parameterName, Reader reader, int length) throws SQLException {
        this.callableStatement.setCharacterStream(parameterName, reader, length);
    }

    public long getLong(int parameterIndex) throws SQLException {
        return this.callableStatement.getLong(parameterIndex);
    }

    public Timestamp getTimestamp(int parameterIndex) throws SQLException {
        return this.callableStatement.getTimestamp(parameterIndex);
    }

    public void registerOutParameter(String parameterName, SQLType sqlType) throws SQLException {
        this.callableStatement.registerOutParameter(parameterName, sqlType);
    }

    public Time getTime(int parameterIndex) throws SQLException {
        return this.callableStatement.getTime(parameterIndex);
    }

    public double getDouble(String parameterName) throws SQLException {
        return this.callableStatement.getDouble(parameterName);
    }

    public String getNString(String parameterName) throws SQLException {
        return this.callableStatement.getNString(parameterName);
    }

    public void setBlob(String parameterName, InputStream inputStream, long length) throws SQLException {
        this.callableStatement.setBlob(parameterName, inputStream, length);
    }

    public byte getByte(String parameterName) throws SQLException {
        return this.callableStatement.getByte(parameterName);
    }

    public <T> T getObject(String parameterName, Class<T> type) throws SQLException {
        return this.callableStatement.getObject(parameterName, type);
    }

    public byte getByte(int parameterIndex) throws SQLException {
        return this.callableStatement.getByte(parameterIndex);
    }

    public void setNull(String parameterName, int sqlType, String typeName) throws SQLException {
        this.callableStatement.setNull(parameterName, sqlType, typeName);
    }

    public void setClob(String parameterName, Reader reader, long length) throws SQLException {
        this.callableStatement.setClob(parameterName, reader, length);
    }

    public float getFloat(String parameterName) throws SQLException {
        return this.callableStatement.getFloat(parameterName);
    }

    public Object getObject(int parameterIndex, Map<String, Class<?>> map) throws SQLException {
        return this.callableStatement.getObject(parameterIndex, map);
    }

    public Time getTime(String parameterName) throws SQLException {
        return this.callableStatement.getTime(parameterName);
    }

    public void setObject(String parameterName, Object x, int targetSqlType) throws SQLException {
        this.callableStatement.setObject(parameterName, x, targetSqlType);
    }

    public String getNString(int parameterIndex) throws SQLException {
        return this.callableStatement.getNString(parameterIndex);
    }

    public Clob getClob(String parameterName) throws SQLException {
        return this.callableStatement.getClob(parameterName);
    }

    public void setBinaryStream(String parameterName, InputStream x) throws SQLException {
        this.callableStatement.setBinaryStream(parameterName, x);
    }

    public void registerOutParameter(String parameterName, int sqlType, String typeName) throws SQLException {
        this.callableStatement.registerOutParameter(parameterName, sqlType, typeName);
    }

    public void setTimestamp(String parameterName, Timestamp x, Calendar cal) throws SQLException {
        this.callableStatement.setTimestamp(parameterName, x, cal);
    }

    public void registerOutParameter(int parameterIndex, SQLType sqlType, int scale) throws SQLException {
        this.callableStatement.registerOutParameter(parameterIndex, sqlType, scale);
    }

    public NClob getNClob(int parameterIndex) throws SQLException {
        return this.callableStatement.getNClob(parameterIndex);
    }

    public void setRowId(String parameterName, RowId x) throws SQLException {
        this.callableStatement.setRowId(parameterName, x);
    }

    public Reader getCharacterStream(String parameterName) throws SQLException {
        return this.callableStatement.getCharacterStream(parameterName);
    }

    public void setAsciiStream(String parameterName, InputStream x, long length) throws SQLException {
        this.callableStatement.setAsciiStream(parameterName, x, length);
    }

    public Reader getNCharacterStream(String parameterName) throws SQLException {
        return this.callableStatement.getNCharacterStream(parameterName);
    }

    public Date getDate(String parameterName, Calendar cal) throws SQLException {
        return this.callableStatement.getDate(parameterName, cal);
    }

    public void setNull(String parameterName, int sqlType) throws SQLException {
        this.callableStatement.setNull(parameterName, sqlType);
    }

    public void registerOutParameter(String parameterName, int sqlType) throws SQLException {
        this.callableStatement.registerOutParameter(parameterName, sqlType);
    }

    public Timestamp getTimestamp(String parameterName) throws SQLException {
        return this.callableStatement.getTimestamp(parameterName);
    }

    public void registerOutParameter(int parameterIndex, int sqlType) throws SQLException {
        this.callableStatement.registerOutParameter(parameterIndex, sqlType);
    }

    public Clob getClob(int parameterIndex) throws SQLException {
        return this.callableStatement.getClob(parameterIndex);
    }

    public boolean wasNull() throws SQLException {
        return this.callableStatement.wasNull();
    }

    public void setNClob(String parameterName, Reader reader, long length) throws SQLException {
        this.callableStatement.setNClob(parameterName, reader, length);
    }

    public Object getObject(String parameterName, Map<String, Class<?>> map) throws SQLException {
        return this.callableStatement.getObject(parameterName, map);
    }

    public Reader getCharacterStream(int parameterIndex) throws SQLException {
        return this.callableStatement.getCharacterStream(parameterIndex);
    }

    public void setURL(String parameterName, URL val) throws SQLException {
        this.callableStatement.setURL(parameterName, val);
    }

    public void setNClob(String parameterName, Reader reader) throws SQLException {
        this.callableStatement.setNClob(parameterName, reader);
    }

    public void setCharacterStream(String parameterName, Reader reader) throws SQLException {
        this.callableStatement.setCharacterStream(parameterName, reader);
    }

    public void setBlob(String parameterName, Blob x) throws SQLException {
        this.callableStatement.setBlob(parameterName, x);
    }

    public URL getURL(String parameterName) throws SQLException {
        return this.callableStatement.getURL(parameterName);
    }

    public void setBinaryStream(String parameterName, InputStream x, int length) throws SQLException {
        this.callableStatement.setBinaryStream(parameterName, x, length);
    }

    public Object getObject(int parameterIndex) throws SQLException {
        return this.callableStatement.getObject(parameterIndex);
    }

    public void registerOutParameter(String parameterName, SQLType sqlType, int scale) throws SQLException {
        this.callableStatement.registerOutParameter(parameterName, sqlType, scale);
    }

    public void setFloat(String parameterName, float x) throws SQLException {
        this.callableStatement.setFloat(parameterName, x);
    }

    public void setBigDecimal(String parameterName, BigDecimal x) throws SQLException {
        this.callableStatement.setBigDecimal(parameterName, x);
    }

    public Blob getBlob(int parameterIndex) throws SQLException {
        return this.callableStatement.getBlob(parameterIndex);
    }

    public void registerOutParameter(int parameterIndex, SQLType sqlType, String typeName) throws SQLException {
        this.callableStatement.registerOutParameter(parameterIndex, sqlType, typeName);
    }

    public int getInt(String parameterName) throws SQLException {
        return this.callableStatement.getInt(parameterName);
    }

    public float getFloat(int parameterIndex) throws SQLException {
        return this.callableStatement.getFloat(parameterIndex);
    }

    public void setNString(String parameterName, String value) throws SQLException {
        this.callableStatement.setNString(parameterName, value);
    }

    public URL getURL(int parameterIndex) throws SQLException {
        return this.callableStatement.getURL(parameterIndex);
    }

    public Reader getNCharacterStream(int parameterIndex) throws SQLException {
        return this.callableStatement.getNCharacterStream(parameterIndex);
    }

    public boolean getBoolean(String parameterName) throws SQLException {
        return this.callableStatement.getBoolean(parameterName);
    }

    public void setSQLXML(String parameterName, SQLXML xmlObject) throws SQLException {
        this.callableStatement.setSQLXML(parameterName, xmlObject);
    }

    public BigDecimal getBigDecimal(int parameterIndex) throws SQLException {
        return this.callableStatement.getBigDecimal(parameterIndex);
    }

    public void setShort(String parameterName, short x) throws SQLException {
        this.callableStatement.setShort(parameterName, x);
    }

    public void setBlob(String parameterName, InputStream inputStream) throws SQLException {
        this.callableStatement.setBlob(parameterName, inputStream);
    }

    public Date getDate(int parameterIndex, Calendar cal) throws SQLException {
        return this.callableStatement.getDate(parameterIndex, cal);
    }

    public RowId getRowId(String parameterName) throws SQLException {
        return this.callableStatement.getRowId(parameterName);
    }

    public void setObject(String parameterName, Object x, SQLType targetSqlType, int scaleOrLength) throws SQLException {
        this.callableStatement.setObject(parameterName, x, targetSqlType, scaleOrLength);
    }

    @Deprecated
    public BigDecimal getBigDecimal(int parameterIndex, int scale) throws SQLException {
        return this.callableStatement.getBigDecimal(parameterIndex, scale);
    }

    public void setObject(String parameterName, Object x, SQLType targetSqlType) throws SQLException {
        this.callableStatement.setObject(parameterName, x, targetSqlType);
    }

    public void registerOutParameter(int parameterIndex, int sqlType, String typeName) throws SQLException {
        this.callableStatement.registerOutParameter(parameterIndex, sqlType, typeName);
    }

    public Ref getRef(String parameterName) throws SQLException {
        return this.callableStatement.getRef(parameterName);
    }

    public Object getObject(String parameterName) throws SQLException {
        return this.callableStatement.getObject(parameterName);
    }

    public Time getTime(int parameterIndex, Calendar cal) throws SQLException {
        return this.callableStatement.getTime(parameterIndex, cal);
    }

    public void setCharacterStream(String parameterName, Reader reader, long length) throws SQLException {
        this.callableStatement.setCharacterStream(parameterName, reader, length);
    }

    public short getShort(int parameterIndex) throws SQLException {
        return this.callableStatement.getShort(parameterIndex);
    }

    public byte[] getBytes(String parameterName) throws SQLException {
        return this.callableStatement.getBytes(parameterName);
    }

    public Timestamp getTimestamp(int parameterIndex, Calendar cal) throws SQLException {
        return this.callableStatement.getTimestamp(parameterIndex, cal);
    }

    public void registerOutParameter(int parameterIndex, SQLType sqlType) throws SQLException {
        this.callableStatement.registerOutParameter(parameterIndex, sqlType);
    }
}
