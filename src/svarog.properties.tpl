conn.type={{ db_conn_type }}
driver.name={{ db_driver_name }}

conn.string={{ db_connstring }}
user.name={{ db_username }}
user.password={{ db_password }}

conn.dbType={{ db_type }}
conn.defaultSchema={{ db_schema }}


sys.jdbc.batch_size=10
jndi.datasource=java:/comp/env/jdbc/svarog

sys.core.cleanup_time = 10
sys.core.is_debug = true
sys.masterRepo=svarog
sys.defaultLocale={{ locale }}
sys.defaultDateFormat=dd/MM/yyyy
sys.defaultTimeFormat=HH:mm:ss
sys.defaultJSDateFormat=d/m/Y
sys.defaultJSTimeFormat=H:i

######################################################################
# GIS specific configuration
######################################################################
sys.gis.default_srid=6316
sys.gis.geom_handler=POSTGIS
sys.gis.grid_size=10
sys.gis.tile_cache=100
#a scale of 1000 specifies milimeter precision. 1 signifies meter precision.
sys.gis.precision_scale=1000


sys.gis.allow_boundary_intersect=false
sys.gis.legal_sdi_unit_type=1


filestore.conn.type=DEFAULT
filestore.table=svarog_filestore
filestore.type=FILESYSTEM
filestore.path=/svarog/filestore

custom.jar=./svarog_custom_afsard_dp-1.0_dev.jar
frontend.services_host=http://192.168.100.155:{{ tomcat_port }}/triglav_rest/
frontend.gui_host=http://192.168.100.155:{{ tomcat_port }}

mail.from = gitlab@prtech.mk
mail.username = gitlab@prtech.mk
mail.password = Fisherman04041980
mail.host = smtp.zoho.com
mail.smtp.auth=true
mail.smtp.starttls.enable=true
mail.smtp.port=587
mail.format=text/html; charset=UTF-8;


security.crypto_type=hsm
security.hsm_cfg_file=C\:\\SoftHSM\\softhsm_svarog.cfg

print.jrxml_path={{ tomcat_path }}/webapps/triglav_rest/reports
triglav.plugin_path={{ tomcat_path }}/webapps/triglav_rest/plugins
sys.service_class=com.peruntech.agriPluginManager.AdapterLoader
