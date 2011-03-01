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

public class BlurUpdate {

  public interface Iface extends BlurSearch.Iface {

    public void update(List<RowMutation> mutations) throws BlurException, TException;

  }

  public interface AsyncIface extends BlurSearch .AsyncIface {

    public void update(List<RowMutation> mutations, AsyncMethodCallback<AsyncClient.update_call> resultHandler) throws TException;

  }

  public static class Client extends BlurSearch.Client implements TServiceClient, Iface {
    public static class Factory implements TServiceClientFactory<Client> {
      public Factory() {}
      public Client getClient(TProtocol prot) {
        return new Client(prot);
      }
      public Client getClient(TProtocol iprot, TProtocol oprot) {
        return new Client(iprot, oprot);
      }
    }

    public Client(TProtocol prot)
    {
      this(prot, prot);
    }

    public Client(TProtocol iprot, TProtocol oprot)
    {
      super(iprot, oprot);
    }

    public void update(List<RowMutation> mutations) throws BlurException, TException
    {
      send_update(mutations);
      recv_update();
    }

    public void send_update(List<RowMutation> mutations) throws TException
    {
      oprot_.writeMessageBegin(new TMessage("update", TMessageType.CALL, ++seqid_));
      update_args args = new update_args();
      args.setMutations(mutations);
      args.write(oprot_);
      oprot_.writeMessageEnd();
      oprot_.getTransport().flush();
    }

    public void recv_update() throws BlurException, TException
    {
      TMessage msg = iprot_.readMessageBegin();
      if (msg.type == TMessageType.EXCEPTION) {
        TApplicationException x = TApplicationException.read(iprot_);
        iprot_.readMessageEnd();
        throw x;
      }
      if (msg.seqid != seqid_) {
        throw new TApplicationException(TApplicationException.BAD_SEQUENCE_ID, "update failed: out of sequence response");
      }
      update_result result = new update_result();
      result.read(iprot_);
      iprot_.readMessageEnd();
      if (result.ex != null) {
        throw result.ex;
      }
      return;
    }

  }
  public static class AsyncClient extends BlurSearch.AsyncClient implements AsyncIface {
    public static class Factory implements TAsyncClientFactory<AsyncClient> {
      private TAsyncClientManager clientManager;
      private TProtocolFactory protocolFactory;
      public Factory(TAsyncClientManager clientManager, TProtocolFactory protocolFactory) {
        this.clientManager = clientManager;
        this.protocolFactory = protocolFactory;
      }
      public AsyncClient getAsyncClient(TNonblockingTransport transport) {
        return new AsyncClient(protocolFactory, clientManager, transport);
      }
    }

    public AsyncClient(TProtocolFactory protocolFactory, TAsyncClientManager clientManager, TNonblockingTransport transport) {
      super(protocolFactory, clientManager, transport);
    }

    public void update(List<RowMutation> mutations, AsyncMethodCallback<update_call> resultHandler) throws TException {
      checkReady();
      update_call method_call = new update_call(mutations, resultHandler, this, protocolFactory, transport);
      manager.call(method_call);
    }

    public static class update_call extends TAsyncMethodCall {
      private List<RowMutation> mutations;
      public update_call(List<RowMutation> mutations, AsyncMethodCallback<update_call> resultHandler, TAsyncClient client, TProtocolFactory protocolFactory, TNonblockingTransport transport) throws TException {
        super(client, protocolFactory, transport, resultHandler, false);
        this.mutations = mutations;
      }

      public void write_args(TProtocol prot) throws TException {
        prot.writeMessageBegin(new TMessage("update", TMessageType.CALL, 0));
        update_args args = new update_args();
        args.setMutations(mutations);
        args.write(prot);
        prot.writeMessageEnd();
      }

      public void getResult() throws BlurException, TException {
        if (getState() != State.RESPONSE_READ) {
          throw new IllegalStateException("Method call not finished!");
        }
        TMemoryInputTransport memoryTransport = new TMemoryInputTransport(getFrameBuffer().array());
        TProtocol prot = client.getProtocolFactory().getProtocol(memoryTransport);
        (new Client(prot)).recv_update();
      }
    }

  }

  public static class Processor extends BlurSearch.Processor implements TProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(Processor.class.getName());
    public Processor(Iface iface)
    {
      super(iface);
      iface_ = iface;
      processMap_.put("update", new update());
    }

    private Iface iface_;

    public boolean process(TProtocol iprot, TProtocol oprot) throws TException
    {
      TMessage msg = iprot.readMessageBegin();
      ProcessFunction fn = processMap_.get(msg.name);
      if (fn == null) {
        TProtocolUtil.skip(iprot, TType.STRUCT);
        iprot.readMessageEnd();
        TApplicationException x = new TApplicationException(TApplicationException.UNKNOWN_METHOD, "Invalid method name: '"+msg.name+"'");
        oprot.writeMessageBegin(new TMessage(msg.name, TMessageType.EXCEPTION, msg.seqid));
        x.write(oprot);
        oprot.writeMessageEnd();
        oprot.getTransport().flush();
        return true;
      }
      fn.process(msg.seqid, iprot, oprot);
      return true;
    }

    private class update implements ProcessFunction {
      public void process(int seqid, TProtocol iprot, TProtocol oprot) throws TException
      {
        update_args args = new update_args();
        try {
          args.read(iprot);
        } catch (TProtocolException e) {
          iprot.readMessageEnd();
          TApplicationException x = new TApplicationException(TApplicationException.PROTOCOL_ERROR, e.getMessage());
          oprot.writeMessageBegin(new TMessage("update", TMessageType.EXCEPTION, seqid));
          x.write(oprot);
          oprot.writeMessageEnd();
          oprot.getTransport().flush();
          return;
        }
        iprot.readMessageEnd();
        update_result result = new update_result();
        try {
          iface_.update(args.mutations);
        } catch (BlurException ex) {
          result.ex = ex;
        } catch (Throwable th) {
          LOGGER.error("Internal error processing update", th);
          TApplicationException x = new TApplicationException(TApplicationException.INTERNAL_ERROR, "Internal error processing update");
          oprot.writeMessageBegin(new TMessage("update", TMessageType.EXCEPTION, seqid));
          x.write(oprot);
          oprot.writeMessageEnd();
          oprot.getTransport().flush();
          return;
        }
        oprot.writeMessageBegin(new TMessage("update", TMessageType.REPLY, seqid));
        result.write(oprot);
        oprot.writeMessageEnd();
        oprot.getTransport().flush();
      }

    }

  }

  public static class update_args implements TBase<update_args, update_args._Fields>, java.io.Serializable, Cloneable   {
    private static final TStruct STRUCT_DESC = new TStruct("update_args");

    private static final TField MUTATIONS_FIELD_DESC = new TField("mutations", TType.LIST, (short)1);

    public List<RowMutation> mutations;

    /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
    public enum _Fields implements TFieldIdEnum {
      MUTATIONS((short)1, "mutations");

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
          case 1: // MUTATIONS
            return MUTATIONS;
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

    public static final Map<_Fields, FieldMetaData> metaDataMap;
    static {
      Map<_Fields, FieldMetaData> tmpMap = new EnumMap<_Fields, FieldMetaData>(_Fields.class);
      tmpMap.put(_Fields.MUTATIONS, new FieldMetaData("mutations", TFieldRequirementType.DEFAULT, 
          new ListMetaData(TType.LIST, 
              new StructMetaData(TType.STRUCT, RowMutation.class))));
      metaDataMap = Collections.unmodifiableMap(tmpMap);
      FieldMetaData.addStructMetaDataMap(update_args.class, metaDataMap);
    }

    public update_args() {
    }

    public update_args(
      List<RowMutation> mutations)
    {
      this();
      this.mutations = mutations;
    }

    /**
     * Performs a deep copy on <i>other</i>.
     */
    public update_args(update_args other) {
      if (other.isSetMutations()) {
        List<RowMutation> __this__mutations = new ArrayList<RowMutation>();
        for (RowMutation other_element : other.mutations) {
          __this__mutations.add(new RowMutation(other_element));
        }
        this.mutations = __this__mutations;
      }
    }

    public update_args deepCopy() {
      return new update_args(this);
    }

    @Override
    public void clear() {
      this.mutations = null;
    }

    public int getMutationsSize() {
      return (this.mutations == null) ? 0 : this.mutations.size();
    }

    public java.util.Iterator<RowMutation> getMutationsIterator() {
      return (this.mutations == null) ? null : this.mutations.iterator();
    }

    public void addToMutations(RowMutation elem) {
      if (this.mutations == null) {
        this.mutations = new ArrayList<RowMutation>();
      }
      this.mutations.add(elem);
    }

    public List<RowMutation> getMutations() {
      return this.mutations;
    }

    public update_args setMutations(List<RowMutation> mutations) {
      this.mutations = mutations;
      return this;
    }

    public void unsetMutations() {
      this.mutations = null;
    }

    /** Returns true if field mutations is set (has been asigned a value) and false otherwise */
    public boolean isSetMutations() {
      return this.mutations != null;
    }

    public void setMutationsIsSet(boolean value) {
      if (!value) {
        this.mutations = null;
      }
    }

    public void setFieldValue(_Fields field, Object value) {
      switch (field) {
      case MUTATIONS:
        if (value == null) {
          unsetMutations();
        } else {
          setMutations((List<RowMutation>)value);
        }
        break;

      }
    }

    public Object getFieldValue(_Fields field) {
      switch (field) {
      case MUTATIONS:
        return getMutations();

      }
      throw new IllegalStateException();
    }

    /** Returns true if field corresponding to fieldID is set (has been asigned a value) and false otherwise */
    public boolean isSet(_Fields field) {
      if (field == null) {
        throw new IllegalArgumentException();
      }

      switch (field) {
      case MUTATIONS:
        return isSetMutations();
      }
      throw new IllegalStateException();
    }

    @Override
    public boolean equals(Object that) {
      if (that == null)
        return false;
      if (that instanceof update_args)
        return this.equals((update_args)that);
      return false;
    }

    public boolean equals(update_args that) {
      if (that == null)
        return false;

      boolean this_present_mutations = true && this.isSetMutations();
      boolean that_present_mutations = true && that.isSetMutations();
      if (this_present_mutations || that_present_mutations) {
        if (!(this_present_mutations && that_present_mutations))
          return false;
        if (!this.mutations.equals(that.mutations))
          return false;
      }

      return true;
    }

    @Override
    public int hashCode() {
      return 0;
    }

    public int compareTo(update_args other) {
      if (!getClass().equals(other.getClass())) {
        return getClass().getName().compareTo(other.getClass().getName());
      }

      int lastComparison = 0;
      update_args typedOther = (update_args)other;

      lastComparison = Boolean.valueOf(isSetMutations()).compareTo(typedOther.isSetMutations());
      if (lastComparison != 0) {
        return lastComparison;
      }
      if (isSetMutations()) {
        lastComparison = TBaseHelper.compareTo(this.mutations, typedOther.mutations);
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
          case 1: // MUTATIONS
            if (field.type == TType.LIST) {
              {
                TList _list101 = iprot.readListBegin();
                this.mutations = new ArrayList<RowMutation>(_list101.size);
                for (int _i102 = 0; _i102 < _list101.size; ++_i102)
                {
                  RowMutation _elem103;
                  _elem103 = new RowMutation();
                  _elem103.read(iprot);
                  this.mutations.add(_elem103);
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
      if (this.mutations != null) {
        oprot.writeFieldBegin(MUTATIONS_FIELD_DESC);
        {
          oprot.writeListBegin(new TList(TType.STRUCT, this.mutations.size()));
          for (RowMutation _iter104 : this.mutations)
          {
            _iter104.write(oprot);
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
      StringBuilder sb = new StringBuilder("update_args(");
      boolean first = true;

      sb.append("mutations:");
      if (this.mutations == null) {
        sb.append("null");
      } else {
        sb.append(this.mutations);
      }
      first = false;
      sb.append(")");
      return sb.toString();
    }

    public void validate() throws TException {
      // check for required fields
    }

  }

  public static class update_result implements TBase<update_result, update_result._Fields>, java.io.Serializable, Cloneable   {
    private static final TStruct STRUCT_DESC = new TStruct("update_result");

    private static final TField EX_FIELD_DESC = new TField("ex", TType.STRUCT, (short)1);

    public BlurException ex;

    /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
    public enum _Fields implements TFieldIdEnum {
      EX((short)1, "ex");

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
          case 1: // EX
            return EX;
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

    public static final Map<_Fields, FieldMetaData> metaDataMap;
    static {
      Map<_Fields, FieldMetaData> tmpMap = new EnumMap<_Fields, FieldMetaData>(_Fields.class);
      tmpMap.put(_Fields.EX, new FieldMetaData("ex", TFieldRequirementType.DEFAULT, 
          new FieldValueMetaData(TType.STRUCT)));
      metaDataMap = Collections.unmodifiableMap(tmpMap);
      FieldMetaData.addStructMetaDataMap(update_result.class, metaDataMap);
    }

    public update_result() {
    }

    public update_result(
      BlurException ex)
    {
      this();
      this.ex = ex;
    }

    /**
     * Performs a deep copy on <i>other</i>.
     */
    public update_result(update_result other) {
      if (other.isSetEx()) {
        this.ex = new BlurException(other.ex);
      }
    }

    public update_result deepCopy() {
      return new update_result(this);
    }

    @Override
    public void clear() {
      this.ex = null;
    }

    public BlurException getEx() {
      return this.ex;
    }

    public update_result setEx(BlurException ex) {
      this.ex = ex;
      return this;
    }

    public void unsetEx() {
      this.ex = null;
    }

    /** Returns true if field ex is set (has been asigned a value) and false otherwise */
    public boolean isSetEx() {
      return this.ex != null;
    }

    public void setExIsSet(boolean value) {
      if (!value) {
        this.ex = null;
      }
    }

    public void setFieldValue(_Fields field, Object value) {
      switch (field) {
      case EX:
        if (value == null) {
          unsetEx();
        } else {
          setEx((BlurException)value);
        }
        break;

      }
    }

    public Object getFieldValue(_Fields field) {
      switch (field) {
      case EX:
        return getEx();

      }
      throw new IllegalStateException();
    }

    /** Returns true if field corresponding to fieldID is set (has been asigned a value) and false otherwise */
    public boolean isSet(_Fields field) {
      if (field == null) {
        throw new IllegalArgumentException();
      }

      switch (field) {
      case EX:
        return isSetEx();
      }
      throw new IllegalStateException();
    }

    @Override
    public boolean equals(Object that) {
      if (that == null)
        return false;
      if (that instanceof update_result)
        return this.equals((update_result)that);
      return false;
    }

    public boolean equals(update_result that) {
      if (that == null)
        return false;

      boolean this_present_ex = true && this.isSetEx();
      boolean that_present_ex = true && that.isSetEx();
      if (this_present_ex || that_present_ex) {
        if (!(this_present_ex && that_present_ex))
          return false;
        if (!this.ex.equals(that.ex))
          return false;
      }

      return true;
    }

    @Override
    public int hashCode() {
      return 0;
    }

    public int compareTo(update_result other) {
      if (!getClass().equals(other.getClass())) {
        return getClass().getName().compareTo(other.getClass().getName());
      }

      int lastComparison = 0;
      update_result typedOther = (update_result)other;

      lastComparison = Boolean.valueOf(isSetEx()).compareTo(typedOther.isSetEx());
      if (lastComparison != 0) {
        return lastComparison;
      }
      if (isSetEx()) {
        lastComparison = TBaseHelper.compareTo(this.ex, typedOther.ex);
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
          case 1: // EX
            if (field.type == TType.STRUCT) {
              this.ex = new BlurException();
              this.ex.read(iprot);
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
      oprot.writeStructBegin(STRUCT_DESC);

      if (this.isSetEx()) {
        oprot.writeFieldBegin(EX_FIELD_DESC);
        this.ex.write(oprot);
        oprot.writeFieldEnd();
      }
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder("update_result(");
      boolean first = true;

      sb.append("ex:");
      if (this.ex == null) {
        sb.append("null");
      } else {
        sb.append(this.ex);
      }
      first = false;
      sb.append(")");
      return sb.toString();
    }

    public void validate() throws TException {
      // check for required fields
    }

  }

}
