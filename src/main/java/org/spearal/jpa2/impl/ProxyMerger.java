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
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.IdentifiableType;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.SingularAttribute;

import org.spearal.configuration.PartialObjectFactory.PartialObjectProxy;
import org.spearal.configuration.PropertyFactory.Property;

/**
 * @author William DRAI
 */
public class ProxyMerger {
	
	private final Logger log = Logger.getLogger(ProxyMerger.class.getName());
	
	private final EntityManager entityManager;
	
	public ProxyMerger(EntityManager entityManager) {
		this.entityManager = entityManager;
	}
	
	public boolean isProxy(Object entity) {
		IdentityHashMap<Object, Object> cache = new IdentityHashMap<Object, Object>();
		return isProxy(entity, cache);
	}
	
	public boolean isProxy(Object entity, IdentityHashMap<Object, Object> cache) {
		if (entity == null)
			return false;
		if (entity instanceof String || entity instanceof Number || entity instanceof Date || entity instanceof Type)
			return false;
		
		if (entity instanceof PartialObjectProxy)
			return true;
		
		if (cache.containsKey(entity))
			return false;		
		cache.put(entity, entity);
		
		PropertyDescriptor[] propertyDescriptors;
		try {
			propertyDescriptors = Introspector.getBeanInfo(entity.getClass(), Introspector.IGNORE_ALL_BEANINFO).getPropertyDescriptors();
		}
		catch (IntrospectionException ie) {
			throw new RuntimeException("Could not introspect class " + entity.getClass(), ie);
		}
		ManagedType<?> managedType = null;
		try {
			managedType = entityManager.getMetamodel().managedType(entity.getClass());
		}
		catch (IllegalArgumentException iae) {
			throw new RuntimeException(entity.getClass().getName() + " is not a managed class", iae);
		}
		
		for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
			Object value = null;
			boolean fieldAccess = false;
			boolean readable = true;
			
			try {
				Attribute<?, ?> attribute = managedType.getAttribute(propertyDescriptor.getName());
				if (attribute.getJavaMember() instanceof Field) {
					try {
						((Field)attribute.getJavaMember()).setAccessible(true);
						value = ((Field)attribute.getJavaMember()).get(entity);
						fieldAccess = true;
					} 
					catch (IllegalAccessException iace) {
						throw new RuntimeException("Could not read field " + attribute.getName() + " of class " + entity.getClass(), iace);
					}
				}
			}
			catch (IllegalArgumentException iae) {
				// No JPA attribute
			}
			if (!fieldAccess) {
				if (propertyDescriptor.getReadMethod() != null) {
					try {
						value = propertyDescriptor.getReadMethod().invoke(entity);
					}
					catch (IllegalAccessException iace) {
						throw new RuntimeException("Could not read property " + propertyDescriptor.getName() + " of class " + entity.getClass(), iace);
					}
					catch (InvocationTargetException ite) {
						throw new RuntimeException("Could not read property " + propertyDescriptor.getName() + " of class " + entity.getClass(), ite);
					}
				}
				else
					readable = false;
			}
			if (!readable)
				continue;
			
			if (value instanceof Collection<?>) {
				for (Object element : ((Collection<?>)value)) {
					if (isProxy(element, cache))
						return true;
				}
			}
			else if (value instanceof Map<?, ?>) {
				for (Entry<?, ?> entry : ((Map<?, ?>)value).entrySet()) {
					if (isProxy(entry.getKey(), cache))
						return true;
					if (isProxy(entry.getValue(), cache))
						return true;
				}
			}
			else if (isProxy(value, cache))
				return true;
		}
			
		return false;
	}
	
	@SuppressWarnings("unchecked")
	public <T> T merge(T entity) {
		IdentityHashMap<Object, Object> cache = new IdentityHashMap<Object, Object>();
		return (T)merge(entity, cache);
	}
	
	private Object merge(Object detachedEntity, IdentityHashMap<Object, Object> cache) {
        if (detachedEntity == null)
            return null;
		
        if (cache.containsKey(detachedEntity))
            return detachedEntity;
        cache.put(detachedEntity, detachedEntity);
        
		boolean isProxy = false;
		Class<?> entityClass = detachedEntity.getClass();
		if (PartialObjectProxy.class.isInstance(detachedEntity)) {
			entityClass = entityClass.getSuperclass(); // proxy is a subclass of real entity class
			isProxy = true;
		}
		
		IdentifiableType<?> identifiableType;
		try {
			ManagedType<?> managedType = entityManager.getMetamodel().managedType(entityClass);
			if (!(managedType instanceof IdentifiableType<?>))
				throw new RuntimeException("Entity class " + entityClass.getName() + " is not an entity");
			identifiableType = (IdentifiableType<?>)managedType;
		}
		catch (IllegalArgumentException iae) {
			throw new RuntimeException("Entity class " + entityClass.getName() + " is not an entity", iae);
		}
		
		SingularAttribute<?, ?> idAttribute = identifiableType.getId(identifiableType.getIdType().getJavaType());
		Object id = null;
		if (idAttribute.getJavaMember() instanceof Field && !isProxy) {
			((Field)idAttribute.getJavaMember()).setAccessible(true);
			try {
				id = ((Field)idAttribute.getJavaMember()).get(detachedEntity);
			} 
			catch (IllegalAccessException iace) {
				throw new RuntimeException("Could not read id field " + idAttribute.getName(), iace);
			}
		}
		else {
			try {
				// Try JPA getter
				if (idAttribute.getJavaMember() instanceof Method) {
					id = ((Method)idAttribute.getJavaMember()).invoke(detachedEntity);
				}
				else {
					Method idGetter;
					try {
						idGetter = detachedEntity.getClass().getMethod("get" + idAttribute.getName().substring(0, 1).toUpperCase() + idAttribute.getName().substring(1));
						id = idGetter.invoke(detachedEntity);
					} 
					catch (NoSuchMethodException nsme) {
						throw new RuntimeException("Could not find id getter " + idAttribute.getName(), nsme);
					} 
				}
			} 
			catch (IllegalAccessException iace) {
				throw new RuntimeException("Could not read id property " + idAttribute.getName(), iace);
			}
			catch (InvocationTargetException ite) {
				throw new RuntimeException("Could not read id property " + idAttribute.getName(), ite);
			}
		}
		
		Object entity = null;
		if (id != null)
			entity = entityManager.find(entityClass, (Serializable)id);
		
		if (entity == null) {
			try {
				entity = entityClass.newInstance();
			}
			catch (IllegalAccessException iace) {
				throw new RuntimeException("Could not instantiate class " + entityClass, iace);
			}
			catch (InstantiationException ie) {
				throw new RuntimeException("Could not instantiate class " + entityClass, ie);
			}
		}
		
//		SingularAttribute<?, ?> versionAttribute = getVersionAttribute(entityManager.getMetamodel().managedType(entityClass));
//		if (versionAttribute != null) {
//			Member versionMember = versionAttribute.getJavaMember();
//			Object incomingVersion = null;
//			Object currentVersion = null;
//			String versionPropertyName = null;
//			
//			if (versionMember instanceof Field) {
//				versionPropertyName = Introspector.getBeanInfo(entityClass).get
//				currentVersion = ((Field)versionMember).get(entity);
//			}
//			if (versionMember instanceof Method) {
//				currentVersion = ((Method)versionMember).invoke(entity);
//				incomingVersion = ((Method)versionMember).invoke(detachedEntity);
//				if ((incomingVersion == null && currentVersion != null) 
//						|| (incomingVersion != null && currentVersion != null && incomingVersion instanceof Number && ((Number)incomingVersion).longValue() < ((Number)currentVersion).longValue())
//						|| (incomingVersion != null && currentVersion != null && incomingVersion instanceof Timestamp && ((Timestamp)incomingVersion).before(((Timestamp)currentVersion)))) {
//					throw new OptimisticLockException(entity);
//				}
//			}
//		}
		
		if (detachedEntity instanceof PartialObjectProxy)
			return mergeProxy(entity, (PartialObjectProxy)detachedEntity, cache);
		else
			return mergeObject(entity, detachedEntity, cache);
	}
	
	
	private Object mergeProxy(Object entity, PartialObjectProxy detachedProxy, IdentityHashMap<Object, Object> cache) {
		
		PropertyDescriptor[] propertyDescriptors;
		try {
			propertyDescriptors = Introspector.getBeanInfo(entity.getClass(), Introspector.IGNORE_ALL_BEANINFO).getPropertyDescriptors();
		}
		catch (IntrospectionException ie) {
			throw new RuntimeException("Could not introspect class " + entity.getClass(), ie);
		}
		
		for (Property property : detachedProxy.$getDefinedProperties()) {
			String propertyName = property.getName();
			Object value = null;
			try {
				value = property.getGetter().invoke(detachedProxy);
			}
			catch (IllegalAccessException iae) {
				throw new RuntimeException("Could not read property " + propertyName, iae);
			}
			catch (InvocationTargetException ite) {
				throw new RuntimeException("Could not read property " + propertyName, ite);
			}
			
			PropertyDescriptor propertyDescriptor = null;
			for (PropertyDescriptor pd : propertyDescriptors) {
				if (pd.getName().equals(propertyName)) {
					propertyDescriptor = pd;
					break;
				}
			}
			
			Attribute<?, ?> attribute = null;
			try {
				attribute = entityManager.getMetamodel().managedType(entity.getClass()).getAttribute(propertyName);
			}
			catch (IllegalArgumentException iae) {
				// Not a JPA attribute
			}
			
			if ((attribute != null && (attribute.isAssociation() || attribute.isCollection())) || attribute == null)
				value = merge(value, cache);
			
			// Should convert ???
			// if (propertyDescriptor != null && propertyDescriptor.getWriteMethod() != null) {
			// 		value = convert(value, propertyDescriptor.getWriteMethod().getGenericParameterTypes()[0]);
			// }
			
			try {
				// Persistent attributes
				if (attribute.getJavaMember() instanceof Field) {
					((Field)attribute.getJavaMember()).setAccessible(true);
					try {
						((Field)attribute.getJavaMember()).set(entity, value);
					} 
					catch (IllegalAccessException iace) {
						throw new RuntimeException("Could not write field " + attribute.getName(), iace);
					}
				}
				else if (attribute.getJavaMember() instanceof Method && propertyDescriptor.getWriteMethod() != null) {
					// Use setter
					try {
						propertyDescriptor.getWriteMethod().invoke(entity, value);
					} 
					catch (IllegalAccessException iace) {
						throw new RuntimeException("Could not write property " + attribute.getName(), iace);
					}
					catch (InvocationTargetException ite) {
						throw new RuntimeException("Could not write property " + attribute.getName(), ite);
					}
				}
				else
					log.logp(Level.FINE, ProxyMerger.class.getName(), "merge", "Property {0} on class {1} does not exist or is not writeable", new Object[] { propertyName, entity.getClass().getName() });
			}
			catch (IllegalArgumentException iae) {
				if (propertyDescriptor != null && propertyDescriptor.getWriteMethod() != null) {
					try {
						propertyDescriptor.getWriteMethod().invoke(entity, value);
					} 
					catch (IllegalAccessException iace) {
						throw new RuntimeException("Could not write property " + propertyDescriptor.getName(), iace);
					}
					catch (InvocationTargetException ite) {
						throw new RuntimeException("Could not write property " + propertyDescriptor.getName(), ite);
					}
				}
				else
					log.logp(Level.FINE, ProxyMerger.class.getName(), "merge", "Property {0} on class {1} does not exist or is not writeable", new Object[] { propertyName, entity.getClass().getName() });
			}
		}
		
		return entity;
	}

    protected Object mergeObject(Object entity, Object detachedEntity, IdentityHashMap<Object, Object> cache) {
        if (detachedEntity == null)
            return null;
        
		PropertyDescriptor[] propertyDescriptors;
		try {
			propertyDescriptors = Introspector.getBeanInfo(entity.getClass(), Introspector.IGNORE_ALL_BEANINFO).getPropertyDescriptors();
		}
		catch (IntrospectionException ie) {
			throw new RuntimeException("Could not introspect class " + entity.getClass(), ie);
		}
        
        if (entity != null && !entityManager.getEntityManagerFactory().getPersistenceUnitUtil().isLoaded(entity)) {
            // cache.contains() cannot be called on un unintialized proxy because hashCode will fail !!
        	ManagedType<?> managedType = entityManager.getMetamodel().managedType(entity.getClass());
        	if (managedType instanceof IdentifiableType<?>) {
//        		Class<?> idType = ((IdentifiableType<?>)managedType).getIdType().getJavaType();
//        		SingularAttribute<?, ?> idAttribute = ((IdentifiableType<?>)managedType).getId(idType);
        		
        		Object id = entityManager.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(detachedEntity);
        		return entityManager.find(entity.getClass(), id);
        	}
        }
        
        // If the detached entity has an id, we should get the managed instance
    	ManagedType<?> managedType = entityManager.getMetamodel().managedType(entity.getClass());
    	Object id = entityManager.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(detachedEntity);
    	
    	if (id != null && managedType instanceof IdentifiableType<?>) {
//    		Class<?> idType = ((IdentifiableType<?>)managedType).getIdType().getJavaType();
//    		SingularAttribute<?, ?> idAttribute = ((IdentifiableType<?>)managedType).getId(idType);
    		
    		return entityManager.find(entity.getClass(), id);
    	}
        
        // If there is no id, traverse the object graph to merge associated objects
    	for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
			Object value = null;
			boolean fieldAccess = false;
			boolean readable = true;
			Attribute<?, ?> attribute = null;
			
			try {
				attribute = managedType.getAttribute(propertyDescriptor.getName());
				if (attribute.getJavaMember() instanceof Field) {
					try {
						((Field)attribute.getJavaMember()).setAccessible(true);
						value = ((Field)attribute.getJavaMember()).get(entity);
						fieldAccess = true;
					} 
					catch (IllegalAccessException iace) {
						throw new RuntimeException("Could not read field " + attribute.getName() + " of class " + entity.getClass(), iace);
					}
				}
			}
			catch (IllegalArgumentException iae) {
				// No JPA attribute
			}
			if (!fieldAccess) {
				if (propertyDescriptor.getReadMethod() != null) {
					try {
						value = propertyDescriptor.getReadMethod().invoke(entity);
					}
					catch (IllegalAccessException iace) {
						throw new RuntimeException("Could not read property " + propertyDescriptor.getName() + " of class " + entity.getClass(), iace);
					}
					catch (InvocationTargetException ite) {
						throw new RuntimeException("Could not read property " + propertyDescriptor.getName() + " of class " + entity.getClass(), ite);
					}
				}
				else
					readable = false;
			}
			if (!readable)
				continue;
			
			if (value instanceof List<?>) {
                @SuppressWarnings("unchecked")
				List<Object> list = (List<Object>)value;
				for (int idx = 0; idx < list.size(); idx++) {
					Object element = list.get(idx);
					if (element == null)
						continue;
					Object newElement = merge(element, cache);
					if (newElement != element)
						list.set(idx, newElement);
				}
			}
			else if (value instanceof Collection<?>) {
                @SuppressWarnings("unchecked")
				Collection<Object> coll = (Collection<Object>)value;
                Iterator<Object> icoll = coll.iterator();
                Set<Object> addedElements = new HashSet<Object>();
                while (icoll.hasNext()) {
                    Object element = icoll.next();
                    if (element != null) {
                        Object newElement = merge(element, cache);
                        if (newElement != element) {
                            icoll.remove();
                            addedElements.add(newElement);
                        }
                    }
                }
                coll.addAll(addedElements);
			}
            else if (value instanceof Map<?, ?>) {
                @SuppressWarnings("unchecked")
				Map<Object, Object> map = (Map<Object, Object>)value;
                Iterator<Entry<Object, Object>> ime = map.entrySet().iterator();
                Map<Object, Object> addedElements = new HashMap<Object, Object>();
                while (ime.hasNext()) {
                    Entry<Object, Object> me = ime.next();
                    Object val = me.getValue();
                    if (val != null) {
                        Object newVal = merge(val, cache);
                        if (newVal != val)
                            me.setValue(newVal);
                    }
                    Object key = me.getKey();
                    if (key != null) {
                        Object newKey = merge(key, cache);
                        if (newKey != key) {
                            ime.remove();
                            addedElements.put(newKey, me.getValue());
                        }
                    }
                }
                map.putAll(addedElements);
            }
            else {
            	Object newValue = value;
            	
            	try {
            		entityManager.getMetamodel().managedType(value.getClass());
            		newValue = merge(value, cache);
            	}
            	catch (IllegalArgumentException iae) {
            		// Not an entity
            	}
        		
        		if (fieldAccess) {
        			try {
						((Field)attribute.getJavaMember()).set(entity, newValue);
					} 
        			catch (IllegalAccessException iace) {
						throw new RuntimeException("Could not write field " + attribute.getName() + " of class " + entity.getClass(), iace);
					}
        		}
        		else {
    				if (propertyDescriptor.getWriteMethod() != null) {
    					try {
    						propertyDescriptor.getWriteMethod().invoke(entity, newValue);
    					}
    					catch (IllegalAccessException iace) {
    						throw new RuntimeException("Could not read property " + propertyDescriptor.getName() + " of class " + entity.getClass(), iace);
    					}
    					catch (InvocationTargetException ite) {
    						throw new RuntimeException("Could not read property " + propertyDescriptor.getName() + " of class " + entity.getClass(), ite);
    					}
    				}
        		}
            }
        }

        return entity;
    }
	
	public static SingularAttribute<?, ?> getVersionAttribute(ManagedType<?> managedType) {
		if (!(managedType instanceof IdentifiableType<?>))
			return null;
		
		IdentifiableType<?> identifiableType = (IdentifiableType<?>)managedType;
		if (!identifiableType.hasVersionAttribute())
			return null;
		
		for (SingularAttribute<?, ?> attribute : identifiableType.getSingularAttributes()) {
			if (attribute.isVersion())
				return attribute;
		}
		return null;
	}

}
