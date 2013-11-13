package es.adri.pfc.connections;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.adri.pfc.config.Configuration;

/**
 * Se encarga de llevar a cabo las consultas a LMF y realizar tratamientos.
 * 
 * @author Adriano Jose Martin Gutierrez
 * @version 1.0
 */
public class ConnectionLMF {
	private Logger log;
	private String urlServerLmf;
	private String baseUrl;
	
	/**
	 * Constructor de la clase ConnectionLmf
	 * 
	 * @param baseUrl .- Direccion base donde se encuentra desplegado el sistema recomendador.
	 * @param urlSeverLmf .- URL de despliegue de LMF.
	 */
	public ConnectionLMF(String baseUrl, Configuration cfg){
		this.baseUrl=baseUrl;
		this.urlServerLmf=cfg.getProperty("serverLmf");
		log = LoggerFactory.getLogger(ConnectionLMF.class);
	}

	/**
	 * Este metodo se encarga de leer y devolver el contenido del fichero que se le solicita.
	 * 
	 * @param fileName .- Nombre del fichero a leer.
	 * @return Un String con el contenido del fichero.
	 */
	public String readSparql(String fileName){
		BufferedReader br = null;
		String content;
		try {
			br = new BufferedReader(new FileReader(fileName));
	        StringBuilder sb = new StringBuilder();
	        String line = br.readLine();
	        while (line != null) {
	        	sb.append(line);
		        sb.append('\n');
		        line = br.readLine();
	        }
	        content = sb.toString();
		} catch (FileNotFoundException e) {
			log.error("Archivo no encontrado");
			throw new RuntimeException("File not found");
		} catch (IOException e) {
			log.error("Erron en ejecucion");
			throw new RuntimeException("IO Error occured");
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return content;
	}
	
	/**
	 * Hace una consulta al servidor donde se encuentran los datos para obtener la respuesta deseada.
	 * 
	 * @param query .- consulta a realizar al servidor.
	 * @return JSONObject con la información solicitada.
	 */
	public JSONObject getJson(String query){
		URL url;
	    HttpURLConnection conn;
	    BufferedReader rd;
	    JSONObject json = new JSONObject();
	    String line;
	    String result = "";
	    try {
	    	url = new URL(query);
	        conn = (HttpURLConnection) url.openConnection();
	        conn.setRequestMethod("GET");
	        rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
	        while ((line = rd.readLine()) != null) {
	        	result += line;
	        }
	        rd.close();
	        json = new JSONObject(result);
	    } catch (Exception e) {
	    	log.info("Error al obtener JSON");
	    	e.printStackTrace();
		}
		return json;
	}
	
	/**
	 * Obtiene el JSON de respuesta resultado de consultar a LMF una determinada peticion. Tambien se 
	 * encarga de codificar la consulta.
	 * 
	 * @param pet .- Peticion que se desea realizar sobre el servidor.
	 * @param conlmf .- Objeto Connection LMF.
	 * @param mustReplace .- booleano que indica si hay un parametro de la peticion a sustituir.
	 * @param replace .- String que debe ser incluido dentro de la peticion, en caso de sustitucion.
	 * @return JSONArray con la respuesta del servidor
	 */
	public JSONArray getResponseQuerySparql(String pet, boolean mustReplace, String replace){
		String query = pet;
		JSONArray response = new JSONArray();
		if (mustReplace)
			query = pet.replace("Resource", replace);
		try {
			query = URLEncoder.encode(query, "UTF-8");
			query = urlServerLmf + "sparql/select?query="+query+"&output=json";
			response = getJson(query).getJSONObject("results").getJSONArray("bindings");
		} catch (UnsupportedEncodingException e) {
			log.error("Error de codificacion no soportada");
			e.printStackTrace();
		} catch (JSONException e) {
			log.error("Error al obtener JSON");
			e.printStackTrace();
		}
		return response;
	}
	
	/**
	 * Devuelve la uri de todas las ofertas que se encuentran disponibles dentro del servidor.
	 * 
	 * @return urisResources .- ArrayList con las uris de todas las ofertas disponibles.
	 */
	public ArrayList<String> getUris() {
		String queryResources = readSparql(baseUrl+"/resources/resources.sparql");
		JSONArray resources = getResponseQuerySparql(queryResources, false, null);
		ArrayList<String> urisResources = new ArrayList<>();
		for (int i = 0; i < resources.length(); i++) {
			try {
				String uriResource = resources.getJSONObject(i).getJSONObject("offers").getString("value");
				urisResources.add(uriResource);
			} catch (JSONException e) {
				log.error("Error al obtener uris :"+e.getMessage());
				e.printStackTrace();
			}
		}
		return urisResources;
	}
}
