/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.cellar.features;

import org.apache.karaf.cellar.core.event.Event;
import org.apache.karaf.features.RepositoryEvent.EventType;

/**
 * Remote repository event.
 */
public class RemoteRepositoryEvent extends Event {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private EventType type;

    public RemoteRepositoryEvent(String id, EventType type) {
        super(id);
        this.type = type;
    }

    public EventType getType() {
        return type;
    }

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "RemoteRepositoryEvent [type=" + type + ", id=" + id
				+ ", sourceNode=" + sourceNode + ", sourceGroup=" + sourceGroup
				+ ", destination=" + destination + ", force=" + force
				+ ", postPublish=" + postPublish + "]";
	}
    
}
