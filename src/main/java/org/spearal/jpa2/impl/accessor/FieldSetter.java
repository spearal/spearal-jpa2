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
package org.spearal.jpa2.impl.accessor;

import java.lang.reflect.Field;

/**
 * @author Franck WOLFF
 */
public class FieldSetter implements Setter {

	private final Field field;
	
	public FieldSetter(Field field) {
		field.setAccessible(true);
		this.field = field;
	}

	@Override
	public void setValue(Object holder, Object value) {
		try {
			field.set(holder, value);
		}
		catch (Exception e) {
			throw new RuntimeException("Could not set property value with field: " + field, e);
		}
	}
}