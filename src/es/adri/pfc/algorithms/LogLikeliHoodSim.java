package es.adri.pfc.algorithms;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.eval.IRStatistics;
import org.apache.mahout.cf.taste.eval.RecommenderBuilder;
import org.apache.mahout.cf.taste.eval.RecommenderEvaluator;
import org.apache.mahout.cf.taste.eval.RecommenderIRStatsEvaluator;
import org.apache.mahout.cf.taste.impl.eval.AverageAbsoluteDifferenceRecommenderEvaluator;
import org.apache.mahout.cf.taste.impl.eval.GenericRecommenderIRStatsEvaluator;
import org.apache.mahout.cf.taste.impl.model.file.FileDataModel;
import org.apache.mahout.cf.taste.impl.model.jdbc.MySQLJDBCDataModel;
import org.apache.mahout.cf.taste.impl.neighborhood.NearestNUserNeighborhood;
import org.apache.mahout.cf.taste.impl.recommender.GenericBooleanPrefUserBasedRecommender;
import org.apache.mahout.cf.taste.impl.similarity.LogLikelihoodSimilarity;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.neighborhood.UserNeighborhood;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.apache.mahout.cf.taste.recommender.Recommender;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.adri.pfc.config.Configuration;
import es.adri.pfc.connections.ConnectionLMF;

/**
 * Recomendador LogLikeliHoodSimilarity
 * 
 * @author Adriano Jose Martin Gutierrez
 * @version 1.0
 */
public class LogLikeliHoodSim implements MotorRecom{
	private Logger log;
	private String baseUrl;
	private ConnectionLMF conLmf;
	private Configuration cfg;
	private String orDat;
	private long userId;
	private int numRecom;
	private DataModel model;
	private Recommender recommender;

	
	/**
	 * Constructor del recomendador LogLikeliHoodSimilarity.
	 * 
	 * @param baseUrl .- Direccion base donde se encuentra desplegado el sistema recomendador.
	 * @param conLmf .- Objeto de la clase ConnectionLMF.
	 * @param userId .- Identificador de usuario
	 * @param cfg .- Objeto de configuracion del servlet
	 * @throws IOException
	 */
	public LogLikeliHoodSim(String baseUrl, ConnectionLMF conLmf, long userId, Configuration cfg) throws IOException{
		this.log = LoggerFactory.getLogger(LogLikeliHoodSim.class);
		this.baseUrl=baseUrl;
		this.conLmf=conLmf;
		this.userId=userId;
		this.cfg=cfg;
		this.orDat=cfg.getProperty("origendat");
		this.numRecom=Integer.parseInt(cfg.getProperty("numRecom"));
		log.info("Numero de recomendaciones solicitadas: "+this.numRecom);
		if (orDat.equals("file")) {
			// Si se basa el modelo de datos se encuentra en un fichero csv
			String filePath = baseUrl+"resources/"+cfg.getProperty("fichero");
			this.model = new FileDataModel(new File(filePath));
		} else if (orDat.equals("bd")) {
			// Si el modelo de datos se ha configurado en una base de datos MySQL
			this.model = new MySQLJDBCDataModel(cfg.getDataSource(), cfg.getProperty("nametable"), 
					cfg.getProperty("coluser"), cfg.getProperty("coloffer"), cfg.getProperty("colpref"), null);
		} else {
			log.error("El origen de datos especificado es incorrecto");
		}
		log.info("Construido recomendador LogLikeliHood");
	}
	
	/**
	 * Ejecuta el motor de recomendacion y devuelve una lista con los objetos recomendados.
	 * 
	 * @return recommendations .- Lista de ofertas recomendadas y el peso que poseen dentro de la recomendacion.
	 */
	private List<RecommendedItem> getRecommendations(){
		try {
			UserSimilarity similarity = new LogLikelihoodSimilarity(model);
	        UserNeighborhood neighborhood = new NearestNUserNeighborhood(10, similarity, model);
	        recommender = new GenericBooleanPrefUserBasedRecommender(model, neighborhood, similarity);
//			recommender = new GenericBooleanPrefItemBasedRecommender(model, (ItemSimilarity) similarity);
			List<RecommendedItem> recommendations = recommender.recommend(userId, numRecom);
			log.info("Numero de recomendaciones obtenidas: "+recommendations.size());
			return recommendations;
		} catch (TasteException e) {
			log.error("Fallo al obtener recomendaciones de recomendador LogLikeliHood: "+e.getMessage());
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Llama al motor de ejecucion y a su vez construye una Hashmap con las uris de las recomendaciones y su valoracion.
	 * 
	 * @return result .- HashMap con las ofertas recomendadas y su correspondiente peso.
	 */
	public HashMap<String, Float> getResult(){
		List<RecommendedItem> recommendations = getRecommendations();
		String query = conLmf.readSparql(baseUrl+"/resources/OfferById.sparql");
		HashMap<String, Float> result = new HashMap<>();
		for (int i = 0; i < recommendations.size(); i++) {
			try {
				RecommendedItem item = recommendations.get(i);
				String id = String.valueOf(item.getItemID());
				float value = item.getValue();
				String urlOffer = conLmf.getResponseQuerySparql(query, true, id).getJSONObject(0).getJSONObject("offer").getString("value");
				result.put(urlOffer, value);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		log.info("Devolviendo resultado de recomendador LogLikelHood");
		return result;
	}
	
	/**
	 * Devuelve la evaluacion del recomendador para la aplicacion y el conjunto de datos.
	 * 
	 * @return valoracion .- Puntuacion de evaluacion del recomendador.
	 */
	public Double[] getEval(){
		Double[] evaluation = new Double[4];
		double trainingPerc = Double.parseDouble(cfg.getProperty("entPorc"));
		double evalPerc = Double.parseDouble(cfg.getProperty("evalPorc"));
		int recomToCons = Integer.parseInt(cfg.getProperty("recomToCons"));
		try {
			
			RecommenderBuilder builder = new RecommenderBuilder() {
				public Recommender buildRecommender(DataModel model) throws TasteException {
			        UserSimilarity similarity = new LogLikelihoodSimilarity(model);
			        UserNeighborhood neighborhood = new NearestNUserNeighborhood(10, similarity, model);
			        return new GenericBooleanPrefUserBasedRecommender(model, neighborhood, similarity);
				}
			};
			
			RecommenderEvaluator evaluator = new AverageAbsoluteDifferenceRecommenderEvaluator();
			evaluation[0] = evaluator.evaluate(builder, null, model, trainingPerc, evalPerc);
			
			RecommenderIRStatsEvaluator evaluatorIR = new GenericRecommenderIRStatsEvaluator();
			IRStatistics stats = evaluatorIR.evaluate(builder, null, model, null, recomToCons, GenericRecommenderIRStatsEvaluator.CHOOSE_THRESHOLD, evalPerc);
			evaluation[1] = stats.getPrecision();
			evaluation[2] = stats.getRecall();
			evaluation[3] = stats.getF1Measure();		
		} catch (TasteException e) {
			log.error("Error a evaluar el recomendador LogLikelHood");
			e.printStackTrace();
		}
		return evaluation;
	}
}
