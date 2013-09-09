# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements. See the NOTICE file distributed with this
# work for additional information regarding copyright ownership. The ASF
# licenses this file to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations
# under the License.
require 'blur_thrift_client'

class BlurQuery < ActiveRecord::Base
  include ActionView::Helpers::NumberHelper
  belongs_to :blur_table
  has_one :cluster, :through => :blur_table
  has_one :zookeeper, :through => :cluster

  def cancel
    begin
      ActiveSupport::Notifications.instrument "cancel.blur", :urls => blur_table.zookeeper.blur_urls, :table => self.blur_table.table_name, :uuid => self.uuid do
        BlurThriftClient.client(blur_table.zookeeper.blur_urls).cancelQuery self.blur_table.table_name, self.uuid
      end
      return true
    rescue Exception => e
      logger.error "Exception in BlurQueries.cancel:"
      logger.error e
      return false
    end
  end

  def state_str
    case read_attribute(:state)
      when 0 then "Running"
      when 1 then "Interrupted"
      when 2 then "Complete"
      when 3 then "Marked Complete by Agent"
      else nil
    end
  end

  def complete
    if self.total_shards == 0
      0
    else
      self.complete_shards / self.total_shards.to_f
    end
  end

  def summary(user)
    summary_hash =
    {
      :id => id,
      :can_update => user.can?(:cancel, :blur_queries),
      :userid => print_value(userid),
      :query => print_value(query_string),
      :tablename => print_value(blur_table.table_name),
      :start => print_value(start, 0),
      :time => created_at.getlocal.strftime('%r'),
      :status => summary_state,
      :state => state_str
    }
    summary_hash.delete(:query) if user.cannot?(:index, :blur_queries, :query_string)
    summary_hash
  end

  private

  def summary_state
    if state == 0
      formattedNumber = "%01d" % (100 * complete)
      formattedNumber + '%'
    elsif state == 1
      "(Interrupted) - #{number_to_percentage(100 * complete, :precision => 0)}"
    elsif state == 3
      "Marked Complete by Agent"
    else
      "Complete"
    end
  end

  def print_value(conditional, default_message = "Not Available")
    return default_message unless conditional
    return conditional unless block_given?
    yield
  end
end
