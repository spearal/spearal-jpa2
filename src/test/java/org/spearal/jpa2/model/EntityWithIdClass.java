package org.spearal.jpa2.model;

import java.io.Serializable;
import java.util.List;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;

@Entity
@IdClass(EntityWithIdClass.EntityIdClass.class)
public class EntityWithIdClass implements Serializable {

	private static final long serialVersionUID = 1L;

	public static class EntityIdClass implements Serializable {
		
		private static final long serialVersionUID = 1L;

		private String firstName;
		private String lastName;
		
		public EntityIdClass() {
		}

		public EntityIdClass(String firstName, String lastName) {
			this.firstName = firstName;
			this.lastName = lastName;
		}

		public String getFirstName() {
			return firstName;
		}

		public String getLastName() {
			return lastName;
		}
	}
	
	@Id
	private String firstName;

	@Id
	private String lastName;
	
	private int age; 
	
	@ElementCollection
	private List<String> phones;
	
	public EntityWithIdClass() {
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	public List<String> getPhones() {
		return phones;
	}

	public void setPhones(List<String> phones) {
		this.phones = phones;
	}
}
