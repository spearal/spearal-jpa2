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
import org.spearal.jpa2.impl.accessor.Setter;

/**
 * @author Franck WOLFF
 */
public class BeanPropertyReference implements Reference {
	
	private final Object holder;
	private final Setter setter;
	private final boolean partial;

	public BeanPropertyReference(Object holder, Setter setter) {
		this.holder = holder;
		this.setter = setter;
		this.partial = (holder instanceof PartialObjectProxy);
	}
	
	public void set(Map<PartialObjectProxy, Object> resolved, Object value) {
		if (partial) {
			Object newHolder = resolved.get(holder);
			if (newHolder == null)
				newHolder = holder;
			setter.setValue(newHolder, value);
		}
		else
			setter.setValue(holder, value);
	}
}