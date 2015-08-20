package cz.cuni.mff.ufal.lindat.utilities.hibernate;

import java.io.Serializable;

public class CmdiProfile extends GenericEntity implements Serializable{

	private static final long serialVersionUID = 1L;

	private int profileId;
	private String clarinId;
	private String name;
	private String form;
	
	public CmdiProfile() {
	}
	
	public CmdiProfile(String clarinId, String name, String form) {
		super();
		this.clarinId = clarinId;
		this.name = name;
		this.form = form;
	}

	public int getProfileId() {
		return profileId;
	}

	public void setProfileId(int profileId) {
		this.profileId = profileId;
	}

	public String getClarinId() {
		return clarinId;
	}

	public void setClarinId(String clarinId) {
		this.clarinId = clarinId;
	}


	public String getName() {
		return name;
	}


	public void setName(String name) {
		this.name = name;
	}


	public String getForm() {
		return form;
	}


	public void setForm(String form) {
		this.form = form;
	}


	@Override
	public int getID() {
		return profileId;
	}

}
