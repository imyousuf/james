/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.nntpserver.mock;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.james.nntpserver.repository.NNTPArticle;
import org.apache.james.nntpserver.repository.NNTPGroup;
import org.apache.james.nntpserver.repository.NNTPRepository;

public class MockNNTPRepository implements NNTPRepository{

	private boolean readOnly;
	private final Map<String,NNTPGroup> groups = new HashMap<String,NNTPGroup>();
	private final Map<Date,NNTPGroup> dates = new HashMap<Date, NNTPGroup>();
	
	public void createArticle(InputStream in) {
		// TODO Auto-generated method stub
		
	}

	public NNTPArticle getArticleFromID(String id) {
		// TODO Auto-generated method stub
		return null;
	}

	public Iterator<NNTPArticle> getArticlesSince(Date dt) {
		// TODO Auto-generated method stub
		return null;
	}

	public NNTPGroup getGroup(String groupName) {
		return groups.get(groupName);
	}

	public Iterator<NNTPGroup> getGroupsSince(Date dt) {
		List<NNTPGroup> gList = new ArrayList<NNTPGroup>();
		Iterator<Date> dIt = dates.keySet().iterator();
		while (dIt.hasNext()) {
			Date d = dIt.next();
			if (dt != null && dt.after(d)) {
				gList.add(dates.get(d));
			}
		}
		
		return gList.iterator();
	}

	public Iterator<NNTPGroup> getMatchedGroups(String wildmat) {
		// TODO Auto-generated method stub
		return null;
	}

	public String[] getOverviewFormat() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isReadOnly() {
		return readOnly;
	}

	public void setReadOnly(boolean readOnly) {
	    this.readOnly = readOnly;	
	}
	
	public void addGroup(Date date, NNTPGroup group) {
		groups.put(group.getName(),group);
		dates.put(date,group);
	}
}
