package models;

import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;

import play.db.ebean.Model;

@Entity
public class Property extends Model {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2447886977934109106L;
	
	
	private static final int NB_LANGAGE = 9;
	
	@Id
	public Long id;

	
	@Column(unique=true)
	public String akey;
	
	public Date updateDate;

	@OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REMOVE})
  @OrderBy("orderKey ASC")
	public List<Value> values;
	
  @Override
  public void save() {
  	//System.err.println("Save "+this);
  	
  	if (values.size() > NB_LANGAGE) {
  		values = values.subList(0, NB_LANGAGE);
  	}
  	while (values.size() < NB_LANGAGE) {
  		values.add(new Value());
  	}
  	
  	int cpt = 0;
  	for (Value value : values) {
			value.orderKey=cpt;
			cpt++;
		}

		updateDate = new Date();
		super.save();
  	
  }
	
  @Override
  public void update() {
		boolean isChanged = ((_ebean_intercept().getChangedProps() != null) && (_ebean_intercept().getChangedProps().size() != 0));
		//Logger.info("update "+isChanged+" "+_ebean_intercept().getChangedProps());
		
		for (Value value : values) {
			boolean vIsChanged = ((value._ebean_intercept().getChangedProps() != null) && (value._ebean_intercept().getChangedProps().size() != 0));
			isChanged = isChanged || vIsChanged;
			if (vIsChanged) {
				value.updateDate = new Date();
			}
		}
		
		if (isChanged) {
			//Logger.info("++++++++++++update++++++++++");
			updateDate = new Date();
			super.update();
		}
  }
	
	/**
	 * finder
	 */
	public static Finder<Long, Property> find = new Finder<Long, Property>(Long.class, Property.class);
	
}
