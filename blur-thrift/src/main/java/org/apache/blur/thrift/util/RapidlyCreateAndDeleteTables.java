package org.apache.blur.thrift.util;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import static org.apache.blur.thrift.util.BlurThriftHelper.newColumn;
import static org.apache.blur.thrift.util.BlurThriftHelper.newRecordMutation;

import java.util.Random;
import java.util.UUID;

import org.apache.blur.thirdparty.thrift_0_9_0.TException;
import org.apache.blur.thrift.BlurClient;
import org.apache.blur.thrift.generated.Blur.Iface;
import org.apache.blur.thrift.generated.BlurException;
import org.apache.blur.thrift.generated.RowMutation;
import org.apache.blur.thrift.generated.RowMutationType;
import org.apache.blur.thrift.generated.TableDescriptor;
public class RapidlyCreateAndDeleteTables {

  public static void main(String[] args) throws BlurException, TException {
    String connectionStr = args[0];
    final String cluster = args[1];
    String uri = args[2];
    int shardCount = 1;
    Iface client = BlurClient.getClient(connectionStr);
    while (true) {
      String tableName = UUID.randomUUID().toString();
      System.out.println("Creating [" + tableName + "]");
      boolean readOnly = createTable(client, cluster, uri, shardCount, tableName);
      if (!readOnly) {
        System.out.println("Loading [" + tableName + "]");
        loadTable(client, tableName);
      }
      System.out.println("Disabling [" + tableName + "]");
      disable(client, tableName);
      System.out.println("Removing [" + tableName + "]");
      delete(client, tableName);
    }
  }

  private static void disable(Iface client, String tableName) throws BlurException, TException {
    client.disableTable(tableName);
  }

  private static void delete(Iface client, String tableName) throws BlurException, TException {
    client.removeTable(tableName, true);
  }

  private static void loadTable(Iface client, String tableName) throws BlurException, TException {
    RowMutation mutation = new RowMutation();
    mutation.table = tableName;
    mutation.waitToBeVisible = true;
    mutation.rowId = "test";
    mutation.addToRecordMutations(newRecordMutation("test", "test", newColumn("test", "test")));
    mutation.rowMutationType = RowMutationType.REPLACE_ROW;
    client.mutate(mutation);
  }

  private static boolean createTable(Iface client, final String cluster, String uri, int shardCount, String tableName) throws BlurException, TException {
    Random random = new Random();
    final TableDescriptor tableDescriptor = new TableDescriptor();
    tableDescriptor.cluster = cluster;

    tableDescriptor.name = tableName;
    tableDescriptor.readOnly = random.nextBoolean();

    tableDescriptor.shardCount = shardCount;
    tableDescriptor.tableUri = uri + "/" + tableName;

    client.createTable(tableDescriptor);

    return tableDescriptor.readOnly;
  }

}
