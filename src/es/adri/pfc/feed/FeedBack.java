package es.adri.pfc.feed;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Vector;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.impl.model.jdbc.MySQLJDBCDataModel;
import org.apache.mahout.cf.taste.model.DataModel;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mysql.jdbc.PreparedStatement;

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
	private Configuration cfg;
	private String idUser;
	private String baseUrl;
	private String idOffer;
	private ConnectionLMF conLmf;
	private DataModel model;
	private Vector<String> lineas = new Vector<String>();
	private boolean datDb=false;
	
	/**
	 * Constructor de la clase FeedBack
	 * 
	 * @param baseUrl .- Direccion base donde se encuentra desplegado el sistema recomendador.
	 * @param user .- Identificador de usuario
	 * @param searchResult .- Datos de la busqueda que se desea guardar.
	 * @param cfg .- Objeto de configuracion del servlet
	 */
	public FeedBack(String baseUrl, Configuration cfg, String user, String searchResult){
		log = LoggerFactory.getLogger(FeedBack.class);
		idUser=user;
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
	 * Constructor de la clase FeedBackDB
	 * 
	 * @param baseUrl .- Direccion base donde se encuentra desplegado el sistema recomendador.
	 * @param cfg .- Objeto de configuracion del servlet
	 * @param user .- Identificador de usuario
	 * @param searchResult .- Datos de la busqueda que se desea guardar.
	 * @param datDB .- Indica que el origen de datos es un base de datos
	 */
	public FeedBack(String baseUrl, Configuration cfg, String user, String searchResult, boolean datDB) {
		log=LoggerFactory.getLogger(FeedBack.class);
		this.cfg=cfg;
		idUser=user;
		model= new MySQLJDBCDataModel(cfg.getDataSource(), cfg.getProperty("nametable"), 
				cfg.getProperty("coluser"), cfg.getProperty("coloffer"), cfg.getProperty("colpref"), null);
		this.baseUrl=baseUrl;
		conLmf=new ConnectionLMF(baseUrl, cfg);
		try {
			JSONObject search = new JSONObject(searchResult);
			String nameOffer = search.getJSONArray("result").getJSONObject(0).getString("entityName");
			this.idOffer=getIdOffer(nameOffer);
		} catch (JSONException e) {
			log.error("Error al obtener JSONObject");
			e.printStackTrace();
		}
		this.datDb=datDB;
	}
	
	/**
	 * Llama a los metodos de introducir o eliminar preferencia segun lo indique el parametro recibido.
	 * 
	 * @param action .- Accion a llevar a cabo: introducir o eliminar preferencia.
	 */
	public void applyAction(String action){
		if (action.equals("add"))
			introducePreference();
		else if (action.equals("delete"))
			deletePreference();
	}
	
	/**
	 * Introduce en el archivo de datos una nueva preferencia.
	 */
	public void introducePreference(){
		if (datDb){
			BigDecimal numeroDecimal = new BigDecimal(Math.random()*2).setScale(1,BigDecimal.ROUND_DOWN ); 
			float rate = 5 - numeroDecimal.floatValue();
			try {
				if (checkPrefPrev())
					deletePreference();
				model.setPreference(Long.parseLong(idUser), Long.parseLong(idOffer), rate);
				log.info("Introducida una nueva valoracion: "+idUser+","+idOffer+","+rate+"\n");
			} catch (TasteException e) {
				log.error("No se ha introducido correctamente la nueva preferencia");
				e.printStackTrace();
			}
		} else {
			checkPrefPrev();
			try {
				FileWriter fw = new FileWriter(file);
	            PrintWriter pw = new PrintWriter(fw); 
				BigDecimal numeroDecimal = new BigDecimal(Math.random()*2).setScale(1,BigDecimal.ROUND_DOWN ); 
				float rate = 5 - numeroDecimal.floatValue();
	            for(int i=0;i<lineas.size();i++){ 
	            	pw.println(lineas.elementAt(i)); 
	            } 
	        	pw.println(idUser+","+idOffer+","+rate); 
	        	pw.close();
				fw.close();
				log.info("Introducida una nueva valoracion: "+idUser+","+idOffer+","+rate+"\n");
			} catch (IOException iox) {
				iox.printStackTrace();
			}
		}
	}
	
	/**
	 * Este metodo se llama cuando se elimina una preferencia.
	 */
	public void deletePreference(){
		if (datDb) {
			try {
				model.removePreference(Long.parseLong(idUser), Long.parseLong(idOffer));
				log.info("Borrada valoracion del usuario: "+idUser);
				log.info("Para la oferta con identificador: "+idOffer);
			} catch (TasteException e) {
				log.error("No se ha podido elimar la preferencia");
				e.printStackTrace();
			}
		} else {
			checkPrefPrev();
			try {
				FileWriter fw = new FileWriter(file);
	            PrintWriter pw = new PrintWriter(fw); 
	            for(int i=0;i<lineas.size();i++){ 
	            	pw.println(lineas.elementAt(i)); 
	            } 
	        	pw.close();
				fw.close();
				log.info("Borrada valoracion del usuario: "+idUser);
				log.info("Para la oferta con identificador: "+idOffer);
			} catch (IOException iox) {
				iox.printStackTrace();
			}
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
	 * @return true o false, dependiendo si ya se habia producido una valoracion.
	 */
	private boolean checkPrefPrev(){
		if (datDb) {
			try {
				String url = cfg.getProperty("url");
				String user = cfg.getProperty("user");
				String pass =  cfg.getProperty("pass");
				Connection dbCon= DriverManager.getConnection(url, user, pass);
				String selectStatement = "SELECT value FROM ratings WHERE id_user = ? AND id_offer = ?";
				PreparedStatement prepStmt = (PreparedStatement) dbCon.prepareStatement(selectStatement);
				prepStmt.setLong(1, Long.parseLong(idUser));
				prepStmt.setLong(2, Long.parseLong(idOffer));
		    	ResultSet res = prepStmt.executeQuery();
		    	if (res.next()){
		    		log.info("Ya habia una valoracion anterior");
		    		return true;
		    	}
			} catch (SQLException e) {
				e.printStackTrace();
		    } catch (Exception e) {
		    	e.printStackTrace();
		    }
		    return false;
		} else {
			try {
				// Comprobamos si  existe la valoracion y en caso afirmativo la quitamos del las lineas que vamos a introducir en el archivo.
				FileReader fr = new FileReader(file);
				BufferedReader br = new BufferedReader(fr);
				String linea;
				while((linea=br.readLine())!=null){
					String[] datos = linea.split(",");
					if (idUser.equals(datos[0]) && idOffer.equals(datos[1]))
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
	}
}
