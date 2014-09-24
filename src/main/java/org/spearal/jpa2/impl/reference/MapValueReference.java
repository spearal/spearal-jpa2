/**
 * == @Spearal ==>
 * 
 * Copyright (C) 2014 Franck WOLFF & William DRAI (http://www.spearal.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.spearal.jpa2.impl.reference;

import java.util.Map;

import org.spearal.configuration.PartialObjectFactory.PartialObjectProxy;

/**
 * @author Franck WOLFF
 */
public class MapValueReference implements Reference {
	
	private final Map<Object, Object> map;
	private final Object key;
	private final boolean partial;

	public MapValueReference(Map<Object, Object> map, Object key) {
		this.map = map;
		this.key = key;
		this.partial = (key instanceof PartialObjectProxy);
	}

	@Override
	public void set(Map<PartialObjectProxy, Object> resolved, Object value) {
		if (partial) {
			Object newKey = resolved.get(key);
			if (newKey == null)
				newKey = key;
			map.put(newKey, value);
		}
		else
			map.put(key, value);
	}
}