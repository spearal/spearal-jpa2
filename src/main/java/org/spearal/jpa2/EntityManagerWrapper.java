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

import java.util.List;
import java.util.Map;

import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.Query;
import javax.persistence.StoredProcedureQuery;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.metamodel.Metamodel;

import org.spearal.jpa2.impl.ProxyMerger;

/**
 * @author William DRAI
 */
@SuppressWarnings("rawtypes")
public class EntityManagerWrapper implements EntityManager {

	private final EntityManager entityManager;
	private final ProxyMerger proxyMerger;

	public EntityManagerWrapper(EntityManager entityManager) {
		this.entityManager = entityManager;
		this.proxyMerger = new ProxyMerger(entityManager);
	}
	
	public EntityManager getWrappedEntityManager() {
		return entityManager;
	}

	public <T> T merge(T entity) {
		if (proxyMerger.isProxy(entity))
			entity = proxyMerger.merge(entity);
		
		return entityManager.merge(entity);
	}

	public void persist(Object entity) {
		if (proxyMerger.isProxy(entity))
			proxyMerger.persist(entity);
		
		entityManager.persist(entity);
	}

	public void refresh(Object entity, Map<String, Object> properties) {
		entityManager.refresh(entity, properties);
	}

	public void refresh(Object entity, LockModeType lockMode) {
		entityManager.refresh(entity, lockMode);
	}

	public void refresh(Object entity, LockModeType lockMode, Map<String, Object> properties) {
		entityManager.refresh(entity, lockMode, properties);
	}

	public void refresh(Object entity) {
		entityManager.refresh(entity);
	}

	public void remove(Object entity) {
		entityManager.remove(entity);
	}
	
	public void lock(Object arg0, LockModeType arg1, Map<String, Object> arg2) {
		entityManager.lock(arg0, arg1, arg2);
	}

	public void lock(Object arg0, LockModeType arg1) {
		entityManager.lock(arg0, arg1);
	}

	public void clear() {
		entityManager.clear();
	}

	public void close() {
		entityManager.close();
	}

	public boolean contains(Object entity) {
		return entityManager.contains(entity);
	}

	public <T> T getReference(Class<T> arg0, Object arg1) {
		return entityManager.getReference(arg0, arg1);
	}

	public <T> EntityGraph<T> createEntityGraph(Class<T> entityClass) {
		return entityManager.createEntityGraph(entityClass);
	}

	public EntityGraph<?> createEntityGraph(String entityName) {
		return entityManager.createEntityGraph(entityName);
	}
	
	public <T> TypedQuery<T> createNamedQuery(String name, Class<T> entityClass) {
		return entityManager.createNamedQuery(name, entityClass);
	}

	public Query createNamedQuery(String name) {
		return entityManager.createNamedQuery(name);
	}

	public StoredProcedureQuery createNamedStoredProcedureQuery(String name) {
		return entityManager.createNamedStoredProcedureQuery(name);
	}

	public Query createNativeQuery(String name, Class entityClass) {
		return entityManager.createNativeQuery(name, entityClass);
	}

	public Query createNativeQuery(String name, String sql) {
		return entityManager.createNativeQuery(name, sql);
	}

	public Query createNativeQuery(String name) {
		return entityManager.createNativeQuery(name);
	}

	public Query createQuery(CriteriaDelete criteria) {
		return entityManager.createQuery(criteria);
	}

	public <T> TypedQuery<T> createQuery(CriteriaQuery<T> query) {
		return entityManager.createQuery(query);
	}

	public Query createQuery(CriteriaUpdate criteria) {
		return entityManager.createQuery(criteria);
	}

	public <T> TypedQuery<T> createQuery(String name, Class<T> entityClass) {
		return entityManager.createQuery(name, entityClass);
	}

	public Query createQuery(String arg0) {
		return entityManager.createQuery(arg0);
	}

	public StoredProcedureQuery createStoredProcedureQuery(String arg0, Class... arg1) {
		return entityManager.createStoredProcedureQuery(arg0, arg1);
	}

	public StoredProcedureQuery createStoredProcedureQuery(String arg0, String... arg1) {
		return entityManager.createStoredProcedureQuery(arg0, arg1);
	}

	public StoredProcedureQuery createStoredProcedureQuery(String arg0) {
		return entityManager.createStoredProcedureQuery(arg0);
	}

	public void detach(Object arg0) {
		entityManager.detach(arg0);
	}

	public <T> T find(Class<T> arg0, Object arg1, LockModeType arg2, Map<String, Object> arg3) {
		return entityManager.find(arg0, arg1, arg2, arg3);
	}

	public <T> T find(Class<T> arg0, Object arg1, LockModeType arg2) {
		return entityManager.find(arg0, arg1, arg2);
	}

	public <T> T find(Class<T> arg0, Object arg1, Map<String, Object> arg2) {
		return entityManager.find(arg0, arg1, arg2);
	}

	public <T> T find(Class<T> arg0, Object arg1) {
		return entityManager.find(arg0, arg1);
	}

	public void flush() {
		entityManager.flush();
	}

	public CriteriaBuilder getCriteriaBuilder() {
		return entityManager.getCriteriaBuilder();
	}

	public Object getDelegate() {
		return entityManager.getDelegate();
	}

	public EntityGraph<?> getEntityGraph(String arg0) {
		return entityManager.getEntityGraph(arg0);
	}

	public <T> List<EntityGraph<? super T>> getEntityGraphs(Class<T> arg0) {
		return entityManager.getEntityGraphs(arg0);
	}

	public EntityManagerFactory getEntityManagerFactory() {
		return entityManager.getEntityManagerFactory();
	}

	public FlushModeType getFlushMode() {
		return entityManager.getFlushMode();
	}

	public LockModeType getLockMode(Object arg0) {
		return entityManager.getLockMode(arg0);
	}

	public Metamodel getMetamodel() {
		return entityManager.getMetamodel();
	}

	public Map<String, Object> getProperties() {
		return entityManager.getProperties();
	}

	public EntityTransaction getTransaction() {
		return entityManager.getTransaction();
	}

	public boolean isJoinedToTransaction() {
		return entityManager.isJoinedToTransaction();
	}

	public boolean isOpen() {
		return entityManager.isOpen();
	}

	public void joinTransaction() {
		entityManager.joinTransaction();
	}
	
	public void setFlushMode(FlushModeType arg0) {
		entityManager.setFlushMode(arg0);
	}

	public void setProperty(String arg0, Object arg1) {
		entityManager.setProperty(arg0, arg1);
	}

	public <T> T unwrap(Class<T> arg0) {
		return entityManager.unwrap(arg0);
	}
}
