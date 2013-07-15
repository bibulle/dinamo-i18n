package models;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;

import controllers.Properties;

import play.Logger;
import play.db.ebean.Model;
import play.libs.Akka;
import scala.concurrent.duration.Duration;

@Entity
public class Property extends Model {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2447886977934109106L;

	private static final int NB_LANGAGE = 9;

	@Id
	public Long id;

	@Column(unique = true)
	public String akey;

	public Date updateDate;

	@OneToMany(cascade = { CascadeType.PERSIST, CascadeType.REMOVE })
	@OrderBy("orderKey ASC")
	public List<Value> values;

	public transient boolean recent = false;

	static final long RECENT_DELTA_MILLI = 10 * 1000;

	@Override
	public void save() {
		//System.err.println("Save " + this);

		if (values.size() > NB_LANGAGE) {
			values = values.subList(0, NB_LANGAGE);
		}
		while (values.size() < NB_LANGAGE) {
			values.add(new Value());
		}

		int cpt = 0;
		for (Value value : values) {
			value.orderKey = cpt;
			cpt++;
		}

		updateDate = new Date();
		super.save();

		calcRecent();
		Properties.sendNews("save", this);

	}

	@Override
	public void update() {
		if (this.akey.startsWith("A3")) System.err.println("Update " + this);
		boolean isChanged = ((_ebean_intercept().getChangedProps() != null) && (_ebean_intercept().getChangedProps().size() != 0));
		if (this.akey.startsWith("A3"))  Logger.info("update "+isChanged+" "+_ebean_intercept().getChangedProps());

		for (Value value : values) {
			boolean vIsChanged = ((value._ebean_intercept().getChangedProps() != null) && (value._ebean_intercept().getChangedProps().size() != 0));
			isChanged = isChanged || vIsChanged;
			if (this.akey.startsWith("A3"))  Logger.info("update "+isChanged+" "+value._ebean_intercept().getChangedProps());
			if (vIsChanged) {
				value.updateDate = new Date();
			}
		}

		if (isChanged) {
			if (this.akey.startsWith("A3")) Logger.info("++++++++++++update++++++++++");
			updateDate = new Date();
			super.update();
		}

		calcRecent();
		Properties.sendNews("save", this);
	}

	@Override
	public void delete() {
		//System.err.println("Delete " + this);
		super.delete();

		Properties.sendNews("delete", this);
	}

	/**
	 * Calc recent
	 */
	private void calcRecent() {
		long delta = (System.currentTimeMillis() - updateDate.getTime());
		recent = (delta < RECENT_DELTA_MILLI);
		// System.out.println(recent+" "+System.currentTimeMillis()+" "+updateDate.getTime()+" "+delta);
		for (Value value : values) {
			value.calcRecent();
		}
		if (recent) {
			Akka.system().scheduler().scheduleOnce(Duration.create(RECENT_DELTA_MILLI - delta + 500, TimeUnit.MILLISECONDS), new Runnable() {

				@Override
				public void run() {
					Property property = Property.findById(id);
					if (property.updateDate.getTime() == updateDate.getTime()) {
						update();
					}
				}
			}, Akka.system().dispatcher());

		}
	}

	/**
	 * finder
	 */
	private static Finder<Long, Property> find = new Finder<Long, Property>(Long.class, Property.class);

	public static int findCountNewer(Calendar cal) {
		return Property.find.where().gt("updateDate", cal.getTime()).findRowCount();
	}

	public static Property findById(Long aId) {
		Property property = Property.find.byId(aId);
		if (property != null) {
			property.calcRecent();
		}
		return property;
	}

	public static Property findByKey(String key) {
		Property property = find.where().eq("akey", key).findUnique();
		if (property != null) {
			property.calcRecent();
		}
		return property;
	}

	public static List<Property> findAllOrderByKey() {
		List<Property> properties = Property.find.orderBy("akey").findList();
		for (Property property : properties) {
			property.calcRecent();
		}
		return properties;
	}

	public static List<Property> findOrderByUpdateDate(long lastUpdateDate, int count) {
		List<Property> properties = Property.find.where().gt("updateDate", new Date(lastUpdateDate)).orderBy("updateDate").setMaxRows(count).findList();
		for (Property property : properties) {
			property.calcRecent();
		}
		return properties;
	}

}
