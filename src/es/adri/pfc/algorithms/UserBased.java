package es.adri.pfc.algorithms;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.common.Weighting;
import org.apache.mahout.cf.taste.eval.IRStatistics;
import org.apache.mahout.cf.taste.eval.RecommenderBuilder;
import org.apache.mahout.cf.taste.eval.RecommenderIRStatsEvaluator;
import org.apache.mahout.cf.taste.impl.eval.GenericRecommenderIRStatsEvaluator;
import org.apache.mahout.cf.taste.impl.model.file.FileDataModel;
import org.apache.mahout.cf.taste.impl.neighborhood.NearestNUserNeighborhood;
import org.apache.mahout.cf.taste.impl.recommender.CachingRecommender;
import org.apache.mahout.cf.taste.impl.recommender.GenericUserBasedRecommender;
import org.apache.mahout.cf.taste.impl.similarity.AveragingPreferenceInferrer;
import org.apache.mahout.cf.taste.impl.similarity.CachingUserSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.EuclideanDistanceSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.PearsonCorrelationSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.SpearmanCorrelationSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.TanimotoCoefficientSimilarity;
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
 * Recomendador basado en usuario.
 * 
 * @author Adriano Jose Martin Gutierrez
 * @version 1.0
 */
public class UserBased implements MotorRecom{
	private Logger log;
	private String baseUrl;
	private ConnectionLMF conLmf;
	private Configuration cfg;
	private long userId;
	private int numRecom;
	private DataModel model;


	/**
	 * Constructor del recomendador basado en usuario.
	 * 
	 * @param baseUrl .- Direccion base donde se encuentra desplegado el sistema recomendador.
	 * @param conLmf .- Objeto de la clase ConnectionLMF.
	 * @param userId .- Identificador de usuario
	 * @param cfg .- Objeto de configuracion del servlet
	 * @throws IOException
	 */
	public UserBased (String baseUrl, ConnectionLMF conLmf, long userId, Configuration cfg) throws IOException{
		log = LoggerFactory.getLogger(UserBased.class);
		this.baseUrl=baseUrl;
		this.conLmf=conLmf;
		this.userId=userId;
		this.cfg=cfg;
		this.numRecom=Integer.parseInt(cfg.getProperty("numRecom"));
		log.info("Numero de recomendaciones solicitadas: "+this.numRecom);
		String filePath = baseUrl+"resources/"+cfg.getProperty("fichero");
		this.model = new FileDataModel(new File(filePath));
		log.info("Construido recomendador UserBased");
	}
	
	/**
	 * Ejecuta el motor de recomendacion y devuelve una lista con los objetos recomendados.
	 * 
	 * @return recommendations .- Lista de ofertas recomendadas y el peso que poseen dentro de la recomendacion.
	 */
	private List<RecommendedItem> getRecommendations(){
		try {
			UserSimilarity userSimilarity = getSimilarityMetric();
			UserNeighborhood neighborhood = new NearestNUserNeighborhood(10, userSimilarity, model);
			Recommender recommender = new GenericUserBasedRecommender(model, neighborhood, userSimilarity);
			Recommender cachingRecommender = new CachingRecommender(recommender);
			List<RecommendedItem> recommendations = cachingRecommender.recommend(userId, numRecom);
			log.info("Numero de recomendaciones obtenidas: "+recommendations.size());
			return recommendations;
		} catch (TasteException e) {
			log.error("Fallo al obtener recomendaciones de recomendador UserBased: "+e.getMessage());
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Devuelve la metrica de similiridad que se ha de aplicar para realizar el calculo de las recomendaciones.
	 * 
	 * @return usrSim .- La metrica de similiridad aplica al modelo de datos.
	 */
	private UserSimilarity getSimilarityMetric(){
		String metric = cfg.getProperty("metodoUsSim");
		log.info("La metrica utilizada es la numero: "+metric);
		UserSimilarity usrSim = null;
		try {
			if (metric.equals("1")) {
				usrSim = new PearsonCorrelationSimilarity(model, Weighting.WEIGHTED);
				usrSim.setPreferenceInferrer(new AveragingPreferenceInferrer(model));
			}
			else if (metric.equals("2")) {
				usrSim = new EuclideanDistanceSimilarity(model);
				usrSim.setPreferenceInferrer(new AveragingPreferenceInferrer(model));
			}
			else if (metric.equals("3"))
				usrSim = new CachingUserSimilarity(new SpearmanCorrelationSimilarity(model), model);
			else if (metric.equals("4"))
				usrSim = new TanimotoCoefficientSimilarity(model);
			else
				usrSim = new PearsonCorrelationSimilarity(model, Weighting.WEIGHTED); 
		} catch (TasteException e) {
			log.error("Error al obtener la metrica para la calcular la similaridad: "+e.getMessage());
			e.printStackTrace();	
		}
		return usrSim;
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
		log.info("Devolviendo resultado de recomendador UserBased");
		return result;
	}
	
	/**
	 * Devuelve la evaluacion del recomendador para la aplicacion y el conjunto de datos.
	 * 
	 * @return valoracion .- Puntuacion de evaluacion del recomendador.
	 */
	public Double[] getEval(){
		Double[] evaluation = new Double[3];
		String filePath = baseUrl+"resources/"+cfg.getProperty("fichero");
		double evalPorc = Double.parseDouble(cfg.getProperty("conjEva"));
		int recomToCons = Integer.parseInt(cfg.getProperty("recomToCons"));
		try {
			DataModel myModel = new FileDataModel(new File(filePath));
			
			RecommenderBuilder builder = new RecommenderBuilder() {
				public Recommender buildRecommender(DataModel model) throws TasteException {
					UserSimilarity userSimilarity = getSimilarityMetric();
					UserNeighborhood neighborhood = new NearestNUserNeighborhood(10, userSimilarity, model);
					return new GenericUserBasedRecommender(model, neighborhood, userSimilarity);
				}
			};
			
			RecommenderIRStatsEvaluator evaluatorIR = new GenericRecommenderIRStatsEvaluator();
			IRStatistics stats = evaluatorIR.evaluate(builder, null, myModel, null, recomToCons, GenericRecommenderIRStatsEvaluator.CHOOSE_THRESHOLD, evalPorc);
			evaluation[0] = stats.getPrecision();
			evaluation[1] = stats.getRecall();
			evaluation[2] = stats.getF1Measure();
		} catch (IOException e) {
			log.error("Error al construir el dataModel en la evaluacion");
			e.printStackTrace();
		} catch (TasteException e) {
			log.error("Error a evaluar el recomendador UserBased");
			e.printStackTrace();
		}
		return evaluation;
	}
}
