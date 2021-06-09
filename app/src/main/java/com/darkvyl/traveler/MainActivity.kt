package com.darkvyl.traveler

import android.Manifest.permission.*
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.MediaStore
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.os.HandlerCompat
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.darkvyl.traveler.adapters.ImageAdapter
import com.darkvyl.traveler.database.AppDatabase
import com.darkvyl.traveler.databinding.ActivityMainBinding
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.concurrent.thread

private const val TAKE_PHOTO = 1
private const val REQ_LOCATION_PERMISSION = 100
private const val REQ_CAMERA_PERMISSION = 101
private const val REQ_ALL_PERMISSION = 102

const val NOTIFICATION_CHANNEL_DEFAULT = "com.darkvyl.traveler.NotificationDefault"

class MainActivity : AppCompatActivity() {
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val handler = HandlerCompat.createAsync(Looper.getMainLooper())

    private val locationClient by lazy { LocationServices.getFusedLocationProviderClient(this) }
    private val geofencingClient by lazy { LocationServices.getGeofencingClient(this) }

    private val sharedPreferences by lazy { PreferenceManager.getDefaultSharedPreferences(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Shared.db = AppDatabase.open(applicationContext)
        setContentView(binding.root)
        binding.toCamera.isEnabled = false
        checkPermissions()
        createNotificationChannel()
        binding.toCamera.setOnClickListener {
            val uri = createUri()
            val intent =
                    Intent(MediaStore.ACTION_IMAGE_CAPTURE).putExtra(MediaStore.EXTRA_OUTPUT, uri)
            startActivityForResult(intent, TAKE_PHOTO)
        }

        thread {
            Shared.db?.place?.getAll()?.let { places -> Shared.imageList.addAll(places.map { BitmapFactory.decodeFile(it.path) }) }
            handler.post {
                setupRecycler()
            }
        }

    }

    @SuppressLint("MissingPermission")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == TAKE_PHOTO) {
            val tmpFile = File(filesDir, "tmp.jpg")
            if (resultCode == RESULT_OK) {
                val place = PlaceDto(path = "${filesDir}/image_${DateTimeFormatter.ISO_INSTANT.format(Instant.now())}.jpg", note = "")
                File(place.path).also { file ->
                    val currentTime = DateTimeFormatter.ofPattern("d MMM yyyy HH:mm").format(LocalDateTime.now())
                    var image = BitmapFactory.decodeFile(tmpFile.absolutePath)
                    locationClient.lastLocation.addOnSuccessListener { location: Location? ->
                        location?.let {
                            image = addTextToBitmap(
                                    image,
                                    "${this.getLocation(location)}, $currentTime"
                            )

                            image.compress(Bitmap.CompressFormat.JPEG, 100, file.outputStream())
                            thread {
                                Shared.db?.place?.insert(place)
                                Shared.imageList.add(image)
                                handler.post {
                                    binding.imageList.adapter?.notifyItemInserted(Shared.imageList.size - 1)
                                    val id = Shared.imageList.size.toLong()
                                    val intent = Intent(this, PlaceDetailsActivity::class.java).also {
                                        it.putExtra("placeId", id)
                                    }
                                    startActivity(intent)
                                    registerGeofence(id, location)
                                }
                            }
                        }
                    }
                }
            }
            tmpFile.delete()
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            binding.toCamera.isEnabled = true
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun addTextToBitmap(src: Bitmap, text: String): Bitmap {
        val result = Bitmap.createBitmap(src.width, src.height, src.config)//.rotate(90f)
        val canvas = Canvas(result)
        canvas.drawBitmap(src/*.rotate(90f)*/, 0f, 0f, null)
        val textSize = sharedPreferences.getString("text_size", "120")!!.toFloat()
        val red = sharedPreferences.getString("red", "255")!!.toFloat()
        val green = sharedPreferences.getString("green", "255")!!.toFloat()
        val blue = sharedPreferences.getString("blue", "255")!!.toFloat()
        val paint = Paint().also {
            it.color = Color.rgb(red / 255f, green / 255f, blue / 255f)
            it.textSize = textSize
        }
        canvas.drawText(text, 50f, src.width.toFloat() - 150f, paint)
        return result
    }

    private fun setupRecycler() {
        binding.imageList.layoutManager = GridLayoutManager(
            applicationContext, 2,
            LinearLayoutManager.VERTICAL, false
        )
        binding.imageList.adapter = ImageAdapter()
    }

    private fun createUri(): Uri {
        val file = filesDir.resolve("tmp.jpg").also {
            it.writeText("")
        }
        return FileProvider.getUriForFile(this, "com.darkvyl.traveler.FileProvider", file)
    }

    private fun checkPermissions() {
        val gpsPermission = checkSelfPermission(ACCESS_FINE_LOCATION)
        val backGroundPermission = checkSelfPermission(ACCESS_BACKGROUND_LOCATION)
        val cameraPermission = checkSelfPermission(CAMERA)
        if (gpsPermission != PackageManager.PERMISSION_GRANTED && cameraPermission != PackageManager.PERMISSION_GRANTED && backGroundPermission != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(ACCESS_FINE_LOCATION, ACCESS_BACKGROUND_LOCATION, CAMERA), REQ_ALL_PERMISSION)
            return
        }
        if (gpsPermission != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(ACCESS_FINE_LOCATION), REQ_LOCATION_PERMISSION)
            return
        }
        if (backGroundPermission != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(ACCESS_BACKGROUND_LOCATION), REQ_LOCATION_PERMISSION)
            return
        }
        if (cameraPermission != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(CAMERA), REQ_CAMERA_PERMISSION)
            return
        }
        binding.toCamera.isEnabled = true
    }

    @SuppressLint("MissingPermission")
    private fun registerGeofence(id: Long, location: Location) {
        val radius = sharedPreferences.getString("radius", "1000")!!.toFloat()
        val geofence = Geofence.Builder()
                .setRequestId(id.toString())
                .setCircularRegion(location.latitude, location.longitude, radius)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build()

        val request = GeofencingRequest.Builder().apply {
            addGeofence(geofence)
        }.build()

        val intent = PendingIntent.getBroadcast(
                applicationContext,
                1,
                Intent(this, GeofencingReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT)

        geofencingClient.addGeofences(request, intent)
    }

    fun toSettings(view: View) {
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    private fun getLocation(location: Location): String {
        if (Geocoder.isPresent()) {
            return Geocoder(this).getFromLocation(location.latitude, location.longitude, 1)[0].locality
        }
        return "Not for Huawei"
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_DEFAULT, "Travel", importance).apply {
                description = "Notification for travelers"
            }
            val notificationManager: NotificationManager =
                    getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun Bitmap.rotate(degrees: Float): Bitmap =
            Bitmap.createBitmap(this, 0, 0, width, height, Matrix().apply { postRotate(degrees) }, true)
}