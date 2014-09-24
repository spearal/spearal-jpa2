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
package org.spearal.jpa2.impl;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import javax.persistence.PersistenceUnitUtil;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.Attribute.PersistentAttributeType;
import javax.persistence.metamodel.IdentifiableType;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.MapAttribute;
import javax.persistence.metamodel.PluralAttribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.Type.PersistenceType;

import org.spearal.configuration.PartialObjectFactory.PartialObjectProxy;
import org.spearal.configuration.PropertyFactory.Property;
import org.spearal.jpa2.impl.accessor.Accessor;
import org.spearal.jpa2.impl.accessor.FieldGetter;
import org.spearal.jpa2.impl.accessor.FieldSetter;
import org.spearal.jpa2.impl.accessor.Getter;
import org.spearal.jpa2.impl.accessor.MethodGetter;
import org.spearal.jpa2.impl.accessor.MethodSetter;
import org.spearal.jpa2.impl.accessor.Setter;
import org.spearal.jpa2.impl.reference.BeanPropertyReference;
import org.spearal.jpa2.impl.reference.CollectionReference;
import org.spearal.jpa2.impl.reference.ListReference;
import org.spearal.jpa2.impl.reference.MapKeyReference;
import org.spearal.jpa2.impl.reference.MapValueReference;
import org.spearal.jpa2.impl.reference.Reference;

/**
 * @author Franck WOLFF
 */
public class PartialEntityResolver {
	
	private final EntityManager entityManager;
	private final PersistenceUnitUtil persistenceUtil;
	
	private final ConcurrentMap<AccessorKey, Accessor> accessorsCache;

	public PartialEntityResolver(EntityManager entityManager) {
		this.entityManager = entityManager;
		this.persistenceUtil = entityManager.getEntityManagerFactory().getPersistenceUnitUtil();

		this.accessorsCache = new ConcurrentHashMap<AccessorKey, Accessor>();
	}
	
	@SuppressWarnings("unchecked")
	public <T> T resolve(Object entity) {
		return (T)resolve(entity, introspect(entity));
	}
	
	public PartialEntityMap introspect(Object entity) {
		PartialEntityMap proxyMap = new PartialEntityMap();
		
		PartialObjectProxy partialObject = (entity instanceof PartialObjectProxy ? (PartialObjectProxy)entity : null);
		Class<?> entityClass = (partialObject != null ? entity.getClass().getSuperclass() : entity.getClass());
		ManagedType<?> managedType = getManagedType(entityClass);
		
		if (managedType == null || managedType.getPersistenceType() != PersistenceType.ENTITY)
			throw new PersistenceException("Not a managed entity: " + entityClass);
		
		if (partialObject != null)
			proxyMap.add(partialObject);
		
		introspect(entity, proxyMap, new IdentityHashMap<Object, Boolean>());
		
		return proxyMap;
	}
	
	public Object resolve(Object entity, PartialEntityMap partialObjectsMap) {
		Map<PartialObjectProxy, Object> resolved = new IdentityHashMap<PartialObjectProxy, Object>(partialObjectsMap.size());
		
		List<Reference> references = partialObjectsMap.remove(entity);
		if (references != null)
			entity = resolve((PartialObjectProxy)entity, references, resolved);
		
		for (Entry<PartialObjectProxy, List<Reference>> partialObjectReferences : partialObjectsMap.entrySet())
			resolve(partialObjectReferences.getKey(), partialObjectReferences.getValue(), resolved);
		
		return entity;
	}
	
	@SuppressWarnings("unchecked")
	private void introspect(Object entity, PartialEntityMap partialObjectsMap, IdentityHashMap<Object, Boolean> visited) {
		if (entity == null || visited.containsKey(entity))
			return;
		
		PartialObjectProxy partialObject = (entity instanceof PartialObjectProxy ? (PartialObjectProxy)entity : null);
		ManagedType<?> managedType = getManagedType(partialObject != null ? entity.getClass().getSuperclass() : entity.getClass());

		if (managedType != null && managedType.getPersistenceType() != PersistenceType.BASIC && persistenceUtil.isLoaded(entity)) {
			visited.put(entity, Boolean.TRUE);
			
			for (Attribute<?, ?> attribute : managedType.getAttributes()) {

				if (attribute.getPersistentAttributeType() == PersistentAttributeType.BASIC)
					continue;
				if (partialObject != null && !partialObject.$isDefined(attribute.getName()))
					continue;
				if (!persistenceUtil.isLoaded(entity, attribute.getName()))
					continue;
				
				switch (attribute.getPersistentAttributeType()) {

					case BASIC:
						continue;
					
					case MANY_TO_ONE: case ONE_TO_ONE: {
						Accessor accessor = getAttributeAccessor(attribute);
						Object entityValue = accessor.getter.getValue(entity);
						if (entityValue != null) {
							if (entityValue instanceof PartialObjectProxy)
								partialObjectsMap.add((PartialObjectProxy)entityValue, new BeanPropertyReference(entity, accessor.setter));
							introspect(entityValue, partialObjectsMap, visited);
						}
						break;
					}
					
					 case EMBEDDED: {
						Accessor accessor = getAttributeAccessor(attribute);
						Object embeddedValue = accessor.getter.getValue(entity);
						if (embeddedValue != null) {
							if (embeddedValue instanceof PartialObjectProxy)
								partialObjectsMap.add((PartialObjectProxy)embeddedValue, new BeanPropertyReference(entity, accessor.setter));
							introspect(embeddedValue, partialObjectsMap, visited);
						}
						break;
					}
					
					case MANY_TO_MANY: case ONE_TO_MANY: case ELEMENT_COLLECTION: {
						PluralAttribute<?, ?, ?> collectionOrMapAttribute = (PluralAttribute<?, ?, ?>)attribute;
						
						switch (collectionOrMapAttribute.getCollectionType()) {
	
							case LIST: {
								PersistenceType elementType = collectionOrMapAttribute.getElementType().getPersistenceType();
								if (elementType == PersistenceType.BASIC)
									continue;
								
								List<Object> listValue = (List<Object>)getAttributeAccessor(attribute).getter.getValue(entity);
								final int size = listValue.size();
								for (int i = 0; i < size; i++) {
									Object element = listValue.get(i);
									if (element instanceof PartialObjectProxy)
										partialObjectsMap.add((PartialObjectProxy)element, new ListReference(listValue, i));
									introspect(element, partialObjectsMap, visited);
								}
								break;
							}
							
							case SET: case COLLECTION: {
								PersistenceType elementType = collectionOrMapAttribute.getElementType().getPersistenceType();
								if (elementType == PersistenceType.BASIC)
									continue;
	
								Collection<Object> collectionValue = (Collection<Object>)getAttributeAccessor(attribute).getter.getValue(entity);
								for (Object element : collectionValue) {
									if (element instanceof PartialObjectProxy)
										partialObjectsMap.add((PartialObjectProxy)element, new CollectionReference(collectionValue, element));
									introspect(element, partialObjectsMap, visited);
								}
								break;
							}
							
							case MAP: {
								MapAttribute<?, ?, ?> mapAttribute = (MapAttribute<?, ?, ?>)collectionOrMapAttribute;
								PersistenceType keyType = mapAttribute.getKeyType().getPersistenceType();
								PersistenceType valueType = mapAttribute.getElementType().getPersistenceType();
								
								if (keyType == PersistenceType.BASIC && valueType == PersistenceType.BASIC)
									continue;
								
								Map<Object, Object> mapValue = (Map<Object, Object>)getAttributeAccessor(attribute).getter.getValue(entity);
								for (Entry<Object, Object> entry : mapValue.entrySet()) {
									Object key = entry.getKey();
									if (keyType != PersistenceType.BASIC) {
										if (key instanceof PartialObjectProxy)
											partialObjectsMap.add((PartialObjectProxy)key, new MapKeyReference(mapValue, key));
										introspect(key, partialObjectsMap, visited);
									}
									if (valueType != PersistenceType.BASIC) {
										Object value = entry.getValue();
										if (value instanceof PartialObjectProxy)
											partialObjectsMap.add((PartialObjectProxy)value, new MapValueReference(mapValue, key));
										introspect(value, partialObjectsMap, visited);
									}
								}
								break;
							}
						}
						break;
					}
				}
			}
		}
	}
	
	private Object resolve(PartialObjectProxy partialObject, List<Reference> references, Map<PartialObjectProxy, Object> resolved) {
		Class<?> entityClass = partialObject.getClass().getSuperclass();
		Object entity = newInstance(entityClass);
		
		for (Property property : partialObject.$getDefinedProperties())
			setPropertyValue(entity, property, getPropertyValue(partialObject, property));

		if (partialObject.$hasUndefinedProperties()) {
			ManagedType<?> managedType = getManagedType(entityClass);
			
			switch (managedType.getPersistenceType()) {
				case BASIC: case MAPPED_SUPERCLASS:
					throw new UnsupportedOperationException("Internal error: " + entityClass.getName() + " - " + managedType.getPersistenceType());

				case ENTITY:  {
					Object id = getId(partialObject, (IdentifiableType<?>)managedType);
					if (id != null) {
						Object loaded = entityManager.find(managedType.getJavaType(), id);
						if (loaded != null) {
							for (Attribute<?, ?> attribute : managedType.getAttributes()) {
								if (!partialObject.$isDefined(attribute.getName())) {
									Accessor accessor = getAttributeAccessor(attribute);
									accessor.setter.setValue(entity, accessor.getter.getValue(loaded));
								}
							}
						}
					}
					break;
				}
				
				case EMBEDDABLE: {
					throw new UnsupportedOperationException("Partial Embeddable: " + entityClass.getName());
				}
			}
		}
		
		for (Reference reference : references)
			reference.set(resolved, entity);
		
		resolved.put(partialObject, entity);
		return entity;
	}
	
	private Object getId(PartialObjectProxy entity, IdentifiableType<?> identifiableType) {
		
		// Single @Id or @EmbeddedId
		if (identifiableType.hasSingleIdAttribute()) {
			SingularAttribute<?, ?> idAttribute = identifiableType.getId(identifiableType.getIdType().getJavaType());
			if (!entity.$isDefined(idAttribute.getName()))
				return null;
			return getAttributeAccessor(idAttribute).getter.getValue(entity);
		}
		
		// @IdClass
		SingularAttribute<?, ?>[] idAttributes = identifiableType.getIdClassAttributes().toArray(new SingularAttribute<?, ?>[0]);
		Class<?> idClassClass = idAttributes[0].getJavaMember().getDeclaringClass();
		Object id = newInstance(idClassClass);
		for (SingularAttribute<?, ?> idAttribute : identifiableType.getIdClassAttributes()) {
			if (entity.$isDefined(idAttribute.getName())) {
				Object value = getAttributeAccessor(idAttribute).getter.getValue(entity);
				getIdClassFieldAccessor(idClassClass, idAttribute.getName()).setter.setValue(id, value);
			}
		}
		return id;
	}
	
	private ManagedType<?> getManagedType(Class<?> entityClass) {
		try {
			return entityManager.getMetamodel().managedType(entityClass);
		}
		catch (IllegalArgumentException e) {
			return null;
		}
	}
	
	private Object newInstance(Class<?> cls) {
		try {
			return cls.newInstance();
		}
		catch (Exception e) {
			throw new RuntimeException("Could not instantiate class: " + cls, e);
		}
	}
	
	private Object getPropertyValue(Object holder, Property property) {
		try {
			if (property.getGetter() != null)
				return property.getGetter().invoke(holder);
			if (property.getField() != null)
				return property.getField().get(holder);
		}
		catch (Exception e) {
			throw new RuntimeException("Error while getting value of property: " + property);
		}
		throw new RuntimeException("Not a readable property: " + property); 
	}
	
	private void setPropertyValue(Object holder, Property property, Object value) {
		boolean writable = true;
		try {
			if (property.getSetter() != null)
				property.getSetter().invoke(holder, value);
			else if (property.getField() != null)
				property.getField().set(holder, value);
			else
				writable = false;
		}
		catch (Exception e) {
			throw new RuntimeException("Error while setting value of property: " + property);
		}
		if (!writable)
			throw new RuntimeException("Not a writable property: " + property); 
	}
	
	private Accessor getAttributeAccessor(Attribute<?, ?> attribute) {
		AccessorKey key = new AccessorKey(attribute.getDeclaringType().getJavaType(), attribute.getName());
		Accessor accessor = accessorsCache.get(key);
		
		if (accessor == null) {
			final Class<?> entityClass = attribute.getDeclaringType().getJavaType();
			final String name = attribute.getName();

			Getter getter = null;
			Setter setter = null;
			
			try {
				PropertyDescriptor[] properties = Introspector.getBeanInfo(entityClass).getPropertyDescriptors();
				for (PropertyDescriptor property : properties) {
					if (name.equals(property.getName())) {
						Method readMethod = property.getReadMethod();
						if (readMethod != null)
							getter = new MethodGetter(readMethod);
						Method writeMethod = property.getWriteMethod();
						if (writeMethod != null)
							setter = new MethodSetter(writeMethod);
						break;
					}
				}
			}
			catch (IntrospectionException e) {
				throw new RuntimeException("Could not introspect bean class: " + entityClass, e);
			}
			
			if (getter == null) {
				Member member = attribute.getJavaMember();
				if (member instanceof Field)
					getter = new FieldGetter((Field)member);
				else if (member instanceof Method && ((Method)member).getReturnType() != void.class)
					getter = new MethodGetter((Method)member);
				else {
					try {
						getter = new FieldGetter(entityClass.getField(name));
					}
					catch (Exception e) {
						throw new RuntimeException("Could not find any way to get the value of: " + attribute, e);
					}
				}
			}
			
			if (setter == null) {
				Member member = attribute.getJavaMember();
				if (member instanceof Field)
					setter = new FieldSetter((Field)member);
				else if (member instanceof Method && ((Method)member).getReturnType() == void.class)
					setter = new MethodSetter((Method)member);
				else {
					try {
						setter = new FieldSetter(entityClass.getField(name));
					}
					catch (Exception e) {
						throw new RuntimeException("Could not find any way to set the value of: " + attribute);
					}
				}
				
			}
			
			accessor = new Accessor(getter, setter);
			accessorsCache.putIfAbsent(key, accessor);
		}

		return accessor;
	}
	
	private Accessor getIdClassFieldAccessor(Class<?> idClassClass, String fieldName) {
		AccessorKey key = new AccessorKey(idClassClass, fieldName);
		Accessor accessor = accessorsCache.get(key);
		if (accessor == null) {
			try {
				Field field = idClassClass.getDeclaredField(fieldName);
				field.setAccessible(true);
				accessor = new Accessor(new FieldGetter(field), new FieldSetter(field));
				accessorsCache.putIfAbsent(key, accessor);
			}
			catch (Exception e) {
				throw new RuntimeException("Could not find field " + fieldName + " in id class: " + idClassClass.getName());
			}
		}
		return accessor;
	}
	
	public static class PartialEntityMap extends IdentityHashMap<PartialObjectProxy, List<Reference>> {

		private static final long serialVersionUID = 1L;
		
		public void add(PartialObjectProxy proxy) {
			add(proxy, null);
		}

		public void add(PartialObjectProxy proxy, Reference reference) {
			List<Reference> references = get(proxy);
			if (references == null) {
				references = new ArrayList<Reference>();
				put(proxy, references);
			}
			if (reference != null) {
				if (reference instanceof MapKeyReference)
					references.add(0, reference);
				else
					references.add(reference);
			}
		}
	}

	public static class AccessorKey {
		
		private final Class<?> cls;
		private final String name;
		private final int hash;

		public AccessorKey(Class<?> cls, String name) {
			this.cls = cls;
			this.name = name;
			this.hash = cls.hashCode() + name.hashCode();
		}

		@Override
		public int hashCode() {
			return hash;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this)
				return true;
			if (!(obj instanceof AccessorKey))
				return false;
			return ((AccessorKey)obj).cls == cls && ((AccessorKey)obj).name.equals(name);
		}

		@Override
		public String toString() {
			return cls.getName() + "." + name;
		}
	}
}
