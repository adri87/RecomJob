package es.adri.pfc.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.adri.pfc.algorithms.ItemBased;
import es.adri.pfc.algorithms.LogLikeliHoodSim;
import es.adri.pfc.algorithms.MotorRecom;
import es.adri.pfc.algorithms.SVD;
import es.adri.pfc.algorithms.SlopeOne;
import es.adri.pfc.algorithms.UserBased;
import es.adri.pfc.config.Configuration;
import es.adri.pfc.connections.ConnectionLMF;
import es.adri.pfc.feed.FeedBack;

/**
 * Servlet del sistema recomendador.
 * 
 * @author Adriano Jose Martin Gutierrez
 * @version 1.0
 */
public class JobMatch extends HttpServlet{
	private Logger log;
	private String baseUrl;
	private Configuration cfg;

	private static final long serialVersionUID = 1L;
	
	/**
	 * Inicializacion del servlet.
	 */
    public void init() throws ServletException {
    	log =  LoggerFactory.getLogger(JobMatch.class);
    	baseUrl = getServletContext().getRealPath("/");
		String urlFileConfiguration = getServletContext().getRealPath("/config/configuration.properties");
		cfg = new Configuration(urlFileConfiguration);
    	super.init();
		log.info("Se ha inicializado correctamente el servlet");
    }
       
	/**
	 * Se encarga de recoger la peticion realizada por el usuario, producir la comunicacion con los motores de recomendacion y devolver 
	 * la respuesta al cliente.
	 *  
	 * @param request .- Peticion realizada
	 * @param response .- Respuesta del servlet. Sera el resultado del recomendador que se haya indicado en el archivo de configuracion.
	 * @throws ServletException
	 * @throws IOException
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		log.info("");
		log.info("------------------------------------------------------------");
		log.info("_______________________RECOMENDACION________________________");
		log.info("------------------------------------------------------------");		
		log.info("");
		
		// Extraccion de los parametros que contiene la "query"
		log.info("Llega peticion de recomendacion");
		long userId = Long.parseLong(request.getParameter("user"));
		log.info("El usuario para el que se pide recomendacion es el: "+userId);
		
		// Definicion de clases
		JSONArray resp = new JSONArray();
		ConnectionLMF conLmf = new ConnectionLMF(baseUrl, cfg);
		// Seleccion del motor de recomendacion
		MotorRecom recom = getMotor(conLmf, userId);

		// Obtenemos la uris de ofertas y ejecutamos el recomendador
		ArrayList<String> urisResources = conLmf.getUris();		
		HashMap<String, Float> resultRecom = recom.getResult();

		// Se construye la respuesta del servlet en un JSONArray 
		for (int i = 0; i < urisResources.size(); i++) {
			try {
				JSONObject jo = new JSONObject();
				if (resultRecom.containsKey(urisResources.get(i))){
					jo.put("url", urisResources.get(i));
					jo.put("sim", resultRecom.get(urisResources.get(i)));
				} else {
					jo.put("url", urisResources.get(i));
					jo.put("sim", 0);
				}
				resp.put(jo);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		log.info("Recomendacion realizada: "+resp);
		
		// Se evalua el recomendador
		log.info("");
		log.info("------------------------------------------------------------");
		log.info("_________________________EVALUACION_________________________");
		log.info("------------------------------------------------------------");		
		log.info("");
		Double[] resulteval = recom.getEval();
		log.info("El resultado de evaluacion del sistema recomendador es: ");
		log.info("Precision = "+resulteval[0]);
		log.info("Recall = "+resulteval[1]);
		log.info("Medida F1 ="+resulteval[2]);

		
		// Devolviendo la salida
		response.setContentType("application/json");
		response.addHeader("Access-Control-Allow-Origin","*");
		PrintWriter pw = new PrintWriter(response.getOutputStream());
		pw.println(resp);
		pw.close();
	}

	/**
	 * Utilizado por el servlet para añadir o eliminar una preferencia por parte del usuario.
	 *  
	 * @param request .- Preferencia a borrar o eliminar.
	 * @param response .- Ok. La operación se ha llevado a cabo con exito
	 * @throws ServletException
	 * @throws IOException
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// Realimentacion del archivo de datos
		String user = request.getParameter("user");
		String search = request.getParameter("offer");
		String action = request.getParameter("action");
		FeedBack feed = new FeedBack(baseUrl, user, search, cfg);
		if (action.equals("add"))
			feed.introduceRate();
		else if (action.equals("delete"))
			feed.deletePunt();
		
		// Devuelve respuesta
		response.setContentType("application/json");
		response.addHeader("Access-Control-Allow-Origin","*");
		PrintWriter pw = new PrintWriter(response.getOutputStream());
		pw.println("[OK]");
		pw.close();
	}
	
	/**
	 * Devuelve el motor de recomendacion seleccionado en el archivo de configuracion.
	 * 
	 * @return recom .- Motor de recomendacion elegido.
	 * @throws IOException 
	 */
	private MotorRecom getMotor(ConnectionLMF conLmf, long userId) throws IOException{
		String motor = cfg.getProperty("motorRecom");
		log.info("El motor de recomendacion elegido es el: "+motor);
		MotorRecom recom;
		if (motor.equals("1"))
			recom = new UserBased(baseUrl, conLmf, userId, cfg);
		else if (motor.equals("2"))
			recom = new ItemBased(baseUrl, conLmf, userId, cfg);
		else if (motor.equals("3"))
			recom = new LogLikeliHoodSim(baseUrl, conLmf, userId, cfg);
		else if (motor.equals("4"))
			recom = new SlopeOne(baseUrl, conLmf, userId, cfg);
		else if (motor.equals("5"))
			recom = new SVD(baseUrl, conLmf, userId, cfg);
		else
			recom = new UserBased(baseUrl, conLmf, userId, cfg);
		return recom;
	}
}
