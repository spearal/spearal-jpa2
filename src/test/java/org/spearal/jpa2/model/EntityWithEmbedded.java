package org.spearal.jpa2.model;

import java.io.Serializable;
import java.util.List;

import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
public class EntityWithEmbedded implements Serializable {

	private static final long serialVersionUID = 1L;

	@Embeddable
	public static class EntityEmbedded implements Serializable {
		
		private static final long serialVersionUID = 1L;

		private String firstName;
		private String lastName;
		
		public EntityEmbedded() {
		}

		public EntityEmbedded(String firstName, String lastName) {
			this.firstName = firstName;
			this.lastName = lastName;
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
	}
	
	@Id @GeneratedValue
	private Long id;
	
	private int age;
	
	@Embedded
	private EntityEmbedded embedded;
	
	@ElementCollection
	private List<String> phones;
	
	public EntityWithEmbedded() {
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	public EntityEmbedded getEmbedded() {
		return embedded;
	}

	public void setEmbedded(EntityEmbedded embedded) {
		this.embedded = embedded;
	}

	public List<String> getPhones() {
		return phones;
	}

	public void setPhones(List<String> phones) {
		this.phones = phones;
	}
}
