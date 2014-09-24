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

import org.spearal.jpa2.impl.PartialEntityResolver;

/**
 * @author William DRAI
 */
public class EntityManagerWrapper implements EntityManager {

	private final EntityManager entityManager;
	private final PartialEntityResolver partialEntityResolver;

	public EntityManagerWrapper(EntityManager entityManager) {
		this.entityManager = entityManager;
		this.partialEntityResolver = new PartialEntityResolver(entityManager);
	}
	
	public EntityManager getWrappedEntityManager() {
		return entityManager;
	}

	public <T> T merge(T entity) {
		entity = partialEntityResolver.resolve(entity);
		return entityManager.merge(entity);
	}

	public void persist(Object entity) {
		entity = partialEntityResolver.resolve(entity);
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
	
	public void lock(Object entity, LockModeType lockMode, Map<String, Object> properties) {
		entityManager.lock(entity, lockMode, properties);
	}

	public void lock(Object entity, LockModeType lockMode) {
		entityManager.lock(entity, lockMode);
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

	public <T> T getReference(Class<T> entityClass, Object primaryKey) {
		return entityManager.getReference(entityClass, primaryKey);
	}

	public <T> EntityGraph<T> createEntityGraph(Class<T> rootType) {
		return entityManager.createEntityGraph(rootType);
	}

	public EntityGraph<?> createEntityGraph(String graphName) {
		return entityManager.createEntityGraph(graphName);
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

	@SuppressWarnings("rawtypes")
	public Query createNativeQuery(String sqlString, Class resultClass) {
		return entityManager.createNativeQuery(sqlString, resultClass);
	}

	public Query createNativeQuery(String sqlString, String resultSetMapping) {
		return entityManager.createNativeQuery(sqlString, resultSetMapping);
	}

	public Query createNativeQuery(String sqlString) {
		return entityManager.createNativeQuery(sqlString);
	}

	@SuppressWarnings("rawtypes")
	public Query createQuery(CriteriaDelete deleteQuery) {
		return entityManager.createQuery(deleteQuery);
	}

	public <T> TypedQuery<T> createQuery(CriteriaQuery<T> criteriaQuery) {
		return entityManager.createQuery(criteriaQuery);
	}

	@SuppressWarnings("rawtypes")
	public Query createQuery(CriteriaUpdate updateQuery) {
		return entityManager.createQuery(updateQuery);
	}

	public <T> TypedQuery<T> createQuery(String qlString, Class<T> resultClass) {
		return entityManager.createQuery(qlString, resultClass);
	}

	public Query createQuery(String qlString) {
		return entityManager.createQuery(qlString);
	}

	@SuppressWarnings("rawtypes")
	public StoredProcedureQuery createStoredProcedureQuery(String procedureName, Class... resultClasses) {
		return entityManager.createStoredProcedureQuery(procedureName, resultClasses);
	}

	public StoredProcedureQuery createStoredProcedureQuery(String procedureName, String... resultSetMappings) {
		return entityManager.createStoredProcedureQuery(procedureName, resultSetMappings);
	}

	public StoredProcedureQuery createStoredProcedureQuery(String procedureName) {
		return entityManager.createStoredProcedureQuery(procedureName);
	}

	public void detach(Object entity) {
		entityManager.detach(entity);
	}

	public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode, Map<String,Object> properties) {
		return entityManager.find(entityClass, primaryKey, lockMode, properties);
	}

	public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode) {
		return entityManager.find(entityClass, primaryKey, lockMode);
	}

	public <T> T find(Class<T> entityClass, Object primaryKey, Map<String,Object> properties) {
		return entityManager.find(entityClass, primaryKey, properties);
	}

	public <T> T find(Class<T> entityClass, Object primaryKey) {
		return entityManager.find(entityClass, primaryKey);
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

	public EntityGraph<?> getEntityGraph(String graphName) {
		return entityManager.getEntityGraph(graphName);
	}

	public <T> List<EntityGraph<? super T>> getEntityGraphs(Class<T> entityClass) {
		return entityManager.getEntityGraphs(entityClass);
	}

	public EntityManagerFactory getEntityManagerFactory() {
		return entityManager.getEntityManagerFactory();
	}

	public FlushModeType getFlushMode() {
		return entityManager.getFlushMode();
	}

	public LockModeType getLockMode(Object entity) {
		return entityManager.getLockMode(entity);
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
	
	public void setFlushMode(FlushModeType flushMode) {
		entityManager.setFlushMode(flushMode);
	}

	public void setProperty(String propertyName, Object value) {
		entityManager.setProperty(propertyName, value);
	}

	public <T> T unwrap(Class<T> cls) {
		return entityManager.unwrap(cls);
	}
}
