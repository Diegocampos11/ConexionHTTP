package com.example.conexionhttp

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.*
import android.widget.Toast
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL


class NuevoSorteo : Service() {

    private var mServiceLooper: Looper? = null
    private var mServiceHandler: ServiceHandler? = null
    private var countIdNotification = 0

    // Handler that receives messages from the thread
    private inner class ServiceHandler : Handler {
        private var param = ""

        constructor(looper: Looper, param: String) : super(looper) {
            this.param = param
        }

        override fun handleMessage( msg: Message ) {
            // Normally we would do some work here, like download a file.
            // For our sample, we just sleep for 5 seconds.

            val sharedPref = PreferenceManager.getDefaultSharedPreferences( getBaseContext() )
            //obtengo el ultimo
            var ultimo = sharedPref.getString( "ultimo", "" )
            var ultimoCalculadoServer = Integer.parseInt( obtenerUltimo() )
            if ( ultimoCalculadoServer > Integer.parseInt( if ( ultimo === "" ) "0" else ultimo ) ){
                ultimo = ultimoCalculadoServer.toString()
                actualizarUltimo( ultimo )
                showNotification( Intent(), "Nuevo sorteo encontrado!! $ultimo", countIdNotification++ )
            }
            else showNotification( Intent(), "El utlimo sorteo registrado es!! $ultimo", countIdNotification++ )
            //stopSelf( msg.arg1 )
        }
    }

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    private fun actualizarUltimo( ultimo: String ){
        //actualizo
        val sharedPref = PreferenceManager.getDefaultSharedPreferences( getBaseContext() )
        val editor = sharedPref.edit()
        editor.putString("ultimo", ultimo )
        editor.commit()
    }

    private fun obtenerUltimo() : String{
        var ultimo = ""
        var ultimoInt = 0
        //http://experimentaciones.000webhostapp.com/subir_resultados.php?num_sorteo=1&premiados=1,2,3,4,5,6,7
        do{
            //consultamos sorteo :D
            ultimo = descargaUrl( "http://experimentaciones.000webhostapp.com/bajar_resultados.php?num_sorteo=" + ++ultimoInt )
            //showNotification( Intent(), "while RES: " + ultimo, countIdNotification++ )
            //Log.d( "ERROR", ultimoInt.toString() + " RES-SERVER: " + ultimo )
        }
        while ( ultimo != ("") )
        return (ultimoInt - 1).toString()
    }

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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        showNotification( Intent(), intent!!.getExtras()!!.getString("param"), countIdNotification++)

        //handler
        val thread = HandlerThread( "ServiceStartArguments", Process.THREAD_PRIORITY_BACKGROUND )
        thread.start()
        // Get the HandlerThread's Looper and use it for our Handler
        mServiceLooper = thread.looper
        //mServiceHandler = new ServiceHandler( mServiceLooper, PreferenceManager.getDefaultSharedPreferences(this).getString(Settings.ruta, "/sdcard"), intent.getExtras().getString("url"), intent.getExtras().getString("title"), intent.getExtras().getInt("notId") );
        mServiceHandler = ServiceHandler(mServiceLooper!!, intent!!.getExtras()!!.getString("param") )
        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job
        val msg = mServiceHandler!!.obtainMessage()
        msg.arg1 = startId
        mServiceHandler!!.sendMessage(msg)
        Log.d("XXXXXXXXXXX", "OK")
        return super.onStartCommand(intent, flags, startId)////Service.START_REDELIVER_INTENT
    }

    override fun onDestroy() {
        showNotification( Intent(), "Destruido", countIdNotification++)
        super.onDestroy()
    }

    fun showNotification(i: Intent, mensaje: String, notId: Int ) {
        val mBuilder: Notification.Builder
        val mNotifyMgr = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val icono = R.drawable.notification_icon_background
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val pendingIntent = PendingIntent.getActivity(applicationContext, 0, i, 0)
        //reproduce automatimcamente la cancion despues de descargarla :o
        //getApplicationContext().startActivity(i);
        mBuilder = Notification.Builder(applicationContext)
            .setContentIntent(pendingIntent)
            .setLargeIcon(BitmapFactory.decodeResource(resources, icono))
            .setSmallIcon(icono)
            .setContentTitle( "titulo" )
            .setContentText( mensaje )
            .setVibrate( longArrayOf(100, 250, 100, 500) )
            .setAutoCancel( true )
        mNotifyMgr.notify(notId, mBuilder.build())
    }
}
