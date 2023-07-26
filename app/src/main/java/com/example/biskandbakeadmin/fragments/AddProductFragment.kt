package com.example.biskandbakeadmin.fragments

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore.Images
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.VISIBLE
import com.example.biskandbakeadmin.R
import com.example.biskandbakeadmin.adapter.AddProductImageAdapter
import com.example.biskandbakeadmin.databinding.FragmentAddProductBinding
import com.example.biskandbakeadmin.databinding.FragmentCategoryBinding
import com.example.biskandbakeadmin.databinding.FragmentProductBinding
import com.example.biskandbakeadmin.model.AddProductModel
import com.example.biskandbakeadmin.model.CategoryModel
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import java.util.*
import kotlin.collections.ArrayList


class AddProductFragment : Fragment() {

    private lateinit var binding: FragmentAddProductBinding
    private lateinit var list: ArrayList<Uri>
    private lateinit var listImages: ArrayList<String>
    private lateinit var adapter: AddProductImageAdapter
    private var coverImage: Uri ? =null
    private lateinit var dialog : Dialog
    private var coverImgUrl:String?= ""
    private lateinit var categoryList: ArrayList<String>


    private var launchGalleryActivity= registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ){
        if(it.resultCode== Activity.RESULT_OK){
            coverImage=it.data!!.data
            binding.productCoverImg.setImageURI(coverImage)
            binding.productCoverImg.visibility= VISIBLE
        }
    }


    private var launchProductActivity= registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ){
        if(it.resultCode== Activity.RESULT_OK){
            val imageUrl=it.data!!.data
            list.add(imageUrl!!)
            adapter.notifyDataSetChanged()
        }
    }



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding= FragmentAddProductBinding.inflate(layoutInflater)
        list= ArrayList()
        listImages=ArrayList()

        dialog= Dialog(requireContext())
        dialog.setContentView(R.layout.progress_layout)
        dialog.setCancelable(false)


        binding.selectCoverImg.setOnClickListener{
            val intent = Intent("android.intent.action.GET_CONTENT")
            intent.type="image/*"
            launchGalleryActivity.launch(intent)

        }

        binding.productImgBtn.setOnClickListener{
            val intent = Intent("android.intent.action.GET_CONTENT")
            intent.type="image/*"
            launchProductActivity.launch(intent)

        }

        setProductCategory()

        adapter= AddProductImageAdapter(list)
        binding.productImgRecyclerView.adapter=adapter

        binding.submitProductBtn.setOnClickListener{
            validateData()
        }

        return binding.root

    }

    private fun validateData() {
        if (binding.productNameEdit.text.toString().isEmpty()){
            binding.productNameEdit.requestFocus()
            binding.productNameEdit.error="Empty"
        }else if (binding.productSpEdit.text.toString().isEmpty()){
            binding.productSpEdit.requestFocus()
            binding.productSpEdit.error="Empty"

        }else if (coverImage==null){
            Toast.makeText(requireContext(),"Please Select cover Images",Toast.LENGTH_SHORT).show()
        }else if (listImages.size<1){
            Toast.makeText(requireContext(),"Please Select product Images",Toast.LENGTH_SHORT).show()
        }else uploadImage()
    }

    private fun uploadImage() {
        dialog.show()

        val filename = UUID.randomUUID().toString()+".jpg"

        val refStorage = FirebaseStorage.getInstance().reference.child("products/$filename")
        refStorage.putFile(coverImage!!)
            .addOnSuccessListener {
                it.storage.downloadUrl.addOnSuccessListener { image->
                    coverImgUrl=image.toString()

                    uploadProductImage()
                }
            }

            .addOnFailureListener{
                dialog.dismiss()
                Toast.makeText(requireContext(),"Something went Wrong with Storage",Toast.LENGTH_SHORT).show()

            }
    }
    private var i= 0
    private fun uploadProductImage() {
        dialog.show()

        val filename = UUID.randomUUID().toString()+".jpg"

        val refStorage = FirebaseStorage.getInstance().reference.child("products/$filename")
        refStorage.putFile(list[i])
            .addOnSuccessListener {
                it.storage.downloadUrl.addOnSuccessListener { image->
                    listImages.add(image!!.toString())
                    if (list.size==listImages.size) {
                        storeData()
                    }else {
                        i += 1
                        uploadProductImage()
                    }
                }
            }

            .addOnFailureListener{
                dialog.dismiss()
                Toast.makeText(requireContext(),"Something went Wrong with Storage",Toast.LENGTH_SHORT).show()

            }
    }

    private fun storeData() {
        val db =Firebase.firestore.collection("products")
        val key= db.id

        val data = AddProductModel(
            binding.productNameEdit.text.toString(),
            binding.productDescriptionEdit.text.toString(),
            coverImgUrl.toString(),
            categoryList[binding.productCategoryDropdown.selectedItemPosition],
            key,
            binding.productMrpEdit.text.toString(),
            binding.productSpEdit.text.toString(),
            listImages

        )

        db.document(key).set(data).addOnSuccessListener {
            dialog.dismiss()
            Toast.makeText(requireContext(),"Product Added ",Toast.LENGTH_SHORT).show()
            binding.productNameEdit.text=null
        }
            .addOnFailureListener{
                dialog.dismiss()
                Toast.makeText(requireContext(),"Something Went Wrong",Toast.LENGTH_SHORT).show()
            }
    }

    private fun setProductCategory(){
        categoryList= ArrayList()
        Firebase.firestore.collection("categories").get().addOnSuccessListener {
            categoryList.clear()
            for (doc in it.documents){
                val data =doc .toObject(CategoryModel::class.java)
                categoryList.add(data!!.cat!!)

            }
            categoryList.add(0,"Select Category ")


            val arrayAdapter = ArrayAdapter(requireContext(),R.layout.dropdown_item_layout,categoryList)
            binding.productCategoryDropdown.adapter=arrayAdapter
        }
    }
}