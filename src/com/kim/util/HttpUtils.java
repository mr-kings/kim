package com.kim.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLStreamHandler;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestWrapper;
import javax.servlet.ServletResponse;
import javax.servlet.ServletResponseWrapper;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;

public class HttpUtils {

	private static boolean hasSunHttpHandler = true;
	static {
		try {
			Class.forName("sun.net.www.protocol.http.Handler");
			Class.forName("sun.net.www.protocol.https.Handler");
		} catch (ClassNotFoundException e) {
			hasSunHttpHandler  = false;
		}
	}
	
	/**
	 * Standard Servlet 2.3+ spec request attributes for include URI and paths.
	 * <p>
	 * If included via a RequestDispatcher, the current resource will see the originating request. Its own URI and paths are exposed as request attributes.
	 */
	public static final String INCLUDE_REQUEST_URI_ATTRIBUTE = "javax.servlet.include.request_uri";
	public static final String INCLUDE_CONTEXT_PATH_ATTRIBUTE = "javax.servlet.include.context_path";
	public static final String INCLUDE_SERVLET_PATH_ATTRIBUTE = "javax.servlet.include.servlet_path";
	public static final String INCLUDE_PATH_INFO_ATTRIBUTE = "javax.servlet.include.path_info";
	public static final String INCLUDE_QUERY_STRING_ATTRIBUTE = "javax.servlet.include.query_string";

	/**
	 * Standard Servlet 2.4+ spec request attributes for forward URI and paths.
	 * <p>
	 * If forwarded to via a RequestDispatcher, the current resource will see its own URI and paths. The originating URI and paths are exposed as request
	 * attributes.
	 */
	public static final String FORWARD_REQUEST_URI_ATTRIBUTE = "javax.servlet.forward.request_uri";
	public static final String FORWARD_CONTEXT_PATH_ATTRIBUTE = "javax.servlet.forward.context_path";
	public static final String FORWARD_SERVLET_PATH_ATTRIBUTE = "javax.servlet.forward.servlet_path";
	public static final String FORWARD_PATH_INFO_ATTRIBUTE = "javax.servlet.forward.path_info";
	public static final String FORWARD_QUERY_STRING_ATTRIBUTE = "javax.servlet.forward.query_string";

	/**
	 * Standard Servlet 2.3+ spec request attributes for error pages.
	 * <p>
	 * To be exposed to JSPs that are marked as error pages, when forwarding to them directly rather than through the servlet container's error page resolution
	 * mechanism.
	 */
	public static final String ERROR_STATUS_CODE_ATTRIBUTE = "javax.servlet.error.status_code";
	public static final String ERROR_EXCEPTION_TYPE_ATTRIBUTE = "javax.servlet.error.exception_type";
	public static final String ERROR_MESSAGE_ATTRIBUTE = "javax.servlet.error.message";
	public static final String ERROR_EXCEPTION_ATTRIBUTE = "javax.servlet.error.exception";
	public static final String ERROR_REQUEST_URI_ATTRIBUTE = "javax.servlet.error.request_uri";
	public static final String ERROR_SERVLET_NAME_ATTRIBUTE = "javax.servlet.error.servlet_name";

	/**
	 * Prefix of the charset clause in a content type String: ";charset="
	 */
	public static final String CONTENT_TYPE_CHARSET_PREFIX = ";charset=";

	/**
	 * Default character encoding to use when {@code request.getCharacterEncoding} returns {@code null}, according to the Servlet spec.
	 * @see ServletRequest#getCharacterEncoding
	 */
	public static final String DEFAULT_CHARACTER_ENCODING = "ISO-8859-1";

	/**
	 * Standard Servlet spec context attribute that specifies a temporary directory for the current web application, of type {@code java.io.File}.
	 */
	public static final String TEMP_DIR_CONTEXT_ATTRIBUTE = "javax.servlet.context.tempdir";

	/**
	 * HTML escape parameter at the servlet context level (i.e. a context-param in {@code web.xml}): "defaultHtmlEscape".
	 */
	public static final String HTML_ESCAPE_CONTEXT_PARAM = "defaultHtmlEscape";

	/**
	 * Web app root key parameter at the servlet context level (i.e. a context-param in {@code web.xml}): "webAppRootKey".
	 */
	public static final String WEB_APP_ROOT_KEY_PARAM = "webAppRootKey";

	/** Default web app root key: "webapp.root" */
	public static final String DEFAULT_WEB_APP_ROOT_KEY = "webapp.root";

	/** Name suffixes in case of image buttons */
	public static final String[] SUBMIT_IMAGE_SUFFIXES = { ".x", ".y" };

	/** Key for the mutex session attribute */
	public static final String SESSION_MUTEX_ATTRIBUTE = HttpUtils.class.getName() + ".MUTEX";

	/**
	 * Set a system property to the web application root directory. The key of the system property can be defined with the "webAppRootKey" context-param in
	 * {@code web.xml}. Default is "webapp.root".
	 * <p>
	 * Can be used for tools that support substition with {@code System.getProperty} values, like log4j's "${key}" syntax within log file locations.
	 * @param servletContext the servlet context of the web application
	 * @throws IllegalStateException if the system property is already set, or if the WAR file is not expanded
	 * @see #WEB_APP_ROOT_KEY_PARAM
	 * @see #DEFAULT_WEB_APP_ROOT_KEY
	 * @see WebAppRootListener
	 * @see Log4jWebConfigurer
	 */
	public static void setWebAppRootSystemProperty(ServletContext servletContext) throws IllegalStateException {
		Assert.notNull(servletContext, "ServletContext must not be null");
		String root = servletContext.getRealPath("/");
		if (root == null) {
			throw new IllegalStateException("Cannot set web app root system property when WAR file is not expanded");
		}
		String param = servletContext.getInitParameter(WEB_APP_ROOT_KEY_PARAM);
		String key = (param != null ? param : DEFAULT_WEB_APP_ROOT_KEY);
		String oldValue = System.getProperty(key);
		if (oldValue != null && !StringUtils.pathEquals(oldValue, root)) {
			throw new IllegalStateException("Web app root system property already set to different value: '" + key + "' = [" + oldValue + "] instead of ["
					+ root + "] - " + "Choose unique values for the 'webAppRootKey' context-param in your web.xml files!");
		}
		System.setProperty(key, root);
		servletContext.log("Set web app root system property: '" + key + "' = [" + root + "]");
	}

	/**
	 * Remove the system property that points to the web app root directory. To be called on shutdown of the web application.
	 * @param servletContext the servlet context of the web application
	 * @see #setWebAppRootSystemProperty
	 */
	public static void removeWebAppRootSystemProperty(ServletContext servletContext) {
		Assert.notNull(servletContext, "ServletContext must not be null");
		String param = servletContext.getInitParameter(WEB_APP_ROOT_KEY_PARAM);
		String key = (param != null ? param : DEFAULT_WEB_APP_ROOT_KEY);
		System.getProperties().remove(key);
	}

	/**
	 * Return whether default HTML escaping is enabled for the web application, i.e. the value of the "defaultHtmlEscape" context-param in {@code web.xml} (if
	 * any). Falls back to {@code false} in case of no explicit default given.
	 * @param servletContext the servlet context of the web application
	 * @return whether default HTML escaping is enabled (default is false)
	 */
	public static boolean isDefaultHtmlEscape(ServletContext servletContext) {
		if (servletContext == null) {
			return false;
		}
		String param = servletContext.getInitParameter(HTML_ESCAPE_CONTEXT_PARAM);
		return Boolean.valueOf(param);
	}

	/**
	 * Return whether default HTML escaping is enabled for the web application, i.e. the value of the "defaultHtmlEscape" context-param in {@code web.xml} (if
	 * any).
	 * <p>
	 * This method differentiates between no param specified at all and an actual boolean value specified, allowing to have a context-specific default in case
	 * of no setting at the global level.
	 * @param servletContext the servlet context of the web application
	 * @return whether default HTML escaping is enabled (null = no explicit default)
	 */
	public static Boolean getDefaultHtmlEscape(ServletContext servletContext) {
		if (servletContext == null) {
			return null;
		}
		Assert.notNull(servletContext, "ServletContext must not be null");
		String param = servletContext.getInitParameter(HTML_ESCAPE_CONTEXT_PARAM);
		return (StringUtils.hasText(param) ? Boolean.valueOf(param) : null);
	}

	/**
	 * Return the temporary directory for the current web application, as provided by the servlet container.
	 * @param servletContext the servlet context of the web application
	 * @return the File representing the temporary directory
	 */
	public static File getTempDir(ServletContext servletContext) {
		Assert.notNull(servletContext, "ServletContext must not be null");
		return (File) servletContext.getAttribute(TEMP_DIR_CONTEXT_ATTRIBUTE);
	}

	/**
	 * Return the real path of the given path within the web application, as provided by the servlet container.
	 * <p>
	 * Prepends a slash if the path does not already start with a slash, and throws a FileNotFoundException if the path cannot be resolved to a resource (in
	 * contrast to ServletContext's {@code getRealPath}, which returns null).
	 * @param servletContext the servlet context of the web application
	 * @param path the path within the web application
	 * @return the corresponding real path
	 * @throws FileNotFoundException if the path cannot be resolved to a resource
	 * @see javax.servlet.ServletContext#getRealPath
	 */
	public static String getRealPath(ServletContext servletContext, String path) throws FileNotFoundException {
		Assert.notNull(servletContext, "ServletContext must not be null");
		// Interpret location as relative to the web application root directory.
		if (!path.startsWith("/")) {
			path = "/" + path;
		}
		String realPath = servletContext.getRealPath(path);
		if (realPath == null) {
			throw new FileNotFoundException("ServletContext resource [" + path + "] cannot be resolved to absolute file path - "
					+ "web application archive not expanded?");
		}
		return realPath;
	}

	/**
	 * Determine the session id of the given request, if any.
	 * @param request current HTTP request
	 * @return the session id, or {@code null} if none
	 */
	public static String getSessionId(HttpServletRequest request) {
		Assert.notNull(request, "Request must not be null");
		HttpSession session = request.getSession(false);
		return (session != null ? session.getId() : null);
	}

	/**
	 * Check the given request for a session attribute of the given name. Returns null if there is no session or if the session has no such attribute. Does not
	 * create a new session if none has existed before!
	 * @param request current HTTP request
	 * @param name the name of the session attribute
	 * @return the value of the session attribute, or {@code null} if not found
	 */
	public static Object getSessionAttribute(HttpServletRequest request, String name) {
		Assert.notNull(request, "Request must not be null");
		HttpSession session = request.getSession(false);
		return (session != null ? session.getAttribute(name) : null);
	}

	/**
	 * Check the given request for a session attribute of the given name. Throws an exception if there is no session or if the session has no such attribute.
	 * Does not create a new session if none has existed before!
	 * @param request current HTTP request
	 * @param name the name of the session attribute
	 * @return the value of the session attribute, or {@code null} if not found
	 * @throws IllegalStateException if the session attribute could not be found
	 */
	public static Object getRequiredSessionAttribute(HttpServletRequest request, String name) throws IllegalStateException {

		Object attr = getSessionAttribute(request, name);
		if (attr == null) {
			throw new IllegalStateException("No session attribute '" + name + "' found");
		}
		return attr;
	}

	/**
	 * Set the session attribute with the given name to the given value. Removes the session attribute if value is null, if a session existed at all. Does not
	 * create a new session if not necessary!
	 * @param request current HTTP request
	 * @param name the name of the session attribute
	 * @param value the value of the session attribute
	 */
	public static void setSessionAttribute(HttpServletRequest request, String name, Object value) {
		Assert.notNull(request, "Request must not be null");
		if (value != null) {
			request.getSession().setAttribute(name, value);
		} else {
			HttpSession session = request.getSession(false);
			if (session != null) {
				session.removeAttribute(name);
			}
		}
	}

	/**
	 * Get the specified session attribute, creating and setting a new attribute if no existing found. The given class needs to have a public no-arg
	 * constructor. Useful for on-demand state objects in a web tier, like shopping carts.
	 * @param session current HTTP session
	 * @param name the name of the session attribute
	 * @param clazz the class to instantiate for a new attribute
	 * @return the value of the session attribute, newly created if not found
	 * @throws IllegalArgumentException if the session attribute could not be instantiated
	 */
	public static Object getOrCreateSessionAttribute(HttpSession session, String name, Class<?> clazz) throws IllegalArgumentException {

		Assert.notNull(session, "Session must not be null");
		Object sessionObject = session.getAttribute(name);
		if (sessionObject == null) {
			try {
				sessionObject = clazz.newInstance();
			} catch (InstantiationException ex) {
				throw new IllegalArgumentException("Could not instantiate class [" + clazz.getName() + "] for session attribute '" + name + "': "
						+ ex.getMessage());
			} catch (IllegalAccessException ex) {
				throw new IllegalArgumentException("Could not access default constructor of class [" + clazz.getName() + "] for session attribute '" + name
						+ "': " + ex.getMessage());
			}
			session.setAttribute(name, sessionObject);
		}
		return sessionObject;
	}

	/**
	 * Return the best available mutex for the given session: that is, an object to synchronize on for the given session.
	 * <p>
	 * Returns the session mutex attribute if available; usually, this means that the HttpSessionMutexListener needs to be defined in {@code web.xml}. Falls
	 * back to the HttpSession itself if no mutex attribute found.
	 * <p>
	 * The session mutex is guaranteed to be the same object during the entire lifetime of the session, available under the key defined by the
	 * {@code SESSION_MUTEX_ATTRIBUTE} constant. It serves as a safe reference to synchronize on for locking on the current session.
	 * <p>
	 * In many cases, the HttpSession reference itself is a safe mutex as well, since it will always be the same object reference for the same active logical
	 * session. However, this is not guaranteed across different servlet containers; the only 100% safe way is a session mutex.
	 * @param session the HttpSession to find a mutex for
	 * @return the mutex object (never {@code null})
	 * @see #SESSION_MUTEX_ATTRIBUTE
	 * @see HttpSessionMutexListener
	 */
	public static Object getSessionMutex(HttpSession session) {
		Assert.notNull(session, "Session must not be null");
		Object mutex = session.getAttribute(SESSION_MUTEX_ATTRIBUTE);
		if (mutex == null) {
			mutex = session;
		}
		return mutex;
	}

	/**
	 * Return an appropriate request object of the specified type, if available, unwrapping the given request as far as necessary.
	 * @param request the servlet request to introspect
	 * @param requiredType the desired type of request object
	 * @return the matching request object, or {@code null} if none of that type is available
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getNativeRequest(ServletRequest request, Class<T> requiredType) {
		if (requiredType != null) {
			if (requiredType.isInstance(request)) {
				return (T) request;
			} else if (request instanceof ServletRequestWrapper) {
				return getNativeRequest(((ServletRequestWrapper) request).getRequest(), requiredType);
			}
		}
		return null;
	}

	/**
	 * Return an appropriate response object of the specified type, if available, unwrapping the given response as far as necessary.
	 * @param response the servlet response to introspect
	 * @param requiredType the desired type of response object
	 * @return the matching response object, or {@code null} if none of that type is available
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getNativeResponse(ServletResponse response, Class<T> requiredType) {
		if (requiredType != null) {
			if (requiredType.isInstance(response)) {
				return (T) response;
			} else if (response instanceof ServletResponseWrapper) {
				return getNativeResponse(((ServletResponseWrapper) response).getResponse(), requiredType);
			}
		}
		return null;
	}

	/**
	 * Determine whether the given request is an include request, that is, not a top-level HTTP request coming in from the outside.
	 * <p>
	 * Checks the presence of the "javax.servlet.include.request_uri" request attribute. Could check any request attribute that is only present in an include
	 * request.
	 * @param request current servlet request
	 * @return whether the given request is an include request
	 */
	public static boolean isIncludeRequest(ServletRequest request) {
		return (request.getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE) != null);
	}

	/**
	 * Expose the current request URI and paths as {@link javax.servlet.http.HttpServletRequest} attributes under the keys defined in the Servlet 2.4
	 * specification, for containers that implement 2.3 or an earlier version of the Servlet API: {@code javax.servlet.forward.request_uri},
	 * {@code javax.servlet.forward.context_path}, {@code javax.servlet.forward.servlet_path}, {@code javax.servlet.forward.path_info},
	 * {@code javax.servlet.forward.query_string}.
	 * <p>
	 * Does not override values if already present, to not cause conflicts with the attributes exposed by Servlet 2.4+ containers themselves.
	 * @param request current servlet request
	 */
	public static void exposeForwardRequestAttributes(HttpServletRequest request) {
		exposeRequestAttributeIfNotPresent(request, FORWARD_REQUEST_URI_ATTRIBUTE, request.getRequestURI());
		exposeRequestAttributeIfNotPresent(request, FORWARD_CONTEXT_PATH_ATTRIBUTE, request.getContextPath());
		exposeRequestAttributeIfNotPresent(request, FORWARD_SERVLET_PATH_ATTRIBUTE, request.getServletPath());
		exposeRequestAttributeIfNotPresent(request, FORWARD_PATH_INFO_ATTRIBUTE, request.getPathInfo());
		exposeRequestAttributeIfNotPresent(request, FORWARD_QUERY_STRING_ATTRIBUTE, request.getQueryString());
	}

	/**
	 * Expose the Servlet spec's error attributes as {@link javax.servlet.http.HttpServletRequest} attributes under the keys defined in the Servlet 2.3
	 * specification, for error pages that are rendered directly rather than through the Servlet container's error page resolution:
	 * {@code javax.servlet.error.status_code}, {@code javax.servlet.error.exception_type}, {@code javax.servlet.error.message},
	 * {@code javax.servlet.error.exception}, {@code javax.servlet.error.request_uri}, {@code javax.servlet.error.servlet_name}.
	 * <p>
	 * Does not override values if already present, to respect attribute values that have been exposed explicitly before.
	 * <p>
	 * Exposes status code 200 by default. Set the "javax.servlet.error.status_code" attribute explicitly (before or after) in order to expose a different
	 * status code.
	 * @param request current servlet request
	 * @param ex the exception encountered
	 * @param servletName the name of the offending servlet
	 */
	public static void exposeErrorRequestAttributes(HttpServletRequest request, Throwable ex, String servletName) {
		exposeRequestAttributeIfNotPresent(request, ERROR_STATUS_CODE_ATTRIBUTE, HttpServletResponse.SC_OK);
		exposeRequestAttributeIfNotPresent(request, ERROR_EXCEPTION_TYPE_ATTRIBUTE, ex.getClass());
		exposeRequestAttributeIfNotPresent(request, ERROR_MESSAGE_ATTRIBUTE, ex.getMessage());
		exposeRequestAttributeIfNotPresent(request, ERROR_EXCEPTION_ATTRIBUTE, ex);
		exposeRequestAttributeIfNotPresent(request, ERROR_REQUEST_URI_ATTRIBUTE, request.getRequestURI());
		exposeRequestAttributeIfNotPresent(request, ERROR_SERVLET_NAME_ATTRIBUTE, servletName);
	}

	/**
	 * Expose the specified request attribute if not already present.
	 * @param request current servlet request
	 * @param name the name of the attribute
	 * @param value the suggested value of the attribute
	 */
	private static void exposeRequestAttributeIfNotPresent(ServletRequest request, String name, Object value) {
		if (request.getAttribute(name) == null) {
			request.setAttribute(name, value);
		}
	}

	/**
	 * Clear the Servlet spec's error attributes as {@link javax.servlet.http.HttpServletRequest} attributes under the keys defined in the Servlet 2.3
	 * specification: {@code javax.servlet.error.status_code}, {@code javax.servlet.error.exception_type}, {@code javax.servlet.error.message},
	 * {@code javax.servlet.error.exception}, {@code javax.servlet.error.request_uri}, {@code javax.servlet.error.servlet_name}.
	 * @param request current servlet request
	 */
	public static void clearErrorRequestAttributes(HttpServletRequest request) {
		request.removeAttribute(ERROR_STATUS_CODE_ATTRIBUTE);
		request.removeAttribute(ERROR_EXCEPTION_TYPE_ATTRIBUTE);
		request.removeAttribute(ERROR_MESSAGE_ATTRIBUTE);
		request.removeAttribute(ERROR_EXCEPTION_ATTRIBUTE);
		request.removeAttribute(ERROR_REQUEST_URI_ATTRIBUTE);
		request.removeAttribute(ERROR_SERVLET_NAME_ATTRIBUTE);
	}

	/**
	 * Expose the given Map as request attributes, using the keys as attribute names and the values as corresponding attribute values. Keys need to be Strings.
	 * @param request current HTTP request
	 * @param attributes the attributes Map
	 */
	public static void exposeRequestAttributes(ServletRequest request, Map<String, ?> attributes) {
		Assert.notNull(request, "Request must not be null");
		Assert.notNull(attributes, "Attributes Map must not be null");
		for (Map.Entry<String, ?> entry : attributes.entrySet()) {
			request.setAttribute(entry.getKey(), entry.getValue());
		}
	}

	/**
	 * Retrieve the first cookie with the given name. Note that multiple cookies can have the same name but different paths or domains.
	 * @param request current servlet request
	 * @param name cookie name
	 * @return the first cookie with the given name, or {@code null} if none is found
	 */
	public static Cookie getCookie(HttpServletRequest request, String name) {
		Assert.notNull(request, "Request must not be null");
		Cookie cookies[] = request.getCookies();
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				if (name.equals(cookie.getName())) {
					return cookie;
				}
			}
		}
		return null;
	}

	/**
	 * Check if a specific input type="submit" parameter was sent in the request, either via a button (directly with name) or via an image (name + ".x" or name
	 * + ".y").
	 * @param request current HTTP request
	 * @param name name of the parameter
	 * @return if the parameter was sent
	 * @see #SUBMIT_IMAGE_SUFFIXES
	 */
	public static boolean hasSubmitParameter(ServletRequest request, String name) {
		Assert.notNull(request, "Request must not be null");
		if (request.getParameter(name) != null) {
			return true;
		}
		for (String suffix : SUBMIT_IMAGE_SUFFIXES) {
			if (request.getParameter(name + suffix) != null) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Obtain a named parameter from the given request parameters.
	 * <p>
	 * See {@link #findParameterValue(java.util.Map, String)} for a description of the lookup algorithm.
	 * @param request current HTTP request
	 * @param name the <i>logical</i> name of the request parameter
	 * @return the value of the parameter, or {@code null} if the parameter does not exist in given request
	 */
	public static String findParameterValue(ServletRequest request, String name) {
		return findParameterValue((Map<String, ?>) request.getParameterMap(), name);
	}

	/**
	 * Obtain a named parameter from the given request parameters.
	 * <p>
	 * This method will try to obtain a parameter value using the following algorithm:
	 * <ol>
	 * <li>Try to get the parameter value using just the given <i>logical</i> name. This handles parameters of the form <tt>logicalName = value</tt>. For normal
	 * parameters, e.g. submitted using a hidden HTML form field, this will return the requested value.</li>
	 * <li>Try to obtain the parameter value from the parameter name, where the parameter name in the request is of the form <tt>logicalName_value = xyz</tt>
	 * with "_" being the configured delimiter. This deals with parameter values submitted using an HTML form submit button.</li>
	 * <li>If the value obtained in the previous step has a ".x" or ".y" suffix, remove that. This handles cases where the value was submitted using an HTML
	 * form image button. In this case the parameter in the request would actually be of the form <tt>logicalName_value.x = 123</tt>.</li>
	 * </ol>
	 * @param parameters the available parameter map
	 * @param name the <i>logical</i> name of the request parameter
	 * @return the value of the parameter, or {@code null} if the parameter does not exist in given request
	 */
	public static String findParameterValue(Map<String, ?> parameters, String name) {
		// First try to get it as a normal name=value parameter
		Object value = parameters.get(name);
		if (value instanceof String[]) {
			String[] values = (String[]) value;
			return (values.length > 0 ? values[0] : null);
		} else if (value != null) {
			return value.toString();
		}
		// If no value yet, try to get it as a name_value=xyz parameter
		String prefix = name + "_";
		for (String paramName : parameters.keySet()) {
			if (paramName.startsWith(prefix)) {
				// Support images buttons, which would submit parameters as name_value.x=123
				for (String suffix : SUBMIT_IMAGE_SUFFIXES) {
					if (paramName.endsWith(suffix)) {
						return paramName.substring(prefix.length(), paramName.length() - suffix.length());
					}
				}
				return paramName.substring(prefix.length());
			}
		}
		// We couldn't find the parameter value...
		return null;
	}

	/**
	 * Return a map containing all parameters with the given prefix. Maps single values to String and multiple values to String array.
	 */
	public static Map<String, Object> getParametersStartingWith(ServletRequest request, String prefix) {
		Assert.notNull(request, "Request must not be null");
		Enumeration<?> paramNames = request.getParameterNames();
		Map<String, Object> params = new TreeMap<String, Object>();
		if (prefix == null) {
			prefix = "";
		}
		while (paramNames != null && paramNames.hasMoreElements()) {
			String paramName = (String) paramNames.nextElement();
			if ("".equals(prefix) || paramName.startsWith(prefix)) {
				String unprefixed = paramName.substring(prefix.length());
				String[] values = request.getParameterValues(paramName);
				if (values == null || values.length == 0) {
					// Do nothing, no values found at all.
				} else if (values.length > 1) {
					params.put(unprefixed, values);
				} else {
					params.put(unprefixed, values[0]);
				}
			}
		}
		return params;
	}

	/**
	 * Return the target page specified in the request.
	 * @param request current servlet request
	 * @param paramPrefix the parameter prefix to check for (e.g. "_target" for parameters like "_target1" or "_target2")
	 * @param currentPage the current page, to be returned as fallback if no target page specified
	 * @return the page specified in the request, or current page if not found
	 */
	public static int getTargetPage(ServletRequest request, String paramPrefix, int currentPage) {
		Enumeration<?> paramNames = request.getParameterNames();
		while (paramNames.hasMoreElements()) {
			String paramName = (String) paramNames.nextElement();
			if (paramName.startsWith(paramPrefix)) {
				for (int i = 0; i < HttpUtils.SUBMIT_IMAGE_SUFFIXES.length; i++) {
					String suffix = HttpUtils.SUBMIT_IMAGE_SUFFIXES[i];
					if (paramName.endsWith(suffix)) {
						paramName = paramName.substring(0, paramName.length() - suffix.length());
					}
				}
				return Integer.parseInt(paramName.substring(paramPrefix.length()));
			}
		}
		return currentPage;
	}

	/**
	 * Extract the URL filename from the given request URL path. Correctly resolves nested paths such as "/products/view.html" as well.
	 * @param urlPath the request URL path (e.g. "/index.html")
	 * @return the extracted URI filename (e.g. "index")
	 */
	public static String extractFilenameFromUrlPath(String urlPath) {
		String filename = extractFullFilenameFromUrlPath(urlPath);
		int dotIndex = filename.lastIndexOf('.');
		if (dotIndex != -1) {
			filename = filename.substring(0, dotIndex);
		}
		return filename;
	}

	/**
	 * Extract the full URL filename (including file extension) from the given request URL path. Correctly resolves nested paths such as "/products/view.html"
	 * as well.
	 * @param urlPath the request URL path (e.g. "/products/index.html")
	 * @return the extracted URI filename (e.g. "index.html")
	 */
	public static String extractFullFilenameFromUrlPath(String urlPath) {
		int end = urlPath.indexOf(';');
		if (end == -1) {
			end = urlPath.indexOf('?');
			if (end == -1) {
				end = urlPath.length();
			}
		}
		int begin = urlPath.lastIndexOf('/', end) + 1;
		return urlPath.substring(begin, end);
	}

	public static Map<String, Cookie> toMap(Cookie[] cookies) {
		if (cookies == null || cookies.length == 0)
			return new HashMap<String, Cookie>(0);

		Map<String, Cookie> map = new HashMap<String, Cookie>(cookies.length * 2);
		for (Cookie c : cookies) {
			map.put(c.getName(), c);
		}
		return map;
	}

	public static String getRemoteAddr(ServletRequest request) {
		String ip = null;
		if (request instanceof HttpServletRequest) {
			HttpServletRequest httpRequest = (HttpServletRequest) request;
			ip = httpRequest.getHeader("X-Remote-IP");
			if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
				ip = httpRequest.getHeader("X-Forwarded-For");
			}
			if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
				ip = httpRequest.getHeader("x-forwarded-for");
			}
			if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
				ip = httpRequest.getHeader("Proxy-Client-IP");
			} else {
				if (ip.contains(","))
					ip = ip.substring(0, ip.indexOf(","));
			}
			if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
				ip = httpRequest.getHeader("X-Proxy-Client-IP");
			}
			if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
				ip = httpRequest.getHeader("WL-Proxy-Client-IP");
			}
			if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
				ip = httpRequest.getHeader("X-Real-IP");
			}
		}
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getRemoteAddr();
		}
		return ip == null ? "" : ip;
	}

	public static String getRemoteHost(ServletRequest request) {
		String remoteHost = null;
		if (request instanceof HttpServletRequest) {
			HttpServletRequest httpRequest = (HttpServletRequest) request;
			remoteHost = httpRequest.getHeader("X-Remote-Host");
		}
		if (remoteHost == null || remoteHost.length() == 0 || "unknown".equalsIgnoreCase(remoteHost)) {
			remoteHost = request.getRemoteAddr();
		}
		return remoteHost == null || remoteHost.length() == 0 ? getRemoteAddr(request) : remoteHost;
	}

	public static String getUserAgent(ServletRequest request) {
		String userAgent = null;
		if (request instanceof HttpServletRequest) {
			HttpServletRequest httpRequest = (HttpServletRequest) request;
			userAgent = httpRequest.getHeader("X-User-Agent");
			if (userAgent != null || "".equals(userAgent) || "unknown".equalsIgnoreCase(userAgent))
				userAgent = httpRequest.getHeader("User-Agent");
			if (userAgent != null || "".equals(userAgent) || "unknown".equalsIgnoreCase(userAgent))
				userAgent = httpRequest.getHeader("user-agent");
		}
		return userAgent == null || "unknown".equalsIgnoreCase(userAgent) ? "" : userAgent;
	}
	
	public static String getHeader(ServletRequest request, String headerName) {
		String headerValue = null;
		if (request instanceof HttpServletRequest && headerName != null && !"".equals(headerName)) {
			HttpServletRequest httpRequest = (HttpServletRequest) request;
			headerValue = httpRequest.getHeader(headerName);
		}
		return headerValue == null || "unknown".equalsIgnoreCase(headerValue) ? "" : headerValue;
	}

	public static boolean isIpV4Addr(String addr) {
		if (addr == null || "".equals(addr) || addr.length() < 7 || addr.length() > 15)
			return false;
		String rexp = "([1-9]|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3}";
		Pattern pat = Pattern.compile(rexp);
		Matcher mat = pat.matcher(addr);
		return mat.find();
	}
	
	public static boolean isIpV6Addr(String addr, boolean includeCompression, boolean includeMixedIpV4) {
		if (addr == null || "".equals(addr) || !addr.contains(":") || (!addr.contains(".") && addr.length() < 2 || addr.length() > 39) || (addr.contains(".") && addr.length() < 14 || addr.length() > 45))
			return false;
		String baseRexp = "^([\\da-fA-F]{1,4}:){7}[\\da-fA-F]{1,4}$";
		String compRexp = "^:((:[\\da-fA-F]{1,4}){1,6}|:)$|^[\\da-fA-F]{1,4}:((:[\\da-fA-F]{1,4}){1,5}|:)$|^([\\da-fA-F]{1,4}:){2}((:[\\da-fA-F]{1,4}){1,4}|:)$|^([\\da-fA-F]{1,4}:){3}((:[\\da-fA-F]{1,4}){1,3}|:)$|^([\\da-fA-F]{1,4}:){4}((:[\\da-fA-F]{1,4}){1,2}|:)$|^([\\da-fA-F]{1,4}:){5}:([\\da-fA-F]{1,4})?$|^([\\da-fA-F]{1,4}:){6}:$";
		String mixeRexp = "^([\\da-fA-F]{1,4}:){6}((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$|^::([\\da-fA-F]{1,4}:){0,4}((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$|^([\\da-fA-F]{1,4}:):([\\da-fA-F]{1,4}:){0,3}((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$|^([\\da-fA-F]{1,4}:){2}:([\\da-fA-F]{1,4}:){0,2}((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$|^([\\da-fA-F]{1,4}:){3}:([\\da-fA-F]{1,4}:){0,1}((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$|^([\\da-fA-F]{1,4}:){4}:((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$";
		String rexp = baseRexp + (includeCompression ? "|" + compRexp : "") + (includeMixedIpV4 ? "|" + mixeRexp : "");
		Pattern pat = Pattern.compile(rexp);
		Matcher mat = pat.matcher(addr);
		return mat.find();
	}
	
	public static boolean isIpV4V6Addr(String addr, boolean v6IncludeCompression, boolean v6IncludeMixedIpV4) {
		return isIpV4Addr(addr) || isIpV6Addr(addr, v6IncludeCompression, v6IncludeMixedIpV4);
	}
	
	public static URL getURL(String url) throws MalformedURLException {
		Assert.isTrue(url.startsWith("http://") || url.startsWith("https://"), "'url' must be a http url");
		
		URL _url = null;
		if (hasSunHttpHandler) {
			try {
				if (url.toLowerCase().startsWith("https")) {
					URLStreamHandler handler = (URLStreamHandler) Class.forName("sun.net.www.protocol.https.Handler").newInstance();
					_url = new URL(null, url, handler);
				} else {
					URLStreamHandler handler = (URLStreamHandler) Class.forName("sun.net.www.protocol.http.Handler").newInstance();
					_url = new URL(null, url, handler);
				}
			} catch (Throwable e) {
			}
		} else {
			_url = new URL(url);
		}
		return _url;
	}
	
	public static HttpURLConnection getHttpURLConnection(String url, int connectTimeoutSeconds, int readTimeoutSeconds, String charset, boolean useCaches) throws IOException {
		URL _url = HttpUtils.getURL(url);
		
		HttpURLConnection conn = (HttpURLConnection) _url.openConnection();
		if (HttpsURLConnection.class.isAssignableFrom(conn.getClass())) {
			System.setProperty("java.protocol.handler.pkgs", "javax.net.ssl");
			((HttpsURLConnection) conn).setHostnameVerifier(new HostnameVerifier() {
				@Override
				public boolean verify(String urlHostName, SSLSession session) {
					return urlHostName.equals(session.getPeerHost());
				}
			});
		}
		
		if (connectTimeoutSeconds > 0) 
			conn.setConnectTimeout(connectTimeoutSeconds * 1000);
		if (readTimeoutSeconds > 0)
			conn.setReadTimeout(readTimeoutSeconds * 1000);

		// 设置通用的请求属性
        conn.setRequestProperty("accept", "*/*");
        conn.setRequestProperty("connection", "Keep-Alive");
        conn.setRequestProperty("user-agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/44.0.2403.155 Safari/537.36");
        if (charset != null && !"".equals(charset))
        	conn.setRequestProperty("charset", charset);
        conn.setUseCaches(false);
        return conn;
	}

	public static void debugRequest(HttpServletRequest request, Logger log) {
		if (log.isDebugEnabled() || log.isTraceEnabled()) {
			log.debug("");
			log.debug("============================================================");
			log.debug(">>>>>>>>>>>>>>>> DEBUG Http request : BEGIN <<<<<<<<<<<<<<<<");
			log.debug("============================================================");
			log.debug("request.getClass(): " + request.getClass());
			log.debug("request.getProtocol(): " + request.getProtocol());
			log.debug("request.getScheme(): " + request.getScheme());
			log.debug("request.getServerName(): " + request.getServerName());
			log.debug("request.getServerPort(): " + request.getServerPort());
			log.debug("request.getMethod(): " + request.getMethod());
			log.debug("request.getPathInfo(): " + request.getPathInfo());
			log.debug("request.getRequestURI(): " + request.getRequestURI());
			log.debug("request.getQueryString(): " + request.getQueryString());
			log.debug("request.getContextPath(): " + request.getContextPath());
			log.debug("request.getContentLength(): " + request.getContentLength());
			log.debug("request.getCharacterEncoding(): " + request.getCharacterEncoding());
			log.debug("request.getServletPath(): " + request.getServletPath());
			log.debug("request.getRemoteAddr(): " + request.getRemoteAddr());
			log.debug("request.getRemoteHost(): " + request.getRemoteHost());
			log.debug("request.getRemotePort(): " + request.getRemotePort());
			log.debug("request.getRemoteUser(): " + request.getRemoteUser());
			log.debug("request.getAuthType(): " + request.getAuthType());
			log.debug("request.getLocalName(): " + request.getLocalName());
			log.debug("request.getLocalAddr(): " + request.getLocalAddr());
			log.debug("request.getLocalPort(): " + request.getLocalPort());
			log.debug("request remoteIp: " + getRemoteAddr(request));
			log.debug("request getRequestedSessionId(): " + request.getRequestedSessionId());
			log.debug("request.isRequestedSessionIdValid(): " + request.isRequestedSessionIdValid());
			log.debug("request.isRequestedSessionIdFromCookie(): " + request.isRequestedSessionIdFromCookie());
			log.debug("request.isRequestedSessionIdFromURL(): " + request.isRequestedSessionIdFromURL());

			log.debug("---------------- Http Headers:              ---------------");
			Enumeration<String> headerNames = request.getHeaderNames();
			if (headerNames != null) {
				while (headerNames.hasMoreElements()) {
					String headerName = headerNames.nextElement();
					log.debug(headerName + ": " + request.getHeader(headerName));
				}
			}

			log.debug("---------------- Http Cookies:              ---------------");
			Cookie[] cookies = request.getCookies();
			if (cookies != null) {
				for (Cookie cookie : cookies) {
					log.debug("name: " + cookie.getName() + ", value: " + cookie.getValue() + ", domain: " + cookie.getDomain() + ", path: " + cookie.getPath()
							+ ", maxAge: " + cookie.getMaxAge());
				}
			}

			log.debug("---------------- Http Attributes:           ---------------");
			Enumeration<String> attrNames = request.getAttributeNames();
			if (attrNames != null) {
				while (attrNames.hasMoreElements()) {
					String attrName = attrNames.nextElement();
					log.debug(attrName + ": " + request.getAttribute(attrName));
				}
			}

			log.debug("---------------- Http Parameters:           ---------------");
			Enumeration<String> paramNames = request.getParameterNames();
			if (paramNames != null) {
				while (paramNames.hasMoreElements()) {
					String paramName = paramNames.nextElement();
					String[] paramValues = request.getParameterValues(paramName);
					String paramValue = "";
					if (paramValues != null) {
						if (paramValues.length == 0) {
							paramValue = "";
						} else if (paramValues.length == 1) {
							paramValue = paramValues[0];
						} else {
							paramValue = Arrays.asList(paramValues).toString();
						}
					}
					log.debug(paramName + ": " + paramValue);
				}
			}

			log.debug("---------------- Http Session Info:              ---------------");
			try {
				HttpSession session = request.getSession(false);
				if (session != null) {
					log.debug("id: " + session.getId() + ", new: " + session.isNew() + ", creationTime: " + session.getCreationTime() + ", lastAccessedTime: "
							+ session.getLastAccessedTime() + ", maxInactiveInterval: " + session.getMaxInactiveInterval());
					Enumeration<String> sessionAttributeNames = session.getAttributeNames();
					if (sessionAttributeNames != null) {
						while (sessionAttributeNames.hasMoreElements()) {
							String sessionAttributeName = sessionAttributeNames.nextElement();
							Object sessionAttributeValue = session.getAttribute(sessionAttributeName);
							log.debug(sessionAttributeName + ": " + sessionAttributeValue);
						}
					}
				}
			} catch (Throwable e) {
			}
			log.debug("============================================================");
			log.debug(">>>>>>>>>>>>>>>> DEBUG Http request : END   <<<<<<<<<<<<<<<<");
			log.debug("============================================================");
			log.debug("");
		}
	}
}

