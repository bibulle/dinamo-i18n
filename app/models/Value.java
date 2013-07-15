package models;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.validation.constraints.NotNull;

import org.codehaus.jackson.annotate.JsonIgnore;

import play.data.validation.Constraints.Required;
import play.db.ebean.Model;

@Entity
public class Value extends Model {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2004206307561966884L;


	@Id
	public Long id;

	public int orderKey;
	
	public String value = "";
	
	public Date updateDate = new Date();
	
	public boolean temporary = false;
	
	@ManyToOne
	@Required
	@NotNull
	@JsonIgnore
	public Property property;

	public transient boolean recent = false;

	@Override
	public void save() {
		// TODO Auto-generated method stub
		//Logger.info("Value save");
		super.save();
	}
	
	/**
	 * Calc recent 
	 */
	void calcRecent() {
		recent = ((System.currentTimeMillis() - updateDate.getTime()) < Property.RECENT_DELTA_MILLI);
		//System.out.println(recent+" "+System.currentTimeMillis()+" "+updateDate.getTime()+" "+(System.currentTimeMillis() - updateDate.getTime()));
		
	}

	/**
	 * finder
	 */
	public static Finder<Long, Value> find = new Finder<Long, Value>(Long.class, Value.class);

}
