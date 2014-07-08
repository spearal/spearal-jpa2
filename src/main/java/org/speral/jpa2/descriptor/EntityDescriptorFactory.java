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
package org.speral.jpa2.descriptor;

import javax.persistence.Entity;
import javax.persistence.Persistence;
import javax.persistence.PersistenceUtil;

import org.spearal.SpearalContext;
import org.spearal.SpearalEncoder;
import org.spearal.configuration.EncoderBeanDescriptorFactory;
import org.spearal.configuration.PropertyFactory.Property;
import org.spearal.impl.util.ClassDescriptionUtil;

/**
 * @author Franck WOLFF
 */
public class EntityDescriptorFactory implements EncoderBeanDescriptorFactory {
	
	public static class EntityDescriptor implements EncoderBeanDescriptor {

		private final String description;
		private final Property[] properties;
		
		public EntityDescriptor(String description, Property[] properties) {
			this.description = description;
			this.properties = properties;
		}

		@Override
		public String getDescription() {
			return description;
		}

		@Override
		public Property[] getProperties() {
			return properties;
		}

		@Override
		public boolean isCacheable() {
			return false;
		}
	}

	private final PersistenceUtil persistenceUtil;
	
	public EntityDescriptorFactory() {
		persistenceUtil = Persistence.getPersistenceUtil();
		if (persistenceUtil == null)
			throw new NullPointerException("Could not get PersistenceUtil");
	}
	
	@Override
	public EncoderBeanDescriptor createDescription(SpearalEncoder encoder, Object value) {
		Class<?> type = value.getClass();
		if (!type.isAnnotationPresent(Entity.class))
			return null;
		
		SpearalContext context = encoder.getContext();
		Property[] properties = encoder.getPropertyFilter().get(type);
		boolean cloned = false;
		for (int i = 0; i < properties.length; i++) {
			try {
				Object propertyValue = properties[i].get(value);
				if (!persistenceUtil.isLoaded(propertyValue)) {
					if (!cloned) {
						properties = properties.clone();
						cloned = true;
					}
					properties[i] = null;
				}
			}
			catch (Exception e) {
				throw new RuntimeException("Could not get property value for: " + properties[i], e);
			}
			
		}
		
		String description = ClassDescriptionUtil.createAliasedDescription(context, type, properties);
		return new EntityDescriptor(description, properties);
	}
}
