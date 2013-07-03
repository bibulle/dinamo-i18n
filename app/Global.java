import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import models.Property;
import models.Value;
import play.Application;
import play.Configuration;
import play.GlobalSettings;
import play.Logger;
import play.libs.Akka;
import play.libs.WS;
import scala.concurrent.duration.Duration;

import com.avaje.ebean.Expr;
import com.memetix.mst.language.Language;
import com.memetix.mst.translate.Translate;

import controllers.Properties;
//import com.memetix.mst.language.Language;
//import com.memetix.mst.translate.Translate;

public class Global extends GlobalSettings {

	public static int port = 81;
	
	@Override
	public void onStart(Application app) {
		
		// Loading default data
//		if (Property.find.findRowCount() == 0) {
//			@SuppressWarnings("unchecked")
//			Map<String, List<Object>> all = (Map<String, List<Object>>) Yaml.load("initial-data.yml");
//			Ebean.save(all.get("properties"));
//
//			List<Property> props = Property.find.all();
//			for (Property property : props) {
//				property.save();
//			}
//		}

		// time to save ?
		Akka.system().scheduler().schedule(Duration.create(0, TimeUnit.MILLISECONDS), Duration.create(1, TimeUnit.HOURS), new Runnable() {
			public void run() {
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd");
				String repName = "sav" + File.separator + sdf.format(new Date());

				File f = new File(repName);
				if (!f.exists()) {
					f.mkdirs();
					Logger.info("Create " + repName);

					// Foreach langs
					for (String local : Properties.locals) {
						OutputStream os = null;
						try {
							InputStream is = WS.url("http://localhost:"+port+"/download/" + local + "/strings").get().get().getBodyAsStream();

							os = new FileOutputStream(new File(f, "Localizable.strings_" + local + ".properties"));

							int read = 0;
							byte[] bytes = new byte[1024];

							while ((read = is.read(bytes)) != -1) {
								os.write(bytes, 0, read);
							}
						} catch (IOException e) {
							Logger.error("Error during save : " + e.getMessage());
						} finally {
							if (os != null) {
								try {
									os.close();
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
						}

					}
				}

			}
		}, Akka.system().dispatcher());

		// translate automaticaly...
		Boolean launchTranslation = Configuration.root().getBoolean("translation.automatic");
		if ((launchTranslation != null) && launchTranslation) {
			launchTranslation();
		}

	}

	public void launchTranslation() {
		Akka.system().scheduler().scheduleOnce(Duration.create(10, TimeUnit.SECONDS), new Runnable() {
			public void run() {
				//Logger.info("Launched " + new Date());
				List<Value> values = Value.find.where().or(Expr.isNull("value"), Expr.eq("value", "")).findList();
				if (!values.isEmpty()) {
					//Logger.info("values.size() " + values.size());
					Value value = values.get((int) (Math.random() * values.size()));
					//Logger.info("value.id " + value.id + " " + value.property);

					Property property = value.property;
					String srcText = "";
					String srcLang = null;
					String trgLang = null;

					for (int i = 0; i < property.values.size(); i++) {
						if ((srcLang == null) && (property.values.get(i).value != null) && (!property.values.get(i).value.trim().isEmpty())) {
							srcLang = Properties.locals[i];
							srcText = property.values.get(i).value;
						}
						if (property.values.get(i).equals(value)) {
							trgLang = Properties.locals[i];
						}
					}

					if ((srcLang != null) && (trgLang != null)) {
						if (trgLang.equals("zh")) {
							trgLang = "zh-CHS";
						}
						try {
							Translate.setClientId("dinamo");
							Translate.setClientSecret("bAMtLtju1FwWdg3RUa4Q/1n8ZjCQM6RkgrnumssADF4=");

							String translatedText = Translate.execute(srcText, Language.fromString(srcLang), Language.fromString(trgLang));
							Logger.info(srcText + " (" + srcLang + ") -> '" + translatedText+"'("+trgLang+")");

							value.value = translatedText;
							value.temporary = true;
							property.update();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}

				launchTranslation();
			}
		}, Akka.system().dispatcher());

	}
	
}
