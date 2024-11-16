package es.ua.eps.informacion_red_wifi

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Bundle
import android.provider.Settings
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.net.InetAddress
import java.net.NetworkInterface

class MainActivity : AppCompatActivity() {
    private lateinit var wifiManager: WifiManager
    private val LOCATION_PERMISSION_REQUEST_CODE = 1

    // Declara el launcher para manejar la actividad de la configuración de ubicación
    private lateinit var gpsSettingsLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializa el launcher para manejar el retorno de la configuración del GPS
        gpsSettingsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            // Al regresar de la pantalla de configuración de GPS, verifica si el GPS está habilitado
            checkAndEnableGPS()
            displayWifiInfo()
        }

        // Verificar y solicitar permisos de ubicación
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        } else {
            checkAndEnableGPS()
            displayWifiInfo()
        }
    }

    private fun checkAndEnableGPS() {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

        if (!isGpsEnabled) {
            // Solicitar al usuario que active el GPS
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            gpsSettingsLauncher.launch(intent) // Usa el launcher para abrir la configuración del GPS
        }
    }

    private fun displayWifiInfo() {
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
// Obtiene el servicio WiFi del sistema usando el contexto de la aplicación, y lo convierte a un objeto WifiManager
        val connectionInfo = wifiManager.connectionInfo
// Obtiene la información actual de la conexión WiFi (SSID, BSSID, velocidad de enlace)
        val dhcpInfo = wifiManager.dhcpInfo
// Obtiene la información DHCP (dirección IP, puerta de enlace, máscara de subred, servidores DNS)

        val ssid = if (connectionInfo.ssid == "<unknown ssid>") "Desconocido" else connectionInfo.ssid.replace("\"", "")
// Obtiene el SSID de la red WiFi a la que está conectado el dispositivo. Si el SSID es "<unknown ssid>", lo reemplaza con "Desconocido".
// También elimina las comillas alrededor del SSID si las hay.
        val bssid = connectionInfo.bssid
// Obtiene el BSSID de la red WiFi, que es la dirección MAC del punto de acceso al que está conectado el dispositivo.
        val linkSpeed = connectionInfo.linkSpeed
// Obtiene la velocidad de enlace de la red WiFi en Mbps (megabits por segundo).
        val frequency = connectionInfo.frequency / 1000
// Obtiene la frecuencia de la red WiFi en MHz y la convierte a GHz para una mayor comprensión (dividiendo entre 1000).
        val rssi = connectionInfo.rssi
// Obtiene el RSSI (nivel de señal) en dBm, que indica la intensidad de la señal de la red WiFi.
        val networkId = connectionInfo.networkId
// Obtiene el ID de la red WiFi a la que está conectado el dispositivo. Si es -1, significa que no está conectado a una red conocida.
        val channel = getChannelFromFrequency(connectionInfo.frequency)
// Calcula el canal de la red WiFi utilizando la frecuencia de la red

        val scanResults = wifiManager.scanResults
// Obtiene una lista de todos los resultados de escaneo de redes WiFi cercanas.
        val currentScanResult: ScanResult? = scanResults.find { it.BSSID == bssid }
// Busca en los resultados del escaneo la red cuyo BSSID coincida con el BSSID de la red a la que está conectado el dispositivo.
        val isHidden = currentScanResult?.SSID.isNullOrEmpty()
// Verifica si la red WiFi es oculta. Si el SSID está vacío o es nulo, la red está oculta.

        val ipAddressStr = intToIp(dhcpInfo.ipAddress)
// Convierte la dirección IP interna del dispositivo (obtenida desde DHCP) de formato entero a una cadena con formato 'x.x.x.x'.
        val gatewayStr = intToIp(dhcpInfo.gateway)
// Convierte la dirección de la puerta de enlace (gateway) obtenida desde DHCP a formato legible 'x.x.x.x'.
        val netmaskStr = intToIp(dhcpInfo.netmask)
// Convierte la máscara de subred obtenida desde DHCP a formato legible 'x.x.x.x'.
        val dns1Str = intToIp(dhcpInfo.dns1)
// Convierte la dirección IP del primer servidor DNS a formato legible 'x.x.x.x'.
        val dns2Str = intToIp(dhcpInfo.dns2)
// Convierte la dirección IP del segundo servidor DNS a formato legible 'x.x.x.x'.
        val externalIp = getExternalIp()
// Llama a la función 'getExternalIp' para obtener la dirección IP externa del dispositivo (la IP pública asignada por el proveedor de Internet).
        val encryption = if (connectionInfo.networkId != -1) "WPA/WPA2" else "Abierta"
// Si el 'networkId' de la conexión WiFi no es -1 (indica que está conectado a una red conocida), se asume que la red está protegida por WPA/WPA2.
// Si no está conectada a una red conocida (networkId == -1), se asume que la red es abierta (sin cifrado).
        val dhcpLease = dhcpInfo.leaseDuration
// Obtiene la duración del arrendamiento DHCP, que indica el tiempo en segundos por el cual la dirección IP asignada es válida.

        val textView = findViewById<TextView>(R.id.textView)

        val wifiInfo = """
            SSID: $ssid
            BSSID: $bssid
            Velocidad de enlace: $linkSpeed Mbps
            Frecuencia: ${frequency}GHz
            Canal: $channel
            RSSI: $rssi dBm
            Encriptación: $encryption
            Dirección IP interna: $ipAddressStr
            Puerta de enlace: $gatewayStr
            Máscara de subred: $netmaskStr
            DNS1: $dns1Str
            DNS2: $dns2Str
            Duración del DHCP lease: $dhcpLease s
            IP externa: $externalIp
            Red oculta: ${if (isHidden) "Sí" else "No"}
        """.trimIndent()

        textView.text = wifiInfo
    }

    private fun intToIp(ip: Int): String {
        return String.format(
            "%d.%d.%d.%d",
            (ip and 0xFF),
            (ip shr 8 and 0xFF),
            (ip shr 16 and 0xFF),
            (ip shr 24 and 0xFF)
        )
    }

    private fun getChannelFromFrequency(frequency: Int): Int {
        return when (frequency) {
            in 2412..2472 -> (frequency - 2407) / 5
            in 5180..5825 -> (frequency - 5000) / 5
            else -> -1 // Desconocido
        }
    }

    private fun getExternalIp(): String {
        return try {
            val ip = NetworkInterface.getNetworkInterfaces().toList()
                .flatMap { it.inetAddresses.toList() }
                .find { !it.isLoopbackAddress && it is InetAddress }
                ?.hostAddress ?: "Desconocida"
            ip
        } catch (e: Exception) {
            "Desconocida"
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkAndEnableGPS()
                displayWifiInfo()
            } else {
                // Manejar caso donde el permiso fue denegado
                findViewById<TextView>(R.id.textView).text = "Permiso de ubicación denegado. No se puede mostrar el SSID."
            }
        }
    }
}