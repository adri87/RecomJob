package es.adri.pfc.feed;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.adri.pfc.config.Configuration;
import es.adri.pfc.connections.ConnectionLMF;

/**
 * Realiza las operaciones de addicion y borado de preferencias en el archivo correspondiente.
 * 
 * @author Adriano Jose Martin Gutierrez
 * @version 1.0
 */
public class FeedBack {
	private Logger log;
	private File file;
	private String user;
	private String baseUrl;
	private String idOffer;
	private ConnectionLMF conLmf;
	private Vector<String> lineas;
	
	/**
	 * Constructor de la clase FeedBack
	 * 
	 * @param baseUrl .- Direccion base donde se encuentra desplegado el sistema recomendador.
	 * @param user .- Identificador de usuario
	 * @param searchResult .- Datos de la busqueda que se desea guardar.
	 * @param cfg .- Objeto de configuracion del servlet
	 */
	public FeedBack(String baseUrl, String user, String searchResult, Configuration cfg){
		log = LoggerFactory.getLogger(FeedBack.class);
		lineas = new Vector<String>(); 
		this.user=user;
		this.baseUrl=baseUrl;
		String filename = this.baseUrl+"resources/"+cfg.getProperty("fichero");
		file = new File(filename);
		conLmf = new ConnectionLMF(baseUrl, cfg);
		try {
			JSONObject search = new JSONObject(searchResult);
			String nameOffer = search.getJSONArray("result").getJSONObject(0).getString("entityName");
			idOffer = getIdOffer(nameOffer);
		} catch (JSONException e) {
			log.error("Error al obtener JSONObject");
			e.printStackTrace();
		}
	}
	
	/**
	 * Introduce en el archivo de datos una nueva valoracion.
	 */
	public void introduceRate(){
		checkPuntPrev();
		try {
			FileWriter fw = new FileWriter(file);
            PrintWriter pw = new PrintWriter(fw); 
			BigDecimal numeroDecimal = new BigDecimal(Math.random()*2).setScale(1,BigDecimal.ROUND_DOWN ); 
			double rate = 5 - numeroDecimal.doubleValue();
            for(int i=0;i<lineas.size();i++){ 
            	pw.println(lineas.elementAt(i)); 
            } 
        	pw.println(user+","+idOffer+","+rate); 
        	pw.close();
			fw.close();
			log.info("Introducida una nueva valoracion: "+user+","+idOffer+","+rate+"\n");
		} catch (IOException iox) {
			iox.printStackTrace();
		}
	}
	
	/**
	 * Este metodo se llama cuando se elimina una valoracion.
	 */
	public void deletePunt(){
		checkPuntPrev();
		try {
			FileWriter fw = new FileWriter(file);
            PrintWriter pw = new PrintWriter(fw); 
            for(int i=0;i<lineas.size();i++){ 
            	pw.println(lineas.elementAt(i)); 
            } 
        	pw.close();
			fw.close();
			log.info("Borrada valoracion del usuario: "+user);
			log.info("Para la oferta con identificador: "+idOffer);
		} catch (IOException iox) {
			iox.printStackTrace();
		}
	}
	
	/**
	 * Devuelve la id asociada al nombre de una oferta determinada.
	 * 
	 * @param offer .- Nombre identificador de la oferta que ha sido guardada.
	 * @return id .- String con la id correspondiente a la oferta.
	 * @throws JSONException
	 */
	private String getIdOffer(String offer) throws JSONException{
		String query = conLmf.readSparql(baseUrl+"/resources/idOffer.sparql");
		JSONArray result = conLmf.getResponseQuerySparql(query, true, offer);
		String id = result.getJSONObject(0).getJSONObject("id").getString("value");
		return id;
	}

	/**
	 * Comprueba si el usuario ya habia valorado anteriormente esa oferta de empleo.
	 * 
	 * @param file .- Archivo con los datos del sistema.
	 * @return true o false, dependiendo si ya se habia producido una valoracion.
	 */
	private boolean checkPuntPrev(){
		try {
			// Comprobamos si  existe la valoracion y en caso afirmativo la quitamos del las lineas que vamos a introducir en el archivo.
			FileReader fr = new FileReader(file);
			BufferedReader br = new BufferedReader(fr);
			String linea;
			while((linea=br.readLine())!=null){
				String[] datos = linea.split(",");
				if (user.equals(datos[0]) && idOffer.equals(datos[1]))
					log.info("Ya habia una valoracion anterior");
				else
					lineas.add(linea);
			}
			br.close();
			fr.close();
		} catch (FileNotFoundException e) {
			log.error("Archivo no encontrado");
			e.printStackTrace();
		} catch (IOException e) {
			log.error("Error con el bufferReader");
			e.printStackTrace();
		}
		return false;
	}
	
	/**
	 * Metodo para leer  el contenido del fichero de datos del servidor en un momento deseado.
	 */
//	private void read(){
//		System.out.println("Leyendo");
//		try {
//			FileReader fr = new FileReader(file);
//			BufferedReader br = new BufferedReader(fr);
//			String linea;
//			while((linea=br.readLine())!=null){
//				System.out.println(linea);
//			}
//			br.close();
//			fr.close();
//		} catch (Exception e) {
//		}
//	}
}
