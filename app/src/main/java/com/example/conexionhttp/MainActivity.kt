

package com.example.conexionhttp

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.AsyncTask
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.util.Log
import android.view.View
import android.widget.*
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.ArrayList
import android.R.attr.action
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build





class MainActivity : AppCompatActivity() {

    private lateinit var db: SQLiteDatabase
    private lateinit var btnIngresar : Button
    private lateinit var txtSorteo : EditText
    private lateinit var txtNumero : EditText
    private lateinit var txtSorteoCon : EditText
    private lateinit var txtPremiados : TextView
    private lateinit var listView : ListView
    private var vibracion = longArrayOf(
        0,
        600,
        100,
        600,
        100,
        600,
        100,
        200,
        100,
        200,
        100,
        200,
        100,
        600,
        100,
        600,
        100,
        600
    )
    //private lateinit var responseServer : String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView( R.layout.activity_main )
        btnIngresar = findViewById( R.id.btnIngresar )
        btnIngresar.setOnClickListener{ btnIngresar() }
        findViewById<Button>( R.id.btnSorteo ).setOnClickListener{ btnSorteo() }
        findViewById<Button>( R.id.btnSorteoCon ).setOnClickListener{ btnSorteoCon() }
        txtSorteo = findViewById( R.id.txtSorteo )
        txtNumero = findViewById( R.id.txtNumero )
        txtSorteoCon = findViewById( R.id.txtSorteoCon )
        txtPremiados = findViewById( R.id.txtPremiados )
        listView = findViewById( R.id.listNumeros )
        openCreateDatabase()
        Listar()
        startService( Intent( this, NuevoSorteo::class.java ).putExtra("param", "NADA") )
    }

    override fun onSupportNavigateUp(): Boolean {
        val tab1 = findViewById<View>(R.id.tab1)
        val tab2 = findViewById<View>(R.id.tab2)
        tab1.visibility = ( View.VISIBLE )
        tab2.visibility = ( View.GONE  )
        supportActionBar!!.setDisplayHomeAsUpEnabled(false)
        return false
    }

    fun btnSorteoCon(){
        //consultamos sorteo :D
        RetrieveTask( baseContext, this ).execute( "http://experimentaciones.000webhostapp.com/bajar_resultados.php?num_sorteo=" + txtSorteoCon.text )
    }

    fun btnSorteo(){
        val tab1 = findViewById<View>(R.id.tab1)
        val tab2 = findViewById<View>(R.id.tab2)
        tab1.visibility = ( View.GONE )
        tab2.visibility = ( View.VISIBLE )
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
    }

    fun btnIngresar(){
        db!!.execSQL( "insert into sorteos values ( ? )", arrayOf( txtSorteo.text ) )
        val split = explode( txtNumero.text.toString() )
        for (i in split.indices) {
            db!!.execSQL( "insert into numeros values ( ?, ? )", arrayOf( txtSorteo.text, split[i] ) )
        }
        Toast.makeText( this, "Inserción realizada con éxito", Toast.LENGTH_LONG ).show()
        Listar()
    }

    fun explode( text : String ) : Array<String> {
        val split = text.split( ",".toRegex() ).dropLastWhile { it.isEmpty() }.toTypedArray()
        return split
    }

    fun openCreateDatabase(){
        db = openOrCreateDatabase("Loteria", Context.MODE_PRIVATE, null)
        db.execSQL(
            "create table if not exists sorteos(\n" +
                    "\tsorteo int primary key\n" +
                ")")
        db.execSQL(
            "create table if not exists numeros(\n" +
                    "\tsorteo int not null, \n" +
                    "\tnumero int not null,\n" +
                    "\tprimary key( sorteo, numero )"+
                    "\tconstraint FK_SORTEO_NUMEROS foreign key (sorteo) references sorteos(sorteo)\n" +
                ");")
    }

    fun Listar() {
        val adaptador: ArrayAdapter<*>
        val lista = ArrayList<Any>()
        val c = db.rawQuery("select sorteo, numero from numeros;", null)
        if ( c.getCount() == 0 )
            lista.add("No hay registros en la BD")
        else {
            while ( c.moveToNext() )
            //Log.d("ERROR", "" + c.getInt(0) + " - (" + c.getInt(1) + ")")
            lista.add("Sorteo: " + c.getInt(0) + " - Numero: " + c.getInt(1) + "")
        }
        adaptador = ArrayAdapter( this@MainActivity , android.R.layout.simple_list_item_1, lista)
        listView!!.adapter = (adaptador)
        c.close()
    }

    class RetrieveTask( contextxx: Context, activity: MainActivity ) : AsyncTask<String, String, String>(){
        //private val contextx = contextxx
        private val activity : MainActivity = activity

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
            val numerosServer = activity.explode( result!! )
            //
            val misNumeros = ArrayList<Any>()
            val c = activity.db.rawQuery("select numero from numeros where sorteo = ?;", arrayOf( activity.txtSorteoCon.text.toString() ) )
            if ( c.count == 0 )
                activity.txtPremiados.text = "No hay registros en la BD"
            else {
                while ( c.moveToNext() ) {
                    activity.txtPremiados.text = "No hay registros en la BD"
                }
            }
            for ( i in misNumeros.toArray().indices ) {
                Log.d( "INFO", i.toString() )
            }
            c.close()
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
