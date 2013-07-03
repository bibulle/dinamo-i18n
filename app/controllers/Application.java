package controllers;

import play.mvc.*;

import views.html.*;

public class Application extends Controller {
  
	/**
	 * This result directly redirect to application home.
	 */
	public static Result GO_HOME = redirect(routes.Application.index());

	public static Result index() {
		
    return ok(index.render());
  }
  
}