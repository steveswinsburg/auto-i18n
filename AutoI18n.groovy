/**
 *  Copyright 2012 Steve Swinsburg
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.util.EntityUtils;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.StringUtils;

@Grab(group='commons-lang', module='commons-lang', version='2.5')
@Grab(group='org.apache.httpcomponents', module='httpclient', version='4.0')
@Grab(group='commons-configuration', module='commons-configuration', version='1.6')

/**
 * Simple utility to automatically translate a properties file into any number of other languages.
 * Configure the languages to translate to (and other settings) via config.properties
 * you then need to provide a properties file as a base to translate from. At the moment this must be in English.
 * Then run via:
 * groovy AutoI18n.groovy <base_properties_file>
 *
 * For example:
 * groovy AutoI18n.groovy my.properties
 *
 * @author Steve Swinsburg (steve.swinsburg@gmail.com)
 * @version 0.1 
 */
class Autoi18n {
	
	private static PropertiesConfiguration config;
	private static PropertiesConfiguration baseProps;
	private static String basePropsFile;
	private static final int DEFAULT_TIMEOUT = 10000; //milliseconds
	
	private static final String RESPONSE_START = "<string xmlns=\"http://schemas.microsoft.com/2003/10/Serialization/\">";
	private static final String RESPONSE_END = "</string>";

	static main(args){
		
		//get config
		config = loadProps("config.properties");
		if(config == null) {
			println("Error loading config, aborting");
			return;
		}
		
		//check args
		if (args.length < 1) {
			printUsage();
			return;
		}
		
		//load base props
		basePropsFile = args[0];
		if(StringUtils.isBlank(basePropsFile)) {
			printUsage();
			return;
		}
		println("Base properties file: " + basePropsFile);
		baseProps = loadProps(basePropsFile);
		if(baseProps == null) {
			println("Error loading base properties file, aborting");
			return;
		}
		
		
		List<String> languages = config.getList("languages");
		println("Configured Languages: " + languages);
		
		
		//translate
		for(String lang:languages) {
			
			println("Translating to: " + lang);
			
			PropertiesConfiguration translation = translate(baseProps, lang);
			
			String newFile = createOutputFilePath(lang);
			
			println("Writing to: " + newFile);
			
			if(writeProps(translation, newFile)) {
				println("Finished.");
			}
		}
		
		
	}
	
	
	private static void printUsage() {
		println("\nUsage:");
		println("groovy Autoi18n.groovy <file.properties>\n");
	}
	
	
	/**
	 * Helper to load a properties file
	 */
	private static PropertiesConfiguration loadProps(String file) {
		try {
			return new PropertiesConfiguration(file);
		} catch (ConfigurationException e) {
			println(e.getClass() + ": " + e.getMessage());
		}
		return null;
	}
	
	
	/**
	 * Helper to write a properties file
	 */
	private static boolean writeProps(PropertiesConfiguration props, String file) {
		try {
			props.save(file);
			return true;
		} catch (ConfigurationException e) {
			println(e.getClass() + ": " + e.getMessage());
		}
		return false;
	}
	
	private static PropertiesConfiguration translate(PropertiesConfiguration toTranslate, String toLang) {
		
		PropertiesConfiguration translation = new PropertiesConfiguration();
		
		Map<String,String> params = new HashMap<String,String>();
		params.put("appId", config.getString("key"));
		params.put("to", toLang);
		
		Iterator t = toTranslate.getKeys();
		while (t.hasNext()) {
		
			String key = t.next();
			String value = toTranslate.getString(key);
		
			params.put("text", value);
			
			String response = doGet(config.getString("url"), params, DEFAULT_TIMEOUT);
			
			String translatedValue = stripResponse(response);
			println(key + "=" + translatedValue);
			
			translation.addProperty(key, translatedValue);
		}
	
		return translation;
	
	}
	
	
	/*
	 * Helper to create the output filePath 
	 */
	private static String createOutputFilePath(String lang) {
		
		String outputDir = config.getString("output.dir");
		
		//strip any leading directory
		String trimmed = StringUtils.substringAfterLast(basePropsFile, File.separator);
		
		//replace the .properties with _lang.properties
		//ensures it is at the end
		trimmed = StringUtils.removeEnd(trimmed, ".properties");
		return outputDir + trimmed + "_" + lang + ".properties";
	
	}
	
	
	
	
	/**
	* Makes a GET request to the given address. Any query string should be appended already.
	* @param address	the fully qualified URL to make the request to
	* @param params		Map of params that will be attached to the URL
	* @param timeout  the timeout, in milliseconds
	* @return
	*/
	private static String doGet(String address, Map<String,String> params, int timeout){
		try {
			HttpClient httpclient = new DefaultHttpClient();
			httpclient.getParams().setParameter(HttpConnectionParams.CONNECTION_TIMEOUT, timeout);
			
			String url = address + "?" + serialiseMapToQueryString(params);
			//println(url);
			
			HttpGet httpget = new HttpGet(url);
			HttpResponse response = httpclient.execute(httpget);
			HttpEntity entity = response.getEntity();
			
			if (entity != null) {
				return EntityUtils.toString(entity);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Serialise the given Map of parameters to a URL query string
	 * 
	 * @param params Map of params
	 * @return a String that contains the serialised parameters in key=value& pairs. Prefix it with a ? to use.
	 * 
	 */
	private static String serialiseMapToQueryString(Map<String,String> params) {
		
		StringBuilder s = new StringBuilder();
		
		//iterate so we can check if we have more values in the map
		for (Iterator<Map.Entry<String,String>> it = params.entrySet().iterator(); it.hasNext();) {
			Map.Entry<String,String> entry = (Map.Entry<String,String>) it.next();
			s.append(entry.getKey());
			s.append("=");
			s.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
				
			if(it.hasNext()){
				s.append("&");
			}
		}
		
		return s.toString();
	}
	
	/**
	 * Strip the XML from the response
	 */
	private static String stripResponse(String xml) {
		String s = StringUtils.removeStart(xml,RESPONSE_START);
		return StringUtils.removeEnd(s, RESPONSE_END);
	}
	
	
}

