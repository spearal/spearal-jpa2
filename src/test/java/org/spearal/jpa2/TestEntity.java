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

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.hibernate.LazyInitializationException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.spearal.DefaultSpearalFactory;
import org.spearal.SpearalDecoder;
import org.spearal.SpearalEncoder;
import org.spearal.SpearalFactory;
import org.spearal.configuration.PartialObjectFactory.UndefinedPropertyException;
import org.spearal.jpa2.model.Person;

/**
 * @author Franck WOLFF
 */
public class TestEntity extends AbstractHibernate4TestUnit {

	@Before
	public void setUp() throws Exception {
		super.setUp();
	}

	@After
	public void tearDown() throws Exception {
		super.tearDown();
	}

	@Test
	public void test() {
		Assert.assertNotNull(entityManagerFactory);
		
		EntityManager manager = entityManagerFactory.createEntityManager();
		EntityTransaction transaction = manager.getTransaction();
		transaction.begin();

		Person friend = new Person("Jim", "Smith");
		manager.persist(friend);

		Person person = new Person("John", "Doo");
		person.setBestFriend(friend);
		manager.persist(person);
		
		manager.flush();
		transaction.commit();
		manager.close();
		
		transaction = null;
		manager = null;
		
		Assert.assertNotNull(person.getId());
		
		manager = entityManagerFactory.createEntityManager();
		transaction = manager.getTransaction();
		transaction.begin();
		
		person = manager.find(Person.class, person.getId());
		
		manager.flush();
		transaction.commit();
		manager.close();
		
		transaction = null;
		manager = null;
		
		Assert.assertNotNull(person);
		Assert.assertEquals("John", person.getFirstName());
		Assert.assertEquals("Doo", person.getLastName());
		
		try {
			person.getBestFriend().getFirstName();
			Assert.fail("Should throw a LazyInitializationException");
		}
		catch (LazyInitializationException e) {
		}
		try {
			person.getContacts().size();
			Assert.fail("Should throw a LazyInitializationException");
		}
		catch (LazyInitializationException e) {
		}
		
		SpearalFactory factory = new DefaultSpearalFactory();
		SpearalConfigurator.init(factory, entityManagerFactory);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		SpearalEncoder encoder = factory.newEncoder(out);
		try {
			encoder.writeAny(person);
		}
		catch (IOException e) {
			Assert.fail(e.toString());
		}
		
		byte[] bytes = out.toByteArray();
		
		// Do not load any services here (pseudo-client application).
		factory = new DefaultSpearalFactory(false);
		SpearalDecoder decoder = factory.newDecoder(new ByteArrayInputStream(bytes));
		try {
			decoder.printAny(factory.newPrinter(System.out));
			System.out.println();
		}
		catch (IOException e) {
			Assert.fail(e.toString());
		}
		
		Person clone = null;
		
		decoder = factory.newDecoder(new ByteArrayInputStream(bytes));
		try {
			clone = decoder.readAny(Person.class);
		}
		catch (IOException e) {
			Assert.fail(e.toString());
		}
		
		Assert.assertNotNull(clone);
		Assert.assertEquals(person.getId(), clone.getId());
		Assert.assertEquals(person.getUid(), clone.getUid());
		Assert.assertEquals(person.getVersion(), clone.getVersion());
		Assert.assertEquals(person.getFirstName(), clone.getFirstName());
		Assert.assertEquals(person.getLastName(), clone.getLastName());
		try {
			clone.getBestFriend();
			Assert.fail("Should throw a UndefinedPropertyException");
		}
		catch (UndefinedPropertyException e) {
		}
		try {
			clone.getContacts();
			Assert.fail("Should throw a UndefinedPropertyException");
		}
		catch (UndefinedPropertyException e) {
		}
	}
}
