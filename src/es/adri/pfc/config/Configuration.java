package es.adri.pfc.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;

/**
 * Clase de configuracion del proyecto.
 * Se basa en la utilizacion de un archivo properties en el que se encontraran todos
 * los campos necesarios para el correcto funcionamiento del proyecto.
 * 
 * @author Adriano Jose Martin Gutierrez
 * @version 1.0
 */
public class Configuration {
	private MysqlDataSource dataSource;
	private String driver = "com.mysql.jdbc.Driver";
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
    	
		dataSource = new MysqlDataSource();
		dataSource.setServerName(getProperty("servername"));
		dataSource.setDatabaseName(getProperty("dbname"));
		dataSource.setUser(getProperty("user"));
		dataSource.setPassword(getProperty("pass"));
		log.info("Establecida conexión de datos a través de Mysql");
		
		try {
			Class.forName(driver).newInstance();
		} catch (Exception e) {
			log.error("Driver MySQL not load");
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
        return this.properties.getProperty(key);
    }
    
	/**
	 * Nos devuelve el objeto dataSource de nuestro recomendador.
	 * 
	 * @return dataSource
	 */
	public MysqlDataSource getDataSource(){
		return dataSource;
	}

}
