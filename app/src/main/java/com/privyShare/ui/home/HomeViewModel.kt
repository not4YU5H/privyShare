package com.privyShare.ui.home

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.privyShare.data.AppPreference
import com.privyShare.data.FileEntity
import com.privyShare.data.UserRepository
import com.privyShare.util.EncryptionHelper
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream
import java.io.File


class HomeViewModel(application: Application) : AndroidViewModel(application) {
    val snackBarMsg: MutableLiveData<String> = MutableLiveData()
    private val context = application
    val fileListEntity: MutableLiveData<ArrayList<FileEntity>> = MutableLiveData()
    private val fileList = ArrayList<FileEntity>()
    val isLoading: MutableLiveData<Boolean> = MutableLiveData()
    val bmp: MutableLiveData<Bitmap> = MutableLiveData()
    val message: MutableLiveData<String> = MutableLiveData()
    val fileName: MutableLiveData<String> = MutableLiveData()
    private val dirImage = File(context.filesDir, "images")
    private val dirFile = File(context.filesDir, "documents")
    val masterToken: MutableLiveData<String> = MutableLiveData()
    val newMasterToken: MutableLiveData<String> = MutableLiveData()
    private var sharedPrefs = EncryptionHelper.getSharedPrefs(application)
    private var appPreference = AppPreference(sharedPrefs)
    private var userRepository = UserRepository(appPreference)




    fun getEncryptedBitmap() {
        viewModelScope.launch {
            val file = File(dirImage, fileName.value!!)
            val encryptedFile = EncryptionHelper.getEncryptedFile(file, context)
            launch(IO) {
                try {
                    encryptedFile.openFileInput().also {
                        val byteArrayInputStream = ByteArrayInputStream(it.readBytes())
                        bmp.postValue(BitmapFactory.decodeStream(byteArrayInputStream))
                    }

                    shareimgg()
                    snackBarMsg.postValue("Image decrypted successfully!")
                } catch (e: Exception) {
                    snackBarMsg.postValue(e.message)
                }
            }
        }
    }

//    fun shareImage() {
//        val file = File(dirImage, fileName.value!!)
//        val shareIntent: Intent = Intent().apply {
//            action = Intent.ACTION_SEND
//            putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file))
//            type = "image/jpg"
//        }
//        context.startActivity(Intent.createChooser(shareIntent, null))
//    }

//    fun shareText() {
//        val file = File(dirFile, fileName.value!!)
//        val uri = Uri.fromFile(file)
//        Log.d(TAG, uri.toString())
//        val shareIntent: Intent = Intent().apply {
//            action = Intent.ACTION_SEND
//            putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file))
//            type = "text/plain"
//        }
//        context.startActivity(Intent.createChooser(shareIntent, null))
//
//    }

//    fun getImageUri(inContext: Context, inImage: Bitmap): Uri? {
//        val bytes = ByteArrayOutputStream()
//        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
//        val path = MediaStore.Images.Media.insertImage(
//            inContext.getContentResolver(),
//            inImage,
//            "Title",
//            null
//        )
//        return Uri.parse(path)
//    }
//
//    private fun saveImage(image: Bitmap): Uri? {
//        //TODO - Should be processed in another thread
//        val imagesFolder: File = File(getActivity().getCacheDir(), "images")
//        var uri: Uri? = null
//        try {
//            imagesFolder.mkdirs()
//            val file = File(imagesFolder, "shared_image.png")
//            val stream = FileOutputStream(file)
//            image.compress(Bitmap.CompressFormat.PNG, 90, stream)
//            stream.flush()
//            stream.close()
//            uri = FileProvider.getUriForFile(this, "com.mydomain.fileprovider", file)
//        } catch (e: IOException) {
////            Log.d(TAG, "IOException while trying to write file for sharing: " + e.getMessage())
//        }
//        return uri
//    }

    fun getEncryptedFile() {
        viewModelScope.launch {
            val file = File(dirFile, fileName.value!!)
            val encryptedFile = EncryptionHelper.getEncryptedFile(file, context)
            launch(IO) {
                try {
                    encryptedFile.openFileInput().also {
                        message.postValue(String(it.readBytes(), Charsets.UTF_8))
                    }

                    sharetxtt()
                    snackBarMsg.postValue("File decrypted successfully!")

                } catch (e: Exception) {
                    snackBarMsg.postValue(e.message)
                }
            }
        }
    }


    fun shareimgg() {
        val file = File(dirImage, fileName.value!!)
        val intent = Intent(Intent.ACTION_SEND)
//        val uri = Uri.fromFile(file)
        val uri = FileProvider.getUriForFile(
            context,
            context.applicationContext.packageName + ".provider",
            file
        )
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.type = "image/jpg"
        context.startActivity(intent)
    }

    fun sharetxtt() {
        val file = File(dirFile, fileName.value!!)
        val intent = Intent(Intent.ACTION_SEND)
//        val uri = Uri.fromFile(file)
        val uri = FileProvider.getUriForFile(
            context,
            context.applicationContext.packageName + ".provider",
            file
        )
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.type = "text/plain"
        context.startActivity(intent)
    }

    fun getFileList() {
        isLoading.value = true
        viewModelScope.launch(IO) {
            val dir = File(context.filesDir.path)
            val list = dir.listFiles()
            list?.forEach {
                if (it.isDirectory) {
                    fileList.addAll(it.listFiles()?.map { file ->
                        FileEntity(file.name, file, "${file.length() / 1024} Kb")
                    }?.sortedByDescending { fileEntity ->
                        fileEntity.fileName
                    } ?: emptyList())
                    fileListEntity.postValue(fileList)

                }
                if (it.isFile) {

                    fileList.add(FileEntity(it.name, it, "${it.length() / 1024} Kb"))
                    fileListEntity.postValue(fileList)
                }
            }
            isLoading.postValue(false)
        }
    }

    fun getMasterToken() {
        masterToken.value = userRepository.getMasterKey()
    }

    fun setMasterToken() {
        userRepository.setMasterKey(masterToken.value!!)
        snackBarMsg.value = "Master Key has been set!"
    }

    fun updateMasterToken() {
        if (masterToken.value != userRepository.getMasterKey()) {
            snackBarMsg.value = "Master Key did not match"
        } else {
            userRepository.setMasterKey(newMasterToken.value!!)
            snackBarMsg.value = "Master Key updated successfully"

        }

    }


}