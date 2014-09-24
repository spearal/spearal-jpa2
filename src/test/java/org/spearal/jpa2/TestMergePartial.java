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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.SingularAttribute;

import org.hibernate.Hibernate;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.spearal.DefaultSpearalFactory;
import org.spearal.SpearalDecoder;
import org.spearal.SpearalEncoder;
import org.spearal.SpearalFactory;
import org.spearal.configuration.PartialObjectFactory.PartialObjectProxy;
import org.spearal.configuration.PartialObjectFactory.UndefinedPropertyException;
import org.spearal.configuration.PropertyFactory.Property;
import org.spearal.impl.partial.JavassistPartialObjectFactory;
import org.spearal.impl.property.AnyProperty;
import org.spearal.impl.property.StringProperty;
import org.spearal.jpa2.impl.PartialEntityResolver;
import org.spearal.jpa2.impl.PartialEntityResolver.PartialEntityMap;
import org.spearal.jpa2.model.AbstractEntity;
import org.spearal.jpa2.model.Contact;
import org.spearal.jpa2.model.EntityWithEmbeddedId;
import org.spearal.jpa2.model.EntityWithEmbeddedId.EntityEmbeddedId;
import org.spearal.jpa2.model.EntityWithIdClass;
import org.spearal.jpa2.model.EntityWithIdClass.EntityIdClass;
import org.spearal.jpa2.model.Person;

/**
 * @author Franck WOLFF
 */
@WithPersistenceUnit(dbName="testdb", persistenceUnitName="hibernate4-merge-pu", wrapped=true)
public class TestMergePartial extends AbstractHibernate4TestUnit {

	@Before
	public void setUp() throws Exception {
		super.setUp();
	}

	@After
	public void tearDown() throws Exception {
		super.tearDown();
	}

	
	@Test
	public void testIsProxy() throws IOException {
		SpearalFactory clientFactory = new DefaultSpearalFactory(false);
		SpearalFactory serverFactory = new DefaultSpearalFactory();
		
		Person serverPerson1 = new Person("Bla", "Bla");
		
		EntityManager entityManager = entityManagerFactory.createEntityManager();
		PartialEntityResolver resolver = new PartialEntityResolver(entityManager);
		Assert.assertTrue("Simple object", resolver.introspect(serverPerson1).isEmpty());
		
		Contact serverContact1 = new Contact(serverPerson1, "bla", "06");
		serverPerson1.getContacts().add(serverContact1);
		Assert.assertTrue("Graph object", resolver.introspect(serverPerson1).isEmpty());

		Person clientPerson2 = new Person("Bruce", "Willis");
		Person serverPerson2 = clientEncodeServerDecode(clientFactory, serverFactory, clientPerson2, Person.class, "id", "version", "firstName");
		PartialEntityMap map = resolver.introspect(serverPerson2);
		Assert.assertFalse("Proxy object", map.isEmpty());
		serverPerson2 = (Person)resolver.resolve(serverPerson2, map);
		map = resolver.introspect(serverPerson2);
		Assert.assertTrue("Non Proxy object", map.isEmpty());
		
		
		Person clientPerson3 = new Person("Bruce", "Willis");
		Contact clientContact3 = new Contact(clientPerson3, "blo", "06");
		clientPerson3.getContacts().add(clientContact3);
		Person serverPerson3 = clientEncodeServerDecode(clientFactory, serverFactory, clientPerson3, Contact.class, "id", "version", "person", "mobile");
		Assert.assertFalse("Non proxy graph root", serverPerson3 instanceof PartialObjectProxy);
		map = resolver.introspect(serverPerson3);
		Assert.assertFalse("Proxy graph", map.isEmpty());
		serverPerson3 = (Person)resolver.resolve(serverPerson3, map);
		map = resolver.introspect(serverPerson3);
		Assert.assertTrue("Non Proxy graph", map.isEmpty());
		
		entityManager.close();
	}
	
	@Test
	public void testIsProxy2() throws Exception {
		SpearalFactory serverFactory = new DefaultSpearalFactory();
		
		Person serverPerson2 = new Person("Blo", "Blo");		
		serverPerson2 = mergeEntity(serverPerson2);
		
		Person serverPerson1 = new Person("Bla", "Bla");
		serverPerson1.setBestFriend(serverPerson2);
		serverPerson1 = mergeEntity(serverPerson1);
		
		serverPerson1 = findEntity(Person.class, serverPerson1.getId());
		EntityManager entityManager = entityManagerFactory.createEntityManager();
		PartialEntityResolver resolver = new PartialEntityResolver(entityManager);
		Assert.assertTrue("Simple object", resolver.introspect(serverPerson1).isEmpty());		
		entityManager.close();
		
		Person incomingPerson1 = new Person("Bla", "Bla");
		incomingPerson1.setId(serverPerson1.getId());
		
		Property[] properties = new Property[2];
		properties[0] = new AnyProperty("id", AbstractEntity.class.getDeclaredField("id"), Person.class.getMethod("getId"), Person.class.getMethod("setId", Long.class));
		properties[1] = new StringProperty("lastName", Person.class.getDeclaredField("lastName"), Person.class.getMethod("getLastName"), Person.class.getMethod("setLastName", String.class));
		Person incomingPerson2 = (Person)new JavassistPartialObjectFactory().instantiatePartial(serverFactory.getContext(), Person.class, properties);
		incomingPerson2.setId(serverPerson2.getId());
		incomingPerson2.setLastName("Toto");
		incomingPerson1.setBestFriend(incomingPerson2);
		
		mergeEntity(incomingPerson1);
		
		serverPerson1 = findEntity(Person.class, serverPerson1.getId());
		Assert.assertEquals("Value updated", "Toto", serverPerson1.getBestFriend().getLastName());
	}
	
	
	@Test
	public void testMergeProxy() throws Exception {
		Assert.assertNotNull(entityManagerFactory);
		
		Person clientPerson = new Person("Bruce", "Willis");
		
		// Do not load any services here (pseudo-client application).
		SpearalFactory clientFactory = new DefaultSpearalFactory(false);
		SpearalFactory serverFactory = new DefaultSpearalFactory();
		
		Person serverPerson = clientEncodeServerDecodeMerge(clientFactory, serverFactory, clientPerson, Person.class, "id", "version", "firstName");		
		
		Assert.assertNotNull("Id not null", serverPerson.getId());
		Assert.assertEquals("Version 0", Long.valueOf(0L), serverPerson.getVersion());
		
		serverPerson = findEntity(Person.class, serverPerson.getId());
		
		Assert.assertEquals("Person firstName", clientPerson.getFirstName(), serverPerson.getFirstName());
		
		clientPerson.setId(serverPerson.getId());
		clientPerson.backdoorSetVersion(serverPerson.getVersion());
		clientPerson.setFirstName("John");
		clientPerson.setLastName("McLane");
		
		serverPerson = clientEncodeServerDecodeMerge(clientFactory, serverFactory, clientPerson, Person.class, "id", "version", "firstName");
		
		Assert.assertEquals("Person firstName", clientPerson.getFirstName(), serverPerson.getFirstName());
		
		serverPerson = findEntity(Person.class, serverPerson.getId());
		
		Assert.assertEquals("Person firstName", clientPerson.getFirstName(), serverPerson.getFirstName());
		Assert.assertEquals("Version 1", Long.valueOf(1L), serverPerson.getVersion());
		
		Person clientFriend = new Person("Hans", "Gruber");
		clientPerson.backdoorSetVersion(serverPerson.getVersion());
		clientPerson.setBestFriend(clientFriend);
		
		serverPerson = clientEncodeServerDecodeMerge(clientFactory, serverFactory, clientPerson, Person.class, "id", "version", "firstName", "bestFriend");
		
		Assert.assertEquals("Friend firstName", clientFriend.getFirstName(), serverPerson.getBestFriend().getFirstName());
		Assert.assertEquals("Version 0", Long.valueOf(0L), serverPerson.getBestFriend().getVersion());
		
		serverPerson = findEntity(Person.class, serverPerson.getId());
		Assert.assertEquals("Version 2", Long.valueOf(2L), serverPerson.getVersion());
	}
	
	@Test
	public void testEmbeddedId() throws Exception {
		Assert.assertNotNull(entityManagerFactory);
		
		EntityWithEmbeddedId entity = new EntityWithEmbeddedId();
		entity.setId(new EntityEmbeddedId("Joe", "Smith"));
		entity.setAge(12);
		entity.setPhones(Arrays.asList("0123456789", "9876543210"));
		
		persistEntity(entity);
		
		entity = new EntityWithEmbeddedId();
		entity.setId(new EntityEmbeddedId("Joe", "Smith"));
		entity.setAge(24);
		
		SpearalFactory clientFactory = new DefaultSpearalFactory(false);
		SpearalFactory serverFactory = new DefaultSpearalFactory();

		entity = clientEncodeServerDecode(clientFactory, serverFactory, entity, EntityWithEmbeddedId.class, "id", "age");
		Assert.assertTrue(entity instanceof PartialObjectProxy);
		Assert.assertFalse(((PartialObjectProxy)entity).$isDefined("phones"));
		try {
			entity.getPhones();
			Assert.fail("Should throw a UndefinedPropertyException");
		}
		catch (UndefinedPropertyException e) {
		}
		
		entity = mergeEntity(entity);
		Assert.assertFalse(entity instanceof PartialObjectProxy);
		Assert.assertNotNull(entity.getPhones());
		Assert.assertFalse(entityManagerFactory.getPersistenceUnitUtil().isLoaded(entity.getPhones()));
		
		entity = findEntity(EntityWithEmbeddedId.class, new EntityEmbeddedId("Joe", "Smith"), "getPhones");
		Assert.assertEquals(24, entity.getAge());
		Assert.assertEquals(Arrays.asList("0123456789", "9876543210"), entity.getPhones());
	}
	
	@Test
	public void testIdClass() throws Exception {
		Assert.assertNotNull(entityManagerFactory);
		
		ManagedType<?> managedType = entityManagerFactory.getMetamodel().managedType(EntityWithIdClass.class);
		int idsCount = 0;
		for (SingularAttribute<?, ?> attribute : managedType.getSingularAttributes()) {
			if (attribute.isId())
				idsCount++;
		}
		Assert.assertEquals(2, idsCount);
		
		EntityWithIdClass entity = new EntityWithIdClass();
		entity.setFirstName("Jim");
		entity.setLastName("Hall");
		entity.setAge(12);
		entity.setPhones(Arrays.asList("0123456789", "9876543210"));
		
		persistEntity(entity);
		
		entity = new EntityWithIdClass();
		entity.setFirstName("Jim");
		entity.setLastName("Hall");
		entity.setAge(24);
		
		SpearalFactory clientFactory = new DefaultSpearalFactory(false);
		SpearalFactory serverFactory = new DefaultSpearalFactory();

		entity = clientEncodeServerDecode(clientFactory, serverFactory, entity, EntityWithIdClass.class, "firstName", "lastName", "age");
		Assert.assertTrue(entity instanceof PartialObjectProxy);
		Assert.assertFalse(((PartialObjectProxy)entity).$isDefined("phones"));
		try {
			entity.getPhones();
			Assert.fail("Should throw a UndefinedPropertyException");
		}
		catch (UndefinedPropertyException e) {
		}
		
		entity = mergeEntity(entity);
		Assert.assertFalse(entity instanceof PartialObjectProxy);
		Assert.assertNotNull(entity.getPhones());
		Assert.assertFalse(entityManagerFactory.getPersistenceUnitUtil().isLoaded(entity.getPhones()));
		
		entity = findEntity(EntityWithIdClass.class, new EntityIdClass("Jim", "Hall"), "getPhones");
		Assert.assertEquals(24, entity.getAge());
		Assert.assertEquals(Arrays.asList("0123456789", "9876543210"), entity.getPhones());
	}
	
//	@Test
//	public void testEmbedded() throws Exception {
//		Assert.assertNotNull(entityManagerFactory);
//		
//		EntityWithEmbedded entity = new EntityWithEmbedded();
//		entity.setEmbedded(new EntityEmbedded("Jack", "Dalton"));
//		entity.setAge(12);
//		entity.setPhones(Arrays.asList("0123456789", "9876543210"));
//		
//		persistEntity(entity);
//		
//		entity = findEntity(EntityWithEmbedded.class, entity.getId());
//		
//		ManagedType<?> managedType = entityManagerFactory.getMetamodel().managedType(EntityWithEmbedded.class);
//		for (Attribute<?, ?> attribute : managedType.getAttributes()) {
//			System.out.println(attribute);
//		}
//	}

	
	private <T> T clientEncodeServerDecodeMerge(SpearalFactory clientFactory, SpearalFactory serverFactory, Object clientEntity, Class<?> filterClass, String... properties) throws IOException {
		T serverEntity = clientEncodeServerDecode(clientFactory, serverFactory, clientEntity, filterClass, properties);
		return mergeEntity(serverEntity);
	}
	
	private <T> T clientEncodeServerDecode(SpearalFactory clientFactory, SpearalFactory serverFactory, Object clientEntity, Class<?> filterClass, String... properties) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		SpearalEncoder clientEncoder = clientFactory.newEncoder(out);
		try {
			clientEncoder.getPropertyFilter().add(filterClass, properties);
			clientEncoder.writeAny(clientEntity);
		}
		catch (IOException e) {
			Assert.fail(e.toString());
		}
		byte[] buf = out.toByteArray();
		
		SpearalDecoder serverDecoder = serverFactory.newDecoder(new ByteArrayInputStream(buf));
		return serverDecoder.readAny(clientEntity.getClass());
	}
	
	private <T> T findEntity(Class<T> serverEntityClass, Object id) {
		return findEntity(serverEntityClass, id, null);
	}
	
	private <T> T findEntity(Class<T> serverEntityClass, Object id, String getterToInitialize) {
		EntityManager entityManager = entityManagerFactory.createEntityManager();
		EntityTransaction tx = entityManager.getTransaction();
		tx.begin();
		T savedEntity = entityManager.find(serverEntityClass, id);
		if (savedEntity != null && getterToInitialize != null) {
			try {
				Hibernate.initialize(serverEntityClass.getMethod(getterToInitialize).invoke(savedEntity));
			}
			catch (Exception e) {
				throw new RuntimeException("Could not initialize: " + getterToInitialize, e);
			}
		}
		entityManager.flush();
		tx.commit();
		return savedEntity;
	}
	
	private void persistEntity(Object entity) {
		EntityManager entityManager = entityManagerFactory.createEntityManager();
		EntityTransaction tx = entityManager.getTransaction();
		tx.begin();
		entityManager.persist(entity);
		entityManager.flush();
		tx.commit();
		entityManager.close();
	}
	
	private <T> T mergeEntity(T serverEntity) {
		EntityManager entityManager = entityManagerFactory.createEntityManager();
		EntityTransaction tx = entityManager.getTransaction();
		tx.begin();
		T savedEntity = entityManager.merge(serverEntity);
		entityManager.flush();
		tx.commit();
		entityManager.close();
		return savedEntity;
	}
}
