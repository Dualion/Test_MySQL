package test.mysql;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class SoftMySQL extends Activity {

    static String SQLEjecutar;
    static Connection conexionMySQL;
    String ipServidorMySQL, contrasenaMySQL, usuarioMySQL, puertoMySQL;
    String catalogoMySQL;
    TextView textSQL, textResultadoSQL;
    String[] listaCatalogos;
    private Button buttonEjecutar;
    private Button buttonCatalogos;
    private Spinner spnCatalogos;
    private CheckBox chbSQLModificacion;

    public static String ejecutarConsultaSQL(Boolean SQLModificacion, Context context) {
        try {
            String resultadoSQL = "";
            //ejecutamos consulta SQL de selección (devuelve datos)
            if (!SQLModificacion) {
                Statement st = conexionMySQL.createStatement();
                ResultSet rs = st.executeQuery(SQLEjecutar);

                //número de columnas (campos) de la consula SQL
                Integer numColumnas = rs.getMetaData().getColumnCount();

                //obtenemos el título de las columnas
                for (int i = 1; i <= numColumnas; i++) {
                    if (!resultadoSQL.equals(""))
                        if (i < numColumnas)
                            resultadoSQL = resultadoSQL +
                                    rs.getMetaData().getColumnName(i) + ";";
                        else
                            resultadoSQL = resultadoSQL +
                                    rs.getMetaData().getColumnName(i);
                    else if (i < numColumnas)
                        resultadoSQL =
                                rs.getMetaData().getColumnName(i) + ";";
                    else
                        resultadoSQL =
                                rs.getMetaData().getColumnName(i);
                }


                //mostramos el resultado de la consulta SQL
                while (rs.next()) {
                    resultadoSQL = resultadoSQL + "\n";

                    //obtenemos los datos de cada columna
                    for (int i = 1; i <= numColumnas; i++) {
                        if (rs.getObject(i) != null) {
                            if (!resultadoSQL.equals(""))
                                if (i < numColumnas)
                                    resultadoSQL = resultadoSQL +
                                            rs.getObject(i).toString() + ";";
                                else
                                    resultadoSQL = resultadoSQL +
                                            rs.getObject(i).toString();
                            else if (i < numColumnas)
                                resultadoSQL = rs.getObject(i).toString() + ";";
                            else
                                resultadoSQL = rs.getObject(i).toString();
                        } else {
                            if (!resultadoSQL.equals(""))
                                resultadoSQL = resultadoSQL + "null;";
                            else
                                resultadoSQL = "null;";
                        }
                    }
                    resultadoSQL = resultadoSQL + "\n";
                }
                st.close();
                rs.close();
            }
            // consulta SQL de modificación de
            // datos (CREATE, DROP, INSERT, UPDATE)
            else {
                Statement st = conexionMySQL.createStatement();
                int numAfectados = st.executeUpdate(SQLEjecutar);
                resultadoSQL = "Registros afectados: " + String.valueOf(numAfectados);
                st.close();
            }
            return resultadoSQL;
        } catch (Exception e) {
            Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return "";
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_soft_my_sql);

        textSQL = (TextView) findViewById(R.id.txtSQL);
        spnCatalogos = (Spinner) findViewById(R.id.lsCatalogos);
        textResultadoSQL = (TextView) findViewById(R.id.txtResultadoSQL);
        buttonEjecutar = (Button) findViewById(R.id.btEjecutar);
        buttonCatalogos = (Button) findViewById(R.id.btCatalogos);
        chbSQLModificacion = (CheckBox) findViewById(R.id.opConsultaModificacion);

        //Botón para mostrar lista de catálogos (bases de datos) de MySQL
        buttonCatalogos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                obtenerListaCatalogos();
                try {
                    ArrayAdapter<String> adaptador =
                            new ArrayAdapter<String>(SoftMySQL.this,
                                    android.R.layout.simple_list_item_1, listaCatalogos);

                    adaptador.setDropDownViewResource(
                            android.R.layout.simple_spinner_dropdown_item);
                    spnCatalogos.setAdapter(adaptador);
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(),
                            "Error: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        //Botón para ejecutar consulta SQL en MySQL
        buttonEjecutar.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                cargarConfiguracion();
                SQLEjecutar = textSQL.getText().toString();
                catalogoMySQL = spnCatalogos.getSelectedItem().toString();
                conectarBDMySQL(usuarioMySQL, contrasenaMySQL,
                        ipServidorMySQL, puertoMySQL, catalogoMySQL);
                String resultadoSQL =
                        ejecutarConsultaSQL(chbSQLModificacion.isChecked(), getApplication());
                textResultadoSQL.setText(resultadoSQL);
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.soft_my_sql, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.menu_configuracion:
                menuConfiguracion();
                return true;

            case R.id.menu_acerca_de:
                visitarURL("http://www.mumus.es");
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }

    }

    //abrir ventana-activity Configuración
    public void menuConfiguracion() {
        Intent i = new Intent(SoftMySQL.this, Configuracion.class);
        startActivity(i);
    }

    //Abrir navegador con URL especificada
    public void visitarURL(String url) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(browserIntent);
    }

    //guardar configuración aplicación Android usando SharedPreferences
    public void guardarConfiguracion() {
        SharedPreferences prefs = getSharedPreferences("SoftMySQL", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("SQL", textSQL.getText().toString());
        editor.putString("Catálogo", spnCatalogos.getSelectedItem().toString());
        editor.apply(); //editor.commit();

    }

    //cargar configuración aplicación Android usando SharedPreferences
    public void cargarConfiguracion() {
        //leemos los valores de conexión al servidor
        //MySQL desde SharedPreferences
        SharedPreferences prefs =
                getSharedPreferences("SoftMySQL", Context.MODE_PRIVATE);

        SQLEjecutar = prefs.getString("SQL", "");
        catalogoMySQL = prefs.getString("Catálogo", "");
        ipServidorMySQL = prefs.getString("Conexión", "192.168.1.100");
        contrasenaMySQL = prefs.getString("Contraseña", "");
        puertoMySQL = Integer.toString(prefs.getInt("Puerto", 3306));
        usuarioMySQL = prefs.getString("Usuario", "root");
    }

    //Obtener lista de catálogos de MySQL
    public void obtenerListaCatalogos() {
        try {
            cargarConfiguracion();
            conectarBDMySQL(usuarioMySQL, contrasenaMySQL, ipServidorMySQL, puertoMySQL, "");

            //ejecutamos consulta SQL
            Statement st = conexionMySQL.createStatement();
            ResultSet rs = st.executeQuery("show databases");
            rs.last();
            Integer numFilas = rs.getRow();

            listaCatalogos = new String[numFilas];
            Integer j = 0;
            //mostramos el resultado
            for (int i = 1; i <= numFilas; i++) {
                listaCatalogos[j] = rs.getObject(1).toString();
                j++;
                rs.previous();
            }
            rs.close();
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(),
                    "Error: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    //conectar al servidor de MySQL Server
    public void conectarBDMySQL(String usuario, String contrasena,
                                String ip, String puerto, String catalogo) {
        if (usuario.equals("") || puerto.equals("") || ip.equals("")) {
            AlertDialog.Builder alertDialog =
                    new AlertDialog.Builder(SoftMySQL.this);
            alertDialog.setMessage("Antes de establecer la conexión " +
                    "con el servidor " +
                    "MySQL debe indicar los datos de conexión " +
                    "(IP, puerto, usuario y contraseña).");
            alertDialog.setTitle("Datos conexión MySQL");
            alertDialog.setIcon(android.R.drawable.ic_dialog_alert);
            alertDialog.setCancelable(false);
            alertDialog.setPositiveButton("Aceptar",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            menuConfiguracion();
                        }
                    });
            alertDialog.show();
        } else {
            String urlConexionMySQL;
            if (!catalogo.equals(""))
                urlConexionMySQL = "jdbc:mysql://" + ip + ":" +
                        puerto + "/" + catalogo;
            else
                urlConexionMySQL = "jdbc:mysql://" + ip + ":" + puerto;
            if (!usuario.equals("") & !contrasena.equals("") & !ip.equals("") & !puerto.equals("")) {
                try {
                    Class.forName("com.mysql.jdbc.Driver");
                    conexionMySQL = DriverManager.getConnection(urlConexionMySQL,
                            usuario, contrasena);
                } catch (ClassNotFoundException e) {
                    Toast.makeText(getApplicationContext(),
                            "Error: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                } catch (SQLException e) {
                    Toast.makeText(getApplicationContext(),
                            "Error: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    //en el evento "Cerrar aplicación" guardar los datos en fichero xml
    @Override
    public void onDestroy() {
        super.onDestroy();
        guardarConfiguracion();
    }

    //en el evento "Abrir aplicación" leemos los datos de
    //configuración del fichero xml
    @Override
    protected void onStart() {
        super.onStart();
        cargarConfiguracion();
        try {
            textSQL.setText(SQLEjecutar);

            //seleccionamos en el Spinner (lista desplegable)
            //el último catálogo MySQL usado
            if (catalogoMySQL.equals("")) {
                listaCatalogos = new String[1];
                listaCatalogos[0] = catalogoMySQL;
                ArrayAdapter<String> adaptador =
                        new ArrayAdapter<String>(SoftMySQL.this,
                                android.R.layout.simple_list_item_1, listaCatalogos);
                adaptador.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spnCatalogos.setAdapter(adaptador);
            }
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(),
                    "Error: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }


}
