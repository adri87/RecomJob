package es.adri.pfc.algorithms;

import java.io.IOException;
import java.util.HashMap;

import org.apache.mahout.cf.taste.common.TasteException;

/**
 * Interfaz que implementa los metodos de los motores de recomendacion.
 * 
 * @author Adriano Jose Martin Gutierrez
 * @version 1.0
 */
public interface MotorRecom {

	/**
	 * Llama al motor de ejecucion y a su vez construye una Hashmap con las uris de las recomendaciones y su valoracion.
	 * 
	 * @return result .- HashMap con las ofertas recomendadas y su correspondiente peso.
	 */
	HashMap<String, Float> getResult();
	
	/**
	 * Lleva a cabo la evaluacion del motor de recomendacion. Devolviendo una valor de la precision que alcanza 
	 * para el entorno y el conjunto de datos disponibles.
	 *
	 * @return
	 * @throws IOException
	 * @throws TasteException
	 */
	Double[] getEval();

}
