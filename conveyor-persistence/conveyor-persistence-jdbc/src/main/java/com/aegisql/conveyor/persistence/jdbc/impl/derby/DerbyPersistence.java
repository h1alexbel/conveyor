package com.aegisql.conveyor.persistence.jdbc.impl.derby;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.LongSupplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.LoadType;
import com.aegisql.conveyor.persistence.core.Persistence;
import com.aegisql.conveyor.persistence.jdbc.BlobConverter;
import com.aegisql.conveyor.persistence.jdbc.EnumConverter;

public class DerbyPersistence<K> implements Persistence<K>{

	private final static Logger LOG = LoggerFactory.getLogger(DerbyPersistence.class);
	
	public static class DerbyPersistenceBuilder<K> {
		
		private DerbyPersistenceBuilder(Class<K> clas) {
			this.keyClass = clas;
			if(clas == Integer.class) {
				this.keyType = "CART_KEY INT";
			} else if(clas == Long.class) {
				this.keyType = "CART_KEY BIGINT";
			} else if(clas == UUID.class) {
				this.keyType = "CART_KEY CHAR(36)";
			} else if(clas.isEnum()) {
				int maxLength = 0;
				for(Object o:clas.getEnumConstants()) {
					maxLength = Math.max(maxLength, o.toString().length());
				}
				this.keyType = "CART_KEY CHAR("+maxLength+")";
			} else {
				this.keyType = "CART_KEY VARCHAR(255)";
			}
			final AtomicLong idGen = new AtomicLong(System.currentTimeMillis() * 1000000);
			idSupplier = idGen::get;
		}
		
		private Class<K> keyClass;
		private BiConsumer<PreparedStatement, K> keyPlacer;
		private String keyType = "CART_KEY VARCHAR(255)";
		private final static String PROTOCOL = "jdbc:derby:";
		private final static int PORT = 1527;
		
		private final static String EMBEDDED = "org.apache.derby.jdbc.EmbeddedDriver";
		private final static String CLIENT   = "org.apache.derby.jdbc.ClientDriver";
		
		private final static String EMBEDDED_URL_PATTERN = PROTOCOL+"{schema}";
		private final static String CLIENT_URL_PATTERN   = PROTOCOL+"//{host}:{port}/{schema}";//;user=judy;password=no12see";
		
		private boolean embedded  = true;
		private String driver     = EMBEDDED; //default
		private String urlPattern = EMBEDDED_URL_PATTERN; //default
		private String host       = "localhost"; //default
		private String username   = "";
		private String password   = "";
		private int port          = 0; //default
		private boolean create    = true;
		
		private String schema      = "conveyor_db";
		private String partTable   = "PART";
		private String completedLogTable = "COMPLETED_LOG";
		
		private LongSupplier idSupplier;
		
		public DerbyPersistenceBuilder<K> embedded(boolean embedded) {
			if(embedded) {
				this.embedded = true;
				this.driver = EMBEDDED;
				this.urlPattern = EMBEDDED_URL_PATTERN;
				this.port = 0;
			} else {
				this.embedded = false;
				this.driver = CLIENT;
				this.urlPattern = CLIENT_URL_PATTERN;
				if(this.port != 0) {
					this.port = PORT;
				}
			}
			return this;
		}

		public DerbyPersistenceBuilder<K> username(String username) {
			this.username = username;
			return this;
		}
		
		public DerbyPersistenceBuilder<K> password(String password) {
			this.password = password;
			return this;
		}

		public DerbyPersistenceBuilder<K> schema(String schema) {
			this.schema = schema;
			return this;
		}

		public DerbyPersistenceBuilder<K> port(int port) {
			this.port = port;
			return this;
		}

		public DerbyPersistenceBuilder<K> idSupplier(LongSupplier idSupplier) {
			this.idSupplier = idSupplier;
			return this;
		}

		public DerbyPersistence<K> build() throws Exception {
			LOG.debug("DERBY PERSISTENCE");

			Class.forName(driver);
			Properties properties = new Properties();
			String url = urlPattern;
			url = url.replace("{schema}", schema);
			if(embedded && create) {
				properties.setProperty("create", "true");
			} else {
				url = url.replace("{host}", host);
				url = url.replace("{port}", ""+port);
				if( ! username.isEmpty()) {
					properties.setProperty("user", username);
				}
				if( ! password.isEmpty()) {
					properties.setProperty("password", password);
				}
			}
			
			LOG.debug("Driver: {}",driver);
			LOG.debug("Connection Url: {}",url);
			LOG.debug("Schema: {}",schema);
			
			Connection conn = DriverManager.getConnection(url, properties);
			DatabaseMetaData meta = conn.getMetaData();
			LOG.debug("Connected!");
			
			ResultSet tables = meta.getTables(null,null,null,null);
			boolean partTableFound   = false;
			boolean partIdTableFound = false;
			boolean keyLogTableFound = false;
			while(tables.next()) {
				String tableSchema = tables.getString("TABLE_SCHEM");
				String tableName = tables.getString("TABLE_NAME");
				if(tableName.equalsIgnoreCase(partTable)) {
					partTableFound = true;
				}
				if(tableName.equalsIgnoreCase(completedLogTable)) {
					keyLogTableFound = true;
				}
			}
			if( ! partTableFound ) {
				try(Statement st = conn.createStatement() ) {
					String sql = "CREATE TABLE "
								+partTable+" ("
								+"ID BIGINT PRIMARY KEY"
								+",LOAD_TYPE CHAR(15)"
								+","+keyType
								+",CART_LABEL VARCHAR(100)"
								+",CREATION_TIME TIMESTAMP NOT NULL"
								+",EXPIRATION_TIME TIMESTAMP NOT NULL"
								+",CART_VALUE BLOB"
								+",CART_PROPERTIES VARCHAR(1024)"
								+",ARCHIVED SMALLINT NOT NULL DEFAULT 0"
								+")";
					LOG.debug("Table '{}' not found. Trying to create...\n{}",partTable,sql);
					st.execute(sql);
					LOG.debug("Table '{}' created",partTable);
					st.execute("CREATE INDEX PART_IDX ON PART(CART_KEY)");
					LOG.debug("Index PART_IDX ON PART(CART_KEY) created");

				} 
			} else {
				LOG.debug("Table '{}' already exists",partTable);
			}

			if( ! keyLogTableFound ) {
				try(Statement st = conn.createStatement() ) {
					String sql = "CREATE TABLE "
								+completedLogTable+" ("
								+keyType+" PRIMARY KEY"
								+",COMPLETION_TIME TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP"
								+")";
					LOG.debug("Table '{}' not found. Trying to create...\n{}",completedLogTable,sql);
					st.execute(sql);
					LOG.debug("Table '{}' created",completedLogTable);
				} 
			} else {
				LOG.debug("Table '{}' already exists",completedLogTable);
			}

			String saveCartQuery = "INSERT INTO " + partTable + "("
					+"ID"
					+",LOAD_TYPE"
					+",CART_KEY"
					+",CART_LABEL"
					+",CREATION_TIME"
					+",EXPIRATION_TIME"
					+",CART_VALUE"
					+",CART_PROPERTIES"
					+") VALUES (?,?,?,?,?,?,?,?)"
					;
			
			return new DerbyPersistence<K>(
					conn
					,idSupplier
					,saveCartQuery
					);
		}

	}

	private final Connection   conn;
	private final LongSupplier idSupplier;
	private final BlobConverter blobConverter;
	private final EnumConverter<LoadType> loadTypeConverter = new EnumConverter<>(LoadType.class);
	
	private final String saveCartQuery;
	
	public DerbyPersistence(
			Connection conn
			,LongSupplier idSupplier
			,String saveCartQuery
			) {
		this.conn          = conn;
		this.idSupplier    = idSupplier;
		this.blobConverter = new BlobConverter<>(conn);
		this.saveCartQuery = saveCartQuery;
	}

	public static <K> DerbyPersistenceBuilder<K> forKeyClass(Class<K> clas) {
		return new DerbyPersistenceBuilder<K>(clas);
	}
	
	@Override
	public long nextUniquePartId() {
		return idSupplier.getAsLong();
	}

	@Override
	public <L> void savePart(long id, Cart<K, ?, L> cart) {
		try(PreparedStatement st = conn.prepareStatement(saveCartQuery) ) {
			st.setLong(1, id);
			st.setString(2, loadTypeConverter.toPersistence(cart.getLoadType()));
			st.setObject(3, cart.getKey());
			st.setObject(4, cart.getLabel());
			st.setTimestamp(5, new Timestamp(cart.getCreationTime()));
			st.setTimestamp(6, new Timestamp(cart.getExpirationTime()));
			st.setBlob(7, blobConverter.toPersistence((Serializable) cart.getValue()));
			st.setString(8, cart.getAllProperties().toString());
			st.execute();
		} catch (Exception e) {
	    	LOG.error("SavePart Exception: {}",cart,e.getMessage());
	    	throw new RuntimeException("Save Part failed",e);
		}
	}

	@Override
	public void savePartId(K key, long partId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void saveCompletedBuildKey(K key) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public <L> Cart<K, ?, L> getPart(long id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<Long> getAllPartIds(K key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <L> Collection<Cart<K, ?, L>> getAllParts() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<K> getCompletedKeys() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void archiveParts(Collection<Long> ids) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void archiveKeys(Collection<K> keys) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void archiveCompleteKeys(Collection<K> keys) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void archiveAll() {
		// TODO Auto-generated method stub
		
	}
	
}