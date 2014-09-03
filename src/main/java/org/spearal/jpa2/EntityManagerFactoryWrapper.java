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
import java.util.Map;
import java.util.Set;

import javax.persistence.Cache;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnitUtil;
import javax.persistence.Query;
import javax.persistence.SynchronizationType;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.Metamodel;
import javax.persistence.metamodel.SingularAttribute;

import org.spearal.SpearalFactory;
import org.spearal.jpa2.descriptor.EntityDescriptorFactory;
import org.spearal.impl.property.SimpleUnfilterablePropertiesProvider;

/**
 * @author William DRAI
 */
@SuppressWarnings("rawtypes")
public class EntityManagerFactoryWrapper implements EntityManagerFactory {

	private final EntityManagerFactory entityManagerFactory;

	public EntityManagerFactoryWrapper(EntityManagerFactory entityManagerFactory, SpearalFactory spearalFactory) {
		this.entityManagerFactory = entityManagerFactory;
		initSpearalFactory(spearalFactory);
	}
	
	private void initSpearalFactory(SpearalFactory spearalFactory) {
		Set<Class<?>> entityClasses = new HashSet<Class<?>>();
		
		for (ManagedType<?> managedType : getMetamodel().getManagedTypes()) {
			List<String> unfilterablePropertiesList = new ArrayList<String>();
			for (SingularAttribute<?, ?> attribute : managedType.getSingularAttributes()) {
				if (attribute.isId())
					unfilterablePropertiesList.add(attribute.getName());
				if (attribute.isVersion())
					unfilterablePropertiesList.add(attribute.getName());
			}
			
			Class<?> entityClass = managedType.getJavaType();
			
			String[] unfilterableProperties = unfilterablePropertiesList.toArray(new String[unfilterablePropertiesList.size()]);
			spearalFactory.getContext().configure(new SimpleUnfilterablePropertiesProvider(entityClass, unfilterableProperties));
			
			entityClasses.add(entityClass);
		}
		
		spearalFactory.getContext().configure(new EntityDescriptorFactory(entityClasses));
	}	

	public EntityManagerFactory getWrappedEntityManagerFactory() {
		return entityManagerFactory;
	}

	public EntityManager createEntityManager() {
		EntityManager entityManager = entityManagerFactory.createEntityManager();
		return new EntityManagerWrapper(entityManager);
	}

	public EntityManager createEntityManager(Map params) {
		EntityManager entityManager = entityManagerFactory.createEntityManager(params);
		return new EntityManagerWrapper(entityManager);
	}

	public EntityManager createEntityManager(SynchronizationType syncType, Map params) {
		EntityManager entityManager = entityManagerFactory.createEntityManager(syncType, params);
		return new EntityManagerWrapper(entityManager);
	}

	public EntityManager createEntityManager(SynchronizationType syncType) {
		EntityManager entityManager = entityManagerFactory.createEntityManager(syncType);
		return new EntityManagerWrapper(entityManager);
	}
	
	public <T> void addNamedEntityGraph(String name, EntityGraph<T> graph) {
		entityManagerFactory.addNamedEntityGraph(name, graph);
	}

	public void addNamedQuery(String name, Query query) {
		entityManagerFactory.addNamedQuery(name, query);
	}

	public void close() {
		entityManagerFactory.close();
	}

	public Cache getCache() {
		return entityManagerFactory.getCache();
	}

	public CriteriaBuilder getCriteriaBuilder() {
		return entityManagerFactory.getCriteriaBuilder();
	}

	public Metamodel getMetamodel() {
		return entityManagerFactory.getMetamodel();
	}

	public PersistenceUnitUtil getPersistenceUnitUtil() {
		return entityManagerFactory.getPersistenceUnitUtil();
	}

	public Map<String, Object> getProperties() {
		return entityManagerFactory.getProperties();
	}

	public boolean isOpen() {
		return entityManagerFactory.isOpen();
	}

	public <T> T unwrap(Class<T> clazz) {
		return entityManagerFactory.unwrap(clazz);
	}
}
