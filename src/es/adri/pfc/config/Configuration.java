package es.adri.pfc.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Clase de configuracion del proyecto.
 * Se basa en la utilizacion de un archivo properties en el que se encontraran todos
 * los campos necesarios para el correcto funcionamiento del proyecto.
 * 
 * @author Adriano Jose Martin Gutierrez
 * @version 1.0
 */
public class Configuration {
	private Logger log = LoggerFactory.getLogger(Configuration.class);
    Properties properties = null;
 
    /**
     * Genera un objeto de configuracion a partir de un determinado archivo.
     * 
     * @param file .- Archivo properties del cual se extraen todos los datos necesarios.
     */
    public Configuration(String file) {
    	log.info("Creando objeto de configuracion del servlet...");
    	properties = new Properties();
    	try {
    		FileInputStream in = new FileInputStream(file);
    		properties.load(in);
			in.close();
		} catch (IOException e) {
			log.error("Fallo al iniciar configuracion: "+e.getMessage());
			e.printStackTrace();
		}
    	log.info("Creado objeto Configuration");
    }
 
    /**
     * Retorna la propiedad de configuracion solicitada.
     *
     * @param key .- Campo del cual se quiere obtener su valor.
     * @return Valor del campo solicitado.
     */
    public String getProperty(String key) {
//    	log.info("Obteniendo la propiedad "+key+"  de la configuración general del servlet");
        return this.properties.getProperty(key);
    }

}
