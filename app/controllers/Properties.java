package controllers;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import models.Property;
import models.Value;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.node.POJONode;

import play.libs.F.*;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.MultipartFormData.FilePart;
import play.mvc.Result;
import play.mvc.WebSocket;
import views.html.properties;
import views.html.xml;

public class Properties extends Controller {

	// private static Gson gson = new
	// GsonBuilder().excludeFieldsWithModifiers(Modifier.STATIC).excludeFieldsWithoutExposeAnnotation().setPrettyPrinting().create();

	private static ObjectWriter objectWriter = (new ObjectMapper()).writerWithDefaultPrettyPrinter();

	public static String[] langs = { "English", "French", "Spanish", "German", "Japanese", "Korean", "Portuguese", "Italian", "Chinese" };
	public static String[] locals = { "en", "fr", "es", "de", "ja", "ko", "pt", "it", "zh" };

	/**
	 * List all the properties
	 * 
	 * @return
	 */
	public static Result list() {
		// Get the properties
		List<Property> list = Property.findAllOrderByKey();

		try {
			return ok(objectWriter.writeValueAsString(list));

		} catch (JsonGenerationException e) {
			e.printStackTrace();
			return badRequest(e.getMessage());
		} catch (JsonMappingException e) {
			e.printStackTrace();
			return badRequest(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			return badRequest(e.getMessage());
		}
	}

	/**
	 * get a property
	 * 
	 * @return
	 */
	public static Result get(Long id) {
		Property property = Property.findById(id);
		try {
			return ok(objectWriter.writeValueAsString(property));

		} catch (JsonGenerationException e) {
			e.printStackTrace();
			return badRequest(e.getMessage());
		} catch (JsonMappingException e) {
			e.printStackTrace();
			return badRequest(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			return badRequest(e.getMessage());
		}
	}

	/**
	 * create a property
	 * 
	 * @return
	 */
	public static Result create() {
		try {
			JsonNode json = request().body().asJson();

			ObjectMapper mapper = new ObjectMapper();
			// mapper.set

			// System.err.println(json);
			Property newProperty = mapper.readValue(json, Property.class);

			// System.err.println(json);
			// System.err.println(newProperty);
			// System.err.println(newProperty.id);
			// System.err.println("4");

			// Logger.info("create "+newProperty);
			// Logger.info("create "+newProperty.id);

			if ((newProperty.id != null) && (newProperty.id > 0)) {
				Property oldProperty = Property.findById(newProperty.id);
				oldProperty.akey = newProperty.akey;
				oldProperty.updateDate = newProperty.updateDate;
				for (int i = 0; i < newProperty.values.size(); i++) {
					Value oldval = oldProperty.values.get(i);
					Value newVal = newProperty.values.get(i);
					oldval.orderKey = newVal.orderKey;
					oldval.temporary = newVal.temporary;
					if ((oldval.value == null) && (newVal.value != null)) {
						oldval.temporary = false;
					} else if ((oldval.value != null) && (newVal.value == null)) {
						oldval.temporary = false;
					} else if ((oldval.value != null) && (newVal.value != null)) {
						if (!oldval.value.equals(newVal.value)) {
							oldval.temporary = false;
						}
					}
					oldval.updateDate = newVal.updateDate;
					oldval.value = newVal.value;
				}
				oldProperty.update();
				newProperty = oldProperty;
			} else {
				newProperty.save();
			}

			return ok(objectWriter.writeValueAsString(newProperty));
		} catch (JsonParseException e) {
			e.printStackTrace();
			return badRequest(e.getMessage());
		} catch (JsonMappingException e) {
			e.printStackTrace();
			return badRequest(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			return badRequest(e.getMessage());
		}

	}

	/**
	 * update a property
	 * 
	 * @return
	 */
	public static Result delete(Long id) {
		try {
			Property dbProperty = Property.findById(id);
			dbProperty.delete();
			return ok(objectWriter.writeValueAsString("success"));
		} catch (JsonParseException e) {
			e.printStackTrace();
			return badRequest(e.getMessage());
		} catch (JsonMappingException e) {
			e.printStackTrace();
			return badRequest(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			return badRequest(e.getMessage());
		}
	}

	/**
	 * load a file
	 * 
	 * @return
	 */
	public static Result uploadFiles() {
		try {
			MultipartFormData body = request().body().asMultipartFormData();
			List<FilePart> files = body.getFiles();

			for (FilePart file : files) {
				// Check file name
				if (!file.getFilename().endsWith(".properties")) {
					System.err.println("File should be a '.properties' (" + file.getFilename() + ")");
					return badRequest("File should be a '.properties' (" + file.getFilename() + ")");
				}

				// Check language
				int i = 0;
				for (String local : locals) {
					if (file.getFilename().endsWith("_" + local + ".properties")) {
						break;
					}
					i++;
				}
				if (i >= locals.length) {
					System.err.println("File should be in a known language (" + file.getFilename() + ", " + Arrays.asList(locals) + ")");
					return badRequest("File should be in a known language (" + file.getFilename() + ", " + Arrays.asList(locals) + ")");
				}

				// Read file
				Pattern p = Pattern.compile("\"(.*)\" = \"(.*)\";");
				Scanner fileScanner = new Scanner(file.getFile());
				while (fileScanner.hasNextLine()) {
					String line = fileScanner.nextLine();

					Matcher matcher = p.matcher(line);

					if (matcher.find()) {
						// Save propertie
						String key = matcher.group(1);
						String valueS = matcher.group(2);

						Property property = Property.findByKey(key);
						if (property == null) {
							property = new Property();
							property.akey = key;
							property.save();
						}
						Value value = property.values.get(i);
						value.value = valueS;
						property.update();
					}
				}
				fileScanner.close();

			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return badRequest(e.getMessage());
		}
		return ok();
	}

	/**
	 * Download a file
	 * 
	 * @return
	 */
	public static Result downloadValidatedFiles(String language, String format) {
		return downloadFiles(language, format, false);
	}

	/**
	 * Download a file
	 * 
	 * @return
	 */
	public static Result downloadAllFiles(String language, String format) {
		return downloadFiles(language, format, true);
	}

	/**
	 * Download a file
	 * 
	 * @return
	 */
	private static Result downloadFiles(String language, String format, boolean withTemporary) {
		int lang_index = -1;
		for (int i = 0; i < locals.length; i++) {
			if (locals[i].equalsIgnoreCase(language)) {
				lang_index = i;
			}
		}
		if (lang_index < 0) {
			return badRequest("Wrong language (" + language + ")");
		}

		// Get the properties
		List<Property> list = Property.findAllOrderByKey();

		for (Property property : list) {
			if (property.values.get(lang_index).value == null) {
				property.values.get(lang_index).value = "";
			} else {
				if (!withTemporary) {
					if (property.values.get(lang_index).temporary) {
						property.values.get(lang_index).value = "";
					}
				}
			}
		}

		response().setContentType("application/x-download");
		if (format.equalsIgnoreCase("strings")) {
			response().setHeader("Content-disposition", "attachment; filename=localizable.strings_" + language + ".properties");
			return ok(properties.render(langs[lang_index], lang_index, list));
		} else {
			response().setHeader("Content-disposition", "attachment; filename=localizable.strings_" + language + ".xml");
			return ok(xml.render(langs[lang_index], lang_index, list));
		}
	}

	private static Map<WebSocket.In<JsonNode>, WebSocket.Out<JsonNode>> webSocketOuts = new HashMap<WebSocket.In<JsonNode>, WebSocket.Out<JsonNode>>();

	public static WebSocket<JsonNode> webSocket() {
		return new WebSocket<JsonNode>() {

			@Override
			public void onReady(final WebSocket.In<JsonNode> in, final WebSocket.Out<JsonNode> out) {

				in.onClose(new Callback0() {
					@Override
					public void invoke() throws Throwable {
						webSocketOuts.remove(in);
					}
				});

				in.onMessage(new Callback<JsonNode>() {
					@Override
					public void invoke(JsonNode event) throws Throwable {
						// System.out.println("############" + event);
						String action = event.get("action").asText();
						// System.out.println(action);

						if (action.equalsIgnoreCase("refresh")) {
							try {
								// System.out.println("let's go");
								long lastUpdateDate = (event.get("lastUpdateDate") == null ? 0 : event.get("lastUpdateDate").asLong(0));
								int count = (event.get("count") == null ? 10 : event.get("count").asInt(10));
								// System.out.println(lastUpdateDate);
								// System.out.println(count);
								// Send all
								// System.out.println("============Debut");
								List<Property> properties = Property.findOrderByUpdateDate(lastUpdateDate, count);
								for (Property property : properties) {
									sendNews("save", property);
								}
								if (properties.isEmpty()) {
									ObjectNode noRefreshEvent = Json.newObject();
									noRefreshEvent.put("action", "noRefreshNeeded");
									out.write(noRefreshEvent);
								}
								// System.out.println("============Fin");
							} catch (Exception e) {
								e.printStackTrace();
							}
						}

					}
				});

				webSocketOuts.put(in, out);

			}

		};

	}

	public static void sendNews(String action, Property property) {
		// try {
		ObjectNode event = Json.newObject();
		event.put("action", action);
		// event.put("property", objectWriter.writeValueAsString(property));
		event.put("property", Json.toJson(property));

		for (WebSocket.Out<JsonNode> out : webSocketOuts.values()) {
			out.write(event);
		}
		// } catch (IOException e) {
		// e.printStackTrace();
		// }
	}

}