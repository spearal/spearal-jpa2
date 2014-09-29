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
package org.spearal.jpa2.descriptor;

import java.util.Set;

import javax.persistence.Persistence;
import javax.persistence.PersistenceUtil;

import org.spearal.SpearalContext;
import org.spearal.SpearalPropertyFilter;
import org.spearal.configuration.FilteredBeanDescriptorFactory;
import org.spearal.configuration.PropertyFactory.Property;
import org.spearal.impl.util.ClassDescriptionUtil;

/**
 * @author Franck WOLFF
 * @author William DRAI
 */
public class EntityDescriptorFactory implements FilteredBeanDescriptorFactory {
	
	private final Set<Class<?>> entityClasses;
		
	private final PersistenceUtil persistenceUtil;
	
	public EntityDescriptorFactory(Set<Class<?>> entityClasses) {
		this.entityClasses = entityClasses;
		
		persistenceUtil = Persistence.getPersistenceUtil();
		if (persistenceUtil == null)
			throw new NullPointerException("Could not get PersistenceUtil");
	}
	
	public static class EntityDescriptor implements FilteredBeanDescriptor {
		
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

	@Override
	public FilteredBeanDescriptor createDescription(SpearalContext context, SpearalPropertyFilter filter, Object value) {
		Class<?> type = value.getClass();
		if (!entityClasses.contains(type))
			return null;
		
		Property[] properties = filter.get(type);
		boolean cloned = false;
		for (int i = 0; i < properties.length; i++) {
			if (properties[i] == null)
				continue;
			if (!persistenceUtil.isLoaded(value, properties[i].getName())) {
				if (!cloned) {
					properties = properties.clone();
					cloned = true;
				}
				properties[i] = null;
			}
		}
		
		String description = ClassDescriptionUtil.createAliasedDescription(context, type, properties);
		return new EntityDescriptor(description, properties);
	}
}
