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
package org.spearal.jpa2;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManagerFactory;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.SingularAttribute;

import org.spearal.SpearalContext;
import org.spearal.SpearalFactory;
import org.spearal.impl.property.SimpleUnfilterablePropertiesProvider;
import org.spearal.jpa2.descriptor.EntityDescriptorFactory;

/**
 * @author William DRAI
 */
public class SpearalConfigurator {

	public static void init(SpearalFactory spearalFactory, EntityManagerFactory entityManagerFactory) {
		SpearalContext context = spearalFactory.getContext();

		Set<Class<?>> entityClasses = new HashSet<Class<?>>();
		for (ManagedType<?> managedType : entityManagerFactory.getMetamodel().getManagedTypes()) {
			List<String> unfilterablePropertiesList = new ArrayList<String>();
			for (SingularAttribute<?, ?> attribute : managedType.getSingularAttributes()) {
				if (attribute.isId() || attribute.isVersion())
					unfilterablePropertiesList.add(attribute.getName());
			}
			String[] unfilterableProperties = unfilterablePropertiesList.toArray(new String[unfilterablePropertiesList.size()]);
			
			Class<?> entityClass = managedType.getJavaType();
			context.configure(new SimpleUnfilterablePropertiesProvider(entityClass, unfilterableProperties));
			entityClasses.add(entityClass);
		}
		context.configure(new EntityDescriptorFactory(entityClasses));
	}	
}
