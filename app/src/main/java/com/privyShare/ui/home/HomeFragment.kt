package com.privyShare.ui.home

import android.R.attr.data
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.*
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.privyShare.R
import com.privyShare.data.FileEntity
import com.privyShare.ui.home.adapter.FileListAdapter
import com.privyShare.util.Utility
import kotlinx.android.synthetic.main.detail_dialog.*
import kotlinx.android.synthetic.main.detail_dialog.view.*
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.key_edit_dialog.*
import kotlinx.android.synthetic.main.key_setup_dialog.*
import java.io.File
import java.util.*


class HomeFragment : Fragment() {

    private var pos: Int = 0
    private var fileListEntity = ArrayList<FileEntity>()
    private val viewModel by viewModels<HomeViewModel>()
    private lateinit var fileListAdapter: FileListAdapter
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private lateinit var biometricPrompt: BiometricPrompt
    private val biometricCallback = object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            super.onAuthenticationError(errorCode, errString)
            Log.d("HomeFragment", "onAuthenticationError: $errorCode")

        }

        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            super.onAuthenticationSucceeded(result)
            Log.d("HomeFragment", "onAuthenticationSucceeded: $result ")
            dialogUpdateMasterKey()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBiometric()
        observeViewModel()
        viewModel.getFileList()
        viewModel.getMasterToken()

        fb_open_file.setOnClickListener {
            selectImage()
        }

    }

    private fun setupBiometric() {
        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock")
            .setDescription("Scan Fingerprint")
            .setDeviceCredentialAllowed(true)
            .build()
        biometricPrompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(requireContext()),
            biometricCallback
        )
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner, Observer {
            if (it) {
                filesRecyclerView.isGone = true
                progressBar.isVisible = true
            } else {
                progressBar.isGone = true
                filesRecyclerView.isVisible = true
            }
        })

        viewModel.snackBarMsg.observe(viewLifecycleOwner, Observer {
            Utility.displaySnackBar(it, requireView())
        })

        viewModel.fileListEntity.observe(viewLifecycleOwner, Observer {
            fileListEntity = it
            setupRecyclerView()
        })
    }

    private fun setupRecyclerView() {
        fileListAdapter = FileListAdapter(fileListEntity) {
            pos = it
            val fileEntity = fileListEntity[it]
            viewModel.fileName.value = fileEntity.fileName
            viewModel.getMasterToken()
            if (viewModel.masterToken.value.isNullOrEmpty()) {
                showAlertDialog(fileEntity.file)
            } else {
                dialogAuth()
            }
        }
        filesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = fileListAdapter
        }
    }



    private fun selectImage() {
//        val choice = arrayOf<CharSequence>("Take Photo", "Choose from Gallery", "Cancel")
//        val myAlertDialog: AlertDialog.Builder = AlertDialog.Builder(this)
//        myAlertDialog.setTitle("Select Image")
//        myAlertDialog.setItems(choice, DialogInterface.OnClickListener { dialog, item ->
//            when {
//                // Select "Choose from Gallery" to pick image from gallery
//                choice[item] == "Choose from Gallery" -> {
                    val pickFromGallery = Intent(Intent.ACTION_GET_CONTENT, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    pickFromGallery.type = "*/*"
                    startActivityForResult(pickFromGallery, 1)

//                }
//                // Select "Take Photo" to take a photo
//                choice[item] == "Take Photo" -> {
//                    val cameraPicture = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
//                    startActivityForResult(cameraPicture, 0)
//                }
//                // Select "Cancel" to cancel the task
//                choice[item] == "Cancel" -> {
//                    myAlertDialog.dismiss()
//                }
//            }
//        })
//        myAlertDialog.show()
        val file = File(pickFromGallery.data!!.path)
        showAlertDialog(file)
    }

    private fun showAlertDialog(file: File) {

        val dialog = MaterialDialog(requireContext())
            .customView(R.layout.detail_dialog)
            .cornerRadius(8f)
        val view = dialog.getCustomView()
        if (file.extension == "jpg") {
            view.dialog_imageView.isVisible = true
            viewModel.bmp.observe(viewLifecycleOwner, Observer {
                view.dialog_imageView.setImageBitmap(it)
            })

            viewModel.getEncryptedBitmap()
        } else {
            view.dialog_textView.isVisible = true
            viewModel.message.observe(viewLifecycleOwner, Observer {
                view.dialog_textView.text = it
            })
            viewModel.getEncryptedFile()
        }
        dialog.show()

    }



    private fun dialogSetMasterKey() {
        MaterialDialog(requireContext()).show {
            val view = customView(R.layout.key_setup_dialog)
            positiveButton(R.string.dialog_ok) {
                val key = view.txtApiKey.text.toString()
                viewModel.masterToken.value = key
                viewModel.setMasterToken()
                dismiss()
            }
            negativeButton(R.string.dialog_close) {
                it.dismiss()
            }
        }
    }

    private fun dialogUpdateMasterKey() {
        MaterialDialog(requireContext()).show {
            val view = customView(R.layout.key_edit_dialog)
            positiveButton(R.string.dialog_ok) {
                val oldKey = view.txtOldApiKey.text.toString()
                val newKey = view.txtNewApiKey.text.toString()
                viewModel.masterToken.value = oldKey
                viewModel.newMasterToken.value = newKey
                viewModel.updateMasterToken()
                dismiss()
            }
            negativeButton(R.string.dialog_close) {
                it.dismiss()
            }
        }
    }

    private fun dialogAuth() {
        MaterialDialog(requireContext()).show {
            val view = customView(R.layout.auth_dialog)
            positiveButton(R.string.dialog_ok) {
                val key = view.txtApiKey.text.toString()
                if (key == viewModel.masterToken.value) {
                    showAlertDialog(fileListEntity[pos].file)
                } else {
                    viewModel.snackBarMsg.value = "Keys don't match!"
                    it.dismiss()
                }
            }
            negativeButton(R.string.dialog_close) {
                it.dismiss()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.main_menu, menu)

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.navigation_master_key -> {
                viewModel.getMasterToken()
                if (viewModel.masterToken.value.isNullOrEmpty()) {
                    dialogSetMasterKey()
                } else {
                    when (BiometricManager.from(requireContext()).canAuthenticate()) {
                        BiometricManager.BIOMETRIC_SUCCESS -> {
                            biometricPrompt.authenticate(promptInfo)
                        }
                        BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE,
                        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED,
                        BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                            dialogUpdateMasterKey()

                        }
                    }
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }

    }
}