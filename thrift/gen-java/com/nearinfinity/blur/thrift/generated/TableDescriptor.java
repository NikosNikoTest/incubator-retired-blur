/**
 * Autogenerated by Thrift
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 */
package com.nearinfinity.blur.thrift.generated;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.EnumMap;
import java.util.Set;
import java.util.HashSet;
import java.util.EnumSet;
import java.util.Collections;
import java.util.BitSet;
import java.nio.ByteBuffer;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.thrift.*;
import org.apache.thrift.async.*;
import org.apache.thrift.meta_data.*;
import org.apache.thrift.transport.*;
import org.apache.thrift.protocol.*;

public class TableDescriptor implements TBase<TableDescriptor, TableDescriptor._Fields>, java.io.Serializable, Cloneable {
  private static final TStruct STRUCT_DESC = new TStruct("TableDescriptor");

  private static final TField IS_ENABLED_FIELD_DESC = new TField("isEnabled", TType.BOOL, (short)1);
  private static final TField ANALYZER_DEF_FIELD_DESC = new TField("analyzerDef", TType.STRING, (short)2);
  private static final TField SHARD_NAMES_FIELD_DESC = new TField("shardNames", TType.LIST, (short)3);

  public boolean isEnabled;
  public String analyzerDef;
  public List<String> shardNames;

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  public enum _Fields implements TFieldIdEnum {
    IS_ENABLED((short)1, "isEnabled"),
    ANALYZER_DEF((short)2, "analyzerDef"),
    SHARD_NAMES((short)3, "shardNames");

    private static final Map<String, _Fields> byName = new HashMap<String, _Fields>();

    static {
      for (_Fields field : EnumSet.allOf(_Fields.class)) {
        byName.put(field.getFieldName(), field);
      }
    }

    /**
     * Find the _Fields constant that matches fieldId, or null if its not found.
     */
    public static _Fields findByThriftId(int fieldId) {
      switch(fieldId) {
        case 1: // IS_ENABLED
          return IS_ENABLED;
        case 2: // ANALYZER_DEF
          return ANALYZER_DEF;
        case 3: // SHARD_NAMES
          return SHARD_NAMES;
        default:
          return null;
      }
    }

    /**
     * Find the _Fields constant that matches fieldId, throwing an exception
     * if it is not found.
     */
    public static _Fields findByThriftIdOrThrow(int fieldId) {
      _Fields fields = findByThriftId(fieldId);
      if (fields == null) throw new IllegalArgumentException("Field " + fieldId + " doesn't exist!");
      return fields;
    }

    /**
     * Find the _Fields constant that matches name, or null if its not found.
     */
    public static _Fields findByName(String name) {
      return byName.get(name);
    }

    private final short _thriftId;
    private final String _fieldName;

    _Fields(short thriftId, String fieldName) {
      _thriftId = thriftId;
      _fieldName = fieldName;
    }

    public short getThriftFieldId() {
      return _thriftId;
    }

    public String getFieldName() {
      return _fieldName;
    }
  }

  // isset id assignments
  private static final int __ISENABLED_ISSET_ID = 0;
  private BitSet __isset_bit_vector = new BitSet(1);

  public static final Map<_Fields, FieldMetaData> metaDataMap;
  static {
    Map<_Fields, FieldMetaData> tmpMap = new EnumMap<_Fields, FieldMetaData>(_Fields.class);
    tmpMap.put(_Fields.IS_ENABLED, new FieldMetaData("isEnabled", TFieldRequirementType.DEFAULT, 
        new FieldValueMetaData(TType.BOOL)));
    tmpMap.put(_Fields.ANALYZER_DEF, new FieldMetaData("analyzerDef", TFieldRequirementType.DEFAULT, 
        new FieldValueMetaData(TType.STRING)));
    tmpMap.put(_Fields.SHARD_NAMES, new FieldMetaData("shardNames", TFieldRequirementType.DEFAULT, 
        new ListMetaData(TType.LIST, 
            new FieldValueMetaData(TType.STRING))));
    metaDataMap = Collections.unmodifiableMap(tmpMap);
    FieldMetaData.addStructMetaDataMap(TableDescriptor.class, metaDataMap);
  }

  public TableDescriptor() {
  }

  public TableDescriptor(
    boolean isEnabled,
    String analyzerDef,
    List<String> shardNames)
  {
    this();
    this.isEnabled = isEnabled;
    setIsEnabledIsSet(true);
    this.analyzerDef = analyzerDef;
    this.shardNames = shardNames;
  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public TableDescriptor(TableDescriptor other) {
    __isset_bit_vector.clear();
    __isset_bit_vector.or(other.__isset_bit_vector);
    this.isEnabled = other.isEnabled;
    if (other.isSetAnalyzerDef()) {
      this.analyzerDef = other.analyzerDef;
    }
    if (other.isSetShardNames()) {
      List<String> __this__shardNames = new ArrayList<String>();
      for (String other_element : other.shardNames) {
        __this__shardNames.add(other_element);
      }
      this.shardNames = __this__shardNames;
    }
  }

  public TableDescriptor deepCopy() {
    return new TableDescriptor(this);
  }

  @Override
  public void clear() {
    setIsEnabledIsSet(false);
    this.isEnabled = false;
    this.analyzerDef = null;
    this.shardNames = null;
  }

  public boolean isIsEnabled() {
    return this.isEnabled;
  }

  public TableDescriptor setIsEnabled(boolean isEnabled) {
    this.isEnabled = isEnabled;
    setIsEnabledIsSet(true);
    return this;
  }

  public void unsetIsEnabled() {
    __isset_bit_vector.clear(__ISENABLED_ISSET_ID);
  }

  /** Returns true if field isEnabled is set (has been asigned a value) and false otherwise */
  public boolean isSetIsEnabled() {
    return __isset_bit_vector.get(__ISENABLED_ISSET_ID);
  }

  public void setIsEnabledIsSet(boolean value) {
    __isset_bit_vector.set(__ISENABLED_ISSET_ID, value);
  }

  public String getAnalyzerDef() {
    return this.analyzerDef;
  }

  public TableDescriptor setAnalyzerDef(String analyzerDef) {
    this.analyzerDef = analyzerDef;
    return this;
  }

  public void unsetAnalyzerDef() {
    this.analyzerDef = null;
  }

  /** Returns true if field analyzerDef is set (has been asigned a value) and false otherwise */
  public boolean isSetAnalyzerDef() {
    return this.analyzerDef != null;
  }

  public void setAnalyzerDefIsSet(boolean value) {
    if (!value) {
      this.analyzerDef = null;
    }
  }

  public int getShardNamesSize() {
    return (this.shardNames == null) ? 0 : this.shardNames.size();
  }

  public java.util.Iterator<String> getShardNamesIterator() {
    return (this.shardNames == null) ? null : this.shardNames.iterator();
  }

  public void addToShardNames(String elem) {
    if (this.shardNames == null) {
      this.shardNames = new ArrayList<String>();
    }
    this.shardNames.add(elem);
  }

  public List<String> getShardNames() {
    return this.shardNames;
  }

  public TableDescriptor setShardNames(List<String> shardNames) {
    this.shardNames = shardNames;
    return this;
  }

  public void unsetShardNames() {
    this.shardNames = null;
  }

  /** Returns true if field shardNames is set (has been asigned a value) and false otherwise */
  public boolean isSetShardNames() {
    return this.shardNames != null;
  }

  public void setShardNamesIsSet(boolean value) {
    if (!value) {
      this.shardNames = null;
    }
  }

  public void setFieldValue(_Fields field, Object value) {
    switch (field) {
    case IS_ENABLED:
      if (value == null) {
        unsetIsEnabled();
      } else {
        setIsEnabled((Boolean)value);
      }
      break;

    case ANALYZER_DEF:
      if (value == null) {
        unsetAnalyzerDef();
      } else {
        setAnalyzerDef((String)value);
      }
      break;

    case SHARD_NAMES:
      if (value == null) {
        unsetShardNames();
      } else {
        setShardNames((List<String>)value);
      }
      break;

    }
  }

  public Object getFieldValue(_Fields field) {
    switch (field) {
    case IS_ENABLED:
      return new Boolean(isIsEnabled());

    case ANALYZER_DEF:
      return getAnalyzerDef();

    case SHARD_NAMES:
      return getShardNames();

    }
    throw new IllegalStateException();
  }

  /** Returns true if field corresponding to fieldID is set (has been asigned a value) and false otherwise */
  public boolean isSet(_Fields field) {
    if (field == null) {
      throw new IllegalArgumentException();
    }

    switch (field) {
    case IS_ENABLED:
      return isSetIsEnabled();
    case ANALYZER_DEF:
      return isSetAnalyzerDef();
    case SHARD_NAMES:
      return isSetShardNames();
    }
    throw new IllegalStateException();
  }

  @Override
  public boolean equals(Object that) {
    if (that == null)
      return false;
    if (that instanceof TableDescriptor)
      return this.equals((TableDescriptor)that);
    return false;
  }

  public boolean equals(TableDescriptor that) {
    if (that == null)
      return false;

    boolean this_present_isEnabled = true;
    boolean that_present_isEnabled = true;
    if (this_present_isEnabled || that_present_isEnabled) {
      if (!(this_present_isEnabled && that_present_isEnabled))
        return false;
      if (this.isEnabled != that.isEnabled)
        return false;
    }

    boolean this_present_analyzerDef = true && this.isSetAnalyzerDef();
    boolean that_present_analyzerDef = true && that.isSetAnalyzerDef();
    if (this_present_analyzerDef || that_present_analyzerDef) {
      if (!(this_present_analyzerDef && that_present_analyzerDef))
        return false;
      if (!this.analyzerDef.equals(that.analyzerDef))
        return false;
    }

    boolean this_present_shardNames = true && this.isSetShardNames();
    boolean that_present_shardNames = true && that.isSetShardNames();
    if (this_present_shardNames || that_present_shardNames) {
      if (!(this_present_shardNames && that_present_shardNames))
        return false;
      if (!this.shardNames.equals(that.shardNames))
        return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    return 0;
  }

  public int compareTo(TableDescriptor other) {
    if (!getClass().equals(other.getClass())) {
      return getClass().getName().compareTo(other.getClass().getName());
    }

    int lastComparison = 0;
    TableDescriptor typedOther = (TableDescriptor)other;

    lastComparison = Boolean.valueOf(isSetIsEnabled()).compareTo(typedOther.isSetIsEnabled());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetIsEnabled()) {
      lastComparison = TBaseHelper.compareTo(this.isEnabled, typedOther.isEnabled);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetAnalyzerDef()).compareTo(typedOther.isSetAnalyzerDef());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetAnalyzerDef()) {
      lastComparison = TBaseHelper.compareTo(this.analyzerDef, typedOther.analyzerDef);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetShardNames()).compareTo(typedOther.isSetShardNames());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetShardNames()) {
      lastComparison = TBaseHelper.compareTo(this.shardNames, typedOther.shardNames);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    return 0;
  }

  public _Fields fieldForId(int fieldId) {
    return _Fields.findByThriftId(fieldId);
  }

  public void read(TProtocol iprot) throws TException {
    TField field;
    iprot.readStructBegin();
    while (true)
    {
      field = iprot.readFieldBegin();
      if (field.type == TType.STOP) { 
        break;
      }
      switch (field.id) {
        case 1: // IS_ENABLED
          if (field.type == TType.BOOL) {
            this.isEnabled = iprot.readBool();
            setIsEnabledIsSet(true);
          } else { 
            TProtocolUtil.skip(iprot, field.type);
          }
          break;
        case 2: // ANALYZER_DEF
          if (field.type == TType.STRING) {
            this.analyzerDef = iprot.readString();
          } else { 
            TProtocolUtil.skip(iprot, field.type);
          }
          break;
        case 3: // SHARD_NAMES
          if (field.type == TType.LIST) {
            {
              TList _list21 = iprot.readListBegin();
              this.shardNames = new ArrayList<String>(_list21.size);
              for (int _i22 = 0; _i22 < _list21.size; ++_i22)
              {
                String _elem23;
                _elem23 = iprot.readString();
                this.shardNames.add(_elem23);
              }
              iprot.readListEnd();
            }
          } else { 
            TProtocolUtil.skip(iprot, field.type);
          }
          break;
        default:
          TProtocolUtil.skip(iprot, field.type);
      }
      iprot.readFieldEnd();
    }
    iprot.readStructEnd();

    // check for required fields of primitive type, which can't be checked in the validate method
    validate();
  }

  public void write(TProtocol oprot) throws TException {
    validate();

    oprot.writeStructBegin(STRUCT_DESC);
    oprot.writeFieldBegin(IS_ENABLED_FIELD_DESC);
    oprot.writeBool(this.isEnabled);
    oprot.writeFieldEnd();
    if (this.analyzerDef != null) {
      oprot.writeFieldBegin(ANALYZER_DEF_FIELD_DESC);
      oprot.writeString(this.analyzerDef);
      oprot.writeFieldEnd();
    }
    if (this.shardNames != null) {
      oprot.writeFieldBegin(SHARD_NAMES_FIELD_DESC);
      {
        oprot.writeListBegin(new TList(TType.STRING, this.shardNames.size()));
        for (String _iter24 : this.shardNames)
        {
          oprot.writeString(_iter24);
        }
        oprot.writeListEnd();
      }
      oprot.writeFieldEnd();
    }
    oprot.writeFieldStop();
    oprot.writeStructEnd();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("TableDescriptor(");
    boolean first = true;

    sb.append("isEnabled:");
    sb.append(this.isEnabled);
    first = false;
    if (!first) sb.append(", ");
    sb.append("analyzerDef:");
    if (this.analyzerDef == null) {
      sb.append("null");
    } else {
      sb.append(this.analyzerDef);
    }
    first = false;
    if (!first) sb.append(", ");
    sb.append("shardNames:");
    if (this.shardNames == null) {
      sb.append("null");
    } else {
      sb.append(this.shardNames);
    }
    first = false;
    sb.append(")");
    return sb.toString();
  }

  public void validate() throws TException {
    // check for required fields
  }

}

