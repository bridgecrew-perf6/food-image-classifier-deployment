package com.example.food_image_recognization

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit


//@Throws(IOException::class)
//fun getBytes(inputStream: InputStream): ByteArray {
//    val byteBuffer = ByteArrayOutputStream()
//    val bufferSize = 1024
//    val buffer = ByteArray(bufferSize)
//    var len = 0
//    while (inputStream.read(buffer).also { len = it } != -1) {
//        byteBuffer.write(buffer, 0, len)
//    }
//    return byteBuffer.toByteArray()
//}

class MainActivity : AppCompatActivity() {

    lateinit var inputData : ByteArray
    lateinit var fileuri : Uri
    private val cameraRequest = 1888
    private val browseRequest = 2000
    var actual_size = 0

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == cameraRequest && resultCode == RESULT_OK && data!=null) {
            println("camera")
            val image_view : ImageView = findViewById(R.id.imageView)
            val cut_buttom : Button = findViewById(R.id.button4)
            val detect_button : Button = findViewById(R.id.button2)

//            val myInputStream: InputStream = ByteArrayInputStream(data.getByteArrayExtra(data.extras))

//            val imageUri: Uri? = data.data!!
//            val iStream = imageUri?.let { contentResolver.openInputStream(it) }
//            inputData = getBytes(iStream!!)


            val photo: Bitmap = data?.extras?.get("data") as Bitmap
            image_view.setImageBitmap(photo)

            val stream = ByteArrayOutputStream()
            val resized = Bitmap.createScaledBitmap(photo, 300, 300, true)
            resized.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            inputData = stream.toByteArray()


            cut_buttom.visibility = View.VISIBLE
            image_view.visibility = View.VISIBLE
            detect_button.visibility = View.VISIBLE
        }

        if(requestCode == browseRequest && resultCode == RESULT_OK && data!=null ){
            println("browse")
            val image_view : ImageView = findViewById(R.id.imageView)
            val image_status : TextView = findViewById(R.id.textView2)
            val cut_buttom : Button = findViewById(R.id.button4)
            val detect_button : Button = findViewById(R.id.button2)

            fileuri = data.data!!
//            val iStream = contentResolver.openInputStream(fileuri)
//            inputData = getBytes(iStream!!)

            val photo = MediaStore.Images.Media.getBitmap(this.contentResolver, fileuri)

            val stream = ByteArrayOutputStream()
            val resized = Bitmap.createScaledBitmap(photo, 300, 300, true)
            resized.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            inputData = stream.toByteArray()


//            val photo: Bitmap = data?.extras?.get("data") as Bitmap
//            val resized = Bitmap.createScaledBitmap(photo, 150, 150, true)

            val cursor = contentResolver.query(fileuri, null, null, null, null)
            cursor?.use {
                it.moveToFirst()
                actual_size = cursor.getInt(it.getColumnIndex(OpenableColumns.SIZE))
                println(actual_size)
            }

            if(actual_size <= 8000000){
                image_view.setImageBitmap(photo)

                cut_buttom.visibility = View.VISIBLE
                image_view.visibility = View.VISIBLE
                detect_button.visibility = View.VISIBLE
            }
            else{
                image_status.visibility = View.VISIBLE
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val take_photo_button : Button = findViewById(R.id.button)
        val ans : TextView = findViewById(R.id.textView)
        val detect_button : Button = findViewById(R.id.button2)
        val image_view : ImageView = findViewById(R.id.imageView)
        val browse_button : Button = findViewById(R.id.button3)
        val cut_button : Button = findViewById(R.id.button4)
        val image_status : TextView = findViewById(R.id.textView2)
        val list_of_categories : Spinner = findViewById(R.id.spinner)
        val available_categories : TextView = findViewById(R.id.textView3)

        val items = arrayOf("Donut", "French Fries", "Fried Rice","Chocolate Cake","Hotdog")
        val adapter: ArrayAdapter<Any?> = ArrayAdapter<Any?>(this, android.R.layout.simple_spinner_dropdown_item, items)

        list_of_categories.setAdapter(adapter);

        take_photo_button.setOnClickListener {
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(cameraIntent, cameraRequest)

            ans.visibility = View.INVISIBLE
            detect_button.visibility = View.INVISIBLE
            image_view.visibility = View.INVISIBLE
            image_status.visibility = View.INVISIBLE
            cut_button.visibility = View.INVISIBLE
        }

        browse_button.setOnClickListener {
            var i = Intent()
            i.setType("image/*")
            i.setAction(Intent.ACTION_GET_CONTENT)
            startActivityForResult(Intent.createChooser(i, "Choose Picture"), browseRequest)

            ans.visibility = View.INVISIBLE
            detect_button.visibility = View.INVISIBLE
            image_view.visibility = View.INVISIBLE
            image_status.visibility = View.INVISIBLE
            cut_button.visibility = View.INVISIBLE
        }

        cut_button.setOnClickListener {
            ans.visibility = View.INVISIBLE
            detect_button.visibility = View.INVISIBLE
            image_view.visibility = View.INVISIBLE
            image_status.visibility = View.INVISIBLE
            cut_button.visibility = View.INVISIBLE
        }

        detect_button.setOnClickListener {

            ans.visibility = View.VISIBLE
            detect_button.visibility = View.INVISIBLE
            image_view.visibility = View.INVISIBLE
            image_status.visibility = View.INVISIBLE
            cut_button.visibility = View.INVISIBLE
            take_photo_button.visibility = View.INVISIBLE
            browse_button.visibility = View.INVISIBLE
            available_categories.visibility = View.INVISIBLE
            list_of_categories.visibility = View.INVISIBLE
            ans.text = "LOADING..."

            //http request being made

            val url = "http://c860cd5162cf.ngrok.io/detect"
            val MEDIA_TYPE = "image/*".toMediaType()

            val formBody = inputData?.let { it1 -> RequestBody.create(MEDIA_TYPE, it1) }?.let { it2 ->
                MultipartBody.Builder().setType(MultipartBody.FORM)
                        .addFormDataPart("file", "file",it2)
                        .build()
            };

            val request = Request.Builder().method("POST", formBody).url(url).build()
            val client = OkHttpClient()

            val slowClient = client.newBuilder()
                    .readTimeout(25, TimeUnit.SECONDS)
                    .connectTimeout(25, TimeUnit.SECONDS)
                    .writeTimeout(25, TimeUnit.SECONDS)
                    .build()

            slowClient.newCall(request).enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {

                    val body = response?.body?.string()

                    try {
                        val json = JSONObject(body)
                        println(json.getString("result"))

                        runOnUiThread {
                            cut_button.visibility = View.VISIBLE
                            take_photo_button.visibility = View.VISIBLE
                            browse_button.visibility = View.VISIBLE
                            image_view.visibility = View.VISIBLE
                            detect_button.visibility = View.VISIBLE
                            available_categories.visibility = View.VISIBLE
                            list_of_categories.visibility = View.VISIBLE
                            ans.text = json.getString("result")
                            ans.visibility = View.VISIBLE
                        }

                    } catch (e: JSONException) {
                        println("Failed to execute request!")
                        println(e)
                        println("Server is down, Please try later!")
                        runOnUiThread {
                            image_status.text = "Server is down, Please try later!"
                            image_status.visibility = View.VISIBLE
                            take_photo_button.visibility = View.VISIBLE
                            browse_button.visibility = View.VISIBLE
                            ans.visibility = View.INVISIBLE
                            available_categories.visibility = View.VISIBLE
                            list_of_categories.visibility = View.VISIBLE
                        }
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    println("Failed to execute request!")
                    println(e)
                    println("Please check your Internet and try again!")
                    runOnUiThread {
                        image_status.text = "Please check your Internet and try again!"
                        image_status.visibility = View.VISIBLE
                        take_photo_button.visibility = View.VISIBLE
                        browse_button.visibility = View.VISIBLE
                        ans.visibility = View.INVISIBLE
                        available_categories.visibility = View.VISIBLE
                        list_of_categories.visibility = View.VISIBLE
                    }

                }
            })

        }


    }
}