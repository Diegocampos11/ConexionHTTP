

package com.example.conexionhttp

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.os.AsyncTask
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.ArrayList

class MainActivity : AppCompatActivity() {

    var listView : ListView? = null
    private lateinit var db: SQLiteDatabase
    private lateinit var btnIngresar : Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //btnIngresar = findViewById( R.id.btnIngresar )
        findViewById<Button>( R.id.btnIngresar ).setOnClickListener{ btnIngresar() }
        //createDatabase()
    }

    fun btnIngresar(){
        val retrieveTastk = RetrieveTask( this )
        retrieveTastk.execute( "http://experimentaciones.000webhostapp.com/premiados.txt" )
    }

    fun createDatabase(){
        db = openOrCreateDatabase("Loteria", Context.MODE_PRIVATE, null)
        db.execSQL(
            "create table sorteo(\n" +
                    "\tid int primary key,\n" +
                    "\tnumero int not null\n" +
                ");\n" +
                "\n" +
                "create table numeros(\n" +
                    "\tid int primary key,\n" +
                    "\tid_sorteo int not null, \n" +
                    "\tnumero int not null,\n" +
                    "\tconstraint FK_ID_SORTEO_NUMEROS foreign key (id_sorteo) references sorteo(id)\n" +
                ");")
    }

    fun Listar() {
        val adaptador: ArrayAdapter<*>
        val lista : ArrayList<Any>? = null
        val c = db.rawQuery("select id_sorteo, numero from numeros;", null)
        if ( c.getCount() == 0 )
            lista!!.add("No hay registros en la BD")
        else {
            while (c.moveToNext())
                lista!!.add(c.getString(0) + " - (" + c.getString(1) + ")")
        }
        adaptador = ArrayAdapter(applicationContext, android.R.layout.simple_list_item_1, lista)
        listView!!.adapter = (adaptador)
        c.close()
    }

    class RetrieveTask( contextxx: Context ) : AsyncTask<String, String, String>(){
        private val contextx = contextxx
        override fun onPreExecute() {
            super.onPreExecute()
        }

        override fun doInBackground(vararg params: String): String? {
            return descargaUrl(params[0])
        }

        override fun onProgressUpdate(vararg params: String) {
            super.onProgressUpdate(*params)
        }

        override fun onPostExecute(result: String?) {
            Toast.makeText( contextx, "Con" + result, Toast.LENGTH_LONG )
        }

        @Throws(IOException::class)
        private fun descargaUrl(miUrl: String): String {
            var `is`: InputStream? = null
            try {

                val url =
                    URL(miUrl) //crea un objeto URL que tiene como argumento el string que ha introducido el usuario
                val conn =
                    url.openConnection() as HttpURLConnection //creamos un objeto para abrir la conexión con la url indicada
                conn.readTimeout = (10000)
                conn.connectTimeout = (15000)
                conn.requestMethod = ("GET") //indicamos que vamos a traer elementos
                conn.doInput = (true)

                conn.connect() //conectamos con la url
                `is` = conn.inputStream //obtenemos un flujo de datos de entrada desde la conexión establecida

                /*llamamos a este método que se encargará de tomar el flujo de entrada y convertirlo en un string,
                que devolveremos a doInBackground
                */
                return leer(`is`!!)
            } finally {
                if (`is` != null) {
                    `is`!!.close()
                }
            }

        }

        private fun leer(`is`: InputStream): String {
            val bo = ByteArrayOutputStream() //creamos un array de bytes en el que escribir
            try {
                var i = `is`.read() //leemos un byte del flujo de entrada
                while (i != -1) {    //mientras haya bytes los escribimos en el array de bytes. Cuando se acabe, la lectura nos devuelve -1 para indicar que no hay más
                    bo.write(i)
                    i = `is`.read()
                }
                /*devolvemos un string a descargaUrl que a su vez lo devolverá a doInBackground
                que se lo dará a onPostExecute que lo escribirá en el textView
                 */
            }catch (e: IOException) {
                Log.d( "ERROR", "Error en al intentar leer el archivo" )
                e.printStackTrace()
                return ""
            }
            return bo.toString()
        }
    }
}
