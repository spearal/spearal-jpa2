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
import java.util.HashSet;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.spearal.DefaultSpearalFactory;
import org.spearal.SpearalDecoder;
import org.spearal.SpearalEncoder;
import org.spearal.SpearalFactory;
import org.spearal.configuration.PartialObjectFactory.PartialObjectProxy;
import org.spearal.jpa2.impl.ProxyMerger;
import org.spearal.jpa2.model.Contact;
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
		ProxyMerger proxyMerger = new ProxyMerger(entityManager);
		Assert.assertFalse("Simple object", proxyMerger.isProxy(serverPerson1));
		
		Contact serverContact1 = new Contact(serverPerson1, "bla", "06");
		serverPerson1.setContacts(new HashSet<Contact>());
		serverPerson1.getContacts().add(serverContact1);
		Assert.assertFalse("Graph object", proxyMerger.isProxy(serverPerson1));

		Person clientPerson2 = new Person("Bruce", "Willis");
		Person serverPerson2 = clientEncodeServerDecode(clientFactory, serverFactory, clientPerson2, Person.class, "id", "version", "firstName");		
		Assert.assertTrue("Proxy object", proxyMerger.isProxy(serverPerson2));
		
		Person clientPerson3 = new Person("Bruce", "Willis");
		clientPerson3.setContacts(new HashSet<Contact>());
		Contact clientContact3 = new Contact(clientPerson3, "blo", "06");
		clientPerson3.getContacts().add(clientContact3);
		Person serverPerson3 = clientEncodeServerDecode(clientFactory, serverFactory, clientPerson3, Contact.class, "id", "version", "person", "mobile");
		Assert.assertFalse("Non proxy graph root", serverPerson3 instanceof PartialObjectProxy);
		Assert.assertTrue("Proxy graph", proxyMerger.isProxy(serverPerson3));
		
		entityManager.close();
	}
	
	
	@Test
	public void testMergeProxy() throws IOException {
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
		clientPerson.setFirstName("John");
		clientPerson.setLastName("McLane");
		
		serverPerson = clientEncodeServerDecodeMerge(clientFactory, serverFactory, clientPerson, Person.class, "id", "version", "firstName");
		
		Assert.assertEquals("Person firstName", clientPerson.getFirstName(), serverPerson.getFirstName());
		
		serverPerson = findEntity(Person.class, serverPerson.getId());
		
		Assert.assertEquals("Person firstName", clientPerson.getFirstName(), serverPerson.getFirstName());
		Assert.assertEquals("Version 1", Long.valueOf(1L), serverPerson.getVersion());
		
		Person clientFriend = new Person("Hans", "Gruber");
		clientPerson.setBestFriend(clientFriend);
		
		serverPerson = clientEncodeServerDecodeMerge(clientFactory, serverFactory, clientPerson, Person.class, "id", "version", "firstName", "bestFriend");
		
		Assert.assertEquals("Friend firstName", clientFriend.getFirstName(), serverPerson.getBestFriend().getFirstName());
		Assert.assertEquals("Version 0", Long.valueOf(0L), serverPerson.getBestFriend().getVersion());
	}

	
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
		EntityManager entityManager = entityManagerFactory.createEntityManager();
		EntityTransaction tx = entityManager.getTransaction();
		tx.begin();
		T savedEntity = entityManager.find(serverEntityClass, id);
		entityManager.flush();
		tx.commit();
		return savedEntity;
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
