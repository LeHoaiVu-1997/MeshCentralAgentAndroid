package com.meshcentral.agent.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
import android.os.Build
import android.os.IBinder
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.meshcentral.agent.MainActivity
import com.meshcentral.agent.MeshAgent
import com.meshcentral.agent.R
import com.meshcentral.agent.agentCertificate
import com.meshcentral.agent.agentCertificateKey
import com.meshcentral.agent.g_ScreenCaptureService
import com.meshcentral.agent.g_autoConnect
import com.meshcentral.agent.g_userDisconnect
import com.meshcentral.agent.hardCodedServerLink
import com.meshcentral.agent.mainFragment
import com.meshcentral.agent.meshAgent
import com.meshcentral.agent.serverLink
import org.spongycastle.asn1.x500.X500Name
import org.spongycastle.cert.X509v3CertificateBuilder
import org.spongycastle.cert.jcajce.JcaX509CertificateConverter
import org.spongycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.spongycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.ByteArrayInputStream
import java.math.BigInteger
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Date
import java.util.Random
import kotlin.math.absoluteValue

class ConnectionService : Service() {
    private val TAG = "ConnectionService"
    private val SERVICE = "com.meshcentral.agent.service.ConnectionService"
    private val NOTIFICATION_ID = 9999
    private var broadcastReceiver: BroadcastReceiver? = null;
    private val NOTIFICATION_CHANNEL_ID = "com.meshcentral.agent.service.ConnectionService";
    private val NOTIFICATION_CHANNEL_NAME = "com.meshcentral.agent.service.ConnectionService";
    override fun onBind(intent: Intent?): IBinder? {
        return null;
    }

    private fun createNotification(): Notification {
        val builder = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
        builder.setSmallIcon(R.mipmap.ic_launcher_foreground)
        builder.setContentTitle("Mesh Central Agent Android Connetion Service")
        builder.setContentText("This service provides third party apps solution to connect Mesh Central host without opening Mesh Central Agent Android")
        builder.setOngoing(true)
        builder.setCategory(Notification.CATEGORY_SERVICE)
        builder.priority = Notification.PRIORITY_LOW
        builder.setShowWhen(true)
        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            );
            channel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun getNotification() : Pair<Int, Notification> {
        createNotificationChannel();
        val notification = createNotification()
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
        return Pair(NOTIFICATION_ID, notification)
    }

    fun startApp() {
        val startActivity: Intent? = packageManager.getLaunchIntentForPackage(applicationContext.packageName)
        if (startActivity != null) {
            startActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity.putExtra("startService", MainActivity.Companion.SCREEN_CAPTURE_SERVICE)
            applicationContext.startActivity(startActivity)
        };
    }

    fun toggleAgentConnection(userInitiated : Boolean) {
        if ((meshAgent == null) && (serverLink != null)) {
            // Create and connect the agent
            if (agentCertificate == null) {
                val sharedPreferences = getSharedPreferences("meshagent", Context.MODE_PRIVATE)
                var certb64 : String? = sharedPreferences?.getString("agentCert", null)
                var keyb64 : String? = sharedPreferences?.getString("agentKey", null)
                if ((certb64 == null) || (keyb64 == null)) {
                    //println("Generating new certificates...")

                    // Generate an RSA key pair
                    val keyGen = KeyPairGenerator.getInstance("RSA")
                    keyGen.initialize(2048, SecureRandom())
                    val keypair = keyGen.generateKeyPair()

                    // Generate Serial Number
                    var serial : BigInteger = BigInteger("12345678");
                    try { serial = BigInteger.valueOf(Random().nextInt().toLong().absoluteValue) } catch (ex: Exception) {}

                    // Create self signed certificate
                    val builder: X509v3CertificateBuilder = JcaX509v3CertificateBuilder(
                        X500Name("CN=android.agent.meshcentral.com"), // issuer authority
                        serial, // serial number of certificate
                        Date(System.currentTimeMillis() - 86400000L * 365), // start of validity
                        Date(253402300799000L), // end of certificate validity
                        X500Name("CN=android.agent.meshcentral.com"), // subject name of certificate
                        keypair.public) // public key of certificate
                    agentCertificate = JcaX509CertificateConverter().setProvider("SC").getCertificate(builder
                        .build(JcaContentSignerBuilder("SHA256withRSA").build(keypair.private))) // Private key of signing authority , here it is self signed
                    agentCertificateKey = keypair.private

                    // Save the certificate and key
                    sharedPreferences?.edit()?.putString("agentCert", Base64.encodeToString(
                        agentCertificate?.encoded, Base64.DEFAULT))?.apply()
                    sharedPreferences?.edit()?.putString("agentKey", Base64.encodeToString(
                        agentCertificateKey?.encoded, Base64.DEFAULT))?.apply()
                } else {
                    //println("Loading certificates...")
                    agentCertificate = CertificateFactory.getInstance("X509").generateCertificate(
                        ByteArrayInputStream(Base64.decode(certb64, Base64.DEFAULT))
                    ) as X509Certificate
                    val keySpec = PKCS8EncodedKeySpec(Base64.decode(keyb64, Base64.DEFAULT))
                    agentCertificateKey = KeyFactory.getInstance("RSA").generatePrivate(keySpec)
                }
                //println("Cert: ${agentCertificate.toString()}")
                //println("XKey: ${agentCertificateKey.toString()}")
            }

            if (!userInitiated) {
                meshAgent = MeshAgent(mainActivity, getServerHost()!!, getServerHash()!!, getDevGroup()!!)
                meshAgent?.Start()
            } else {
                if (g_autoConnect) {
                    if (g_userDisconnect) {
                        // We are not trying to connect, switch to connecting
                        g_userDisconnect = false
                        meshAgent =
                            MeshAgent(mainActivity, getServerHost()!!, getServerHash()!!, getDevGroup()!!)
                        meshAgent?.Start()
                    } else {
                        // We are trying to connect, switch to not trying
                        g_userDisconnect = true
                    }
                } else {
                    // We are not in auto connect mode, try to connect
                    g_userDisconnect = true
                    meshAgent =
                        MeshAgent(mainActivity, getServerHost()!!, getServerHash()!!, getDevGroup()!!)
                    meshAgent?.Start()
                }
            }
        } else if (meshAgent != null) {
            // Stop the agent
            if (userInitiated) { g_userDisconnect = true }
            stopProjection()
            meshAgent?.Stop()
            meshAgent = null
        }
        mainFragment?.refreshInfo()
    }

    fun disconnectMesh(userInitiated : Boolean) {
        if (meshAgent != null) {
            // Stop the agent
            if (userInitiated) { g_userDisconnect = true }
            stopProjection()
            meshAgent?.Stop()
            meshAgent = null
        }
    }

    private fun getServerHost() : String? {
        if (serverLink == null) return null
        var x : List<String> = serverLink!!.split(',')
        var serverHost = x[0]
        return serverHost.substring(5)
    }

    private fun getServerHash() : String? {
        if (serverLink == null) return null
        var x : List<String> = serverLink!!.split(',')
        return x[1]
    }

    private fun getDevGroup() : String? {
        if (serverLink == null) return null
        var x : List<String> = serverLink!!.split(',')
        return x[2]
    }


    fun isMshStringValid(x: String):Boolean {
        if (!x.startsWith("mc://"))  return false
        val xs = x.split(',')
        if (xs.count() < 3) return false
        if (xs[0].length < 8) return false
        if (xs[1].length < 3) return false
        if (xs[2].length < 3) return false
        return xs[0].indexOf('.') != -1
    }

    fun setMeshServerLink(x: String?) {
        if ((serverLink == x) || (hardCodedServerLink != null)) return
        if (meshAgent != null) { // Stop the agent
            meshAgent?.Stop()
            meshAgent = null
        }
        serverLink = x
        val sharedPreferences = getSharedPreferences("meshagent", Context.MODE_PRIVATE)
        sharedPreferences.edit().putString("qrmsh", x).apply()
        mainFragment?.refreshInfo()
        g_userDisconnect = false
        if (g_autoConnect) { toggleAgentConnection(false) }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = getNotification()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            startForeground(notification.first, notification.second)
        } else {
            startForeground(notification.first, notification.second, FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        }

        service = this

        val filter = IntentFilter(SERVICE)
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val command = intent?.getIntExtra("command", 0);
                if (command == IntentCommands.CONNECT) {
                    val connectionString = intent.getStringExtra("connection")
                    if (connectionString != null && isMshStringValid(connectionString)) {
                        Log.d(TAG, "onReceive, go here")
                        setMeshServerLink(connectionString)
                        toggleAgentConnection(false)
                    } else {
                        Toast.makeText(applicationContext, "Connection string invalid", Toast.LENGTH_LONG).show()
                    }
                } else if (command == IntentCommands.DISCONNECT) {
                    disconnectMesh(false)
                }
            }

        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            registerReceiver(broadcastReceiver, filter)
        } else {
            registerReceiver(broadcastReceiver, filter, RECEIVER_EXPORTED);
        }

        return START_STICKY
    }

    fun stopProjection() {
        if (g_ScreenCaptureService == null) return
        startService(com.meshcentral.agent.ScreenCaptureService.getStopIntent(this))
    }

    override fun onDestroy() {
        if (broadcastReceiver != null) {
            unregisterReceiver(broadcastReceiver);
            broadcastReceiver = null;
        }
        super.onDestroy()
    }

    companion object {
        lateinit var service: ConnectionService
        lateinit var mainActivity: MainActivity;
    }
}